"""Dependency-free WSGI API for the BusPay synchronization service."""

from __future__ import annotations

import hmac
import json
from pathlib import Path
from typing import Any, Callable, Dict, Iterable, Mapping, Tuple

from .contract import CONTRACT_VERSION, ContractError, parse_catalog, parse_sync_batch
from .database import SyncDatabase


MAX_REQUEST_BYTES = 1_000_000
ROLE_DEVICE_SYNC = "device_sync"
ROLE_REPORT_READ = "report_read"
ROLE_CATALOG_READ = "catalog_read"
ROLE_CATALOG_WRITE = "catalog_write"
ALL_ROLES = frozenset(
    (ROLE_DEVICE_SYNC, ROLE_REPORT_READ, ROLE_CATALOG_READ, ROLE_CATALOG_WRITE)
)
WEB_ROOT = Path(__file__).with_name("web")
ADMIN_ASSETS = {
    "/admin": ("admin.html", "text/html; charset=utf-8"),
    "/admin/": ("admin.html", "text/html; charset=utf-8"),
    "/admin/assets/admin.css": ("admin.css", "text/css; charset=utf-8"),
    "/admin/assets/admin.js": ("admin.js", "text/javascript; charset=utf-8"),
}
StartResponse = Callable[[str, list], None]


class BusPayApplication:
    def __init__(
        self,
        database: SyncDatabase,
        bearer_token: str | Mapping[str, str],
    ) -> None:
        self.database = database
        if isinstance(bearer_token, str):
            role_tokens = {role: bearer_token for role in ALL_ROLES}
        else:
            role_tokens = dict(bearer_token)
        if set(role_tokens) != ALL_ROLES:
            raise ValueError("Every server access role requires a token")
        if any(not token or "\n" in token or "\r" in token for token in role_tokens.values()):
            raise ValueError("Every server access role requires a valid token")
        self.role_tokens = {
            role: token.encode("utf-8") for role, token in role_tokens.items()
        }

    def __call__(self, environ: Dict[str, Any], start_response: StartResponse) -> Iterable[bytes]:
        try:
            status, headers, body = self._dispatch(environ)
        except ContractError as error:
            status, headers, body = self._json_response(
                error.status_code,
                {"error": error.message, "contractVersion": CONTRACT_VERSION},
            )
        except Exception:
            status, headers, body = self._json_response(
                500,
                {"error": "Internal server error", "contractVersion": CONTRACT_VERSION},
            )
        start_response(status, headers)
        return [body]

    def _dispatch(self, environ: Dict[str, Any]) -> Tuple[str, list, bytes]:
        method = environ.get("REQUEST_METHOD", "GET").upper()
        path = environ.get("PATH_INFO", "/")
        if path in ADMIN_ASSETS:
            if method != "GET":
                return self._method_not_allowed("GET")
            filename, content_type = ADMIN_ASSETS[path]
            return self._static_response(WEB_ROOT / filename, content_type)
        if method == "GET" and path == "/health":
            return self._json_response(
                200,
                {
                    "status": "ok" if self.database.health() else "unavailable",
                    "contractVersion": CONTRACT_VERSION,
                    "database": "ready",
                },
            )
        if path == "/v1/access":
            if method != "GET":
                return self._method_not_allowed("GET")
            principal, unauthorized = self._authorize(environ, ALL_ROLES)
            if unauthorized is not None:
                return unauthorized
            return self._json_response(
                200,
                {
                    "contractVersion": CONTRACT_VERSION,
                    "roles": sorted(principal),
                },
            )
        if path == "/v1/sync":
            if method != "POST":
                return self._method_not_allowed("POST")
            _, unauthorized = self._authorize(environ, {ROLE_DEVICE_SYNC})
            if unauthorized is not None:
                return unauthorized
            payload = self._read_json(environ)
            batch = parse_sync_batch(payload)
            if environ.get("HTTP_X_BUSPAY_CONTRACT_VERSION") != str(CONTRACT_VERSION):
                raise ContractError("X-BusPay-Contract-Version must match the request contract")
            if environ.get("HTTP_IDEMPOTENCY_KEY") != batch.request_id:
                raise ContractError("Idempotency-Key must match the request ID")
            response, replayed = self.database.ingest(batch)
            return self._json_response(200, response, [("X-Idempotent-Replay", str(replayed).lower())])
        if path == "/v1/reports/admin":
            if method != "GET":
                return self._method_not_allowed("GET")
            _, unauthorized = self._authorize(environ, {ROLE_REPORT_READ})
            if unauthorized is not None:
                return unauthorized
            return self._json_response(200, self.database.report())
        if path == "/v1/catalog":
            if method not in ("GET", "PUT"):
                return self._method_not_allowed("GET, PUT")
            required_roles = (
                {ROLE_CATALOG_READ, ROLE_CATALOG_WRITE}
                if method == "GET"
                else {ROLE_CATALOG_WRITE}
            )
            _, unauthorized = self._authorize(environ, required_roles)
            if unauthorized is not None:
                return unauthorized
            if method == "GET":
                return self._json_response(200, self.database.catalog())
            payload = self._read_json(environ)
            return self._json_response(200, self.database.replace_catalog(parse_catalog(payload)))
        return self._json_response(
            404,
            {"error": "Endpoint not found", "contractVersion": CONTRACT_VERSION},
        )

    def _authorize(self, environ: Dict[str, Any], allowed_roles):
        authorization = environ.get("HTTP_AUTHORIZATION", "")
        scheme, separator, supplied_token = authorization.partition(" ")
        supplied = supplied_token.encode("utf-8")
        roles = frozenset(
            role
            for role, expected in self.role_tokens.items()
            if separator == " "
            and scheme.lower() == "bearer"
            and hmac.compare_digest(supplied, expected)
        )
        if not roles:
            return None, self._json_response(
                401,
                {"error": "Authentication required", "contractVersion": CONTRACT_VERSION},
                [("WWW-Authenticate", 'Bearer realm="buspay-sync"')],
            )
        if roles.isdisjoint(allowed_roles):
            return roles, self._json_response(
                403,
                {"error": "Insufficient permission", "contractVersion": CONTRACT_VERSION},
            )
        return roles, None

    @staticmethod
    def _read_json(environ: Dict[str, Any]) -> Any:
        content_type = environ.get("CONTENT_TYPE", "").split(";", 1)[0].strip().lower()
        if content_type != "application/json":
            raise ContractError("Content-Type must be application/json", 415)
        raw_length = environ.get("CONTENT_LENGTH", "")
        try:
            content_length = int(raw_length)
        except (TypeError, ValueError):
            raise ContractError("A valid Content-Length header is required", 411)
        if content_length < 1 or content_length > MAX_REQUEST_BYTES:
            raise ContractError("Request body size is invalid", 413)
        raw_body = environ["wsgi.input"].read(content_length)
        try:
            return json.loads(raw_body.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError):
            raise ContractError("Request body must be valid UTF-8 JSON")

    @staticmethod
    def _method_not_allowed(allowed_method: str):
        return BusPayApplication._json_response(
            405,
            {"error": "Method not allowed", "contractVersion": CONTRACT_VERSION},
            [("Allow", allowed_method)],
        )

    @staticmethod
    def _json_response(status_code: int, value: Any, extra_headers=None):
        body = json.dumps(value, sort_keys=True, separators=(",", ":")).encode("utf-8")
        status_text = {
            200: "OK",
            400: "Bad Request",
            401: "Unauthorized",
            403: "Forbidden",
            404: "Not Found",
            405: "Method Not Allowed",
            409: "Conflict",
            411: "Length Required",
            413: "Payload Too Large",
            415: "Unsupported Media Type",
            500: "Internal Server Error",
        }.get(status_code, "Error")
        headers = [
            ("Content-Type", "application/json; charset=utf-8"),
            ("Content-Length", str(len(body))),
            ("Cache-Control", "no-store"),
            ("X-Content-Type-Options", "nosniff"),
        ]
        headers.extend(extra_headers or [])
        return f"{status_code} {status_text}", headers, body

    @staticmethod
    def _static_response(path: Path, content_type: str):
        body = path.read_bytes()
        headers = [
            ("Content-Type", content_type),
            ("Content-Length", str(len(body))),
            ("Cache-Control", "no-store"),
            ("X-Content-Type-Options", "nosniff"),
            ("Content-Security-Policy", "default-src 'none'; script-src 'self'; style-src 'self'; connect-src 'self'; img-src 'self' data:; base-uri 'none'; form-action 'none'; frame-ancestors 'none'"),
            ("Referrer-Policy", "no-referrer"),
            ("X-Frame-Options", "DENY"),
            ("Permissions-Policy", "camera=(), geolocation=(), microphone=()"),
            ("Cross-Origin-Opener-Policy", "same-origin"),
            ("Cross-Origin-Resource-Policy", "same-origin"),
        ]
        return "200 OK", headers, body

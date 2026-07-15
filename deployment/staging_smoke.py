"""Authenticated, non-mutating smoke checks for a deployed BusPay staging service."""

from __future__ import annotations

import argparse
from dataclasses import dataclass
import json
from pathlib import Path
from typing import Callable
from urllib.error import HTTPError
from urllib.parse import urlsplit
from urllib.request import Request, urlopen


@dataclass(frozen=True)
class SmokeConfiguration:
    base_url: str
    token: str
    allow_local_http: bool = False

    def validate(self) -> None:
        parsed = urlsplit(self.base_url)
        is_loopback = parsed.hostname in ("localhost", "127.0.0.1", "::1")
        allowed_scheme = parsed.scheme == "https" or (
            self.allow_local_http and parsed.scheme == "http" and is_loopback
        )
        if not allowed_scheme or not parsed.hostname or parsed.path not in ("", "/"):
            raise ValueError("Smoke-test URL must be an HTTPS origin")
        if not self.token or "\n" in self.token or "\r" in self.token:
            raise ValueError("A valid smoke-test token is required")


def _request(url: str, token: str | None = None) -> tuple[int, dict[str, str], bytes]:
    headers = {"Accept": "application/json"}
    if token is not None:
        headers["Authorization"] = f"Bearer {token}"
    request = Request(url, headers=headers, method="GET")
    try:
        with urlopen(request, timeout=10) as response:
            return response.status, dict(response.headers.items()), response.read()
    except HTTPError as error:
        return error.code, dict(error.headers.items()), error.read()


def run_smoke(
    configuration: SmokeConfiguration,
    requester: Callable[[str, str | None], tuple[int, dict[str, str], bytes]] = _request,
) -> str:
    configuration.validate()
    root = configuration.base_url.rstrip("/")

    health_status, health_headers, health_raw = requester(f"{root}/health", None)
    if health_status != 200:
        raise RuntimeError(f"Health check failed with HTTP {health_status}")
    health = json.loads(health_raw)
    if health.get("status") != "ok" or health.get("database") != "ready":
        raise RuntimeError("Health response is not ready")
    if health_headers.get("Cache-Control", "").lower() != "no-store":
        raise RuntimeError("Health response is missing no-store cache protection")

    report_status, report_headers, report_raw = requester(
        f"{root}/v1/reports/admin", configuration.token
    )
    if report_status != 200:
        raise RuntimeError(f"Authenticated report failed with HTTP {report_status}")
    report = json.loads(report_raw)
    overall = report.get("overall")
    if not isinstance(overall, dict) or not all(
        name in overall for name in ("driverCount", "shiftCount", "ticketCount", "cashTotalCents")
    ):
        raise RuntimeError("Authenticated report does not match contract v1")
    if report_headers.get("Cache-Control", "").lower() != "no-store":
        raise RuntimeError("Report response is missing no-store cache protection")

    rejected_status, _, _ = requester(
        f"{root}/v1/reports/admin", f"invalid-{configuration.token}"
    )
    if rejected_status != 401:
        raise RuntimeError("Invalid-token check did not return HTTP 401")

    return (
        "BusPay staging smoke: PASS\n"
        f"Contract: {health.get('contractVersion')}\n"
        f"Drivers: {overall['driverCount']}\n"
        f"Closed shifts: {overall['shiftCount']}\n"
        f"Tickets: {overall['ticketCount']}\n"
        f"Cash cents: {overall['cashTotalCents']}\n"
        "Invalid token: rejected"
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--token-file", type=Path, required=True)
    parser.add_argument("--allow-local-http", action="store_true", help=argparse.SUPPRESS)
    arguments = parser.parse_args()
    token = arguments.token_file.read_text(encoding="utf-8").strip()
    print(run_smoke(SmokeConfiguration(arguments.base_url.rstrip("/"), token, arguments.allow_local_http)))


if __name__ == "__main__":
    main()

"""Runtime configuration boundary shared by development and production servers."""

from __future__ import annotations

import os
from pathlib import Path
from typing import Mapping, Optional

from .application import (
    ROLE_CATALOG_READ,
    ROLE_CATALOG_WRITE,
    ROLE_DEVICE_SYNC,
    ROLE_REPORT_READ,
    BusPayApplication,
)
from .database import SyncDatabase


DEFAULT_DATABASE_PATH = "/data/buspay.db"
MAX_SECRET_BYTES = 8_192


class RuntimeConfigurationError(ValueError):
    """Raised when safe server startup configuration is incomplete."""


def create_application(environ: Optional[Mapping[str, str]] = None) -> BusPayApplication:
    values = os.environ if environ is None else environ
    credentials = _load_role_tokens(values)
    database_path = values.get("BUSPAY_DB_PATH", DEFAULT_DATABASE_PATH).strip()
    if not database_path:
        raise RuntimeConfigurationError("BUSPAY_DB_PATH cannot be empty")
    return BusPayApplication(SyncDatabase(database_path), credentials)


def _load_role_tokens(environ: Mapping[str, str]) -> dict[str, str]:
    role_file_names = {
        "device": "BUSPAY_DEVICE_TOKEN_FILE",
        "report": "BUSPAY_REPORT_TOKEN_FILE",
        "catalog": "BUSPAY_CATALOG_TOKEN_FILE",
    }
    configured_role_files = {
        name: environ.get(variable, "").strip()
        for name, variable in role_file_names.items()
        if environ.get(variable, "").strip()
    }
    legacy_configured = bool(
        environ.get("BUSPAY_SYNC_TOKEN_FILE", "").strip()
        or environ.get("BUSPAY_SYNC_TOKEN", "").strip()
    )
    if configured_role_files and legacy_configured:
        raise RuntimeConfigurationError(
            "Role token files cannot be combined with the legacy shared token"
        )
    if configured_role_files:
        if set(configured_role_files) != set(role_file_names):
            raise RuntimeConfigurationError(
                "Device, report, and catalog token files must all be configured"
            )
        tokens = {
            name: _load_token_file(Path(path), role_file_names[name])
            for name, path in configured_role_files.items()
        }
        if len(set(tokens.values())) != len(tokens):
            raise RuntimeConfigurationError("Role tokens must be distinct")
        return {
            ROLE_DEVICE_SYNC: tokens["device"],
            ROLE_REPORT_READ: tokens["report"],
            ROLE_CATALOG_READ: tokens["device"],
            ROLE_CATALOG_WRITE: tokens["catalog"],
        }

    legacy = _load_bearer_token(environ)
    return {
        ROLE_DEVICE_SYNC: legacy,
        ROLE_REPORT_READ: legacy,
        ROLE_CATALOG_READ: legacy,
        ROLE_CATALOG_WRITE: legacy,
    }


def _load_token_file(path: Path, variable_name: str) -> str:
    try:
        if path.stat().st_size > MAX_SECRET_BYTES:
            raise RuntimeConfigurationError(f"{variable_name} is too large")
        token = path.read_text(encoding="utf-8").strip()
    except OSError as error:
        raise RuntimeConfigurationError(f"{variable_name} cannot be read") from error
    if not token or "\n" in token or "\r" in token:
        raise RuntimeConfigurationError(f"{variable_name} must contain one valid token")
    return token


def _load_bearer_token(environ: Mapping[str, str]) -> str:
    token_file = environ.get("BUSPAY_SYNC_TOKEN_FILE", "").strip()
    inline_token = environ.get("BUSPAY_SYNC_TOKEN", "")
    if token_file and inline_token:
        raise RuntimeConfigurationError(
            "Configure BUSPAY_SYNC_TOKEN_FILE or BUSPAY_SYNC_TOKEN, not both"
        )
    if token_file:
        token = _load_token_file(Path(token_file), "BUSPAY_SYNC_TOKEN_FILE")
    else:
        token = inline_token.strip()
    if not token or "\n" in token or "\r" in token:
        raise RuntimeConfigurationError("A valid bearer token is required")
    return token

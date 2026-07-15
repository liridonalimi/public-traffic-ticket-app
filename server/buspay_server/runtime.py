"""Runtime configuration boundary shared by development and production servers."""

from __future__ import annotations

import os
from pathlib import Path
from typing import Mapping, Optional

from .application import BusPayApplication
from .database import SyncDatabase


DEFAULT_DATABASE_PATH = "/data/buspay.db"
MAX_SECRET_BYTES = 8_192


class RuntimeConfigurationError(ValueError):
    """Raised when safe server startup configuration is incomplete."""


def create_application(environ: Optional[Mapping[str, str]] = None) -> BusPayApplication:
    values = os.environ if environ is None else environ
    token = _load_bearer_token(values)
    database_path = values.get("BUSPAY_DB_PATH", DEFAULT_DATABASE_PATH).strip()
    if not database_path:
        raise RuntimeConfigurationError("BUSPAY_DB_PATH cannot be empty")
    return BusPayApplication(SyncDatabase(database_path), token)


def _load_bearer_token(environ: Mapping[str, str]) -> str:
    token_file = environ.get("BUSPAY_SYNC_TOKEN_FILE", "").strip()
    inline_token = environ.get("BUSPAY_SYNC_TOKEN", "")
    if token_file and inline_token:
        raise RuntimeConfigurationError(
            "Configure BUSPAY_SYNC_TOKEN_FILE or BUSPAY_SYNC_TOKEN, not both"
        )
    if token_file:
        path = Path(token_file)
        try:
            if path.stat().st_size > MAX_SECRET_BYTES:
                raise RuntimeConfigurationError("Bearer-token secret file is too large")
            token = path.read_text(encoding="utf-8").strip()
        except OSError as error:
            raise RuntimeConfigurationError("Bearer-token secret file cannot be read") from error
    else:
        token = inline_token.strip()
    if not token or "\n" in token or "\r" in token:
        raise RuntimeConfigurationError("A valid bearer token is required")
    return token

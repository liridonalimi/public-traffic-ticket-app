"""Fail-closed validation for a BusPay staging deployment configuration."""

from __future__ import annotations

import argparse
from dataclasses import dataclass
import os
from pathlib import Path
import re
import stat
from typing import Mapping, Optional
from urllib.parse import urlsplit


IMAGE_DIGEST = re.compile(r"^[^\s@]+@sha256:[0-9a-f]{64}$")
PLACEHOLDER_PARTS = ("replace-with", ".example", "example/", "example.")
MINIMUM_TOKEN_CHARACTERS = 32


class StagingPreflightError(ValueError):
    """Raised when a staging release is unsafe or operationally unowned."""


@dataclass(frozen=True)
class StagingConfiguration:
    base_url: str
    image: str
    edge_network: str
    device_token_file: Path
    report_token_file: Path
    catalog_token_file: Path
    audit_token_file: Path
    region: str
    operations_owner: str
    security_owner: str
    backup_owner: str
    backup_retention_days: int

    @classmethod
    def from_values(
        cls,
        values: Mapping[str, str],
        relative_to: Optional[Path] = None,
    ) -> "StagingConfiguration":
        def required(name: str) -> str:
            value = values.get(name, "").strip()
            if not value:
                raise StagingPreflightError(f"{name} is required")
            lowered = value.lower()
            if any(part in lowered for part in PLACEHOLDER_PARTS):
                raise StagingPreflightError(f"{name} still contains a placeholder")
            return value

        base_url = required("BUSPAY_STAGING_BASE_URL")
        parsed_url = urlsplit(base_url)
        if (
            parsed_url.scheme != "https"
            or not parsed_url.hostname
            or parsed_url.username
            or parsed_url.password
            or parsed_url.path not in ("", "/")
            or parsed_url.query
            or parsed_url.fragment
        ):
            raise StagingPreflightError(
                "BUSPAY_STAGING_BASE_URL must be an HTTPS origin without credentials, path, query, or fragment"
            )
        if parsed_url.hostname in ("localhost", "127.0.0.1", "::1"):
            raise StagingPreflightError("BUSPAY_STAGING_BASE_URL cannot use a loopback host")

        image = required("BUSPAY_IMAGE")
        if not IMAGE_DIGEST.fullmatch(image):
            raise StagingPreflightError("BUSPAY_IMAGE must be pinned by sha256 digest")

        def protected_token(name: str) -> tuple[Path, tuple[str, ...]]:
            token_file = Path(required(name)).expanduser()
            if not token_file.is_absolute() and relative_to is not None:
                token_file = relative_to / token_file
            try:
                token_stat = token_file.stat()
                raw_tokens = token_file.read_text(encoding="utf-8")
            except OSError as error:
                raise StagingPreflightError(f"{name} cannot be read") from error
            if not stat.S_ISREG(token_stat.st_mode):
                raise StagingPreflightError(f"{name} must be a regular file")
            if token_stat.st_mode & (stat.S_IRWXG | stat.S_IRWXO):
                raise StagingPreflightError(f"{name} must not allow group or other access")
            tokens = tuple(line.strip() for line in raw_tokens.splitlines() if line.strip())
            if (
                not 1 <= len(tokens) <= 2
                or len(set(tokens)) != len(tokens)
                or any(len(token) < MINIMUM_TOKEN_CHARACTERS for token in tokens)
            ):
                raise StagingPreflightError(
                    f"{name} must contain one active token or two distinct rotation tokens, each at least {MINIMUM_TOKEN_CHARACTERS} characters"
                )
            return token_file, tokens

        device_token_file, device_token = protected_token("BUSPAY_DEVICE_TOKEN_FILE")
        report_token_file, report_token = protected_token("BUSPAY_REPORT_TOKEN_FILE")
        catalog_token_file, catalog_token = protected_token("BUSPAY_CATALOG_TOKEN_FILE")
        audit_token_file, audit_token = protected_token("BUSPAY_AUDIT_TOKEN_FILE")
        all_tokens = device_token + report_token + catalog_token + audit_token
        if len(set(all_tokens)) != len(all_tokens):
            raise StagingPreflightError("Device, report, catalog, and audit tokens must be distinct")

        raw_retention = required("BUSPAY_BACKUP_RETENTION_DAYS")
        try:
            retention = int(raw_retention)
        except ValueError as error:
            raise StagingPreflightError("BUSPAY_BACKUP_RETENTION_DAYS must be an integer") from error
        if not 7 <= retention <= 365:
            raise StagingPreflightError("BUSPAY_BACKUP_RETENTION_DAYS must be between 7 and 365")

        return cls(
            base_url=base_url.rstrip("/"),
            image=image,
            edge_network=required("BUSPAY_EDGE_NETWORK"),
            device_token_file=device_token_file,
            report_token_file=report_token_file,
            catalog_token_file=catalog_token_file,
            audit_token_file=audit_token_file,
            region=required("BUSPAY_STAGING_REGION"),
            operations_owner=required("BUSPAY_OPERATIONS_OWNER"),
            security_owner=required("BUSPAY_SECURITY_OWNER"),
            backup_owner=required("BUSPAY_BACKUP_OWNER"),
            backup_retention_days=retention,
        )

    def safe_summary(self) -> str:
        return "\n".join(
            (
                "BusPay staging preflight: READY",
                f"URL: {self.base_url}",
                f"Image: {self.image}",
                f"Private ingress network: {self.edge_network}",
                f"Region: {self.region}",
                f"Operations owner: {self.operations_owner}",
                f"Security owner: {self.security_owner}",
                f"Backup owner: {self.backup_owner}",
                f"Backup retention: {self.backup_retention_days} days",
                "Role tokens: 4 protected files and rotation bundles validated (values not displayed)",
            )
        )


def read_env_file(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for line_number, raw_line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            raise StagingPreflightError(f"Invalid environment entry on line {line_number}")
        name, value = line.split("=", 1)
        name = name.strip()
        if not name or not re.fullmatch(r"[A-Z][A-Z0-9_]*", name):
            raise StagingPreflightError(f"Invalid environment name on line {line_number}")
        values[name] = value.strip().strip("'\"")
    return values


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--env-file", type=Path, required=True)
    arguments = parser.parse_args()
    env_file = arguments.env_file.resolve()
    values = read_env_file(env_file)
    values.update({name: value for name, value in os.environ.items() if name.startswith("BUSPAY_")})
    configuration = StagingConfiguration.from_values(values, env_file.parent)
    print(configuration.safe_summary())


if __name__ == "__main__":
    main()

"""Verify and atomically restore a BusPay SQLite backup."""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
from pathlib import Path
import sqlite3


def restore_backup(backup_path: Path, target_path: Path, replace: bool = False) -> Path:
    checksum_path = backup_path.with_suffix(".db.sha256")
    if not backup_path.is_file() or not checksum_path.is_file():
        raise ValueError("Backup database and checksum are required")
    expected_checksum = checksum_path.read_text(encoding="utf-8").split()[0]
    actual_checksum = hashlib.sha256(backup_path.read_bytes()).hexdigest()
    if not expected_checksum or actual_checksum != expected_checksum:
        raise ValueError("Backup checksum does not match")

    with sqlite3.connect(str(backup_path)) as source:
        if source.execute("PRAGMA integrity_check").fetchone()[0] != "ok":
            raise ValueError("Backup integrity check failed")

    target_path.parent.mkdir(parents=True, exist_ok=True)
    preserved = None
    if target_path.exists():
        if not replace:
            raise ValueError("Target exists; pass replace=True only during an approved restore")
        timestamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
        preserved = target_path.with_name(f"{target_path.name}.pre-restore-{timestamp}")
        target_path.replace(preserved)

    temporary = target_path.with_suffix(target_path.suffix + ".partial")
    temporary_sidecars = tuple(
        Path(f"{temporary}{suffix}") for suffix in ("-shm", "-wal")
    )
    try:
        with sqlite3.connect(str(backup_path)) as source:
            with sqlite3.connect(temporary) as target:
                source.backup(target)
                target.execute("PRAGMA journal_mode=DELETE").fetchone()
        temporary.replace(target_path)
    except Exception:
        if preserved is not None and not target_path.exists():
            preserved.replace(target_path)
        raise
    finally:
        temporary.unlink(missing_ok=True)
        for sidecar in temporary_sidecars:
            sidecar.unlink(missing_ok=True)
    return target_path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--backup", type=Path, required=True)
    parser.add_argument("--target", type=Path, required=True)
    parser.add_argument("--replace", action="store_true")
    arguments = parser.parse_args()
    restored = restore_backup(arguments.backup, arguments.target, arguments.replace)
    print(restored)


if __name__ == "__main__":
    main()

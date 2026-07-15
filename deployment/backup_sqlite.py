"""Create an integrity-checked, checksummed SQLite backup."""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
from pathlib import Path
import sqlite3


def create_backup(database_path: Path, output_directory: Path) -> Path:
    if not database_path.is_file():
        raise ValueError("Source database does not exist")
    output_directory.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    destination = output_directory / f"buspay-{timestamp}.db"
    temporary = destination.with_suffix(".db.partial")
    temporary_sidecars = tuple(
        Path(f"{temporary}{suffix}") for suffix in ("-shm", "-wal")
    )
    if destination.exists() or temporary.exists():
        raise ValueError("A backup already exists for this timestamp")

    source_uri = f"file:{database_path.resolve()}?mode=ro"
    try:
        with sqlite3.connect(source_uri, uri=True) as source:
            with sqlite3.connect(temporary) as target:
                source.backup(target)
                target.execute("PRAGMA journal_mode=DELETE").fetchone()
                integrity = target.execute("PRAGMA integrity_check").fetchone()[0]
                if integrity != "ok":
                    raise ValueError("Backup integrity check failed")
        temporary.replace(destination)
    finally:
        temporary.unlink(missing_ok=True)
        for sidecar in temporary_sidecars:
            sidecar.unlink(missing_ok=True)
    checksum = hashlib.sha256(destination.read_bytes()).hexdigest()
    destination.with_suffix(".db.sha256").write_text(
        f"{checksum}  {destination.name}\n",
        encoding="utf-8",
    )
    return destination


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--database", type=Path, required=True)
    parser.add_argument("--output-directory", type=Path, required=True)
    arguments = parser.parse_args()
    backup = create_backup(arguments.database, arguments.output_directory)
    print(backup)


if __name__ == "__main__":
    main()

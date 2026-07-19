from __future__ import annotations

import hashlib
from pathlib import Path
import sqlite3
import tempfile
import unittest

from buspay_server.contract import parse_sync_batch
from buspay_server.database import SyncDatabase
from buspay_server.runtime import RuntimeConfigurationError, create_application
from deployment.backup_sqlite import create_backup
from deployment.restore_sqlite import restore_backup


PROJECT_ROOT = Path(__file__).resolve().parents[2]


def payload():
    return {
        "contractVersion": 1,
        "requestId": "sync-aaaabbbbccccdddd",
        "sentAtMillis": 300,
        "shifts": [
            {
                "id": "shift-deployment",
                "driverId": "driver-001",
                "busId": "bus-001",
                "routeId": "route-001",
                "startedAtMillis": 100,
                "endedAtMillis": 300,
            }
        ],
        "tickets": [
            {
                "id": "ticket-deployment",
                "shiftId": "shift-deployment",
                "fareTypeId": "standard",
                "priceCents": 50,
                "soldAtMillis": 200,
            }
        ],
    }


class RuntimeConfigurationTest(unittest.TestCase):
    def test_runtime_loads_file_secret_without_persisting_it(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            secret = root / "token.txt"
            database = root / "buspay.db"
            secret.write_text("file-secret\n", encoding="utf-8")

            application = create_application(
                {
                    "BUSPAY_SYNC_TOKEN_FILE": str(secret),
                    "BUSPAY_DB_PATH": str(database),
                }
            )

            self.assertTrue(application.database.health())
            self.assertNotIn(b"file-secret", database.read_bytes())

    def test_runtime_loads_distinct_role_secret_files(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            values = {"BUSPAY_DB_PATH": str(root / "buspay.db")}
            for role in ("DEVICE", "REPORT", "CATALOG"):
                secret = root / f"{role.lower()}.txt"
                secret.write_text(f"distinct-{role.lower()}-token\n", encoding="utf-8")
                values[f"BUSPAY_{role}_TOKEN_FILE"] = str(secret)

            application = create_application(values)

            self.assertTrue(application.database.health())
            self.assertEqual(4, len(application.role_tokens))

    def test_runtime_rejects_partial_or_reused_role_tokens(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            shared = root / "shared.txt"
            shared.write_text("same-token", encoding="utf-8")
            partial = {
                "BUSPAY_DEVICE_TOKEN_FILE": str(shared),
                "BUSPAY_REPORT_TOKEN_FILE": str(shared),
            }
            reused = {
                **partial,
                "BUSPAY_CATALOG_TOKEN_FILE": str(shared),
            }

            for values in (partial, reused):
                with self.assertRaises(RuntimeConfigurationError):
                    create_application(values)

    def test_runtime_rejects_ambiguous_missing_and_unreadable_secrets(self):
        invalid_values = (
            {},
            {"BUSPAY_SYNC_TOKEN": "inline", "BUSPAY_SYNC_TOKEN_FILE": "/tmp/secret"},
            {"BUSPAY_SYNC_TOKEN_FILE": "/missing/buspay-secret"},
        )

        for values in invalid_values:
            with self.subTest(values=values):
                with self.assertRaises(RuntimeConfigurationError):
                    create_application(values)


class BackupRestoreTest(unittest.TestCase):
    def setUp(self):
        self.temp_directory = tempfile.TemporaryDirectory()
        self.root = Path(self.temp_directory.name)
        self.source_path = self.root / "source.db"
        self.database = SyncDatabase(str(self.source_path))
        self.database.ingest(parse_sync_batch(payload()))

    def tearDown(self):
        self.temp_directory.cleanup()

    def test_backup_and_restore_preserve_reconciled_data(self):
        backup = create_backup(self.source_path, self.root / "backups")
        restored_path = restore_backup(backup, self.root / "restored.db")
        restored_report = SyncDatabase(str(restored_path)).report()

        self.assertTrue(backup.with_suffix(".db.sha256").is_file())
        self.assertEqual([], list(self.root.glob("**/*.partial*")))
        self.assertEqual(1, restored_report["overall"]["shiftCount"])
        self.assertEqual(1, restored_report["overall"]["ticketCount"])
        self.assertEqual(50, restored_report["overall"]["cashTotalCents"])

    def test_restore_rejects_tampered_backup_and_existing_target(self):
        backup = create_backup(self.source_path, self.root / "backups")
        target = self.root / "target.db"
        target.write_bytes(b"existing")

        with self.assertRaises(ValueError):
            restore_backup(backup, target)

        backup.with_suffix(".db.sha256").write_text(
            f"{'0' * 64}  {backup.name}\n",
            encoding="utf-8",
        )
        with self.assertRaises(ValueError):
            restore_backup(backup, self.root / "another.db")


class DeploymentAssetTest(unittest.TestCase):
    def test_container_runs_pinned_non_root_read_only_service(self):
        dockerfile = (PROJECT_ROOT / "deployment/Dockerfile").read_text(encoding="utf-8")
        compose = (PROJECT_ROOT / "deployment/compose.yaml").read_text(encoding="utf-8")
        requirements = (PROJECT_ROOT / "deployment/requirements.txt").read_text(
            encoding="utf-8"
        )

        self.assertIn("python:3.12.13-slim-trixie", dockerfile)
        self.assertIn("USER 10001:10001", dockerfile)
        self.assertIn("/data /backups", dockerfile)
        self.assertIn("chown -R 10001:10001 /opt/buspay /data /backups", dockerfile)
        self.assertIn("HEALTHCHECK", dockerfile)
        self.assertIn("gunicorn", dockerfile)
        self.assertEqual(
            ["gunicorn==26.0.0", "packaging==26.2"],
            requirements.splitlines(),
        )
        for required in (
            "127.0.0.1:8080:8080",
            "read_only: true",
            "no-new-privileges:true",
            "cap_drop:",
            "buspay-data:/data",
            "/run/secrets/buspay_device_token",
            "/run/secrets/buspay_report_token",
            "/run/secrets/buspay_catalog_token",
            "healthcheck:",
        ):
            self.assertIn(required, compose)

    def test_build_context_excludes_credentials_and_local_artifacts(self):
        ignored = (PROJECT_ROOT / "deployment/Dockerfile.dockerignore").read_text(
            encoding="utf-8"
        )
        compose = (PROJECT_ROOT / "deployment/compose.yaml").read_text(encoding="utf-8")
        gitignored = (PROJECT_ROOT / ".gitignore").read_text(encoding="utf-8")
        example_secret = (
            PROJECT_ROOT / "deployment/secrets/buspay_sync_token.txt.example"
        ).read_text(encoding="utf-8")

        self.assertIn("deployment/secrets/*.txt", ignored)
        self.assertNotIn("replace-with-a-long-random-secret", compose)
        self.assertIn("deployment/secrets/*.txt", gitignored)
        for name in ("device", "report", "catalog"):
            self.assertNotIn(f"module-21-local-{name}-token", compose)
        self.assertEqual(64, len(hashlib.sha256(example_secret.encode("utf-8")).hexdigest()))


if __name__ == "__main__":
    unittest.main()

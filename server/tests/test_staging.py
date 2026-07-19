from __future__ import annotations

from pathlib import Path
import tempfile
import unittest

from deployment.staging_preflight import StagingConfiguration, StagingPreflightError
from deployment.staging_smoke import SmokeConfiguration, run_smoke


PROJECT_ROOT = Path(__file__).resolve().parents[2]
DIGEST_IMAGE = "registry.buspay.test/sync@sha256:" + ("a" * 64)


class StagingPreflightTest(unittest.TestCase):
    def setUp(self):
        self.temp_directory = tempfile.TemporaryDirectory()
        self.root = Path(self.temp_directory.name)
        self.token_files = {}
        for role in ("device", "report", "catalog", "audit"):
            token_file = self.root / f"{role}-token"
            token_file.write_text(
                f"staging-{role}-token-with-at-least-32-characters\n",
                encoding="utf-8",
            )
            token_file.chmod(0o600)
            self.token_files[role] = token_file
        self.values = {
            "BUSPAY_STAGING_BASE_URL": "https://staging.buspay.test",
            "BUSPAY_IMAGE": DIGEST_IMAGE,
            "BUSPAY_EDGE_NETWORK": "staging-edge",
            "BUSPAY_DEVICE_TOKEN_FILE": str(self.token_files["device"]),
            "BUSPAY_REPORT_TOKEN_FILE": str(self.token_files["report"]),
            "BUSPAY_CATALOG_TOKEN_FILE": str(self.token_files["catalog"]),
            "BUSPAY_AUDIT_TOKEN_FILE": str(self.token_files["audit"]),
            "BUSPAY_STAGING_REGION": "eu-test-1",
            "BUSPAY_OPERATIONS_OWNER": "operations-team",
            "BUSPAY_SECURITY_OWNER": "security-team",
            "BUSPAY_BACKUP_OWNER": "database-team",
            "BUSPAY_BACKUP_RETENTION_DAYS": "14",
        }

    def tearDown(self):
        self.temp_directory.cleanup()

    def test_ready_configuration_is_redacted_and_owned(self):
        configuration = StagingConfiguration.from_values(self.values)
        summary = configuration.safe_summary()

        self.assertIn("READY", summary)
        self.assertIn("operations-team", summary)
        self.assertNotIn("staging-token-with", summary)

    def test_insecure_origin_mutable_image_and_placeholder_are_rejected(self):
        invalid_overrides = (
            {"BUSPAY_STAGING_BASE_URL": "http://staging.buspay.test"},
            {"BUSPAY_IMAGE": "registry.buspay.test/sync:latest"},
            {"BUSPAY_SECURITY_OWNER": "replace-with-security-owner"},
        )
        for override in invalid_overrides:
            with self.subTest(override=override):
                values = dict(self.values)
                values.update(override)
                with self.assertRaises(StagingPreflightError):
                    StagingConfiguration.from_values(values)

    def test_weak_or_permissive_secret_is_rejected(self):
        self.token_files["device"].write_text("too-short", encoding="utf-8")
        with self.assertRaises(StagingPreflightError):
            StagingConfiguration.from_values(self.values)

        self.token_files["device"].write_text(
            "staging-device-token-with-at-least-32-characters",
            encoding="utf-8",
        )
        self.token_files["device"].chmod(0o644)
        with self.assertRaises(StagingPreflightError):
            StagingConfiguration.from_values(self.values)

    def test_two_token_rotation_bundle_is_accepted_but_duplicates_are_rejected(self):
        self.token_files["device"].write_text(
            "staging-device-old-token-with-at-least-32-characters\n"
            "staging-device-new-token-with-at-least-32-characters\n",
            encoding="utf-8",
        )
        configuration = StagingConfiguration.from_values(self.values)
        self.assertEqual(self.token_files["device"], configuration.device_token_file)

        self.token_files["audit"].write_text(
            "staging-device-new-token-with-at-least-32-characters\n",
            encoding="utf-8",
        )
        with self.assertRaises(StagingPreflightError):
            StagingConfiguration.from_values(self.values)

class StagingSmokeTest(unittest.TestCase):
    def test_smoke_checks_health_report_and_invalid_token(self):
        calls = []

        def requester(url, token):
            calls.append((url, token))
            if url.endswith("/health"):
                return 200, {"Cache-Control": "no-store"}, b'{"status":"ok","database":"ready","contractVersion":1}'
            if token == "correct-token":
                return 200, {"Cache-Control": "no-store"}, (
                    b'{"overall":{"driverCount":2,"shiftCount":3,"ticketCount":4,"cashTotalCents":125}}'
                )
            return 401, {"Cache-Control": "no-store"}, b'{"error":"Authentication required"}'

        output = run_smoke(
            SmokeConfiguration("https://staging.buspay.test", "correct-token"),
            requester,
        )

        self.assertIn("PASS", output)
        self.assertIn("Invalid token: rejected", output)
        self.assertEqual(3, len(calls))

    def test_smoke_rejects_cleartext_non_loopback(self):
        with self.assertRaises(ValueError):
            run_smoke(SmokeConfiguration("http://staging.buspay.test", "token"))

    def test_smoke_checks_audit_contract_and_role_isolation(self):
        def requester(url, token):
            headers = {"Cache-Control": "no-store"}
            if url.endswith("/health"):
                return 200, headers, b'{"status":"ok","database":"ready","contractVersion":1}'
            if url.endswith("/v1/audit?limit=25"):
                if token == "audit-token":
                    return 200, headers, b'{"contractVersion":1,"events":[]}'
                return 403, headers, b'{"error":"Insufficient permission"}'
            if url.endswith("/v1/reports/admin"):
                if token == "report-token":
                    return 200, headers, b'{"overall":{"driverCount":0,"shiftCount":0,"ticketCount":0,"cashTotalCents":0}}'
                if token == "audit-token":
                    return 403, headers, b'{"error":"Insufficient permission"}'
                return 401, headers, b'{"error":"Authentication required"}'
            raise AssertionError(url)

        output = run_smoke(
            SmokeConfiguration(
                "https://staging.buspay.test",
                "report-token",
                audit_token="audit-token",
            ),
            requester,
        )

        self.assertIn("Audit role: isolated", output)


class StagingAssetTest(unittest.TestCase):
    def test_staging_compose_uses_digest_secret_private_ingress_and_hardening(self):
        compose = (PROJECT_ROOT / "deployment/compose.staging.yaml").read_text(encoding="utf-8")
        for required in (
            "BUSPAY_IMAGE:?",
            "BUSPAY_DEVICE_TOKEN_FILE:?",
            "BUSPAY_REPORT_TOKEN_FILE:?",
            "BUSPAY_CATALOG_TOKEN_FILE:?",
            "BUSPAY_AUDIT_TOKEN_FILE:?",
            "BUSPAY_EDGE_NETWORK:?",
            "external: true",
            'expose:',
            '"8080"',
            "read_only: true",
            "no-new-privileges:true",
            "cap_drop:",
        ):
            self.assertIn(required, compose)
        self.assertNotIn("ports:", compose)
        self.assertNotIn("BUSPAY_SYNC_TOKEN:", compose)


if __name__ == "__main__":
    unittest.main()

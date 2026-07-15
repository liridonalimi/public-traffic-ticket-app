from __future__ import annotations

from io import BytesIO
import json
from pathlib import Path
import tempfile
import unittest

from buspay_server.application import BusPayApplication
from buspay_server.contract import ContractError, parse_sync_batch
from buspay_server.database import SyncDatabase


TOKEN = "module-12-test-token"


def batch_payload(request_id="sync-0123456789abcdef", price_cents=30):
    return {
        "contractVersion": 1,
        "requestId": request_id,
        "sentAtMillis": 300,
        "shifts": [
            {
                "id": "shift-1",
                "driverId": "driver-001",
                "busId": "bus-001",
                "routeId": "route-001",
                "startedAtMillis": 100,
                "endedAtMillis": 300,
            }
        ],
        "tickets": [
            {
                "id": "ticket-1",
                "shiftId": "shift-1",
                "fareTypeId": "student",
                "priceCents": price_cents,
                "soldAtMillis": 200,
            }
        ],
    }


class ContractTest(unittest.TestCase):
    def test_contract_rejects_active_duplicate_and_unsupported_records(self):
        active = batch_payload()
        active["shifts"][0]["endedAtMillis"] = 50
        duplicate = batch_payload()
        duplicate["tickets"].append(dict(duplicate["tickets"][0]))
        unsupported = batch_payload()
        unsupported["contractVersion"] = 2

        for invalid in (active, duplicate, unsupported):
            with self.assertRaises(ContractError):
                parse_sync_batch(invalid)


class DatabaseTest(unittest.TestCase):
    def setUp(self):
        self.temp_directory = tempfile.TemporaryDirectory()
        self.database_path = str(Path(self.temp_directory.name) / "buspay.db")
        self.database = SyncDatabase(self.database_path)

    def tearDown(self):
        self.temp_directory.cleanup()

    def test_ingest_is_transactional_persistent_and_idempotent(self):
        batch = parse_sync_batch(batch_payload())
        first, first_replayed = self.database.ingest(batch)
        second, second_replayed = self.database.ingest(batch)
        restarted_report = SyncDatabase(self.database_path).report()

        self.assertFalse(first_replayed)
        self.assertTrue(second_replayed)
        self.assertEqual(first, second)
        self.assertEqual(1, restarted_report["overall"]["shiftCount"])
        self.assertEqual(1, restarted_report["overall"]["ticketCount"])
        self.assertEqual(30, restarted_report["overall"]["cashTotalCents"])

    def test_same_request_with_changed_payload_conflicts_without_mutation(self):
        self.database.ingest(parse_sync_batch(batch_payload()))

        with self.assertRaises(ContractError) as failure:
            self.database.ingest(parse_sync_batch(batch_payload(price_cents=50)))

        self.assertEqual(409, failure.exception.status_code)
        self.assertEqual(30, self.database.report()["overall"]["cashTotalCents"])

    def test_ticket_for_unknown_shift_rolls_back_request(self):
        payload = batch_payload()
        payload["shifts"] = []
        payload["tickets"][0]["shiftId"] = "missing-shift"

        with self.assertRaises(ContractError):
            self.database.ingest(parse_sync_batch(payload))

        self.assertEqual(0, self.database.report()["overall"]["ticketCount"])

    def test_admin_dashboard_sample_reconciles_expected_totals(self):
        sample_path = Path(__file__).resolve().parents[1] / "sample-admin-sync.json"
        sample = json.loads(sample_path.read_text(encoding="utf-8"))

        self.database.ingest(parse_sync_batch(sample))
        report = self.database.report()

        self.assertEqual(3, report["overall"]["driverCount"])
        self.assertEqual(4, report["overall"]["shiftCount"])
        self.assertEqual(12, report["overall"]["ticketCount"])
        self.assertEqual(400, report["overall"]["cashTotalCents"])


class ApplicationTest(unittest.TestCase):
    def setUp(self):
        self.database = SyncDatabase(":memory:")
        self.application = BusPayApplication(self.database, TOKEN)

    def request(
        self,
        method,
        path,
        payload=None,
        token=None,
        content_type="application/json",
        contract_version="1",
        idempotency_key=None,
    ):
        status, headers, body = self.request_raw(
            method,
            path,
            payload,
            token,
            content_type,
            contract_version,
            idempotency_key,
        )
        return status, headers, json.loads(body)

    def request_raw(
        self,
        method,
        path,
        payload=None,
        token=None,
        content_type="application/json",
        contract_version="1",
        idempotency_key=None,
    ):
        raw_body = b"" if payload is None else json.dumps(payload).encode("utf-8")
        environ = {
            "REQUEST_METHOD": method,
            "PATH_INFO": path,
            "CONTENT_TYPE": content_type,
            "CONTENT_LENGTH": str(len(raw_body)),
            "wsgi.input": BytesIO(raw_body),
        }
        if token is not None:
            environ["HTTP_AUTHORIZATION"] = f"Bearer {token}"
        if contract_version is not None:
            environ["HTTP_X_BUSPAY_CONTRACT_VERSION"] = contract_version
        if payload is not None:
            environ["HTTP_IDEMPOTENCY_KEY"] = idempotency_key or payload.get("requestId", "")
        captured = {}

        def start_response(status, headers):
            captured["status"] = status
            captured["headers"] = dict(headers)

        body = b"".join(self.application(environ, start_response))
        return int(captured["status"].split()[0]), captured["headers"], body

    def test_health_is_public_and_reports_database_readiness(self):
        status, _, body = self.request("GET", "/health")

        self.assertEqual(200, status)
        self.assertEqual("ok", body["status"])
        self.assertEqual("ready", body["database"])

    def test_admin_dashboard_assets_are_public_but_hardened(self):
        html_status, html_headers, html = self.request_raw("GET", "/admin")
        css_status, css_headers, css = self.request_raw("GET", "/admin/assets/admin.css")
        js_status, js_headers, javascript = self.request_raw("GET", "/admin/assets/admin.js")

        self.assertEqual(200, html_status)
        self.assertEqual(200, css_status)
        self.assertEqual(200, js_status)
        self.assertEqual("text/html; charset=utf-8", html_headers["Content-Type"])
        self.assertEqual("text/css; charset=utf-8", css_headers["Content-Type"])
        self.assertEqual("text/javascript; charset=utf-8", js_headers["Content-Type"])
        self.assertIn("default-src 'none'", html_headers["Content-Security-Policy"])
        self.assertIn("connect-src 'self'", html_headers["Content-Security-Policy"])
        self.assertEqual("DENY", html_headers["X-Frame-Options"])
        self.assertEqual("no-referrer", html_headers["Referrer-Policy"])
        self.assertIn(b"BusPay Control", html)
        self.assertIn(b"/admin/assets/admin.css", html)
        self.assertIn(b"/v1/reports/admin", javascript)
        self.assertIn(b"Authorization", javascript)
        self.assertNotIn(b"localStorage", javascript)
        self.assertNotIn(b"sessionStorage", javascript)
        self.assertNotIn(TOKEN.encode("utf-8"), html + css + javascript)

    def test_admin_dashboard_rejects_unsupported_methods(self):
        status, headers, _ = self.request("POST", "/admin")

        self.assertEqual(405, status)
        self.assertEqual("GET", headers["Allow"])

    def test_sync_and_report_require_valid_bearer_authentication(self):
        missing, missing_headers, _ = self.request("POST", "/v1/sync", batch_payload())
        wrong, _, _ = self.request("GET", "/v1/reports/admin", token="wrong")

        self.assertEqual(401, missing)
        self.assertIn("Bearer", missing_headers["WWW-Authenticate"])
        self.assertEqual(401, wrong)

    def test_authenticated_sync_replay_and_report_reconcile(self):
        first_status, first_headers, acknowledgement = self.request(
            "POST", "/v1/sync", batch_payload(), TOKEN
        )
        replay_status, replay_headers, replay_acknowledgement = self.request(
            "POST", "/v1/sync", batch_payload(), TOKEN
        )
        report_status, _, report = self.request("GET", "/v1/reports/admin", token=TOKEN)

        self.assertEqual(200, first_status)
        self.assertEqual("false", first_headers["X-Idempotent-Replay"])
        self.assertEqual(200, replay_status)
        self.assertEqual("true", replay_headers["X-Idempotent-Replay"])
        self.assertEqual(acknowledgement, replay_acknowledgement)
        self.assertEqual(["shift-1"], acknowledgement["acknowledgedShiftIds"])
        self.assertEqual(["ticket-1"], acknowledgement["acknowledgedTicketIds"])
        self.assertEqual(200, report_status)
        self.assertEqual(1, report["overall"]["driverCount"])
        self.assertEqual(1, report["overall"]["shiftCount"])
        self.assertEqual(1, report["overall"]["ticketCount"])
        self.assertEqual(30, report["overall"]["cashTotalCents"])
        self.assertEqual("student", report["fares"][0]["fareTypeId"])
        self.assertEqual(["shift-1"], report["drivers"][0]["shiftIds"])
        self.assertEqual("SYNCED", report["shifts"][0]["syncStatus"])

    def test_invalid_content_and_method_return_safe_errors(self):
        method_status, method_headers, _ = self.request("GET", "/v1/sync", token=TOKEN)
        content_status, _, content_body = self.request(
            "POST", "/v1/sync", batch_payload(), TOKEN, "text/plain"
        )

        self.assertEqual(405, method_status)
        self.assertEqual("POST", method_headers["Allow"])
        self.assertEqual(415, content_status)
        self.assertEqual("Content-Type must be application/json", content_body["error"])

    def test_contract_and_idempotency_headers_must_match_body(self):
        contract_status, _, _ = self.request(
            "POST", "/v1/sync", batch_payload(), TOKEN, contract_version="2"
        )
        key_status, _, key_body = self.request(
            "POST",
            "/v1/sync",
            batch_payload(),
            TOKEN,
            idempotency_key="sync-fedcba9876543210",
        )

        self.assertEqual(400, contract_status)
        self.assertEqual(400, key_status)
        self.assertEqual("Idempotency-Key must match the request ID", key_body["error"])


if __name__ == "__main__":
    unittest.main()

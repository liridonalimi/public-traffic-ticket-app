from __future__ import annotations

from io import BytesIO
import json
from pathlib import Path
import sqlite3
import tempfile
import unittest

import buspay_server.database as database_module
from buspay_server.application import (
    ROLE_AUDIT_READ,
    ROLE_CATALOG_READ,
    ROLE_CATALOG_WRITE,
    ROLE_DEVICE_SYNC,
    ROLE_REPORT_READ,
    BusPayApplication,
)
from buspay_server.contract import ContractError, parse_catalog, parse_sync_batch
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
                "expectedCashCents": 30,
                "declaredCashCents": 25,
                "reconciledAtMillis": 300,
                "scheduledTripId": "trip-route-1-0800",
                "assignmentId": "assignment-test-001",
            }
        ],
        "tickets": [
            {
                "id": "ticket-1",
                "shiftId": "shift-1",
                "fareTypeId": "student",
                "priceCents": price_cents,
                "soldAtMillis": 200,
                "farePolicyRevision": 2,
                "originStopId": "stop-origin",
                "destinationStopId": "stop-destination",
                "zoneCount": 2,
                "offPeakApplied": True,
                "transferValidUntilMillis": 1_800_200,
            }
        ],
    }


def catalog_payload(expected_revision=1):
    return {
        "contractVersion": 1,
        "expectedRevision": expected_revision,
        "drivers": [{"id": "driver-managed", "name": "Besa Gashi"}],
        "buses": [{"id": "bus-managed", "plateNumber": "01-900-KS"}],
        "routes": [{"id": "route-managed", "name": "Linja Testuese"}],
        "stops": [
            {
                "id": "stop-managed",
                "routeId": "route-managed",
                "name": "Ndalesa Testuese",
                "latitude": 42.66,
                "longitude": 21.16,
                "order": 1,
                "zoneId": "A",
            }
        ],
        "fares": [
            {
                "id": "standard",
                "name": "E rregullt",
                "priceCents": 60,
                "eligibility": None,
                "additionalZoneCents": 20,
                "offPeakDiscountCents": 10,
                "offPeakStartMinutes": 540,
                "offPeakEndMinutes": 960,
                "transferWindowMinutes": 30,
                "routeId": "route-managed",
            }
        ],
        "serviceCalendars": [
            {
                "id": "calendar-managed",
                "name": "Daily test service",
                "startDate": "2026-01-01",
                "endDate": "2030-12-31",
                "activeWeekdays": [1, 2, 3, 4, 5, 6, 7],
            }
        ],
        "scheduledTrips": [
            {
                "id": "trip-managed",
                "routeId": "route-managed",
                "serviceCalendarId": "calendar-managed",
                "departureMinutes": 480,
                "direction": "OUTBOUND",
                "stopTimes": [
                    {"stopId": "stop-managed", "arrivalMinutes": 480, "departureMinutes": 500}
                ],
            }
        ],
        "tripAssignments": [
            {
                "id": "assignment-managed",
                "tripId": "trip-managed",
                "serviceDate": "2026-07-20",
                "driverId": "driver-managed",
                "busId": "bus-managed",
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

    def test_catalog_requires_complete_consistent_reference_data(self):
        parsed = parse_catalog(catalog_payload())
        missing_route = catalog_payload()
        missing_route["stops"][0]["routeId"] = "unknown"
        duplicate_order = catalog_payload()
        duplicate_order["stops"].append(
            {**duplicate_order["stops"][0], "id": "stop-managed-2"}
        )

        self.assertEqual("Besa Gashi", parsed.drivers[0].name)
        self.assertEqual("A", parsed.stops[0].zone_id)
        self.assertEqual(20, parsed.fares[0].additional_zone_cents)
        self.assertEqual(540, parsed.fares[0].off_peak_start_minutes)
        for invalid in (missing_route, duplicate_order):
            with self.assertRaises(ContractError):
                parse_catalog(invalid)

    def test_cash_reconciliation_is_all_or_none_and_legacy_shifts_remain_valid(self):
        complete = parse_sync_batch(batch_payload()).shifts[0]
        legacy = batch_payload()
        for field in ("expectedCashCents", "declaredCashCents", "reconciledAtMillis"):
            legacy["shifts"][0].pop(field)
        partial = batch_payload()
        partial["shifts"][0]["declaredCashCents"] = None

        self.assertEqual(30, complete.expected_cash_cents)
        self.assertEqual("trip-route-1-0800", complete.scheduled_trip_id)
        self.assertIsNone(parse_sync_batch(legacy).shifts[0].expected_cash_cents)
        with self.assertRaises(ContractError):
            parse_sync_batch(partial)

    def test_fare_policy_snapshot_is_all_or_none_and_legacy_tickets_remain_valid(self):
        complete = parse_sync_batch(batch_payload()).tickets[0]
        legacy = batch_payload()
        snapshot_fields = (
            "farePolicyRevision", "originStopId", "destinationStopId", "zoneCount",
            "offPeakApplied", "transferValidUntilMillis",
        )
        for field in snapshot_fields:
            legacy["tickets"][0].pop(field)
        partial = batch_payload()
        partial["tickets"][0].pop("destinationStopId")

        self.assertEqual(2, complete.fare_policy_revision)
        self.assertIsNone(parse_sync_batch(legacy).tickets[0].fare_policy_revision)
        with self.assertRaises(ContractError):
            parse_sync_batch(partial)

    def test_every_route_requires_an_applicable_fare(self):
        incomplete = catalog_payload()
        incomplete["routes"].append({"id": "route-without-fare", "name": "Unpriced route"})
        incomplete["stops"].append({
            **incomplete["stops"][0],
            "id": "stop-without-fare",
            "routeId": "route-without-fare",
        })

        with self.assertRaises(ContractError):
            parse_catalog(incomplete)

    def test_schedule_references_and_overlapping_assignments_are_rejected(self):
        valid = parse_catalog(catalog_payload())
        overlap = catalog_payload()
        overlap["scheduledTrips"].append({
            **overlap["scheduledTrips"][0],
            "id": "trip-overlap",
            "departureMinutes": 490,
            "stopTimes": [
                {"stopId": "stop-managed", "arrivalMinutes": 490, "departureMinutes": 510}
            ],
        })
        overlap["tripAssignments"].append({
            **overlap["tripAssignments"][0],
            "id": "assignment-overlap",
            "tripId": "trip-overlap",
        })

        self.assertEqual("trip-managed", valid.scheduled_trips[0].id)
        with self.assertRaises(ContractError):
            parse_catalog(overlap)


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
        self.assertEqual(30, restarted_report["overall"]["expectedCashTotalCents"])
        self.assertEqual(25, restarted_report["overall"]["declaredCashTotalCents"])
        self.assertEqual(-5, restarted_report["overall"]["cashVarianceTotalCents"])
        self.assertEqual(1, restarted_report["overall"]["reconciledShiftCount"])
        self.assertEqual("SHORTAGE", restarted_report["shifts"][0]["cashReconciliationStatus"])
        self.assertEqual("trip-route-1-0800", restarted_report["shifts"][0]["scheduledTripId"])
        self.assertEqual("assignment-test-001", restarted_report["shifts"][0]["assignmentId"])
        ticket = restarted_report["shifts"][0]["tickets"][0]
        self.assertEqual(2, ticket["farePolicyRevision"])
        self.assertEqual("stop-origin", ticket["originStopId"])
        self.assertEqual("stop-destination", ticket["destinationStopId"])
        self.assertEqual(2, ticket["zoneCount"])
        self.assertTrue(ticket["offPeakApplied"])

    def test_existing_shift_table_is_migrated_for_reconciliation(self):
        legacy_path = str(Path(self.temp_directory.name) / "legacy.db")
        with sqlite3.connect(legacy_path) as connection:
            connection.execute(
                """
                CREATE TABLE shifts (
                    id TEXT PRIMARY KEY,
                    driver_id TEXT NOT NULL,
                    bus_id TEXT NOT NULL,
                    route_id TEXT NOT NULL,
                    started_at_millis INTEGER NOT NULL,
                    ended_at_millis INTEGER NOT NULL,
                    first_request_id TEXT NOT NULL
                )
                """
            )

        SyncDatabase(legacy_path)
        with sqlite3.connect(legacy_path) as connection:
            columns = {row[1] for row in connection.execute("PRAGMA table_info(shifts)")}

        self.assertTrue(
            {
                "expected_cash_cents", "declared_cash_cents", "reconciled_at_millis",
                "scheduled_trip_id", "assignment_id",
            }
            <= columns
        )

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
        self.assertEqual(400, report["overall"]["expectedCashTotalCents"])
        self.assertEqual(400, report["overall"]["declaredCashTotalCents"])
        self.assertEqual(0, report["overall"]["cashVarianceTotalCents"])
        self.assertEqual(4, report["overall"]["reconciledShiftCount"])
        self.assertEqual(
            {"MATCHED", "SHORTAGE", "SURPLUS"},
            {shift["cashReconciliationStatus"] for shift in report["shifts"]},
        )

    def test_catalog_is_seeded_replaced_atomically_and_versioned(self):
        initial = self.database.catalog()
        replaced = self.database.replace_catalog(parse_catalog(catalog_payload(initial["revision"])))
        restarted = SyncDatabase(self.database_path).catalog()

        self.assertGreater(len(initial["drivers"]), 1)
        self.assertEqual(initial["revision"] + 1, replaced["revision"])
        self.assertEqual("driver-managed", restarted["drivers"][0]["id"])
        self.assertEqual(60, restarted["fares"][0]["priceCents"])
        self.assertEqual("A", restarted["stops"][0]["zoneId"])
        self.assertEqual(20, restarted["fares"][0]["additionalZoneCents"])
        self.assertEqual(30, restarted["fares"][0]["transferWindowMinutes"])

        with self.assertRaises(ContractError) as conflict:
            self.database.replace_catalog(parse_catalog(catalog_payload(initial["revision"])))
        self.assertEqual(409, conflict.exception.status_code)
        self.assertEqual(replaced, self.database.catalog())

    def test_authorization_audit_is_persistent_limited_and_contains_no_token(self):
        self.database.record_authorization_event(
            "allowed", "GET", "/v1/catalog", (ROLE_CATALOG_READ,), "127.0.0.1"
        )
        self.database.record_authorization_event(
            "forbidden", "PUT", "/v1/catalog", (ROLE_CATALOG_READ,), "127.0.0.1"
        )

        audit = SyncDatabase(self.database_path).authorization_audit(limit=1)

        self.assertEqual(1, len(audit["events"]))
        self.assertEqual("forbidden", audit["events"][0]["outcome"])
        self.assertEqual([ROLE_CATALOG_READ], audit["events"][0]["roles"])
        self.assertNotIn("token", json.dumps(audit).lower())

    def test_authorization_audit_retention_is_bounded(self):
        original_limit = database_module.MAX_AUTHORIZATION_EVENTS
        database_module.MAX_AUTHORIZATION_EVENTS = 3
        try:
            for index in range(100):
                self.database.record_authorization_event(
                    "allowed", "GET", f"/v1/test/{index}", (), "127.0.0.1"
                )
        finally:
            database_module.MAX_AUTHORIZATION_EVENTS = original_limit

        events = self.database.authorization_audit(500)["events"]
        self.assertEqual(3, len(events))
        self.assertEqual("/v1/test/99", events[0]["path"])


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
        path_info, _, query_string = path.partition("?")
        environ = {
            "REQUEST_METHOD": method,
            "PATH_INFO": path_info,
            "QUERY_STRING": query_string,
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
        self.assertIn(b"/v1/catalog", javascript)
        self.assertIn(b"Operations catalog", html)
        self.assertIn(b"Publish catalog", html)
        self.assertIn(b"Authorization", javascript)
        self.assertIn(b"metric-cash-variance", html)
        self.assertIn(b"cashReconciliationStatus", javascript)
        self.assertIn(b"Cash handover", javascript)
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

    def test_role_tokens_are_least_privilege_and_access_is_discoverable(self):
        self.application = BusPayApplication(
            self.database,
            {
                ROLE_DEVICE_SYNC: "device-token",
                ROLE_CATALOG_READ: "device-token",
                ROLE_REPORT_READ: "report-token",
                ROLE_CATALOG_WRITE: "catalog-token",
                ROLE_AUDIT_READ: ("audit-token-old", "audit-token-new"),
            },
        )

        device_access, _, device_roles = self.request("GET", "/v1/access", token="device-token")
        sync_status, _, _ = self.request(
            "POST", "/v1/sync", batch_payload(), token="device-token"
        )
        device_catalog, _, _ = self.request("GET", "/v1/catalog", token="device-token")
        device_report, _, _ = self.request("GET", "/v1/reports/admin", token="device-token")
        device_write, _, _ = self.request(
            "PUT", "/v1/catalog", catalog_payload(), token="device-token"
        )

        report_access, _, report_roles = self.request("GET", "/v1/access", token="report-token")
        report_status, _, _ = self.request("GET", "/v1/reports/admin", token="report-token")
        report_catalog, _, _ = self.request("GET", "/v1/catalog", token="report-token")
        report_sync, _, _ = self.request(
            "POST", "/v1/sync", batch_payload(), token="report-token"
        )

        catalog_access, _, catalog_roles = self.request("GET", "/v1/access", token="catalog-token")
        catalog_read, _, current = self.request("GET", "/v1/catalog", token="catalog-token")
        catalog_write, _, _ = self.request(
            "PUT",
            "/v1/catalog",
            catalog_payload(current["revision"]),
            token="catalog-token",
        )
        catalog_report, _, _ = self.request("GET", "/v1/reports/admin", token="catalog-token")
        audit_old, _, audit_payload = self.request("GET", "/v1/audit", token="audit-token-old")
        audit_new, _, _ = self.request("GET", "/v1/audit", token="audit-token-new")
        audit_report, _, _ = self.request("GET", "/v1/audit", token="report-token")
        audit_limited, _, limited_payload = self.request(
            "GET", "/v1/audit?limit=1", token="audit-token-new"
        )
        audit_bad_limit, _, _ = self.request(
            "GET", "/v1/audit?limit=0", token="audit-token-new"
        )
        access_wrong_method, access_headers, _ = self.request(
            "POST", "/v1/access", token="report-token"
        )

        self.assertEqual(200, device_access)
        self.assertEqual([ROLE_CATALOG_READ, ROLE_DEVICE_SYNC], device_roles["roles"])
        self.assertEqual((200, 200, 403, 403), (sync_status, device_catalog, device_report, device_write))
        self.assertEqual(200, report_access)
        self.assertEqual([ROLE_REPORT_READ], report_roles["roles"])
        self.assertEqual((200, 403, 403), (report_status, report_catalog, report_sync))
        self.assertEqual(200, catalog_access)
        self.assertEqual([ROLE_CATALOG_WRITE], catalog_roles["roles"])
        self.assertEqual((200, 200, 403), (catalog_read, catalog_write, catalog_report))
        self.assertEqual((200, 200, 403), (audit_old, audit_new, audit_report))
        self.assertEqual((200, 400), (audit_limited, audit_bad_limit))
        self.assertEqual(1, len(limited_payload["events"]))
        self.assertGreater(len(audit_payload["events"]), 0)
        self.assertTrue(all("token" not in json.dumps(event).lower() for event in audit_payload["events"]))
        self.assertEqual(405, access_wrong_method)
        self.assertEqual("GET", access_headers["Allow"])

    def test_catalog_requires_authentication_and_supports_versioned_replace(self):
        missing_status, _, _ = self.request("GET", "/v1/catalog")
        get_status, _, initial = self.request("GET", "/v1/catalog", token=TOKEN)
        update_status, _, updated = self.request(
            "PUT",
            "/v1/catalog",
            catalog_payload(initial["revision"]),
            TOKEN,
        )
        stale_status, _, stale = self.request(
            "PUT",
            "/v1/catalog",
            catalog_payload(initial["revision"]),
            TOKEN,
        )

        self.assertEqual(401, missing_status)
        self.assertEqual(200, get_status)
        self.assertEqual(200, update_status)
        self.assertEqual(initial["revision"] + 1, updated["revision"])
        self.assertEqual(409, stale_status)
        self.assertIn("refresh", stale["error"])

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
        self.assertEqual(30, report["overall"]["expectedCashTotalCents"])
        self.assertEqual(25, report["overall"]["declaredCashTotalCents"])
        self.assertEqual(-5, report["overall"]["cashVarianceTotalCents"])
        self.assertEqual("SHORTAGE", report["shifts"][0]["cashReconciliationStatus"])
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

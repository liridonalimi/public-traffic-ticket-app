"""Transactional SQLite persistence for BusPay synchronization batches."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
import sqlite3
import time
from typing import Any, Dict, Iterable, Tuple

from .contract import CONTRACT_VERSION, ContractError, ShiftInput, SyncBatchInput, TicketInput


class SyncDatabase:
    def __init__(self, path: str) -> None:
        self.path = path
        if path != ":memory:":
            Path(path).parent.mkdir(parents=True, exist_ok=True)
        self._memory_connection = self._connect() if path == ":memory:" else None
        self.initialize()

    def _connect(self) -> sqlite3.Connection:
        connection = sqlite3.connect(self.path, timeout=10.0)
        connection.row_factory = sqlite3.Row
        connection.execute("PRAGMA foreign_keys = ON")
        connection.execute("PRAGMA busy_timeout = 10000")
        if self.path != ":memory:":
            connection.execute("PRAGMA journal_mode = WAL")
        return connection

    def _connection(self) -> Tuple[sqlite3.Connection, bool]:
        if self._memory_connection is not None:
            return self._memory_connection, False
        return self._connect(), True

    def initialize(self) -> None:
        connection, should_close = self._connection()
        try:
            connection.executescript(
                """
                CREATE TABLE IF NOT EXISTS sync_requests (
                    request_id TEXT PRIMARY KEY,
                    contract_version INTEGER NOT NULL,
                    payload_hash TEXT NOT NULL,
                    response_json TEXT NOT NULL,
                    received_at_millis INTEGER NOT NULL
                );

                CREATE TABLE IF NOT EXISTS shifts (
                    id TEXT PRIMARY KEY,
                    driver_id TEXT NOT NULL,
                    bus_id TEXT NOT NULL,
                    route_id TEXT NOT NULL,
                    started_at_millis INTEGER NOT NULL,
                    ended_at_millis INTEGER NOT NULL,
                    first_request_id TEXT NOT NULL REFERENCES sync_requests(request_id)
                );

                CREATE TABLE IF NOT EXISTS tickets (
                    id TEXT PRIMARY KEY,
                    shift_id TEXT NOT NULL REFERENCES shifts(id),
                    fare_type_id TEXT NOT NULL,
                    price_cents INTEGER NOT NULL CHECK(price_cents >= 0),
                    sold_at_millis INTEGER NOT NULL,
                    first_request_id TEXT NOT NULL REFERENCES sync_requests(request_id)
                );

                CREATE INDEX IF NOT EXISTS idx_shifts_driver ON shifts(driver_id);
                CREATE INDEX IF NOT EXISTS idx_tickets_shift ON tickets(shift_id);
                CREATE INDEX IF NOT EXISTS idx_tickets_fare ON tickets(fare_type_id);
                """
            )
            connection.commit()
        finally:
            if should_close:
                connection.close()

    def health(self) -> bool:
        connection, should_close = self._connection()
        try:
            return connection.execute("SELECT 1").fetchone()[0] == 1
        finally:
            if should_close:
                connection.close()

    def ingest(self, batch: SyncBatchInput) -> Tuple[Dict[str, Any], bool]:
        canonical_json = json.dumps(batch.canonical(), sort_keys=True, separators=(",", ":"))
        payload_hash = hashlib.sha256(canonical_json.encode("utf-8")).hexdigest()
        connection, should_close = self._connection()
        try:
            connection.execute("BEGIN IMMEDIATE")
            existing_request = connection.execute(
                "SELECT payload_hash, response_json FROM sync_requests WHERE request_id = ?",
                (batch.request_id,),
            ).fetchone()
            if existing_request is not None:
                if existing_request["payload_hash"] != payload_hash:
                    raise ContractError("Request ID was already used with different data", 409)
                connection.commit()
                return json.loads(existing_request["response_json"]), True

            response = {
                "contractVersion": CONTRACT_VERSION,
                "requestId": batch.request_id,
                "acknowledgedShiftIds": [shift.id for shift in batch.shifts],
                "acknowledgedTicketIds": [ticket.id for ticket in batch.tickets],
            }
            connection.execute(
                """
                INSERT INTO sync_requests(
                    request_id, contract_version, payload_hash, response_json, received_at_millis
                ) VALUES (?, ?, ?, ?, ?)
                """,
                (
                    batch.request_id,
                    CONTRACT_VERSION,
                    payload_hash,
                    json.dumps(response, sort_keys=True, separators=(",", ":")),
                    int(time.time() * 1000),
                ),
            )
            for shift in batch.shifts:
                self._insert_or_match_shift(connection, shift, batch.request_id)
            for ticket in batch.tickets:
                self._insert_or_match_ticket(connection, ticket, batch.request_id)
            connection.commit()
            return response, False
        except Exception:
            connection.rollback()
            raise
        finally:
            if should_close:
                connection.close()

    def report(self) -> Dict[str, Any]:
        connection, should_close = self._connection()
        try:
            overall = connection.execute(
                """
                SELECT
                    COUNT(DISTINCT s.driver_id) AS drivers,
                    COUNT(DISTINCT s.id) AS shifts,
                    COUNT(t.id) AS tickets,
                    COALESCE(SUM(t.price_cents), 0) AS cash_cents
                FROM shifts s
                LEFT JOIN tickets t ON t.shift_id = s.id
                """
            ).fetchone()
            fares = connection.execute(
                """
                SELECT fare_type_id, COUNT(*) AS tickets, SUM(price_cents) AS cash_cents
                FROM tickets GROUP BY fare_type_id ORDER BY fare_type_id
                """
            ).fetchall()
            drivers = connection.execute(
                """
                SELECT
                    s.driver_id,
                    COUNT(DISTINCT s.id) AS shifts,
                    COUNT(t.id) AS tickets,
                    COALESCE(SUM(t.price_cents), 0) AS cash_cents
                FROM shifts s
                LEFT JOIN tickets t ON t.shift_id = s.id
                GROUP BY s.driver_id ORDER BY s.driver_id
                """
            ).fetchall()
            return {
                "contractVersion": CONTRACT_VERSION,
                "generatedAtMillis": int(time.time() * 1000),
                "overall": {
                    "driverCount": overall["drivers"],
                    "shiftCount": overall["shifts"],
                    "ticketCount": overall["tickets"],
                    "cashTotalCents": overall["cash_cents"],
                },
                "fares": [
                    {
                        "fareTypeId": row["fare_type_id"],
                        "ticketCount": row["tickets"],
                        "cashTotalCents": row["cash_cents"],
                    }
                    for row in fares
                ],
                "drivers": [
                    {
                        "driverId": row["driver_id"],
                        "shiftCount": row["shifts"],
                        "ticketCount": row["tickets"],
                        "cashTotalCents": row["cash_cents"],
                        "shiftIds": [
                            shift["id"]
                            for shift in connection.execute(
                                "SELECT id FROM shifts WHERE driver_id = ? ORDER BY started_at_millis DESC",
                                (row["driver_id"],),
                            ).fetchall()
                        ],
                    }
                    for row in drivers
                ],
                "shifts": self._shift_details(connection),
            }
        finally:
            if should_close:
                connection.close()

    def _shift_details(self, connection: sqlite3.Connection) -> list:
        shifts = connection.execute(
            """
            SELECT id, driver_id, bus_id, route_id, started_at_millis, ended_at_millis
            FROM shifts ORDER BY started_at_millis DESC, id
            """
        ).fetchall()
        result = []
        for shift in shifts:
            tickets = connection.execute(
                """
                SELECT id, shift_id, fare_type_id, price_cents, sold_at_millis
                FROM tickets WHERE shift_id = ? ORDER BY sold_at_millis, id
                """,
                (shift["id"],),
            ).fetchall()
            ticket_values = [
                {
                    "ticketId": ticket["id"],
                    "shiftId": ticket["shift_id"],
                    "fareTypeId": ticket["fare_type_id"],
                    "priceCents": ticket["price_cents"],
                    "soldAtMillis": ticket["sold_at_millis"],
                    "synced": True,
                }
                for ticket in tickets
            ]
            value = {
                "shiftId": shift["id"],
                "driverId": shift["driver_id"],
                "busId": shift["bus_id"],
                "routeId": shift["route_id"],
                "startedAtMillis": shift["started_at_millis"],
                "endedAtMillis": shift["ended_at_millis"],
                "durationMillis": max(
                    0,
                    shift["ended_at_millis"] - shift["started_at_millis"],
                ),
                "ticketCount": len(ticket_values),
                "cashTotalCents": sum(ticket["priceCents"] for ticket in ticket_values),
                "syncStatus": "SYNCED",
                "tickets": ticket_values,
            }
            result.append(value)
        return result

    @staticmethod
    def _insert_or_match_shift(
        connection: sqlite3.Connection,
        shift: ShiftInput,
        request_id: str,
    ) -> None:
        existing = connection.execute("SELECT * FROM shifts WHERE id = ?", (shift.id,)).fetchone()
        expected = (
            shift.driver_id,
            shift.bus_id,
            shift.route_id,
            shift.started_at_millis,
            shift.ended_at_millis,
        )
        if existing is not None:
            actual = tuple(existing[name] for name in (
                "driver_id", "bus_id", "route_id", "started_at_millis", "ended_at_millis"
            ))
            if actual != expected:
                raise ContractError("Shift ID already exists with different data", 409)
            return
        connection.execute(
            """
            INSERT INTO shifts(
                id, driver_id, bus_id, route_id, started_at_millis, ended_at_millis,
                first_request_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            (shift.id, *expected, request_id),
        )

    @staticmethod
    def _insert_or_match_ticket(
        connection: sqlite3.Connection,
        ticket: TicketInput,
        request_id: str,
    ) -> None:
        shift_exists = connection.execute(
            "SELECT 1 FROM shifts WHERE id = ?", (ticket.shift_id,)
        ).fetchone()
        if shift_exists is None:
            raise ContractError("Ticket references an unknown shift", 409)
        existing = connection.execute("SELECT * FROM tickets WHERE id = ?", (ticket.id,)).fetchone()
        expected = (ticket.shift_id, ticket.fare_type_id, ticket.price_cents, ticket.sold_at_millis)
        if existing is not None:
            actual = tuple(existing[name] for name in (
                "shift_id", "fare_type_id", "price_cents", "sold_at_millis"
            ))
            if actual != expected:
                raise ContractError("Ticket ID already exists with different data", 409)
            return
        connection.execute(
            """
            INSERT INTO tickets(
                id, shift_id, fare_type_id, price_cents, sold_at_millis, first_request_id
            ) VALUES (?, ?, ?, ?, ?, ?)
            """,
            (ticket.id, *expected, request_id),
        )

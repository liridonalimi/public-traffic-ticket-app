"""Validation for the BusPay synchronization contract version 1."""

from __future__ import annotations

from dataclasses import dataclass
import re
from typing import Any, Dict, List


CONTRACT_VERSION = 1
MAX_SHIFTS_PER_BATCH = 250
MAX_TICKETS_PER_BATCH = 5_000
REQUEST_ID_PATTERN = re.compile(r"^sync-[a-f0-9]{16}$")


class ContractError(Exception):
    def __init__(self, message: str, status_code: int = 400) -> None:
        super().__init__(message)
        self.message = message
        self.status_code = status_code


@dataclass(frozen=True)
class ShiftInput:
    id: str
    driver_id: str
    bus_id: str
    route_id: str
    started_at_millis: int
    ended_at_millis: int

    def canonical(self) -> Dict[str, Any]:
        return {
            "id": self.id,
            "driverId": self.driver_id,
            "busId": self.bus_id,
            "routeId": self.route_id,
            "startedAtMillis": self.started_at_millis,
            "endedAtMillis": self.ended_at_millis,
        }


@dataclass(frozen=True)
class TicketInput:
    id: str
    shift_id: str
    fare_type_id: str
    price_cents: int
    sold_at_millis: int

    def canonical(self) -> Dict[str, Any]:
        return {
            "id": self.id,
            "shiftId": self.shift_id,
            "fareTypeId": self.fare_type_id,
            "priceCents": self.price_cents,
            "soldAtMillis": self.sold_at_millis,
        }


@dataclass(frozen=True)
class SyncBatchInput:
    request_id: str
    sent_at_millis: int
    shifts: List[ShiftInput]
    tickets: List[TicketInput]

    def canonical(self) -> Dict[str, Any]:
        return {
            "contractVersion": CONTRACT_VERSION,
            "requestId": self.request_id,
            "sentAtMillis": self.sent_at_millis,
            "shifts": [shift.canonical() for shift in self.shifts],
            "tickets": [ticket.canonical() for ticket in self.tickets],
        }


def parse_sync_batch(payload: Any) -> SyncBatchInput:
    if not isinstance(payload, dict):
        raise ContractError("Request body must be a JSON object")
    if payload.get("contractVersion") != CONTRACT_VERSION:
        raise ContractError("Unsupported contract version")

    request_id = _required_string(payload, "requestId", maximum=80)
    if not REQUEST_ID_PATTERN.fullmatch(request_id):
        raise ContractError("Invalid request ID")
    sent_at_millis = _required_integer(payload, "sentAtMillis", minimum=0)
    raw_shifts = _required_list(payload, "shifts", MAX_SHIFTS_PER_BATCH)
    raw_tickets = _required_list(payload, "tickets", MAX_TICKETS_PER_BATCH)
    if not raw_shifts and not raw_tickets:
        raise ContractError("Sync batch must contain at least one record")

    shifts = [_parse_shift(value) for value in raw_shifts]
    tickets = [_parse_ticket(value) for value in raw_tickets]
    _require_unique([shift.id for shift in shifts], "shift")
    _require_unique([ticket.id for ticket in tickets], "ticket")
    return SyncBatchInput(
        request_id=request_id,
        sent_at_millis=sent_at_millis,
        shifts=shifts,
        tickets=tickets,
    )


def _parse_shift(value: Any) -> ShiftInput:
    if not isinstance(value, dict):
        raise ContractError("Every shift must be a JSON object")
    started_at = _required_integer(value, "startedAtMillis", minimum=0)
    ended_at = _required_integer(value, "endedAtMillis", minimum=started_at)
    return ShiftInput(
        id=_required_string(value, "id"),
        driver_id=_required_string(value, "driverId"),
        bus_id=_required_string(value, "busId"),
        route_id=_required_string(value, "routeId"),
        started_at_millis=started_at,
        ended_at_millis=ended_at,
    )


def _parse_ticket(value: Any) -> TicketInput:
    if not isinstance(value, dict):
        raise ContractError("Every ticket must be a JSON object")
    return TicketInput(
        id=_required_string(value, "id"),
        shift_id=_required_string(value, "shiftId"),
        fare_type_id=_required_string(value, "fareTypeId"),
        price_cents=_required_integer(value, "priceCents", minimum=0, maximum=100_000),
        sold_at_millis=_required_integer(value, "soldAtMillis", minimum=0),
    )


def _required_string(value: Dict[str, Any], name: str, maximum: int = 200) -> str:
    result = value.get(name)
    if not isinstance(result, str) or not result or len(result) > maximum:
        raise ContractError(f"Field {name} must be a nonempty string")
    if any(ord(character) < 0x20 for character in result):
        raise ContractError(f"Field {name} contains invalid control characters")
    return result


def _required_integer(
    value: Dict[str, Any],
    name: str,
    minimum: int,
    maximum: int = 9_223_372_036_854_775_807,
) -> int:
    result = value.get(name)
    if isinstance(result, bool) or not isinstance(result, int) or not minimum <= result <= maximum:
        raise ContractError(f"Field {name} must be an integer between {minimum} and {maximum}")
    return result


def _required_list(value: Dict[str, Any], name: str, maximum: int) -> List[Any]:
    result = value.get(name)
    if not isinstance(result, list):
        raise ContractError(f"Field {name} must be an array")
    if len(result) > maximum:
        raise ContractError(f"Field {name} exceeds the batch limit")
    return result


def _require_unique(values: List[str], record_name: str) -> None:
    if len(values) != len(set(values)):
        raise ContractError(f"Duplicate {record_name} ID in sync batch")

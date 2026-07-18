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


@dataclass(frozen=True)
class CatalogDriverInput:
    id: str
    name: str


@dataclass(frozen=True)
class CatalogBusInput:
    id: str
    plate_number: str


@dataclass(frozen=True)
class CatalogRouteInput:
    id: str
    name: str


@dataclass(frozen=True)
class CatalogStopInput:
    id: str
    route_id: str
    name: str
    latitude: float
    longitude: float
    order: int


@dataclass(frozen=True)
class CatalogFareInput:
    id: str
    name: str
    price_cents: int
    eligibility: str | None


@dataclass(frozen=True)
class CatalogInput:
    expected_revision: int
    drivers: List[CatalogDriverInput]
    buses: List[CatalogBusInput]
    routes: List[CatalogRouteInput]
    stops: List[CatalogStopInput]
    fares: List[CatalogFareInput]


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


def parse_catalog(payload: Any) -> CatalogInput:
    if not isinstance(payload, dict):
        raise ContractError("Request body must be a JSON object")
    if payload.get("contractVersion") != CONTRACT_VERSION:
        raise ContractError("Unsupported contract version")

    drivers = [_parse_catalog_driver(value) for value in _required_nonempty_list(payload, "drivers", 500)]
    buses = [_parse_catalog_bus(value) for value in _required_nonempty_list(payload, "buses", 500)]
    routes = [_parse_catalog_route(value) for value in _required_nonempty_list(payload, "routes", 500)]
    stops = [_parse_catalog_stop(value) for value in _required_nonempty_list(payload, "stops", 5_000)]
    fares = [_parse_catalog_fare(value) for value in _required_nonempty_list(payload, "fares", 500)]

    for values, name in (
        (drivers, "driver"),
        (buses, "bus"),
        (routes, "route"),
        (stops, "stop"),
        (fares, "fare"),
    ):
        _require_unique([value.id for value in values], name)

    route_ids = {route.id for route in routes}
    if any(stop.route_id not in route_ids for stop in stops):
        raise ContractError("Every stop must reference a route in this catalog")
    route_orders = [(stop.route_id, stop.order) for stop in stops]
    if len(route_orders) != len(set(route_orders)):
        raise ContractError("Stop order must be unique within each route")
    routes_with_stops = {stop.route_id for stop in stops}
    if route_ids != routes_with_stops:
        raise ContractError("Every route must contain at least one stop")

    return CatalogInput(
        expected_revision=_required_integer(payload, "expectedRevision", minimum=0),
        drivers=drivers,
        buses=buses,
        routes=routes,
        stops=stops,
        fares=fares,
    )


def _parse_catalog_driver(value: Any) -> CatalogDriverInput:
    _require_object(value, "driver")
    return CatalogDriverInput(
        id=_required_string(value, "id", maximum=80),
        name=_required_string(value, "name", maximum=160),
    )


def _parse_catalog_bus(value: Any) -> CatalogBusInput:
    _require_object(value, "bus")
    return CatalogBusInput(
        id=_required_string(value, "id", maximum=80),
        plate_number=_required_string(value, "plateNumber", maximum=40),
    )


def _parse_catalog_route(value: Any) -> CatalogRouteInput:
    _require_object(value, "route")
    return CatalogRouteInput(
        id=_required_string(value, "id", maximum=80),
        name=_required_string(value, "name", maximum=200),
    )


def _parse_catalog_stop(value: Any) -> CatalogStopInput:
    _require_object(value, "stop")
    latitude = _required_number(value, "latitude", -90.0, 90.0)
    longitude = _required_number(value, "longitude", -180.0, 180.0)
    return CatalogStopInput(
        id=_required_string(value, "id", maximum=80),
        route_id=_required_string(value, "routeId", maximum=80),
        name=_required_string(value, "name", maximum=200),
        latitude=latitude,
        longitude=longitude,
        order=_required_integer(value, "order", minimum=1, maximum=10_000),
    )


def _parse_catalog_fare(value: Any) -> CatalogFareInput:
    _require_object(value, "fare")
    eligibility = value.get("eligibility")
    if eligibility is not None:
        if not isinstance(eligibility, str) or len(eligibility) > 300:
            raise ContractError("Field eligibility must be a string or null")
        eligibility = eligibility.strip() or None
    return CatalogFareInput(
        id=_required_string(value, "id", maximum=80),
        name=_required_string(value, "name", maximum=160),
        price_cents=_required_integer(value, "priceCents", minimum=0, maximum=100_000),
        eligibility=eligibility,
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


def _required_nonempty_list(value: Dict[str, Any], name: str, maximum: int) -> List[Any]:
    result = _required_list(value, name, maximum)
    if not result:
        raise ContractError(f"Field {name} must contain at least one record")
    return result


def _require_object(value: Any, record_name: str) -> None:
    if not isinstance(value, dict):
        raise ContractError(f"Every {record_name} must be a JSON object")


def _required_number(value: Dict[str, Any], name: str, minimum: float, maximum: float) -> float:
    result = value.get(name)
    if isinstance(result, bool) or not isinstance(result, (int, float)):
        raise ContractError(f"Field {name} must be a number")
    number = float(result)
    if not minimum <= number <= maximum:
        raise ContractError(f"Field {name} must be between {minimum} and {maximum}")
    return number


def _require_unique(values: List[str], record_name: str) -> None:
    if len(values) != len(set(values)):
        raise ContractError(f"Duplicate {record_name} ID in sync batch")

"""Validation for the BusPay synchronization contract version 1."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import date
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
    expected_cash_cents: int | None
    declared_cash_cents: int | None
    reconciled_at_millis: int | None
    scheduled_trip_id: str | None
    assignment_id: str | None

    def canonical(self) -> Dict[str, Any]:
        return {
            "id": self.id,
            "driverId": self.driver_id,
            "busId": self.bus_id,
            "routeId": self.route_id,
            "startedAtMillis": self.started_at_millis,
            "endedAtMillis": self.ended_at_millis,
            "expectedCashCents": self.expected_cash_cents,
            "declaredCashCents": self.declared_cash_cents,
            "reconciledAtMillis": self.reconciled_at_millis,
            "scheduledTripId": self.scheduled_trip_id,
            "assignmentId": self.assignment_id,
        }


@dataclass(frozen=True)
class TicketInput:
    id: str
    shift_id: str
    fare_type_id: str
    price_cents: int
    sold_at_millis: int
    fare_policy_revision: int | None
    origin_stop_id: str | None
    destination_stop_id: str | None
    zone_count: int | None
    off_peak_applied: bool | None
    transfer_valid_until_millis: int | None

    def canonical(self) -> Dict[str, Any]:
        return {
            "id": self.id,
            "shiftId": self.shift_id,
            "fareTypeId": self.fare_type_id,
            "priceCents": self.price_cents,
            "soldAtMillis": self.sold_at_millis,
            "farePolicyRevision": self.fare_policy_revision,
            "originStopId": self.origin_stop_id,
            "destinationStopId": self.destination_stop_id,
            "zoneCount": self.zone_count,
            "offPeakApplied": self.off_peak_applied,
            "transferValidUntilMillis": self.transfer_valid_until_millis,
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
    zone_id: str


@dataclass(frozen=True)
class CatalogFareInput:
    id: str
    name: str
    price_cents: int
    eligibility: str | None
    additional_zone_cents: int
    off_peak_discount_cents: int
    off_peak_start_minutes: int | None
    off_peak_end_minutes: int | None
    transfer_window_minutes: int
    route_id: str | None


@dataclass(frozen=True)
class CatalogServiceCalendarInput:
    id: str
    name: str
    start_date: str
    end_date: str
    active_weekdays: List[int]


@dataclass(frozen=True)
class CatalogStopTimeInput:
    stop_id: str
    arrival_minutes: int
    departure_minutes: int


@dataclass(frozen=True)
class CatalogTripInput:
    id: str
    route_id: str
    service_calendar_id: str
    departure_minutes: int
    direction: str
    stop_times: List[CatalogStopTimeInput]

    @property
    def end_minutes(self) -> int:
        return self.stop_times[-1].departure_minutes


@dataclass(frozen=True)
class CatalogAssignmentInput:
    id: str
    trip_id: str
    service_date: str
    driver_id: str
    bus_id: str


@dataclass(frozen=True)
class CatalogInput:
    expected_revision: int
    drivers: List[CatalogDriverInput]
    buses: List[CatalogBusInput]
    routes: List[CatalogRouteInput]
    stops: List[CatalogStopInput]
    fares: List[CatalogFareInput]
    service_calendars: List[CatalogServiceCalendarInput]
    scheduled_trips: List[CatalogTripInput]
    trip_assignments: List[CatalogAssignmentInput]


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
    calendars = [_parse_catalog_calendar(value) for value in _optional_list(payload, "serviceCalendars", 500)]
    trips = [_parse_catalog_trip(value) for value in _optional_list(payload, "scheduledTrips", 5_000)]
    assignments = [_parse_catalog_assignment(value) for value in _optional_list(payload, "tripAssignments", 20_000)]

    for values, name in (
        (drivers, "driver"),
        (buses, "bus"),
        (routes, "route"),
        (stops, "stop"),
        (fares, "fare"),
        (calendars, "service calendar"),
        (trips, "scheduled trip"),
        (assignments, "trip assignment"),
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
    if any(fare.route_id is not None and fare.route_id not in route_ids for fare in fares):
        raise ContractError("Every route-specific fare must reference a route in this catalog")
    if any(
        not any(fare.route_id is None or fare.route_id == route_id for fare in fares)
        for route_id in route_ids
    ):
        raise ContractError("Every route must have at least one applicable fare")

    calendar_by_id = {value.id: value for value in calendars}
    trip_by_id = {value.id: value for value in trips}
    stops_by_route = {
        route_id: sorted((stop for stop in stops if stop.route_id == route_id), key=lambda stop: stop.order)
        for route_id in route_ids
    }
    for trip in trips:
        if trip.route_id not in route_ids or trip.service_calendar_id not in calendar_by_id:
            raise ContractError("Every scheduled trip must reference a route and service calendar")
        expected_stop_ids = [stop.id for stop in stops_by_route[trip.route_id]]
        if [stop_time.stop_id for stop_time in trip.stop_times] != expected_stop_ids:
            raise ContractError("Scheduled stop times must follow every stop on the selected route")
    driver_ids = {value.id for value in drivers}
    bus_ids = {value.id for value in buses}
    for assignment in assignments:
        trip = trip_by_id.get(assignment.trip_id)
        if trip is None or assignment.driver_id not in driver_ids or assignment.bus_id not in bus_ids:
            raise ContractError("Every assignment must reference a trip, driver, and bus")
        calendar = calendar_by_id[trip.service_calendar_id]
        service_day = date.fromisoformat(assignment.service_date)
        if not date.fromisoformat(calendar.start_date) <= service_day <= date.fromisoformat(calendar.end_date):
            raise ContractError("Assignment date is outside the service calendar")
        if service_day.isoweekday() not in calendar.active_weekdays:
            raise ContractError("Assignment date is not active in the service calendar")
    for index, first in enumerate(assignments):
        first_trip = trip_by_id[first.trip_id]
        for second in assignments[index + 1:]:
            if first.service_date != second.service_date:
                continue
            second_trip = trip_by_id[second.trip_id]
            overlaps = first_trip.departure_minutes < second_trip.end_minutes and second_trip.departure_minutes < first_trip.end_minutes
            if overlaps and (first.driver_id == second.driver_id or first.bus_id == second.bus_id):
                raise ContractError("Driver or bus assignments overlap")

    return CatalogInput(
        expected_revision=_required_integer(payload, "expectedRevision", minimum=0),
        drivers=drivers,
        buses=buses,
        routes=routes,
        stops=stops,
        fares=fares,
        service_calendars=calendars,
        scheduled_trips=trips,
        trip_assignments=assignments,
    )


def _parse_catalog_calendar(value: Any) -> CatalogServiceCalendarInput:
    _require_object(value, "service calendar")
    start = _required_date(value, "startDate")
    end = _required_date(value, "endDate")
    if end < start:
        raise ContractError("Service calendar end date must not precede its start date")
    weekdays = _required_list(value, "activeWeekdays", 7)
    if not weekdays or any(isinstance(day, bool) or not isinstance(day, int) or day not in range(1, 8) for day in weekdays):
        raise ContractError("Active weekdays must contain ISO weekday numbers 1 through 7")
    if len(weekdays) != len(set(weekdays)):
        raise ContractError("Active weekdays must be unique")
    return CatalogServiceCalendarInput(
        id=_required_string(value, "id", maximum=80),
        name=_required_string(value, "name", maximum=160),
        start_date=start.isoformat(),
        end_date=end.isoformat(),
        active_weekdays=weekdays,
    )


def _parse_catalog_trip(value: Any) -> CatalogTripInput:
    _require_object(value, "scheduled trip")
    raw_stop_times = _required_nonempty_list(value, "stopTimes", 500)
    stop_times = [_parse_catalog_stop_time(item) for item in raw_stop_times]
    _require_unique([item.stop_id for item in stop_times], "scheduled stop")
    previous = -1
    for stop_time in stop_times:
        if stop_time.arrival_minutes < previous or stop_time.departure_minutes < stop_time.arrival_minutes:
            raise ContractError("Scheduled stop times must be ordered")
        previous = stop_time.departure_minutes
    direction = _required_string(value, "direction", maximum=20)
    if direction not in ("OUTBOUND", "INBOUND"):
        raise ContractError("Trip direction must be OUTBOUND or INBOUND")
    departure = _required_integer(value, "departureMinutes", minimum=0, maximum=1_439)
    if stop_times[0].arrival_minutes < departure:
        raise ContractError("First stop time cannot precede trip departure")
    return CatalogTripInput(
        id=_required_string(value, "id", maximum=80),
        route_id=_required_string(value, "routeId", maximum=80),
        service_calendar_id=_required_string(value, "serviceCalendarId", maximum=80),
        departure_minutes=departure,
        direction=direction,
        stop_times=stop_times,
    )


def _parse_catalog_stop_time(value: Any) -> CatalogStopTimeInput:
    _require_object(value, "scheduled stop time")
    return CatalogStopTimeInput(
        stop_id=_required_string(value, "stopId", maximum=80),
        arrival_minutes=_required_integer(value, "arrivalMinutes", minimum=0, maximum=2_879),
        departure_minutes=_required_integer(value, "departureMinutes", minimum=0, maximum=2_879),
    )


def _parse_catalog_assignment(value: Any) -> CatalogAssignmentInput:
    _require_object(value, "trip assignment")
    return CatalogAssignmentInput(
        id=_required_string(value, "id", maximum=80),
        trip_id=_required_string(value, "tripId", maximum=80),
        service_date=_required_date(value, "serviceDate").isoformat(),
        driver_id=_required_string(value, "driverId", maximum=80),
        bus_id=_required_string(value, "busId", maximum=80),
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
        zone_id=_optional_string(value, "zoneId", maximum=40) or "1",
    )


def _parse_catalog_fare(value: Any) -> CatalogFareInput:
    _require_object(value, "fare")
    eligibility = value.get("eligibility")
    if eligibility is not None:
        if not isinstance(eligibility, str) or len(eligibility) > 300:
            raise ContractError("Field eligibility must be a string or null")
        eligibility = eligibility.strip() or None
    off_peak_start = _optional_integer(value, "offPeakStartMinutes", minimum=0, maximum=1_439)
    off_peak_end = _optional_integer(value, "offPeakEndMinutes", minimum=0, maximum=1_439)
    if (off_peak_start is None) != (off_peak_end is None):
        raise ContractError("Off-peak start and end times must both be set or both be empty")
    if off_peak_start is not None and off_peak_start == off_peak_end:
        raise ContractError("Off-peak start and end times must be different")
    return CatalogFareInput(
        id=_required_string(value, "id", maximum=80),
        name=_required_string(value, "name", maximum=160),
        price_cents=_required_integer(value, "priceCents", minimum=0, maximum=100_000),
        eligibility=eligibility,
        additional_zone_cents=_optional_integer(value, "additionalZoneCents", minimum=0, maximum=100_000) or 0,
        off_peak_discount_cents=_optional_integer(value, "offPeakDiscountCents", minimum=0, maximum=100_000) or 0,
        off_peak_start_minutes=off_peak_start,
        off_peak_end_minutes=off_peak_end,
        transfer_window_minutes=_optional_integer(value, "transferWindowMinutes", minimum=0, maximum=1_440) or 0,
        route_id=_optional_string(value, "routeId", maximum=80),
    )


def _parse_shift(value: Any) -> ShiftInput:
    if not isinstance(value, dict):
        raise ContractError("Every shift must be a JSON object")
    started_at = _required_integer(value, "startedAtMillis", minimum=0)
    ended_at = _required_integer(value, "endedAtMillis", minimum=started_at)
    expected_cash = _optional_integer(value, "expectedCashCents", minimum=0, maximum=100_000_000)
    declared_cash = _optional_integer(value, "declaredCashCents", minimum=0, maximum=100_000_000)
    reconciled_at = _optional_integer(value, "reconciledAtMillis", minimum=ended_at)
    reconciliation_values = (expected_cash, declared_cash, reconciled_at)
    if any(item is None for item in reconciliation_values) and any(
        item is not None for item in reconciliation_values
    ):
        raise ContractError("Shift cash reconciliation fields must be supplied together")
    return ShiftInput(
        id=_required_string(value, "id"),
        driver_id=_required_string(value, "driverId"),
        bus_id=_required_string(value, "busId"),
        route_id=_required_string(value, "routeId"),
        started_at_millis=started_at,
        ended_at_millis=ended_at,
        expected_cash_cents=expected_cash,
        declared_cash_cents=declared_cash,
        reconciled_at_millis=reconciled_at,
        scheduled_trip_id=_optional_string(value, "scheduledTripId"),
        assignment_id=_optional_string(value, "assignmentId"),
    )


def _parse_ticket(value: Any) -> TicketInput:
    if not isinstance(value, dict):
        raise ContractError("Every ticket must be a JSON object")
    sold_at = _required_integer(value, "soldAtMillis", minimum=0)
    revision = _optional_integer(value, "farePolicyRevision", minimum=1)
    origin = _optional_string(value, "originStopId", maximum=80)
    destination = _optional_string(value, "destinationStopId", maximum=80)
    zone_count = _optional_integer(value, "zoneCount", minimum=1, maximum=1_000)
    off_peak = _optional_boolean(value, "offPeakApplied")
    transfer_valid_until = _optional_integer(
        value, "transferValidUntilMillis", minimum=sold_at
    )
    snapshot = (revision, origin, destination, zone_count, off_peak)
    if any(item is None for item in snapshot) and any(item is not None for item in snapshot):
        raise ContractError("Ticket fare policy snapshot fields must be supplied together")
    if transfer_valid_until is not None and revision is None:
        raise ContractError("Transfer validity requires a ticket fare policy snapshot")
    return TicketInput(
        id=_required_string(value, "id"),
        shift_id=_required_string(value, "shiftId"),
        fare_type_id=_required_string(value, "fareTypeId"),
        price_cents=_required_integer(value, "priceCents", minimum=0, maximum=100_000),
        sold_at_millis=sold_at,
        fare_policy_revision=revision,
        origin_stop_id=origin,
        destination_stop_id=destination,
        zone_count=zone_count,
        off_peak_applied=off_peak,
        transfer_valid_until_millis=transfer_valid_until,
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


def _optional_string(value: Dict[str, Any], name: str, maximum: int = 200) -> str | None:
    result = value.get(name)
    if result is None:
        return None
    if not isinstance(result, str) or not result or len(result) > maximum:
        raise ContractError(f"Field {name} must be null or a nonempty string")
    return result


def _optional_boolean(value: Dict[str, Any], name: str) -> bool | None:
    result = value.get(name)
    if result is None:
        return None
    if not isinstance(result, bool):
        raise ContractError(f"Field {name} must be a boolean or null")
    return result


def _required_date(value: Dict[str, Any], name: str) -> date:
    raw = _required_string(value, name, maximum=10)
    try:
        return date.fromisoformat(raw)
    except ValueError as error:
        raise ContractError(f"Field {name} must be an ISO date") from error


def _optional_integer(
    value: Dict[str, Any],
    name: str,
    minimum: int,
    maximum: int = 9_223_372_036_854_775_807,
) -> int | None:
    result = value.get(name)
    if result is None:
        return None
    if isinstance(result, bool) or not isinstance(result, int) or not minimum <= result <= maximum:
        raise ContractError(f"Field {name} must be null or an integer between {minimum} and {maximum}")
    return result


def _required_list(value: Dict[str, Any], name: str, maximum: int) -> List[Any]:
    result = value.get(name)
    if not isinstance(result, list):
        raise ContractError(f"Field {name} must be an array")
    if len(result) > maximum:
        raise ContractError(f"Field {name} exceeds the batch limit")
    return result


def _optional_list(value: Dict[str, Any], name: str, maximum: int) -> List[Any]:
    if name not in value:
        return []
    return _required_list(value, name, maximum)


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

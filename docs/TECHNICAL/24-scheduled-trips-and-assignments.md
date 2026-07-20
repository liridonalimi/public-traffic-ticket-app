# Module 24 — Scheduled work technical design

## Model and persistence

`ManagedCatalog` now carries `ServiceCalendar`, `ScheduledTrip`, ordered `ScheduledStopTime`, and `TripAssignment` records. Android JSON decoding remains backward compatible because schedule arrays are optional and default to empty. The complete catalog is persisted as one offline SharedPreferences snapshot.

Server SQLite adds normalized calendar, trip, stop-time, and assignment tables with foreign keys and cascade rules. Catalog replacement deletes and inserts the full graph inside the existing immediate transaction and increments one revision only after all validation succeeds.

## Validation invariants

- ISO calendar dates are ordered and active weekdays use unique values 1–7.
- Every trip references a published route/calendar and supplies stop times for every route stop in exact order.
- Arrival/departure values are monotonic and bounded.
- Every assignment references a published trip, driver, and bus and uses an active service date.
- On a service date, intervals may not overlap when either driver or bus is reused.

Android repeats structural and conflict checks before accepting a downloaded catalog. Server validation remains authoritative for publication.

## Administrator input adapter

The catalog UI presents named weekday checkboxes and `HH:mm` controls, then adapts them to the unchanged v1 model. Weekday selections become ISO integers, while departure and stop clocks become minute-after-midnight integers. Each stop is rendered from the selected route so the form cannot rely on an ambiguous comma-separated list. A per-stop next-day flag adds 1,440 minutes for overnight service. Descriptive trip options include the time range, route, direction, and ID; no API or persistence migration is required for this UI polish.

## Shift compatibility

Version 1 shift JSON gains nullable `scheduledTripId` and `assignmentId`. SQLite migration adds nullable columns to existing databases. Null means an ad-hoc or legacy operation, preserving existing clients and history. Idempotency hashing includes both fields, and report details expose them.

## Verification

Automated coverage checks catalog parsing, offline duty projection, overlap rejection, schedule publication/persistence, nullable scheduled shift serialization, legacy compatibility, server migrations, transactional ingestion, Android build, and lint.

Independent web validation completed on July 20, 2026 additionally verified empty-weekday rejection, route-driven stop-control generation, client-side chronological-order rejection, and the `+1 day` overnight conversion. Module 24 is complete.

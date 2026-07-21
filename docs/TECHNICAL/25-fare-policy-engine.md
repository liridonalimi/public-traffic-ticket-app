# Module 25 — Fare Policy Engine (Technical)

## Model

`FareType` remains the passenger category and adds an optional route scope, an extra-zone amount, an optional off-peak interval and discount, and transfer-validity minutes. `Stop.zoneId` supplies the zone sequence. Missing new fields deserialize to zero/null defaults for backward compatibility.

`calculateFareQuote` is a pure deterministic function. It counts distinct zones between the current and selected destination stop, applies the extra-zone charge, evaluates normal or overnight off-peak intervals, floors the result at zero, and calculates transfer expiry.

## Persistence and contract

SQLite migrations add policy fields without replacing existing databases. Catalog publication remains atomic and validates route references and policy bounds.

New managed-catalog ticket sync records include:

- `farePolicyRevision`
- `originStopId` and `destinationStopId`
- `zoneCount`
- `offPeakApplied`
- optional `transferValidUntilMillis`
- the existing immutable `fareTypeId` and `priceCents`

The five core snapshot fields are all-or-none in contract v1. Legacy tickets without a snapshot remain accepted. Server reports return the stored snapshot, never a recalculation against the latest catalog.

## Boundaries

The driver explicitly selects a fare category, so the engine evaluates one policy and has no ambiguous priority ordering. Transfer validation and consumption are outside this module and belong to Module 27.

## Verification

- Android unit tests cover legacy fixed fares, cross-zone pricing, off-peak discounts, transfer expiry, overnight intervals, and non-negative price flooring.
- Server tests cover contract validation, persistence/restart, catalog revisioning, and policy-snapshot reporting.
- Full Android test, assembly, lint, and Python server suites are required before handoff.

## Validation evidence

Device and report-reader acceptance testing completed on July 21, 2026.

- Policy revision 17 produced EUR 1.20 for a two-zone journey (`100 + 40 - 20` cents) and EUR 0.80 for a one-zone off-peak journey (`100 - 20` cents).
- Two synchronized shifts contained five policy-priced tickets: EUR 3.20 and EUR 2.00, matching the report aggregate of EUR 5.20.
- Synced ticket snapshots retained fare ID, applied price, policy revision, origin and destination stops, zone count, off-peak status, and transfer-expiry time.
- Both cash handovers were reported as `MATCHED`.

This completes Module 25 acceptance.

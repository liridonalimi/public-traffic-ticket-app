# Technical: Admin Reporting Data Contract

## Scope

This module defines contract version 1 for administrative reporting, implements deterministic aggregation from local closed shifts and tickets, and renders an in-app preview for acceptance. It does not implement the production reporting service or web dashboard.

## Contract types

`AdminReport` contains:

- `contractVersion`
- `generatedAtMillis`
- echoed `filter`
- overall `totals`
- `drivers`
- detailed `shifts`
- `dataQuality`

`ShiftReport` carries identifying and operational dimensions, a complete list of selected `TicketReport` records, derived ticket/cash/fare totals, duration, and `ReportingSyncStatus`.

`DriverReport` aggregates shift count, ticket count, cash total, and contributing shift IDs. `AdminReportTotals` aggregates the same report-wide values plus fare and sync-status breakdowns.

## Filter contract

`AdminReportFilter` supports nullable:

- `fromStartedAtMillis` (inclusive)
- `toStartedAtMillis` (inclusive)
- `driverId`
- `busId`
- `routeId`
- `fareTypeId`

Date and entity filters select shifts. A fare filter selects ticket detail within those shifts and excludes shifts with no matching tickets. Totals and driver summaries are always recomputed from the filtered shift reports.

## Suggested wire representation

A future endpoint can expose the contract as:

```http
GET /v1/admin/reports/shifts?fromStartedAtMillis=...&toStartedAtMillis=...&driverId=...
```

Representative response shape:

```json
{
  "contractVersion": 1,
  "generatedAtMillis": 1784020000000,
  "filter": { "driverId": "driver-001" },
  "totals": {
    "driverCount": 1,
    "shiftCount": 2,
    "ticketCount": 125,
    "cashTotalCents": 5350,
    "syncedShiftCount": 2,
    "partiallySyncedShiftCount": 0,
    "pendingShiftCount": 0,
    "fareTypeSummaries": []
  },
  "drivers": [],
  "shifts": [],
  "dataQuality": {
    "unmatchedTicketCount": 0,
    "unmatchedTicketCashCents": 0,
    "unknownDriverShiftCount": 0,
    "unknownBusShiftCount": 0,
    "unknownRouteShiftCount": 0
  }
}
```

Money is represented exclusively as integer euro cents. Times and durations use integer milliseconds. IDs remain opaque strings.

## Aggregation rules

1. Only records with `endedAtMillis` participate as closed shifts.
2. Tickets join to shifts by exact `shiftId`.
3. Shift ticket count and cash are calculated from joined, filtered tickets.
4. Fare totals group by stored `fareTypeId` and sum stored charged prices.
5. Driver totals sum the filtered shift reports belonging to `driverId`.
6. Overall totals sum the same shift reports; they are not calculated independently.
7. Duration is `max(endedAtMillis - startedAtMillis, 0)`.
8. Unknown catalog references remain visible with fallback labels.

## Sync-status derivation

- `SYNCED`: shift is synced and every selected ticket is synced.
- `PARTIALLY_SYNCED`: shift or any selected ticket is synced, but the complete selection is not.
- `PENDING`: shift and selected tickets are unsynced.

For a fare-filtered report, status describes the selected ticket subset. The unfiltered report describes the complete shift.

## Data-quality boundary

Tickets whose `shiftId` has no closed-shift record are excluded from attributed totals and counted separately with their cash value. This is especially important for legacy tickets created before Module 09.

Unknown driver, bus, and route references are included in shift reports with fallback names and counted independently. They are never dropped silently.

Active-shift tickets are excluded before report generation in the view model, so they are neither reported as closed-shift revenue nor misclassified as legacy unmatched tickets.

## Local preview

`OfflineFirstRepository` exposes read-only closed-shift and ticket snapshots. `DriverShiftViewModel` builds the report from those snapshots and refreshes it after shift closure, synchronization, or an explicit refresh action.

`AdminReportScreen` displays overall totals, fare totals, data quality, driver aggregates, shift details, and individual ticket records. It is a pilot presentation without role authorization.

## Verification

`AdminReportingTest` covers:

- report-wide and driver aggregation
- fare and cash reconciliation
- full, partial, and pending sync states
- inclusive date and driver filtering
- fare filtering
- legacy unmatched-ticket exclusion
- unknown driver/bus/route visibility

The complete unit-test suite, debug assembly, and Android lint verify integration with Modules 01–09.

## Production requirements

- Authenticate administrators and enforce reporting roles.
- Implement the versioned endpoint and JSON serialization.
- Query the production database rather than Android local storage.
- Add pagination for shift and ticket detail.
- Add timezone/reporting-day rules.
- Add exports, charts, audit logging, retention, and privacy controls.
- Preserve version 1 compatibility or introduce an explicitly versioned successor.

## Next product phase

Implement the authenticated production backend, real sync adapter, and administrative dashboard using this contract.

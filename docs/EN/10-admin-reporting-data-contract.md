# Admin Reporting Data Contract

## Purpose

Module 10 defines a versioned reporting contract for a future administrative dashboard and provides an in-app preview built from local closed shifts and their linked tickets. The same detailed records drive shift, driver, fare, cash, and synchronization totals so summary values remain reconcilable with individual tickets.

## What the admin preview shows

- Number of drivers represented in the report.
- Closed-shift count.
- Ticket count and cash total.
- Totals by standard, student, senior, and child fare.
- Synchronized, partially synchronized, and pending shift counts.
- Per-driver shift, ticket, and cash totals.
- Every closed shift with driver, bus, route, start/end time, and duration.
- Ticket records belonging to each shift, including fare, price, and sync state.
- Data-quality warnings for legacy tickets without closed-shift metadata and unknown references.

The preview is opened from **Open Admin Report Preview** on the driver console. **Refresh Report** rebuilds it from current local storage.

## Reporting relationships

The contract uses these primary relationships:

- One driver can have many closed shifts.
- One closed shift belongs to one driver, bus, and route.
- One closed shift can contain many tickets.
- One ticket belongs to one shift and one fare type.

These relationships allow an administrator to start with overall totals, inspect a driver's totals, open an individual shift, and reconcile that shift against its ticket records.

## Shift synchronization status

- **SYNCED:** the shift and every selected ticket are acknowledged.
- **PARTIALLY SYNCED:** the shift or at least one selected ticket is acknowledged, but the complete set is not.
- **PENDING:** neither the shift nor its selected tickets are acknowledged.

Status is derived rather than copied into a separate report field, preventing the report from drifting away from the operational records.

## Filters defined by the contract

The reporting model supports:

- inclusive shift-start date/time range
- driver ID
- bus ID
- route ID
- fare-type ID

A fare filter retains only matching ticket detail and excludes shifts with no matching tickets. The current pilot preview shows all locally stored closed shifts; production dashboard controls will populate these filter fields.

## Data-quality rules

Tickets from versions before Module 09 may not have matching closed-shift metadata. They are excluded from attributed driver/shift totals and shown separately as unmatched ticket count and cash. This avoids assigning historical cash to an incorrect driver.

Unknown driver, bus, or route references remain visible with clear fallback labels and separate quality counters.

## Double-test checklist

1. Close at least two shifts, preferably with different drivers and fare types.
2. Leave one shift pending and synchronize another through the Module 09 demo workflow.
3. Tap **Open Admin Report Preview**.
4. Confirm overall closed-shift, ticket, and cash totals match the stored shifts.
5. Confirm the fare totals equal the individual shift fare totals.
6. Confirm each driver shows the correct shift, ticket, and cash totals.
7. Inspect each shift and verify driver, bus, route, start/end time, duration, and sync status.
8. Confirm the ticket records under each shift match its ticket count and cash total.
9. If legacy tickets exist, confirm they appear under **Data quality** and are not added to attributed totals.
10. Return to the console, create/close another shift, reopen the preview, and tap **Refresh Report**.
11. Confirm the new shift and tickets appear without changing earlier shift details.

## Current limitations

- The preview reads local Android storage rather than a production reporting API.
- There is no administrator authentication or role enforcement in the pilot.
- Filter controls are defined by the contract but not exposed in the preview UI.
- There is no web dashboard, export, pagination, charting, or server database yet.
- Only closed shifts stored since Module 09 can be attributed fully.

## Next product phase

Connect the production sync adapter and reporting endpoint to an authenticated administrative dashboard that consumes contract version 1.

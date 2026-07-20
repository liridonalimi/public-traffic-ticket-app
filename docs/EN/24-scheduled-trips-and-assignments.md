# Module 24 — Scheduled trips and driver/vehicle assignments

## Outcome

Module 24 turns the centrally managed operations catalog into a daily duty plan. A catalog revision can now contain service calendars, scheduled trips, ordered stop times, direction, and date-specific assignments joining one trip to one driver and one bus.

The Android app keeps this schedule in the same offline catalog snapshot as drivers, buses, routes, stops, and fares. After sign-in, a driver sees only their assigned duties. Selecting a duty locks its bus and route into the shift and stores the trip and assignment identifiers with the actual shift start and end timestamps. The established ad-hoc pilot path remains available.

## Central scheduling workflow

In the authenticated catalog-administrator workspace:

1. Define a service calendar with a date range and select its named weekdays.
2. Define a scheduled trip using a route, calendar, normal `HH:mm` departure time, direction, and the generated `HH:mm` field for every route stop in route order. Mark a stop as “Next day” only for service after midnight.
3. Assign the trip to a service date, driver, and bus.
4. Publish the complete catalog revision atomically.

The web form translates these operator-friendly controls to the contract representation: ISO weekday numbers (`1` Monday through `7` Sunday) and integer minutes after midnight. Trip and assignment selectors include the time range, route, direction, and record ID so similar duties remain distinguishable.

Publication rejects unknown references, invalid dates, unordered stop times, inactive service dates, and overlapping assignments that reuse the same driver or bus.

## Driver and reporting behavior

The tablet refreshes schedules through the existing authenticated catalog endpoint and retains them offline. Scheduled duty selection automatically chooses the assigned bus and route. A restarted active shift restores its assignment context. Synchronization includes nullable `scheduledTripId` and `assignmentId` fields while legacy and ad-hoc shifts remain valid.

The report ledger distinguishes scheduled operations from ad-hoc pilot operations and exposes the actual start/end data already captured by the shift.

## Independent validation checklist

1. Open `/admin` with the catalog-admin token and verify seeded calendars, trips, and assignments are visible.
2. Add or update a calendar, trip, and assignment; publish and confirm the catalog revision increases.
3. Try overlapping trips for the same driver or bus on the same date and verify publication is rejected.
4. On Android, configure the local server, refresh managed data, and sign in as the assigned driver.
5. Select the assigned duty and verify its date, departure, route, and bus; confirm bus and route cannot be changed while that duty is selected.
6. Switch to the ad-hoc option and verify manual bus/route selection still works.
7. Select the duty, disconnect networking, restart the app, and verify the duty remains available from the offline catalog.
8. Start and close the scheduled shift, synchronize it, and verify the web report identifies the scheduled trip and assignment.

## Completion boundary

This module plans and assigns work; it does not dispatch replacement drivers, calculate lateness, publish GTFS, or supervise live vehicle positions. Those capabilities belong to later roadmap modules.

## Independent validation result

Completed on July 20, 2026. Independent catalog-admin validation confirmed that a calendar without a selected service day is rejected, route selection generates the ordered stop-time controls, decreasing stop times are rejected, and an overnight trip is accepted only when the appropriate stops are marked “Next day.” The revised weekday and `HH:mm` controls were also confirmed to be clear and usable.

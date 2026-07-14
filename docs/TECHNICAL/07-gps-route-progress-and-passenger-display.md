# Technical: GPS Route Progress and Passenger Display

## Scope

This module connects Android location updates to the ordered stops of the active shift's selected route. Route progress is stored locally and projected into a driver view and passenger view from one `DriverShiftUiState`.

## Main components

- `AndroidGpsTracker` listens to enabled GPS and network providers through `LocationManager`.
- `nearestForwardStopIndex` selects the nearest stop within a 150-meter arrival radius whose index is not behind stored progress.
- `RouteProgress` records the shift ID, current stop index, update time, and source (`SHIFT_START`, `GPS`, or `MANUAL`).
- `OfflineFirstRepository` persists progress in `SharedPreferences` and restores it only when its shift ID matches the active shift.
- `DriverShiftViewModel` owns GPS lifecycle, applies automatic or demo updates, and exposes current/next-stop status.
- `PassengerDisplay` consumes the same view-model state as the driver console.

## State flow

1. Starting a shift creates progress at stop index 0 with source `SHIFT_START`.
2. After location permission is granted, the view model starts the GPS tracker.
3. Each location update selects the nearest forward stop and persists a `GPS` progress record.
4. **Advance Stop (Demo)** increments the index and persists a `MANUAL` record.
5. Driver and passenger presentations recompute current/next stops from that shared record.
6. Ending a shift stops location updates and clears active route progress.
7. Before clearing active progress, the view model retains an in-memory route/progress snapshot for a read-only last passenger display.

## Forward-only rule

GPS matching considers only the current stop and later stops and requires the selected stop to be within a fixed 150-meter arrival radius. This avoids regressions from stale, noisy, out-of-order, or far-away readings. The current pilot deliberately uses stop proximity rather than a complete route polyline. Production matching should make the radius configurable and add route geometry, direction, accuracy, and stale-reading controls.

## Permission and lifecycle

`ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` are declared in the manifest and requested together at runtime. Either granted permission permits tracking. The tracker registers only during an active shift, removes listeners when the shift ends, and also removes them when the view model is cleared.

If permission is missing or no location provider is enabled, route progress remains usable through the demo control.

## Closed-shift passenger snapshot

Ending a shift copies the selected route and final `RouteProgress` into separate UI-state fields before active persistence is cleared. This keeps the historical passenger presentation independent from route selection for the next shift. The snapshot is read-only, marked as ended, and intentionally process-local, matching the existing process-local last-closed ticket summary.

## Persistence compatibility

Progress is stored separately from the active shift. An active shift created before this module has no progress record, so restoration safely initializes it at stop index 0. Clearing the shift also clears route progress.

## Verification

`RouteProgressTest` verifies nearest-stop selection, the forward-only constraint, current/next-stop projection, final-stop completion, and empty-route safety. The full Android unit-test build, debug APK assembly, and lint are used to catch integration, manifest, and Compose issues.

Manual acceptance still covers runtime permission behavior, actual device GPS providers, restart restoration, and synchronization between the driver and passenger presentations.

## Next technical step

Integrate the stop-request button and propagate an active stop request into the shared shift state and both onboard presentations.

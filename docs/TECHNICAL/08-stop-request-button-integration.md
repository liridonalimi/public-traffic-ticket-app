# Technical: Stop-Request Button Integration

## Scope

This module adds one persisted stop request to the active shift, routes input through a device boundary, projects the request into driver and passenger presentations, and clears it when route progress reaches the target stop.

## Domain state

`StopRequest` contains:

- `shiftId`
- `requestedStopIndex`
- `requestedAtMillis`

The requested stop is represented by its ordered route index so it can be compared directly with `RouteProgress.currentStopIndex`. `isStopRequestReached` requires matching shift IDs and returns true when progress is at or beyond the target index.

## Device boundary

`StopRequestInput` exposes lifecycle methods:

- `start(onStopRequested)`
- `stop()`

`DemoStopRequestInput` implements the boundary with an in-memory listener and a `trigger()` method. Both demo UI controls call this trigger rather than mutating stop-request state directly. A hardware adapter can implement the same boundary for a vehicle-specific transport.

The view model starts listening when a shift starts or is restored, and stops listening when the shift ends or the view model is cleared.

## Request sequence

1. The device input emits a button event.
2. The view model verifies that a shift, route, progress record, and next stop exist.
3. If no request is active, it targets `currentStopIndex + 1`.
4. The repository persists the request before UI state is updated.
5. Driver and passenger displays resolve and show the target stop from the selected route.
6. Further button events are ignored while the request remains active.

## Arrival clearing

Every GPS or manual route-progress update is persisted and then compared with the active request. When the requested index is reached or passed, the repository clears the request and the shared UI state removes it.

Persisting progress before clearing the request is crash-safe on restoration: if the app stops between those operations, initialization detects that restored progress already reached the request and removes the stale request.

## Persistence

`OfflineFirstRepository` stores the active request as JSON in `SharedPreferences`. Loading requires a shift-ID match. Ending a shift clears active shift data, route progress, and the stop request together.

Older installations have no stop-request key and therefore restore with no active request; no data migration is required.

## Presentation

`DriverShiftUiState.requestedStop` resolves the target `Stop` from the active route. The driver console and passenger display consume this shared derived value. The passenger screen exposes a demo request control, while the driver console exposes a second simulation control for acceptance testing.

At the final stop, request controls are disabled or omitted. The read-only Last Passenger Display never carries an active request after shift closure.

## Verification

- `StopRequestTest` covers before-target, at-target, passed-target, and mismatched-shift behavior.
- `DemoStopRequestInputTest` verifies that events are delivered only while listening.
- `DriverShiftUiStateTest` verifies target-stop projection for both displays.
- The full unit-test, debug-assembly, and Android-lint tasks verify integration with the existing app.

Manual acceptance covers process restart, visible synchronization between screens, demo-button interaction, automatic arrival clearing, final-stop behavior, and shift closure.

## Limitations

- No production hardware transport has been selected or implemented.
- There is one active request rather than an event history.
- There is no acknowledgement, chime, vibration, or indicator-light output.
- Requests always target the immediate next stop.

## Next technical step

Add server synchronization for locally stored shifts and tickets while preserving offline-first behavior and idempotent upload rules.

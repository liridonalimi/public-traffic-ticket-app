# GPS Route Progress and Passenger Display

## Purpose

This module connects an active driver shift to the selected route's stop sequence. The app can update progress from the Android device location and shows the same current and next stop on both the driver console and a dedicated passenger display.

## What the driver can do now

- Start a shift at the first stop of the selected route.
- Allow location access and use GPS to update route progress automatically.
- See the current stop and next stop on the driver console.
- Use **Advance Stop (Demo)** to test the complete route without moving the device.
- Open a large passenger-facing display during an active shift.
- Reach the final stop and see that the route is complete.
- Reopen the last passenger display after ending the shift.
- Restart the app and restore the last saved stop for the active shift.

## Passenger display

The passenger display shows the selected line, the current stop, the next stop, and the stop number. It reads the same state as the driver console, so a GPS update or demo advance is reflected immediately. After a shift ends, the console retains an in-memory snapshot of the route and final progress so the driver can reopen the last passenger display. The retained view is marked **SHIFT ENDED** and cannot change route progress.

## GPS behavior

Route progress moves forward only and advances when the bus is within 150 meters of a route stop. A later GPS reading cannot move the bus back to an earlier stop, and a location far from the route cannot skip stops. If location permission or a location provider is unavailable, the driver can still validate and operate the demo flow manually.

## Double-test checklist

1. Sign in, choose a bus and route, and start a shift.
2. Confirm the first route stop appears as the current stop and the second as next.
3. Deny or skip location permission and confirm **Advance Stop (Demo)** still works.
4. Open the passenger display and confirm it matches the driver console.
5. Return to the console, advance one stop, and reopen the passenger display.
6. Advance to the final stop and confirm **Route complete** / **Final destination** appears.
7. Close and reopen the app during an active shift and confirm the saved stop is restored.
8. If testing on a GPS-capable device, allow location access and confirm GPS tracking becomes active.
9. End the shift, tap **Open Last Passenger Display**, and confirm the final route state remains visible with **SHIFT ENDED**.

## Current limitations

- Route and stop coordinates are local demo data.
- Progress uses the nearest stop at or ahead of the current stop within a fixed 150-meter arrival radius; it does not yet use route geometry, direction, speed, or GPS accuracy.
- This pilot uses one app screen for the passenger view. A production vehicle may use a second physical display or a separate companion app.
- The last closed passenger snapshot is retained only while the app process remains alive; it is not restored after a full app restart.
- Stop announcements and stop-request buttons are not included yet.

## Next module

Module 08 will integrate the stop-request button and surface the request to the driver and passenger display.

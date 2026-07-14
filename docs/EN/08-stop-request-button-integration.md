# Stop-Request Button Integration

## Purpose

Module 08 lets a passenger request the next stop and keeps that request synchronized with the driver console and passenger display. The request is attached to the active shift, survives an app restart, and clears automatically when the bus reaches the requested stop.

## What passengers and drivers can do

- Press **Request Stop (Demo)** on the passenger display.
- Use **Press Stop Button (Demo)** on the driver console to simulate the same device input.
- See **STOP REQUESTED** and the requested stop name on both displays.
- Continue seeing the request after closing and reopening the app during the active shift.
- Advance the route with GPS or the demo control and clear the request automatically on arrival.
- Request the following stop after the previous request has cleared.

## Request behavior

A button press always targets the stop immediately after the current stop. Only one request can be active at a time, so repeated presses do not create duplicate requests. The request stores the shift ID, target stop index, and request time.

When GPS or demo progress reaches or passes the requested stop, the request is cleared from local storage and both displays. No request can be created at the final stop because there is no next stop.

## Offline behavior

The active request is stored locally with the active shift. It does not require internet connectivity and is restored only when its shift ID matches the restored shift. Ending the shift clears the request.

## Device integration boundary

This pilot includes a demo stop-button input that sends events through the same `StopRequestInput` boundary intended for physical hardware. It validates application state and screen synchronization without claiming compatibility with a particular vehicle button.

A production installation will require an adapter for the selected hardware transport, such as GPIO through a vehicle controller, Bluetooth, USB, or a vendor-specific interface.

## Double-test checklist

1. Start a shift and confirm no stop request is active.
2. Open the passenger display and tap **Request Stop (Demo)**.
3. Confirm **STOP REQUESTED** shows the next stop name.
4. Return to the driver console and confirm the same request and stop name appear there.
5. Close and reopen the app during the shift and confirm the request is restored.
6. Tap **Advance Stop (Demo)** and confirm the request clears at the requested stop.
7. Create another request and confirm it targets the new next stop.
8. Advance to the final stop and confirm the request clears and both request buttons are disabled or hidden.
9. End the shift and confirm the Last Passenger Display has no active stop request.

## Current limitations

- The included button is a software demo, not a physical vehicle button.
- A request always targets the immediate next stop; passengers cannot select another stop.
- There is no audible chime, vibration, button light, or driver acknowledgement workflow yet.
- Stop-request history is not retained after the request is cleared.

## Next module

Module 09 will synchronize shifts and tickets with the server while preserving offline operation.

# Module 17 - Driver Pilot UX and Role Separation

## Outcome

Module 17 turns the Android pilot into a clearer driver-facing workflow while keeping technical and administrative controls available in a separate supervisor workspace.

The default screen now prioritizes the tasks a driver needs during service:

- confirm the signed-in driver, bus, and route;
- start a shift;
- follow route and stop-request status;
- select a fare and sell tickets;
- review shift ticket and cash totals;
- end a shift through a confirmation dialog;
- synchronize closed shifts without seeing server credentials.

## Driver safeguards

- A clear `READY TO START` or `SHIFT ACTIVE` status is shown near the top of the console.
- Operations and setup tools cannot be opened during an active shift.
- Ending a shift requires explicit confirmation and explains that sales will close.
- Technical endpoint, token, demo-server, and reporting controls are removed from the active driver workflow.
- If closed shifts are waiting, the driver receives one focused `Sync Closed Shifts` action.

## Operations and setup workspace

Between shifts, `Operations & Setup` opens a separate local pilot workspace for:

- selecting demo, local-validation, or configured production synchronization;
- entering a server endpoint and session-only access token;
- validating online/offline and retry behavior;
- manually synchronizing pending data;
- opening the in-app administrative report preview.

This is workflow separation for the pilot. It is not production authentication or role-based authorization. Those controls remain a later security milestone.

## Driver validation checklist

1. Sign in and confirm the status reads `READY TO START`.
2. Open `Operations & Setup`, then return to the driver console.
3. Start a shift and confirm the operations button is no longer available.
4. Sell tickets and verify ticket count, cash, fare totals, and passenger display.
5. Press `End Shift`, cancel once with `Keep Shift Open`, and confirm the shift remains active.
6. Press `End Shift` again and confirm it.
7. Verify the closed shift appears in synchronization status.
8. Press `Sync Closed Shifts` and verify the pending counts reach zero.
9. Open `Operations & Setup` and confirm server setup and reporting remain functional.

## Acceptance criteria

- The driver can complete the full shift without entering the supervisor workspace.
- Supervisor controls are unavailable during service.
- A shift cannot be ended with one accidental tap.
- Closed-shift data remains retryable and synchronizes exactly as before.
- Existing printer, route, passenger display, stop request, reporting, and offline behavior remain intact.

# Offline Server Synchronization

## Purpose

Module 09 adds an offline-first synchronization workflow for closed shifts and ticket sales. Operational data remains local until a server explicitly acknowledges its IDs. A deterministic demo server is included so success, failure, and retry behavior can be validated without a production endpoint or credentials.

## What the driver can do

- See how many closed shifts and tickets are waiting for synchronization.
- Keep selling tickets while offline.
- Close a shift and retain its driver, bus, route, start time, and end time for upload.
- Set the demo server offline and verify that synchronization fails safely.
- Set the demo server online and retry the same pending data.
- See exactly how many shifts and tickets were acknowledged.
- Restart the app and keep unsynchronized closed shifts and tickets available.

## Offline-first behavior

Tickets are stored at sale time, as in previous modules. Module 09 additionally stores every closed shift before clearing active-shift state. Each shift and ticket has its own stable ID, which serves as its server idempotency key.

Active-shift tickets are not uploaded because their shift has not finished. Previously closed shifts, historical tickets, and tickets created by older app versions can still be synchronized while another shift is active.

## Acknowledged-only updates

A sync attempt creates a deterministic batch request ID from the included entity IDs. The client sends the pending records and receives separate acknowledged shift and ticket ID sets.

Only IDs listed in that acknowledgement are marked synchronized. A connection failure, server failure, missing acknowledgement, or unknown returned ID cannot mark another local record as synchronized. Unacknowledged data remains ready for retry.

## Demo server

The driver console contains:

- **Go Offline / Go Online** to control the demo server.
- **Sync Now** to attempt synchronization.

The demo server acknowledges every submitted record when online and acknowledges nothing when offline. It validates local queueing, status messages, retry behavior, and acknowledgement handling. It is not an internet server and does not claim production connectivity.

The demo adapter does not retain a server-side copy of the submitted data. Its acknowledgement means only that the client workflow was accepted for testing.

## Double-test checklist

1. Note the initial numbers under **Total waiting for sync**.
2. Tap **Go Offline**, then **Sync Now**.
3. Confirm the failure message appears and both pending counts remain unchanged.
4. Tap **Go Online**, then **Sync Now**.
5. Confirm the success message appears and eligible pending counts become zero.
6. Start a shift, sell and successfully print at least one ticket.
7. Tap **Sync Now** and confirm the active-shift ticket remains pending.
8. End the shift and confirm one closed shift plus its ticket are waiting.
9. Close and reopen the app and confirm those pending counts are restored.
10. Test offline failure once more, then go online and confirm the retry clears both counts.
11. Tap **Sync Now** again and confirm the app reports that no data is waiting.

## Current limitations

- The included server is an in-app demo adapter.
- No production base URL, authentication token, TLS policy, or HTTP response schema has been supplied yet.
- Synchronization is manual; there is no background scheduler or connectivity-triggered retry.
- The app does not yet expose per-record sync history or server timestamps.
- Historical tickets from earlier modules may not have matching closed-shift metadata because those versions did not store closed shifts.

## Production integration

A production client must implement the same `TransitSyncClient` boundary, use shift and ticket IDs as idempotency keys, authenticate securely, and return authoritative acknowledgement ID sets. The local repository and UI workflow do not need to change when that adapter is introduced.

The intended server relationship is: each closed shift stores its driver, bus, route, and start/end times; each ticket references that shift ID and stores its fare, charged price, and sale time. This allows reporting of shift counts per driver and ticket totals per shift. GPS history and stop-request history are not part of the current sync payload.

## Next module

Module 10 will define the reporting data contract used by an administrative dashboard.

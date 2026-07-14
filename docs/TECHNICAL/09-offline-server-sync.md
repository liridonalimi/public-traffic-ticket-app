# Technical: Offline Server Synchronization

## Scope

This module introduces durable closed-shift history, pending sync queries, stable batch identities, a server-client boundary, acknowledged-only local updates, and driver-visible failure/retry controls. A demo client validates the workflow until a production endpoint and authentication contract are available.

## Durable entities

`Shift` now contains a backward-compatible `synced` flag whose default is `false`. When a shift ends, the view model creates a copy with `endedAtMillis`, stores it in the closed-shift collection, and only then clears active-shift state.

Tickets already contain stable IDs and a `synced` flag. Active-shift tickets remain local and are excluded from sync batches until the shift closes.

Closed shifts are stored as a JSON array under a new `SharedPreferences` key. Older installations have no closed-shift array and load an empty collection without migration. Existing ticket JSON remains compatible.

## Sync domain contract

`SyncBatch` contains:

- `requestId`
- pending closed `shifts`
- eligible pending `tickets`

`requestId` is a deterministic SHA-256-derived value based on the sorted entity type/ID keys. Entity IDs are the authoritative idempotency keys; resending the same entities must be safe on the server.

`SyncAcknowledgement` contains separate acknowledged shift-ID and ticket-ID sets. `SyncResult` is either `Success(acknowledgement)` or `Failure(message)`.

## Server boundary

`TransitSyncClient.sync(batch)` is a suspend function so a production adapter can perform network I/O without changing the view model. `DemoTransitSyncClient` implements the boundary for acceptance testing:

- online: acknowledges every submitted entity ID
- offline: returns a failure and no acknowledgement

The demo adapter is not an HTTP server. Production work still requires an HTTPS transport, serialization schema, authentication, timeouts, response validation, and server-side idempotent upserts.

The demo adapter also retains no server-side records. A success result validates client acknowledgement handling only; it is not evidence that data exists outside the Android application.

## Repository operations

`OfflineFirstRepository` now supports:

- saving and loading closed shifts
- querying unsynced closed shifts
- querying unsynced tickets while excluding the active shift
- counting pending shifts and tickets
- applying acknowledged shift and ticket ID sets

Acknowledgement helpers mark only matching local IDs. Unknown or omitted IDs have no effect. A partial acknowledgement therefore leaves the remaining records pending for retry.

## Sync sequence

1. Query pending closed shifts and eligible tickets.
2. Build a deterministic batch.
3. Call `TransitSyncClient`.
4. On failure, preserve every local pending flag and show the error.
5. On success, apply only the returned acknowledgement IDs.
6. Recount pending data and display the acknowledged totals.

An empty eligible batch produces an informational message. If active tickets are pending, the message explains that they wait for shift closure.

## Compatibility boundary

Tickets created before Module 09 may reference shifts whose closed records were never stored. They remain eligible ticket records so existing local sales are not trapped permanently. A production API must define how it accepts or reconciles these legacy ticket-only records.

## Verification

- `SyncModelsTest` verifies stable batch identity and acknowledged-only shift/ticket updates.
- `DemoTransitSyncClientTest` verifies full acknowledgement online and safe failure offline.
- Existing ticket, shift-state, route, stop-request, and printer tests continue to run.
- Debug assembly and Android lint validate the integrated UI and persistence changes.

Manual acceptance covers pending counts, active-shift exclusion, closed-shift persistence, restart recovery, offline failure, online retry, and empty-queue behavior.

## Production requirements

- Define an HTTPS base URL and endpoint.
- Define authenticated request headers or device credentials.
- Define request/response JSON and versioning.
- Enforce server-side idempotency by entity ID.
- Return explicit per-entity acknowledgements.
- Add bounded timeouts, retry policy, telemetry, and background scheduling.
- Define handling for legacy tickets without stored closed-shift metadata.

The intended production relationship is one driver to many closed shifts and one closed shift to many tickets. The current sync domain supplies the required IDs and operational fields for those relationships; GPS trails and stop-request history are outside the payload.

## Next technical step

Define the administrative reporting data contract for shift totals, fare breakdowns, cash reconciliation, and synchronization visibility.

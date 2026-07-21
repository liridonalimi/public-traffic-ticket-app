# Module 26 — Controlled ticket actions (technical)

Module 26 introduces `TicketAction` as an append-only entity with a stable ID, original ticket and shift references, action type, controlled reason, supervisor identifier, authorization/creation times, optional corrected fare/price, and synchronization state.

`VOID` and `CORRECTION` are mutually exclusive per original ticket. `REPRINT` is repeatable and has no revenue effect. Revenue projection uses the original price when no financial action exists, zero for a void, and the corrected price for a correction. Original ticket rows and offline JSON records are never rewritten by an action.

The version-1 sync body accepts the backward-compatible optional `ticketActions` array and returns `acknowledgedTicketActionIds`. Empty action arrays are omitted from canonical hashing so pre-Module-26 idempotency replays retain their original request hash. Action batches exclude the changing transport `sentAtMillis` value from identity hashing, allowing a lost acknowledgement to replay safely. SQLite enforces foreign keys, immutable IDs, correction field shape, and a partial unique index limiting each ticket to one financial action.

Every pending action batch also carries the immutable original ticket and closed shift, even when the tablet previously marked them synchronized. This allows a tablet that used demo synchronization before selecting a real server to establish the required foreign-key records without duplicating revenue; an existing identical ticket/shift is matched idempotently.

The server report projects adjusted revenue and exposes gross shift revenue plus complete action history. Android reporting applies the same deterministic projection. Reprint events are persisted only after the selected printer reports success.

## Testing and validation

- `TicketActionTest` verifies zero-value void projection, corrected-value projection, revenue-neutral reprints, immutable original prices, and rejection of a second financial action.
- `SyncModelsTest` verifies a pending action carries its original ticket and closed shift even when demo mode previously marked those records synchronized.
- `ProductionTransitSyncClientTest` verifies JSON serialization and independent `acknowledgedTicketActionIds` handling.
- Server contract tests reject missing supervisor evidence, unknown reasons, incomplete corrections, unknown original tickets, and conflicting IDs.
- SQLite tests verify one financial action per ticket, identical retry replay, changed-data conflict rejection, and unchanged totals after a rejected transaction.
- Android product validation covers void, correction, successful/failed reprint, app restart before sync, real-server synchronization, and duplicate-safe retry.
- Web validation compares original gross revenue with the deterministic effective projection and inspects action type, reason, supervisor, time, and original ticket link.

Expected invariants: tickets are never edited or deleted, a financial action affects revenue exactly once, and a reprint never creates revenue.

Automated coverage verifies validation, exact-once revenue, duplicate financial-action rejection, immutable original values, action-only synchronization, replay safety, and reporting. Production-grade supervisor authentication is intentionally deferred to the selected identity platform; the local pilot captures offline authorization evidence explicitly.

Independent tablet/web validation completed on July 21, 2026. It confirmed dependency-aware synchronization after prior demo acknowledgement, one void, two corrections, two reprints, duplicate financial-action rejection, and matching gross/effective revenue projections. Module 26 is complete.

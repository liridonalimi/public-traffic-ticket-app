# Module 26 — Controlled ticket voids, corrections, and reprints

## Outcome

Module 26 adds append-only post-sale actions without changing or deleting the original ticket. Between shifts, the local supervisor workspace can select a closed ticket, choose a reason, record the supervisor identifier, and authorize a void, price/fare correction, or reprint.

- A ticket may have at most one revenue-changing action: void or correction.
- A void changes effective revenue to zero exactly once.
- A correction records the replacement fare and amount while retaining the original sale.
- A reprint prints the original durable ticket and records a zero-value audit event only after output succeeds.
- Multiple reprints are allowed and remain distinguishable from new sales.

## Persistence, synchronization, and reporting

Ticket actions have stable IDs and are stored offline separately from tickets. Synchronization acknowledges action IDs independently. The server stores actions in an immutable table, rejects conflicting IDs and a second financial action for one ticket, and safely replays identical requests.

Android and web reports show adjusted revenue, original gross revenue when it differs, action counts, and per-shift action history with reason, supervisor identifier, and authorization time. Original ticket price and policy snapshots remain unchanged.

## Validation checklist

1. Close and synchronize a shift containing at least two tickets.
2. In **Operations & Setup**, void one ticket with a reason and supervisor identifier.
3. Confirm the local report retains the ticket but reduces effective revenue exactly once.
4. Attempt another void/correction on the same ticket and confirm rejection.
5. Correct another ticket to a different fare and amount; confirm gross and adjusted totals.
6. Reprint a ticket with the PDF simulator; confirm no new ticket is created and revenue is unchanged.
7. Synchronize, refresh the web report, and confirm all three action types and supervisor evidence appear.
8. Retry synchronization and confirm totals and action counts do not change.

## Completion boundary

The local pilot records a supervisor identifier as offline authorization evidence. A production identity provider, strong supervisor authentication, fiscal cancellation, refunds, and accounting-ledger posting remain external integrations. QR inspection consumes void state in Module 27.

## Validation result

Completed on July 21, 2026. Independent tablet and report-reader validation confirmed:

- a void is recorded once and a second financial action on the same ticket is rejected;
- corrections and reprints remain distinguishable from ticket sales;
- actions survive locally until the real server acknowledges their IDs;
- action synchronization carries missing original ticket/shift dependencies safely when earlier demo acknowledgement did not populate the real server;
- the web report displays one void, two corrections, and two reprints with supervisor evidence;
- gross and effective revenue remain visible together, while original tickets and cash-handover evidence remain unchanged.

Module 26 is complete.

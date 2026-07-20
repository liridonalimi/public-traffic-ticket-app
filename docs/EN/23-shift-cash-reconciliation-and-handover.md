# Module 23 — Shift cash reconciliation and handover

## Outcome

Module 23 makes shift closure an accountable cash-handover operation. Before a driver can end a shift, the app shows the ticket-derived expected cash and requires the physically counted cash amount. The shift stores both values, the signed variance, the reconciliation time, and one of four states: matched, shortage, surplus, or not recorded for legacy data.

Money input is converted directly to integer euro cents. Negative amounts, text, more than two decimal places, and values above the local safety limit are rejected. A valid amount of zero is accepted for a zero-cash shift.

## Driver workflow

1. Complete ticket sales and resolve any pending/failed print.
2. Press **End Shift**.
3. Confirm the displayed expected cash.
4. Physically count the cash and enter the amount in EUR, for example `2.90`.
5. Review the immediately calculated variance. Zero is matched, a negative value is a shortage, and a positive value is a surplus.
6. Press **Confirm Handover**. Until a valid amount is entered, the confirmation action remains disabled and the shift stays open.

After closure, the driver console shows the expected cash, declared cash, variance, and reconciliation status in the last-closed-shift summary. The record remains available offline and waits for the normal acknowledged synchronization workflow.

## Reporting and synchronization

The shift synchronization record carries `expectedCashCents`, `declaredCashCents`, and `reconciledAtMillis` together. The server rejects a partially supplied reconciliation. Existing shifts without these fields remain valid and are clearly reported as `NOT_RECORDED`; the system does not invent a historical declaration.

The Android report preview and authenticated web report show:

- expected and declared cash totals;
- signed aggregate variance;
- reconciled and unreconciled shift counts;
- per-shift expected cash, declared cash, variance, and status.

Server ingestion remains transactional and idempotent. Backup and restore preserve the reconciliation values with the shift.

## Independent validation checklist

### Matched shift

1. Start a shift and sell several tickets.
2. Press **End Shift** and verify the expected amount matches the visible ticket cash total.
3. Confirm that an empty or invalid amount cannot close the shift.
4. Enter exactly the expected amount and confirm the handover.
5. Verify the last shift summary shows a zero variance and matched status.

### Shortage or surplus

1. Complete another shift with at least one ticket.
2. Enter an amount below or above the expected cash.
3. Verify the signed variance and shortage/surplus wording before confirmation and in the closed-shift summary.

### Persistence, sync, and reporting

1. Close and reopen the app before synchronization; verify the closed shift remains pending.
2. Configure the local server and synchronize.
3. Open the authenticated report-reader dashboard and refresh it.
4. Verify the new shift appears with the same expected, declared, variance, and status values.
5. Verify the aggregate reconciliation cards update and older shifts without handovers remain visibly unreconciled.

## Completion boundary

This module records and reports the driver's counted-cash declaration. It does not provide supervisor approval, deposit/bag tracking, denominations, ticket voids, refunds, or accounting-ledger integration. Controlled corrections arrive in Module 26; richer operational exports arrive in Module 28.

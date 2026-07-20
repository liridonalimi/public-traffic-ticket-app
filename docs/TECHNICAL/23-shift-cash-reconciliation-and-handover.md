# Module 23 — Shift cash reconciliation and handover (technical)

## Domain and local persistence

`Shift` adds nullable `expectedCashCents`, `declaredCashCents`, and `reconciledAtMillis` fields. All three are null for active and legacy shifts and all three are populated atomically at new shift closure. `cashVarianceCents` is declared minus expected. `CashReconciliationStatus` derives `MATCHED`, `SHORTAGE`, `SURPLUS`, or `NOT_RECORDED` without storing redundant status state.

The close dialog uses `parseEuroAmountToCents`, which accepts dot/comma decimal separators and zero to two decimal digits, converts with integer string arithmetic, and applies a bounded maximum. No binary floating-point value participates in money input.

`OfflineFirstRepository` serializes the three nullable fields in active/closed shift JSON and decodes missing fields as null for backward compatibility. Closing writes the reconciled shift before clearing active-shift state.

## Contract and database migration

Contract v1 adds three nullable shift members. The parser requires all three together or none, bounds both money values, and requires the reconciliation timestamp to be no earlier than shift end. Omitting all three preserves compatibility with already stored and older client records.

SQLite initialization creates the new columns for fresh databases and checks `PRAGMA table_info(shifts)` to add missing nullable columns to an existing database. Transactional ingestion and canonical request hashing include reconciliation values. Insert-or-match conflict checks therefore protect those values from mutation under an existing shift ID.

## Reporting

Server reporting adds overall expected cash, declared cash, signed variance, reconciled count, and unreconciled count. Each shift includes the three stored fields, derived variance/status, and reconciliation time. The admin web dashboard renders aggregate cards and per-shift handover detail. Legacy shifts remain `NOT_RECORDED` rather than being treated as matched.

The in-app administrative preview derives equivalent totals from the offline repository. For legacy presentation only, ticket revenue can supply the expected display total, but declared cash remains absent and the shift remains unreconciled.

## Verification

- money parsing and all four derived statuses have Kotlin unit coverage;
- production-client tests verify all reconciliation members in outbound JSON;
- Android reporting tests cover aggregate and per-shift shortage behavior;
- contract tests cover complete, partial, and legacy field sets;
- database tests cover persistence, idempotency, report projection, and schema migration;
- backup/restore verifies reconciled values survive recovery;
- staging smoke requires the new overall reporting fields;
- JavaScript/browser validation checks aggregate cards and per-shift detail;
- full Android unit tests, debug APK assembly, lint, Python tests, and compilation are required before independent validation.

## Security and audit boundary

The declared amount is operational financial data and follows the existing report-reader boundary. Shift synchronization remains available only to `device_sync`; reports remain `report_read`. Authorization decisions continue to be recorded by Module 22. This module does not introduce a mutable reconciliation endpoint, so a synchronized declaration cannot be silently edited through the administrative UI.

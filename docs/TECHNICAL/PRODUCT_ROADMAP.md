# Technical product roadmap — Modules 23–35

## Architectural sequence

The roadmap extends existing domain and contract boundaries instead of replacing them. Every new persisted field requires backward-compatible decoding or an explicit contract-version migration. Every synchronized mutation remains idempotent. Historical tickets and shifts retain immutable snapshots of the policies that produced their financial values.

1. **Module 23 — Cash reconciliation:** extend shift persistence, sync DTOs, SQLite projection, and reporting with expected/declared/variance/status values.
2. **Module 24 — Scheduled work:** add service calendars, trips, directions, stop times, assignments, conflict validation, and offline duty snapshots.
3. **Module 25 — Fare policies:** introduce versioned conditions and deterministic rule evaluation with publication-time ambiguity checks.
4. **Module 26 — Ticket corrections:** model append-only void/correction/reprint events, reason codes, supervisor authorization, and idempotent revenue projection.
5. **Module 27 — Inspection:** define signed/versioned QR payloads, validation outcomes, revocation/expiry inputs, replay evidence, and an isolated inspector role.
6. **Module 28 — Analytics/export:** add reproducible aggregation contracts and streamed, formula-safe CSV output with export audit metadata.
7. **Module 29 — GTFS Schedule:** create an import staging model, reference validator, atomic catalog publication, and deterministic export mapping.
8. **Module 30 — Accessibility/alerts:** separate passenger presentation state from driver controls and add localized, time-bounded alert contracts.
9. **Module 31 — Live operations:** ingest rate-limited position/progress updates, expose freshness semantics, and prepare GTFS Realtime-compatible projections.
10. **Module 32 — Device fleet:** replace shared device credentials with per-device identity, enrollment, revocation, configuration revisions, and health reports.
11. **Module 33 — Payments:** introduce payment attempts and settlement states behind a provider interface; preserve cash as the default implementation.
12. **Module 34 — Operator/depot tenancy:** add mandatory ownership keys, tenant-filtered repositories, scoped uniqueness, and cross-tenant authorization tests.
13. **Module 35 — Release assurance:** establish data retention, privacy operations, MASVS-oriented mobile checks, recovery objectives/evidence, upgrade/rollback gates, and final release criteria.

## Persistent cross-cutting rules

- Android remains offline-first for active service.
- Server acknowledgements are entity-specific and never discard unacknowledged records.
- Financial corrections are append-only and exactly-once in projections.
- Catalog and policy changes publish atomically by revision.
- Administrative and operational roles remain least-privilege and independently auditable.
- Sensitive values never appear in logs, URLs, browser persistence, reports, exports, or screenshots.
- English, Albanian, automated tests, technical documentation, and independent device/web validation remain part of every module definition of done.

## External activation boundary

Modules 23–34 can be developed against the local reference service and provider-neutral staging package. Production payment capture, fiscal approval, physical-printer certification, hosted identity, public GTFS/Realtime publication, monitoring delivery, domain/TLS, and disaster-recovery ownership remain explicit external activation gates rather than simulated claims of production compliance.

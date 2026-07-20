# Product roadmap — Modules 23–35

## Direction

Modules 1–22 established a working offline-first driver application, durable ticketing, printing simulation, route progress, synchronization, central reference data, reporting, role isolation, credential rotation, and authorization auditing. The next phase develops the pilot into a fuller transport-operations product without assuming a hosting provider, payment processor, physical printer, or production identity platform.

Each module keeps the established delivery gate: implementation, automated tests, Android/server build checks, English/Albanian/technical documentation, assistant validation, independent operator validation on the app or web workspace, and an explicit Git handoff only after acceptance.

## Phase A — Daily service and revenue control

### Module 23 — Shift cash reconciliation and handover

The driver records counted cash when ending a shift. The app compares counted cash with expected cash ticket revenue, shows the variance, requires confirmation, preserves the handover offline, and synchronizes it. Reporting shows expected, declared, and variance amounts with reconciliation status. This is the immediate next module.

### Module 24 — Scheduled trips and driver/vehicle assignments

Add service calendars, planned trips, departure times, direction, and central assignments of a driver and vehicle. The tablet presents only relevant work, prevents conflicting assignments, retains an offline duty list, and records actual start/end times without replacing the existing ad-hoc pilot path.

### Module 25 — Fare policy engine

Replace flat catalog fares with versioned rules for route, zone, time period, passenger category, transfer validity, and rule priority. The ticket must retain a snapshot of the applied rule so later catalog changes never rewrite historical revenue. Ambiguous or incomplete fare rules fail safely before publication.

### Module 26 — Controlled ticket voids, corrections, and reprints

Introduce explicit post-sale actions with reason codes, supervisor authorization, immutable links to the original ticket, and visible audit history. Reprints remain distinguishable from new sales, voids adjust revenue exactly once, and synchronized corrections remain idempotent.

## Phase B — Revenue assurance and passenger operations

### Module 27 — Verifiable QR tickets and inspector validation

Issue a compact QR validation payload tied to the ticket, fare, issue time, and device. Add an isolated inspector workspace that reports valid, expired, voided, duplicate, unknown, or unverifiable outcomes. The design must support offline inspection with bounded trust and must not claim fiscal compliance without the applicable legal certification.

### Module 28 — Operational analytics and accountable exports

Extend reporting with route, trip, time-band, vehicle, driver, fare, variance, void, and inspection measures. Provide filterable CSV exports with contract version, generation time, applied filters, and source revision so exported figures can be reproduced and audited.

### Module 29 — GTFS Schedule import/export and validation

Map the managed catalog and scheduled trips to the GTFS Schedule model for agencies, routes, stops, trips, stop times, calendars, and fare-related data. Imports use a reviewable draft, structural and referential validation, and atomic publication. Exports are deterministic and clearly report unsupported or lossy mappings.

### Module 30 — Passenger accessibility, announcements, and service alerts

Improve the passenger display with large-text modes, contrast-safe presentation, bilingual stop announcements, configurable audio cues, disruption messages, and an operator preview. Alerts have severity, validity windows, affected routes/stops, and offline behavior.

### Module 31 — Vehicle positions and live operations supervision

Publish bounded, consented vehicle-position updates from active shifts and show last-known position, freshness, trip progress, and delay state in an operations view. Stale data must be obvious, location collection must stop outside active service, and the model should remain compatible with later GTFS Realtime publication.

## Phase C — Fleet scale, payments, and release assurance

### Module 32 — Device enrollment, configuration, and fleet health

Give each tablet a managed device identity, enrollment/revocation lifecycle, assigned vehicle/depot, configuration revision, app version, last contact, and health status. Device secrets must not be shared, sensitive material must use Android platform protection, and lost devices must be individually revocable.

### Module 33 — Payment-method abstraction and cashless readiness

Represent cash, card, account, voucher, and future payment methods without binding the core ticket domain to one processor. Add a simulator for approved/declined/unknown outcomes, durable payment references, retry-safe reconciliation, and clear boundaries: real card acceptance, certification, chargebacks, and settlement require a selected provider.

### Module 34 — Multi-operator and depot separation

Scope catalogs, assignments, devices, shifts, reports, and administrative roles by operator and depot. Prevent cross-operator access at the API and database boundaries, support controlled shared routes/stops where required, and make operator ownership visible in every administrative workspace.

### Module 35 — Privacy, security, recovery, and production release assurance

Consolidate data classification, retention/deletion rules, mobile security verification, backup/restore drills, disaster-recovery evidence, upgrade/rollback checks, observability requirements, and a production acceptance checklist. Local and staging gates remain provider-neutral. Final hosted activation still requires infrastructure, domain/TLS ownership, accountable operators, legal review, physical-printer certification, payment certification where applicable, and integration with the selected identity provider.

## Ordering rule

Modules are intentionally ordered by dependency. Cash reconciliation precedes richer reports; schedules precede GTFS exchange and live supervision; ticket corrections precede inspection; device identity precedes production-scale cashless operations; operator separation precedes final production assurance. A module may be resequenced only when its required data and authorization boundaries already exist.

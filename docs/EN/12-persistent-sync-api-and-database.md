# Persistent Sync API and Database

## Purpose

Module 12 implements the server side of the Module 11 synchronization contract as a runnable reference service. It authenticates requests, validates the complete version 1 payload, writes shifts and tickets to a transactional SQLite database, returns strict acknowledgements, handles safe retries, and exposes synchronized data for administration/reporting.

This module is implemented locally but not presented as a cloud deployment. A production host, domain, managed database, TLS certificate, secrets manager, and identity provider must be selected before external use.

## What was added

- `POST /v1/sync` authenticated synchronization endpoint.
- `GET /health` database readiness endpoint.
- `GET /v1/reports/admin` authenticated reporting projection.
- Strict bearer-token verification using constant-time comparison.
- Contract version and idempotency-header verification.
- Batch size, record shape, timestamp, price, uniqueness, and relationship validation.
- Transactional SQLite tables for requests, shifts, and tickets.
- Foreign-key enforcement between tickets and shifts.
- Stable replay of an identical request without duplicate records.
- HTTP `409` rejection when a request/entity ID is reused with changed data.
- Persistent driver, fare, shift, ticket, and cash aggregation.
- Optional TLS certificate/key startup for controlled environments.
- Safe error responses that do not return exception or database details.

## Data lifecycle

1. The Android client closes a shift and builds a stable sync batch.
2. The service verifies the bearer token and required headers.
3. The JSON contract is validated before database access.
4. The service begins an immediate transaction.
5. A new request, its shifts, and its tickets are written together.
6. The transaction commits before acknowledgement is returned.
7. The Android app marks only acknowledged IDs as synchronized.

If any record fails, the entire transaction rolls back. An identical retry returns the stored acknowledgement with `X-Idempotent-Replay: true`.

## Local double-test

Use two terminal windows from the project root.

Terminal 1:

```bash
PYTHONPATH=server \
BUSPAY_SYNC_TOKEN=module-12-local-token \
BUSPAY_DB_PATH=/tmp/buspay-module-12.db \
python3 -m buspay_server
```

Terminal 2 — health:

```bash
curl http://127.0.0.1:8080/health
```

Terminal 2 — authenticated sync:

```bash
curl -i -X POST http://127.0.0.1:8080/v1/sync \
  -H 'Authorization: Bearer module-12-local-token' \
  -H 'Content-Type: application/json' \
  -H 'X-BusPay-Contract-Version: 1' \
  -H 'Idempotency-Key: sync-1111222233334444' \
  --data-binary @server/sample-sync.json
```

Run the same command twice. The first response must include `X-Idempotent-Replay: false`; the second must show `true`. Both acknowledgements must contain the same shift and ticket IDs.

Terminal 2 — report:

```bash
curl http://127.0.0.1:8080/v1/reports/admin \
  -H 'Authorization: Bearer module-12-local-token'
```

Confirm the report contains `driverCount: 1`, `shiftCount: 1`, `ticketCount: 1`, and `cashTotalCents: 50`. Also confirm the Android **Sync service** card shows **Reference API/database: implemented • deployment pending** and that existing demo synchronization still works.

## Automated tests

```bash
PYTHONPATH=server python3 -m unittest discover -s server/tests -v
```

## Current limitations

- The built-in WSGI runner is for local validation, not internet-facing production traffic.
- Local HTTP is used only for command-line testing. The Android production client still requires trusted HTTPS.
- The environment bearer token demonstrates enforcement but does not replace short-lived identity-provider tokens.
- SQLite is appropriate for the reference service; production scale may require PostgreSQL or another managed relational database.
- The report contains operational IDs but not a server-managed catalog of driver/bus/route display names.
- No cloud infrastructure has been changed by this module.

## Next product phase

Select production infrastructure and identity, deploy this contract behind managed TLS with a managed database, then connect the Android runtime client using short-lived credentials.

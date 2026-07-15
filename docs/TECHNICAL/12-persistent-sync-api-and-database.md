# Module 12 Technical Notes: Persistent Sync API and Database

## Components

- `server/buspay_server/contract.py`: version 1 input types and structural validation.
- `server/buspay_server/database.py`: schema, transactional ingestion, idempotency, conflicts, and report queries.
- `server/buspay_server/application.py`: dependency-free WSGI routing, bearer authentication, request limits, and JSON responses.
- `server/buspay_server/__main__.py`: local runner with optional TLS wrapping.
- `server/tests/test_service.py`: contract, database, persistence, authentication, API, replay, and report tests.
- `server/sample-sync.json`: deterministic validation request.

The service uses only the Python 3 standard library. This keeps the reference implementation reproducible without package installation while preserving boundaries that can later be hosted behind a production WSGI server or migrated to another framework.

## Database schema

`sync_requests`

- primary key: `request_id`
- contract version
- SHA-256 hash of canonical validated content
- stored acknowledgement JSON
- receive time

`shifts`

- primary key: shift ID
- driver, bus, route IDs
- start/end timestamps
- first request foreign key

`tickets`

- primary key: ticket ID
- shift foreign key
- fare ID, price, sale timestamp
- first request foreign key

Foreign keys are enabled for every connection. File databases use WAL mode and a busy timeout. Ingestion uses `BEGIN IMMEDIATE` so request lookup, inserts, and stored acknowledgement commit atomically.

## Idempotency algorithm

1. Parse and normalize the version 1 contract.
2. Serialize canonical content with stable key ordering.
3. Calculate the SHA-256 payload hash.
4. Lock the write transaction.
5. If the request ID exists with the same hash, return its stored acknowledgement.
6. If the request ID exists with another hash, return `409`.
7. Insert the request shell, then match/insert every shift and ticket.
8. Any entity ID with different stored content returns `409` and rolls back.
9. Store and commit the acknowledgement before responding.

This supports both request retries and entity re-submission in later batches without duplicates.

## HTTP contract

### `GET /health`

Unauthenticated minimal readiness response. It contains no operational records.

### `POST /v1/sync`

Requires:

- valid bearer authorization
- `application/json`
- body size from 1 byte through 1 MB
- `X-BusPay-Contract-Version: 1`
- `Idempotency-Key` exactly matching the JSON `requestId`

It returns the Module 11 acknowledgement and `X-Idempotent-Replay`.

### `GET /v1/reports/admin`

Requires bearer authorization. Returns overall, fare, driver, shift, and ticket projections from committed server records.

## Security boundaries

- Authentication uses `hmac.compare_digest`.
- The service never writes tokens to the database or response.
- Error responses do not include stack traces, SQL, payloads, or exception strings.
- Request size and record-count limits bound resource consumption.
- Control characters and invalid IDs/timestamps/prices are rejected.
- The built-in runner binds to `127.0.0.1` by default.
- Optional TLS requires both `BUSPAY_TLS_CERT` and `BUSPAY_TLS_KEY`.

The environment token is a development/deployment adapter, not an identity-provider implementation. Production should validate scoped, expiring tokens at a gateway or replace this adapter with issuer/audience/signature validation.

## Configuration

- `BUSPAY_SYNC_TOKEN` — required bearer secret.
- `BUSPAY_DB_PATH` — defaults to ignored `server/data/buspay.db`.
- `BUSPAY_HOST` — defaults to `127.0.0.1`.
- `BUSPAY_PORT` — defaults to `8080`.
- `BUSPAY_TLS_CERT` / `BUSPAY_TLS_KEY` — optional pair.

## Validation coverage

Nine server tests cover:

- invalid contract versions, active shifts, and duplicate IDs
- transactional storage and persistence across database instances
- idempotent replay
- changed-payload conflicts without mutation
- unknown-shift rollback
- public health readiness
- missing/wrong bearer authentication
- sync/report reconciliation
- method, content type, contract header, and idempotency header enforcement

The Android test/build/lint suite remains the regression gate because Module 12 also adds a visible backend status line to the driver console.

## Production handoff

The next infrastructure step must choose a cloud/runtime, managed relational database, domain/TLS, secret storage, logging/monitoring, backups, and an identity issuer. Those choices require deployment ownership and credentials and are intentionally not inferred in source code.

# Module 11 Technical Notes: Authenticated HTTPS Sync Client

## Architecture

`ProductionTransitSyncClient` implements the existing `TransitSyncClient` boundary, so `DriverShiftViewModel` and `OfflineFirstRepository` retain the Module 09 acknowledged-only workflow. Deployment chooses the transport implementation; the queue and acknowledgement persistence rules do not change.

Main types:

- `ProductionSyncConfig`: validates endpoint, token, and timeouts.
- `SyncHttpRequest` / `SyncHttpResponse`: transport-neutral HTTP values.
- `SyncHttpTransport`: injectable suspend boundary used by production code and unit tests.
- `HttpsUrlConnectionSyncTransport`: Android/JVM HTTPS implementation running on `Dispatchers.IO`.
- `ProductionTransitSyncClient`: contract serialization, headers, status mapping, response validation, and `SyncResult` mapping.

## Contract version 1

Endpoint method: `POST`

Required headers:

```text
Authorization: Bearer <runtime-token>
Content-Type: application/json; charset=utf-8
Accept: application/json
Idempotency-Key: sync-<stable-digest>
X-BusPay-Contract-Version: 1
```

Request shape:

```json
{
  "contractVersion": 1,
  "requestId": "sync-...",
  "sentAtMillis": 1784040000000,
  "shifts": [
    {
      "id": "shift-...",
      "driverId": "driver-001",
      "busId": "bus-001",
      "routeId": "route-001",
      "startedAtMillis": 1784039000000,
      "endedAtMillis": 1784040000000
    }
  ],
  "tickets": [
    {
      "id": "ticket-...",
      "shiftId": "shift-...",
      "fareTypeId": "student",
      "priceCents": 30,
      "soldAtMillis": 1784039500000
    }
  ]
}
```

Acknowledgement shape:

```json
{
  "contractVersion": 1,
  "requestId": "sync-...",
  "acknowledgedShiftIds": ["shift-..."],
  "acknowledgedTicketIds": ["ticket-..."]
}
```

## Validation invariants

1. A batch must contain at least one entity.
2. Every submitted shift must be closed.
3. The configuration must use an absolute HTTPS URL without URL credentials or a fragment.
4. The bearer token must be nonblank and cannot contain CR/LF.
5. Redirects are disabled.
6. Successful HTTP status alone is insufficient: contract version and request ID must match.
7. Acknowledged IDs must be subsets of submitted IDs.
8. Only the repository acknowledgement step changes local `synced` flags.

## Error mapping

- `400`: rejected payload
- `401` / `403`: rejected authentication
- `409`: conflicting batch
- `413`: oversized batch
- `429`: service busy
- `5xx`: temporarily unavailable
- I/O failure: unreachable service
- malformed or mismatched response: invalid acknowledgement

Messages intentionally omit exception details and response bodies to avoid exposing tokens or infrastructure information.

## Android network policy

`android:usesCleartextTraffic="false"` blocks cleartext application traffic. The runtime transport additionally checks that the opened connection is `HttpsURLConnection`.

## Credential lifecycle

The module accepts a runtime token but does not generate or persist it. Production identity should issue a short-lived scoped token and provide it to the client in memory. Static bearer tokens must not be added to Gradle files, resources, source code, screenshots, logs, or version control.

## Tests

`ProductionTransitSyncClientTest` covers:

- configuration security rules
- authorization, idempotency, content, and contract headers
- request serialization without token leakage
- valid acknowledgements
- mismatched request IDs
- out-of-batch acknowledgements
- authentication and network failures
- pre-network rejection of empty batches and active shifts

## Deployment handoff

To activate production mode, the backend must implement contract version 1 with idempotent storage, and the identity layer must supply a runtime endpoint/token pair. Construct `ProductionTransitSyncClient(ProductionSyncConfig(...))` at the application composition boundary and inject it where the demo `TransitSyncClient` is currently created.

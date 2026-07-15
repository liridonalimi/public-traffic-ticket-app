# Module 15 Technical Design - Android-to-Server Live Sync

## Runtime model

`SyncRuntimeMode` now has three states:

- `DEMO`: in-memory `DemoTransitSyncClient` and simulated availability controls.
- `LOCAL_VALIDATION`: authenticated contract-v1 client over loopback HTTP.
- `PRODUCTION`: authenticated contract-v1 client over HTTPS only.

`DriverShiftViewModel` owns the active `SyncRuntimeConfig` and `TransitSyncClient`. Server endpoint and token drafts are Compose state only. After activation, the token draft is cleared and the active client retains the credential in memory. Nothing is written to `OfflineFirstRepository`, `SharedPreferences`, URL parameters, logs, or report data.

The app intentionally returns to demo mode when its process is recreated. Persistent server credentials require the future authenticated identity/session design.

## Transport policy

`ProductionSyncConfig` remains secure by default. Its public constructor accepts HTTPS endpoints only.

`ProductionSyncConfig.localValidation()` enables a narrower rule:

- scheme must be `http` or secure `https`;
- cleartext host must be exactly `127.0.0.1`, `localhost`, or `::1`;
- user-info and fragments are rejected;
- bearer token must be nonblank and contain no CR/LF;
- existing timeout limits remain enforced.

`UrlConnectionSyncTransport` accepts `HttpsURLConnection` normally. It accepts `HttpURLConnection` only when the config explicitly enables loopback validation and the URI passes the same loopback test.

## Android manifest boundary

The main manifest remains:

```xml
android:usesCleartextTraffic="false"
```

`app/src/debug/AndroidManifest.xml` overrides this only for the debug variant. The application-layer endpoint validation still rejects non-loopback cleartext. Release builds therefore have both manifest and application-level protection.

## ADB network path

For a USB-connected physical device:

```text
Android 127.0.0.1:8080
        |
        | adb reverse tcp:8080 tcp:8080
        v
Mac 127.0.0.1:8080
        |
        v
Docker Compose -> gunicorn -> BusPay WSGI -> SQLite
                                         |
                                         v
                                  /v1/reports/admin
```

The Docker port remains bound to Mac loopback. It is not exposed on the LAN.

## Synchronization semantics

The existing offline-first guarantees are unchanged:

1. active-shift tickets are excluded;
2. only closed shifts enter the sync batch;
3. request IDs are sent as idempotency keys;
4. the server transactionally stores shifts and tickets;
5. Android marks only explicitly acknowledged IDs as synchronized;
6. authentication, network, HTTP, or acknowledgement failures preserve pending data;
7. retrying the same data remains safe.

## UI states

The sync card exposes:

- current runtime mode;
- endpoint and password-style token fields while configuration is open;
- activation validation errors without echoing the token;
- local/production configured status;
- a return-to-demo action;
- the existing synchronization progress and result messages.

## Tests

`LocalValidationTransitSyncClientTest` uses a real JVM `ServerSocket` on loopback and verifies:

- actual HTTP POST transport;
- `Authorization: Bearer` header;
- contract version header;
- idempotency key;
- shift and ticket JSON payload;
- absence of the token from the body;
- acknowledgement parsing and success.

Runtime and production-client tests additionally verify:

- local loopback acceptance;
- non-loopback cleartext rejection;
- missing local credentials rejection;
- production cleartext rejection;
- all previous authentication, acknowledgement, and failure guarantees.

## Operational limitations

This bridge is a local validation mechanism. It does not replace production TLS, DNS, identity, secret issuance, certificate policy, device fleet configuration, observability, or remote revocation.

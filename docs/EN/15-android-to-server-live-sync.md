# Module 15 - Android-to-Server Live Synchronization

## Purpose

Module 15 connects a debug Android build to the real local BusPay sync API. A driver can close a shift, send its pending shift and tickets to the Docker service, and then see the acknowledged records in the authenticated browser dashboard after refreshing the report.

The existing demo mode remains the default. The operator explicitly activates a server connection for the current app session.

## What was added

- Runtime selection between demo, local validation, and production HTTPS sync.
- In-app endpoint and bearer-token configuration.
- Session-only token handling; the token is cleared from the visible field and is not stored in the offline repository or Android preferences.
- Debug-only loopback HTTP support for `adb reverse` validation.
- Loopback restrictions to `127.0.0.1`, `localhost`, and `::1`.
- Continued HTTPS-only enforcement for production mode and release builds.
- Real loopback transport test covering HTTP, bearer authentication, idempotency, contract version, payload delivery, and acknowledgement parsing.

## Physical-device double test

Perform these steps before staging or committing Module 15.

### 1. Prepare Docker

Keep Docker Desktop running. From the project root:

```bash
cp deployment/secrets/buspay_sync_token.txt.example \
  deployment/secrets/buspay_sync_token.txt
```

Replace the file content with this local validation token:

```text
module-15-local-bridge-token-2026
```

Start the service:

```bash
docker compose -f deployment/compose.yaml up --build -d
docker compose -f deployment/compose.yaml ps
curl http://127.0.0.1:8080/health
```

Expected: the container becomes `healthy` and health returns `"status":"ok"`.

### 2. Connect the Android device

Enable Developer options and USB debugging on the tablet, connect it by USB, accept the authorization dialog, then run:

```bash
adb devices -l
adb reverse tcp:8080 tcp:8080
adb reverse --list
```

Expected: the tablet is listed as `device` and the reverse list contains `tcp:8080 tcp:8080`.

`adb reverse` makes the tablet's `127.0.0.1:8080` reach the Mac's localhost Docker port. It is a development bridge, not a production network design.

### 3. Install and configure the debug app

Run the `app` configuration from Android Studio so the debug manifest is used.

In the driver console:

1. Find **Sync service**.
2. Press **Configure Sync Server**.
3. Keep the endpoint:

```text
http://127.0.0.1:8080/v1/sync
```

4. Enter:

```text
module-15-local-bridge-token-2026
```

5. Press **Activate Server**.

Confirm:

- **Active mode: Local server validation**;
- **Local validation server: configured**;
- the token field disappears and is cleared;
- **Use Demo Mode** is available for returning to the isolated demo client.

### 4. Validate failure retention

This is recommended before the success path:

1. Configure the server with a deliberately wrong token.
2. Close a short shift with at least one ticket.
3. Press **Sync Now**.
4. Confirm the app reports authentication rejection.
5. Confirm the pending shift and tickets remain waiting for sync.
6. Reconfigure with the correct token.

No record may be marked synchronized after a rejected request.

### 5. Validate live synchronization

1. Sign in a driver.
2. Start a shift.
3. Sell tickets across at least two fares.
4. End the shift.
5. Confirm one closed shift and its tickets are waiting for sync.
6. Press **Sync Now**.
7. Confirm the message reports the acknowledged shift and ticket counts.
8. Confirm the waiting counts return to zero.

Open [http://127.0.0.1:8080/admin](http://127.0.0.1:8080/admin) on the Mac. Authenticate with the same token and press **Refresh report**. Confirm the new driver shift, bus, route, fares, ticket records, and revenue appear in the shift ledger.

The browser does not push automatically in Module 15; **Refresh report** performs the latest authenticated report request.

### 6. Optional API confirmation

```bash
curl --fail --silent --show-error \
  http://127.0.0.1:8080/v1/reports/admin \
  -H 'Authorization: Bearer module-15-local-bridge-token-2026'
```

The returned JSON should contain the new shift and ticket identifiers.

### 7. Clean up

```bash
adb reverse --remove tcp:8080
docker compose -f deployment/compose.yaml down -v
rm deployment/secrets/buspay_sync_token.txt
docker compose -f deployment/compose.yaml ps --all
```

The final Compose list should be empty.

## Automated validation

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug

PYTHONPATH=server:. python3 -m unittest discover -s server/tests -v
```

## Security boundaries

- Demo mode remains the startup default.
- Cleartext is enabled only in the debug manifest.
- The client accepts cleartext only for loopback hosts.
- Non-loopback HTTP is rejected even in local validation mode.
- Production configuration accepts HTTPS only.
- Release builds retain `usesCleartextTraffic="false"`.
- The bearer token remains in memory for the running ViewModel session and is not persisted.
- A real production deployment still requires trusted HTTPS and short-lived identity credentials.

## Current limitations

- Server configuration is reset when the app process is recreated.
- The local bridge requires USB debugging and `adb reverse`.
- Browser updates require pressing **Refresh report**.
- The shared local token is only for development validation.
- Production domain, TLS, identity, token refresh, and certificate policy remain future infrastructure work.

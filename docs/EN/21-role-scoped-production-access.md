# Module 21 — Role-scoped production access

## Outcome

Module 21 replaces the shared local-pilot credential with three independent access boundaries:

- **Device sync** uploads closed shifts and reads the published operations catalog.
- **Report reader** opens the administrative report without catalog mutation rights.
- **Catalog administrator** reads and publishes managed drivers, buses, routes, stops, and fares without access to synchronized financial reports.

The API discovers the authenticated roles through `GET /v1/access` and enforces every permission server-side. Hiding a web control is presentation only; it is not the security boundary.

## Local validation

Create the three ignored secret files from their `.example` counterparts, keep each value on one line, and ensure the values are different. Start the stack with:

```bash
docker compose -f deployment/compose.yaml up --build -d
docker compose -f deployment/compose.yaml ps
```

Open `http://127.0.0.1:8080/admin`. A report-reader credential must show only reporting. A catalog-administrator credential must show only the managed catalog. A device credential must not open an administrative workspace.

On the tablet, configure `http://127.0.0.1:8080/v1/sync` with the device credential after activating `adb reverse tcp:8080 tcp:8080`. Catalog refresh and closed-shift sync must succeed. Administrative credentials must be rejected by the tablet workflow.

## Completion boundary

The module provides a provider-neutral least-privilege foundation. External identity federation, user lifecycle management, credential rotation, domain/TLS activation, and audit ownership remain deployment decisions after an infrastructure and identity provider are selected.

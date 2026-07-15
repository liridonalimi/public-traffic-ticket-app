# Module 14 - Authenticated Administrative Web Reporting Dashboard

## Purpose

Module 14 turns reporting contract version 1 into a real administrative web experience. The dashboard is served by the existing BusPay service, requests the protected report from the same origin, and renders reconciled operational totals without adding a JavaScript framework or another runtime service.

## What was added

- Responsive **BusPay Control** dashboard at `/admin`.
- Bearer-token connection screen with rejection and sign-out states.
- Overall revenue, ticket, closed-shift, and driver metrics.
- Fare mix with ticket counts and revenue.
- Per-driver shift, ticket, and revenue totals.
- Expandable shift ledger with individual ticket records.
- Driver and fare filters plus shift/bus/route search.
- Empty, loading, success, and authentication-error states.
- Multi-driver validation dataset in `server/sample-admin-sync.json`.
- Browser security headers and same-origin-only API access.

## Access and token behavior

The HTML, CSS, and JavaScript files are public so the sign-in screen can load. Report data remains protected at `/v1/reports/admin`.

The operator enters a bearer token into a password field. After successful authentication:

- the visible password field is cleared;
- the token remains only in the running page's JavaScript memory;
- it is not placed in the URL, cookies, `localStorage`, or `sessionStorage`;
- sign-out and authentication failure clear the in-memory token and report.

The shared token is appropriate only for local pilot validation. Production requires a real administrative identity provider with short-lived, role-scoped credentials.

## Local Docker double-test

Keep Docker Desktop running and start from the project root.

1. Run all server/deployment tests:

```bash
PYTHONPATH=server:. python3 -m unittest discover -s server/tests -v
```

Expected: **18 tests** and `OK`.

2. Create the ignored local secret:

```bash
cp deployment/secrets/buspay_sync_token.txt.example \
  deployment/secrets/buspay_sync_token.txt
```

Replace its content with:

```text
module-14-local-dashboard-token-2026
```

3. Build and start the service:

```bash
docker compose -f deployment/compose.yaml up --build -d
docker compose -f deployment/compose.yaml ps
```

Wait until the service is `healthy`.

4. Load the validation report:

```bash
curl -i -X POST http://127.0.0.1:8080/v1/sync \
  -H 'Authorization: Bearer module-14-local-dashboard-token-2026' \
  -H 'Content-Type: application/json' \
  -H 'X-BusPay-Contract-Version: 1' \
  -H 'Idempotency-Key: sync-2222333344445555' \
  --data-binary @server/sample-admin-sync.json
```

Expected: HTTP 200 and acknowledgements for 4 shifts and 12 tickets.

5. Open [http://127.0.0.1:8080/admin](http://127.0.0.1:8080/admin).

6. Enter a wrong token first. Confirm the page shows **The access token was rejected.** and does not reveal report data.

7. Enter `module-14-local-dashboard-token-2026`. Confirm:

- Revenue: **€4.00**
- Tickets: **12**
- Closed shifts: **4**
- Drivers: **3**
- driver-001: 2 shifts, 6 tickets, €2.25
- driver-002: 1 shift, 3 tickets, €0.80
- driver-003: 1 shift, 3 tickets, €0.95

8. Validate interactions:

- filter driver `driver-002` and confirm 1 of 4 shifts;
- combine `driver-002` with fare `standard` and confirm the empty state;
- reset filters and search `01-417-KS` to find `shift-admin-004`;
- expand the shift and confirm its three ticket records;
- refresh and confirm the totals remain unchanged;
- sign out and confirm the report disappears and the page returns to **Locked**.

9. Resize the browser or test on a narrow device. Cards, tables, filters, and shift details must remain readable without horizontal page overflow.

10. Clean up:

```bash
docker compose -f deployment/compose.yaml down -v
rm deployment/secrets/buspay_sync_token.txt
docker compose -f deployment/compose.yaml ps --all
```

The final list should be empty.

## Security controls

- The report API still requires bearer authentication.
- Static responses use `Cache-Control: no-store`.
- Content Security Policy allows scripts, styles, and API connections only from the same origin.
- Framing, referrer leakage, MIME sniffing, camera, location, and microphone access are restricted.
- Dynamic report values are inserted with DOM text APIs rather than HTML interpolation.
- The container remains non-root with a read-only root filesystem.

## Current limitations

- No role-based access control or short-lived administrative login yet.
- No export, pagination, date-range filters, charts over time, or audit log yet.
- Driver IDs, bus IDs, and route IDs are shown because the server does not yet own reference-name tables.
- Compose remains a localhost HTTP validation package; staging and production require trusted HTTPS.
- The dashboard is English-only in Module 14; the module documentation is bilingual.

## Next product phase

Deploy the service and dashboard to an owned staging environment behind HTTPS, connect role-based administrative identity, and add privacy, audit, export, monitoring, pagination, and governance controls.

## References

- [W3C Content Security Policy Level 3](https://www.w3.org/TR/CSP/)
- [MDN Cache-Control](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Cache-Control)
- [MDN X-Content-Type-Options](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/X-Content-Type-Options)

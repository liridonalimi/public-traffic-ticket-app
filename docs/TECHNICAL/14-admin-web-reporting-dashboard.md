# Module 14 - Administrative Web Reporting Dashboard

## Scope

Module 14 adds a same-origin administrative presentation layer to the dependency-free WSGI reporting service. It consumes the existing `/v1/reports/admin` contract without changing persisted records or synchronization semantics.

## Components

- `server/buspay_server/web/admin.html`: semantic login and dashboard structure.
- `server/buspay_server/web/admin.css`: responsive, dependency-free presentation and breakpoints.
- `server/buspay_server/web/admin.js`: in-memory authentication, report fetch, projection, filtering, expansion, refresh, and sign-out.
- `server/buspay_server/application.py`: allow-listed static routes and hardened static response headers.
- `server/sample-admin-sync.json`: deterministic 3-driver/4-shift/12-ticket acceptance dataset.
- `server/tests/test_service.py`: static-route, header, token-exclusion, method, and reconciliation regression coverage.

## Request flow

```text
GET /admin
  -> public static login shell
  -> operator enters bearer token
  -> GET /v1/reports/admin with Authorization header
  -> existing server authentication
  -> transactional SQLite reporting projection
  -> contract v1 JSON
  -> safe DOM text rendering
```

Static asset routes are explicitly allow-listed:

- `/admin`
- `/admin/`
- `/admin/assets/admin.css`
- `/admin/assets/admin.js`

Unsupported methods receive HTTP 405. Unknown assets receive the existing JSON 404 response.

## Authentication boundary

The static application shell is intentionally public; otherwise an unauthenticated operator could not load the sign-in UI. The reporting endpoint remains protected.

The submitted bearer token is assigned to a module-scoped JavaScript variable only. The password input is cleared after a successful response. No code writes credentials to URLs, cookies, `localStorage`, or `sessionStorage`. Sign-out, rejected authentication, reload, and process termination remove access to the token.

This limits persistence but does not replace production identity. The pilot shared secret must become a short-lived, role-scoped credential issued by the selected identity provider.

## Browser hardening

Every dashboard asset receives:

- `Cache-Control: no-store`
- `X-Content-Type-Options: nosniff`
- `Content-Security-Policy` with `default-src 'none'`
- same-origin `script-src`, `style-src`, and `connect-src`
- `base-uri 'none'`, `form-action 'none'`, and `frame-ancestors 'none'`
- `Referrer-Policy: no-referrer`
- `X-Frame-Options: DENY`
- disabled camera, geolocation, and microphone permissions
- same-origin opener/resource policies

The W3C CSP `connect-src` directive covers scripted network APIs such as `fetch`, so restricting it to `'self'` constrains the dashboard to the BusPay origin. `frame-ancestors 'none'` and `X-Frame-Options: DENY` provide modern and compatibility framing controls.

Dynamic API values use `textContent`, element properties, and DOM node creation. The dashboard does not use `innerHTML` for report data.

## Report projections

The browser does not recalculate authoritative totals. It displays `overall`, `fares`, and `drivers` directly from contract v1 and uses `shifts` for the filterable ledger.

Client-only filters do not mutate or re-request data:

- exact driver ID
- ticket-level fare membership
- case-insensitive substring across shift, driver, bus, and route IDs

Expanding a shift shows ticket ID, fare, price, and sale time. Currency uses `Intl.NumberFormat` with EUR. Dates use the browser locale formatter.

## Responsive and accessibility behavior

- Semantic regions, headings, tables, labels, status announcements, and native details/select controls.
- Keyboard focus indicators and skip navigation.
- Four/two/one-column metric transitions.
- Tables scroll within their panel rather than widening the page.
- Shift summary metadata progressively hides at narrower widths while ticket details remain available.
- Reduced-motion preference disables transitions.

## Validation coverage

Automated validation now contains 18 server/deployment tests and verifies:

- all public dashboard assets and MIME types;
- Content Security Policy, framing, referrer, and token-exclusion controls;
- unsupported dashboard methods;
- the deterministic acceptance dataset totals;
- all previous authentication, idempotency, persistence, backup, and deployment behavior.

Live Docker/browser validation covers:

- healthy image build and same-origin asset delivery;
- rejected and accepted bearer tokens;
- EUR 4.00 / 12 tickets / 4 shifts / 3 drivers;
- driver, fare, combined-empty, and text-search states;
- expandable ticket detail;
- refresh and sign-out;
- desktop and narrow responsive layouts.

The existing Android suite is also rerun because the dashboard shares the same deployed service and roadmap documentation.

## Deployment implications

The current Compose binding stays on `127.0.0.1:8080` for safe local validation. A staging deployment must provide trusted HTTPS, DNS, an identity issuer, secret management, monitoring, and authorization policy. No provider is selected or configured by this module.

## Remaining production work

- role-based administrative authorization
- short-lived credentials and session lifecycle
- audit events for access/export/filter operations
- privacy classification and retention rules
- server-side date filters, pagination, and export
- reference-name tables for drivers, buses, and routes
- monitoring, alerting, CSP reporting, and incident ownership

## References

- [W3C Content Security Policy Level 3](https://www.w3.org/TR/CSP/)
- [MDN Cache-Control](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Cache-Control)
- [MDN X-Content-Type-Options](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/X-Content-Type-Options)

# Authenticated HTTPS Sync Client

## Purpose

Module 11 provides the production transport foundation for sending closed shifts and tickets to a real server. It replaces no local data and does not embed deployment credentials. Instead, it adds a production `TransitSyncClient` implementation that can be activated when the operator supplies an HTTPS endpoint and an access token from a future authenticated session.

The existing demo client remains active in the pilot so synchronization can still be tested without a deployed backend.

## What was added

- HTTPS-only production endpoint configuration.
- Bearer-token authentication.
- Version 1 JSON request and acknowledgement contract.
- Stable idempotency key based on the existing sync batch request ID.
- Connection and read timeouts.
- Redirect blocking and a bounded response body.
- Safe handling of authentication, network, server, rate-limit, payload, and conflict failures.
- Strict matching of contract version and request ID.
- Rejection of acknowledgements for records outside the submitted batch.
- Android cleartext-traffic blocking.
- A console status card that distinguishes demo validation from production readiness.

## Request contract

The production client sends a `POST` request with:

- `Authorization: Bearer <access-token>`
- `Content-Type: application/json; charset=utf-8`
- `Accept: application/json`
- `Idempotency-Key: <stable-request-id>`
- `X-BusPay-Contract-Version: 1`

The body contains the contract version, request ID, send time, closed shifts, and tickets. Shift records include driver, bus, route, start, and end identifiers/times. Ticket records include shift, fare, price, and sale time.

## Acknowledgement rule

The server response must contain:

- contract version `1`
- the same request ID
- acknowledged shift IDs
- acknowledged ticket IDs

Only IDs present in the submitted batch are accepted. The offline repository continues to mark only acknowledged records as synchronized. Invalid, partial, rejected, or unreachable responses leave unacknowledged data locally available for retry.

## Security decisions

- HTTP endpoints are rejected before a request is created.
- URLs containing credentials or fragments are rejected.
- Empty or header-injection bearer tokens are rejected.
- Access tokens are placed only in the authorization header, never in the JSON payload or user-facing failure text.
- Redirects are not followed, preventing credentials from being forwarded to another host.
- The token is not persisted by this module and is not committed to the repository.

## Double-test checklist

1. Install and open the new debug build.
2. Scroll to **Total waiting for sync**.
3. Confirm the **Sync service** card shows **Active mode: Demo validation**.
4. Confirm it shows **Production HTTPS contract v1: ready**.
5. Confirm the activation note states that a production server URL and authenticated access token are required.
6. Close a shift with at least one ticket.
7. Tap **Go Offline**, then **Sync Now**, and confirm the failure leaves the shift and ticket waiting.
8. Tap **Go Online**, then **Sync Now**, and confirm the demo acknowledgement clears the waiting records.
9. Reopen **Admin Report Preview** and confirm the shift/tickets show `SYNCED`/`synced`.
10. Confirm all previous driver, printer, route, passenger-display, stop-request, and reporting flows still work.

## Current limitations

- A production server URL has not been supplied.
- Production identity and access-token issuance are not part of this local-driver-login pilot.
- The demo client remains the active transport until those deployment inputs exist.
- Certificate pinning and token refresh depend on the production identity and infrastructure design.
- The server database and reporting web application are separate upcoming deployment tasks.

## Next product phase

Deploy the authenticated synchronization endpoint and database, issue short-lived access tokens through production identity, and instantiate `ProductionTransitSyncClient` with those runtime values.

# Module 22 — Credential rotation and authorization audit (technical)

**Status: complete.** The final independent validation exercised overlap, client migration, revocation, and post-revocation continuity for both the device and audit roles.

## Credential bundles

`runtime._load_token_bundle` accepts one or two non-empty lines per role secret. `BusPayApplication` normalizes every role to a tuple of byte values and performs constant-time comparisons against all configured values before determining the principal roles.

The four required role files are device, report, catalog, and audit. The device bundle maps to both `device_sync` and `catalog_read`; the security bundle maps only to `audit_read`. Tokens must be unique across every bundle.

The two-line format is intentionally bounded. It supports an old/new overlap without turning a secret file into an unmanaged credential registry.

## Audit persistence

SQLite table `authorization_events` stores:

- monotonic event ID and wall-clock milliseconds;
- `allowed`, `forbidden`, or `unauthenticated` outcome;
- bounded HTTP method/path and source values;
- sorted authenticated roles as JSON.

`BusPayApplication._authorize` records the decision before returning the protected response. The `GET /v1/audit?limit=N` endpoint requires `audit_read`, accepts limits from 1 to 500, and returns newest-first events under contract version 1. Reading the audit is itself audited.

No bearer value, authorization header, request body, or response body is persisted. Database backup/restore preserves the audit table with the rest of the operational database.

The database retains at most the latest 10,000 authorization events and prunes in batches every 100 inserts, avoiding an unbounded local audit table while preserving a useful operational window.

## Web isolation

The admin page discovers `audit_read` through `/v1/access`, fetches only `/v1/audit`, and renders a security-auditor workspace. Report and catalog sections remain hidden, while the API independently returns HTTP 403 for those endpoints.

## Validation

- unit tests cover persistent events, limits, no-token output, 401/403/200 decisions, and both rotation values;
- runtime and preflight tests cover four complete role files, one/two-line bundles, duplicates, partial configuration, weak values, and unsafe permissions;
- deployment tests verify the fourth mounted secret and backup/restore preservation;
- staging smoke optionally verifies the audit contract and bidirectional report/audit isolation;
- browser validation verifies the isolated audit workspace and absence of credential text.
- independent tablet/web validation confirmed that old and rotated credentials both worked during overlap, old credentials returned HTTP 401 after removal, and rotated credentials retained catalog, synchronization, and audit access.

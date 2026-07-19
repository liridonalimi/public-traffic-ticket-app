# Module 22 — Credential rotation and authorization audit

**Status: complete.** Automated verification and independent tablet/web validation covered the overlap window, migration to rotated credentials, rejection of revoked credentials, catalog refresh, synchronization, role isolation, and audit updates.

## Outcome

Module 22 adds two provider-neutral production controls:

- each role secret file may contain one active credential or two distinct credentials during a controlled rotation window;
- every protected API authorization decision is stored as allowed, forbidden, or unauthenticated and can be reviewed through an isolated security-auditor workspace.

Credentials are never written to the database, API response, browser storage, URL, or audit table. Audit records contain only the time, outcome, HTTP method/path, authenticated roles, and request source. Local retention is bounded to the latest 10,000 authorization events.

## Rotation protocol

For the role being rotated:

1. Keep the existing credential on line 1 of its protected file.
2. Add the replacement credential on line 2.
3. Restart the service and verify that both credentials work.
4. Move every authorized client to the replacement credential.
5. Remove the old first line, restart, and verify that the old credential returns HTTP 401 while the replacement remains valid.

The staging preflight rejects more than two values, duplicates within one file, reuse across roles, weak values, and permissive file permissions.

## Local validation

Start Docker and open `http://127.0.0.1:8080/admin`. Use the security-auditor credential from the ignored local secret file. Only **Authorization audit** must be visible; reporting and catalog controls must be absent.

Generate events by signing out and trying:

- the report credential in the report workspace;
- the device credential in the web page, which is authenticated but has no administrative workspace;
- an invalid credential;
- the security-auditor credential again.

Refresh the audit. It must show allowed, forbidden, and unauthenticated decisions without showing any credential text. Test both old and rotated device credentials on the tablet; both must refresh the catalog and synchronize during the overlap window.

Independent validation completed the full cycle: both credentials worked during overlap, clients moved to the rotated credentials, the old audit and device credentials were removed, and the final server state rejected both old credentials while the rotated credentials continued to authorize the intended operations.

## Completion boundary

This module defines and verifies the local rotation/audit protocol. Hosted log export, long-term retention policy, alerting, operator identity federation, revocation automation, and SIEM integration depend on the selected production infrastructure and identity provider.

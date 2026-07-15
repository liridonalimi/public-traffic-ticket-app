# Module 13 Technical Notes: Production Deployment Package

## Runtime topology

```text
Android ProductionTransitSyncClient
            |
       trusted HTTPS
            |
managed ingress / load balancer
            |
private Gunicorn container :8080
            |
persistent relational storage + backup volume
```

Compose intentionally binds `127.0.0.1:8080` for workstation validation. A real deployment should remove public port publication and route only private ingress traffic to port 8080.

## Deployment assets

- `deployment/Dockerfile`: pinned base, non-root user, health check, Gunicorn command.
- `deployment/Dockerfile.dockerignore`: excludes source-control, Android, build, backup, and secret material.
- `deployment/requirements.txt`: exact Gunicorn runtime version.
- `deployment/gunicorn.conf.py`: bounded concurrency, timeouts, request parsing, recycling, and stdout/stderr logs.
- `deployment/compose.yaml`: secret mount, data/backup volumes, read-only root, dropped capabilities, health check, local binding.
- `deployment/backup_sqlite.py`: online SQLite backup API, integrity check, atomic rename, SHA-256 sidecar.
- `deployment/restore_sqlite.py`: checksum/integrity verification, refusal to overwrite by default, preservation of replaced database, atomic restore.

## Secret loading

`buspay_server.runtime.create_application()` accepts either:

- `BUSPAY_SYNC_TOKEN_FILE` (preferred), or
- `BUSPAY_SYNC_TOKEN` (development fallback).

Configuring both is rejected. The secret file is size-bounded, read once, trimmed, never written to SQLite, and never returned by API errors. Compose mounts it read-only under `/run/secrets`.

## Container controls

- exact Python `3.12.13-slim-trixie` tag
- application UID/GID `10001`
- root filesystem read-only at runtime
- writable mounts limited to `/data`, `/backups`, and a 16 MB `/tmp` tmpfs
- all Linux capabilities dropped
- `no-new-privileges`
- build-context exclusion of actual secret files
- service restart policy and 40-second graceful stop window
- health probe against the non-sensitive `/health` endpoint

Base images should additionally be pinned by digest in the target registry after the deployment platform/architecture is selected.

## Gunicorn controls

Gunicorn uses two `gthread` workers with four threads each by default. Environment variables can adjust worker/thread counts after load testing. Requests time out after 30 seconds, workers recycle after approximately 2,000 requests, request header counts/sizes are bounded, and logs are emitted to container stdout/stderr.

## Android activation boundary

`SyncRuntimeConfig` and `createTransitSyncClient()` now create either:

- `DemoTransitSyncClient`, or
- `ProductionTransitSyncClient` with validated HTTPS endpoint/token.

The current `DriverShiftViewModel` explicitly uses `SyncRuntimeConfig.demo()`. Production identity should provide the short-lived token to the composition root and select `SyncRuntimeConfig.production()`; tokens must not be Gradle fields, resources, source, screenshots, or persistent demo preferences.

## Backup and restore

Backups use SQLite's online backup API, not raw copying of a live database/WAL pair. Every completed backup receives a SHA-256 sidecar. Restore verifies checksum and SQLite integrity before mutation. Existing targets are refused unless an operator explicitly selects replacement, at which point the old database is preserved with a timestamped suffix.

Production restore procedure:

1. Announce maintenance and stop writers.
2. Confirm target environment and backup timestamp/checksum.
3. Take a pre-restore backup.
4. Run restore with explicit replacement approval.
5. Start one instance and run health/report reconciliation.
6. Run a test idempotent sync.
7. Scale service and monitor errors/latency.
8. Retain the preserved pre-restore database until sign-off.

## Validation coverage

Module 13 adds tests for:

- file-secret loading and non-persistence
- missing, unreadable, and ambiguous secret rejection
- backup/restore reconciliation
- existing-target and tampered-checksum rejection
- pinned/non-root container runtime
- read-only, capability, persistence, secret, and health Compose invariants
- build-context credential exclusion
- Android demo/production client selection and HTTPS enforcement

The local Docker double-test passed on Docker Desktop 4.82.0, covering Compose validation, image build, non-root health, authenticated ingestion, idempotent replay, restart persistence, and backup creation. Registry image scanning remains an explicit CI/deployment responsibility.

## External decisions still required

- cloud/runtime and region
- domain and DNS ownership
- managed TLS ingress
- identity issuer, audience, scopes, and token lifecycle
- SQLite approval or managed PostgreSQL migration
- provider secret manager
- logging, metrics, alerts, and on-call ownership
- backup retention, encryption, and recovery objectives
- image registry, vulnerability scanning, rollout, and rollback policy

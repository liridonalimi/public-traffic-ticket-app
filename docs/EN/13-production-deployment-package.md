# Production Deployment Package

## Purpose

Module 13 turns the Module 12 reference service into a provider-neutral deployment package. It introduces a production WSGI runtime, hardened container/Compose configuration, file-mounted secrets, persistent volumes, health checks, runtime client selection, and verified backup/restore tools.

No public infrastructure is changed by this module. The package is ready for a selected hosting platform and managed HTTPS ingress, but a real domain, identity issuer, database policy, and deployment account must be supplied by the infrastructure owner.

## What was added

- Pinned Python 3.12.13 slim container base.
- Pinned Gunicorn 26.0.0 WSGI runtime.
- Non-root UID/GID `10001` execution.
- Read-only container filesystem with all Linux capabilities dropped.
- `no-new-privileges` enforcement.
- Localhost-only published test port.
- Persistent database and backup volumes.
- File-mounted bearer-token secret excluded from Git and the build context.
- Container and Compose health checks.
- Controlled worker, thread, timeout, request-limit, and logging defaults.
- Atomic, integrity-checked SQLite backup and restore utilities with SHA-256 verification.
- Runtime configuration factory shared by development and Gunicorn.
- Android demo/production `TransitSyncClient` selection boundary.
- Visible app status: **Deployment package: ready • infrastructure selection pending**.

## Local container double-test

Docker is required for this part.

1. Copy `deployment/secrets/buspay_sync_token.txt.example` to `deployment/secrets/buspay_sync_token.txt`.
2. Replace its content with a long random local test secret. Do not commit this file.
3. Validate configuration:

```bash
docker compose -f deployment/compose.yaml config
```

4. Build and start:

```bash
docker compose -f deployment/compose.yaml up --build -d
```

5. Confirm container health:

```bash
docker compose -f deployment/compose.yaml ps
curl http://127.0.0.1:8080/health
```

6. Send `server/sample-sync.json` using the secret you placed in the file:

```bash
curl -i -X POST http://127.0.0.1:8080/v1/sync \
  -H 'Authorization: Bearer YOUR_LOCAL_SECRET' \
  -H 'Content-Type: application/json' \
  -H 'X-BusPay-Contract-Version: 1' \
  -H 'Idempotency-Key: sync-1111222233334444' \
  --data-binary @server/sample-sync.json
```

7. Run it twice and confirm `X-Idempotent-Replay` changes from `false` to `true`.
8. Restart the service and confirm the report remains populated:

```bash
docker compose -f deployment/compose.yaml restart
curl http://127.0.0.1:8080/v1/reports/admin \
  -H 'Authorization: Bearer YOUR_LOCAL_SECRET'
```

9. Create a backup:

```bash
docker compose -f deployment/compose.yaml exec buspay-sync \
  python deployment/backup_sqlite.py \
  --database /data/buspay.db \
  --output-directory /backups
```

10. Confirm the app status line appears and existing demo synchronization still works.

## Non-container validation

If Docker is unavailable, run:

```bash
PYTHONPATH=server:. python3 -m unittest discover -s server/tests -v
```

This validates runtime secrets, container invariants, transactional persistence, backup/restore, authentication, idempotency, and reports without launching an image.

## Production rollout checklist

Before exposing the service publicly:

- Select a hosting provider and region.
- Assign a production domain.
- Terminate trusted HTTPS at a managed load balancer/ingress.
- Keep container port 8080 private.
- Replace the shared environment secret with scoped, expiring identity-provider tokens.
- Select managed relational storage or formally approve the persistent SQLite limits.
- Store secrets in the provider secret manager.
- Schedule backups and test restoration in a separate environment.
- Configure logs, metrics, uptime alerts, database-capacity alerts, and incident ownership.
- Run database and API smoke tests before switching the Android client.
- Use `SyncRuntimeConfig.production()` only with the trusted HTTPS endpoint and an authenticated runtime token.
- Retain the previous image and database backup for rollback.

## Current limitations

- The local Docker double-test passed on Docker Desktop 4.82.0: healthy non-root startup, authenticated ingestion, idempotent replay, restart persistence, and writable backup volume.
- Compose exposes only localhost HTTP for local validation; production Android continues to require trusted HTTPS.
- SQLite remains the packaged persistence adapter. A managed PostgreSQL adapter requires an infrastructure decision and migration plan.
- Token issuance, DNS, certificates, monitoring accounts, and cloud deployment need external ownership/credentials.
- The Android pilot still starts in demo mode until production identity supplies a short-lived token.

## Next product phase

Choose the production infrastructure and identity provider, run this package in staging behind trusted HTTPS, complete backup/rollback tests, and then perform a real Android-to-server synchronization.

## Implementation references

- [Official Python container image](https://hub.docker.com/_/python/)
- [Docker Compose service and secret settings](https://docs.docker.com/reference/compose-file/services/)
- [Gunicorn package](https://pypi.org/project/gunicorn/)
- [Gunicorn settings](https://docs.gunicorn.org/en/stable/settings.html)

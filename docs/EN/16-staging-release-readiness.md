# Module 16 - Staging Release Readiness

## Purpose

Module 16 creates a safe release gate between the validated local pilot and a future hosted staging environment. It does not claim that a public environment exists. It makes the deployment inputs explicit, rejects unsafe configuration, and provides repeatable post-deploy checks.

## What was added

- A staging Compose profile that runs an immutable registry image.
- Private ingress-network attachment without a public host port.
- The existing read-only root, non-root user, dropped capabilities, secret mount, persistent data, backups, health check, and graceful shutdown controls.
- A preflight validator for the HTTPS origin, image digest, protected token file, region, retention period, and named operations/security/backup owners.
- A non-mutating staging smoke test for health, database readiness, authenticated reporting, contract shape, cache protection, and invalid-token rejection.
- A tracked example environment containing placeholders but no credential.

## Local double-test

Run this before staging or committing Module 16.

### 1. Run all automated tests

```bash
PYTHONPATH=server:. python3 -m unittest discover -s server/tests -v

JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

### 2. Validate a safe staging configuration

Create temporary operator-owned files outside the repository:

```bash
printf '%s\n' 'module-16-staging-validation-token-2026' > /tmp/buspay-staging-token
chmod 600 /tmp/buspay-staging-token

cp deployment/staging.env.example /tmp/buspay-staging.env
```

Edit `/tmp/buspay-staging.env` and use:

```text
BUSPAY_STAGING_BASE_URL=https://staging.buspay.test
BUSPAY_IMAGE=registry.buspay.test/sync@sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
BUSPAY_EDGE_NETWORK=buspay-staging-edge
BUSPAY_DEVICE_TOKEN_FILE=/tmp/buspay-staging-device-token
BUSPAY_REPORT_TOKEN_FILE=/tmp/buspay-staging-report-token
BUSPAY_CATALOG_TOKEN_FILE=/tmp/buspay-staging-catalog-token
BUSPAY_STAGING_REGION=eu-validation-1
BUSPAY_OPERATIONS_OWNER=operations-team
BUSPAY_SECURITY_OWNER=security-team
BUSPAY_BACKUP_OWNER=database-team
BUSPAY_BACKUP_RETENTION_DAYS=14
BUSPAY_WEB_WORKERS=2
BUSPAY_WEB_THREADS=4
```

Run:

```bash
PYTHONPATH=. python3 -m deployment.staging_preflight \
  --env-file /tmp/buspay-staging.env

docker compose --env-file /tmp/buspay-staging.env \
  -f deployment/compose.staging.yaml config
```

Expected: preflight prints `BusPay staging preflight: READY`; the rendered Compose service has `expose: 8080`, no host `ports`, a file secret, persistent volumes, and the external private ingress network.

### 3. Validate the smoke command against local Docker

The local-only flag is intentionally hidden and accepts HTTP only on loopback:

```bash
cp deployment/secrets/buspay_device_token.txt.example deployment/secrets/buspay_device_token.txt
cp deployment/secrets/buspay_report_token.txt.example deployment/secrets/buspay_report_token.txt
cp deployment/secrets/buspay_catalog_token.txt.example deployment/secrets/buspay_catalog_token.txt

docker compose -f deployment/compose.yaml up --build -d

PYTHONPATH=. python3 -m deployment.staging_smoke \
  --base-url http://127.0.0.1:8080 \
  --report-token-file deployment/secrets/buspay_report_token.txt \
  --allow-local-http
```

Expected: `BusPay staging smoke: PASS`, report totals, and `Invalid token: rejected`.

Clean up:

```bash
docker compose -f deployment/compose.yaml down -v
rm deployment/secrets/buspay_device_token.txt deployment/secrets/buspay_report_token.txt deployment/secrets/buspay_catalog_token.txt
rm /tmp/buspay-staging-device-token /tmp/buspay-staging-report-token /tmp/buspay-staging-catalog-token /tmp/buspay-staging.env
```

## Hosted staging activation

After choosing infrastructure, registry, domain, TLS ingress, and owners:

1. Build and scan the container image in CI.
2. Push it to the selected registry and record its digest.
3. Create the staging token in the provider secret manager.
4. Create persistent data and backup storage.
5. Attach the service only to the private ingress network.
6. Terminate trusted HTTPS at managed ingress.
7. Complete and run `staging_preflight`.
8. Render and review `compose.staging.yaml` or map its controls to the provider deployment specification.
9. Deploy one instance and wait for health.
10. Run `staging_smoke` against the HTTPS origin without `--allow-local-http`.
11. Configure one debug Android session with the HTTPS staging endpoint, close a test shift, synchronize it, and rerun the smoke/report check.
12. Record the image digest, owners, backup policy, smoke output, and rollback approval.

## Rollback gate

If health, authentication, report reconciliation, or Android synchronization fails:

- stop the rollout;
- keep the failed image digest for investigation;
- restore the previous immutable image digest;
- do not restore or delete the database unless data integrity is independently affected;
- rerun the non-mutating smoke test;
- document the incident and owner decision.

## Current boundary

The repository is ready for a staging release, but the first hosted deployment still requires an infrastructure provider, registry, domain/DNS owner, managed TLS, secret manager, storage decision, and accountable operational owners. Those external choices must not be fabricated in source code.

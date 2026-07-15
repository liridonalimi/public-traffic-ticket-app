# Module 16 Technical Notes: Staging Release Readiness

## Release topology

```text
Android HTTPS client / admin browser
                  |
          managed TLS ingress
                  |
      external private edge network
                  |
      BusPay immutable image :8080
          |                 |
   persistent data     backup storage
```

`compose.staging.yaml` deliberately uses `expose`, not `ports`. The application is reachable only by a separately owned ingress attached to the named external edge network. Provider-specific ingress and TLS configuration remain outside this repository.

## Immutable release input

`BUSPAY_IMAGE` must use `registry/repository@sha256:<64 lowercase hex characters>`. Tags such as `latest`, `staging`, or a semantic version alone are rejected because they can be moved after approval. Rollback selects a previously recorded digest.

## Preflight contract

`StagingConfiguration` rejects:

- non-HTTPS, loopback, credential-bearing, or path-bearing staging origins;
- mutable image references;
- absent or placeholder region/network/owner fields;
- missing, short, multiline, group-readable, or world-readable token files;
- backup retention outside 7-365 days.

The safe summary never reads the token value back into output. Environment overrides allow a deployment system to inject controlled values over an operator file.

## Smoke contract

`staging_smoke` is intentionally non-mutating. It verifies:

1. public `/health` returns HTTP 200, contract version, database readiness, and `Cache-Control: no-store`;
2. authenticated `/v1/reports/admin` returns HTTP 200 and the contract-v1 overall totals;
3. the report is non-cacheable;
4. a deliberately incorrect bearer token returns HTTP 401.

It does not create a synthetic shift in staging. End-to-end mutation is performed explicitly from a controlled Android test shift after the non-mutating gate passes.

Production smoke checks require HTTPS. `--allow-local-http` is hidden, restricted to exact loopback hosts, and exists only for workstation validation against Docker Desktop.

## Secret boundary

The tracked `.env.example` contains no usable token. The actual token file must remain outside source control, be owner-readable only, and should be materialized by the selected secret manager. Compose mounts it under `/run/secrets`; it is not placed in an environment variable, image layer, command argument, URL, report, or smoke output.

## Operational ownership

The preflight requires separate recorded values for:

- operations owner: deployment, availability, and rollback;
- security owner: TLS, token issuance/revocation, and incident response;
- backup owner: retention, restore tests, and recovery approval.

Names in a file are not authorization. The selected organization must map them to real teams and an auditable change process.

## Rollout and rollback

The initial rollout is one instance. Health and authenticated smoke checks run before Android validation or scaling. A failed release rolls back the image digest without destructive database restoration. Database restore follows the Module 13 integrity procedure only when an independently confirmed data incident requires it.

## Tests

Module 16 tests cover:

- accepted, redacted, operationally owned staging configuration;
- rejection of HTTP origins, mutable images, placeholders, weak tokens, and permissive file modes;
- health/report/invalid-token smoke flow;
- cleartext rejection outside explicit local loopback validation;
- staging Compose invariants for immutable image input, file secrets, private ingress, hardening, and absence of host port publication.

## External completion criteria

Source implementation is complete when local tests pass. Hosted staging is complete only when the actual infrastructure/domain/identity owners supply values, CI produces a scanned digest, managed HTTPS is active, persistent storage and backups exist, smoke passes, Android synchronizes a controlled shift, and rollback evidence is recorded.

# Module 20 Technical Notes: Central Operations Catalog

## Architecture

Module 20 adds a server-owned reference-data boundary alongside the existing synchronization and reporting contracts:

```text
BusPay Control -- GET/PUT /v1/catalog --> SQLite catalog tables
       Tablet -- GET     /v1/catalog --> validated local catalog cache
       Driver workflow ----------------> last successful local catalog
```

The catalog uses contract version 1 and an integer `revision`. `GET /v1/catalog` returns the complete snapshot. `PUT /v1/catalog` requires `expectedRevision`; the database uses `BEGIN IMMEDIATE`, validates the expected revision, replaces all catalog tables, increments the revision, and commits once.

## Persistence schema

- `catalog_metadata(singleton, revision, updated_at_millis)`
- `catalog_drivers(id, name)`
- `catalog_buses(id, plate_number)`
- `catalog_routes(id, name)`
- `catalog_stops(id, route_id, name, latitude, longitude, stop_order)`
- `catalog_fares(id, name, price_cents, eligibility)`

Catalog tables are intentionally independent from historical shift/ticket foreign keys. Renaming or removing a currently managed record does not destroy synchronized history. Existing databases are migrated additively and receive the baseline pilot catalog only when catalog metadata is absent.

## Validation boundary

`parse_catalog` enforces bounded, nonempty arrays; unique IDs; valid coordinate and price ranges; existing route references; at least one stop per route; and unique stop order inside a route. The Android `ManagedCatalog.isOperationallyValid` check provides a second fail-closed boundary before persistence or UI application.

## Android integration

`ManagedCatalogClient` derives `/v1/catalog` from the configured same-origin `/v1/sync` endpoint and reuses its bearer token, TLS/loopback rules, and timeouts. `OfflineFirstRepository` stores the successful snapshot. `DriverShiftViewModel` applies it only between shifts, preserves selections by stable ID where possible, and signs out a removed driver.

The app deliberately does not poll or refresh during service. This prevents price, stop sequence, bus, or driver definitions from changing during a shift. A future production control plane may add signed catalog versions and scheduled effective dates.

## Admin UI

The dependency-free admin client keeps edits in memory and publishes a complete snapshot. It uses DOM construction rather than HTML injection for catalog values. Tokens remain in page memory only. Optimistic concurrency prevents two open browser sessions from silently overwriting each other.

## Automated validation

Server tests cover schema seeding, persistence across restart, transactional replacement, stale-revision conflict, authentication, and catalog validation. Android tests cover endpoint derivation and catalog completeness/uniqueness. The standard gate remains:

```bash
PYTHONPATH=server python3 -m unittest discover -s server/tests

JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

The deployment asset test expects the ignored local token file to be absent. During local Docker/tablet validation, run the service/domain suites separately or remove the token only after finishing the manual test.

## Regression boundaries

Module 20 does not change ticket IDs, shift closure, print durability, sync idempotency, reporting totals, backup format, or the staging HTTPS activation gate. The pilot still uses one shared bearer token; production role-scoped identity remains a separate deployment requirement.

## Validation record

On 18 July 2026, operator validation confirmed the full local path: authenticated Admin Dashboard catalog access and publication, Docker persistence, USB-connected `adb reverse` access to port 8080, authenticated tablet catalog refresh, offline catalog application, and pending-shift synchronization. Module 20 is complete.

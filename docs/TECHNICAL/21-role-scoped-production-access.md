# Module 21 — Role-scoped production access (technical)

## Authorization model

The runtime loads three distinct secret files and maps them to API capabilities:

| Credential | Server roles | Allowed endpoints |
| --- | --- | --- |
| Device | `device_sync`, `catalog_read` | `POST /v1/sync`, `GET /v1/catalog`, `GET /v1/access` |
| Report reader | `report_read` | `GET /v1/reports/admin`, `GET /v1/access` |
| Catalog administrator | `catalog_write` | `GET/PUT /v1/catalog`, `GET /v1/access` |

Unknown credentials return HTTP 401. An authenticated credential without the required role returns HTTP 403. The application keeps constant-time token comparisons and never returns token values through the access endpoint.

The role-file environment variables are:

- `BUSPAY_DEVICE_TOKEN_FILE`
- `BUSPAY_REPORT_TOKEN_FILE`
- `BUSPAY_CATALOG_TOKEN_FILE`

All three are required together, must contain single-line values, and must be distinct. The legacy shared-token configuration remains accepted by the Python runtime only for controlled migration, but the Compose profiles use role files.

## Web behavior

The admin page first calls `GET /v1/access`. It then requests only the permitted resource and keeps the credential in JavaScript memory. Report controls and catalog controls are mutually hidden, but endpoint authorization is independently tested and remains authoritative.

## Validation coverage

- service tests cover 401/403 behavior and every role/endpoint combination;
- deployment tests cover complete, partial, duplicate, and legacy secret configuration;
- staging preflight validates permissions, length, single-line content, and uniqueness for all three files;
- Docker validation exercises the role endpoints against the real container;
- Android tests distinguish an invalid credential from an authenticated credential without catalog permission.

The staging smoke command now accepts `--report-token-file`; `--token-file` remains a compatibility alias.

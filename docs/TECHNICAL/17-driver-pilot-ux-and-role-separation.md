# Module 17 Technical Notes: Driver Pilot UX and Role Separation

## Scope

Module 17 reorganizes the existing Compose UI without changing the persistence, ticketing, printing, route, or synchronization contracts.

## Workspace policy

`PilotRolePolicy.kt` defines:

- `PilotWorkspace.DRIVER` and `PilotWorkspace.OPERATIONS`;
- `canOpenOperationsTools`, which denies supervisor workspace access while a shift or print operation is active;
- `driverShiftStatus`, which produces the primary driver state;
- `driverSyncSummary`, which converts runtime synchronization state into concise driver language.

These functions are pure and covered by JVM unit tests.

## Compose navigation boundary

`DriverHomeScreen` owns an in-memory workspace selection. The driver workspace is always the default. Operations tools can be entered only between shifts. If the state changes to active service, the operations branch is no longer rendered.

The operations workspace contains the controls moved out of the driver workflow:

- sync runtime details;
- endpoint and session-token activation;
- demo/local validation switches;
- manual sync diagnostics;
- in-app admin report navigation.

The driver workspace retains a safe `Sync Closed Shifts` action when pending closed shifts exist. It does not expose credentials or environment selection.

## Shift-ending guard

The bottom action now opens a Material 3 `AlertDialog`. `endShift()` is called only after explicit confirmation. Existing constraints still prevent closure while printing or while unprinted tickets remain.

## Security boundary

The separation is intentionally described as a pilot workflow boundary. It does not assert that a user is an authorized supervisor. Production identity, short-lived credentials, and server-side authorization remain future work.

## Automated validation

Run:

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

The added `PilotRolePolicyTest` verifies active-shift blocking, between-shift access, primary status, and concise synchronization language.

## Regression boundaries

Module 17 does not change:

- shift or ticket database schemas;
- sync payloads, acknowledgement rules, or server API;
- printer selection and durable print state;
- route progress or passenger display state;
- administrative reporting calculations.

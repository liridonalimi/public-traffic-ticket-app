# Technical: Driver Login and Identity

## Scope

This module adds a local driver identity flow to the Android pilot. It does not add backend authentication yet. The goal is to make every new shift use a selected driver ID instead of a hard-coded demo driver.

## Main files

- `app/src/main/java/com/buspay/app/data/OfflineFirstRepository.kt`
  - Stores the signed-in driver in `SharedPreferences`.
  - Loads the signed-in driver after app restart.
  - Clears the signed-in driver on sign-out.

- `app/src/main/java/com/buspay/app/ui/screens/DriverShiftViewModel.kt`
  - Holds available demo drivers.
  - Tracks selected driver and signed-in driver separately.
  - Blocks shift start until a driver is signed in.
  - Saves signed-in driver identity locally.
  - Restores the driver from the active shift when a shift is already running.

- `app/src/main/java/com/buspay/app/ui/screens/DriverHomeScreen.kt`
  - Shows a driver selector before sign-in.
  - Shows signed-in driver name and ID after sign-in.
  - Allows sign-out only when there is no active shift.

## State model

`DriverShiftUiState` now contains:

- available drivers
- selected driver
- signed-in driver
- active shift and ticket state from previous modules

Derived values include:

- `isDriverSignedIn`
- `isShiftActive`

## Business rules in this module

- A driver must be signed in before a shift can start.
- A driver can be selected only before sign-in.
- The signed-in driver cannot sign out during an active shift.
- A restored active shift restores the driver identity from the shift driver ID.
- New shifts store the signed-in driver ID in the `Shift` model.

## Limitations

- Driver data is still local demo data.
- There is no PIN, password, card scan, or backend session token yet.
- There is no role or permission model yet.
- Login and logout audit events are not stored yet.

## Testing and validation

- Assert shift start is rejected when `signedInDriver` is null.
- Persist a selected driver, recreate the application state, and assert the same ID is restored.
- Start a shift and assert driver selection and sign-out paths do not mutate identity.
- Restore an active shift and assert its `driverId` takes precedence over a stale selected-driver value.
- Close the shift, sign out, select another driver, and assert each closed shift retains its original driver ID.

Expected invariant: driver attribution is copied into the shift at start and cannot change during that shift.

## Next technical step

Add ticket fare types and discounts so ticket sales can use more than one fixed standard price.

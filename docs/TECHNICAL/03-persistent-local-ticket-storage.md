# Technical: Persistent Local Ticket Storage

## Scope

This module adds local persistence to the driver shift flow. It keeps the active shift and sold cash tickets in Android app storage so the current work session can survive an app restart.

## Main files

- `app/src/main/java/com/buspay/app/data/OfflineFirstRepository.kt`
  - Stores the active shift in `SharedPreferences`.
  - Stores sold tickets as a local JSON array.
  - Loads tickets by shift ID.
  - Counts unsynced tickets for future backend sync.

- `app/src/main/java/com/buspay/app/ui/screens/DriverShiftViewModel.kt`
  - Uses `AndroidViewModel` so it can access app-local storage through the repository.
  - Restores an active shift during startup.
  - Restores tickets for the active shift.
  - Saves the active shift when a shift starts.
  - Saves each ticket when it is sold.
  - Clears only the active shift marker when the shift ends, leaving tickets available for future sync.

- `app/src/main/java/com/buspay/app/ui/screens/DriverHomeScreen.kt`
  - Displays the number of locally saved tickets waiting for sync.

## Storage model

The repository currently uses two keys:

- `active_shift`: one JSON object for the currently active shift.
- `tickets`: one JSON array containing locally saved tickets.

Each ticket keeps `synced = false`. This preserves a queue of tickets that a future sync module can send to the server.

## Business rules in this module

- Starting a shift saves the shift locally.
- Selling a ticket immediately saves the ticket locally.
- Restarting the app restores the active shift and its tickets.
- Ending a shift clears the active shift marker but keeps tickets stored as unsynced.

## Limitations

- Storage uses `SharedPreferences`, which is acceptable for the pilot but not the final larger data layer.
- There is no server sync yet.
- There is no ticket deduplication or conflict handling yet.
- Local data corruption handling is minimal.

## Next technical step

Add driver login and driver identity so tickets are linked to a real driver instead of the current demo driver.

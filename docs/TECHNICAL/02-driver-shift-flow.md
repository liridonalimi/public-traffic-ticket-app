# Technical: Driver Shift Flow

## Scope

This module implements the first interactive driver workflow in the native Android app. It is intentionally local-only and uses demo data so the UI and domain flow can be validated on a real tablet before adding persistence and server sync.

## Main files

- `app/src/main/java/com/buspay/app/ui/screens/DriverHomeScreen.kt`
  - Jetpack Compose screen for the driver console.
  - Displays selected bus, selected route, next stop, ticket count, cash total, and last closed shift summary.
  - Provides actions for starting a shift, selling tickets, and ending a shift.

- `app/src/main/java/com/buspay/app/ui/screens/DriverShiftViewModel.kt`
  - Holds UI state with Compose `mutableStateOf`.
  - Owns the first version of the shift business flow.
  - Contains temporary demo buses, demo routes, and the standard ticket price.

- `app/src/main/java/com/buspay/app/domain/Models.kt`
  - Defines shared domain models: `Driver`, `Bus`, `Route`, `Stop`, `Shift`, `Ticket`, and `DriverShiftSummary`.

## State model

`DriverShiftUiState` contains:

- current demo driver
- available buses
- available routes
- selected bus
- selected route
- active shift, when one is running
- tickets sold in the active shift
- last closed shift summary

Derived values include:

- `isShiftActive`
- `nextStopName`
- `ticketCount`
- `cashTotalCents`

## Business rules in this module

- Bus and route can only be changed before the shift starts.
- A shift can only start if both bus and route are selected.
- A ticket can only be sold during an active shift.
- Each demo ticket has a fixed price of 50 cents.
- Ending the shift clears active tickets and stores a summary for display.

## Limitations

- No permanent local storage yet.
- No backend sync yet.
- No driver authentication yet.
- No route progress based on GPS yet.
- No printer integration yet.

## Next technical step

Add Room or another local persistence layer so shifts and tickets survive app restarts and can later sync to the central server.

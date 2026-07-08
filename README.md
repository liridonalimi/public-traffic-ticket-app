# Public Traffic Ticket App

Native Android pilot for a bus ticketing and passenger information system.

## Current status

The repository is prepared with a Kotlin + Jetpack Compose Android app skeleton.

It currently includes:

- driver console screen
- first driver shift flow
- bus and route selection using demo data
- cash ticket count and cash total during an active shift
- base domain models for buses, drivers, routes, stops, shifts, and tickets
- offline-first repository placeholder
- device integration interfaces for GPS, printer, and stop-request buttons
- Android setup guide in `docs/SETUP.md`

## Documentation

Presentation documentation:

- English: `docs/EN/02-driver-shift-flow.md`
- Albanian: `docs/SQ/02-rrjedha-e-turnit-te-shoferit.md`

Technical documentation:

- `docs/TECHNICAL/02-driver-shift-flow.md`

## Next build milestone

Build persistent local ticket storage:

1. Save active shift data locally.
2. Save sold tickets locally.
3. Restore active shift after app restart.
4. Prepare unsynced tickets for future server sync.

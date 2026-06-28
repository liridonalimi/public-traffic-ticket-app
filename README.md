# Public Traffic Ticket App

Native Android pilot for a bus ticketing and passenger information system.

## Current status

The repository is prepared with a Kotlin + Jetpack Compose Android app skeleton.

It currently includes:

- driver console screen
- base domain models for buses, drivers, routes, stops, shifts, and tickets
- offline-first repository placeholder
- device integration interfaces for GPS, printer, and stop-request buttons
- Android setup guide in `docs/SETUP.md`

## Next build milestone

Build the first real driver flow:

1. Driver login
2. Bus and route selection
3. Start shift
4. GPS tracking
5. Cash ticket sale
6. Offline ticket storage
7. Shift closing report

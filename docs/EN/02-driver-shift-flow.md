# Driver Shift Flow

## Purpose

The driver shift flow lets a driver prepare the bus for service before selling tickets. The driver selects the bus, selects the route, starts the shift, sells tickets, and closes the shift with a clear ticket and cash summary.

## What the driver can do now

- Select one of the available demo buses.
- Select one of the available demo routes.
- Start a shift for the selected bus and route.
- Sell standard cash tickets during the active shift.
- See the number of tickets sold.
- See the current cash total.
- End the shift and see the last closed shift summary.

## Business value

This module is the base for driver accountability. Each ticket sale belongs to an active shift, bus, route, and driver. This makes daily cash control easier and prepares the system for later reporting in the admin dashboard.

## Current implementation status

This is an in-app pilot flow. The bus list, route list, ticket price, and driver are demo data inside the Android app. Ticket sales are counted during the app session but are not yet saved permanently after closing the app.

## Testing and validation

1. Launch the app and select a demo bus and route.
2. Start a shift and confirm bus/route selection can no longer change during the active shift.
3. Sell three standard tickets. Expected: ticket count is `3` and cash is EUR 1.50.
4. End the shift and confirm the last-shift summary retains the selected bus, route, three tickets, and EUR 1.50.
5. Try selling before starting a new shift. Expected: no ticket is created and cash remains unchanged.

The acceptance result is that every sale belongs to one active shift and shift totals are derived from its tickets.

## Planned improvements

- Save tickets locally on the tablet.
- Sync shift and ticket data to the central server.
- Add driver login.
- Add printed receipts.
- Add different fare types and discounts.

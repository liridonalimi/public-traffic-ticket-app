# Ticket Fares and Discounts

## Purpose

Module 05 replaced the single fixed ticket price with a selectable fare catalog. It allows the driver to charge the correct standard or discounted fare and keeps the chosen fare attached to every stored ticket.

## Available pilot fares

- **Standard:** EUR 0.50.
- **Student:** EUR 0.30; a valid student ID is required.
- **Senior 65+:** EUR 0.25; for passengers aged 65 or older.
- **Child:** EUR 0.20; for children aged 6 to 12.

These names, prices, and eligibility rules are pilot configuration. They are not yet downloaded from a central server.

## What the driver can do

- Select a fare during an active shift.
- See the price before selling the ticket.
- See eligibility guidance for discounted fares.
- Sell tickets using different fare types in the same shift.
- See the ticket count and cash subtotal for each fare.
- See the same fare breakdown in the last-closed-shift summary.

## Ticket history and compatibility

Each ticket stores both the fare-type ID and the price actually charged. Keeping the charged price protects historical records if the catalog changes later. Tickets created before this module did not contain a fare ID, so they are safely interpreted as standard-fare tickets when loaded.

## Business value

This module supports real passenger categories, improves cash reconciliation, and prepares fare-level reporting. It also separates the fare catalog from historical ticket prices, which is important for future price changes and audits.

## Current limitations

- The catalog is local demo data rather than server-managed configuration.
- The app displays eligibility guidance, but the driver remains responsible for checking proof of eligibility.
- There is no configurable validity period, zone pricing, transfer fare, pass, or promotional fare yet.
- Fare names and guidance are currently displayed in the application's pilot language rather than being fully localized.

## Testing and validation

1. Start a shift and sell one Standard, Student, Senior 65+, and Child ticket.
2. Before each sale, confirm the displayed eligibility guidance and price match the selected fare.
3. Expected totals: four tickets and EUR 1.25, split as EUR 0.50, EUR 0.30, EUR 0.25, and EUR 0.20.
4. End the shift and confirm the same per-fare counts and cash subtotals appear in the closed-shift summary.
5. Restart the app and confirm each stored ticket retains its fare ID and charged price.
6. Load a legacy ticket without a fare ID through the compatibility test. Expected: it is reported as Standard without rewriting its stored amount.

The acceptance result is that mixed fares reconcile to the overall shift cash and historical charged prices remain immutable.

## Next module

Module 06 connected each stored ticket sale to Bluetooth ESC/POS printing and a PDF test-printer workflow.

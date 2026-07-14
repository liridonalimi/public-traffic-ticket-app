# Technical: Ticket Fares and Discounts

## Scope

This module replaces the single hard-coded ticket price with a local fare catalog. A driver can select a standard or discounted fare during an active shift, and every saved ticket records the fare used for that sale.

## Fare catalog

The pilot includes four demo fares:

- Standard: EUR 0.50
- Student: EUR 0.30, with valid student ID
- Senior 65+: EUR 0.25
- Child: EUR 0.20, for ages 6 to 12

Production fare names, prices, and eligibility rules will eventually come from server configuration.

## Persistence and compatibility

`Ticket` now contains `fareTypeId`, and `OfflineFirstRepository` writes it into the local JSON record. Tickets stored by earlier versions have no fare ID; while loading, they are assigned the `standard` fare so existing local data remains usable.

The price is still copied onto each ticket. This preserves the actual amount charged even if the catalog price later changes.

## Shift totals

The UI derives a count and cash subtotal for each fare present in the active shift. The same breakdown is captured in the last-closed-shift summary before the active ticket list is cleared.

## Verification

`TicketSummaryTest` covers multi-fare counts, cash totals, catalog ordering, and tickets whose historical fare is no longer in the current catalog.

## Next technical step

Connect ticket sales to a label-printer implementation, track print results, and support retrying failed prints without creating duplicate ticket sales.

# Module 25 — Fare Policy Engine

Module 25 turns each managed fare into a deterministic pricing policy while preserving the fixed fares already used by the pilot.

## Product behavior

- A fare may apply to every route or one selected route.
- Stops carry operator-defined fare zones.
- The base price can gain a charge for every additional zone crossed.
- A configured time window can apply an off-peak discount, including windows that cross midnight.
- A fare may grant a transfer-validity window.
- The driver selects the destination and sees the quoted price, zone count, off-peak status, and transfer guidance before selling.
- A policy with zero adjustments behaves exactly like the previous fixed fare.

The driver still explicitly selects the passenger fare category. Because only that selected category is evaluated, competing-rule priority is not needed in this contract.

## Safety and auditability

Catalog publication rejects unknown route references, incomplete or zero-length off-peak windows, negative values, and invalid stop zones. A new managed-catalog ticket stores the catalog revision, origin, destination, zone count, off-peak result, transfer expiry, category, and final charged amount. Later catalog edits cannot change the historical sale.

## Device validation

1. Publish a fixed fare with all adjustments set to zero; confirm its old price remains unchanged.
2. Give consecutive stops two zones and an extra-zone charge; refresh the tablet and compare a same-zone trip with a cross-zone trip.
3. Configure an off-peak window covering the current time; confirm the discount and the visible off-peak indicator.
4. Configure a transfer window; confirm the transfer guidance appears.
5. Create a route-specific fare; confirm it appears only on that route.
6. Sell and synchronize a ticket, then edit and republish the fare. Confirm the earlier report entry retains its original amount and policy snapshot.
7. Restart the app before synchronization and confirm the pending ticket remains available.

Transfer entitlement is recorded in this module. QR-based inspection and redemption are intentionally deferred to Module 27.

## Validation result

Completed on July 21, 2026. Manual tablet and report-reader validation confirmed:

- published fixed-price changes replace the earlier price on the tablet;
- zone surcharges and off-peak discounts are calculated together and remain visible after synchronization;
- route-specific fares appear only on their configured route;
- transfer validity and the policy revision are retained in each synchronized ticket record;
- synchronized cash totals reconcile as `MATCHED` when the declared cash equals the calculated ticket total.

Report-reader acceptance evidence for `Module25-Offpeak-Test`:

- a two-zone journey cost EUR 1.20: EUR 1.00 base + EUR 0.40 extra zone - EUR 0.20 off-peak discount;
- a one-zone journey cost EUR 0.80: EUR 1.00 base - EUR 0.20 off-peak discount;
- one shift contained EUR 1.20 + EUR 1.20 + EUR 0.80 = EUR 3.20;
- the second shift contained EUR 1.20 + EUR 0.80 = EUR 2.00;
- the report grouped all five tickets as EUR 5.20 and displayed policy revision 17, journey endpoints, zone counts, off-peak status, and transfer-expiry times;
- both tested shifts were reported as `MATCHED`.

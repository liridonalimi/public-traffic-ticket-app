# Module 20 - Central Operations Catalog

## Outcome

Module 20 replaces fixed tablet-only reference data with a centrally managed, versioned catalog for drivers, buses, routes, stops, and fares. An authenticated operator edits and publishes the catalog from BusPay Control. A configured tablet downloads the latest revision between shifts and stores it for offline operation.

## Admin web workflow

After connecting to `/admin`, the **Operations catalog** section displays the published revision and record counts. Each record type has an add/update form and removable draft rows:

- drivers: stable ID and full name;
- buses: stable ID and plate number;
- routes: stable ID and operator-facing name;
- stops: stable ID, route, display name, coordinates, and route order;
- fares: stable ID, name, price in cents, and optional eligibility text.

Changes remain a browser draft until **Publish catalog** is pressed. Publication replaces the complete catalog in one SQLite transaction and increments its revision. The browser sends the revision it originally loaded; if another operator published first, the server rejects the stale draft and requires a refresh.

## Tablet behavior

The tablet continues with built-in demo data until a managed catalog has been downloaded. In **Operations & Setup**:

1. configure the authenticated sync endpoint;
2. press **Refresh Data from Server**;
3. confirm the revision and counts;
4. return to the driver console and confirm the managed drivers, buses, routes, stops, and fares.

The downloaded catalog is stored locally. Closing the app, losing the network, or returning to demo synchronization does not remove it. Catalog refresh is blocked during an active shift so route and fare definitions cannot change in the middle of service. If the signed-in driver is removed centrally, the next refresh safely signs that driver out.

## Double-test checklist

1. Start Docker and ensure `adb reverse tcp:8080 tcp:8080` is active.
2. Open `http://127.0.0.1:8080/admin` and connect with the local Module 20 token.
3. Add a clearly identifiable test driver, bus, route with at least one stop, and fare. Use Albanian `ë` or `ç` in at least one name.
4. Publish and confirm the revision increments and the success message appears.
5. On the tablet, configure `http://127.0.0.1:8080/v1/sync` with the same token, then refresh managed data.
6. Confirm the same revision and counts appear on the tablet.
7. Return to the driver console and verify every new record appears in the appropriate selector.
8. Start a shift with the new driver, bus, route, and fare; confirm the stop order, ticket price, and printed/PDF names use the managed values.
9. End and synchronize the shift; confirm the new IDs appear correctly in the admin report.
10. Fully close and reopen the app, without refreshing, and confirm the catalog remains available.
11. Optionally disconnect USB/network and confirm a new offline shift can still use the saved catalog.
12. Restore or remove temporary test records in the admin draft and publish one final revision.

## Acceptance criteria

- Catalog reads and writes require bearer authentication.
- Incomplete catalogs, duplicate IDs, missing route references, duplicate stop order, and invalid prices or coordinates are rejected.
- Publication is atomic and protected from stale overwrites.
- Tablets apply only complete catalogs and keep the last successful revision offline.
- Active shifts cannot be changed by a catalog refresh.
- Historical shifts and tickets remain intact when reference records change.

## Validation status

Automated and local Docker validation are complete. On 18 July 2026, the operator independently confirmed that the Admin Dashboard loaded and published the managed catalog, the tablet fetched the same revision through the authenticated ADB reverse bridge, the managed records appeared in the app, and synchronization worked correctly. Module 20 is complete.

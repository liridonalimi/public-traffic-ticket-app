# Driver Login and Identity

## Purpose

Driver login and identity replaces the fixed demo driver with a local sign-in flow. The driver chooses their identity before starting a shift, and every new shift is linked to that driver ID.

## What the driver can do now

- Select a driver from the local pilot driver list.
- Sign in before starting a shift.
- See the signed-in driver name and ID on the driver console.
- Sign out when no shift is active.
- Restart the app and keep the signed-in driver restored.
- Restore an active shift with the correct driver identity.

## Business value

This module improves driver accountability. Ticket sales and cash totals are now connected to a specific driver ID, which prepares the system for future cash control, audit reports, and backend authentication.

## Current implementation status

The app uses a local demo driver list inside the Android app. The signed-in driver is stored locally through the offline-first repository. Starting a shift is blocked until a driver is signed in, and sign-out is blocked while a shift is active.

## Planned improvements

- Replace the local driver list with backend authentication.
- Add PIN, password, or card-based login.
- Add role-based access for drivers, inspectors, and administrators.
- Record login and logout events for audits.

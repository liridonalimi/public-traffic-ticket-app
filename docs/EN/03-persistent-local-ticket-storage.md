# Persistent Local Ticket Storage

## Purpose

Persistent local ticket storage lets the driver continue working even if the tablet is restarted or the network is unavailable. The active shift and sold tickets are saved inside the Android app storage.

## What the driver can do now

- Start a shift and keep it saved locally.
- Sell cash tickets and keep them saved locally.
- Restart the app and return to the active shift.
- See current-shift tickets separately from the total tickets waiting for future sync.
- End the shift while keeping sold tickets stored for later server upload.

## Business value

This module protects ticket and cash data during daily bus operations. A closed app or interrupted tablet session should not erase the active shift or the tickets already sold. It also prepares the project for the next offline-first step: syncing unsent tickets to a central server.

## Current implementation status

The app uses local Android app storage through the offline-first repository. The active shift is stored separately from the ticket list. The driver screen shows current-shift ticket totals separately from the total unsynced local queue. Tickets remain marked as unsynced so a later sync module can upload them to the backend.

## Planned improvements

- Move storage to Room when the data model becomes larger.
- Add server sync with retry handling.
- Mark tickets as synced after successful upload.
- Add stronger recovery and validation for corrupted local data.

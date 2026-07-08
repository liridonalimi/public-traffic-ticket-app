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

#######################################

# Aplikacioni i Gjobave të Trafikut Publik

Pilot i brendshëm Android për një sistem biletash autobusësh dhe informacioni për pasagjerët.

## Statusi aktual

Depozita është përgatitur me një skelet aplikacioni Android Kotlin + Jetpack Compose.

Aktualisht përfshin:

- ekranin e konsolës së shoferit
- modelet e domenit bazë për autobusët, shoferët, itineraret, ndalesat, turnet dhe biletat
- vendin e depozitës së parë jashtë linje
- ndërfaqet e integrimit të pajisjes për butonat GPS, printerin dhe kërkesën për ndalesë
- Udhëzuesin e konfigurimit Android në `docs/SETUP.md`

## Pika e ndërtimit tjetër

Ndërtoni rrjedhën e parë të vërtetë të shoferit:

1. Hyrja e shoferit
2. Përzgjedhja e autobusit dhe itinerarit
3. Fillimi i turnit
4. Gjurmimi GPS
5. Shitja e biletave me para në dorë
6. Ruajtja e biletave jashtë linje
7. Raporti i mbylljes së turnit

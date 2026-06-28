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

# Public Traffic Ticket App

Native Android pilot for a bus ticketing and passenger information system.

## Current status

The repository is prepared with a Kotlin + Jetpack Compose Android app skeleton.

It currently includes:

- driver console screen
- first driver shift flow
- local driver sign-in and driver identity
- bus and route selection using demo data
- standard, student, senior, and child ticket fares
- discounted-fare eligibility guidance and per-fare shift totals
- paired Bluetooth ESC/POS label-printer selection
- automatic ticket printing with durable failure state and retry
- built-in PDF test printer for validation without printer hardware
- forward-only GPS route progress with persistent current-stop state
- synchronized current/next-stop information on the driver console
- dedicated passenger display with a hardware-free demo advance control
- cash ticket count and cash total during an active shift
- persistent local active shift storage
- persistent local ticket storage for future sync
- base domain models for buses, drivers, routes, stops, shifts, and tickets
- offline-first repository using local app storage
- device integration interfaces for GPS, printer, and stop-request buttons
- Android setup guide in `docs/SETUP.md`

## Module timeline

1. Project foundation and Android app skeleton - complete
2. Driver shift flow - complete
3. Persistent local ticket storage - complete
4. Driver login and driver identity - complete
5. Ticket fare types and discounts - complete
6. Receipt printing integration - complete
7. GPS route progress and next-stop tracking - complete
8. Stop-request button integration - next
9. Server sync for shifts and tickets - planned
10. Admin reporting dashboard data contract - planned

## Documentation

Presentation documentation:

- English: `docs/EN/02-driver-shift-flow.md`
- English: `docs/EN/03-persistent-local-ticket-storage.md`
- English: `docs/EN/04-driver-login-and-identity.md`
- English: `docs/EN/07-gps-route-progress-and-passenger-display.md`
- Albanian: `docs/SQ/02-rrjedha-e-turnit-te-shoferit.md`
- Albanian: `docs/SQ/03-ruajtja-lokale-e-biletave.md`
- Albanian: `docs/SQ/04-kycja-dhe-identiteti-i-shoferit.md`
- Albanian: `docs/SQ/07-perparimi-i-linjes-me-gps-dhe-ekrani-i-pasagjereve.md`

Technical documentation:

- `docs/TECHNICAL/02-driver-shift-flow.md`
- `docs/TECHNICAL/03-persistent-local-ticket-storage.md`
- `docs/TECHNICAL/04-driver-login-and-identity.md`
- `docs/TECHNICAL/05-ticket-fares-and-discounts.md`
- `docs/TECHNICAL/06-bluetooth-ticket-printing.md`
- `docs/TECHNICAL/07-gps-route-progress-and-passenger-display.md`

## Next build milestone

Build stop-request button integration:

1. Receive a stop request from the device integration boundary.
2. Show the active request to the driver and passengers.
3. Clear the request safely when the bus reaches the requested stop.

#######################################

# Aplikacioni i Gjobave të Trafikut Publik

Pilot i brendshëm Android për një sistem biletash autobusësh dhe informacioni për pasagjerët.

## Statusi aktual

Depozita është përgatitur me një skelet aplikacioni Android Kotlin + Jetpack Compose.

Aktualisht përfshin:

- ekranin e konsolës së shoferit
- rrjedhën e parë të turnit të shoferit
- kyçjen lokale dhe identitetin e shoferit
- zgjedhjen e autobusit dhe linjës me të dhëna demo
- tarifat standarde, studentore, për të moshuar dhe për fëmijë
- udhëzimet për zbritje dhe totalet e turnit sipas tarifës
- zgjedhjen e printerit Bluetooth ESC/POS të çiftuar
- printimin automatik të biletës me ruajtjen e dështimit dhe riprovim
- printerin testues PDF për validim pa pajisje fizike
- përparimin e linjës me GPS vetëm përpara dhe ruajtjen e ndalesës aktuale
- ndalesën aktuale dhe të ardhshme të sinkronizuar në konsolën e shoferit
- ekranin e veçantë për pasagjerë me avancim demo pa pajisje
- numërimin e biletave me para të gatshme dhe totalin e arkës gjatë turnit aktiv
- ruajtjen lokale të turnit aktiv
- ruajtjen lokale të biletave për sinkronizim të ardhshëm
- modelet e domenit bazë për autobusët, shoferët, itineraret, ndalesat, turnet dhe biletat
- depozitën offline-first me ruajtje lokale të aplikacionit
- ndërfaqet e integrimit të pajisjes për butonat GPS, printerin dhe kërkesën për ndalesë
- Udhëzuesin e konfigurimit Android në `docs/SETUP.md`

## Afati i moduleve

1. Themeli i projektit dhe skeleti Android - përfunduar
2. Rrjedha e turnit të shoferit - përfunduar
3. Ruajtja lokale e biletave - përfunduar
4. Kyçja e shoferit dhe identiteti i shoferit - përfunduar
5. Llojet e tarifave dhe zbritjet - përfunduar
6. Integrimi i printimit të biletave - përfunduar
7. Përparimi i linjës me GPS dhe ndalesa e radhës - përfunduar
8. Integrimi i butonit për kërkesë ndalese - moduli i radhës
9. Sinkronizimi i turneve dhe biletave me serverin - planifikuar
10. Kontrata e të dhënave për raportim në panelin administrativ - planifikuar

## Pika e ndërtimit tjetër

Ndërtoni integrimin e butonit për kërkesë ndalese:

1. Pranojeni kërkesën për ndalesë nga kufiri i integrimit të pajisjes.
2. Shfaqeni kërkesën aktive te shoferi dhe pasagjerët.
3. Pastrojeni kërkesën në mënyrë të sigurt kur autobusi arrin në ndalesën e kërkuar.

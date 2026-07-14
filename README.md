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
7. GPS route progress and next-stop tracking - next
8. Stop-request button integration - planned
9. Server sync for shifts and tickets - planned
10. Admin reporting dashboard data contract - planned

## Documentation

Presentation documentation:

- English: `docs/EN/02-driver-shift-flow.md`
- English: `docs/EN/03-persistent-local-ticket-storage.md`
- English: `docs/EN/04-driver-login-and-identity.md`
- Albanian: `docs/SQ/02-rrjedha-e-turnit-te-shoferit.md`
- Albanian: `docs/SQ/03-ruajtja-lokale-e-biletave.md`
- Albanian: `docs/SQ/04-kycja-dhe-identiteti-i-shoferit.md`

Technical documentation:

- `docs/TECHNICAL/02-driver-shift-flow.md`
- `docs/TECHNICAL/03-persistent-local-ticket-storage.md`
- `docs/TECHNICAL/04-driver-login-and-identity.md`

## Next build milestone

Build route progress and the passenger display:

1. Track the bus position against the selected route.
2. Show the current and next stops on a dedicated passenger screen.
3. Keep the onboard display synchronized with the active driver shift.

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
7. Përparimi i linjës me GPS dhe ndalesa e radhës - moduli i radhës
8. Integrimi i butonit për kërkesë ndalese - planifikuar
9. Sinkronizimi i turneve dhe biletave me serverin - planifikuar
10. Kontrata e të dhënave për raportim në panelin administrativ - planifikuar

## Pika e ndërtimit tjetër

Ndërtoni përparimin e linjës dhe ekranin e pasagjerëve:

1. Ndiqni pozitën e autobusit kundrejt linjës së zgjedhur.
2. Shfaqni ndalesën aktuale dhe të ardhshme në një ekran të posaçëm.
3. Mbajeni ekranin në autobus të sinkronizuar me turnin aktiv të shoferit.

# Public Traffic Ticket App

Native Android pilot for a bus ticketing and passenger information system.

## Current status

The repository is prepared with a Kotlin + Jetpack Compose Android app skeleton.

It currently includes:

- driver console screen
- first driver shift flow
- bus and route selection using demo data
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
4. Driver login and driver identity - next
5. Ticket fare types and discounts - planned
6. Receipt printing integration - planned
7. GPS route progress and next-stop tracking - planned
8. Stop-request button integration - planned
9. Server sync for shifts and tickets - planned
10. Admin reporting dashboard data contract - planned

## Documentation

Presentation documentation:

- English: `docs/EN/02-driver-shift-flow.md`
- English: `docs/EN/03-persistent-local-ticket-storage.md`
- Albanian: `docs/SQ/02-rrjedha-e-turnit-te-shoferit.md`
- Albanian: `docs/SQ/03-ruajtja-lokale-e-biletave.md`

Technical documentation:

- `docs/TECHNICAL/02-driver-shift-flow.md`
- `docs/TECHNICAL/03-persistent-local-ticket-storage.md`

## Next build milestone

Build driver login and driver identity:

1. Replace the demo driver with a selectable or login-based driver.
2. Keep ticket sales linked to the real driver ID.
3. Prepare the login flow for later backend authentication.

#######################################

# Aplikacioni i Gjobave të Trafikut Publik

Pilot i brendshëm Android për një sistem biletash autobusësh dhe informacioni për pasagjerët.

## Statusi aktual

Depozita është përgatitur me një skelet aplikacioni Android Kotlin + Jetpack Compose.

Aktualisht përfshin:

- ekranin e konsolës së shoferit
- rrjedhën e parë të turnit të shoferit
- zgjedhjen e autobusit dhe linjës me të dhëna demo
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
4. Kyçja e shoferit dhe identiteti i shoferit - moduli i radhës
5. Llojet e tarifave dhe zbritjet - planifikuar
6. Integrimi i printimit të biletave - planifikuar
7. Përparimi i linjës me GPS dhe ndalesa e radhës - planifikuar
8. Integrimi i butonit për kërkesë ndalese - planifikuar
9. Sinkronizimi i turneve dhe biletave me serverin - planifikuar
10. Kontrata e të dhënave për raportim në panelin administrativ - planifikuar

## Pika e ndërtimit tjetër

Ndërtoni kyçjen dhe identitetin e shoferit:

1. Zëvendësoni shoferin demo me zgjedhje ose kyçje të shoferit.
2. Lidhni shitjet e biletave me ID-në reale të shoferit.
3. Përgatitni rrjedhën e kyçjes për autentikim të ardhshëm me backend.

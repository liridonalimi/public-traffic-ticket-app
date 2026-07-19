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
- persistent next-stop requests synchronized across driver and passenger displays
- automatic stop-request clearing on GPS or demo arrival
- durable closed-shift storage and acknowledged-only offline sync
- demo server controls for safe failure and retry validation
- versioned admin reporting contract with driver, shift, ticket, fare, cash, and sync totals
- in-app admin report preview with legacy-data quality warnings
- authenticated HTTPS production sync client with idempotent contract validation
- cleartext blocking and safe acknowledgement/error handling for production sync
- authenticated reference sync API with transactional SQLite persistence
- idempotent server ingestion and synchronized reporting projection
- hardened provider-neutral container deployment package with health and secret controls
- integrity-checked database backup/restore and Android runtime client selection
- responsive authenticated administrative web dashboard served by the reporting API
- reconciled fare, driver, shift, ticket, revenue, search, and filter views
- session-only Android sync-server configuration with debug loopback validation
- end-to-end Android-to-Docker synchronization through an authenticated adb bridge
- provider-neutral staging Compose profile with private ingress-only exposure
- fail-closed staging preflight for HTTPS, immutable images, protected secrets, retention, and named owners
- authenticated, non-mutating staging smoke validation with negative-token verification
- driver-first pilot workspace with explicit shift status and end-shift confirmation
- separate between-shift operations workspace for sync setup and administrative tools
- Android language selector with system default, Albanian, and English choices
- Kosovo-oriented pilot terminology for lines, stops, fares, shifts, and cash sales
- 58 mm and 80 mm PDF receipt simulation with width-aware ESC/POS formatting
- visible printer hardware certification gate for pairing, output, retry, and endurance checks
- versioned central catalog for drivers, buses, routes, stops, and fares
- authenticated web catalog editing with atomic publication and tablet offline refresh
- least-privilege device-sync, report-reader, and catalog-administrator credentials
- server-enforced role discovery with separate report, catalog-read, catalog-write, and sync permissions
- two-credential overlap windows for controlled, zero-downtime role-token rotation
- persistent authorization audit events and an isolated security-auditor web workspace
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
8. Stop-request button integration - complete
9. Server sync for shifts and tickets - complete
10. Admin reporting dashboard data contract - complete
11. Authenticated HTTPS sync client foundation - complete
12. Persistent sync API and database service - complete
13. Production deployment package and runtime activation boundary - complete
14. Authenticated administrative web reporting dashboard - complete
15. Android-to-server live synchronization activation - complete
16. Staging release readiness and operational validation - complete
17. Driver pilot UX and role separation - complete
18. Albanian-first localization and Kosovo operator terminology - complete
19. Printer simulation, receipt quality, and hardware certification - complete (physical-printer certification pending hardware)
20. Centrally managed drivers, buses, routes, stops, and fares - complete
21. Production access boundaries and role-scoped administration - complete
22. Credential rotation and authorization auditability - complete

## Documentation

Presentation documentation:

- English: `docs/EN/01-project-foundation-and-android-skeleton.md`
- English: `docs/EN/02-driver-shift-flow.md`
- English: `docs/EN/03-persistent-local-ticket-storage.md`
- English: `docs/EN/04-driver-login-and-identity.md`
- English: `docs/EN/05-ticket-fares-and-discounts.md`
- English: `docs/EN/06-bluetooth-and-pdf-ticket-printing.md`
- English: `docs/EN/07-gps-route-progress-and-passenger-display.md`
- English: `docs/EN/08-stop-request-button-integration.md`
- English: `docs/EN/09-offline-server-sync.md`
- English: `docs/EN/10-admin-reporting-data-contract.md`
- English: `docs/EN/11-authenticated-https-sync-client.md`
- English: `docs/EN/12-persistent-sync-api-and-database.md`
- English: `docs/EN/13-production-deployment-package.md`
- English: `docs/EN/14-admin-web-reporting-dashboard.md`
- English: `docs/EN/15-android-to-server-live-sync.md`
- English: `docs/EN/16-staging-release-readiness.md`
- English: `docs/EN/17-driver-pilot-ux-and-role-separation.md`
- English: `docs/EN/18-albanian-first-localization.md`
- English: `docs/EN/19-printer-simulation-and-certification.md`
- English: `docs/EN/20-central-operations-catalog.md`
- English: `docs/EN/21-role-scoped-production-access.md`
- English: `docs/EN/22-credential-rotation-and-authorization-audit.md`
- Albanian: `docs/SQ/01-themeli-i-projektit-dhe-skeleti-android.md`
- Albanian: `docs/SQ/02-rrjedha-e-turnit-te-shoferit.md`
- Albanian: `docs/SQ/03-ruajtja-lokale-e-biletave.md`
- Albanian: `docs/SQ/04-kycja-dhe-identiteti-i-shoferit.md`
- Albanian: `docs/SQ/05-tarifat-e-biletave-dhe-zbritjet.md`
- Albanian: `docs/SQ/06-printimi-i-biletave-me-bluetooth-dhe-pdf.md`
- Albanian: `docs/SQ/07-perparimi-i-linjes-me-gps-dhe-ekrani-i-pasagjereve.md`
- Albanian: `docs/SQ/08-integrimi-i-butonit-per-kerkese-ndalese.md`
- Albanian: `docs/SQ/09-sinkronizimi-offline-me-serverin.md`
- Albanian: `docs/SQ/10-kontrata-e-raportimit-administrativ.md`
- Albanian: `docs/SQ/11-klienti-i-autentikuar-i-sinkronizimit-https.md`
- Albanian: `docs/SQ/12-api-dhe-baza-e-qendrueshme-e-sinkronizimit.md`
- Albanian: `docs/SQ/13-paketa-e-vendosjes-ne-prodhim.md`
- Albanian: `docs/SQ/14-paneli-web-i-raportimit-administrativ.md`
- Albanian: `docs/SQ/15-sinkronizimi-live-android-me-serverin.md`
- Albanian: `docs/SQ/16-gatishmeria-e-publikimit-staging.md`
- Albanian: `docs/SQ/17-pervoja-e-pilotit-per-shoferin-dhe-ndarja-e-roleve.md`
- Albanian: `docs/SQ/18-lokalizimi-me-shqipen-si-gjuhe-kryesore.md`
- Albanian: `docs/SQ/19-simulimi-i-printerit-dhe-certifikimi.md`
- Albanian: `docs/SQ/20-katalogu-qendror-i-operacioneve.md`
- Albanian: `docs/SQ/21-qasja-ne-prodhim-sipas-roleve.md`
- Albanian: `docs/SQ/22-rotacioni-i-kredencialeve-dhe-auditimi-i-qasjes.md`

Technical documentation:

- `docs/TECHNICAL/01-project-foundation-and-android-skeleton.md`
- `docs/TECHNICAL/02-driver-shift-flow.md`
- `docs/TECHNICAL/03-persistent-local-ticket-storage.md`
- `docs/TECHNICAL/04-driver-login-and-identity.md`
- `docs/TECHNICAL/05-ticket-fares-and-discounts.md`
- `docs/TECHNICAL/06-bluetooth-ticket-printing.md`
- `docs/TECHNICAL/07-gps-route-progress-and-passenger-display.md`
- `docs/TECHNICAL/08-stop-request-button-integration.md`
- `docs/TECHNICAL/09-offline-server-sync.md`
- `docs/TECHNICAL/10-admin-reporting-data-contract.md`
- `docs/TECHNICAL/11-authenticated-https-sync-client.md`
- `docs/TECHNICAL/12-persistent-sync-api-and-database.md`
- `docs/TECHNICAL/13-production-deployment-package.md`
- `docs/TECHNICAL/14-admin-web-reporting-dashboard.md`
- `docs/TECHNICAL/15-android-to-server-live-sync.md`
- `docs/TECHNICAL/16-staging-release-readiness.md`
- `docs/TECHNICAL/17-driver-pilot-ux-and-role-separation.md`
- `docs/TECHNICAL/18-albanian-first-localization.md`
- `docs/TECHNICAL/19-printer-simulation-and-certification.md`
- `docs/TECHNICAL/20-central-operations-catalog.md`
- `docs/TECHNICAL/21-role-scoped-production-access.md`
- `docs/TECHNICAL/22-credential-rotation-and-authorization-audit.md`

## Next build milestone

Client-readiness continues with:

1. Select hosting infrastructure and activate the already prepared staging release gate when a domain and accountable operator are available.
2. Integrate the role boundary and tested rotation protocol with the selected production identity provider.

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
- kërkesat e ruajtura për ndalesën e ardhshme, të sinkronizuara në të dy ekranet
- pastrimin automatik të kërkesës pas mbërritjes me GPS ose demo
- ruajtjen e turneve të përfunduara dhe sinkronizimin vetëm pas konfirmimit
- kontrollet e serverit demo për validimin e dështimit dhe riprovimit
- kontratën e versionuar të raportimit me totalet e shoferëve, turneve, biletave, tarifave, arkës dhe sinkronizimit
- pamjen administrative në aplikacion me paralajmërime për cilësinë e të dhënave të vjetra
- klientin e autentikuar HTTPS për sinkronizimin e prodhimit me kontratë idempotente
- bllokimin e trafikut të pakriptuar dhe trajtimin e sigurt të konfirmimeve/gabimeve
- API-në referuese të autentikuar me ruajtje transaksionale SQLite
- pranimin idempotent në server dhe projektimin e raportimit të sinkronizuar
- paketën e fortifikuar të container-it me health checks dhe kontroll të sekreteve
- backup/restore me integritet dhe zgjedhjen e klientit Android gjatë ekzekutimit
- panelin web administrativ responsiv dhe të autentikuar, të shërbyer nga API-ja e raportimit
- pamjet e rakorduara për tarifat, shoferët, turnet, biletat, të ardhurat, kërkimin dhe filtrat
- konfigurimin vetëm për sesion të serverit të sinkronizimit në Android me validim lokal debug
- sinkronizimin Android-Docker nga fillimi në fund përmes urës së autentikuar adb
- profilin staging të pavarur nga ofruesi me ekspozim vetëm përmes ingress-it privat
- kontrollin staging që dështon në mënyrë të sigurt për HTTPS, imazhin e pandryshueshëm, sekretet, ruajtjen dhe pronarët
- kontrollin e autentikuar dhe pa ndryshim të të dhënave pas publikimit, përfshirë token-in e gabuar
- hapësirën e fokusuar për shoferin me status të qartë dhe konfirmim për përfundimin e turnit
- hapësirën e veçantë ndërmjet turneve për konfigurimin e sinkronizimit dhe mjetet administrative
- zgjedhësin e gjuhës Android me opsionet e pajisjes, shqip dhe anglisht
- terminologjinë e pilotit për Kosovën për linjat, ndalesat, tarifat, turnet dhe shitjet me para të gatshme
- simulimin PDF të kuponit 58 mm dhe 80 mm me formatim ESC/POS sipas gjerësisë
- kontrollin e dukshëm të certifikimit të printerit për çiftim, dalje, riprovim dhe qëndrueshmëri
- katalogun qendror të versionuar për shoferët, autobusët, linjat, ndalesat dhe tarifat
- redaktimin e autentikuar në web me publikim atomik dhe rifreskim offline në tablet
- kredencialet me privilegj minimal për pajisjen, lexuesin e raportit dhe administratorin e katalogut
- zbulimin e roleve dhe ndarjen në server të raportimit, leximit/shkrimit të katalogut dhe sinkronizimit
- dritaren me dy kredenciale për rotacion të kontrolluar pa ndërprerje të shërbimit
- ngjarjet e ruajtura të autorizimit dhe hapësirën e izoluar web për auditorin e sigurisë
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
8. Integrimi i butonit për kërkesë ndalese - përfunduar
9. Sinkronizimi i turneve dhe biletave me serverin - përfunduar
10. Kontrata e të dhënave për raportim në panelin administrativ - përfunduar
11. Baza e klientit të autentikuar për sinkronizim HTTPS - përfunduar
12. API dhe baza e qëndrueshme e sinkronizimit - përfunduar
13. Paketa e vendosjes dhe kufiri i aktivizimit në prodhim - përfunduar
14. Paneli web administrativ i autentikuar për raportim - përfunduar
15. Aktivizimi i sinkronizimit live Android-server - përfunduar
16. Gatishmëria e publikimit staging dhe validimi operacional - përfunduar
17. Përvoja e pilotit për shoferin dhe ndarja e roleve - përfunduar
18. Lokalizimi me shqipen si gjuhë kryesore dhe terminologjia e operatorit në Kosovë - përfunduar
19. Simulimi i printerit, cilësia e kuponit dhe certifikimi i pajisjes - përfunduar (certifikimi i printerit fizik pret pajisjen)
20. Menaxhimi qendror i shoferëve, autobusëve, linjave, ndalesave dhe tarifave - përfunduar
21. Kufijtë e qasjes në prodhim dhe administrimi sipas roleve - përfunduar
22. Rotacioni i kredencialeve dhe auditimi i autorizimit - përfunduar

## Pika e ndërtimit tjetër

Gatishmëria për klientin vazhdon me:

1. Zgjidhni infrastrukturën dhe aktivizoni kontrollin staging tashmë të përgatitur kur të ketë domain dhe operator përgjegjës.
2. Lidhni kufirin e roleve dhe protokollin e testuar të rotacionit me ofruesin e zgjedhur të identitetit.

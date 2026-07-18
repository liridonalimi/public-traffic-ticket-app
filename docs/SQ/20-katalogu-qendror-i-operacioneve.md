# Moduli 20 - Katalogu qendror i operacioneve

## Rezultati

Moduli 20 i zëvendëson të dhënat referente të fiksuara vetëm në tablet me një katalog qendror të versionuar për shoferët, autobusët, linjat, ndalesat dhe tarifat. Operatori i autentikuar e redakton dhe publikon katalogun nga BusPay Control. Tableti i konfiguruar e shkarkon revizionin e fundit ndërmjet turneve dhe e ruan për punë offline.

## Rrjedha në panelin web

Pas lidhjes në `/admin`, seksioni **Operations catalog** tregon revizionin e publikuar dhe numrin e të dhënave. Secili lloj ka formularin shto/përditëso dhe rreshtat e draftit që mund të hiqen:

- shoferët: ID e qëndrueshme dhe emri i plotë;
- autobusët: ID e qëndrueshme dhe targa;
- linjat: ID e qëndrueshme dhe emri për operatorin;
- ndalesat: ID, linja, emri, koordinatat dhe rendi në linjë;
- tarifat: ID, emri, çmimi në cent dhe kushti opsional.

Ndryshimet mbeten draft në shfletues derisa shtypet **Publish catalog**. Publikimi e zëvendëson katalogun e plotë në një transaksion SQLite dhe e rrit revizionin. Nëse një operator tjetër publikon më parë, serveri e refuzon draftin e vjetër dhe kërkon rifreskim.

## Sjellja në tablet

Tableti vazhdon me të dhënat demo derisa të shkarkohet katalogu i menaxhuar. Te **Operacionet dhe konfigurimi**:

1. konfiguroni adresën e autentikuar të sinkronizimit;
2. shtypni **Rifresko të dhënat nga serveri**;
3. konfirmoni revizionin dhe numrat;
4. kthehuni te paneli i shoferit dhe kontrolloni shoferët, autobusët, linjat, ndalesat dhe tarifat.

Katalogu ruhet në pajisje. Mbyllja e aplikacionit, humbja e rrjetit ose kthimi te sinkronizimi demo nuk e fshin. Rifreskimi bllokohet gjatë turnit aktiv. Nëse shoferi i identifikuar hiqet nga katalogu qendror, rifreskimi i ardhshëm e çidentifikon në mënyrë të sigurt.

## Lista e testimit të dyfishtë

1. Nisni Docker-in dhe konfirmoni `adb reverse tcp:8080 tcp:8080`.
2. Hapni `http://127.0.0.1:8080/admin` dhe lidheni me tokenin lokal të Modulit 20.
3. Shtoni një shofer, autobus, linjë me të paktën një ndalesë dhe tarifë testuese. Përdorni `ë` ose `ç` në të paktën një emër.
4. Publikoni dhe konfirmoni rritjen e revizionit dhe mesazhin e suksesit.
5. Në tablet konfiguroni `http://127.0.0.1:8080/v1/sync` me të njëjtin token dhe rifreskoni të dhënat.
6. Konfirmoni të njëjtin revizion dhe numrat në tablet.
7. Kthehuni te paneli i shoferit dhe verifikoni secilën të dhënë të re në zgjedhësin përkatës.
8. Filloni turn me shoferin, autobusin, linjën dhe tarifën e re; kontrolloni rendin e ndalesave, çmimin dhe emrat në biletë/PDF.
9. Përfundoni dhe sinkronizoni turnin; kontrolloni ID-të e reja në raportin administrativ.
10. Mbylleni plotësisht dhe hapeni aplikacionin pa rifreskim; katalogu duhet të mbetet.
11. Opsionale: shkëputni USB/rrjetin dhe konfirmoni se katalogu i ruajtur punon offline.
12. Hiqni ose riktheni të dhënat e përkohshme dhe publikoni revizionin përfundimtar.

## Kriteret e pranimit

- Leximi dhe shkrimi i katalogut kërkojnë autentikim bearer.
- Katalogët e paplotë, ID-të e dyfishta, linjat që mungojnë, rendi i dyfishtë dhe çmimet/koordinatat e pavlefshme refuzohen.
- Publikimi është atomik dhe mbrohet nga mbishkrimi i draftit të vjetër.
- Tableti aplikon vetëm katalog të plotë dhe ruan revizionin e fundit offline.
- Turni aktiv nuk ndryshohet nga rifreskimi i katalogut.
- Turnet dhe biletat historike mbeten të paprekura kur ndryshojnë të dhënat referente.

## Statusi i validimit

Validimi automatik dhe lokal me Docker është përfunduar. Më 18 korrik 2026, operatori konfirmoi në mënyrë të pavarur se paneli administrativ e lexoi dhe publikoi katalogun, tableti e shkarkoi të njëjtin revizion përmes urës së autentikuar ADB reverse, të dhënat e menaxhuara u shfaqën në aplikacion dhe sinkronizimi funksionoi siç duhet. Moduli 20 është përfunduar.

# Udhërrëfyesi i produktit — Modulet 23–35

## Drejtimi

Modulet 1–22 krijuan aplikacionin offline-first për shoferin, ruajtjen dhe printimin simulues të biletave, ecurinë në linjë, sinkronizimin, të dhënat qendrore, raportimin, ndarjen e roleve, rotacionin e kredencialeve dhe auditimin. Faza e re e zhvillon pilotin në një produkt më të plotë operacional pa supozuar hosting, procesues pagese, printer fizik ose platformë identiteti.

Çdo modul ruan të njëjtin kontroll dorëzimi: implementim, teste automatike, build Android/server, dokumentacion anglisht/shqip/teknik, validim nga asistenti, validim i pavarur në aplikacion ose web dhe procedurë Git vetëm pas pranimit.

## Faza A — Shërbimi ditor dhe kontrolli i të ardhurave

### Moduli 23 — Rakordimi dhe dorëzimi i arkës së turnit

Shoferi shënon paratë e numëruara kur mbyll turnin. Aplikacioni i krahason me të ardhurat e pritshme nga biletat, tregon diferencën, kërkon konfirmim, e ruan dorëzimin offline dhe e sinkronizon. Raporti shfaq shumën e pritur, të deklaruar, diferencën dhe statusin. Ky është moduli i ardhshëm.

### Moduli 24 — Udhëtimet e planifikuara dhe caktimet

Shtohen kalendarët e shërbimit, udhëtimet, nisjet, drejtimet dhe caktimi qendror i shoferit dhe mjetit. Tableti shfaq punën përkatëse, pengon konfliktet, ruan listën offline dhe regjistron kohën reale të fillimit/mbarimit, duke ruajtur rrjedhën ad-hoc të pilotit.

### Moduli 25 — Motori i politikave të tarifave

Tarifat e thjeshta zëvendësohen me rregulla të versionuara sipas linjës, zonës, kohës, kategorisë së pasagjerit, vlefshmërisë së transferit dhe prioritetit. Bileta ruan kopjen e rregullit të aplikuar, ndërsa rregullat e paqarta ose jo të plota bllokohen para publikimit.

### Moduli 26 — Anulimi, korrigjimi dhe riprintimi i kontrolluar

Shtohen veprime pas shitjes me arsye, autorizim mbikëqyrësi, lidhje të pandryshueshme me biletën origjinale dhe histori auditi. Riprintimi nuk numërohet si shitje, anulimi e rregullon shumën vetëm një herë dhe korrigjimet mbeten idempotente gjatë sinkronizimit.

## Faza B — Siguria e të ardhurave dhe operacionet për pasagjerë

### Moduli 27 — Biletat QR të verifikueshme dhe kontrollori

Krijohet payload QR i lidhur me biletën, tarifën, kohën dhe pajisjen. Hapësira e izoluar e kontrollorit raporton biletë të vlefshme, të skaduar, të anuluar, të dyfishtë, të panjohur ose të paverifikueshme. Validimi offline ka kufij të qartë dhe nuk pretendon pajtueshmëri fiskale pa certifikimin ligjor.

### Moduli 28 — Analitika operative dhe eksportet

Raportimi zgjerohet sipas linjës, udhëtimit, orarit, mjetit, shoferit, tarifës, diferencës, anulimit dhe kontrollit. Eksportet CSV përfshijnë versionin e kontratës, kohën, filtrat dhe revisionin burimor për riprodhueshmëri dhe auditim.

### Moduli 29 — Importi/eksporti dhe validimi GTFS Schedule

Katalogu dhe udhëtimet lidhen me agency, routes, stops, trips, stop times, calendars dhe të dhënat e tarifave të GTFS Schedule. Importi kalon në draft të kontrollueshëm, validim strukturor/referencial dhe publikim atomik; eksporti është determinist dhe raporton humbjet e mundshme.

### Moduli 30 — Qasshmëria, njoftimet dhe alarmet

Ekrani i pasagjerëve merr tekst të madh, kontrast të sigurt, njoftime dygjuhëshe të ndalesave, sinjale audio dhe mesazhe për ndërprerje. Alarmet kanë rëndësi, afat vlefshmërie, linja/ndalesa të prekura dhe sjellje offline.

### Moduli 31 — Pozicioni i mjeteve dhe mbikëqyrja live

Turnet aktive publikojnë pozicion të kufizuar dhe të kontrolluar. Pamja operative shfaq pozicionin e fundit, freskinë, ecurinë dhe vonesën. Të dhënat e vjetra dallohen qartë, mbledhja ndalet jashtë shërbimit dhe modeli mbetet i përshtatshëm për GTFS Realtime.

## Faza C — Shkallëzimi i flotës, pagesat dhe publikimi

### Moduli 32 — Regjistrimi dhe gjendja e pajisjeve

Çdo tablet merr identitet, cikël regjistrimi/revokimi, mjet/depo, revision konfigurimi, version aplikacioni, kontakt të fundit dhe gjendje. Sekretet nuk ndahen mes pajisjeve dhe pajisjet e humbura revokohen veçmas.

### Moduli 33 — Abstraksioni i pagesave dhe gatishmëria cashless

Modelohen pagesat cash, kartë, llogari dhe kupon pa e lidhur domenin me një procesues. Simulatori mbulon aprovim, refuzim dhe rezultat të panjohur; referencat ruhen dhe rakordimi është retry-safe. Pagesat reale dhe settlement kërkojnë ofrues të zgjedhur.

### Moduli 34 — Ndarja sipas operatorit dhe depos

Katalogët, caktimet, pajisjet, turnet, raportet dhe rolet kufizohen sipas operatorit/depos. API-ja dhe baza pengojnë qasjen ndërmjet operatorëve, ndërsa pronësia shfaqet në çdo hapësirë administrative.

### Moduli 35 — Privatësia, siguria, rikuperimi dhe publikimi

Bashkohen klasifikimi dhe ruajtja/fshirja e të dhënave, verifikimi i sigurisë mobile, provat backup/restore, rikuperimi, upgrade/rollback, observability dhe lista e pranimit në prodhim. Aktivizimi final pret infrastrukturën, domain/TLS, operatorët përgjegjës, shqyrtimin ligjor, certifikimin e printerit/pagesave dhe ofruesin e identitetit.

## Rregulli i renditjes

Renditja ndjek varësitë: rakordimi para raporteve të avancuara; oraret para GTFS dhe mbikëqyrjes live; korrigjimet para kontrollit QR; identiteti i pajisjes para pagesave në shkallë prodhimi; ndarja e operatorëve para kontrollit final të publikimit.

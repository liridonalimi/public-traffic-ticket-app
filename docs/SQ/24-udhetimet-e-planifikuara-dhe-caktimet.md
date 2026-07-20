# Moduli 24 — Udhëtimet e planifikuara dhe caktimi i shoferit/mjetit

## Rezultati

Moduli 24 e zgjeron katalogun qendror me kalendarë shërbimi, udhëtime të planifikuara, orare të ndalesave, drejtim dhe caktime ditore të shoferit e autobusit.

Tableti e ruan planin bashkë me katalogun offline. Pas kyçjes, shoferi sheh vetëm detyrat e veta. Zgjedhja e detyrës cakton automatikisht autobusin dhe linjën, ndërsa turni ruan identifikuesin e udhëtimit, identifikuesin e caktimit dhe kohën reale të fillimit/mbarimit. Rrjedha ekzistuese pilot pa planifikim mbetet e disponueshme.

## Rrjedha e planifikimit qendror

Në hapësirën web të administratorit të katalogut:

1. krijoni kalendarin me interval datash dhe zgjidhni ditët me emër;
2. krijoni udhëtimin me linjë, kalendar, orën normale `HH:mm`, drejtimin dhe fushën `HH:mm` të krijuar për çdo ndalesë sipas rendit; shënoni “Next day” vetëm kur udhëtimi vazhdon pas mesnatës;
3. caktoni datën, shoferin dhe autobusin;
4. publikoni tërë ndryshimin si një revizion atomik.

Forma web i përkthen këto fusha të kuptueshme në formatin teknik të kontratës: numrat ISO të ditëve (`1` e hënë deri `7` e diel) dhe minutat e plota pas mesnatës. Zgjedhjet e udhëtimit dhe caktimit shfaqin intervalin e orës, linjën, drejtimin dhe ID-në që detyrat e ngjashme të dallohen qartë.

Publikimi refuzon referencat e panjohura, datat e pavlefshme, oraret jashtë rendit, datat joaktive dhe mbivendosjen e të njëjtit shofer ose autobus.

## Lista për validim të pavarur

1. Hapni `/admin` me tokenin e administratorit të katalogut dhe kontrolloni kalendarët, udhëtimet dhe caktimet.
2. Shtoni ose ndryshoni një kalendar, udhëtim dhe caktim; publikoni dhe kontrolloni rritjen e revizionit.
3. Provoni dy udhëtime që mbivendosen për të njëjtin shofer ose autobus dhe kontrolloni refuzimin.
4. Në Android rifreskoni të dhënat dhe kyçuni si shoferi i caktuar.
5. Zgjidhni detyrën; kontrolloni datën, nisjen, linjën dhe autobusin dhe që linja/autobusi nuk ndryshohen.
6. Zgjidhni turnin pa planifikim dhe kontrolloni se zgjedhja manuale vazhdon të punojë.
7. Pa rrjet, rindizni aplikacionin dhe kontrolloni se lista e detyrave ruhet.
8. Kryeni dhe sinkronizoni turnin; në raport kontrolloni udhëtimin dhe caktimin.

## Kufiri i modulit

Moduli planifikon dhe cakton punën. Zëvendësimi dinamik i shoferit, vonesat, GTFS dhe mbikëqyrja live trajtohen në module të mëvonshme.

## Rezultati i validimit të pavarur

Përfunduar më 20 korrik 2026. Validimi i pavarur në katalogun administrativ konfirmoi se kalendari pa asnjë ditë shërbimi refuzohet, zgjedhja e linjës krijon automatikisht fushat e orës për çdo ndalesë sipas rendit, ora më e hershme në ndalesën pasuese refuzohet dhe udhëtimi pas mesnatës pranohet vetëm kur ndalesat përkatëse shënohen “Next day”. Gjithashtu u konfirmua se zgjedhja e ditëve me emër dhe oraret `HH:mm` janë të qarta dhe miqësore për përdoruesin.

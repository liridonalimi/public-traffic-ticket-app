# Moduli 18 - Lokalizimi me shqipen si gjuhë kryesore

## Rezultati

Moduli 18 ofron ndërfaqe të plota në shqip dhe anglisht përmes burimeve të lokalizimit të Android-it. Në përdorimin e parë, aplikacioni ndjek gjuhën e pajisjes kur ajo mbështetet dhe përdor anglishten kur gjuha e pajisjes nuk mbështetet. Një zgjedhës brenda aplikacionit i ofron operatorit `Sipas gjuhës së pajisjes`, `Shqip` dhe `English`, edhe kur tableti nuk e ofron shqipen në cilësimet e sistemit.

Përvoja e shoferit, udhëtarëve, operacioneve, raportimit, linjës, tarifave, printimit, GPS-it, kërkesës për ndalje dhe sinkronizimit ndryshon gjuhë pa ndryshuar kontratat e biletave ose sinkronizimit.

## Terminologjia e operatorit në Kosovë

- Përdoren termat `Shoferi`, `Autobusi`, `Linja`, `Ndalesa` dhe `Turni`.
- Shitjet përdorin termin `para të gatshme`, ndërsa çmimet mbeten në euro.
- Tarifat demo janë `E rregullt`, `Student`, `Pensionist 65+` dhe `Fëmijë`.
- Linjat dhe ndalesat demo përdorin emra shqip, duke përfshirë `Qendër`, `Stacioni Qendror`, `Bulevardi Nënë Tereza` dhe `Bregu i Diellit`.
- Shkronjat shqipe `ë` dhe `ç` ruhen drejtpërdrejt në burimet UTF-8 të Android-it.

## Sjellja e gjuhës

Katalogu bazë `values/strings.xml` është në shqip dhe `values-en/strings.xml` ofron anglishten. Menaxheri i gjuhës e zgjidh sjelljen e pajisjes në mënyrë të qartë: pajisjet në shqip përdorin shqipen, pajisjet në anglisht përdorin anglishten dhe gjuhët e pambështetura përdorin anglishten. Zgjedhja e qartë brenda aplikacionit e tejkalon gjuhën e pajisjes dhe ruhet lokalisht.

Numrat dinamikë, identifikuesit, shumat, emrat e ndalesave dhe vlerat e raporteve përdorin burime të formatuara ose në shumës. Edhe mesazhet e krijuara nga view model-i dhe integrimet e pajisjes përdorin gjuhën aktive.

## Lista e validimit në tablet

1. Lëreni zgjedhjen te `Sipas gjuhës së pajisjes` dhe konfirmoni gjuhën e mbështetur ose anglishten për një gjuhë të pambështetur.
2. Hapni `Operacionet dhe konfigurimi`, zgjidhni `Shqip` dhe konfirmoni se aplikacioni hapet përsëri në shqip.
   Konfirmoni se menyja mbyllet pa ngecje dhe aplikacioni mbetet te Operacionet dhe konfigurimi.
3. Kryeni identifikimin, zgjedhjen e autobusit dhe linjës dhe fillimin e turnit.
4. Kontrolloni `ë`, `ç`, etiketat e gjata, kartat dhe dialogët për prerje të tekstit.
5. Shitni dhe printoni secilën tarifë; kontrolloni ekranin e udhëtarëve dhe dialogun e përfundimit të turnit.
6. Sinkronizoni një turn të mbyllur dhe hapni Operacionet dhe raportin administrativ.
7. Zgjidhni `English` brenda aplikacionit dhe përsëriteni rrjedhën kryesore.
8. Konfirmoni se zgjedhja ruhet pas rindizjes dhe se turnet e biletat e ruajtura mbeten.
9. Kthejeni zgjedhjen te `Sipas gjuhës së pajisjes`.

## Kriteret e pranimit

- Opsioni i pajisjes ndjek gjuhët e mbështetura dhe përdor anglishten për gjuhët e pambështetura.
- Shqipja dhe anglishtja mund të zgjidhen dhe të ruhen brenda aplikacionit.
- Rrjedha e plotë Android nuk ka etiketa të ndërfaqes vetëm në anglisht.
- Terminologjia për linjat, tarifat, turnet dhe paratë e gatshme është e njëtrajtshme.
- Ndryshimi i gjuhës nuk ndryshon identifikuesit, vlerat e biletave ose ngarkesat e sinkronizimit.
- Ndarja e roleve dhe mbrojtja e përfundimit të turnit nga Moduli 17 mbeten të paprekura.

## Statusi i validimit

Përfunduar. Testet automatike, ndërtimi debug dhe Android lint kaluan me sukses. Rrjedhat në shqip dhe anglisht, ruajtja dhe ndryshimi i gjuhës, mbyllja e menysë, puna e shoferit, ekrani i udhëtarëve, raportimi, printimi, përfundimi i turnit dhe sinkronizimi u validuan në pajisjen e pilotit.

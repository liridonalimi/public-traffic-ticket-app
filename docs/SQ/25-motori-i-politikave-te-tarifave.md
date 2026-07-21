# Moduli 25 — Motori i politikave të tarifave

Moduli 25 e shndërron çdo tarifë të menaxhuar në një politikë çmimi të parashikueshme, duke ruajtur tarifat fikse ekzistuese.

## Sjellja e produktit

- Tarifa mund të vlejë për të gjitha linjat ose vetëm për një linjë.
- Ndalesat kanë zona tarifore të përcaktuara nga operatori.
- Çmimit bazë mund t'i shtohet një shumë për çdo zonë shtesë.
- Një interval kohor mund të japë zbritje jashtë pikut, edhe kur kalon mesnatën.
- Tarifa mund të japë një afat vlefshmërie për transfer.
- Shoferi zgjedh destinacionin dhe sheh çmimin, numrin e zonave, zbritjen jashtë pikut dhe udhëzimin për transfer para shitjes.
- Vlerat zero ruajnë sjelljen e vjetër të tarifës fikse.

Shoferi vazhdon të zgjedhë kategorinë e pasagjerit. Meqë vlerësohet vetëm kategoria e zgjedhur, nuk nevojitet prioritet mes rregullave konkurruese në këtë kontratë.

## Siguria dhe auditimi

Publikimi refuzon linja të panjohura, intervale jo të plota ose me fillim dhe fund të njëjtë, vlera negative dhe zona të pavlefshme. Bileta e re ruan revisionin e katalogut, origjinën, destinacionin, zonat, rezultatin jashtë pikut, skadimin e transferit, kategorinë dhe shumën finale. Ndryshimet e ardhshme nuk e ndryshojnë shitjen historike.

## Testimi në pajisje

1. Publikoni një tarifë me të gjitha rregullimet zero dhe verifikoni çmimin e vjetër.
2. Vendosni dy zona dhe tarifë shtesë; krahasoni udhëtimin brenda zonës me kalimin në zonë tjetër.
3. Vendosni një interval jashtë pikut që përfshin orën aktuale dhe kontrolloni zbritjen.
4. Vendosni afatin e transferit dhe kontrolloni udhëzimin.
5. Krijoni tarifë vetëm për një linjë dhe verifikoni se shfaqet vetëm aty.
6. Shisni e sinkronizoni një biletë, ndryshoni tarifën dhe publikojeni sërish; shitja e vjetër duhet të ruajë çmimin dhe snapshot-in e saj.
7. Rinisni aplikacionin para sinkronizimit dhe verifikoni që bileta mbetet në pritje.

Kontrolli QR dhe përdorimi i transferit do të trajtohen në Modulin 27.

## Rezultati i testimit

Përfunduar më 21 korrik 2026. Testimi manual në tablet dhe në pamjen e raporteve konfirmoi se:

- ndryshimet e publikuara të çmimit fiks zëvendësojnë çmimin e mëparshëm në tablet;
- shtesa e zonës dhe zbritja jashtë pikut llogariten së bashku dhe ruhen pas sinkronizimit;
- tarifat për një linjë të caktuar shfaqen vetëm në atë linjë;
- afati i transferit dhe versioni i politikës ruhen në çdo biletë të sinkronizuar;
- totali i arkës shfaqet `MATCHED` kur arka e deklaruar përputhet me totalin e llogaritur.

Dëshmia përfundimtare në raport për `Module25-Offpeak-Test`:

- udhëtimi me dy zona kushtoi EUR 1.20: EUR 1.00 bazë + EUR 0.40 zonë shtesë - EUR 0.20 zbritje jashtë pikut;
- udhëtimi me një zonë kushtoi EUR 0.80: EUR 1.00 bazë - EUR 0.20 zbritje jashtë pikut;
- një turn përmbante EUR 1.20 + EUR 1.20 + EUR 0.80 = EUR 3.20;
- turni i dytë përmbante EUR 1.20 + EUR 0.80 = EUR 2.00;
- raporti i grupoi të pesë biletat në EUR 5.20 dhe shfaqi versionin 17 të politikës, ndalesat, numrin e zonave, zbritjen jashtë pikut dhe skadimin e transferit;
- të dy turnet u shfaqën si `MATCHED`.

# Moduli 19 - Simulimi i printerit dhe certifikimi

## Rezultati

Moduli 19 e mundëson validimin e kuponit para zgjedhjes së pajisjes së prodhimit. Piloti Android ofron simulatorë PDF të veçantë 58 mm dhe 80 mm, krijon dalje ESC/POS sipas gjerësisë, përmirëson pamjen e kuponit jo fiskal dhe e vendos kontrollin e certifikimit të pajisjes te Operacionet dhe konfigurimi.

## Simulatorët e kuponit

Zgjedhësi i printerit tani përfshin:

- `Simulator PDF 58 mm`, për printerët termikë mobilë me 32 kolona teksti;
- `Simulator PDF 80 mm`, për printerët më të gjerë me 48 kolona teksti.

Secili simulator krijon skedar PDF të veçantë me gjerësinë në emër. Të dy formatet përfshijnë tarifën, totalin, pagesën me para të gatshme, kodin e biletës, autobusin, linjën, operatorin, kohën e shitjes, QR-në e të dhënave lokale dhe shenjën e qartë jo fiskale.

## Cilësia ESC/POS

- Rreshtat e kuponit e vendosin çmimin në skajin e djathtë sipas gjerësisë.
- Linjat, operatorët dhe vlerat e gjata ndahen në kufijtë e fjalëve.
- Profili fizik Bluetooth mbetet formati konservativ 58 mm/32 kolona derisa modeli të certifikohet.
- Komandat e inicializimit, rreshtimit, trashësisë, furnizimit dhe prerjes mbeten aktive.
- Dështimi i printimit mbetet i ruajtur dhe i riprovueshëm; turni nuk mbyllet me bileta të paprintuara.

## Lista e validimit në tablet

1. Ndërmjet turneve, hapni zgjedhësin e printerit dhe konfirmoni të dy simulatorët PDF.
2. Zgjidhni simulatorin 58 mm, filloni turnin dhe shitni nga një biletë për secilën tarifë.
3. Hapni PDF-në e fundit dhe kontrolloni ndarjen e tekstit, rreshtimin, shumën, identifikuesit, QR-në dhe shenjat `JO FISKAL`.
4. Përfundoni turnin, zgjidhni simulatorin 80 mm, filloni turn tjetër dhe përsëriteni me emrat më të gjatë.
5. Konfirmoni se PDF-ja 80 mm është më e gjerë dhe emri përfundon me `-80mm.pdf`.
6. Rindizeni aplikacionin dhe konfirmoni se simulatori i zgjedhur rikthehet.
7. Nëse keni printer fizik, kaloni secilin kontroll më poshtë para miratimit.

## Kontrolli i certifikimit të pajisjes

Modeli fizik nuk certifikohet për prodhim pa kaluar të gjitha:

- çiftimi, rilidhja, rindizja e aplikacionit, printerit dhe furnizimit të automjetit;
- rrotulla e saktë 58/80 mm, furnizimi, margjinat, errësira dhe prerja;
- `ë`, `ç`, emrat e gjatë, secila tarifë, totalet dhe koha të lexueshme;
- skanimi i QR-së në ndriçim normal dhe shenja e qartë `JO FISKAL`;
- rikthimi nga mungesa e letrës, kapaku i hapur, Bluetooth-i i fikur dhe ndërprerja gjatë printimit;
- riprovimi printon të njëjtën biletë të ruajtur dhe nuk krijon shitje të dytë;
- përfundimi i turnit mbetet i bllokuar derisa bileta të printohet;
- 50 bileta radhazi pa shkëputje, prerje teksti, mbinxehje ose harxhim të papranueshëm të baterisë.

Para certifikimit shënoni prodhuesin, modelin, firmware-in, gjerësinë, sjelljen e shkronjave/code-page, datën, testuesin dhe dëshmitë.

## Kriteret e pranimit

- Të dy gjerësitë krijojnë kuponë të lexueshëm dhe qartësisht jo fiskalë.
- Teksti ESC/POS nuk e kalon gjerësinë e profilit.
- Vlerat e gjata ndahen pa humbur të dhënat e biletës.
- Mbrojtjet ekzistuese të dështimit dhe riprovimit mbeten të paprekura.
- Asnjë printer fizik nuk quhet i certifikuar pa kaluar listën e pajisjes.

## Statusi i validimit

Validimi i aplikacionit dhe simulatorëve u përfundua më 17 korrik 2026. PDF-të 58 mm dhe 80 mm u konfirmuan me gjerësi dukshëm të ndryshme, përmbajtje të lexueshme, QR brenda kufijve dhe pa prerje ose mbivendosje. Gjithashtu u konfirmua se simulatori i zgjedhur ruhet pasi aplikacioni mbyllet plotësisht dhe hapet përsëri.

Moduli 19 është përfunduar për aplikacionin dhe simulatorët. Certifikimi i printerit fizik mbetet në pritje sepse pajisja ende nuk është në dispozicion; çdo model duhet ta kalojë kontrollin e certifikimit para miratimit për prodhim.

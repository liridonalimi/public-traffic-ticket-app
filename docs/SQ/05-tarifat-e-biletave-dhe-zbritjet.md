# Tarifat e Biletave dhe Zbritjet

## Qëllimi

Moduli 05 e zëvendësoi çmimin e vetëm fiks me një katalog tarifash që mund të zgjidhen. Shoferi mund ta aplikojë tarifën e saktë standarde ose me zbritje, ndërsa tarifa e zgjedhur ruhet në secilën biletë.

## Tarifat e pilotit

- **Standarde:** EUR 0.50.
- **Student:** EUR 0.30; kërkohet kartelë e vlefshme studenti.
- **Të moshuar 65+:** EUR 0.25; për pasagjerë të moshës 65 vjeç ose më shumë.
- **Fëmijë:** EUR 0.20; për fëmijë të moshës 6 deri në 12 vjeç.

Këta emra, çmime dhe rregulla të përfitimit janë konfigurim pilot. Ende nuk merren nga serveri qendror.

## Çfarë mund të bëjë shoferi

- Ta zgjedhë tarifën gjatë turnit aktiv.
- Ta shohë çmimin para shitjes së biletës.
- T'i shohë udhëzimet për përfitimin e tarifave me zbritje.
- Të shesë bileta me tarifa të ndryshme brenda të njëjtit turn.
- Ta shohë numrin e biletave dhe nëntotalin e parave për secilën tarifë.
- Ta shohë të njëjtën ndarje në përmbledhjen e turnit të fundit të përfunduar.

## Historia e biletës dhe përputhshmëria

Secila biletë e ruan ID-në e llojit të tarifës dhe çmimin real të paguar. Ruajtja e çmimit e mbron historikun nëse katalogu ndryshon më vonë. Biletat e krijuara para këtij moduli nuk kishin ID tarife, prandaj gjatë leximit trajtohen në mënyrë të sigurt si bileta standarde.

## Vlera për biznesin

Ky modul i mbështet kategoritë reale të pasagjerëve, e përmirëson barazimin e arkës dhe përgatit raportimin sipas tarifave. Ndarja e katalogut nga çmimi historik është gjithashtu e rëndësishme për ndryshimet e ardhshme dhe auditimin.

## Kufizimet aktuale

- Katalogu është e dhënë lokale demo dhe nuk menaxhohet nga serveri.
- Aplikacioni jep udhëzime, por shoferi mbetet përgjegjës për kontrollimin e dëshmisë së përfitimit.
- Ende nuk ka periudhë vlefshmërie, çmime sipas zonave, transfer, abonim ose tarifa promocionale.
- Emrat dhe udhëzimet e tarifave ende nuk janë të lokalizuara plotësisht.

## Testimi dhe validimi

1. Filloni një turn dhe shitni nga një biletë Standarde, Student, Të moshuar 65+ dhe Fëmijë.
2. Para secilës shitje kontrolloni udhëzimin e përfitimit dhe çmimin e tarifës së zgjedhur.
3. Priten katër bileta dhe EUR 1.25: EUR 0.50, EUR 0.30, EUR 0.25 dhe EUR 0.20.
4. Përfundojeni turnin dhe konfirmoni të njëjtët numra dhe nëntotale sipas tarifës.
5. Rindizeni aplikacionin dhe kontrolloni që secila biletë ruan ID-në e tarifës dhe çmimin e paguar.
6. Në testin e përputhshmërisë, një biletë e vjetër pa ID tarife duhet të paraqitet si Standarde pa ndryshuar shumën e ruajtur.

Pranimi kërkon që ndarja sipas tarifave të rakordohet me arkën totale dhe çmimet historike të mbeten të pandryshueshme.

## Moduli i ardhshëm

Moduli 06 e lidhi secilën shitje të ruajtur me printimin Bluetooth ESC/POS dhe me rrjedhën e printerit testues PDF.

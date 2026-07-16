# Moduli 17 - Përvoja e pilotit për shoferin dhe ndarja e roleve

## Rezultati

Moduli 17 e bën pilotin Android më të qartë për shoferin, ndërsa kontrollet teknike dhe administrative i vendos në një hapësirë të veçantë për mbikëqyrësin.

Ekrani kryesor tani përqendrohet në detyrat e shoferit gjatë shërbimit:

- konfirmimin e shoferit të kyçur, autobusit dhe linjës;
- fillimin e turnit;
- përcjelljen e linjës dhe kërkesave për ndalesë;
- zgjedhjen e tarifës dhe shitjen e biletave;
- kontrollin e numrit të biletave dhe arkës së turnit;
- përfundimin e turnit me konfirmim;
- sinkronizimin e turneve të mbyllura pa parë kredencialet e serverit.

## Mbrojtjet për shoferin

- Statusi `READY TO START` ose `SHIFT ACTIVE` shfaqet qartë në pjesën e sipërme.
- Mjetet operative dhe të konfigurimit nuk mund të hapen gjatë turnit aktiv.
- Përfundimi i turnit kërkon konfirmim të qartë dhe shpjegon se shitja do të mbyllet.
- Endpoint-i, token-i, kontrollet demo dhe raportimi administrativ janë larguar nga rrjedha aktive e shoferit.
- Kur ka turne të mbyllura në pritje, shoferi ka vetëm veprimin e fokusuar `Sync Closed Shifts`.

## Hapësira Operations & Setup

Ndërmjet turneve, `Operations & Setup` hap hapësirën lokale të mbikëqyrësit për:

- zgjedhjen e sinkronizimit demo, lokal ose të konfiguruar për prodhim;
- vendosjen e endpoint-it dhe token-it që mbahet vetëm gjatë sesionit;
- validimin online/offline dhe riprovimin;
- sinkronizimin manual të të dhënave në pritje;
- hapjen e pamjes administrative të raportimit në aplikacion.

Kjo është ndarje e rrjedhës së punës për pilotin. Nuk është autentikim ose autorizim real sipas roleve. Këto mbeten pjesë e një moduli të ardhshëm të sigurisë.

## Lista e validimit nga shoferi

1. Kyçuni dhe konfirmoni statusin `READY TO START`.
2. Hapni `Operations & Setup` dhe kthehuni në konsolën e shoferit.
3. Filloni turnin dhe konfirmoni se butoni i mjeteve operative nuk shfaqet më.
4. Shitni bileta dhe kontrolloni numrin, arkën, totalet sipas tarifës dhe ekranin e pasagjerëve.
5. Shtypni `End Shift`, anuloni një herë me `Keep Shift Open` dhe konfirmoni se turni vazhdon.
6. Shtypni përsëri `End Shift` dhe konfirmojeni.
7. Kontrolloni që turni i mbyllur shfaqet në statusin e sinkronizimit.
8. Shtypni `Sync Closed Shifts` dhe kontrolloni që numëruesit në pritje bëhen zero.
9. Hapni `Operations & Setup` dhe kontrolloni konfigurimin e serverit dhe raportimin.

## Kriteret e pranimit

- Shoferi e kryen turnin e plotë pa hyrë në hapësirën e mbikëqyrësit.
- Kontrollet e mbikëqyrësit nuk janë në dispozicion gjatë shërbimit.
- Turni nuk mund të përfundojë me një prekje aksidentale.
- Të dhënat e turnit të mbyllur mbeten të riprovueshme dhe sinkronizohen si më parë.
- Printeri, linja, ekrani i pasagjerëve, kërkesa për ndalesë, raportimi dhe puna offline vazhdojnë të funksionojnë.

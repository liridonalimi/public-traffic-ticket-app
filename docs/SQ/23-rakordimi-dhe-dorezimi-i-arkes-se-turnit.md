# Moduli 23 — Rakordimi dhe dorëzimi i arkës së turnit

## Rezultati

Moduli 23 e bën mbylljen e turnit një dorëzim të gjurmueshëm të arkës. Para përfundimit, aplikacioni shfaq arkën e pritur nga biletat dhe kërkon shumën fizike të numëruar. Turni ruan shumën e pritur, shumën e deklaruar, diferencën me shenjë, kohën e rakordimit dhe statusin: përputhet, mungesë, tepricë ose pa rakordim për të dhënat e vjetra.

Shuma konvertohet drejtpërdrejt në centë, pa llogaritje floating-point. Refuzohen vlerat negative, teksti, më shumë se dy shifra dhjetore dhe vlerat mbi kufirin e sigurisë. Zeroja pranohet për turn pa arkë.

## Rrjedha e shoferit

1. Përfundoni shitjet dhe zgjidhni çdo printim në pritje ose të dështuar.
2. Shtypni **Përfundo turnin**.
3. Kontrolloni arkën e pritur.
4. Numëroni paratë fizike dhe shkruani shumën në EUR, p.sh. `2.90`.
5. Kontrolloni diferencën: zero do të thotë përputhje, vlerë negative mungesë dhe pozitive tepricë.
6. Shtypni **Konfirmo dorëzimin**. Pa shumë të vlefshme butoni mbetet i çaktivizuar dhe turni vazhdon të jetë i hapur.

Pas mbylljes, paneli shfaq arkën e pritur, të deklaruar, diferencën dhe statusin. Regjistri ruhet offline dhe pret sinkronizimin normal me konfirmim nga serveri.

## Raportimi dhe sinkronizimi

Regjistri i turnit dërgon së bashku `expectedCashCents`, `declaredCashCents` dhe `reconciledAtMillis`. Serveri refuzon rakordimin e pjesshëm. Turnet e vjetra pa këto fusha mbeten të vlefshme dhe shfaqen si `NOT_RECORDED`; sistemi nuk shpik deklarata historike.

Raporti Android dhe paneli web shfaqin:

- totalin e pritur dhe të deklaruar;
- diferencën totale me shenjë;
- numrin e turneve të rakorduara dhe pa rakordim;
- vlerat dhe statusin për çdo turn.

Pranimi në server mbetet transaksional dhe idempotent. Backup/restore i ruan vlerat e rakordimit.

## Lista e validimit të pavarur

### Turni që përputhet

1. Filloni turnin dhe shitni disa bileta.
2. Shtypni **Përfundo turnin** dhe krahasoni arkën e pritur me totalin e biletave.
3. Verifikoni që fusha e zbrazët ose e pavlefshme nuk e mbyll turnin.
4. Shkruani saktësisht shumën e pritur dhe konfirmoni.
5. Kontrolloni që përmbledhja shfaq diferencë zero dhe status përputhjeje.

### Mungesa ose teprica

1. Përfundoni një turn tjetër me së paku një biletë.
2. Shkruani shumë më të vogël ose më të madhe se arka e pritur.
3. Kontrolloni shenjën e diferencës dhe tekstin mungesë/tepricë para dhe pas konfirmimit.

### Ruajtja, sinkronizimi dhe raporti

1. Mbylleni dhe rihapeni aplikacionin para sinkronizimit; turni duhet të mbetet në pritje.
2. Konfiguroni serverin lokal dhe sinkronizoni.
3. Hapni panelin e lexuesit të raportit dhe rifreskojeni.
4. Kontrolloni të njëjtat vlera të pritura, të deklaruara, diferencën dhe statusin.
5. Kontrolloni kartat e totalit dhe që turnet e vjetra shfaqen qartë pa rakordim.

## Kufiri i përfundimit

Ky modul regjistron deklaratën e shoferit për arkën e numëruar. Nuk përfshin miratimin e mbikëqyrësit, numrin e çantës/depozitës, prerjet monetare, anulimin e biletave, rimbursimet ose integrimin kontabël. Korrigjimet e kontrolluara vijnë në Modulin 26 dhe eksportet e avancuara në Modulin 28.

# Moduli 26 — Anulimet, korrigjimet dhe riprintimet e kontrolluara

Moduli 26 shton veprime të pandryshueshme pas shitjes pa e fshirë ose ndryshuar biletën origjinale. Në hapësirën **Operacionet dhe konfigurimi**, mbikëqyrësi zgjedh një biletë të mbyllur, arsyen dhe identifikuesin e autorizimit.

- Anulimi e ul të ardhurën efektive në zero vetëm një herë.
- Korrigjimi ruan tarifën dhe shumën zëvendësuese, ndërsa shitja origjinale mbetet e dukshme.
- Riprintimi përdor të njëjtën biletë dhe nuk krijon shitje ose të ardhur të re.
- Veprimet ruhen offline, sinkronizohen me ID të veçanta dhe paraqiten në raportet Android dhe web.

Një biletë mund të ketë vetëm një anulim ose korrigjim financiar, por mund të ketë disa riprintime. Serveri refuzon ID-të konfliktuese dhe e trajton riprovimin identik pa dyfishim.

## Testimi dhe validimi

1. Krijoni një turn me dy bileta, mbylleni me arkë të rakorduar dhe sinkronizojeni.
2. Anuloni njërën biletë me arsye dhe ID mbikëqyrësi. Pritet që bileta të mbetet, ndërsa e ardhura efektive të ulet vetëm një herë.
3. Provoni anulim ose korrigjim të dytë financiar në të njëjtën biletë. Pritet refuzim pa ndryshim të totalit.
4. Korrigjoni biletën tjetër me tarifë dhe çmim të ri. Raporti duhet të tregojë veçmas të ardhurën bruto dhe efektive.
5. Riprintoni një biletë. ID-ja, numri i biletave dhe të ardhurat duhet të mbeten të njëjta; vetëm numri dhe historiku i riprintimeve rriten.
6. Regjistroni një veprim pa rrjet, rindizeni aplikacionin dhe kontrolloni që mbetet në pritje.
7. Sinkronizoni me serverin real dhe rifreskoni raportin web. Kontrolloni llojin, arsyen, mbikëqyrësin, kohën dhe lidhjen me biletën origjinale.
8. Përsëriteni sinkronizimin. Pritet asnjë dublikatë dhe asnjë ndryshim i dytë i të ardhurave.

Validimi i pavarur në tablet dhe në panelin web përfundoi më 21 korrik 2026. U konfirmuan një anulim, dy korrigjime dhe dy riprintime, refuzimi i veprimit të dytë financiar në të njëjtën biletë, sinkronizimi i sigurt i varësive origjinale dhe paraqitja e të ardhurave bruto dhe efektive. Moduli 26 është i përfunduar.

Identiteti i fortë i mbikëqyrësit, anulimi fiskal, rimbursimet dhe kontabiliteti mbeten integrime të jashtme.

# Moduli 22 — Rotacioni i kredencialeve dhe auditimi i autorizimit

**Statusi: përfunduar.** Verifikimi automatik dhe validimi i pavarur në tablet/web mbuluan mbivendosjen, kalimin te kredencialet e reja, refuzimin e kredencialeve të revokuara, rifreskimin e katalogut, sinkronizimin, izolimin e roleve dhe përditësimin e auditimit.

## Rezultati

Moduli 22 shton dy kontrolle të prodhimit të pavarura nga ofruesi:

- çdo skedar sekret i rolit mund të përmbajë një kredencial aktiv ose dy kredenciale të ndryshme gjatë rotacionit të kontrolluar;
- çdo vendim autorizimi në API ruhet si i lejuar, i ndaluar ose i paautentikuar dhe shfaqet në një hapësirë të izoluar për auditorin e sigurisë.

Kredencialet nuk shkruhen në bazën e të dhënave, përgjigjen e API-së, browser storage, URL ose tabelën e auditimit. Regjistri përmban vetëm kohën, rezultatin, metodën/rrugën HTTP, rolet dhe burimin e kërkesës. Ruajtja lokale kufizohet në 10,000 ngjarjet më të fundit.

## Protokolli i rotacionit

Për rolin që po ndryshohet:

1. Mbajeni kredencialin ekzistues në rreshtin e parë të skedarit të mbrojtur.
2. Shtojeni kredencialin e ri në rreshtin e dytë.
3. Riniseni shërbimin dhe verifikoni të dy kredencialet.
4. Kaloni çdo klient të autorizuar në kredencialin e ri.
5. Hiqeni rreshtin e vjetër, riniseni dhe verifikoni që kredenciali i vjetër kthen HTTP 401, ndërsa i riu vazhdon të punojë.

Kontrolli staging refuzon më shumë se dy vlera, vlerat e përsëritura, ripërdorimin mes roleve, vlerat e dobëta dhe lejet e pasigurta të skedarit.

## Validimi lokal

Niseni Docker-in dhe hapni `http://127.0.0.1:8080/admin`. Përdorni kredencialin lokal të auditorit. Duhet të shfaqet vetëm **Authorization audit**; raportimi dhe katalogu duhet të mungojnë.

Krijoni ngjarje duke provuar kredencialin e raportit, kredencialin e pajisjes në web, një kredencial të gabuar dhe pastaj auditorin përsëri. Pas rifreskimit duhet të shfaqen vendime të lejuara, të ndaluara dhe të paautentikuara pa asnjë tekst kredenciali.

Në tablet provoni kredencialin e vjetër dhe atë të ri të pajisjes. Gjatë dritares së mbivendosjes, të dy duhet të rifreskojnë katalogun dhe të sinkronizojnë turnet e mbyllura.

Validimi i pavarur përfundoi të gjithë ciklin: të dy kredencialet punuan gjatë mbivendosjes, klientët kaluan te kredencialet e reja, kredencialet e vjetra të auditorit dhe pajisjes u hoqën, ndërsa gjendja përfundimtare i refuzoi ato dhe vazhdoi të autorizonte kredencialet e reja.

## Kufiri i përfundimit

Moduli përcakton dhe verifikon protokollin lokal të rotacionit/auditimit. Eksporti i logjeve, politika e ruajtjes, alarmet, federimi i identitetit, revokimi automatik dhe integrimi SIEM varen nga infrastruktura dhe ofruesi i identitetit që do të zgjidhen.

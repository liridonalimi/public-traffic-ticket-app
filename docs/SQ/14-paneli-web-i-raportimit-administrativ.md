# Moduli 14 - Paneli Web i Autentikuar për Raportim Administrativ

## Qëllimi

Moduli 14 e kthen versionin 1 të kontratës së raportimit në një përvojë reale administrative në web. Paneli shërbehet nga shërbimi ekzistues BusPay, kërkon raportin e mbrojtur nga i njëjti origin dhe paraqet totalet e rakorduara pa shtuar framework JavaScript ose shërbim tjetër runtime.

## Çfarë u shtua

- Paneli responsiv **BusPay Control** në `/admin`.
- Ekrani i lidhjes me bearer token, refuzim dhe dalje.
- Metrikat për të ardhurat, biletat, turnet e mbyllura dhe shoferët.
- Përzierja e tarifave me numër bilete dhe të ardhura.
- Totalet e turneve, biletave dhe të ardhurave sipas shoferit.
- Regjistri i zgjerueshëm i turneve me biletat individuale.
- Filtra sipas shoferit/tarifës dhe kërkim sipas turnit/autobusit/linjës.
- Gjendje boshe, ngarkimi, suksesi dhe gabimi të autentikimit.
- Dataset për validim me disa shoferë në `server/sample-admin-sync.json`.
- Header-a sigurie dhe qasje në API vetëm nga i njëjti origin.

## Sjellja e qasjes dhe token-it

HTML, CSS dhe JavaScript janë publike që ekrani i hyrjes të ngarkohet. Të dhënat mbeten të mbrojtura në `/v1/reports/admin`.

Operatori shkruan bearer token në fushë password. Pas autentikimit të suksesshëm:

- fusha e dukshme pastrohet;
- token-i mbetet vetëm në memorien JavaScript të faqes aktive;
- nuk vendoset në URL, cookies, `localStorage` ose `sessionStorage`;
- dalja ose dështimi i autentikimit pastron token-in dhe raportin.

Token-i i përbashkët përdoret vetëm për validimin lokal të pilotit. Prodhimi kërkon identitet administrativ me kredenciale jetëshkurtra dhe sipas roleve.

## Testimi i dyfishtë lokal me Docker

Mbajeni Docker Desktop aktiv dhe nisuni nga rrënja e projektit.

1. Ekzekutoni testet:

```bash
PYTHONPATH=server:. python3 -m unittest discover -s server/tests -v
```

Priten **18 teste** dhe `OK`.

2. Krijoni sekretin lokal të përjashtuar nga Git:

```bash
cp deployment/secrets/buspay_sync_token.txt.example \
  deployment/secrets/buspay_sync_token.txt
```

Zëvendësoni përmbajtjen me:

```text
module-14-local-dashboard-token-2026
```

3. Ndërtoni dhe nisni shërbimin:

```bash
docker compose -f deployment/compose.yaml up --build -d
docker compose -f deployment/compose.yaml ps
```

Prisni derisa statusi të jetë `healthy`.

4. Ngarkoni raportin testues:

```bash
curl -i -X POST http://127.0.0.1:8080/v1/sync \
  -H 'Authorization: Bearer module-14-local-dashboard-token-2026' \
  -H 'Content-Type: application/json' \
  -H 'X-BusPay-Contract-Version: 1' \
  -H 'Idempotency-Key: sync-2222333344445555' \
  --data-binary @server/sample-admin-sync.json
```

Priten HTTP 200 dhe konfirmime për 4 turne e 12 bileta.

5. Hapni [http://127.0.0.1:8080/admin](http://127.0.0.1:8080/admin).

6. Provoni fillimisht token të gabuar. Duhet të shfaqet **The access token was rejected.** pa të dhëna raporti.

7. Vendosni `module-14-local-dashboard-token-2026`. Konfirmoni:

- Të ardhurat: **€4.00**
- Biletat: **12**
- Turnet e mbyllura: **4**
- Shoferët: **3**
- driver-001: 2 turne, 6 bileta, €2.25
- driver-002: 1 turn, 3 bileta, €0.80
- driver-003: 1 turn, 3 bileta, €0.95

8. Validoni ndërveprimet:

- filtroni `driver-002` dhe konfirmoni 1 nga 4 turne;
- kombinoni `driver-002` me tarifën `standard` dhe konfirmoni gjendjen boshe;
- rivendosni filtrat dhe kërkoni `01-417-KS` për `shift-admin-004`;
- zgjeroni turnin dhe konfirmoni tri biletat;
- rifreskoni dhe konfirmoni totalet;
- dilni dhe konfirmoni kthimin në gjendjen **Locked**.

9. Ngushtoni browser-in ose testoni në pajisje të ngushtë. Kartat, tabelat, filtrat dhe detajet duhet të mbeten të lexueshme pa overflow horizontal të faqes.

10. Pastroni mjedisin:

```bash
docker compose -f deployment/compose.yaml down -v
rm deployment/secrets/buspay_sync_token.txt
docker compose -f deployment/compose.yaml ps --all
```

Lista finale duhet të jetë boshe.

## Kontrollet e sigurisë

- API-ja e raportit kërkon ende bearer authentication.
- Përgjigjet statike përdorin `Cache-Control: no-store`.
- Content Security Policy lejon script, style dhe lidhje API vetëm nga i njëjti origin.
- Kufizohen framing, referrer, MIME sniffing, kamera, lokacioni dhe mikrofoni.
- Vlerat dinamike vendosen me API tekstuale të DOM-it, jo me interpolim HTML.
- Container-i mbetet pa root dhe me root filesystem read-only.

## Kufizimet aktuale

- Ende nuk ka role ose hyrje administrative jetëshkurtër.
- Ende nuk ka eksport, faqosje, interval datash, grafikë kohore ose audit log.
- Shfaqen ID-të e shoferit, autobusit dhe linjës sepse serveri nuk ka ende tabela referuese me emra.
- Compose mbetet HTTP vetëm në localhost; staging/prodhimi kërkon HTTPS.
- Paneli në Modulin 14 është vetëm anglisht; dokumentacioni është dygjuhësh.

## Faza e ardhshme

Vendoseni shërbimin dhe panelin në staging të zotëruar pas HTTPS, lidhni identitet sipas roleve dhe shtoni privatësi, auditim, eksport, monitorim, faqosje dhe qeverisje.

## Referencat

- [W3C Content Security Policy Level 3](https://www.w3.org/TR/CSP/)
- [MDN Cache-Control](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Cache-Control)
- [MDN X-Content-Type-Options](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/X-Content-Type-Options)

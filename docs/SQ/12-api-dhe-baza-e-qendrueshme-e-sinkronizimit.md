# API dhe Baza e Qëndrueshme e Sinkronizimit

## Qëllimi

Moduli 12 implementon anën e serverit të kontratës së sinkronizimit nga Moduli 11 si shërbim referues i ekzekutueshëm. Ai autentikon kërkesat, validon payload-in e plotë të versionit 1, shkruan turnet dhe biletat në një bazë transaksionale SQLite, kthen konfirmime strikte, trajton riprovimet dhe ekspozon të dhënat e sinkronizuara për administrim/raportim.

Ky modul është implementuar lokalisht, por nuk paraqitet si vendosje në cloud. Para përdorimit të jashtëm duhet të zgjidhen hosti, domain-i, baza e menaxhuar, certifikata TLS, menaxhimi i sekreteve dhe ofruesi i identitetit.

## Çfarë u shtua

- Endpoint-i i autentikuar `POST /v1/sync`.
- Endpoint-i `GET /health` për gatishmërinë e bazës.
- Endpoint-i i autentikuar `GET /v1/reports/admin`.
- Verifikimi i bearer token me krahasim në kohë konstante.
- Verifikimi i versionit të kontratës dhe header-it të idempotencës.
- Validimi i madhësisë së paketës, formës së regjistrimeve, kohës, çmimit, unikësisë dhe lidhjeve.
- Tabelat transaksionale SQLite për kërkesat, turnet dhe biletat.
- Foreign key ndërmjet biletave dhe turneve.
- Ripërsëritja stabile e të njëjtës kërkesë pa duplikim.
- Refuzimi HTTP `409` kur e njëjta ID ripërdoret me të dhëna të ndryshuara.
- Agregimi i qëndrueshëm sipas shoferit, tarifës, turnit, biletës dhe arkës.
- Nisja opsionale me certifikatë/çelës TLS.
- Përgjigje të sigurta pa detaje të exception-it ose bazës.

## Cikli i të dhënave

1. Aplikacioni Android mbyll turnin dhe krijon paketën stabile.
2. Shërbimi verifikon tokenin dhe header-at.
3. Kontrata JSON validohet para qasjes në bazë.
4. Shërbimi fillon një transaksion të menjëhershëm.
5. Kërkesa, turnet dhe biletat shkruhen së bashku.
6. Transaksioni konfirmohet para kthimit të acknowledgement-it.
7. Android shënon si të sinkronizuara vetëm ID-të e konfirmuara.

Nëse një regjistrim dështon, transaksioni kthehet plotësisht prapa. Riprovimi identik kthen të njëjtin konfirmim me `X-Idempotent-Replay: true`.

## Testimi lokal i dyfishtë

Përdorni dy terminale nga rrënja e projektit.

Terminali 1:

```bash
PYTHONPATH=server \
BUSPAY_SYNC_TOKEN=module-12-local-token \
BUSPAY_DB_PATH=/tmp/buspay-module-12.db \
python3 -m buspay_server
```

Terminali 2 — health:

```bash
curl http://127.0.0.1:8080/health
```

Terminali 2 — sinkronizimi:

```bash
curl -i -X POST http://127.0.0.1:8080/v1/sync \
  -H 'Authorization: Bearer module-12-local-token' \
  -H 'Content-Type: application/json' \
  -H 'X-BusPay-Contract-Version: 1' \
  -H 'Idempotency-Key: sync-1111222233334444' \
  --data-binary @server/sample-sync.json
```

Ekzekutoni të njëjtën komandë dy herë. Përgjigjja e parë duhet të ketë `X-Idempotent-Replay: false`, e dyta `true`. Të dyja duhet të kenë të njëjtat ID të turnit dhe biletës.

Terminali 2 — raporti:

```bash
curl http://127.0.0.1:8080/v1/reports/admin \
  -H 'Authorization: Bearer module-12-local-token'
```

Konfirmoni `driverCount: 1`, `shiftCount: 1`, `ticketCount: 1` dhe `cashTotalCents: 50`. Në aplikacion konfirmoni tekstin **Reference API/database: implemented • deployment pending** dhe funksionimin e sinkronizimit demo.

## Testet automatike

```bash
PYTHONPATH=server python3 -m unittest discover -s server/tests -v
```

## Kufizimet aktuale

- Serveri WSGI i integruar është vetëm për validim lokal, jo për trafik publik.
- HTTP lokal përdoret vetëm nga terminali. Klienti Android i prodhimit kërkon HTTPS të besueshëm.
- Tokeni nga environment demonstron kontrollin, por nuk zëvendëson tokenët jetëshkurtër.
- SQLite është i përshtatshëm për referencë; prodhimi mund të kërkojë PostgreSQL të menaxhuar.
- Raporti ka ID operative, por jo katalog serveri për emrat e shoferëve, autobusëve dhe linjave.
- Ky modul nuk ka ndryshuar infrastrukturë cloud.

## Faza e ardhshme

Zgjidhni infrastrukturën dhe identitetin e prodhimit, vendoseni kontratën pas TLS të menaxhuar me bazë të menaxhuar dhe lidhni Android me kredenciale jetëshkurtra.

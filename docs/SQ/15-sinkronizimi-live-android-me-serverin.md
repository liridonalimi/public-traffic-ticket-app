# Moduli 15 - Sinkronizimi Live Android me Serverin

## Qëllimi

Moduli 15 lidh build-in debug të aplikacionit Android me API-në reale lokale të BusPay. Shoferi mund ta mbyllë turnin, t'i dërgojë turnin dhe biletat në shërbimin Docker dhe pastaj t'i shohë në panelin web pas rifreskimit të raportit.

Mënyra demo mbetet zgjedhja fillestare. Lidhja me serverin aktivizohet shprehimisht vetëm për sesionin aktual të aplikacionit.

## Çfarë u shtua

- Zgjedhja gjatë ekzekutimit ndërmjet demo, validimit lokal dhe HTTPS të prodhimit.
- Konfigurimi në aplikacion i endpoint-it dhe bearer token-it.
- Token-i mbahet vetëm në memorien e sesionit; pastrohet nga fusha dhe nuk ruhet në repository ose Android preferences.
- HTTP loopback vetëm për debug dhe validim me `adb reverse`.
- Kufizimi i loopback në `127.0.0.1`, `localhost` dhe `::1`.
- HTTPS mbetet i detyrueshëm për prodhim dhe release.
- Test real loopback për HTTP, autentikim, idempotency, contract version, payload dhe acknowledgement.

## Testi i dyfishtë me pajisje fizike

### 1. Përgatit Docker-in

```bash
cp deployment/secrets/buspay_sync_token.txt.example \
  deployment/secrets/buspay_sync_token.txt
```

Vendos këtë token në skedar:

```text
module-15-local-bridge-token-2026
```

```bash
docker compose -f deployment/compose.yaml up --build -d
docker compose -f deployment/compose.yaml ps
curl http://127.0.0.1:8080/health
```

Pritet: container-i është `healthy` dhe health kthen `"status":"ok"`.

### 2. Lidh pajisjen Android

Aktivizo Developer options dhe USB debugging, lidhe tabletin me USB, prano autorizimin dhe ekzekuto:

```bash
adb devices -l
adb reverse tcp:8080 tcp:8080
adb reverse --list
```

Tableti duhet të shfaqet si `device`, ndërsa lista reverse duhet të përmbajë `tcp:8080 tcp:8080`.

`adb reverse` bën që `127.0.0.1:8080` në tablet të arrijë portin localhost të Docker-it në Mac. Kjo është urë zhvillimi, jo arkitekturë prodhimi.

### 3. Instalo dhe konfiguro aplikacionin debug

Ekzekuto konfigurimin `app` nga Android Studio.

Në konsolën e shoferit:

1. Gjej **Sync service**.
2. Shtyp **Configure Sync Server**.
3. Mbaj endpoint-in:

```text
http://127.0.0.1:8080/v1/sync
```

4. Vendos token-in:

```text
module-15-local-bridge-token-2026
```

5. Shtyp **Activate Server**.

Verifiko:

- **Active mode: Local server validation**;
- **Local validation server: configured**;
- fusha e token-it zhduket dhe pastrohet;
- butoni **Use Demo Mode** e kthen aplikacionin në klientin demo.

### 4. Valido ruajtjen pas dështimit

1. Konfiguro fillimisht një token të gabuar.
2. Mbyll një turn të shkurtër me të paktën një biletë.
3. Shtyp **Sync Now**.
4. Verifiko refuzimin e autentikimit.
5. Verifiko se turni dhe biletat mbeten në pritje.
6. Rikonfiguro token-in e saktë.

Asnjë rekord nuk duhet të shënohet i sinkronizuar pas një kërkese të refuzuar.

### 5. Valido sinkronizimin live

1. Kyçu si shofer.
2. Fillo një turn.
3. Shit bileta me të paktën dy tarifa.
4. Mbyll turnin.
5. Verifiko turnin dhe biletat në pritje.
6. Shtyp **Sync Now**.
7. Verifiko numrin e turneve dhe biletave të konfirmuara.
8. Verifiko se numrat në pritje bëhen zero.

Në Mac hap [http://127.0.0.1:8080/admin](http://127.0.0.1:8080/admin), fut të njëjtin token dhe shtyp **Refresh report**. Turni i ri, autobusi, linja, tarifat, biletat dhe të ardhurat duhet të shfaqen në ledger.

Paneli nuk bën push automatik në Modulin 15; **Refresh report** kërkon raportin më të fundit.

### 6. Kontroll opsional i API-së

```bash
curl --fail --silent --show-error \
  http://127.0.0.1:8080/v1/reports/admin \
  -H 'Authorization: Bearer module-15-local-bridge-token-2026'
```

JSON-i duhet të përmbajë ID-të e turnit dhe biletave të reja.

### 7. Pastrimi

```bash
adb reverse --remove tcp:8080
docker compose -f deployment/compose.yaml down -v
rm deployment/secrets/buspay_sync_token.txt
docker compose -f deployment/compose.yaml ps --all
```

Lista përfundimtare e Compose duhet të jetë bosh.

## Validimi automatik

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug

PYTHONPATH=server:. python3 -m unittest discover -s server/tests -v
```

## Kufijtë e sigurisë

- Demo është mënyra fillestare.
- Cleartext aktivizohet vetëm në manifestin debug.
- Klienti pranon cleartext vetëm për hostet loopback.
- HTTP jashtë loopback refuzohet edhe në validimin lokal.
- Konfigurimi i prodhimit pranon vetëm HTTPS.
- Release ruan `usesCleartextTraffic="false"`.
- Token-i qëndron në memorien e ViewModel-it dhe nuk ruhet lokalisht.
- Prodhimi kërkon HTTPS të besuar dhe kredenciale afatshkurtra.

## Kufizimet aktuale

- Konfigurimi humbet kur procesi i aplikacionit rikrijohet.
- Ura lokale kërkon USB debugging dhe `adb reverse`.
- Paneli web kërkon **Refresh report**.
- Token-i i përbashkët lokal është vetëm për zhvillim.
- Domain-i, TLS, identity, token refresh dhe certificate policy të prodhimit mbeten punë infrastrukturore.

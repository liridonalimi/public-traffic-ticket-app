# Moduli 16 - Gatishmëria e publikimit staging

## Qëllimi

Moduli 16 krijon një portë të sigurt publikimi ndërmjet pilotit lokal të validuar dhe një mjedisi të ardhshëm staging. Ai nuk pretendon se ekziston një mjedis publik. Hyrjet e publikimit bëhen të qarta, konfigurimi i pasigurt refuzohet dhe kontrollet pas publikimit janë të përsëritshme.

## Çfarë u shtua

- Profil Compose staging që përdor imazh të pandryshueshëm nga registry.
- Lidhje me rrjetin privat të ingress-it pa port publik në host.
- Ruajtja e kontrolleve ekzistuese: root filesystem vetëm për lexim, përdorues jo-root, heqje e capabilities, secret file, volume të qëndrueshme, health check dhe ndalim gradual.
- Kontroll paraprak për origin HTTPS, digest-in e imazhit, skedarin e mbrojtur të token-it, rajonin, afatin e backup-it dhe pronarët e operacioneve/sigurisë/backup-it.
- Smoke test pa ndryshim të të dhënave për health, bazën, raportimin e autentikuar, kontratën, cache protection dhe refuzimin e token-it të gabuar.
- Shembull konfigurimi me placeholder-a, pa kredenciale.

## Testimi i dyfishtë lokal

### 1. Testet automatike

```bash
PYTHONPATH=server:. python3 -m unittest discover -s server/tests -v

JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

### 2. Kontrolli i konfigurimit staging

Krijoni skedarë të përkohshëm jashtë repository-t:

```bash
printf '%s\n' 'module-16-staging-validation-token-2026' > /tmp/buspay-staging-token
chmod 600 /tmp/buspay-staging-token
cp deployment/staging.env.example /tmp/buspay-staging.env
```

Në `/tmp/buspay-staging.env` zëvendësoni të gjithë placeholder-at me URL HTTPS, imazh me `sha256`, rrjetin privat, `/tmp/buspay-staging-token`, rajonin, tre pronarët dhe ruajtjen 14-ditore. Pastaj ekzekutoni:

```bash
PYTHONPATH=. python3 -m deployment.staging_preflight \
  --env-file /tmp/buspay-staging.env

docker compose --env-file /tmp/buspay-staging.env \
  -f deployment/compose.staging.yaml config
```

Rezultati i pritur: `BusPay staging preflight: READY`; Compose ka vetëm `expose: 8080`, pa `ports` publike, secret file, volume dhe rrjet privat të jashtëm.

### 3. Smoke test lokal

```bash
cp deployment/secrets/buspay_device_token.txt.example deployment/secrets/buspay_device_token.txt
cp deployment/secrets/buspay_report_token.txt.example deployment/secrets/buspay_report_token.txt
cp deployment/secrets/buspay_catalog_token.txt.example deployment/secrets/buspay_catalog_token.txt
cp deployment/secrets/buspay_audit_token.txt.example deployment/secrets/buspay_audit_token.txt

docker compose -f deployment/compose.yaml up --build -d

PYTHONPATH=. python3 -m deployment.staging_smoke \
  --base-url http://127.0.0.1:8080 \
  --report-token-file deployment/secrets/buspay_report_token.txt \
  --audit-token-file deployment/secrets/buspay_audit_token.txt \
  --allow-local-http
```

Rezultati i pritur: `BusPay staging smoke: PASS`, totalet e raportit, `Invalid token: rejected` dhe `Audit role: isolated`.

Pastrimi:

```bash
docker compose -f deployment/compose.yaml down -v
rm deployment/secrets/buspay_device_token.txt deployment/secrets/buspay_report_token.txt deployment/secrets/buspay_catalog_token.txt deployment/secrets/buspay_audit_token.txt
rm /tmp/buspay-staging-device-token /tmp/buspay-staging-report-token /tmp/buspay-staging-catalog-token /tmp/buspay-staging-audit-token /tmp/buspay-staging.env
```

## Aktivizimi real staging

Pas zgjedhjes së infrastrukturës, registry-t, domain-it, TLS ingress dhe pronarëve:

1. Ndërtoni dhe skanoni imazhin në CI.
2. Publikojeni dhe regjistroni digest-in.
3. Krijoni token-in në secret manager.
4. Krijoni storage për të dhënat dhe backup-et.
5. Lidheni shërbimin vetëm me ingress-in privat.
6. Përfundoni HTTPS te ingress-i i menaxhuar.
7. Ekzekutoni preflight-in.
8. Publikoni një instancë dhe prisni health check-un.
9. Ekzekutoni smoke test-in ndaj origin-it HTTPS pa `--allow-local-http`.
10. Sinkronizoni një turn testues nga Android dhe kontrolloni raportin përsëri.
11. Regjistroni digest-in, pronarët, politikën e backup-it, rezultatin dhe aprovimin e rollback-ut.

## Kufiri aktual

Repository është gati për staging, por publikimi i parë kërkon ofrues infrastrukture, registry, domain/DNS, TLS të menaxhuar, secret manager, storage dhe pronarë operativë. Këto zgjedhje të jashtme nuk duhet të trillohen në kod.

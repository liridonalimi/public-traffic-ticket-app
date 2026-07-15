# Paketa e Vendosjes në Prodhim

## Qëllimi

Moduli 13 e kthen shërbimin referues të Modulit 12 në një paketë vendosjeje të pavarur nga ofruesi. Ai shton runtime WSGI për prodhim, konfigurim të fortifikuar Container/Compose, sekrete nga skedari, volume të qëndrueshme, health checks, zgjedhjen e klientit gjatë ekzekutimit dhe mjete të validuara për backup/restore.

Ky modul nuk ndryshon infrastrukturë publike. Paketa është gati për platformën e zgjedhur dhe HTTPS të menaxhuar, por domain-i, identiteti, politika e bazës dhe llogaria e vendosjes duhet të sigurohen nga pronari i infrastrukturës.

## Çfarë u shtua

- Imazhi i fiksuar Python 3.12.13 slim.
- Gunicorn 26.0.0 i fiksuar.
- Ekzekutimi si përdorues jo-root `10001`.
- Filesystem read-only, heqja e capabilities dhe `no-new-privileges`.
- Port testues vetëm në localhost.
- Volume të qëndrueshme për bazën dhe backup-et.
- Sekreti bearer nga skedari, i përjashtuar nga Git dhe build context.
- Health checks në Container dhe Compose.
- Kufij të kontrolluar për worker, thread, timeout, kërkesa dhe logim.
- Backup/restore atomik SQLite me integrity check dhe SHA-256.
- Factory e konfigurimit për development dhe Gunicorn.
- Kufiri Android për zgjedhjen demo/prodhim të `TransitSyncClient`.
- Statusi **Deployment package: ready • infrastructure selection pending** në aplikacion.

## Testimi i dyfishtë me container

Kjo pjesë kërkon Docker.

1. Kopjoni `deployment/secrets/buspay_sync_token.txt.example` si `deployment/secrets/buspay_sync_token.txt`.
2. Vendosni një sekret lokal të gjatë. Mos e bëni commit skedarin.
3. Validoni konfigurimin:

```bash
docker compose -f deployment/compose.yaml config
```

4. Ndërtoni dhe nisni shërbimin:

```bash
docker compose -f deployment/compose.yaml up --build -d
```

5. Kontrolloni health:

```bash
docker compose -f deployment/compose.yaml ps
curl http://127.0.0.1:8080/health
```

6. Dërgoni `server/sample-sync.json` me sekretin lokal:

```bash
curl -i -X POST http://127.0.0.1:8080/v1/sync \
  -H 'Authorization: Bearer YOUR_LOCAL_SECRET' \
  -H 'Content-Type: application/json' \
  -H 'X-BusPay-Contract-Version: 1' \
  -H 'Idempotency-Key: sync-1111222233334444' \
  --data-binary @server/sample-sync.json
```

7. Ekzekutojeni dy herë dhe konfirmoni `false`, pastaj `true` për replay.
8. Rinisni shërbimin dhe konfirmoni se raporti ruhet:

```bash
docker compose -f deployment/compose.yaml restart
curl http://127.0.0.1:8080/v1/reports/admin \
  -H 'Authorization: Bearer YOUR_LOCAL_SECRET'
```

9. Krijoni backup:

```bash
docker compose -f deployment/compose.yaml exec buspay-sync \
  python deployment/backup_sqlite.py \
  --database /data/buspay.db \
  --output-directory /backups
```

10. Konfirmoni statusin e ri në aplikacion dhe funksionimin e sinkronizimit demo.

## Validimi pa container

Kur Docker nuk është i disponueshëm:

```bash
PYTHONPATH=server:. python3 -m unittest discover -s server/tests -v
```

Ky test validon sekretet, invariantet e container-it, ruajtjen, backup/restore, autentikimin, idempotencën dhe raportet.

## Lista para prodhimit

- Zgjidhni ofruesin, rajonin dhe domain-in.
- Përdorni HTTPS të besueshëm në ingress/load balancer.
- Mbajeni portin 8080 privat.
- Zëvendësoni sekretin e përbashkët me tokenë jetëshkurtër.
- Zgjidhni bazën e menaxhuar ose aprovoni formalisht kufijtë SQLite.
- Ruani sekretet në secret manager.
- Planifikoni backup-et dhe testoni restore në mjedis të veçantë.
- Konfiguroni loget, metrikat, alarmet dhe pronarin e incidenteve.
- Kryeni smoke tests para kalimit të Android në prodhim.
- Përdorni `SyncRuntimeConfig.production()` vetëm me HTTPS dhe token të autentikuar.
- Mbani imazhin dhe backup-in e mëparshëm për rollback.

## Kufizimet aktuale

- Testi i dyfishtë lokal në Docker kaloi në Docker Desktop 4.82.0: nisje e shëndetshme pa root, ingestim i autentikuar, replay idempotent, ruajtje pas restart-it dhe volum backup-i i shkrueshëm.
- Compose ekspozon HTTP vetëm në localhost; Android i prodhimit kërkon HTTPS të besueshëm.
- SQLite mbetet adapteri i paketuar. PostgreSQL kërkon vendim infrastrukture dhe migrim.
- DNS, certifikatat, identiteti, monitorimi dhe cloud kërkojnë pronësi/kredenciale të jashtme.
- Piloti Android nis në demo derisa identiteti të japë token jetëshkurtër.

## Faza e ardhshme

Zgjidhni infrastrukturën dhe identitetin, vendoseni paketën në staging pas HTTPS, testoni backup/rollback dhe kryeni sinkronizim real Android-server.

# Moduli 21 — Qasja në prodhim sipas roleve

## Rezultati

Moduli 21 zëvendëson kredencialin e përbashkët të pilotit lokal me tre kufij të pavarur:

- **Pajisja për sinkronizim** dërgon turnet e mbyllura dhe lexon katalogun e publikuar.
- **Lexuesi i raportit** hap raportimin administrativ pa të drejtë ndryshimi të katalogut.
- **Administratori i katalogut** lexon dhe publikon shoferët, autobusët, linjat, ndalesat dhe tarifat pa qasje në raportet financiare të sinkronizuara.

API-ja i tregon rolet e autentikuara përmes `GET /v1/access` dhe çdo leje kontrollohet në server. Fshehja e një butoni në web është vetëm paraqitje; siguria zbatohet nga API-ja.

## Validimi lokal

Krijoni skedarët sekretë të pajisjes, raportit, katalogut dhe auditimit të Modulit 22 duke përdorur skedarët `.example`, me vlera të ndryshme. Niseni shërbimin me:

```bash
docker compose -f deployment/compose.yaml up --build -d
docker compose -f deployment/compose.yaml ps
```

Hapni `http://127.0.0.1:8080/admin`. Kredenciali i raportit duhet të shfaqë vetëm raportimin. Kredenciali i administratorit duhet të shfaqë vetëm katalogun. Kredenciali i pajisjes nuk duhet ta hapë hapësirën administrative.

Në tablet konfiguroni `http://127.0.0.1:8080/v1/sync` me kredencialin e pajisjes pasi të aktivizoni `adb reverse tcp:8080 tcp:8080`. Rifreskimi i katalogut dhe sinkronizimi i turneve duhet të funksionojnë. Kredencialet administrative duhet të refuzohen në rrjedhën e tabletit.

## Kufiri i përfundimit

Moduli krijon një bazë me privilegj minimal, të pavarur nga ofruesi. Federimi i identitetit, menaxhimi i përdoruesve, rotacioni i kredencialeve, domain/TLS dhe përgjegjësia e auditimit mbeten vendime të vendosjes pasi të zgjidhen infrastruktura dhe ofruesi i identitetit.

Moduli 22 shton më pas rolin e katërt të auditorit të sigurisë dhe paketat e kufizuara me dy kredenciale. Përdorimi aktual i Compose kërkon edhe skedarin sekret të auditimit të dokumentuar në Modulin 22.

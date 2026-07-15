# Klienti i Autentikuar i Sinkronizimit HTTPS

## Qëllimi

Moduli 11 siguron bazën e transportit për dërgimin e turneve të mbyllura dhe biletave në një server real. Ai nuk zëvendëson të dhënat lokale dhe nuk vendos kredenciale të prodhimit brenda aplikacionit. Në vend të kësaj, shton një implementim prodhimi të `TransitSyncClient`, i cili mund të aktivizohet kur operatori të sigurojë adresën HTTPS dhe tokenin nga një sesion i ardhshëm i autentikuar.

Klienti demo mbetet aktiv në pilot, në mënyrë që sinkronizimi të testohet edhe pa një backend të vendosur.

## Çfarë u shtua

- Konfigurimi i adresës së prodhimit vetëm me HTTPS.
- Autentikimi me bearer token.
- Kontrata JSON version 1 për kërkesën dhe konfirmimin.
- Çelësi stabil i idempotencës nga ID-ja ekzistuese e paketës së sinkronizimit.
- Afatet kohore për lidhjen dhe leximin.
- Bllokimi i ridrejtimeve dhe kufizimi i madhësisë së përgjigjes.
- Trajtimi i sigurt i dështimeve të autentikimit, rrjetit, serverit, kufirit të kërkesave, madhësisë dhe konfliktit.
- Përputhja strikte e versionit të kontratës dhe ID-së së kërkesës.
- Refuzimi i konfirmimeve për regjistrime jashtë paketës së dërguar.
- Bllokimi i trafikut të pakriptuar në Android.
- Kartela e statusit që dallon testimin demo nga gatishmëria për prodhim.

## Kontrata e kërkesës

Klienti i prodhimit dërgon një kërkesë `POST` me:

- `Authorization: Bearer <access-token>`
- `Content-Type: application/json; charset=utf-8`
- `Accept: application/json`
- `Idempotency-Key: <stable-request-id>`
- `X-BusPay-Contract-Version: 1`

Trupi përmban versionin e kontratës, ID-në e kërkesës, kohën e dërgimit, turnet e mbyllura dhe biletat. Turni përmban shoferin, autobusin, linjën dhe kohën e fillimit/mbarimit. Bileta përmban turnin, tarifën, çmimin dhe kohën e shitjes.

## Rregulli i konfirmimit

Përgjigjja e serverit duhet të përmbajë:

- versionin e kontratës `1`
- të njëjtën ID të kërkesës
- ID-të e turneve të konfirmuara
- ID-të e biletave të konfirmuara

Pranohen vetëm ID-të që ekzistojnë në paketën e dërguar. Depoja offline vazhdon të shënojë si të sinkronizuara vetëm regjistrimet e konfirmuara. Përgjigjet e pavlefshme, të pjesshme, të refuzuara ose të paarritshme i lënë të dhënat e pakonfirmuara lokalisht për riprovim.

## Vendimet e sigurisë

- Adresat HTTP refuzohen para krijimit të kërkesës.
- Refuzohen URL-të që përmbajnë kredenciale ose fragment.
- Refuzohen tokenët bosh ose me karaktere që mund të injektojnë header-a.
- Tokeni vendoset vetëm në header-in e autorizimit, jo në JSON ose në mesazhet e gabimit.
- Ridrejtimet nuk ndiqen, që kredencialet të mos dërgohen në një host tjetër.
- Tokeni nuk ruhet nga ky modul dhe nuk vendoset në repository.

## Lista e testimit të dyfishtë

1. Instaloni dhe hapni debug build-in e ri.
2. Shkoni te **Total waiting for sync**.
3. Konfirmoni se kartela **Sync service** shfaq **Active mode: Demo validation**.
4. Konfirmoni se shfaq **Production HTTPS contract v1: ready**.
5. Konfirmoni njoftimin se për aktivizim duhen URL-ja e serverit dhe tokeni i autentikuar.
6. Mbyllni një turn me të paktën një biletë.
7. Shtypni **Go Offline**, pastaj **Sync Now**, dhe konfirmoni se dështimi i lë të dhënat në pritje.
8. Shtypni **Go Online**, pastaj **Sync Now**, dhe konfirmoni se sinkronizimi demo i largon regjistrimet nga pritja.
9. Hapni **Admin Report Preview** dhe konfirmoni statuset `SYNCED`/`synced`.
10. Konfirmoni se funksionet e shoferit, printerit, linjës, ekranit të pasagjerëve, kërkesës për ndalesë dhe raportimit vazhdojnë të punojnë.

## Kufizimet aktuale

- Ende nuk është dhënë URL-ja e serverit të prodhimit.
- Identiteti i prodhimit dhe lëshimi i tokenit nuk janë pjesë e pilotit me kyçje lokale.
- Klienti demo mbetet transporti aktiv deri sa të ekzistojnë këto të dhëna të vendosjes.
- Certificate pinning dhe rifreskimi i tokenit varen nga dizajni i identitetit dhe infrastrukturës.
- Baza e serverit dhe paneli web janë detyra të ardhshme të vendosjes.

## Faza e ardhshme

Vendosni endpoint-in e autentikuar dhe bazën e të dhënave, lëshoni tokenë jetëshkurtër përmes identitetit të prodhimit dhe krijoni `ProductionTransitSyncClient` me këto vlera gjatë ekzekutimit.

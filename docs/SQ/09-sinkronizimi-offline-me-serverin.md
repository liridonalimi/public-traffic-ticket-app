# Sinkronizimi Offline me Serverin

## Qëllimi

Moduli 09 shton rrjedhën offline-first për sinkronizimin e turneve të përfunduara dhe shitjeve të biletave. Të dhënat mbeten lokale derisa serveri t'i konfirmojë ID-të e tyre. Përfshihet një server demo përcaktues, në mënyrë që suksesi, dështimi dhe riprovimi të testohen pa endpoint ose kredenciale prodhimi.

## Çfarë mund të bëjë shoferi

- Ta shohë numrin e turneve të përfunduara dhe biletave që presin sinkronizim.
- Të vazhdojë shitjen e biletave pa internet.
- Ta përfundojë turnin dhe t'i ruajë shoferin, autobusin, linjën, kohën e fillimit dhe përfundimit për ngarkim.
- Ta vendosë serverin demo offline dhe të verifikojë dështimin e sigurt.
- Ta vendosë serverin demo online dhe t'i riprovojë të njëjtat të dhëna.
- Ta shohë numrin e saktë të turneve dhe biletave të konfirmuara.
- Ta rihapë aplikacionin dhe t'i ruajë turnet e biletat e pasinkronizuara.

## Sjellja offline-first

Biletat ruhen në momentin e shitjes, si në modulet paraprake. Moduli 09 e ruan gjithashtu secilin turn të përfunduar para pastrimit të gjendjes aktive. Secili turn dhe secila biletë ka ID stabile që shërben si çelës idempotence në server.

Biletat e turnit aktiv nuk ngarkohen, sepse turni ende nuk ka përfunduar. Turnet e mëparshme, biletat historike dhe biletat e krijuara nga versionet e vjetra mund të sinkronizohen gjatë një turni tjetër aktiv.

## Përditësimi vetëm pas konfirmimit

Tentimi i sinkronizimit krijon një ID të qëndrueshme të paketës nga ID-të e të dhënave të përfshira. Klienti i dërgon të dhënat në pritje dhe pranon grupe të ndara të ID-ve të konfirmuara për turnet dhe biletat.

Vetëm ID-të e konfirmuara shënohen si të sinkronizuara. Dështimi i lidhjes ose serverit, mungesa e konfirmimit ose një ID e panjohur nuk mund ta shënojë një të dhënë tjetër lokale. Të dhënat e pakonfirmuara mbeten gati për riprovim.

## Serveri demo

Konsola e shoferit përmban:

- **Go Offline / Go Online** për kontrollin e serverit demo.
- **Sync Now** për tentimin e sinkronizimit.

Serveri demo i konfirmon të gjitha të dhënat kur është online dhe nuk konfirmon asgjë kur është offline. Ai validon radhën lokale, mesazhet, riprovimin dhe trajtimin e konfirmimit. Nuk është server interneti dhe nuk pretendon lidhje prodhimi.

Adapteri demo nuk e ruan një kopje të të dhënave në server. Konfirmimi i tij do të thotë vetëm se rrjedha e klientit u pranua për testim.

## Lista për testimin e dyfishtë

1. Shënoni numrat fillestarë te **Total waiting for sync**.
2. Shtypni **Go Offline**, pastaj **Sync Now**.
3. Konfirmoni mesazhin e dështimit dhe që numrat nuk ndryshojnë.
4. Shtypni **Go Online**, pastaj **Sync Now**.
5. Konfirmoni suksesin dhe që numrat e gatshëm bëhen zero.
6. Fillojeni turnin, shiteni dhe printojeni me sukses të paktën një biletë.
7. Shtypni **Sync Now** dhe konfirmoni që bileta e turnit aktiv mbetet në pritje.
8. Përfundojeni turnin dhe konfirmoni që një turn dhe bileta e tij presin sinkronizim.
9. Mbylleni dhe rihapeni aplikacionin dhe konfirmoni rikthimin e numrave.
10. Testojeni përsëri dështimin offline, pastaj kaloni online dhe konfirmoni që riprovimi i pastron të dy numrat.
11. Shtypni përsëri **Sync Now** dhe konfirmoni mesazhin se nuk ka të dhëna në pritje.

## Kufizimet aktuale

- Serveri i përfshirë është adapter demo brenda aplikacionit.
- Ende nuk janë dhënë URL-ja e prodhimit, tokeni i autentikimit, politika TLS ose skema e përgjigjes HTTP.
- Sinkronizimi është manual; nuk ka planifikues në prapavijë ose riprovim automatik pas rikthimit të rrjetit.
- Aplikacioni ende nuk shfaq historik për secilën të dhënë ose kohën e serverit.
- Biletat historike nga modulet e vjetra mund të mos kenë turn të përfunduar përkatës, sepse ato versione nuk i ruanin turnet e mbyllura.

## Integrimi në prodhim

Klienti i prodhimit duhet ta implementojë të njëjtin kufi `TransitSyncClient`, t'i përdorë ID-të si çelësa idempotence, të autentikohet në mënyrë të sigurt dhe të kthejë grupe autoritative të ID-ve të konfirmuara. Depoja lokale dhe rrjedha e ndërfaqes nuk kanë nevojë të ndryshojnë.

Lidhja e planifikuar në server është: secili turn i përfunduar e ruan shoferin, autobusin, linjën dhe kohën e fillimit/përfundimit; secila biletë lidhet me ID-në e turnit dhe ruan tarifën, çmimin dhe kohën e shitjes. Kjo mundëson numrin e turneve për shofer dhe totalet e biletave për turn. Historiku GPS dhe historiku i kërkesave për ndalesë nuk janë pjesë e payload-it aktual.

## Moduli i ardhshëm

Moduli 10 do ta përcaktojë kontratën e të dhënave për raportimin në panelin administrativ.

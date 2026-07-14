# Integrimi i Butonit për Kërkesë Ndalese

## Qëllimi

Moduli 08 i mundëson pasagjerit ta kërkojë ndalesën e ardhshme dhe e mban kërkesën të sinkronizuar në konsolën e shoferit dhe ekranin e pasagjerëve. Kërkesa lidhet me turnin aktiv, rikthehet pas rihapjes së aplikacionit dhe pastrohet automatikisht kur autobusi arrin në ndalesën e kërkuar.

## Çfarë mund të bëjnë pasagjerët dhe shoferët

- Ta shtypin **Request Stop (Demo)** në ekranin e pasagjerëve.
- Ta përdorin **Press Stop Button (Demo)** në konsolën e shoferit për ta simuluar të njëjtën pajisje.
- Ta shohin mesazhin **STOP REQUESTED** dhe emrin e ndalesës në të dy ekranet.
- Ta shohin përsëri kërkesën pas mbylljes dhe rihapjes së aplikacionit gjatë turnit aktiv.
- Ta avancojnë linjën me GPS ose kontrollin demo dhe ta pastrojnë kërkesën automatikisht në mbërritje.
- Ta kërkojnë ndalesën vijuese pasi kërkesa paraprake të jetë pastruar.

## Sjellja e kërkesës

Shtypja e butonit gjithmonë e cakton ndalesën menjëherë pas ndalesës aktuale. Vetëm një kërkesë mund të jetë aktive, prandaj shtypjet e përsëritura nuk krijojnë kërkesa të dyfishta. Ruhen ID-ja e turnit, indeksi i ndalesës dhe koha e kërkesës.

Kur përparimi me GPS ose demo e arrin ose e kalon ndalesën e kërkuar, kërkesa pastrohet nga ruajtja lokale dhe nga të dy ekranet. Në ndalesën e fundit nuk mund të krijohet kërkesë, sepse nuk ka ndalesë të ardhshme.

## Sjellja offline

Kërkesa aktive ruhet lokalisht bashkë me turnin aktiv. Nuk kërkon internet dhe rikthehet vetëm kur ID-ja e saj përputhet me turnin e rikthyer. Përfundimi i turnit e pastron kërkesën.

## Kufiri i integrimit me pajisjen

Ky pilot përfshin një hyrje demo që i dërgon ngjarjet përmes të njëjtit kufi `StopRequestInput` të paraparë për pajisjen fizike. Kjo e validon gjendjen e aplikacionit dhe sinkronizimin e ekraneve pa pretenduar përputhshmëri me një buton konkret automjeti.

Instalimi në prodhim do të kërkojë adapter për transportin e zgjedhur, si GPIO përmes kontrolluesit të automjetit, Bluetooth, USB ose ndërfaqe të prodhuesit.

## Lista për testimin e dyfishtë

1. Fillojeni turnin dhe konfirmoni që nuk ka kërkesë aktive.
2. Hapeni ekranin e pasagjerëve dhe shtypni **Request Stop (Demo)**.
3. Konfirmoni që **STOP REQUESTED** e shfaq emrin e ndalesës së ardhshme.
4. Kthehuni në konsolën e shoferit dhe konfirmoni të njëjtën kërkesë dhe ndalesë.
5. Mbylleni dhe rihapeni aplikacionin gjatë turnit dhe konfirmoni rikthimin e kërkesës.
6. Shtypni **Advance Stop (Demo)** dhe konfirmoni që kërkesa pastrohet në ndalesën e kërkuar.
7. Krijoni kërkesë tjetër dhe konfirmoni që ajo e cakton ndalesën e re të ardhshme.
8. Avanconi në ndalesën e fundit dhe konfirmoni pastrimin e kërkesës dhe çaktivizimin ose fshehjen e butonave.
9. Përfundojeni turnin dhe konfirmoni që Last Passenger Display nuk ka kërkesë aktive.

## Kufizimet aktuale

- Butoni i përfshirë është demo softuerike dhe jo buton fizik i automjetit.
- Kërkesa gjithmonë vlen për ndalesën e menjëhershme të ardhshme; pasagjeri nuk mund të zgjedhë ndalesë tjetër.
- Ende nuk ka sinjal zanor, vibrim, dritë të butonit ose pranim nga shoferi.
- Historiku i kërkesave nuk ruhet pasi kërkesa të pastrohet.

## Moduli i ardhshëm

Moduli 09 do t'i sinkronizojë turnet dhe biletat me serverin duke e ruajtur funksionimin offline.

# Printimi i Biletave me Bluetooth dhe PDF

## Qëllimi

Moduli 06 e lidh secilën shitje të biletës me dalje të menjëhershme. Ai mbështet printerë të çiftuar Bluetooth Classic ESC/POS dhe përfshin printerin testues PDF për rastet kur pajisja fizike nuk është në dispozicion.

## Çfarë mund të bëjë shoferi

- Ta përdorë printerin testues PDF pa dhënë leje për Bluetooth.
- Ta lejojë Bluetooth-in dhe ta rifreskojë listën e printerëve të çiftuar.
- Ta zgjedhë printerin fizik ose printerin testues PDF.
- Ta shesë biletën dhe ta printojë automatikisht.
- Ta shohë nëse printimi pati sukses ose dështoi.
- Ta riprovojë printimin e fundit të dështuar ose në pritje pa regjistruar shitje tjetër.
- Ta hapë PDF-në më të fundit në një lexues PDF të instaluar.

## Sjellja e sigurt e shitjes dhe riprovimit

Bileta ruhet para fillimit të printimit. Gjendja e printimit ruhet si në pritje, e printuar ose e dështuar, së bashku me numrin e tentimeve dhe gabimin e fundit. Riprovimi e përdor të njëjtën biletë dhe nuk e rrit numrin e biletave ose totalin e arkës.

Shoferi nuk mund ta përfundojë turnin përderisa një biletë mbetet në pritje ose e dështuar. Kjo parandalon humbjen e problemit të printimit gjatë mbylljes së turnit.

## Të dhënat e biletës

Dalja Bluetooth dhe PDF përdorin të njëjtat të dhëna:

- kodin e biletës
- targën e autobusit
- linjën
- shoferin/operatorin
- llojin e tarifës dhe çmimin
- datën dhe orën e shitjes

PDF-ja përdor format të ngushtë rreth 58 mm me titull në shqip, rreshtin e tarifës, totalin, pagesën me para të gatshme, të dhënat e shitjes dhe kodin QR testues.

## Kufiri i rëndësishëm jofiskal

PDF-ja shënohet qëllimisht me **KUPON TESTUES - JO FISKAL**. Kodi QR përmban vetëm të dhëna lokale testuese dhe nuk është kod verifikimi fiskal i ATK-së. PDF-ja validon përmbajtjen dhe rrjedhën e aplikacionit; nuk paraqet aprovim ose pajtueshmëri fiskale.

Identifikuesit fiskalë, deklarimet tatimore, logot zyrtare dhe nënshkrimet digjitale duhet të shtohen vetëm përmes një integrimi të autorizuar të fiskalizimit.

## Përputhshmëria e printerit Bluetooth

Adapteri i përfshirë mbështet pajisje Bluetooth Classic që pranojnë komanda ESC/POS përmes profilit serial SPP. Pa adapter tjetër nuk mbështeten printerët ZPL, TSPL, CPCL, vetëm-BLE, USB ose protokollet pronësore.

Testimi fizik mbetet i domosdoshëm për secilin model printeri, sepse gjerësia e letrës, tabelat e karaktereve, prerësi, furnizimi i letrës, shpejtësia dhe besueshmëria ndryshojnë ndërmjet pajisjeve.

## Lejet në Android

Android 12 dhe versionet më të reja kërkojnë lejen për pajisjet në afërsi/Bluetooth para leximit të printerëve të çiftuar ose lidhjes. Versionet më të vjetra përdorin lejet tradicionale Bluetooth. Testimi PDF mbetet i disponueshëm kur leja Bluetooth refuzohet.

## Testimi dhe validimi

1. Refuzoni lejen Bluetooth, zgjidhni printerin testues PDF dhe shitni një biletë. Pritet shitje e suksesshme dhe PDF i lexueshëm.
2. Hapeni PDF-në dhe kontrolloni kodin, autobusin, linjën, shoferin, tarifën, shumën, kohën, QR-në dhe shenjën `JO FISKAL`.
3. Simuloni dështim printimi. Pritet që bileta të mbetet e ruajtur si e dështuar/në pritje dhe mbyllja e turnit të bllokohet.
4. Riprovoni printimin. Duhet të përdoret e njëjta ID bilete, të rritet numri i tentimeve dhe të mos rriten biletat ose arka.
5. Pas printimit të suksesshëm, mbylleni turnin dhe konfirmoni një shitje të vetme në total.
6. Me printer fizik, përsëriteni përmes Bluetooth Classic ESC/POS dhe kontrolloni çiftimin, rilidhjen, shkronjat shqipe, letrën dhe rikuperimin nga gabimi.

Pranimi kërkon ruajtje para printimit, riprovim pa dyfishim dhe ndarje të qartë nga certifikimi fiskal.

## Kufizimet aktuale

- PDF-ja testuese nuk mund ta validojë transportin Bluetooth ose sjelljen fizike të letrës.
- Bileta mbetet jofiskale deri në integrimin e një shërbimi të autorizuar fiskal.
- Vetëm PDF-ja më e fundit mund të hapet drejtpërdrejt nga konsola aktuale.
- Për printerë jashtë ESC/POS me Bluetooth Classic nevojiten implementime shtesë.

## Moduli i ardhshëm

Moduli 07 shtoi përparimin e linjës me GPS dhe ekranin e sinkronizuar për pasagjerë me ndalesën aktuale dhe të ardhshme.

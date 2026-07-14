# Themeli i Projektit dhe Skeleti Android

## Qëllimi

Moduli 01 vendosi themelin Android për pilotin BusPay të biletimit në transportin publik. Qëllimi ishte krijimi i një projekti funksional dhe të mirëmbajtshëm, mbi të cilin modulet e ardhshme mund të shtonin turnet e shoferit, shitjen offline të biletave, printimin, përcjelljen e linjës dhe informacionin për pasagjerët.

## Çfarë solli ky modul

- Aplikacion Android të shkruar në Kotlin.
- Bazën e ndërfaqes me Jetpack Compose.
- Konfigurimin Gradle, skriptet wrapper dhe modulin Android të aplikacionit.
- Manifestin Android, temën, resurset dhe aktivitetin fillestar.
- Ekranin fillestar të konsolës së shoferit.
- Modelet bazë për shoferët, autobusët, linjat, ndalesat, turnet dhe biletat.
- Ndarjen e paketave për ndërfaqen, domenin, të dhënat lokale dhe pajisjet.
- Ndërfaqet fillestare për GPS, printerin e biletave dhe butonin për kërkesë ndalese.
- Udhëzuesin për konfigurimin dhe nisjen e ambientit Android.

## Arkitektura e vendosur

Aplikacioni u nda në katër përgjegjësi kryesore:

- `ui`: ekranet Compose dhe pamjet për shoferin e pasagjerët.
- `domain`: modelet kryesore dhe rregullat e biznesit për transportin dhe biletat.
- `data`: ruajtja offline-first dhe sinkronizimi i ardhshëm.
- `device`: kufijtë e integrimit me Android dhe pajisjet e automjetit.

Kjo ndarje u mundëson moduleve të ardhshme t'i zëvendësojnë implementimet demo pa e rishkruar të gjithë aplikacionin. Për shembull, të dhënat lokale mund të zëvendësohen me të dhëna nga serveri dhe një adapter printeri mund të zëvendësohet me një tjetër.

## Vlera për biznesin

Themeli e zvogëloi rrezikun teknik para shtimit të proceseve të biznesit. Ai krijoi një projekt të instalueshëm Android, një fjalor të përbashkët të transportit dhe pika të qarta integrimi për pajisjet e tabletit dhe shërbimet e ardhshme backend.

## Statusi aktual

Ky themel vazhdon të jetë baza e aplikacionit. Modulet e mëvonshme i kanë plotësuar pjesët fillestare me turne dhe bileta të ruajtura, identitet të shoferit, tarifa të ndryshme, printim, përparim me GPS dhe ekran për pasagjerë.

## Kufizimet fillestare

Moduli 01 qëllimisht nuk ofroi një rrjedhë të plotë operative. Autentikimi, menaxhimi i turnit, ruajtja e përhershme lokale, komunikimi me printer, GPS-i aktiv, sinkronizimi me server dhe raportimi u planifikuan për modulet e ardhshme.

## Moduli i ardhshëm

Moduli 02 shtoi rrjedhën e parë të plotë të turnit me zgjedhjen e autobusit dhe linjës, totalet e biletave dhe përfundimin e turnit.

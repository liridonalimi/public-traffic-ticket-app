# Kontrata e Raportimit Administrativ

## Qëllimi

Moduli 10 përcakton një kontratë të versionuar për panelin e ardhshëm administrativ dhe ofron një pamje paraprake brenda aplikacionit, të ndërtuar nga turnet lokale të përfunduara dhe biletat e lidhura. Të njëjtat të dhëna të detajuara përdoren për totalet e turneve, shoferëve, tarifave, arkës dhe sinkronizimit, në mënyrë që përmbledhjet të mund të barazohen me biletat individuale.

## Çfarë shfaq pamja administrative

- Numrin e shoferëve në raport.
- Numrin e turneve të përfunduara.
- Numrin e biletave dhe totalin e arkës.
- Totalet sipas tarifës standarde, studentore, për të moshuar dhe fëmijë.
- Numrin e turneve të sinkronizuara, pjesërisht të sinkronizuara dhe në pritje.
- Turnet, biletat dhe arkën për secilin shofer.
- Secilin turn me shoferin, autobusin, linjën, fillimin, përfundimin dhe kohëzgjatjen.
- Biletat e secilit turn me tarifën, çmimin dhe gjendjen e sinkronizimit.
- Paralajmërimet për bileta të vjetra pa turn të ruajtur dhe referenca të panjohura.

Pamja hapet me **Open Admin Report Preview** në konsolën e shoferit. **Refresh Report** e rindërton raportin nga ruajtja aktuale lokale.

## Lidhjet e raportimit

Kontrata përdor këto lidhje kryesore:

- Një shofer mund të ketë shumë turne të përfunduara.
- Një turn i përket një shoferi, autobusi dhe linje.
- Një turn mund të përmbajë shumë bileta.
- Një biletë i përket një turni dhe një lloji tarife.

Këto lidhje i mundësojnë administratorit të fillojë nga totalet, të kontrollojë një shofer, ta hapë turnin dhe ta barazojë me biletat e tij.

## Gjendja e sinkronizimit të turnit

- **SYNCED:** turni dhe të gjitha biletat e zgjedhura janë konfirmuar.
- **PARTIALLY SYNCED:** turni ose së paku një biletë është konfirmuar, por jo i gjithë grupi.
- **PENDING:** as turni dhe as biletat e zgjedhura nuk janë konfirmuar.

Gjendja llogaritet nga të dhënat operative dhe nuk kopjohet si vlerë e veçantë, duke parandaluar dallimet ndërmjet raportit dhe burimit.

## Filtrat e përcaktuar

Modeli mbështet:

- intervalin përfshirës të datës/orës së fillimit të turnit
- ID-në e shoferit
- ID-në e autobusit
- ID-në e linjës
- ID-në e tarifës

Filtri i tarifës ruan vetëm biletat përkatëse dhe i largon turnet pa bileta të tilla. Pamja pilot i shfaq të gjitha turnet lokale; kontrollet e panelit të prodhimit do t'i plotësojnë filtrat.

## Rregullat e cilësisë së të dhënave

Biletat nga versionet para Modulit 09 mund të mos kenë turn të ruajtur. Ato përjashtohen nga totalet e atribuara për shofer/turn dhe shfaqen veçmas si numër dhe vlerë e biletave të palidhura. Kjo parandalon atribuimin e arkës te shoferi i gabuar.

Referencat e panjohura të shoferit, autobusit ose linjës mbeten të dukshme me emërtime rezervë dhe numërues të veçantë.

## Lista për testimin e dyfishtë

1. Përfundoni së paku dy turne, mundësisht me shoferë dhe tarifa të ndryshme.
2. Lëreni një turn në pritje dhe sinkronizojeni tjetrin me rrjedhën demo të Modulit 09.
3. Shtypni **Open Admin Report Preview**.
4. Konfirmoni që totalet e turneve, biletave dhe arkës përputhen me turnet e ruajtura.
5. Konfirmoni që totalet e tarifave përputhen me turnet individuale.
6. Konfirmoni turnet, biletat dhe arkën e secilit shofer.
7. Kontrolloni shoferin, autobusin, linjën, kohën, kohëzgjatjen dhe sinkronizimin e secilit turn.
8. Konfirmoni që biletat nën secilin turn përputhen me numrin dhe arkën e turnit.
9. Nëse ka bileta të vjetra, konfirmoni paraqitjen te **Data quality** dhe përjashtimin nga totalet e atribuuara.
10. Kthehuni në konsole, krijoni dhe përfundoni një turn tjetër, rihapeni pamjen dhe shtypni **Refresh Report**.
11. Konfirmoni që turni i ri shfaqet pa ndryshuar detajet e turneve të mëparshme.

## Kufizimet aktuale

- Pamja lexon ruajtjen lokale Android dhe jo API prodhimi.
- Piloti nuk ka autentikim ose role administrative.
- Filtrat janë në kontratë, por ende nuk kanë kontrolle në ndërfaqe.
- Ende nuk ka panel web, eksport, faqezim, grafikë ose bazë të dhënash në server.
- Vetëm turnet e ruajtura nga Moduli 09 mund të atribuohen plotësisht.

## Faza e ardhshme e produktit

Lidheni adapterin e prodhimit dhe endpoint-in e raportimit me një panel administrativ të autentikuar që përdor versionin 1 të kontratës.

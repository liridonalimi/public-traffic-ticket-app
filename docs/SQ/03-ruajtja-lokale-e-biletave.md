# Ruajtja Lokale e Biletave

## Qellimi

Ruajtja lokale e biletave i mundeson shoferit te vazhdoje punen edhe nese tableti rindizet ose rrjeti nuk eshte i disponueshem. Turni aktiv dhe biletat e shitura ruhen brenda hapesires lokale te aplikacionit Android.

## Cfare mund te beje shoferi tani

- Te filloje nje turn dhe ta mbaje te ruajtur lokalisht.
- Te shese bileta me para te gatshme dhe t'i mbaje te ruajtura lokalisht.
- Te rihape aplikacionin dhe te kthehet ne turnin aktiv.
- Te shoh biletat e turnit aktual ndaras nga totali i biletave qe presin sinkronizimin e ardhshem.
- Te mbylle turnin duke i mbajtur biletat e shitura te ruajtura per dergim te mevonshme ne server.

## Vlera per biznesin

Ky modul mbron te dhenat e biletave dhe te arkes gjate operimit ditor te autobusit. Mbyllja e aplikacionit ose nderprerja e sesionit ne tablet nuk duhet te fshije turnin aktiv ose biletat e shitura. Moduli gjithashtu pergatit projektin per hapin tjeter offline-first: sinkronizimin e biletave te paderguara me serverin qendror.

## Statusi aktual i implementimit

Aplikacioni perdor ruajtjen lokale te Android permes depozites offline-first. Turni aktiv ruhet ndaras nga lista e biletave. Ekrani i shoferit shfaq totalet e biletave te turnit aktual ndaras nga radha totale lokale e biletave te pasinkronizuara. Biletat mbeten te shenuara si te pasinkronizuara qe nje modul i ardhshem t'i dergoje ne backend.

## Testimi dhe validimi

1. Filloni një turn dhe shitni dy bileta.
2. Mbylleni plotësisht aplikacionin nga lista e aplikacioneve dhe hapeni përsëri.
3. Konfirmoni që rikthehen i njëjti turn, numri i biletave dhe totali i arkës.
4. Shitni edhe një biletë dhe kontrolloni që totali rritet vetëm për atë shitje.
5. Përfundojeni turnin, rindizeni aplikacionin dhe konfirmoni që turni nuk është aktiv, ndërsa tri biletat mbeten në radhën lokale.
6. Përsëriteni pa rrjet. Pritet sjellje e njëjtë sepse ruajtja nuk varet nga serveri.

Pranimi kërkon që restarti të mos humbë ose dyfishojë turnin dhe biletat.

## Permiresimet e planifikuara

- Kalimi ne Room kur modeli i te dhenave te behet me i madh.
- Sinkronizimi me serverin me mekanizem riprovimi.
- Shenimi i biletave si te sinkronizuara pas dergimit te suksesshem.
- Rikuperim dhe validim me i forte per te dhena lokale te demtuara.

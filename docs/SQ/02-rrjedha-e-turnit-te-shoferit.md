# Rrjedha e Turnit te Shoferit

## Qellimi

Rrjedha e turnit i mundeson shoferit ta pergatise autobusin per pune para shitjes se biletave. Shoferi zgjedh autobusin, zgjedh linjen, fillon turnin, shet bileta dhe e mbyll turnin me nje permbledhje te qarte te biletave dhe parave te gatshme.

## Cfare mund te beje shoferi tani

- Te zgjedhe nje nga autobuset demo.
- Te zgjedhe nje nga linjat demo.
- Te filloje turnin per autobusin dhe linjen e zgjedhur.
- Te shese bileta standarde me para te gatshme gjate turnit aktiv.
- Te shoh numrin e biletave te shitura.
- Te shoh totalin aktual te parave.
- Te mbylle turnin dhe te shoh permbledhjen e turnit te fundit.

## Vlera per biznesin

Ky modul eshte baza per pergjegjesine e shoferit. Cdo shitje bilete lidhet me nje turn aktiv, autobus, linje dhe shofer. Kjo e ben me te lehte kontrollin ditor te parave dhe e pergatit sistemin per raportim ne panelin administrativ.

## Statusi aktual i implementimit

Kjo eshte nje rrjedhe pilot brenda aplikacionit. Lista e autobuseve, lista e linjave, cmimi i biletes dhe shoferi jane te dhena demo brenda aplikacionit Android. Shitjet numerohen gjate sesionit te aplikacionit, por ende nuk ruhen pergjithmone pas mbylljes se aplikacionit.

## Testimi dhe validimi

1. Niseni aplikacionin dhe zgjidhni një autobus dhe linjë demo.
2. Fillojeni turnin dhe konfirmoni që autobusi dhe linja nuk mund të ndryshohen gjatë turnit aktiv.
3. Shitni tri bileta standarde. Pritet numri `3` dhe arka EUR 1.50.
4. Përfundojeni turnin dhe kontrolloni që përmbledhja ruan autobusin, linjën, tri biletat dhe EUR 1.50.
5. Provoni të shitni pa turn aktiv. Pritet që të mos krijohet biletë dhe arka të mos ndryshojë.

Pranimi kërkon që secila shitje t'i përkasë një turni aktiv dhe totalet të dalin nga biletat e tij.

## Permiresimet e planifikuara

- Ruajtja lokale e biletave ne tablet.
- Sinkronizimi i turneve dhe biletave me serverin qendror.
- Kyqja e shoferit.
- Printimi i faturave/biletave.
- Lloje te ndryshme tarifash dhe zbritjesh.

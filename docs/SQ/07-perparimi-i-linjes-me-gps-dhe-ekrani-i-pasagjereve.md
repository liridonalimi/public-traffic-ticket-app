# Perparimi i Linjes me GPS dhe Ekrani i Pasagjereve

## Qellimi

Ky modul e lidh turnin aktiv te shoferit me renditjen e ndalesave te linjes se zgjedhur. Aplikacioni mund ta perditesoje perparimin nga lokacioni i pajisjes Android dhe e shfaq te njejten ndalese aktuale dhe te ardhshme ne konsolen e shoferit dhe ne ekranin e vecante per pasagjere.

## Cfare mund te beje shoferi tani

- Ta filloje turnin ne ndalesen e pare te linjes se zgjedhur.
- Ta lejoje qasjen ne lokacion dhe ta perdore GPS-in per perditesim automatik.
- Ta shohe ndalesen aktuale dhe ndalesen e ardhshme ne konsolen e shoferit.
- Ta perdore **Advance Stop (Demo)** per ta testuar linjen pa levizur pajisjen.
- Ta hape ekranin e madh per pasagjere gjate turnit aktiv.
- Te arrije ne ndalesen e fundit dhe ta shohe linjen si te perfunduar.
- Ta rihape ekranin e fundit te pasagjereve pas perfundimit te turnit.
- Ta rihape aplikacionin dhe ta riktheje ndalesen e fundit te ruajtur per turnin aktiv.

## Ekrani i pasagjereve

Ekrani i pasagjereve e shfaq linjen e zgjedhur, ndalesen aktuale, ndalesen e ardhshme dhe numrin e ndaleses. Ai e perdor te njejten gjendje si konsola e shoferit, prandaj nje perditesim nga GPS-i ose avancimi demo shfaqet menjehere. Pas perfundimit te turnit, konsola mban ne memorie nje kopje te linjes dhe perparimit final, qe shoferi ta rihape ekranin e fundit. Pamja e ruajtur shenohet me **SHIFT ENDED** dhe nuk mund ta ndryshoje perparimin.

## Sjellja e GPS-it

Perparimi i linjes leviz vetem perpara dhe avancon kur autobusi gjendet brenda 150 metrave nga nje ndalese e linjes. Nje lexim i mevonshem i GPS-it nuk mund ta ktheje autobusin ne nje ndalese me te hershme dhe nje lokacion larg linjes nuk mund te kaperceje ndalesa. Nese leja e lokacionit ose sherbimi i lokacionit nuk eshte i disponueshem, shoferi mund ta validoje dhe perdore rrjedhen demo manualisht.

## Lista per testimin e dyte

1. Kyquni, zgjidhni autobusin dhe linjen, pastaj filloni turnin.
2. Vertetoni qe ndalesa e pare shfaqet si aktuale dhe e dyta si e ardhshme.
3. Refuzoni ose anashkaloni lejen e lokacionit dhe vertetoni qe **Advance Stop (Demo)** vazhdon te punoje.
4. Hapeni ekranin e pasagjereve dhe vertetoni qe perputhet me konsolen e shoferit.
5. Kthehuni ne konsole, avanconi nje ndalese dhe rihapeni ekranin e pasagjereve.
6. Avanconi deri ne ndalesen e fundit dhe vertetoni mesazhin **Route complete** / **Final destination**.
7. Mbylleni dhe rihapeni aplikacionin gjate turnit aktiv dhe vertetoni rikthimin e ndaleses se ruajtur.
8. Ne pajisje me GPS, lejojeni lokacionin dhe vertetoni qe gjurmimi GPS aktivizohet.
9. Perfundoni turnin, shtypni **Open Last Passenger Display** dhe vertetoni qe gjendja finale mbetet e dukshme me **SHIFT ENDED**.

## Kufizimet aktuale

- Linjat dhe koordinatat e ndalesave jane te dhena lokale demo.
- Perparimi perdor ndalesen me te afert ne ose pas pozites aktuale brenda nje rrezeje fikse prej 150 metrash; ende nuk perdor gjeometrine e linjes, drejtimin, shpejtesine ose saktesine e GPS-it.
- Ky pilot e perdor nje ekran te aplikacionit per pamjen e pasagjereve. Nje automjet prodhimi mund te perdore ekran te dyte fizik ose aplikacion shoqerues.
- Kopja e turnit te fundit ruhet vetem derisa procesi i aplikacionit mbetet aktiv; nuk rikthehet pas rihapjes se plote te aplikacionit.
- Njoftimet zanore dhe butoni per kerkese ndalese ende nuk perfshihen.

## Moduli i ardhshem

Moduli 08 do ta integroje butonin per kerkese ndalese dhe do ta shfaqe kerkesen te shoferi dhe ne ekranin e pasagjereve.

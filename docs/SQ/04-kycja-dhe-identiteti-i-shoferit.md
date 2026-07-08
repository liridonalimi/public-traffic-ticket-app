# Kycja dhe Identiteti i Shoferit

## Qellimi

Kycja dhe identiteti i shoferit e zevendeson shoferin fiks demo me nje rrjedhe lokale te kycjes. Shoferi zgjedh identitetin e tij para fillimit te turnit dhe cdo turn i ri lidhet me ate ID te shoferit.

## Cfare mund te beje shoferi tani

- Te zgjedhe nje shofer nga lista lokale pilot.
- Te kyqet para fillimit te turnit.
- Te shoh emrin dhe ID-ne e shoferit te kyqur ne konsolen e shoferit.
- Te ckyqet kur nuk ka turn aktiv.
- Te rihape aplikacionin dhe ta mbaje shoferin e kyqur te rikthyer.
- Te riktheje nje turn aktiv me identitetin e sakte te shoferit.

## Vlera per biznesin

Ky modul e permireson pergjegjesine e shoferit. Shitjet e biletave dhe totalet e arkes tani lidhen me nje ID konkrete te shoferit, qe e pergatit sistemin per kontroll te arkes, raporte auditimi dhe autentikim me backend ne te ardhmen.

## Statusi aktual i implementimit

Aplikacioni perdor nje liste lokale demo te shoferave brenda aplikacionit Android. Shoferi i kyqur ruhet lokalisht permes depozites offline-first. Fillimi i turnit bllokohet derisa nje shofer te jete i kyqur, ndersa ckycja bllokohet gjate nje turni aktiv.

## Permiresimet e planifikuara

- Zevendesimi i listes lokale te shoferave me autentikim nga backend.
- Shtimi i kycjes me PIN, fjalekalim ose kartele.
- Shtimi i roleve per shofer, inspektor dhe administrator.
- Regjistrimi i ngjarjeve te kycjes dhe ckycjes per auditim.

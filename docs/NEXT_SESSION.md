Pokračujeme na Intelia Launcheru — **P1 Onboarding V2: ověření na telefonu (Etapa E)** (`AGENTS.md` platí; česky, stručně,
~200k tokenů, žádní agenti). Větev `claude/onboarding-v2-final` (PR do `main`, nemergovat bez majitele). Zdroj pravdy: `docs/product/ONBOARDING_V2.md` §11
(požadavky) a **§12–§13 (co je postavené)**, `docs/STATUS.md` (ověřeno / neověřeno).

## Stav (2026-10-19)
- Onboarding V2 + finální revize + Discovery Hub v kódu, 258 testů PASS; celá cesta ověřena na telefonu, 6 nálezů
  opraveno (`STATUS.md`). Lab majitele běží z této větve; regrese PULSE po zamknutí/odemknutí a Global ARC PASS.

## Další kroky
1. Majitel projde PR #7 a screenshoty; merge jen s jeho souhlasem.
2. Neověřeno: TalkBack (Porovnat, PULSE, čipy), Reduced Motion, světlá tapeta, malý displej, udělení / zamítnutí
   oprávnění (jen na testovacím telefonu nebo profilu), PULSE v průvodci bez běžící služby Labu.
3. Drobnosti: uvítací panel zakrývá datum na signature ploše; Porovnat se ukáže i u vizuálně shodného stavu
   (rozvržení přepsané na stejné); pilulka „Porovnat“ u velmi malého náhledu.

Pozor: před každým `adb input` ověřit, že je v popředí `dev.intelia.launcher.test`; swipe dolů z kamery stáhne
oznamovací lištu (soukromá oznámení) — po testu Global ARC `adb shell cmd statusbar collapse`, snímky smazat.

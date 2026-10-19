Pokračujeme na Intelia Launcheru podle `docs/product/MASTER_PLAN.md` (V2) (`AGENTS.md` platí; česky, stručně,
~200k tokenů, žádní agenti). `main` obsahuje Onboarding V2 a vydání **v0.1.0** (první stabilní, beta stažena).

## Stav (2026-10-19)
- 260 testů PASS. „Porovnat“ jen při vizuálním rozdílu a vedle náhledu (datum už nezakrývá) — ověřeno na telefonu.
- Otevřené (neřešeno, nevydávat za hotové): Global ARC × oznamovací lišta (`PULSE_RELIABILITY.md` §9); klepnutí na PULSE v průvodci (Lab služba drží kameru).
- Neověřeno: světlý motiv/tapeta, zvětšené písmo na tomto buildu, malý displej, Reduced Motion, TalkBack,
  udělení/zamítnutí oprávnění.

## Další kroky
1. Priorita 1: dlouhodobá spolehlivost a ergonomie globálního PULSE (vč. §9).
2. Action Engine → sjednocení ARC/PULSE/FLOW/hardwarových gest → personalizace Home a Studio.

Pozor: před každým `adb input` ověřit popředí (`.test/`); majitel telefon používá souběžně. Po testu Global ARC
`adb shell cmd statusbar collapse`, snímky smazat.

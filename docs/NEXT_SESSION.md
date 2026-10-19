Pokračujeme na Intelia Launcheru — **P1 Onboarding V2: ověření na telefonu (Etapa E)** (`AGENTS.md` platí; česky, stručně,
~200k tokenů, žádní agenti). Větev `claude/onboarding-v2-final` (PR do `main`, nemergovat bez majitele). Zdroj pravdy: `docs/product/ONBOARDING_V2.md` §11
(požadavky) a **§12–§13 (co je postavené)**, `docs/STATUS.md` (ověřeno / neověřeno).

## Stav (2026-10-19)
- Onboarding V2 včetně finální UX revize (§13) a Discovery Hubu je v kódu; 258 testů PASS, `assembleDebug` PASS.
- Intelia Debug (`dev.intelia.launcher.test`) je na telefonu nainstalovaná s tímto buildem. Na zařízení **nic
  neověřeno** — telefon byl zamčený.

## Další kroky (Etapa E — jen na odemčeném telefonu, majitel u něj nepracuje)
1. Celá cesta v Intelia Debug po `pm clear dev.intelia.launcher.test` (nikdy ne Lab): Vítej → Plocha (Prázdná /
   Základní / Vlastní, Změnit aplikace, Upravit rozložení viditelné nad tlačítkem) → Vzhled (záložky v jednom řádku,
   barva, hodiny) → Porovnat (podržet / pustit) → Vyzkoušej (LENS, ARC, FLOW; PULSE jen bez běžící služby Labu) →
   Oprávnění (řádky jen číst, na Výchozí plocha / Služba PULSE neklepat) → Hotovo → Přejít na plochu a Prozkoumat.
2. Process death uprostřed (`install -r`), opakovaný průvodce ze Studia, Discovery Hub stavy.
3. Velké písmo 130 %, klávesnice v hledání aplikací, světlá tapeta, TalkBack na Porovnat a PULSE.
4. Screenshoty (smazat po prohlédnutí), regrese PULSE v Labu po zamknutí/odemknutí a Global ARC
   (`dumpsys activity service dev.intelia.launcher.lab/dev.intelia.launcher.ui.PulseAccessibilityService`).
5. `STATUS.md` jen ověřenými fakty; při chybě opravit dřív než merge.

Pozor: před každým `adb input` ověřit, že je v popředí `dev.intelia.launcher.test` (majitel telefon používá).

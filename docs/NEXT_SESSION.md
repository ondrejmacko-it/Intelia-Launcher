Pokračujeme na Intelia Launcheru — **P1 Onboarding V2: dotažení a ověření** (`AGENTS.md` platí; česky, stručně,
~200k tokenů, žádní agenti). Větev `claude/pulse-reliability`. Zdroj pravdy: `docs/product/ONBOARDING_V2.md` §11
(požadavky) a **§12 (co je postavené)**, `docs/STATUS.md` (ověřeno / neověřeno).

## Stav (2026-10-19)
- Onboarding V2 je v kódu: `onboarding/OnboardingDraft.kt`, `ui/OnboardingHost.kt`; 254 testů PASS.
- Vývoj se testuje v **Intelia Debug** (`dev.intelia.launcher.test`, D30); Lab majitele je optimalizovaný
  `assembleLab`. `pm clear` jen na `dev.intelia.launcher.test`.
- Commity nejsou pushnuté; majitel pushne sám, merge do `main` jen s jeho souhlasem.

## Další kroky
1. Discovery Hub ve Studiu (Studio › Prozkoumat, stavy Hotovo / Nastavit / Vyzkoušet / Připravujeme), „Prozkoumat
   Intelii“ v Hotovo ho otevře.
2. Ověřit na telefonu: Vlastní → Home Editor v draftu, FLOW záložka, TalkBack, velké písmo (130 %), Reduced Motion,
   světlá tapeta. PULSE v průvodci jde ověřit jen bez běžící PULSE služby Labu (služba Labu vlastní kameru).
3. Vizuál podle majitele (velikost náhledu, typografie panelu), pak `STATUS.md` jen ověřenými fakty.

Pozor: na telefonu nikdy neklepat na řádky Výchozí plocha / Služba PULSE (mění telefon majitele) a před každým
`adb input` ověřit, že je v popředí Intelia Debug (majitel telefon používá).

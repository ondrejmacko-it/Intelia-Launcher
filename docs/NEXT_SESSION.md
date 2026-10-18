Pokračujeme na Intelia Launcheru — **R1 Studio V5, krok 1** (`AGENTS.md` platí;
česky, stručně, ~200k tokenů, žádní agenti).

## Stav (2026-10-19)
- **Vydáno:** GitHub Pre-release `v0.1.0-beta.1` s podepsaným `intelia-0.1.0-beta.1.apk` (SHA-256 `3dd122a7…dff7`,
  certifikát `5ddb08b2…dd99d3`). Na GitHubu je jen `main`, tag a Releases; zálohy větví lokálně
  (`../git-backup-2026-10-19/`, větve `backup/*`).
- Release klíč jen v `.signing/` (majitel + offline zálohy). Debug klíč už není v repu (D27), Lab se podepisuje stejně.
- Dokumentace: `docs/product/MASTER_PLAN.md`, `docs/research/COMPETITOR_RESEARCH.md`, `docs/audit/*`,
  `docs/release/v0.1.0-beta.1/*`, README EN/CZ, NOTICE, TRADEMARKS, SECURITY, CREDITS, DECISIONS D26–D27.
- Otevřená rozhodnutí majitele: přepis historie (e-mail autora, starý debug klíč) = force push, nedoporučeno;
  ochrana větve `main` a secret scanning v nastavení GitHubu; případná změna licence budoucích verzí.
- Neověřeno: podepsaný release APK na telefonu, lock/unlock, Recents → Home, PULSE nad aplikacemi, Global ARC,
  TalkBack, světlý motiv, anglické UI na telefonu.

## Úkol — R1 Studio V5, krok 1 (nová větev `claude/studio-v5` z `main`)
0. Ověř podepsanou betu: stáhni APK z Releases, porovnej SHA-256 (instalovat na telefon jen se souhlasem majitele).
1. U1: zjisti, proč Studio ukazuje „Služba PULSE: Vypnuto“, když je služba bound — `PulseAccessibilityService.running`
   po `install -r` / po `uiautomator dump`. Ověř bez dumpu (screenshot), oprav jen skutečnou chybu, test.
2. U2: přejmenuj skupinu „Data a systém“ na stránce Systém a data (např. „Data“), EN i CS.
3. U3: řádek Jazyk do `StudioIndex` (+ synonyma „language“, „čeština“, „english“); test, že každý `SettingRow` na
   stránkách má záznam v indexu (pokud jde napsat čistě).
4. Návrh `PreviewKind` pro Drawer, Fonts, Icons, Haptics do `DECISIONS.md` (D27, PROPOSED) — bez kódu.
5. Anglické UI na telefonu: přepni jazyk Intelie přes Studio › Jazyk na English, projdi hlavní stránky (dump), vrať
   **Čeština**. Zapiš přetečení textů do UX_BUGS.

Soubory: `ui/Studio.kt` (`SystemPage`), `ui/StudioIndex.kt`, `ui/PulseAccessibilityService.kt`, `res/values*/strings.xml`,
testy `StudioIndexTest`, `StudioNavTest`.

## Build a telefon
JAVA_HOME `C:/Users/Ondra/AppData/Local/lesnik-toolchain/jdk-17.0.20.1+1`; adb
`C:/Users/Ondra/AppData/Local/lesnik-toolchain/sdk/platform-tools/adb.exe`, `MSYS_NO_PATHCONV=1`.
APK `app/build/outputs/apk/lab/app-lab.apk` (ověř čas), `install -r`, **NIKDY uninstall**, pak profileinstaller broadcast +
`pm compile -m speed-profile -f dev.intelia.launcher.lab`. Před každým vstupem ověř top activity `dev.intelia.launcher.lab/`;
jinak žádné dotyky ani screenshoty. Studio: Home dlouhý stisk 540 1100 → Nastavení 907 2240; scroll jen u x ≈ 20.
Majitelovy vazby, FLOW, ikony, akcent, haptiku, rozložení a hudbu neměnit.

## Git
Jedna větev na úkol, žádný push do `main`, force push ani přepis historie. Nesledované `docs/screens/*.rar`,
`s3/`, `s4.2/`, `s4.3/`, `hs_err_*` necommitovat (soukromé / artefakty). Debug klíč nikdy pro distribuci; `.signing/` nikdy nečíst do výstupu ani necommitovat. Po merge smaž pracovní větev na GitHubu (na GitHubu má být jen `main`).

## Hotovo když
Testy PASS, Lab nainstalovaný a ověřený na telefonu, STATUS jen s ověřenými fakty, commit + push na pracovní větev,
nový NEXT_SESSION.

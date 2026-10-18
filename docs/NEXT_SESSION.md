Pokračujeme na Intelia Launcheru — **R0 dokončení (podpis + vydání bety) a R1 Studio V5, krok 1** (`AGENTS.md` platí;
česky, stručně, ~200k tokenů, žádní agenti).

## Stav (2026-10-19)
- Verze `0.1.0-beta.1` (versionCode 2) je v `main` přes PR z `feat/control-system-v1` (ověř `git log -3 origin/main`).
  Tag `v0.1.0-beta.1` **neexistuje**; GitHub Release je jen **draft** bez APK — blokuje ho chybějící produkční klíč.
- Build PASS: `:app:testDebugUnitTest :app:assembleLab :app:assembleRelease` (235 testů). Lab `0.1.0-beta.1-lab`
  nainstalovaný na Phone (3) (`install -r`, HOME role i PULSE služba zachované). Release APK je `unsigned`.
- Dokumentace: `docs/product/MASTER_PLAN.md` (roadmapa R0–R12), `docs/audit/BETA_0.1.0_BASELINE.md` (inventura),
  `docs/audit/UX_BUGS.md` (U1–U7), `docs/release/v0.1.0-beta.1/` (notes + checklist), README EN/CZ, `DECISIONS.md` D26.
- Neověřeno: lock/unlock, Recents → Home, PULSE nad aplikacemi, Global ARC, restart procesu, TalkBack, světlý motiv,
  anglické UI na telefonu. Online rešerše konkurence neproběhla (MASTER_PLAN §13 → R0.5).

## Úkol 1 — R0: podpis a vydání (jen pokud majitel klíč založil / schválil)
1. Majitel vytvoří klíč podle `docs/release/v0.1.0-beta.1/RELEASE_CHECKLIST.md` (keytool, heslo jen interaktivně,
   2 offline zálohy) a `.signing/release.properties`. Agent heslo nikdy nevidí v příkazové řádce ani v gitu.
2. `./gradlew -q --max-workers=1 -Dorg.gradle.jvmargs=-Xmx1536m :app:testDebugUnitTest :app:assembleRelease`;
   `apksigner verify --print-certs` (DN NESMÍ být `CN=Intelia Debug`); přejmenuj na `intelia-0.1.0-beta.1.apk`; SHA-256.
3. Doplň SHA-256 do RELEASE_NOTES (PR do main), pak tag `v0.1.0-beta.1` na přesném commitu v `main`, publikuj draft jako
   **Pre-release** s APK; ověř stažení a hash.
4. Bez klíče: nic nepublikuj, pokračuj úkolem 2.

## Úkol 2 — R1 Studio V5, krok 1 (nová větev `claude/studio-v5` z `main`)
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
`s3/`, `s4.2/`, `s4.3/`, `hs_err_*` necommitovat (soukromé / artefakty). Debug klíč nikdy pro distribuci.

## Hotovo když
Testy PASS, Lab nainstalovaný a ověřený na telefonu, STATUS jen s ověřenými fakty, commit + push na pracovní větev,
nový NEXT_SESSION.

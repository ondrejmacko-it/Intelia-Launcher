# Prompt pro další session — Intelia Veil V2, část 11 (dotažení LENS v editoru, přístupnost)

Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

```
Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky, stručně. Tvrdý limit 250 000 tokenů na session,
cíl ~180k. Žádní agenti, soubory nečti celé opakovaně, nepiš dlouhé texty do chatu. Když se blížíš ke 200k, zastav
se, commitni hotové a napiš prompt na pokračování.

Podklady (jen relevantní části): horní záznam docs/STATUS.md; docs/DESIGN_SYSTEM.md §5.5 (Gestures, Inspector,
Accessibility), §5.10; kód `ui/HomeEditorState.kt` (`startLensDrag`/`dragLensBy`/`dropLens`, `lensSlot`),
`ui/HomeInspector.kt` (`LensInspector`, `GhostArc`, `InspectorCard`), `ui/HomeRoot.kt` (editor vrstva), `ui/Search.kt`.

Hotovo minule (`7dca980`): poloha LENS je `Anchor.dx` (dp od středu, ±120, ≥ 60 dp od okraje, migrace z `Reach`),
tažení LENS v editoru se snapem na střed, ghost ARC, undo jedním krokem, posuvník Poloha v inspektoru; Search a ARC
berou polohu z aktivního rozvržení; sloty řady LENS u okraje přetékají na druhou stranu. Release měření beze změny
(14–16 janky, p99 18–24 ms). Ověřeno na telefonu: tažení, undo, uložení, Search, ARC vpravo.

Tato session (každý bod: testy → build → telefon → commit + push):
1. Ghost ARC je při vybrané LENS částečně pod kartou inspektoru: karta LENS ať se drží mimo oblouky (nebo je ghost
   nad scrimem a karta průhlednější) — ověř screenshotem s LENS na středu i u okraje.
2. TalkBack custom actions na LENS v editoru ("Posunout vlevo/vpravo", "Na střed") přes `setLensDx`; ověř
   `uiautomator dump` (akce v uzlu) a pokud jde, TalkBack ručně majitelem.
3. Search morph s LENS mimo střed: `screenrecord` LENS vpravo (+120) → Search, zkontroluj že kruh startuje na LENS.
4. Tažení LENS jede za prstem o touch slop (~8 dp) — přičti slop k posunu (u LENS i ikon stejně), test.
5. Zeptej se majitele na: pocit tažení LENS a přeskládání řady u okraje, pinch, styl Veil ikon, záznam morphu.

Git: větev `claude/crystal-redesign`, na začátku `git fetch` a `git status`, malé commity + push, žádný merge do
`main`. Před testy zazálohuj shared_prefs/intelia.xml (`adb exec-out run-as dev.intelia.launcher.lab cat …`), po
testu vrať (push do /data/local/tmp, `am force-stop`, `run-as … cp`, ověř `cmp`). Release build pro měření:
dočasně v `app/build.gradle.kts` release `signingConfig = signingConfigs.getByName("debug"); applicationIdSuffix =
".lab"`, `assembleRelease`, soubor hned vrátit; po instalaci `cmd package compile -m speed -f dev.intelia.launcher.lab`;
na konci zase nainstalovat debug (jinak nejde `run-as`). Build: `.\gradlew.bat :app:testDebugUnitTest
:app:assembleDebug` (JAVA_HOME = `C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`; v Git Bash
`MSYS_NO_PATHCONV=1`, ale cesty pro `adb push/install` převeď `cygpath -w`), adb
`C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe`. Telefon Nothing Phone (3), 1080×2392,
LENS (540, 2193) px; Lab: `am start -n dev.intelia.launcher.lab/dev.intelia.launcher.MainActivity`. Před KAŽDÝM
`input` ověř `topResumedActivity` (po instalaci se může nahoře objevit jiná aplikace); klepnutí u kamery chytá
overlay PULSE všude. Texty přes `uiautomator dump`. Python skripty piš do souboru ve scratchpadu. Na konci STATUS
jen ověřená fakta, DESIGN_SYSTEM §5, nový prompt v docs/NEXT_SESSION.md, krátký report.
```

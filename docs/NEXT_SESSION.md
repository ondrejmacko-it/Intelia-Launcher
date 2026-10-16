# Prompt pro další session — Intelia Veil V2, část 10 (LENS v editoru, zbytek Search)

Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

```
Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky, stručně. Tvrdý limit 250 000 tokenů na session,
cíl ~180k. Žádní agenti, soubory nečti celé opakovaně, nepiš dlouhé texty do chatu. Když se blížíš ke 200k, zastav
se, commitni hotové a napiš prompt na pokračování.

Podklady (jen relevantní části): horní záznam docs/STATUS.md; docs/DESIGN_SYSTEM.md §5.5 (Gestures in the editor:
LENS drag x 60–300 dp, snap na střed, ghost ARC), §5.10 (release měření); kód `data/LauncherSettings.kt` (`Reach`),
`ui/Lens.kt` (`lensShift`), `ui/HomeEditorState.kt` (geometry, `lensX`, drag), `ui/HomeEditor.kt`.

Hotovo minule: Search skládá kartu dva snímky po poli (release: p99 28–29 → 21–25 ms, nejdelší UI snímek 21–26 →
10–19 ms); list voleb nad Search/šuplíkem je Veil Focus + 60 % scrim. Ověřeno na telefonu: Reduced Motion, dlouhý
stisk v Search, Enter (aplikace i FLOW), světlý motiv; morph nahrán (screenrecord). Debug build měřit nemá smysl
(debuggable = bez AOT, ~2,5× pomalejší) — měř release build podepsaný debug klíčem se stejným `.lab` id.

Tato session (každý bod: testy → build → telefon → commit + push):
1. LENS tažení v editoru (§5.5): spojitá poloha LENS (dp od středu, -120…+120 → x 60–300 dp), migrace z `Reach`
   v `sanitized()` s testy, tažení LENS v editoru (ne ARC), snap na střed, ghost ARC ukazuje přizpůsobený oblouk,
   undo jeden krok, posun i z inspektoru LENS (přístupnost). Search pilulka a ARC berou novou polohu.
2. Změř LENS → Search v release buildu po změně (stejný postup jako §5.10), ať se nic nezhoršilo.
3. Požádat majitele o test pinch, stylu Veil ikon a o pohled na záznam morphu.

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

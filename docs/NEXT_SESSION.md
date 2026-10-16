# Prompt pro další session — Intelia Veil V2, část 12 (TalkBack v editoru, slop ikon)

Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

```
Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky, stručně. Tvrdý limit 250 000 tokenů na session,
cíl ~180k. Žádní agenti, soubory nečti celé opakovaně, nepiš dlouhé texty do chatu. Když se blížíš ke 200k, zastav
se, commitni hotové a napiš prompt na pokračování.

Podklady (jen relevantní části): horní záznam docs/STATUS.md; docs/DESIGN_SYSTEM.md §5.5 (Gestures, Inspector,
Accessibility), §4.8; kód `ui/HomeRoot.kt` (editor vrstva, `editActions` pro LENS), `ui/Lens.kt` (uzel LENS:
semantics `onClick`/`onLongClick`), `ui/HomeEditorState.kt` (`withSlop`, `setLensDx`), `ui/HomeInspector.kt`.

Hotovo minule (`7780a44`, `1861107`, `f361373`): karta inspektoru LENS se drží nad nejvyšším ghost obloukem (smí
krýt hodiny); TalkBack akce "Posunout LENS vlevo/vpravo" (24 dp) a "LENS na střed"; tažení v editoru přičítá touch
slop (LENS ověřena: 50 dp tažení → 55 dp z 104, střed 685 px vs prst 682 px); Search morph s LENS vpravo ověřen
záznamem (`screenrecord --output-format=raw-frames --size 216x480`, nekončí sám → `pkill -INT screenrecord`; RGB
216×480×3 B na snímek, montáž přes vlastní PNG zápis v Pythonu — PIL ani ffmpeg nejsou).

Tato session (každý bod: testy → build → telefon → commit + push):
1. TalkBack v editoru: dvojklep na LENS teď volá `onClick` = Search, ne výběr. V editoru ať `onClick` LENS vybere
   (otevře inspektor) a `onLongClick` neotevírá ARC; stejně zkontroluj ikony. Ověř `uiautomator dump`
   (clickable/long-clickable) a ručně majitelem s TalkBackem včetně nových akcí.
2. Změř slop u tažení ikony v editoru (`input swipe` o známou vzdálenost, polohu přes dump) — mělo by sedět jako LENS.
3. Pokud majitel odpověděl na otázky (pocit tažení LENS, přeskládání řady u okraje, pinch, styl Veil ikon, morph),
   zapracuj; jinak se zeptej znovu a vyber další bod z §5.5 "As built" (vlastní text popisku / varianty ikon).

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

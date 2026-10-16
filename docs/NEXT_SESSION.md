# Prompt pro další session — Intelia Veil V2, část 13 (paleta barev, barevné ikony, Veil hodiny, Clean Veil)

Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

```
Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky, stručně. Tvrdý limit 250 000 tokenů na session,
cíl ~180k. Žádní agenti, soubory nečti celé opakovaně, nepiš dlouhé texty do chatu, výstupy příkazů filtruj
(grep/tail). Když se blížíš ke 200k, zastav se, commitni hotové a napiš prompt na pokračování.

Podklady (jen relevantní části): horní záznam docs/STATUS.md; docs/DESIGN_SYSTEM.md §4 (barvy, Accent, Veil úrovně),
§5.6, §5.10; docs/design/veil-prototype.html (hodiny `.clock.bgv`, volba `bg` none/veil ř. ~423 a ~642); kód
`ui/Ink.kt` (`accentColor` ř. ~158, `inkPalette`, `InteliaTheme`), `ui/Veil.kt` (`veilTokens`, `veilSurface`),
`ui/Icons.kt` (`RenderedIconImage`, IconStyle Veil), `data/LauncherSettings.kt` (`Accent` ř. 11, `IconStyle`,
`ClockStyle`, `sanitized()`), `data/HomeModel.kt` (`ItemTint` ř. ~85), `ui/HomeInspector.kt`, `ui/Studio.kt`.

Hotovo minule (`130df1a`, `cf4f122`): drawer se skládá 700 ms po klidu Home a zůstává zaparkovaný mimo obrazovku
(alpha 0, bez sémantiky), otevření jen animuje; `IconCache.warm` předehřívá ikony (cache 16 MB); první snímek
drawer 23.6 → 2.8 ms UI (release). Animace Calm zkráceny o ~25 % (§5.6 "brisker motion"). Pozor: minule byl Lab
odinstalován (`adb uninstall`) → HOME role je teď `com.nothing.launcher` a PULSE accessibility služba Labu není
zapnutá; zeptej se majitele, zda to chce vrátit (nastavení dělá on). Nikdy neodinstalovávej, jen `install -r`.

Tato session (každý bod: testy → build → telefon → commit + push; pořadí 2 → 6):
1. Plynulost: majitel potvrdil, že release build je výrazně plynulejší než debug (debug: snímek 85 ms při otevření
   drawer, Search 5–6 % jank). V telefonu je teď release Labu (nejde `run-as`): před zálohou prefs nainstaluj
   debug přes `install -r`, na konci vrať release (`install -r`, `compile -m speed`), ať majitel jezdí plynule.
2. PALETA BAREV: `Accent` rozšířit (přidávat jen na konec enumu, staré názvy zachovat) na 16 odstínů + Dynamic
   (např. Rose, Coral, Amber, Lime, Mint, Teal, Sky, Indigo, Violet, Orchid), dark/light varianta s kontrastem
   ≥ 4.5:1 proti Veil podkladu (JVM test). Nový `palette` v `LauncherSettings` (clock, text, icons, lens, search,
   studio: `Accent?`, null = hlavní akcent) + `unified` přepínač (výchozí true = jedna barva všude). Validace jen
   v `sanitized()`, staré dokumenty beze změny (ItemTint.Accent = barva daného cíle palety). Propsat do Studia
   (texty, přepínače, posuvníky přes `LocalInk`/Material scheme), Search, drawer, LENS. Volba v Studio (mřížka
   barev) a v inspektoru hodin/LENS.
3. BAREVNÉ IKONY: nový `IconStyle` na konec enumu (např. `Tinted`): glyf/duotón z barvy palety pro ikony na jemném
   Veil disku; System/Ink/Veil zůstávají; volba v Studio i v inspektoru ikony. Screenshot na světlé i tmavé tapetě.
4. VEIL HODINY: volba pozadí hodin "Veil" (`.clock.bgv`: padding 14/20/16 px, radius 26, Veil podklad za časem
   i datem) v inspektoru hodin; nové pole na konec modelu + `sanitized()`.
5. CLEAN VEIL: čistší průhlednost — méně šedého závoje (base/deep v `veilTokens`), jasnější okraj, čitelný text;
   jednotně (karty, plachty, pilulky, inspektor); screenshoty před/po.
6. TalkBack: dvojklep na LENS v editoru volá Search místo výběru (v editoru `onClick` = výběr, `onLongClick`
   neotevírá ARC; zkontroluj i ikony); změřit slop u tažení ikon. Požádej majitele o TalkBack test.

Git: větev `claude/crystal-redesign`, na začátku `git fetch` a `git status`, malé commity + push, žádný merge do
`main`. Zálohuj shared_prefs/intelia.xml (`adb exec-out run-as dev.intelia.launcher.lab cat …`), po testu vrať
(push do /data/local/tmp, `am force-stop`, `run-as … cp`, ověř `cmp`). Release pro měření: dočasně v
`app/build.gradle.kts` release `signingConfig = signingConfigs.getByName("debug"); applicationIdSuffix = ".lab"`,
`assembleRelease`, soubor hned vrátit; `install -r` (stejný klíč, žádný uninstall), `cmd package compile -m speed -f
dev.intelia.launcher.lab`; na konci `install -r` debug. Měření: `dumpsys gfxinfo … reset` / `framestats`
(sloupce IntendedVsync, HandleInputStart, SyncStart, IssueDrawCommandsStart, GpuCompleted). Build:
`.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` (JAVA_HOME = `C:\Users\Ondra\AppData\Local\lesnik-toolchain\
jdk-17.0.20.1+1`; v Git Bash `MSYS_NO_PATHCONV=1`, ale cesty pro `adb push/install` a argumenty `python` převeď
`cygpath -w`), adb `C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe`. Telefon Nothing
Phone (3), 1080×2392, 120 Hz, LENS (540, 2193) px, swipe up na Home `input swipe 540 1900 540 900 150`; Lab:
`am start -n dev.intelia.launcher.lab/dev.intelia.launcher.MainActivity`. Před KAŽDÝM `input` ověř
`topResumedActivity`; klepnutí u kamery chytá overlay PULSE všude. Texty přes `uiautomator dump`. Python skripty
piš do souboru ve scratchpadu. Na konci STATUS jen ověřená fakta, DESIGN_SYSTEM §5, nový prompt v
docs/NEXT_SESSION.md, krátký report.
```

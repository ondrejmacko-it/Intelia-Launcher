# Prompt pro další session — Intelia Veil V2, část 14 (zpětná vazba na paletu, LENS v barvě, Studio náhled, měření)

Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

```
Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky, stručně. Tvrdý limit 250 000 tokenů na session,
cíl ~180k. Žádní agenti, soubory nečti celé opakovaně, nepiš dlouhé texty do chatu, výstupy příkazů filtruj
(grep/tail). Když se blížíš ke 200k, zastav se, commitni hotové a napiš prompt na pokračování.

Podklady (jen relevantní části): horní záznam docs/STATUS.md; docs/DESIGN_SYSTEM.md §5.2 (Clean Veil), §5.8
(Tinted), §5.12 (paleta, Veil hodiny); kód `data/AccentColors.kt`, `data/LauncherSettings.kt` (`Palette`,
`accentFor`), `ui/Ink.kt` (`PlaceColors`, `AccentScope`), `ui/PaletteEditor.kt`, `ui/Icons.kt`, `ui/Veil.kt`,
`ui/HomeRoot.kt` (hodiny ~ř. 470, `LensArc` ~ř. 585), `ui/Lens.kt` (`onEditSelect`).

Hotovo minule (`27c5a9b`…`1554151`): 16 akcentů + Dynamic s kontrastem ≥ 4.5:1, paleta po místech (hodiny,
text, ikony, LENS, hledání, studio) s přepínačem "Jedna barva všude"; IconStyle Tinted; Veil podklad hodin;
Clean Veil (tenčí, čistší závoj, jasnější okraj); v editoru klik/Enter na LENS vybírá místo Search. V telefonu je
release Lab (bez `run-as`), HOME role = Lab.

Tato session (každý bod: testy → build → telefon → commit + push):
1. Zpětná vazba majitele na paletu, Tinted ikony, Veil hodiny a Clean Veil (zeptej se na začátku; co chce
   upravit, udělej první). Požádej o TalkBack test dvojklepu na LENS v editoru.
2. LENS v barvě palety: když má místo LENS vlastní barvu (ne unified), kreslit kroužek LENS v ní (dnes jen ARC
   zvýraznění); místo Text použít i na popisky ikon na Home a datum bez tintu (rozhodni s majitelem).
3. Studio náhled: Veil podklad hodin a Tinted ikony i v `HomePreview`, ať náhled odpovídá Home.
4. Release měření po Clean Veil a Tinted (drawer swipe, LENS → Search, 3 série), porovnat se STATUS 2026-10-16.
5. Tinted na opravdu světlé a tmavé tapetě — jen pokud majitel tapetu sám přepne nebo to dovolí.

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

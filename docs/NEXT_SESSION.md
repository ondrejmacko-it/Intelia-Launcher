# Prompt pro další session — Intelia Veil V2, část 9 (plynulost Search, ověření, editor)

Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

```
Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky, stručně. Tvrdý limit 250 000 tokenů na session,
cíl ~180k. Žádní agenti, soubory nečti celé opakovaně, nepiš dlouhé texty do chatu. Když se blížíš ke 200k, zastav
se, commitni hotové a napiš prompt na pokračování.

Podklady (jen relevantní části): horní záznam docs/STATUS.md; docs/DESIGN_SYSTEM.md §5.6 (odstavec „As built
(Search in Veil)“), §5.10 (měření), §5.5 (LENS v editoru); kód `ui/Search.kt` (SearchOverlay, DrawerOverlay),
`search/SearchPicks.kt`, `ui/HomeRoot.kt` (Page.Search/Drawer jako vrstvy nad Home).

Hotovo minule: LENS → Search jako Veil vrstva nad ztlumenou plochou (pilulka jede s IME, karta ≤ 6, FLOW karta,
jedno Back), šuplík jako Veil Focus, pole šuplíku otevírá Search. Měření (3 cykly, teplé): janky 83–85 → 43–45
snímků, p99 150 → 61–65 ms; první snímek otevření ~46 ms UI, snímky při jízdě s IME 10–13 ms (rozpočet 8,3 ms).

Tato session (každý bod: testy → build → telefon → commit + push):
1. Plynulost otevření Search: zjisti, co stojí ~46 ms v prvním snímku (framestats skript: sloupce
   HandleInputStart…SyncStart; zkus přednačíst/odložit kartu, vyhnout se rekompozici Home při změně `page`,
   `SystemBarIcons`) a proč snímky při IME stojí 10–13 ms (RenderThread? scrim + Veil gradienty). Měř před/po stejným
   postupem (zahřátí 1× open/close, `gfxinfo reset`, 3× LENS → Search → Back → Back); hlas janky počet i %, p99.
2. Ověřit na telefonu, co minule nešlo: Reduced Motion (0 ms), dlouhý stisk v Search (možnosti, Search nesmí
   zmizet), Enter na klávesnici (aplikace / FLOW), světlý motiv; morph kroužek → pilulka zachytit `screenrecord`.
3. Požádat majitele o test pinch a stylu Veil ikon (adb nemá multi-touch).
4. Zbytek: LENS tažení v editoru (§5.5), 18 ms stagger řádků (jen pokud nezhorší janky).

Git: větev `claude/crystal-redesign`, na začátku `git fetch` a `git status`, malé commity + push, žádný merge do
`main`. Před testy zazálohuj shared_prefs/intelia.xml (`adb exec-out run-as dev.intelia.launcher.lab cat …`), po
testu vrať (push do /data/local/tmp, `am force-stop`, `run-as … cp`, ověř `cmp`) — změny v panelu Vzhled se ukládají
hned. Build: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` (JAVA_HOME =
`C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`; v Git Bash `MSYS_NO_PATHCONV=1`), adb
`C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe`. Telefon Nothing Phone (3), 1080×2392,
LENS (540, 2193) px; Lab: `am start -n dev.intelia.launcher.lab/dev.intelia.launcher.MainActivity`. Před `input`
ověř `topResumedActivity`; klepnutí u kamery chytá overlay PULSE všude. Texty přes `uiautomator dump`. Python
skripty piš do souboru ve scratchpadu (inline heredoc s uvozovkami se rozbije). Na konci STATUS jen ověřená fakta,
DESIGN_SYSTEM §5, nový prompt v docs/NEXT_SESSION.md, krátký report.
```

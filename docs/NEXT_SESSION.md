Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky, stručně. Tvrdý limit 250 000 tokenů na session,
cíl ~180k. Žádní agenti, soubory nečti celé opakovaně, výstupy příkazů filtruj. Když se blížíš ke 200k, zastav se,
commitni hotové a napiš prompt na pokračování.

Podklady: horní záznam docs/STATUS.md; docs/DECISIONS.md D13 (tapety: kreslené launcherem bez oprávnění + volitelně
jako systémová tapeta s `SET_WALLPAPER` ve vlastním commitu); docs/DESIGN_SYSTEM.md §5.2, §5.5.3, §5.12;
docs/screens/README.md; kód `ui/HomeRoot.kt` (pozadí Home, `homeInk`, `wallPlaces`, `TEXT_TOP`, `TEXT_EDGE`), `ui/HomeEditor.kt`
(`EditorBar`, `BAR_HEIGHT`), `ui/Veil.kt`, `data/LauncherSettings.kt` (`sanitized()`, `Glass`, `IconShape`),
`data/HomeModel.kt` (`DockSpec`).

Hotovo minule (`811eae3`): celé nastavení je v rozbalovacích sekcích, otevřená je vždy jen jedna (zvýrazněná
akcentem); duplicity pryč (vzhled jen ve Vzhledu, chování jen ve Studiu); volby bez účinku se v dané kombinaci
skrývají (Matné je pátý materiál, písmo hodin je jeden seznam, tloušťka jen u písem s váhami…), viz DESIGN_SYSTEM
§5.5.4. Náhled ve Studiu kreslí hodiny a datum z plochy. Na
začátku se zeptej majitele, jestli mu vadí, že lišta editoru překrývá hodiny dané úplně nahoru (řešení: lištu při
tažení hodin skrýt / přesunout dolů), a zda chce u řady LENS vlastní posun každého slotu.

Tato session (každý bod: testy → build → telefon → commit + push):
1. Bod ④ programu D13: 10+ minimalistických rozmazaných tapet kreslených launcherem (procedurálně, bez assetů
   a oprávnění), výběr ve Vzhledu a ve Studiu, světlý i tmavý motiv, kontrast textu (≥ 4.5:1 pro hodiny/datum).
   Nejdřív navrhni model (`LauncherSettings.wallpaper` + `sanitized()`) a náhled majiteli, pak implementuj.
   Měř cenu kreslení (čas snímku) na telefonu, žádná tvrzení bez měření.
2. Využití celé obrazovky pro prvky: majitel chce mít hodiny a ostatní prvky i úplně nahoře / u okrajů
   (ověř, co ještě brání — okraje 28 dp u hodin, `MARGIN` 16 dp, `areaTop` = status + 24 dp, PULSE u kamery).
3. Volitelně „Nastavit jako systémovou tapetu“ — jen pokud majitel potvrdí; `SET_WALLPAPER` v samostatném commitu
   a záznam v DECISIONS.
4. Snímky (nové tapety, rozestupy řady LENS, nová lišta) do `docs/screens/` (nová složka, starou smazat), README.

Git: větev `claude/crystal-redesign`, na začátku `git fetch` a `git status`, malé commity + push, žádný merge do
`main`. V telefonu je debug Lab (run-as funguje), HOME role = Lab. Zálohuj shared_prefs/intelia.xml
(`adb exec-out run-as dev.intelia.launcher.lab cat …`), po testu vrať (push do /data/local/tmp — cesta pro adb.exe
ve tvaru `C:/…`, ne `/c/…` —, `am force-stop`, `run-as … cp`, ověř shodu bajtů). Build:
`.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` (JAVA_HOME =
`C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`; výstup přesměruj do souboru a filtruj; daemon
často padá — při „daemon disappeared“, „Test process encountered an unexpected problem“ nebo `commit_memory` smaž
`hs_err_pid*.log`, spusť `gradlew --stop` a build znovu, klidně dvakrát), adb
C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe. Telefon Nothing Phone (3), 1080×2392,
LENS (540, 2193) px; editor = dlouhý stisk na prázdné ploše (540, 1600) (v otevřeném editoru otevře menu Přidat);
lišta editoru je teď nižší: ✓ ~(1050, 88), Předlohy ~(90, 88), Vzhled ~(205, 88) px (ověř dumpem); hodiny a datum
jsou o ~130 px výš než dřív. Před KAŽDÝM `input` ověř `topResumedActivity`. Python ovladač napiš do scratchpadu
(adb přes subprocess s MSYS_NO_PATHCONV, `uiautomator dump /dev/tty`, výpis s `sys.stdout.reconfigure(encoding="utf-8")`
— jinak padá na znaku −, hledání podle textu i content-desc, klepnutí na střed bounds; čipy se stejným textem
rozliš posledním výskytem; ARC držením přes `input motionevent DOWN/MOVE/UP`; snímek → JPEG 50 %, kontrolní arch
více snímků najednou). Na konci STATUS jen ověřená fakta, DESIGN_SYSTEM §5, nový prompt v docs/NEXT_SESSION.md,
krátký report.

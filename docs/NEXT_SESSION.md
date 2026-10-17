Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky, stručně. Tvrdý limit 250 000 tokenů na session,
cíl ~180k. Žádní agenti, soubory nečti celé opakovaně, výstupy příkazů filtruj. Když se blížíš ke 200k, zastav se,
commitni hotové a napiš prompt na pokračování.

Podklady: horní záznam docs/STATUS.md; docs/DECISIONS.md D13 (tapety: kreslené launcherem bez oprávnění + volitelně
jako systémová tapeta s `SET_WALLPAPER` ve vlastním commitu); docs/DESIGN_SYSTEM.md §5.2, §5.5.3, §5.12;
docs/screens/README.md; kód `ui/Dock.kt` (řada LENS na libovolné hraně, otočení ARC), `ui/HomeRoot.kt` (pozadí Home,
`homeInk`, `wallPlaces`), `ui/Veil.kt`, `data/LauncherSettings.kt` (`sanitized()`, `Glass`, `IconShape`).

Hotovo minule (`7da1855`, `f78f659`, `db5d450`): tvary disku ikon (Kruh / Squircle / Zaoblený čtverec / Slza) pro
Veil a Tónované; řada LENS dole / nahoře / vlevo / vpravo + LENS první / uprostřed / poslední v inspektoru LENS
(`LayoutSpec.dock`), ARC se z boku i shora otevírá ke středu, na boku sloty bez popisků; TalkBack: Větší / Menší na
tečce velikosti ověřil majitel; nová sada snímků `docs/screens/v0.1.0-2026-10-17b/` (i světlý motiv).

Na začátku se zeptej majitele, jak mu sedí řada LENS na boku / nahoře (rozestupy 46 dp, žádné popisky na boku,
vyhledávání se otevírá vždy dole) a tvary ikon.
Tato session (každý bod: testy → build → telefon → commit + push):
1. Bod ④ programu D13: 10+ minimalistických rozmazaných tapet kreslených launcherem (procedurálně, bez assetů a
   oprávnění), výběr ve Vzhledu a ve Studiu, světlý i tmavý motiv, kontrast textu (≥ 4.5:1 pro hodiny/datum).
   Nejdřív navrhni model (`LauncherSettings.wallpaper` + `sanitized()`) a náhled majiteli, pak implementuj.
   Měř cenu kreslení (čas snímku) na telefonu, žádná tvrzení bez měření.
2. Volitelně „Nastavit jako systémovou tapetu“ — jen pokud majitel potvrdí; `SET_WALLPAPER` v samostatném commitu
   a záznam v DECISIONS.
3. Snímky nových tapet do `docs/screens/` (nová složka, starou smazat), README.

Git: větev `claude/crystal-redesign`, na začátku `git fetch` a `git status`, malé commity + push, žádný merge do `main`.
V telefonu je debug Lab (run-as funguje), HOME role = Lab. Zálohuj shared_prefs/intelia.xml (`adb exec-out run-as
dev.intelia.launcher.lab cat …`), po testu vrať (push do /data/local/tmp — cesta pro adb.exe ve tvaru `C:/…`, ne
`/c/…` —, `am force-stop`, `run-as … cp`, ověř shodu bajtů). Build: `.\gradlew.bat :app:testDebugUnitTest
:app:assembleDebug` (JAVA_HOME = `C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`; výstup přesměruj do
souboru a filtruj; při „Test process encountered an unexpected problem“ nebo `commit_memory` spusť `gradlew --stop`
a build znovu), adb C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe.
Telefon Nothing Phone (3), 1080×2392, LENS (540, 2193) px; editor = dlouhý stisk na prázdné ploše (540, 1600)
(v otevřeném editoru otevře menu Přidat); hodiny ~(230, 700), datum (318, 414); ✓ = „Uložit rozložení“ (995, 200).
Před KAŽDÝM `input` ověř `topResumedActivity`. Python ovladač napiš do scratchpadu (adb přes subprocess s
MSYS_NO_PATHCONV, `uiautomator dump /dev/tty`, hledání podle textu i content-desc, klepnutí na střed bounds; čipy se
stejným textem — např. „Veil“ (sklo i ikony) — rozliš posledním výskytem; ARC držením přes `input motionevent
DOWN/MOVE/UP`; snímek → JPEG 50 %, kontrolní arch více snímků najednou). Na konci STATUS jen ověřená fakta,
DESIGN_SYSTEM §5, nový prompt v docs/NEXT_SESSION.md, krátký report.

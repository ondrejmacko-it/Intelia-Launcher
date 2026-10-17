Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky, stručně. Tvrdý limit 250 000 tokenů na session,
cíl ~180k. Žádní agenti, soubory nečti celé opakovaně, výstupy příkazů filtruj. Když se blížíš ke 200k, zastav se,
commitni hotové a napiš prompt na pokračování.

Podklady: horní záznam docs/STATUS.md; docs/DECISIONS.md D13; docs/DESIGN_SYSTEM.md §5.5.2; docs/screens/README.md;
kód `ui/HomeRoot.kt` (geometrie LENS řádku `geometry.lensY`, `lensPt`, `ArcRings`, `LensRow` sloty, `SizeHandle`),
`data/HomeModel.kt` (`AnchorType.LensRow`, `HomeItem`), `data/LauncherSettings.kt` (`IconStyle`, `editorCard`),
`ui/HomeEditor.kt` (`SizeHandle`, `PlateHandles`), `ui/Veil.kt` (`plateGlass`).

Hotovo minule (`5ac8c7f`, `a6b507b`, `780911a`): karta editoru se pamatuje s rozložením (`editorCard`); úchyty
karty jsou nenápadné značky v jejím okraji, úchyty desek a tečky velikosti malé a neutrální (majitel: „menší, méně
výrazné, nepřekážet“); výraznější sklo desek (`plateGlass`); Vzhled a Předlohy jsou stejná přesouvatelná karta;
tečka velikosti i u ikon a LENS (mimo prvek); oprava data v JetBrains Mono (poslední slovo mizelo); test českých
glyfů ve všech písmech. Ověřeno: otočení, čeština, Studio (Zahodit/Uložit), popisky přístupnosti.

Na začátku se zeptej majitele, jak mu sedí nové nenápadné úchyty a výraznější desky.
Tato session (každý bod: testy → build → telefon → commit + push):
1. Bod ③ programu D13: dok/LENS kdekoli + tvary ikon (squircle, zaoblený čtverec, kapka), pořadí 4 slotů,
   ukotvení dole/nahoře/vlevo/vpravo (na boku svisle, ARC do středu). Nejdřív navrhni model (pole v `LayoutSpec`
   nebo `LauncherSettings` + `sanitized()`), krátce ho ukaž majiteli, pak implementuj.
2. Skutečný TalkBack (akce Větší/Menší na úchytech) — jen pokud majitel TalkBack zapne sám; jinak nech neověřené.
3. Nová sada snímků do `docs/screens/<verze>-<datum>/`, starou `v0.1.0-2026-10-17` smazat (zůstane v historii),
   aktualizovat docs/screens/README.md; zahrnout i světlý motiv.

Git: větev `claude/crystal-redesign`, na začátku `git fetch` a `git status`, malé commity + push, žádný merge do `main`.
V telefonu je debug Lab (run-as funguje), HOME role = Lab. Zálohuj shared_prefs/intelia.xml (`adb exec-out run-as
dev.intelia.launcher.lab cat …`), po testu vrať (push do /data/local/tmp, `am force-stop`, `run-as … cp`, ověř shodu
bajtů). Build: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` (JAVA_HOME =
`C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`; výstup přesměruj do souboru a filtruj, daemon občas
spadne — stačí spustit znovu), adb C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe.
Telefon Nothing Phone (3), 1080×2392, LENS (540, 2193) px; editor = dlouhý stisk na prázdné ploše (540, 1300)
(v otevřeném editoru otevře menu Přidat); hodiny vlevo nahoře (~(230, 700)), datum (318, 414); ✓ = „Uložit
rozložení“ (995, 200). Před KAŽDÝM `input` ověř `topResumedActivity`. Python ovladač ve scratchpadu (adb přes
subprocess s MSYS_NO_PATHCONV, `uiautomator dump /dev/tty`, hledání podle textu i content-desc, klepnutí na střed
bounds, snímek → JPEG 50 % + náhled 40 %); uiautomator vrací i bounds mimo viditelnou část karty — klepnout jen
uvnitř. Uložená poloha karty z testu se po obnově prefs vrátí na majitelovu. Na konci STATUS jen ověřená fakta,
DESIGN_SYSTEM §5, nový prompt v docs/NEXT_SESSION.md, krátký report.
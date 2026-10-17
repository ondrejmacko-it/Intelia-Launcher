# Prompt pro další session — Intelia, část 15 (hodiny a datum: fonty, oddělovače, orientace, volný posun)

Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

```
Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky, stručně. Tvrdý limit 250 000 tokenů na session,
cíl ~180k. Žádní agenti, soubory nečti celé opakovaně, výstupy příkazů filtruj. Když se blížíš ke 200k, zastav se,
commitni hotové a napiš prompt na pokračování.

Podklady: horní záznam docs/STATUS.md; docs/DECISIONS.md D11 a D13; docs/DESIGN_SYSTEM.md §5.2.1, §5.12; kód
`data/HomeModel.kt` (`HomeItem`, `DateFormat`, `ClockBackground`), `ui/HomeRoot.kt` (hodiny ~ř. 470, `SplitClock`,
`DateText` ~ř. 823), `ui/HomeInspector.kt` (inspektor hodin/data ~ř. 300), `ui/Crystal.kt` (`CrystalType`).

Hotovo minule (`31bd339`, `0a316ce`): globální styl skla Glass = Veil / Krystal / Mráz / Kapka (bez hran: sheen,
zrno, bloom), volba v editoru → Vzhled i ve Studiu; oprava barvy hodin ve světlém motivu na tmavé tapetě
(`LocalWallPlaces`). Majitel schválil (D13): tapety kreslené launcherem + volitelně jako systémová (SET_WALLPAPER),
přibalit 4–6 OFL fontů. Na začátku se zeptej, jak se mu líbí tři skla v telefonu (při jeho hustotě +10 % vypadají
podobně — případně zvýraznit rozdíly).

Plán celého programu (D13): ① skla + audit (hotovo), ② hodiny a datum (tato session), ③ dok/LENS kdekoli +
tvary ikon (squircle iOS, zaoblený čtverec macOS/Windows, kapka; pořadí 4 slotů; ukotvení dole/nahoře/vlevo/vpravo,
na boku svisle, ARC do středu), ④ 10+ minimalistických rozmazaných tapet (procedurálně, Nothing styl) +
"Nastavit jako tapetu", ⑤ profily vzhledu, náhodný styl, Studio náhled = Home (Veil hodiny, Tinted ikony).

Tato session (každý bod: testy → build → telefon → commit + push):
1. Fonty: vybrat 4–6 OFL rodin (návrh: Inter, Space Grotesk, DM Serif Display, JetBrains Mono, Bodoni Moda,
   Unbounded), licence do assets + D14 v DECISIONS, `res/font`; jen subset latin + latin-ext (čeština).
2. Hodiny: oddělovač (`:` `·` `•` `|` mezera nic), orientace (vodorovně / pod sebou hodiny nad minutami / šikmo),
   tloušťka, rozestup písmen, zarovnání (vlevo/střed/vpravo), 12/24 h. Vše v `HomeItem` (sanitizace + testy dekódu).
3. Datum: formáty (+ vlastní: den, číslo, měsíc, rok), font, VERZÁLKY/malá, rozestup, svisle shora dolů, a posun
   data uvnitř spojení s hodinami (offset dx/dy v dp, omezený), vlastní Veil podklad i pro samostatné datum.
4. Inspektor hodin/data: přehledné sekce, živý náhled; proklikat každou volbu na telefonu ve světlém i tmavém
   motivu a v každém skle, opravit, co se rozbije.

Git: větev `claude/crystal-redesign`, na začátku `git fetch` a `git status`, malé commity + push, žádný merge do
`main`. V telefonu je debug Lab (run-as funguje), HOME role = Lab. Zálohuj shared_prefs/intelia.xml (`adb exec-out
run-as dev.intelia.launcher.lab cat …`), po testu vrať (`MSYS_NO_PATHCONV=1 adb push <cygpath -w> /data/local/tmp`,
`am force-stop`, `run-as … cp`, ověř `cmp`). Build: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`
(JAVA_HOME = `C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`), adb
`C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe`. Telefon Nothing Phone (3), 1080×2392,
LENS (540, 2193) px; editor = dlouhý stisk na prázdné ploše (540, 1300); Lab: `am start -n
dev.intelia.launcher.lab/dev.intelia.launcher.MainActivity`. Před KAŽDÝM `input` ověř `topResumedActivity`. Texty
přes `uiautomator dump /dev/tty`, snímky zmenšuj (PIL) na 40 %. Python skripty piš do scratchpadu. Na konci STATUS
jen ověřená fakta, DESIGN_SYSTEM §5, nový prompt v docs/NEXT_SESSION.md, krátký report.
```

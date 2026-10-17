Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky, stručně. Tvrdý limit 250 000 tokenů na session,
cíl ~180k. Žádní agenti, soubory nečti celé opakovaně, výstupy příkazů filtruj. Když se blížíš ke 200k, zastav se,
commitni hotové a napiš prompt na pokračování.

Podklady: horní záznam docs/STATUS.md; docs/DECISIONS.md D13, D14; docs/DESIGN_SYSTEM.md §5.2.1, §5.12.1, §5.5.1;
kód `data/HomeModel.kt` (`TextLook`), `ui/ClockText.kt`, `ui/Kit.kt` (`SplitClock`), `ui/HomeRoot.kt` (`DateText`),
`ui/HomeInspector.kt` (`Fold`, `ClockInspector`, `DateInspector`), `ui/Veil.kt`.

Hotovo minule (`dbacc69`, `a8d71ee`, `ae92400`): skla výrazně odlišená (Krystal fasety + horní hrana, Mráz mléčný se
zrnem, Kapka svítící obvod podle Liquid Glass předlohy majitele); PULSE se v editoru nastavuje klepnutím na kapsli
(editor skryje stavový řádek, překryv PULSE všude nebere dotyky), položka ve Vzhledu zrušena; 6 OFL fontů (D14);
hodiny/datum: písmo, rozestup, oddělovač, vedle sebe / pod sebou / šikmo, 12/24 h, zarovnání, vlastní formát data,
velikost písmen, svisle, posun data, Veil pod samostatným datem; skládací sekce inspektoru s pružinou.

Na začátku se zeptej majitele, jak se mu líbí nová skla a PULSE klepnutím.

Tato session (každý bod: testy → build → telefon → commit + push):
1. Dokončit ověření ② na telefonu (nic z toho zatím neviděno): Pod sebou po opravě (trim řádků), Šikmo, všechny
   oddělovače, 12 h, každé písmo (čeština: ěščřžýáíéůú), datum Vlastní/ABC/abc/svisle/posun/Veil. Světlý i tmavý motiv,
   všechna 4 skla. Pozor: Python s českými texty v `uiautomator` hledání piš do souboru ve scratchpadu (inline v bashi
   se diakritika rozbila); klepnutí na hodiny ověř přes bounds z dumpu.
2. Plynulost inspektoru: náhled písma přímo na čipech (každý čip ve svém fontu), plynulý přechod změny rozložení
   hodin (`animateContentSize` na sloupci hodin), případně segmentový přepínač místo čipů pro Čas.
3. Bod ③ programu D13: dok/LENS kdekoli + tvary ikon (squircle, zaoblený čtverec, kapka), pořadí 4 slotů, ukotvení
   dole/nahoře/vlevo/vpravo (na boku svisle, ARC do středu).

Git: větev `claude/crystal-redesign`, na začátku `git fetch` a `git status`, malé commity + push, žádný merge do
`main`. V telefonu je debug Lab (run-as funguje), HOME role = Lab. Zálohuj shared_prefs/intelia.xml (`adb exec-out
run-as dev.intelia.launcher.lab cat …`), po testu vrať (`MSYS_NO_PATHCONV=1 adb push <cygpath -w> /data/local/tmp`,
`am force-stop`, `run-as … cp`, ověř `cmp`). Build: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`
(JAVA_HOME = `C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`), adb
`C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe`. Telefon Nothing Phone (3), 1080×2392,
LENS (540, 2193) px; editor = dlouhý stisk na prázdné ploše (540, 1300); hodiny v editoru ~ (530, 560); PULSE (600, 60);
Lab: `am start -n dev.intelia.launcher.lab/dev.intelia.launcher.MainActivity`. Před KAŽDÝM `input` ověř
`topResumedActivity`. Texty přes `uiautomator dump /dev/tty`, snímky zmenšuj (PIL) na 40 %. Python skripty piš do
scratchpadu. Na konci STATUS jen ověřená fakta, DESIGN_SYSTEM §5, nový prompt v docs/NEXT_SESSION.md, krátký report.

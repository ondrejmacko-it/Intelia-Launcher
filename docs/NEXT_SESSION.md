# Prompt pro další session — PULSE kapsle podle stavového řádku (session C)

Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

---

Pokračujeme na **Intelia Launcheru** (`AGENTS.md` platí). Větev `claude/ink-rework`, pushnutá, otevřené PR #2 do `main` (nemergovat). Tento soubor je jediný kontext; nic dalšího celé nečti. **Odpovídej česky.**

ROZPOČET: tvrdý limit 200k tokenů, cíl ~120k. Malé commity, na konci push do stejné větve (PR #2 se aktualizuje sám).

## Cíl
Předělat PULSE kapsli u kamery (`ui/Pulse.kt`) tak, aby:
1. **Seděla do stavového řádku**: výška a svislá poloha podle stavového řádku a výřezu kamery, ne pevné `hr * 2 + 10.dp`.
2. **Nikdy nepřekryla ikony stavového řádku** (čas a notifikace vlevo, signál a baterie vpravo).
3. **V klidu ukazovala co nejméně**: žádný text. Stačí prstenec kolem kamery (nabíjení = oblouk baterie) a nanejvýš jedna malá značka (budík = tečka nebo zvoneček) v šířce výřezu. Neopakovat to, co už ukazuje systém (procenta baterie, ikona budíku).
4. **Po klepnutí ukázala detaily**: rozbalí se z kamery dolů (panel `PulsePanel` už existuje: budík s časem a dnem, baterie v % a stav, svítilna). Zavření klepnutím vedle nebo tlačítkem Zpět.
5. Byla **po našem**, ne kopie MT Capsule: jazyk „Prstenec a nit“, akcent terakota, systémová písma, žádný kód ani grafika z MT Capsule.

## Nejdřív se podívej, jak to dělá MT Capsule (jen prohlížet)
- Balíček `com.pryshedko.mtisland`. Běží jako overlay a zachytává klepnutí na kameru. **Nic v ní neměň, nic nevypínej ani neodinstalovávej.**
- Na ploše jiné aplikace pořiď 1–2 screenshoty jejího klidového a rozbaleného stavu (rozbalit ho může majitel; přes adb klepnutí na kameru zachytí MT Capsule, ne Lab). Zapiš si: šířku a výšku klidové kapsle vůči výřezu, co ukazuje v klidu, jak se rozbalí. Screenshoty po prohlédnutí smaž z telefonu (jen ty své).
- Pokud nejde nic z toho udělat bez zásahu do MT Capsule, popiš to a pokračuj podle bodů 1–5.

## Fakta z telefonu (Nothing Phone (3), Android 16, 1080×2392, 420 dpi = 2,625 px/dp)
- Stavový řádek: `statusBars frame=[0,0][1080,126]` → 48 dp.
- Výřez kamery (automatická detekce z `cutoutPath`): střed x 205,5 dp, y 26,0 dp, poloměr 8,0 dp. V Lab je teď uložená **vlastní** poloha kamery (z dřívějška); neměnit bez souhlasu.
- Ikony stavového řádku vlevo končí asi u x ≈ 380 px, vpravo začínají asi u x ≈ 760 px (podle screenshotu 2026-10-16 s budíkem a notifikacemi). Polohy ikon žádné veřejné API nedává, proto kapsle v klidu **nesmí být širší než výřez + malý okraj**. Rozšiřování do stran je jen pro rozbalený stav.
- Současná chyba (STATUS 2026-10-16): s budíkem do 12 h a nabíjením kapsle ukazuje zvoneček + „10:00“ vlevo a „90 %“ vpravo, levá půlka překrývá systémovou ikonu budíku.
- D7 (posuvníky šířky 0–40 dp a neprůhlednosti 40–100 % ve Studiu) přidal minulá session. Rozhodni, jestli šířka v novém návrhu dává smysl. Pokud ne, odeber ji a zapiš to do nového D-záznamu (D8). Neprůhlednost může zůstat.

## Úkoly
1. Prohlédnout MT Capsule (viz výše), krátké poznámky do odpovědi.
2. Návrh do `docs/DECISIONS.md` jako **D8**: klidový stav (co se ukáže při nabíjení, budíku, obojím), rozbalený stav, rozměry odvozené z `WindowInsets.statusBars` a výřezu, co se stane s D7.
3. Implementace v `ui/Pulse.kt` (+ Studio, pokud se mění posuvníky). Čistou logiku (rozměry kapsle z insetu a výřezu, výběr toho, co ukázat) dát do čisté Kotlin funkce a pokrýt JVM testy.
4. Build + testy: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug` (`JAVA_HOME=C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`), PASS/FAIL.
5. Telefon: nainstalovat `.lab`, spustit `am start -n dev.intelia.launcher.lab/dev.intelia.launcher.MainActivity`, ověřit klid s nabíjením, s budíkem (majitel ho nastavuje sám; zkontroluj `dumpsys alarm | grep -A1 "Next alarm clock"`), s obojím a rozbalení klepnutím. Screenshot ukázat majiteli přes SendUserFile.
6. `docs/STATUS.md` jen ověřená fakta, commit, push. Report: příkazy PASS/FAIL, co ověřeno na telefonu a co ne.

## Telefon a soukromí
- adb: `C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe`, v Git Bash `MSYS_NO_PATHCONV=1`. Pro `adb pull` dej cílovou cestu jako Windows cestu bez proměnných za zpětným lomítkem (`"$D\\$f.png"` nefunguje), nebo nejdřív `cd` do scratchpadu a pulluj relativně.
- Před každým `input` ověř, že nahoře je `dev.intelia.launcher.lab/` (`dumpsys activity activities | grep -m1 topResumedActivity`).
- Lab **není** výchozí plocha (HOME je `dev.intelia.launcher.dev`). HOME neměnit bez souhlasu. Klávesa HOME vede do `.dev`.
- Studio: dlouhé podržení na ploše (540,1300). Scrolluj jen u levého okraje (x≈20). Před klepnutím vždy znovu `uiautomator dump`. Neklepej podle souřadnic ze starého dumpu po scrollu (minule to omylem klepnulo jinam).
- Rychlý `input swipe` z LENS nahoru otevře šuplík, ne ARC. Pro ARC použij `input motionevent DOWN/MOVE/UP` s pauzou ≥0,9 s a před UP se vrať na LENS (zrušení), jinak se FLOW spustí.
- Nic nemazat (kromě vlastních dočasných souborů v `/sdcard`), neodinstalovávat, do osobních aplikací nechodit, MT Capsule jen prohlížet. V `/data/local/tmp` leží ~27 starších `.png`, nechat být.

## Na později (neřešit teď)
- Spuštění zkratky puštěním v prstenci 3 (zkratky vedou do osobních aplikací).
- Písma Newsreader / Instrument Sans / Geist Mono jen se souhlasem (licence → D-záznam).
- Posuvníky ve Studiu: tažení korálku a TalkBack zatím neověřené.

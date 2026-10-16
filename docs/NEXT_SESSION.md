# Prompt pro další session — Intelia Ink rework (část 2)

Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

---

Pokračujeme ve vývoji **Intelia Launcheru** (`AGENTS.md` platí). Jsi hlavní Android engineer a UI/UX architekt. **Rozpočet session: max ~200k tokenů.** Šetři: místo screenshotů čti `uiautomator dump`, velké soubory nečti celé, a před limitem skonči commitem a novým promptem v `docs/NEXT_SESSION.md`. Žádný merge do `main`, force push, `reset --hard` ani `git clean`.

## Kde jsme
- Větev `claude/ink-rework`, **zatím nepushnutá**. Build i 19 JVM testů zelené (`./gradlew.bat :app:testDebugUnitTest :app:assembleDebug`, `JAVA_HOME=C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`).
- Hotovo a ověřeno na telefonu (`.lab` jako běžná aktivita, ne HOME): onboarding 3 kroky; Home bez jakékoli celoplošné vrstvy (jen tapeta); Essentials vyvážené kolem LENS; klepnutí na LENS → hledání s klávesnicí; swipe nahoru → šuplík (Moje + A–Z + lišta); výsledky rostou nahoru; Enter otevře první aplikaci; přesná shoda FLOW (40sp kurzíva + korálky + Spustit) a Enter spustí FLOW (Kalkulačka → Hodiny ve správném pořadí); psaní nic nespouští; app options (ARC, oblíbené, skrýt, info); ARC prstenec 1 (puštění spustí, návrat na LENS zruší); ARC prstenec 2 s náhledem kroků během držení a spuštěním FLOW puštěním; Studio s živým náhledem na tapetě; světlý motiv; PULSE kapsle s ukazatelem baterie (při připojení k síti). Studený start ~845 ms, teplý ~57 ms (debug), PSS ~131 MB, žádné pády.
- **Nová kalibrace kamery PULSE** (`ui/Calibration.kt`, Studio → PULSE → Poloha kamery): černá plocha, akcentová tečka se schová pod kameru, tři „nit s korálkem“ posuvníky (Vodorovně / Svisle / Velikost čočky) s ‹ › kroky po 0,5 dp, Automaticky, Uložit. Nastavení `pulseLens`, `pulseEcho`, `pulseAlarm`, `pulseAlarmHours`, `pulseCharging` v `LauncherSettings` (sanitized). Automatická detekce teď bere přesnou `cutoutPath` (Nothing OS hlásí chybný bounding rect).
- Inspirace z MT Capsule (jen prohlíženo): Screen profiles → Size and position (X, Y, šířka, výška, ‹ ›, Center, reset, Save), opacity, border, scale, alignment, animace (styl, délka, bounce), haptika, události mini kapsle.

## Úkoly (v tomto pořadí, malé commity)
1. **Ověřit kalibraci na telefonu**: ‹ › mění hodnotu přesně o 0,5 dp (při minulém testu se hodnoty posunuly podivně — možná souběžné dotyky majitele, ověř izolovaně přes `uiautomator dump`), Uložit → Studio ukáže „Vlastní“, kapsle na ploše sedí na kameře, Automaticky vrátí detekci. Ozvěna/budík/nabíjení přepínače fungují. Zvážit: posuvník pro šířku kapsle a průhlednost (jako MT Capsule) — jen pokud je to levné.
2. Přidat JVM testy: sanitizace `pulseLens` a `pulseAlarmHours`.
3. **Recents bez obdélníku**: požádej majitele o dočasné přepnutí HOME na Lab, pak snímky během gesta do Recents (`screencap` do `/data/local/tmp`, hned smazat). Také tlačítko Home, návrat z aplikací, prstenec 3 se zkratkami (funguje jen jako HOME). Po testu požádej o vrácení HOME na `.dev`.
4. Dokumentace: `docs/DECISIONS.md` D5 (FLOW puštění v ARC a Enter na přesné shodě = explicitní spuštění; swipe nahoru = šuplík; prstenec 3 = zkratky; PULSE jen na ploše; jazyk „Prstenec a nit“; kalibrace kamery), `docs/STATUS.md` jen ověřená fakta (viz výše), `AGENTS.md` (ARC = tři prstence, nové pravidlo FLOW), `README.md`.
5. Push větve, PR do `main` (nemergovat), report: příkazy PASS/FAIL, co ověřeno na telefonu a co ne.

## Známé drobnosti
- Pozice FLOW v prstenci 2 je pevná zleva (první FLOW je úplně vlevo, hůř dosažitelný) — případně navrhnout řazení od středu jako D-záznam.
- Klepnutí na kameru zachytí overlay MT Capsule, takže panel PULSE se přes adb nedá otevřít, dokud MT Capsule běží.
- Písma jsou zatím systémová; Newsreader / Instrument Sans / Geist Mono jen se souhlasem (D6).

## Telefon a soukromí
Nothing Phone (3), Android 16, USB. adb: `C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe`, v Git Bash vždy `MSYS_NO_PATHCONV=1`. Před každým `input` ověř, že nahoře je `dev.intelia.launcher.lab/` (majitel může telefon zároveň používat). Nic neodinstalovávat, nemazat data, HOME neměnit bez souhlasu. Do osobních aplikací nechodit; screenshot s osobním obsahem hned smazat. MT Capsule jen prohlížet, nic neukládat.

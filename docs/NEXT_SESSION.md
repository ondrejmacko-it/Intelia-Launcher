# Prompt pro další session — Intelia Ink rework (session B: dokumentace, push, PR)

Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

---

Pokračujeme na **Intelia Launcheru** (`AGENTS.md` platí). Větev `claude/ink-rework`, **zatím nepushnutá**. Tento soubor je jediný kontext; nic dalšího celé nečti.

ROZPOČET: tvrdý limit 200k tokenů, cíl ~80k. Tato session je jen dokumentace + push + PR, na telefon sahat netřeba.

## Kde jsme (ověřeno)
- Build + **21 JVM testů** zelené: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug` (`JAVA_HOME=C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`).
- Na telefonu (Nothing Phone (3), Android 16, `.lab` build) ověřeno dříve: onboarding 3 kroky; Home jen tapeta; Essentials kolem LENS; klepnutí na LENS → hledání s klávesnicí; swipe nahoru → šuplík; výsledky rostou nahoru; Enter otevře první aplikaci; přesná shoda FLOW (Spustit, Enter spustí FLOW ve správném pořadí); psaní nic nespouští; app options; ARC prstenec 1 a 2 (FLOW puštěním); Studio s živým náhledem; světlý motiv; PULSE kapsle s baterií při nabíjení. Studený start ~845 ms, teplý ~57 ms (debug), PSS ~131 MB.
- **Session A (2026-10-16) ověřila na telefonu:**
  - Kalibrace kamery (Studio → PULSE → Poloha kamery): ‹ › mění hodnotu přesně o 0,5 dp (izolovaně: 205,5→206,0; 25,5→24,5 dvěma kroky; 10,5→11,0). Uložit → Studio „Vlastní“, hodnoty po znovuotevření zachované. Automaticky → Studio „Automaticky“, detekce z `cutoutPath` = 205,5 / 26,0 / 8,0 dp. Kapsle na ploše sedí kolem kamery (1 screenshot, smazán).
  - S Lab jako HOME: Recents (Nothing quickstep fallback) ukazuje za kartami jen tapetu, **žádný obdélník**; Home z Recents, z aplikace (Kalkulačka spuštěná z LENS) i ze šuplíku vrací čistou plochu; výběr zkratek v Studiu funguje jen jako HOME; **ARC prstenec 3** se zobrazí („ZKRATKY“) a vybere zkratku úhlem (−40° = 1. zkratka), návrat na LENS zruší. Spuštění zkratky puštěním **neověřeno** (zkratky vedou do osobních aplikací). Testovací zkratky odebrány, prstence vráceny na 2.
  - Neověřeno: přepínače Ozvěna / budík / nabíjení.
- Nové JVM testy: `pulseLensIsClampedAndNonFiniteFallsBackToAuto`, `pulseAlarmHoursStayWithinADay`.

## Úkoly session B (malé commity)
1. `docs/DECISIONS.md` **D5**: FLOW puštěním v ARC a Enter na přesné shodě = explicitní spuštění; swipe nahoru = šuplík; prstenec 3 = zkratky aplikací (jen jako výchozí plocha); PULSE jen na ploše; jazyk „Prstenec a nit“; kalibrace kamery (krok 0,5 dp, null = automatická detekce z `cutoutPath`).
2. `docs/STATUS.md` jen ověřená fakta (výše), včetně toho, co ověřeno **není**.
3. `AGENTS.md`: ARC = až tři prstence (aplikace / FLOW / zkratky); pravidlo FLOW doplnit o puštění v ARC a Enter na přesné shodě. Krátce.
4. `README.md`: aktualizovat popis pilířů a PULSE.
5. Build + testy, commit, `git push -u origin claude/ink-rework`, PR do `main` (nemergovat). Report: příkazy PASS/FAIL, co ověřeno na telefonu a co ne.

## Známé drobnosti (na později)
- Pozice FLOW v prstenci 2 pevně zleva — případně řazení od středu jako D-záznam.
- MT Capsule overlay zachytí klepnutí na kameru → panel PULSE přes adb neotevřeš, dokud běží.
- Písma systémová; Newsreader / Instrument Sans / Geist Mono jen se souhlasem (D6).
- Volitelně: posuvník šířky kapsle a průhlednosti (jako MT Capsule).
- V `/data/local/tmp` na telefonu leží ~27 starších `.png` (ne z této session) — majitel rozhodne, nic nemazat bez souhlasu.

## Telefon a soukromí (pokud by bylo třeba)
adb: `C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe`, v Git Bash `MSYS_NO_PATHCONV=1` (pro `adb pull` cíl jako Windows cestu). Před každým `input` ověř, že nahoře je `dev.intelia.launcher.lab/`. Ve Studiu scrolluj jen u levého okraje (x≈20) — uprostřed hrozí klepnutí na „Nastavit Intelia jako plochu“, vpravo na „Odebrat“. Nic nemazat, neodinstalovávat, HOME neměnit bez souhlasu, do osobních aplikací nechodit, MT Capsule jen prohlížet.

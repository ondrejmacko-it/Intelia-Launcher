# Design handoff — stav k 2026-10-15

Interaktivní návrh (soukromé plátno, otevřít přihlášený na claude.ai):
https://claude.ai/artifact/Wa9q4hwhdQQYNC1E919rMa

Zdroj z Claude Design: `Intelia Launcher - návrhy.html` (kola 1–3; základem je Ink z kola 2b/3).

## Rozhodnuto
- Styl: vlastní, vychází z **Ink** — Newsreader (hodiny, seznamy), Instrument Sans (UI), Geist Mono (drobné popisky); papír #ECE7DF / inkoust #1C1814, akcent terakota #B5532F; žádné karty, jen jemné linky.
- Filozofie: maximální minimalismus, maximální možnosti; složitost v gestech a nastavení.
- **Home**: hodiny + LENS. Při onboardingu volba: Prázdná / Kolem LENS (Telefon, Zprávy, Foto, Chrome u palce) / Vlastní (textový seznam oblíbených).
- **ARC = prstence** (varianta A): podrž LENS, táhni nahoru; vzdálenost volí prstenec (1: aplikace 5, 2: FLOW 7, 3: zkratky a lidé 9), úhel položku; vnější prstence se odkrývají postupně; pozice pevné kvůli svalové paměti; zpět na LENS = zrušit. Nastavení: počet prstenců 1–3, obsah a pořadí.
- **PULSE = kapsle u kamery**: v klidu velikost čočky (neviditelná), hudba/událost = malá kapsle, klepnutí rozbalí tmavý panel z kamery.
- **Studio**: přizpůsobení se živým náhledem (písmo hodin, motiv, akcent, pohyb, haptika, gesta, ikony).
- Hledání: pole u palce, výsledky rostou nahoru, přesná shoda FLOW + Spustit/Enter.
- Šuplík: řádek „Moje“ + A–Z s lištou.
- FLOW: seznam + editor přes celou obrazovku (Uložit nahoře).

## Otevřené otázky (pokračovat zde)
1. Výchozí rozložení Home pro nové uživatele (navrhuji Kolem LENS).
2. PULSE: co dělá podržení kapsle (návrh: horní ARC s FLOW) a kalibrace čočky.
3. Gesta LENS: swipe nahoru = šuplík? swipe do stran?
4. Tah (psaní písmene na ploše) — později, nebo hned?
5. Doladit Hledání, Šuplík a FLOW editor do nového stylu (zatím starší verze na plátně).

## Pak implementace
Až bude design schválený: nová větev z `claude/standalone-foundation`, Android implementace po obrazovkách s ověřením na telefonu (Nothing Phone 3, USB). Nic na `main` bez schválení.

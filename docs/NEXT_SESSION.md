# Prompt pro další session — Intelia Veil V2, implementace část 1

Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

---

Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky, stručně. **Tvrdý limit 250 000 tokenů na session**, cíl ~180k. Žádní agenti, žádné dlouhé shrnutí, soubory nečti celé opakovaně (jen potřebné řádky), nepiš dlouhé texty do chatu. Když se blížíš ke 200k, zastav se, commitni hotové a napiš prompt na pokračování.

Majitel **schválil směr Intelia Veil (D12)** s úpravami níže. Zapiš schválení do `docs/DECISIONS.md` u D12 (jedna řádka).

Podklady (čti jen relevantní části): `docs/DESIGN_SYSTEM.md` §5, D12 v `docs/DECISIONS.md`, `docs/STATUS.md` (horní záznam). Vizuální reference = `docs/design/veil-prototype.html` (totožný s `Intelia Veil.html` od majitele, 13 scén). Nevymýšlej jinou podobu.

Ověřená fakta (neověřuj znovu): Nothing Phone (3) nemá blur za oknem (`ro.surface_flinger.supports_background_blur=0`, `mBlurEnabled=false`), tapeta je živá Nothing Atmosphere → rozostření tapety nejde. Fallback = hustší tónovaný materiál + scrim; rozostřit jde jen vlastní vrstva Home (`RenderEffect`, API 31+, minSdk 26). Nikdy nevydávej fallback za blur. Home dnes kreslí rozložení napevno v `ui/HomeRoot.kt` (`Home()`, enum `HomeLayout` Pure/Essentials/Custom, `favorites`, LENS přes `LensArc` v `ui/Lens.kt`); nastavení je jeden dokument `data/LauncherSettings.kt` (`schema = 1`, validace jen v `sanitized()`); tokeny Crystal v `ui/Crystal.kt`. Long-press prázdné Home dnes otevírá Studio.

Úpravy majitele oproti prototypu:
- Panely nastavení mnohem klidnější a matnější. Tři úrovně: **Veil Light** (malé ovládací prvky), **Veil Dense** (kontextové editory, krytí ~75–85 %), **Veil Focus** (Studio, ~88–95 %). Ostrý text, žádné neonové hrany ani agresivní gradienty; světelná hrana od původu zůstává, ale jemná. Funguje na světlé, tmavé i barevné tapetě.
- Kontextové editory s progressive disclosure: první úroveň jen nejčastější volby, ostatní pod „Pokročilé“. Inspektor nesmí zakrýt upravovaný prvek (hodiny i ikona musí být vidět se změnou živě).
- Horní lišta: vlevo předlohy + zpět/reset, vpravo Studio + ✓; nesmí překážet hodinám ani sobě. Předlohy jako vizuální miniatury s náhledem před potvrzením; vlastní rozložení se nesmí smazat bez varování.
- Hitbox LENS vs. vnitřní Essentials: 84 × 100 dp z prototypu nepřebírej bez testu na telefonu; ověř tap, hold, swipe up, ARC.
- Nedělat teď: redesign ARC, PULSE 2.0, přepis FLOW, nesouvisející systémové integrace, velké frameworky. Widgety jen rezervované v modelu, ne „hotové“.

Git: větev `claude/crystal-redesign`, na začátku `git fetch` a `git status`. Malé commity + push, žádný merge do `main`, nic destruktivního. Data majitele (Essentials, hodiny Crystal, FLOW, ARC) se nesmí ztratit.

**Tato session = části 1–3** (každá: testy → build → telefon → commit):
1. `VeilTokens` + modifikátor `veilSurface(level, origin)` pro Light/Dense/Focus (světlá/tmavá varianta, jemná hrana od původu). Použít na Studio (Focus) a stávající sheety (Dense). Screenshot na tmavé tapetě; světlou a barevnou jen se souhlasem majitele (tapetu neměnit bez souhlasu).
2. Datový model rozložení podle §5.4 (předlohy Pure/Essentials/Balanced jako šablony, vlastní rozložení, Grid/Free/Hybrid, kotvy) + migrace schema 1 → 2 v `sanitized()` s JVM testy (starý, prázdný, poškozený dokument, round trip). Home se kreslí z modelu beze změny vzhledu. Čistý Kotlin `HomeEditorState` (tažení, přichycení, vodítka, kolize/uhnutí, řada LENS, undo/redo ≤ 50) s testy.
3. Edit mode: podržení plochy / ikony → editace (podržení ikony rovnou táhne), LENS v editoru nespouští ARC, horní lišta, přetahování s mřížkou, ✓ uloží atomicky, Zpět s neuloženými změnami se zeptá. Studio zůstává dostupné přes ozubené kolečko.

Příště (nedělat teď): 4. kontextové editory + předlohy s miniaturami, 5. LENS → Search s reálným IME, 6. polish, TalkBack, výkon (`gfxinfo`, PSS).

Build: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`, instalace `scripts/dev.sh` nebo adb install. Telefon: Nothing Phone (3), Android 16, 1080×2392, 420 dpi (411 × 911 dp). adb `C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe` (Git Bash: `MSYS_NO_PATHCONV=1`), JDK `C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`. Před každým `input` ověř `topResumedActivity`; když telefon používá majitel, počkej a nic neklikej. Lab (`dev.intelia.launcher.lab`) je výchozí HOME. MT Capsule, tapetu a jiné aplikace neměnit bez souhlasu.

Na konci: `docs/STATUS.md` jen ověřená fakta, §5 v `docs/DESIGN_SYSTEM.md` doplnit o Light/Dense/Focus, nový prompt v `docs/NEXT_SESSION.md` pro části 4–6 (se stejným limitem 250k), krátký report: co je hotové, screenshoty před/po, testy, omezení, commit.

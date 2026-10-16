# Prompt pro další session — Redesign, fáze B3 (ARC: gesto, sady, Action engine)

Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

---

Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky. Rozpočet 200k tokenů, cíl ~120k, žádní zbyteční agenti, soubory nečti opakovaně celé.

Zdroj pravdy: `docs/REDESIGN_BRIEF.md` a `docs/DESIGN_SYSTEM.md` (hlavně 4.3–4.6, 4.8). Rozhodnutí D10/D11 v `docs/DECISIONS.md`. Prototyp `docs/design/crystal-prototype.html` (scény „LENS aktivní“, „ARC výběr“, „ARC přepnutí sady“).

Stav po B2 (viz `docs/STATUS.md`): `InteliaTheme` poskytuje `CrystalTheme` (`LocalCrystal`), Home má Crystal hodiny (`ClockStyle.Crystal`, výchozí pro nové instalace; Ink = Serif zůstává volbou), datum Roboto Flex 12 sp caps, `LocalHomeInk` = Crystal `fg` (světlý inkoust na světlé tapetě), stín 1 dp. LENS je kruh 44 dp bez podtržítka (`LensMark` v `ui/Kit.kt`), dotyková plocha 100 dp, Essentials ±62/±108 dp od LENS. Hold zatím otevírá STARÉ ARC v `ui/Lens.kt` (`LensArc`, `ArcRings`, prstence). `ui/CrystalArc` (jen vykreslení) a `ui/CrystalArcGeometry.kt` existují, nenapojené; `action/Action.kt` (Action engine) nenapojený. Telefon má 420 dpi = 411 dp šířka.

Git: větev `claude/crystal-redesign`, na začátku `git fetch` a ověř stav. Nemergovat do `main`, nic destruktivního.

Dělej jen B3 — ARC podle `DESIGN_SYSTEM.md` 4.5/4.6:
1. Gesto: hold 260 ms → jeden nízký oblouk s 5 místy (`CrystalArc` + `CrystalArcGeometry`), neutrální zóna 40 dp, výběr 70–200 dp a ±15°, vybrané místo 120 %, název nad obloukem (`CrystalType.arcName`); haptika `LONG_PRESS` / `CLOCK_TICK` / `CONFIRM` / `REJECT`; Reduced Motion = 0 ms. Čistý stavový automat s JVM testy (navázat na `ArcGesture`).
2. Sady na koncích oblouku: dojetí na konec přepne sadu (`SEGMENT_TICK`, 0 ms výměna obsahu).
3. Migrace v `sanitized()` (schema 1 → 2): `arc`/`arcFlows`/`arcShortcuts` → sady akcí, bez ztráty dat, s testy (starý dokument, prázdný, poškozený, round trip).
4. Napojit sloty na Action engine (`Action.run` → `Done | Opened | Unavailable(reason)`); nedostupná akce = `REJECT` + důvod, nic se nespustí.
5. Alternativa bez gesta (TalkBack/klávesnice) zůstává: seznam akcí jako tlačítka.
6. Studio: zatím jen nutné minimum, aby šlo sady naplnit (plné Studio až B5).
7. `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`, instalace, na telefonu: hold → oblouk, výběr, puštění spustí, návrat do neutrální zóny zruší, přepnutí sady, FLOW se spustí jen puštěním; screenshot.

Telefon: Nothing Phone (3), Android 16, 1080×2392, 420 dpi. adb `C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe` (v Git Bash `MSYS_NO_PATHCONV=1`), JDK `C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`. Před každým `input` ověř `topResumedActivity` — majitel telefon občas používá; v tom případě počkej a nic neklikej. Lab (`dev.intelia.launcher.lab`) je výchozí HOME. MT Capsule nic neměnit. Tapetu neměnit bez souhlasu (světlá tapeta z B2 zatím neověřena na telefonu).

Na konci: commit a push do `claude/crystal-redesign`, `docs/STATUS.md` jen s ověřenými fakty, nový prompt v `docs/NEXT_SESSION.md` (B4 PULSE 2.0).

# Prompt pro další session — Redesign, fáze B2 (Home + LENS)

Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

---

Pokračujeme na **Intelia Launcheru** (`AGENTS.md` platí). **Odpovídej česky.** Rozpočet 200k tokenů, cíl ~120k, žádní zbyteční agenti, soubory nečti opakovaně celé.

Zdroj pravdy: `docs/REDESIGN_BRIEF.md` (zadání) a `docs/DESIGN_SYSTEM.md` (schválená specifikace, hlavně 4.1–4.3, 4.6, 4.8). Rozhodnutí D10 a D11 (Roboto Flex) v `docs/DECISIONS.md`. Prototyp: `docs/design/crystal-prototype.html` (https://claude.ai/artifact/BPhBEVCpmzktdFPXDGDEta).

Stav po B1 (viz `docs/STATUS.md`): `ui/Crystal.kt` má tokeny (`CrystalPalette`, `CrystalType` na Roboto Flex přes osy wght/wdth, `CrystalDims`), `CrystalTheme`, `Modifier.crystalMat`, `CrystalArc` (jen vykreslení), `StudioRow`, `LiveCapsule`; `ui/CrystalArcGeometry.kt` je čistá geometrie oblouku. `action/Action.kt` je Action engine (sealed `Action`, `unavailableReason`/`isAvailable`/`run` → `Done | Opened | Unavailable(reason)`, rozhraní `ActionHost`), zatím nenapojený. Nastavení má `schema = 1` a akcenty `Frost`/`Silver`; nové instalace dostanou Mráz, stávající zůstávají na Terakotě. Nic z toho zatím není na Home vidět.

Git: větev `claude/crystal-redesign`, na začátku `git fetch` a ověř stav. Nemergovat do `main`, nic destruktivního.

Dělej **jen B2 — Home + LENS** podle `DESIGN_SYSTEM.md`:
1. `CrystalTheme` napojit do `InteliaTheme` (akcent ze settings přes `accentColor`), Home přepnout na Crystal typografii: hodiny 84 sp 220·118 tabular, datum 12 sp caps. `clockStyle` Serif zůstává jako volba (Ink); výchozí pro nové instalace Crystal — rozhodnout, zda nový `ClockStyle` + migrace v `sanitized()` s testy.
2. Čitelnost na světlé tapetě: hodiny/datum `fg` light, když `HINT_SUPPORTS_DARK_TEXT` (už existuje `rememberWallpaperWantsDarkText`); stín jen 0–1 dp.
3. LENS: kruh 44 dp, tah 1.5 dp, bez podtržítka (`TideDash` z Home pryč), střed = spodní inset + 52 dp, dotyková plocha 100 dp; hold zatím otevírá stávající ARC (nové gesto až B3). Tap = Search, swipe up = drawer beze změny.
4. Essentials: 4 ikony 40 dp ve dvou párech x = 72/118 a 242/288 dp na baseline LENS, bez podložek; Pure a Custom zachovat.
5. Žádná klidová animace; Reduced Motion respektovat.
6. `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`, instalace, na telefonu: Home tmavá i světlá tapeta, LENS tap/swipe/hold, Essentials, Recents → Home; screenshot před/po.

Telefon: Nothing Phone (3), Android 16, 1080×2392. adb `C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe` (v Git Bash `MSYS_NO_PATHCONV=1`), JDK `C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`. Před každým `input` ověř `topResumedActivity` — majitel telefon občas používá (Instagram, Fotky atd.), v tom případě počkej a nic neklikej. Lab (`dev.intelia.launcher.lab`) je výchozí HOME. MT Capsule je nainstalovaná, ale má vypnutý portrait profil; nic v ní neměnit.

Na konci: commit a push do `claude/crystal-redesign`, `docs/STATUS.md` jen s ověřenými fakty, nový prompt v `docs/NEXT_SESSION.md` (B3 ARC: gesto, sady na koncích oblouku, migrace `arc`/`arcFlows`/`arcShortcuts` → sady, napojení na Action engine).

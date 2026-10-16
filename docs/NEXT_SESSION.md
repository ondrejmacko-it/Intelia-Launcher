# Prompt pro další session — Redesign, fáze B1 (fáze A schválena)

Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

---

Pokračujeme na **Intelia Launcheru** (`AGENTS.md` platí). **Odpovídej česky.** Rozpočet 200k tokenů, cíl ~120k, žádní zbyteční agenti, soubory nečti opakovaně celé.

Zdroj pravdy: `docs/REDESIGN_BRIEF.md` (zadání) a `docs/DESIGN_SYSTEM.md` (schválená specifikace). Rozhodnutí D10 v `docs/DECISIONS.md`. Prototyp: `docs/design/crystal-prototype.html` (https://claude.ai/artifact/BPhBEVCpmzktdFPXDGDEta).

Majitel 2026-10-16 **schválil** směr A Quiet Crystal včetně přepínání sad konci oblouku a PULSE 2.0 (D10). Začni rovnou B1.

Git: větev `claude/crystal-redesign`, na začátku `git fetch` a ověř stav. Nemergovat do `main`, nic destruktivního.

Dělej **jen B1 — design systém a komponenty**:
1. Licence Roboto Flex (OFL) zapsat do `docs/DECISIONS.md`, font přidat do `res/font`.
2. Tokeny z `DESIGN_SYSTEM.md` 4.1–4.3 jako Compose theme (barvy dark/light, typografie, rozměry), akcent jako volba v nastavení s migrací (`Terracotta` zůstává pro stávající uživatele) — migrace v `LauncherSettings.sanitized()` + JVM testy.
3. Komponenty: materiál `mat` (bez blur), oblouk + slot + koncový přepínač sady (jen vykreslení, gesto až B3), Studio řádek, živá kapsle (jen tvar).
4. Pure-Kotlin `Action` engine (sealed type + `isAvailable`/`run` výsledek) s testy, zatím nenapojený na UI.
5. `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`, instalace na telefon, screenshot před/po na Home (beze změny chování).

Telefon: Nothing Phone (3), Android 16, 1080×2392. adb `C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe` (v Git Bash `MSYS_NO_PATHCONV=1`), JDK `C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`. Před každým `input` ověř `topResumedActivity` — majitel telefon občas používá (Instagram atd.), v tom případě počkej a nic neklikej. Lab (`dev.intelia.launcher.lab`) je výchozí HOME. MT Capsule je nainstalovaná, ale má vypnutý portrait profil; nic v ní neměnit.

Na konci: commit a push do `claude/crystal-redesign`, `docs/STATUS.md` jen s ověřenými fakty, nový prompt v `docs/NEXT_SESSION.md` (B2 Home + LENS).

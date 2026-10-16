# Prompt pro další session — Intelia Veil + Home Studio V2, etapa B (implementace)

Použij až po tom, co majitel schválí D12 (návrh Intelia Veil). Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

---

Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky. Rozpočet 200k tokenů, cíl ~120k, žádní zbyteční agenti, soubory nečti opakovaně celé.

Zdroj pravdy: `docs/DESIGN_SYSTEM.md` §5 (Intelia Veil + Home Studio V2) a D12 v `docs/DECISIONS.md` (schváleno majitelem; případné úpravy majitele jsou zapsané u D12). Prototyp `docs/design/veil-prototype.html` (scény 02–10). Sekce 1–4 (Crystal, ARC, PULSE 2.0) dál platí, pokud §5 neříká jinak.

Stav: Crystal B1 + B2 hotové (`docs/STATUS.md`). B3 (nové ARC) ještě neproběhlo; ARC zatím staré v `ui/Lens.kt`. Telefon Nothing Phone (3) nemá blur za oknem (`supports_background_blur=0`) a tapeta je živá Atmosphere, takže Frosted = blur jen vlastní vrstvy Intelie (API 31+).

Git: větev `claude/crystal-redesign`, na začátku `git fetch` a ověř stav. Nemergovat do `main`, nic destruktivního.

Dělej kroky 1–3 z §5.11, každý jako malý commit s vlastní kontrolou:
1. `VeilTokens` + modifikátor `veilSurface(origin)` (výplň, světelná hrana od původu, vnitřní záře), úrovně Clear/Veil/Frosted/Solid, detekce `isCrossWindowBlurEnabled` s živým fallbackem; Studio → Vzhled ukazuje pravdu. Zatím použít jen na hledání a Studio.
2. Datový model `HomeLayouts` (§5.4), předlohy Pure/Essentials/Balanced jako šablony, migrace schema 1 → 2 v `sanitized()` s testy (starý, prázdný, poškozený dokument, round trip). Home se kreslí z modelu beze změny vzhledu. Dotyková zóna LENS 84 × 100 dp.
3. `HomeEditorState` jako čistý Kotlin (resolve tažení, mřížka/volně/hybrid, vodítka, kolize a uhnutí, řada LENS, undo/redo ≤ 50) s JVM testy.
Pak `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`, instalace, na telefonu: Home vypadá stejně, data majitele (Essentials, hodiny Crystal, FLOW, ARC) zůstala, LENS tap/hold/swipe fungují, vnitřní ikony Essentials jdou klepnout u LENS.

Telefon: Nothing Phone (3), Android 16, 1080×2392, 420 dpi. adb `C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe` (v Git Bash `MSYS_NO_PATHCONV=1`), JDK `C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`. Před každým `input` ověř `topResumedActivity`; když telefon majitel používá, počkej a nic neklikej. Lab (`dev.intelia.launcher.lab`) je výchozí HOME. MT Capsule ani tapetu neměnit bez souhlasu.

Na konci: commit a push do `claude/crystal-redesign`, `docs/STATUS.md` jen s ověřenými fakty, nový prompt v `docs/NEXT_SESSION.md` (kroky 4–7: edit mode UI, inspektory, předlohy, LENS → Search).

Pokračujeme na Intelia Launcheru — **M2 krok 3: haptika po plochách + dokončení Studio UX** (`AGENTS.md` platí;
česky, stručně, ~200k, žádní agenti).

Stav (2026-10-18): větev `feat/control-system-v1` (nemergovat do `main`). M2 krok 2 hotový — viz `docs/STATUS.md`
nahoře a `DECISIONS.md` D24 (síla haptiky, VIBRATE + fallback na 4 stupně) a D25 (vlastní globální akcent).
Klíčové místo v kódu: `data/HapticPrefs.kt` (`Haptic.plan`, `HapticPrefs.per` připravené pro ARC/PULSE/gesta/tlačítka),
`ui/HapticEngine.kt`, `data/CustomColors.kt`, `AccentColors.derive`, `mainAccent`/`placeAccent` v `ui/Ink.kt`.

1. Haptika po plochách (majitel chtěl „později individuální nastavení“): v Animace a haptika pod globální silou
   rozbalovací „Podle místa“ — ARC, PULSE, Gesta, Tlačítka; každé „Jako celek“ (null) nebo vlastní síla/stupeň.
   Pure: `HapticPrefs.with(surface, level?)` + testy. Gesta: Home gesta v `HomeRoot.kt` volají
   `haptic.performHapticFeedback` přímo — převést na `rememberInkHaptics(HapticSurface.Gestures)`.
   Zvaž sloučení přepínačů „Ťuknutí na každém slotu“ (ARC a Global ARC) se silou ARC — nejdřív návrh do DECISIONS.
2. D25 další krok jen jako návrh (bez kódu): vlastní barva pro místo palety (`CustomColors.places`), pak Design Lab.
3. Testy na telefonu: Phone (3) nemá řízení amplitudy a má vypnutou systémovou hmatovou odezvu — systémové
   nastavení neměň; plynulý slider tedy na zařízení ověřit nejde, napiš to.
4. Lab build, ověření (top activity před každým vstupem, scroll x ≈ 20), commit + push, STATUS, nový NEXT_SESSION.

Build/telefon: JAVA_HOME `C:/Users/Ondra/AppData/Local/lesnik-toolchain/jdk-17.0.20.1+1`,
`./gradlew -q --max-workers=1 -Dorg.gradle.jvmargs=-Xmx1536m :app:testDebugUnitTest :app:assembleLab` (ověř čas APK;
chyba paměti → `gradlew --stop`; zamčený `classes.jar` = zbloudilý „Gradle Test Executor“ java proces → ukončit).
APK `app/build/outputs/apk/lab/app-lab.apk`, `install -r`, NIKDY uninstall. adb v `lesnik-toolchain/sdk/platform-tools`,
`MSYS_NO_PATHCONV=1`. Navigace: Home dlouhý stisk 540 1100 → Nastavení 907 2240; Vzhled (250, 1590), Ovládání
(250, 1885) → Animace a haptika (375, 880). Když nahoře není Lab (např. Recents), žádný vstup a žádné screenshoty
(majitel telefon používá). Majitelovy vazby, FLOW, ikony, akcent (teď Coral), haptiku (Jemná) a rozložení neměnit;
test vlastní barvy vždy ukončit „Zpět na předvolbu“. Hudbu nespouštět.

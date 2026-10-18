Pokračujeme na Intelia Launcheru — **M2 Studio UX, krok 2** (`AGENTS.md` platí; česky, stručně, ~200k, žádní agenti).

Stav (2026-10-18): větev `feat/control-system-v1` (nemergovat do `main`). M2 krok 1 hotový — viz `docs/STATUS.md`
nahoře: počty v popiscích skupin Gesta a tlačítka, dlaždice Motivu, náhled Global ARC na záložce ARC, pruh Search
podle změřené karty.

1. Zeptej se majitele na **D24** (haptický slider: A = 4 stupně systémové haptiky bez oprávnění, doporučeno;
   B = skutečná síla přes `VIBRATE`) a **D25** (volná barva: nejdřív jedna vlastní barva pro celý launcher, nebo
   hned po místech). Bez odpovědi nic z toho nestav.
2. Po schválení: pure logika (mapování stupňů / validace barvy a kontrastu v `sanitized()`) s JVM testy, pak UI.
3. Na telefonu ověř i to, co zůstalo: build s `SEARCH_HEAD` 48 dp (štítek „Ukázka“ nad kartou Search), náhled Global
   ARC při vypnutém PULSE.
4. Lab build, ověření na Phone (3) (top activity před každým vstupem, scroll x ≈ 20), commit + push, STATUS.

Build/telefon: JAVA_HOME `C:/Users/Ondra/AppData/Local/lesnik-toolchain/jdk-17.0.20.1+1`,
`./gradlew -q --max-workers=1 -Dorg.gradle.jvmargs=-Xmx1536m :app:testDebugUnitTest :app:assembleLab` (ověř čas APK;
při chybě paměti `gradlew --stop`; zamčený `classes.jar` = zbloudilý „Gradle Test Executor“ java proces → ukončit).
APK `app/build/outputs/apk/lab/app-lab.apk`, `install -r`, NIKDY uninstall. adb v `lesnik-toolchain/sdk/platform-tools`,
`MSYS_NO_PATHCONV=1`. Navigace: Home dlouhý stisk 540 1100 → Nastavení 907 2240. Po instalaci telefon někdy ukáže
systémové Recents — pak žádný vstup (majitel může telefon používat). Majitelovy vazby, FLOW, ikony, motiv a rozložení
neměnit, hudbu nespouštět.

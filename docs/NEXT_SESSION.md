Pokračujeme na Intelia Launcheru — **PULSE 2.0** (`AGENTS.md` platí). Odpovídej česky, stručně. Limit
~200–250k tokenů: žádní agenti, soubory nečti celé, výstupy filtruj, screenshoty zmenšuj (PIL, výřez).

Stav (2026-10-18): větev `feat/control-system-v1` (pushnutá, nemergovat do `main`). Hotovo M0–M5 Control System V1
a **M3.1 PULSE Foundation** (`6091d36`, viz `docs/STATUS.md` nahoře): `pulse/PulseGestures.kt` jeden rozpoznávač
(Home `ui/PulseTouch.kt` + služba), `Gesture.DoubleTapHold`, režim Vyp / Jen na ploše / Všude (`pulseEverywhere`,
`pulseMode()`), služba bez terče na zamčené obrazovce.

Koncept PULSE 2.0 (platí): Hidden = nic nevykresleno, jen malý terč; Compact = malá kapsle jen při události;
Expanded = kompaktní panel z kamery; Arc = Global ARC (ARC V2 geometrie, vlastní sady). Veil Trace = tenká zakřivená
linka v akcentu, jen při otevření, volitelná. Jedno Studio: náhled → krátké základy → přehled akcí → Pokročilé.

Úkol této session:
0. Na začátku ověř na telefonu (majitel ho odemkne): po odemknutí se terč PULSE vrátí (`dumpsys window windows |
   grep -c "Intelia PULSE"` ≥ 1), dvojí klepnutí + podržení s testovací vazbou (pak vrátit).
1. **M3.2 Music PULSE**: `NotificationListenerService` jen pro `MediaSessionManager.getActiveSessions` (nové
   oprávnění = nejdřív se zeptej majitele, zapisuje se do DECISIONS), `MediaController.Callback` (žádný polling).
   Compact: vlevo sloupce (jen při PLAYING, statické při Reduced Motion), vpravo kulatý artwork / ikona aplikace.
   Expanded: artwork, název, interpret, ⏮ ⏯ ⏭, časová osa + seek jen když `ACTION_SEEK_TO`; ovladače bez podpory
   akce nezobrazovat. Napřed Home, pak služba. Test: Spotify / YouTube Music, commit + push.
2. Pak M3.3 Global ARC (podržení kamery → ARC V2 pod kamerou, release/cancel, Search jako akce; aplikace mimo Home
   spouštět přes `LauncherApps`, FLOW s náhledem).

Známá fakta: tah dolů z kamery nad aplikacemi bere systémová lišta (nelze); tah nahoru z kamery nedává smysl;
svítilnu ověřuj `logcat -d | grep FlashlightController` (`dumpsys media.camera` ji po ~10 s zhasne).

Build/telefon: JAVA_HOME `C:/Users/Ondra/AppData/Local/lesnik-toolchain/jdk-17.0.20.1+1`,
`./gradlew -q -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleLab`; pro `run-as`/logcat
`assembleDebug` (stejný balíček, `install -r`, NIKDY uninstall). adb v `lesnik-toolchain/sdk/platform-tools`,
`MSYS_NO_PATHCONV=1`. Souřadnice: kamera 540 70 (služba), Home kapsle 540 150 (režim Jen na ploše), LENS 540 2190,
Studio = dlouhý stisk 540 1000 → Nastavení 907 2256 → hledat „pulse“ (pole 540 1440, výsledek 400 1615).
Majitelovy vazby (PULSE Podržení → Svítilna, Dvojí klepnutí → Spotify) neměnit. `docs/screens/*.rar` nemazat ani
necommitovat.

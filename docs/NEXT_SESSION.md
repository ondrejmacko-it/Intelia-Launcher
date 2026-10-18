Pokračujeme na Intelia Launcheru — **M0: stabilizace Studia (scroll, nechtěné zavírání, přechody)** (`AGENTS.md` platí).
Odpovídej česky, stručně. Limit ~200k tokenů: žádní agenti, soubory nečti celé, výstupy filtruj, screenshoty zmenšuj.

Stav (2026-10-18): větev `feat/control-system-v1` (nemergovat do `main`). S4.4 hotová a pushnutá — viz `docs/STATUS.md`
nahoře (LENS ustupuje sousedům, Home mimo a11y strom pod Search/Drawerem, PULSE quick 2 × 2 při velkém písmu).

Schválený velký plán (milníky, každý = funkční celek + testy + Lab build + ověření na Phone (3) + commit):
M0 Studio stabilizace → M1 PreviewHost (kontextové živé náhledy) → M2 jednotné Studio UX → M3 DeviceCapabilities →
M4 Shizuku (volitelné) → M5 hardwarová tlačítka → M6 knihovna akcí + FLOW → M7 Onboarding V2 → M8 integrace.
Inspirace KEA jen organizačně, žádný cizí kód ani grafika. Screenshoty `docs/screens/*` (necommitovat).

M0 — úkoly:
1. Zreprodukuj na telefonu: rychlý scroll dlouhého nastavení (Studio › Ovládání › PULSE) mění výšku sheetu až po zavření.
   Příčina skoro jistě `ui/StudioSheet.kt` `NestedScrollConnection.onPostScroll` (nevyužitý scroll seznamu hýbe sheetem).
2. Oprav: scroll seznamu nikdy nemění výšku; výšku mění jen tah za madlo (touch slop, velocity threshold, snap na
   Preview / Balanced / Settings); zavření jen Back nebo jednoznačné gesto na madle; fling seznamu nic nezavře.
   Zachovej polohu při otevření podstránky, Reduced Motion, TalkBack akce na madle.
3. Přechody: otevření Studia z Home, změna polohy a podstránky bez bliknutí a skoku; nic dražšího než transformace.
4. Testy: pure stav sheetu (snap, prahy) na JVM; na telefonu 10× rychlý scroll nahoru/dolů bez změny výšky (porovnej
   bounds madla v dumpu před/po), tah za madlo mezi třemi polohami, Back. Scrolluj jen u levého okraje (x ≈ 20).
5. Commit + push, STATUS, pak (zbude-li rozpočet) M1: návrh `PreviewHost` nad stávajícími renderery (PULSE, ARC, LENS,
   Search, Home) — bezpečné náhledy, nic nespouští.

Build/telefon: JAVA_HOME `C:/Users/Ondra/AppData/Local/lesnik-toolchain/jdk-17.0.20.1+1`,
`./gradlew -q --max-workers=2 -Dorg.gradle.jvmargs=-Xmx1536m :app:testDebugUnitTest :app:assembleLab` (ověř čas APK — tichý pád daemonu
nechá staré APK); APK `app/build/outputs/apk/lab/app-lab.apk`, `install -r`, NIKDY uninstall. adb
v `lesnik-toolchain/sdk/platform-tools`, `MSYS_NO_PATHCONV=1`. Obrazovka 1080×2392; kamera ≈ (540, 63) px; LENS (540, 2190).
Navigace: Home dlouhý stisk 540 1100 → Nastavení 907 2240.
Před každým `input` ověř, že top activity je `dev.intelia.launcher.lab` (majitel může telefon používat).
**Souřadnice z `uiautomator dump` bývají zastaralé** — klepej podle čerstvého screenshotu, nikdy na odvozenou souřadnici,
když hledání textu selže. Systémové písmo/density měň jen dočasně a vrať (1.0 / `wm density reset`).
Majitelovy vazby (PULSE Podržení → Global ARC, Dvojí klepnutí → Spotify, Klepnutí a podržení → Svítilna), FLOW, ikony
a rozložení neměnit, hudbu nespouštět. Mluvený TalkBack test s majitelem zatím neproběhl.

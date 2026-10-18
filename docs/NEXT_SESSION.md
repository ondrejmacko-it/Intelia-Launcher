Pokračujeme na Intelia Launcheru — **UX stabilizace & Studio V4** (`AGENTS.md` platí). Odpovídej česky, stručně.
Limit ~200k tokenů: žádní agenti, soubory nečti celé, výstupy filtruj, screenshoty zmenšuj (PIL, kontaktní listy).

Stav (2026-10-18): větev `feat/control-system-v1` (pushnutá, nemergovat do `main`). Původní kroky 5–7 (Veil 3.0)
jsou **pozastavené**. Hotovo S0 + S1 (viz `docs/STATUS.md` nahoře): `ui/StudioNav.kt` (`StudioStack` zpětný stack,
`SheetStops`), `ui/StudioSheet.kt` (jeden tažitelný Veil Focus sheet Preview / Balanced / Settings, sdílená poloha,
zavření tahem madla → původ), `Studio.kt` už nemá pevné `up()` ani `PREVIEW_SHARE`.

Zadání majitele (zkráceně, platí celé): PULSE neviditelný v klidu, užitečný při použití; věrné náhledy; ikony a popisky
samostatně; přehledné podstránky; ARC jen polish Studia. Pořadí milníků:

1. **S2 — Náhledy a Home prvky**: reprodukuj na telefonu chyby při zvětšení ikon a zapnutí popisků (grid/free/hybrid,
   text 100/130/150 %, dlouhé české názvy), najdi přesnou příčinu. Sjednoť `HomePreview` s Home (společný layout
   model / renderer, stejné měřítko, insets, pozice kamery a LENS). Ikona (velikost, vzhled, tvar, barva, průhlednost,
   pozice) a popisek (zap/vyp, velikost, font/váha, barva, zarovnání, rozestup, max. řádky) jako globální výchozí +
   výjimka v `HomeItem`; migrace přes `LauncherSettings.sanitized()`. Testy rozložení a ukládání.
2. **S3 — PULSE UX**: hitbox ~48 dp oddělený od vizuální kapsle, kamery a systémových indikátorů (`lensTarget()`,
   kalibrace, WindowManager overlay), kalibrace + test v nastavení; místo černého dashboardu minimalistický kontextový
   panel (hudba / relevantní akce, nenápadné zavření); věrné náhledy Hidden / Music / Expanded / Global ARC.
   Známá chyba: text „Návrat ke kameře gesto zruší“ u Global ARC se kreslí přes hodiny.
3. **S4 — Polish**: podstránky (Základní / Přizpůsobení / Gesta a akce / Pokročilé), ARC Studio (náhled geometrie,
   sady a sloty, Action Picker), barvy/typografie/haptika.

Každý milník: testy, Lab build, `adb install -r`, ověření na telefonu, malý commit + push, `docs/STATUS.md`.

Build/telefon: JAVA_HOME `C:/Users/Ondra/AppData/Local/lesnik-toolchain/jdk-17.0.20.1+1`,
`./gradlew -q -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleLab` (při pádu daemonu kvůli paměti
`./gradlew --stop` a znovu); APK `app/build/outputs/apk/lab/app-lab.apk`, `install -r`, NIKDY uninstall. adb
v `lesnik-toolchain/sdk/platform-tools`, `MSYS_NO_PATHCONV=1`. Telefon bývá zamčený — požádej majitele o odemčení.
Navigace: Home dlouhý stisk 540 1000 → editor → Nastavení 907 2256; texty a souřadnice prvků přes
`uiautomator dump` (hledat `text="…"` a `bounds`). Majitelovy vazby (PULSE Podržení → Svítilna, Dvojí klepnutí →
Spotify) neměnit. `docs/screens/*.rar` nemazat ani necommitovat. Screenshoty majitele: `~/Downloads/Screenshot_20261018-16*.png`.

Pokračujeme na Intelia Launcheru — **S4 Unified UI Polish** (`AGENTS.md` platí). Odpovídej česky, stručně.
Limit ~200k tokenů: žádní agenti, soubory nečti celé, výstupy filtruj, screenshoty zmenšuj (PIL, výřezy).

Stav (2026-10-18): větev `feat/control-system-v1` (pushnutá, nemergovat do `main`). Hotovo S0–S3 a **S3.1**
(viz `docs/STATUS.md` nahoře): Quick Actions v `Controls.quick` + `ui/PulseQuick.kt`, `Action.Alarms`, glance
materiál `veilGlass` + Veil Trace v `ui/PulseGlance.kt`, adaptivní náhledy a 6 skupin v `ui/PulseStudio.kt`,
kalibrace na VeilKit v `ui/Calibration.kt`, nápověda Global ARC jen po 1,4 s nečinnosti.

Zbývá z S3.1 (ověřit na telefonu, nic majiteli nespouštět bez souhlasu): tlačítka glance (svítilna / budíky /
oznámení), nečinná nápověda ARC (držet ≥ 2 s), Global ARC nad aplikací, světlý motiv, text 130 %, TalkBack,
plynulost otevření/zavření, režim Jen na ploše. Exit animace ve službě: okno se odebírá po animaci karty (ověřit).

Zadání majitele (platí celé, viz jeho zpráva k S3 review): jeden sjednocený design napříč Intelií — čistý, matný,
čitelný „glass“ (inspirace: jemně tónovaná karta s hairline okrajem a světlem shora, text pod ní neruší), vlastní
identita, nic nepředělávat od nuly. S4:
1. Projít všechny podstránky Studia; odstranit duplicity, zbytečné instrukce, nepřehledné seznamy.
2. Stránky s málem obsahu lépe rozvrhnout; sjednotit náhledy, slidery (`SliderRow`), přepínače, barvy, spacing, navigaci.
3. Najít zbytky starého designu (Crystal/`LocalCrystal`, staré karty, `Kicker`/`Capsule` obrazovky) a převést na VeilKit
   (`VeilGroup`, `SettingRow`, `SwitchRow`, `ChoiceSetting`, `veilGlass` pro plovoucí karty).
4. ARC Studio dokončit bez změny fungujícího ARC V2; Action Picker a sheety na stejný materiál.
5. Konzistentní akcent v Search, App Drawer a FLOW (jemně, ne jen přepínače); hodiny/Home beze změny rozvržení.
6. Regresní testy, screenshoty, malé commity + push, `docs/STATUS.md`.

Build/telefon: JAVA_HOME `C:/Users/Ondra/AppData/Local/lesnik-toolchain/jdk-17.0.20.1+1`,
`./gradlew -q -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleLab` (ověř čas APK — tichý pád daemonu
nechá staré APK; pak spusť znovu); APK `app/build/outputs/apk/lab/app-lab.apk`, `install -r`, NIKDY uninstall. adb
v `lesnik-toolchain/sdk/platform-tools`, `MSYS_NO_PATHCONV=1`. Obrazovka 1080×2392; kamera ≈ (540, 63) px.
Navigace: Home dlouhý stisk 540 1100 → Nastavení 907 2240 → rychlý přístup PULSE 476 2386.
**Souřadnice z `uiautomator dump` bývají zastaralé** (po animaci/scrollu) — klepej podle čerstvého screenshotu a nikdy
neklepej na odvozenou souřadnici, když hledání textu selže (minule to otevřelo panel oznámení).
Majitelovy vazby (PULSE Podržení → Global ARC, Dvojí klepnutí → Spotify, Klepnutí a podržení → Svítilna) neměnit,
hudbu nespouštět ani nezastavovat. `docs/screens/*` nemazat ani necommitovat.

Pokračujeme na Intelia Launcheru — **S4 část 3: dokončení ověření a zbytků** (`AGENTS.md` platí). Odpovídej česky, stručně.
Limit ~200k tokenů: žádní agenti, soubory nečti celé, výstupy filtruj, screenshoty zmenšuj (PIL, výřezy).

Stav (2026-10-18): větev `feat/control-system-v1` (nemergovat do `main`). S4 část 2 hotová — viz `docs/STATUS.md`
nahoře: mrtvé UI odebráno, Onboarding a app sheet na Veil, titulky na Roboto Flex, kratší nápovědy, bloom kreslený jen
v rohu (Drawer 18–20 % janky, baseline 17 %). Screenshoty `docs/screens/s4.2/` (necommitovat).

Zbývá:
1. S toho, co nejde bez majitele: text 130/150 % a TalkBack (systémová nastavení — požádej majitele, ať je zapne, pak
   projdi Studio, Search, Drawer, PULSE kartu: ořezy, dotykové plochy ≥ 48 dp, popisky).
2. Reduce motion (Intelia › Animace) a světlá tapeta — screenshot Home, Search, Drawer, Studio.
3. PULSE „Jen na ploše“ jen se souhlasem majitele (přepnutí může vyžadovat znovu zapnout službu přístupnosti),
   tlačítka glance (svítilna/budíky/oznámení — nespouštět bez souhlasu), nečinná nápověda Global ARC (držet ≥ 2 s).
4. Studio první otevření: 18 % janky, p90 53 ms — změř 5×, zkus odložit náhled plochy o snímek, ověř zlepšení.
5. Volitelně: FLOW stránky a kalibrace na plném papíře vs. Studio s tapetou — sjednotit, jen pokud to majitel chce.
6. Action Picker na telefonu (screenshot), editor/inspector čtou `LocalCrystal` — přepis jen bez změny vzhledu.

Build/telefon: JAVA_HOME `C:/Users/Ondra/AppData/Local/lesnik-toolchain/jdk-17.0.20.1+1`,
`./gradlew -q --max-workers=2 -Dorg.gradle.jvmargs=-Xmx1536m :app:testDebugUnitTest :app:assembleLab` (ověř čas APK — tichý pád daemonu
nechá staré APK; pak spusť znovu); APK `app/build/outputs/apk/lab/app-lab.apk`, `install -r`, NIKDY uninstall. adb
v `lesnik-toolchain/sdk/platform-tools`, `MSYS_NO_PATHCONV=1`. Obrazovka 1080×2392; kamera ≈ (540, 63) px.
Navigace: Home dlouhý stisk 540 1100 → Nastavení 907 2240 → rychlý přístup PULSE 476 2386.
**Souřadnice z `uiautomator dump` bývají zastaralé** (po animaci/scrollu) — klepej podle čerstvého screenshotu a nikdy
neklepej na odvozenou souřadnici, když hledání textu selže (minule to otevřelo panel oznámení).
Majitelovy vazby (PULSE Podržení → Global ARC, Dvojí klepnutí → Spotify, Klepnutí a podržení → Svítilna) neměnit,
hudbu nespouštět ani nezastavovat. `docs/screens/*` nemazat ani necommitovat.

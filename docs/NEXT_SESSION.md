Pokračujeme na Intelia Launcheru — **Control System V1** (`AGENTS.md` platí). Odpovídej česky, stručně. Limit
~200–250k tokenů: žádní agenti, soubory nečti celé, výstupy filtruj, screenshoty zmenšuj (PIL, výřez).

Stav (2026-10-18): větev `feat/control-system-v1`, nemergovat do `main`. Hotovo M0–M4 a M5 v kódu (viz
`docs/STATUS.md` nahoře): Studio › Ovládání (`ui/ControlsPage.kt`), picker se záložkami a „Už použité“, PULSE tahy
dolů/doleva/doprava (služba i Home), Home tah doleva/doprava, D20a remap tlačítek přes službu PULSE
(`action/KeyRemap.kt`, `Controls.keys/keysEverywhere`).

Úkol této session — OVĚŘIT NA TELEFONU (majitel musí být u telefonu, odemknout ho a mačkat tlačítka):
1. Essential Key: `adb shell getevent -lt` (na pozadí, grep EV_KEY) → majitel zmáčkne Essential Key 1×, 2×, podrží;
   pak hlasitost. Na `gpio-keys` je neznámý scan code 0x00fa (250), v `gpio-keys.kl` nenamapovaný — ověř, jestli to
   je on. Pak Studio › Ovládání › Hardwarová tlačítka › „Ověřit další tlačítko“: ukáže se vůbec? Výsledek do STATUS
   pravdivě. Když nepřijde, napiš to a navrhni jen systémové nastavení Essential Key (nic nepředstírat).
2. Remap: přiřaď hlasitost nahoru → Dvojí stisk → Svítilna, zapni „Tlačítka i mimo Intelii“. Ověř fyzicky: 1× mění
   hlasitost (změř zpoždění), 2× svítilna (`dumpsys media.camera | grep "Torch for"`), v jiné aplikaci taky,
   v hledání Intelie ne, se zhasnutou obrazovkou ne. `input keyevent` do filtru NEJDE, `sendevent` nemá práva.
   Pak binding a přepínač vrať / zeptej se majitele.
3. UI na telefonu: projdi Ovládání (hledání „svítilna“, sbalení skupin, zamčená gesta, křížek), picker (záložky,
   hledání, štítky mimo Home), PULSE tahy (služba: start na 540 70) a Home tah doleva/doprava (prázdné místo 540 1650);
   zkontroluj, že tah nahoru na Home dál otevírá šuplík a podržení kapsle dál funguje.
4. Pokud zbydou tokeny: export/import `controlConfig` včetně bindingů a `keys`; úklid legacy `arc/arcFlows/...`.

Build/telefon: JAVA_HOME `C:/Users/Ondra/AppData/Local/lesnik-toolchain/jdk-17.0.20.1+1`,
`./gradlew -q -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleLab`; Lab není debuggable, pro
`run-as`/logcat `assembleDebug` (stejný balíček, `install -r`, NIKDY uninstall). adb v
`lesnik-toolchain/sdk/platform-tools`, `MSYS_NO_PATHCONV=1`. Prefs: `run-as dev.intelia.launcher.lab cat
shared_prefs/intelia.xml` (jen debug build). Souřadnice: LENS 540 2190, kamera 540 70, Home 540 1650, Studio
dlouhý stisk 540 1000 → Nastavení 907 2256. Majitelovy bindingy (PULSE Podržení → Svítilna, Dvojí klepnutí →
Spotify) neměnit. Netracked `docs/screens/*.rar` nemazat ani necommitovat.

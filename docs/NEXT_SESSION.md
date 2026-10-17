Pokračujeme na Intelia Launcheru — **Control System V1** (`AGENTS.md` platí). Odpovídej česky, stručně. Limit
~200–250k tokenů na session: žádní agenti, soubory nečti celé, výstupy filtruj, screenshoty zmenšuj (PIL, výřez).

Stav (2026-10-18, po M2): větev `feat/control-system-v1` z `main` 710e84f (PR #3 mergnutý). Nemergovat do `main`.
Zadání celé řady: M0 → M5 (Action Engine, ARC V2, PULSE 2.0, gesta + hardwarová tlačítka, Studio „Ovládání“).

Hotovo a ověřeno na Nothing Phone (3) (Lab build, `install -r`):
- **M0** (`adffdc6`): filtr v pickeru „Přidat aplikaci/FLOW“ (bez diakritiky, „zpra“ → Zprávy); názvy v řádku „Moje“
  v šuplíku mají 4 dp okraj a výpustku (130 % ověřeno); klepnutí na kameru v editoru otevírá PULSE inspektor
  (Home je v editoru škálovaná na 86 %, kamera má vlastní cíl v souřadnicích obrazovky). Nastavení při 130 %: žádný
  ovládací prvek pod 48 dp. Zbývá: na Home je FLOW ikona těsně nad LENS ořezaná na 44 dp výšky (překryv s LENS),
  PULSE overlay cíl je 40 dp vysoký (řeší M3).
- **M1 jádro** (`970e324`): `action/Controls.kt` — `Trigger` (ArcSlot, Pulse, Home, Lens, Key) × `Gesture`,
  `Binding`, `ArcSet`, `Controls` + `sanitized()` + nedestruktivní `migrate(arc, arcFlows, arcShortcuts)`.
  `Action.Flow(id)` (stabilní ID), `Action.Lock`, `Requirement`. `LauncherSettings.controlConfig` (null = odvozeno
  z legacy polí přes `settings.controls`). `ui/AndroidActionHost.kt` (+ `rememberActionHost`). ARC puštění → `Action`
  → `HomeRoot.perform` (hláška podle `Reason`). Telefon: ARC app slot spustil Claude, FLOW „pocty“ spustil
  Kalkulačku → Hodiny v pořadí, návrat na LENS nespustil nic, žádné pády.

- **M2 ARC V2** (`50edba3`, `558234f` + oprava): ARC ze `settings.controls` (migrace: sada na prstenec), mělký
  oblouk `ArcBow` (slot 1 uprostřed, ≥ 48 dp rozestupy, okraj obrazovky), sady přes tah za konec nebo tečky,
  haptika na slot, malý popisek + náhled FLOW. Sdílený `ActionPicker` (Aplikace/FLOW/Zkratky/Systém, filtr,
  `Reason`/`Requirement`), Studio › Ovládání › ARC (sloty 3–7, S/M/L, úhel, haptika, sady). Telefon: viz STATUS.

Pravdivě NEhotovo:
- TalkBack přímo (ověřena jen klávesnice → stejný assisted režim), Reduced Motion, LENS na boční/horní hraně na
  telefonu, export/import `controlConfig`. Legacy pole `arc/arcFlows/arcShortcuts/arcRings` se už nečtou pro ARC
  (jen pro migraci); `LauncherSettings.ARC_*` konstanty a `ArcRings` (`CoreTest`) jsou zbytky k úklidu.
- `Binding`y pro PULSE/Home/LENS/klávesy nikdo nečte; PULSE služba má vlastní `PulseAction` menu.
- Akce Media/Sound/Panel/Lock/Screenshot ověřené nejsou (Svítilna ano, přes ARC).

Další krok — **M3 PULSE 2.0**: gesta kapsle přes `Trigger.Pulse(gesture)` → `Controls.bound` → `HomeRoot.perform`
(na Home) a v `PulseAccessibilityService` (mimo Home, jen akce bez UI Intelie); Studio › Ovládání › PULSE se
sdíleným `ActionPicker` (tap/double/hold), PULSE cíl ≥ 48 dp. Pak M4 (diagnostika kláves), M5 Studio.

Build/telefon: JAVA_HOME `C:/Users/Ondra/AppData/Local/lesnik-toolchain/jdk-17.0.20.1+1`, `./gradlew -q
-Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleLab`, adb v `lesnik-toolchain/sdk/platform-tools`,
`MSYS_NO_PATHCONV=1`, `install -r` (nikdy uninstall), před každým `input` ověřit `topResumedActivity`. Lab není
debuggable (`run-as` nejde) — zálohu dělej exportem v aplikaci. ARC test: `input motionevent DOWN 540 2190; sleep
0.9; MOVE …; UP …`. Python na Windows potřebuje cesty `C:/…`, ne `/c/…`.
Netracked: `docs/screens/v0.1.0-2026-10-17b.rar`, `…17c.rar` (majitelovy, nemazat ani necommitovat).

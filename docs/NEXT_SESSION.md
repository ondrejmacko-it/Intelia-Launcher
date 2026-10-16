# Prompt pro další session — Intelia Veil V2, část 12 (plynulost drawer, rychlost, paleta barev, Veil)

Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

```
Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky, stručně. Tvrdý limit 250 000 tokenů na session,
cíl ~180k. Žádní agenti, soubory nečti celé opakovaně, nepiš dlouhé texty do chatu. Když se blížíš ke 200k, zastav
se, commitni hotové a napiš prompt na pokračování.

Podklady (jen relevantní části): horní záznam docs/STATUS.md; docs/DESIGN_SYSTEM.md §4 (barvy, Accent, Veil
úrovně), §5.5, §5.10; docs/design/veil-prototype.html (hodiny: `.clock.bgv`, volba `bg` none/veil ř. ~423 a ~642);
kód `ui/Ink.kt` (`accentColor`, `inkPalette`), `ui/Veil.kt`, `ui/Icons.kt` (IconStyle Veil), `data/LauncherSettings.kt`
(`Accent`, `IconStyle`, `ClockStyle`), `data/HomeModel.kt` (`ItemTint`), `ui/Search.kt` (`DrawerOverlay`, `Drawer`
s `LazyColumn`), `ui/HomeRoot.kt`, `ui/Lens.kt`, `ui/Studio.kt`.

Hotovo minule (`7780a44`, `1861107`, `f361373`, `e7f4aa5`): karta inspektoru LENS nad ghost ARC; TalkBack akce
"Posunout LENS vlevo/vpravo" a "LENS na střed"; tažení v editoru přičítá touch slop; Search morph s LENS vpravo
ověřen záznamem (`screenrecord --output-format=raw-frames --size 216x480`, nekončí sám → `pkill -INT screenrecord`;
RGB 216×480×3 B/snímek, montáž vlastním PNG zápisem v Pythonu — PIL ani ffmpeg nejsou).
Majitel: tažení LENS, přeskládání řady, pinch i morph "jde to, funguje to", animace dobrá. Výhrady níže.

Tato session (každý bod: testy → build → telefon → commit + push; začni bodem 1, je nejvíc znát):
1. PLYNULOST DRAWERU (priorita): swipe zespoda nahoru → seznam aplikací A–Z se "strašně seká", vůbec není plynulý
   (Search je OK). Změř release buildem (`dumpsys gfxinfo … reset`, 3 série swipe up → scroll → Back), najdi
   příčinu (kompozice celé `LazyColumn` v prvním snímku, načítání ikon na hlavním vlákně, chybějící `key`/
   `contentType`, Veil blur/scrim přes celou plochu, rail A–Z) a oprav: první snímek jen plachta + MOJE, seznam
   o snímek později (jako Search v `61484cf`), ikony z `IconCache` předem, stabilní klíče. Před/po čísla do STATUS.
2. ZRYCHLENÍ CELKU: projdi délky animací (§5.6) a vše zkrať/zrychli (otevření/zavření drawer, Search, editor,
   inspektor, ARC); cíl svižný pocit bez ztráty plynulosti. Release měření před/po.
3. PALETA BAREV: místo několika akcentů paleta s větším výběrem (např. 12–16 odstínů + Dynamic z tapety), volitelná
   zvlášť pro: hodiny, datum/texty na ploše, ikony, LENS, Search, a akcent nastavení (Studio). Výchozí = jedna
   barva propsaná všude; "sjednotit" přepínač. Vše přes `LauncherSettings` (+ `sanitized()`), migrace z `Accent`
   a `ItemTint`. Barvy se musí propsat i do Studia (texty, přepínače, posuvníky) a do Search/drawer.
4. BAREVNÉ IKONY: Veil ikony jsou "bez života". Přidej styl ikon barevně sjednocený se systémem (tint/duotón
   z vybrané barvy palety, s jemným Veil podkladem), ponech System/Ink/Veil; volba v Studio i v inspektoru ikony.
   Ověř kontrast na světlé i tmavé tapetě.
5. VEIL HODINY: volba pozadí hodin "Veil" jako v návrhu (`docs/design/veil-prototype.html`, `.clock.bgv`:
   padding 14/20/16 px, radius 26, Veil podklad za časem i datem) v inspektoru hodin.
6. CLEAN VEIL: průhlednost celého Veilu čistší — méně šumu/zrna a šedého závoje, jasnější okraj, čitelný text;
   uprav úrovně ve `Veil.kt` jednotně (karty, plachty, pilulky, inspektor) a porovnej screenshoty před/po.
7. Z minula zbývá: TalkBack dvojklep na LENS v editoru volá Search místo výběru (v editoru ať `onClick` vybere,
   `onLongClick` neotevírá ARC; zkontroluj i ikony) a změřit slop u tažení ikon. Majitele požádej o TalkBack test.
Nestihne-li se vše, priorita 1 → 2 → 3 → 4 → 5 → 6 → 7; zbytek do nového promptu.

Git: větev `claude/crystal-redesign`, na začátku `git fetch` a `git status`, malé commity + push, žádný merge do
`main`. Před testy zazálohuj shared_prefs/intelia.xml (`adb exec-out run-as dev.intelia.launcher.lab cat …`), po
testu vrať (push do /data/local/tmp, `am force-stop`, `run-as … cp`, ověř `cmp`). Release build pro měření:
dočasně v `app/build.gradle.kts` release `signingConfig = signingConfigs.getByName("debug"); applicationIdSuffix =
".lab"`, `assembleRelease`, soubor hned vrátit; po instalaci `cmd package compile -m speed -f dev.intelia.launcher.lab`;
na konci zase nainstalovat debug (jinak nejde `run-as`). Build: `.\gradlew.bat :app:testDebugUnitTest
:app:assembleDebug` (JAVA_HOME = `C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`; v Git Bash
`MSYS_NO_PATHCONV=1`, ale cesty pro `adb push/install` převeď `cygpath -w`), adb
`C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe`. Telefon Nothing Phone (3), 1080×2392,
LENS (540, 2193) px; Lab: `am start -n dev.intelia.launcher.lab/dev.intelia.launcher.MainActivity`. Před KAŽDÝM
`input` ověř `topResumedActivity` (po instalaci se může nahoře objevit jiná aplikace); klepnutí u kamery chytá
overlay PULSE všude. Texty přes `uiautomator dump`. Python skripty piš do souboru ve scratchpadu. Na konci STATUS
jen ověřená fakta, DESIGN_SYSTEM §5, nový prompt v docs/NEXT_SESSION.md, krátký report.
```

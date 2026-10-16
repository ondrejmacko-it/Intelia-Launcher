# Prompt pro další session — Intelia Ink rework

Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

---

Pokračujeme ve vývoji **Intelia Launcheru** (Kotlin + Jetpack Compose, `AGENTS.md` platí). Jsi hlavní Android engineer a UI/UX architekt. Pracuj autonomně, testuj na telefonu a commituj po malých celcích. Žádný merge do `main` bez mého schválení, žádný force push, `git reset --hard` ani `git clean`.

## Kde jsme
- Větev **`claude/ink-rework`**, poslední commit `7c7de20 wip: … (does not build yet)`. **Nic z větve zatím není na GitHubu.** Pushni až po zeleném buildu.
- Kontext si přečti v `docs/DESIGN_HANDOFF.md`, `docs/STATUS.md` a `docs/DECISIONS.md` (D1–D4). Design v Claude Design je na https://claude.ai/artifact/Wa9q4hwhdQQYNC1E919rMa (10 artboardů, čti přes nástroj Artifact). Je to výchozí bod, ne závazná šablona.
- **Nástroje na tomto PC:** JDK `C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1` (nastav `JAVA_HOME`), SDK `…\lesnik-toolchain\sdk` (už je v `local.properties`), adb `…\lesnik-toolchain\sdk\platform-tools\adb.exe`. Build: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.
- **Telefon:** Nothing Phone (3) A059, Android 16 / API 36, USB. Varianty: `dev.intelia.launcher.debug` (starý fork), `dev.intelia.launcher.dev` (foundation, **aktuální HOME**, podepsaný ztraceným klíčem, nedá se aktualizovat) a `dev.intelia.launcher.lab` („Intelia Lab“, náš debug build, podepsaný `keystore/debug.keystore`). Neodinstalovávej nic, nemaž data a výchozí HOME neměň bez mého souhlasu. Když budeš potřebovat oprávnění nebo přepnout HOME na Lab, napiš mi a já to na telefonu povolím.
- **Soukromí:** na telefonu nechoď do osobních dat, souborů, financí ani zpráv. Screenshot s osobním obsahem hned smaž. MT Capsule (`com.pryshedko.mtisland`) jsem povolil jen prohlížet, službu přístupnosti nezapínej.

## Moje rozhodnutí
1. Starý klíč nemám, proto vznikla varianta `.lab` se sdíleným klíčem (D4).
2. Home = systémová tapeta. **Při přechodu do Recents nesmí být vidět žádný obdélník**, launcher se musí chovat jako plocha, ne jako aplikace. Ověřená příčina: během gesta systém okno plochy skryje a zůstane čistá tapeta. Stará Home přes celou obrazovku kreslí ztmavení, které tak vypadá jako obdélník. Nová Home proto **nesmí mít žádnou celoplošnou vrstvu ani pozadí**; čitelnost zajistí stín textu a barva textu podle tapety. Ověř to na telefonu (snímky během gesta, `screencap` do `/data/local/tmp`, potom smazat).
3. FLOW v ARC: vyber praktičtější řešení. Návrh: puštění prstu na FLOW v ARC je explicitní spuštění, náhled kroků je vidět během držení. Enter při přesné shodě v hledání taky spustí. Psaní samo nikdy nic nespustí. Zapiš to jako D5 a uprav pravidlo v `AGENTS.md`.
4. Prstenec 3 v ARC = zkratky aplikací (`LauncherApps`, fungují jen když je Intelia HOME). Kontakty až později, vyžadují oprávnění.
5. Mám kreativní volnost: inspirace MT Capsule (PULSE efekt), Niagara a Pie launcher, ale nic nekopírovat. Chci jednotný, unikátní a nový styl tlačítek, hodin, textů, PULSE, FLOW, ARC a nastavení. Každé odchýlení od designu musí být vratné a zdokumentované v `DECISIONS.md`.

## Designový jazyk „Prstenec a nit“ (už v kódu)
- Dvě ohniska plochy: **PULSE** u kamery (systém mluví k tobě) a **LENS** u palce (ty mluvíš k systému). Vše je z kroužků, vlasových linek a nití, žádné karty.
- Paleta Ink: papír `#EFEBE4`, inkoust `#1C1814`, akcent terakota (`#A84B29` světlý, `#E39A78` tmavý). Na Home je inkoust nebo papír podle `WallpaperColors.HINT_SUPPORTS_DARK_TEXT`.
- Hodiny dvěma hlasy: hodiny rovně, minuty kurzívou, místo dvojtečky malý akcentový prstenec (`SplitClock`).
- LENS: otevřený prstenec a „přílivová čárka“ (`TideDash`). Stejná čárka značí výběr všude, například v `Segments`.
- ARC: tři orbity (aplikace 5 / FLOW 7 / zkratky 7). Akcentová nit vede od LENS k vybrané položce, nahoře je velký náhled. Vnější prstence se odkrývají postupně a návrat na LENS gesto zruší.
- FLOW se zobrazuje jako korálky na niti (`BeadThread`). Přepínač = prstenec, který se vyplní (`RingToggle`).
- PULSE: v klidu neviditelný. Při návratu na plochu vyjde od kamery ozvěna. Při blízkém budíku nebo nabíjení se ukáže kapsle a okraj kolem kamery funguje jako ukazatel baterie. Klepnutí otevře panel s budíkem, baterií a svítilnou. Žádná oprávnění ani práce na pozadí.
- Písma: zatím systémová (Serif/SansSerif/Monospace) v `InkFonts`. Newsreader, Instrument Sans a Geist Mono (vše OFL) přibalit do `res/font` **jen s mým souhlasem ke stažení** (uveď zdroj, název a velikost) a zapsat jako D6.

## Stav kódu (`app/src/main/java/dev/intelia/launcher/`)
Hotovo (napsáno, zatím nezkompilováno):
- `data/LauncherSettings.kt`: `layout`, `favorites`, `arcFlows`, `arcShortcuts`, `arcRings`, `clockStyle`, `motion`, `haptics`, `iconStyle`, `pulse`, `onboarded`; migrace `reduceMotion` → `motion`.
- `data/AppRepository.kt`: `essentialPackages()`, zkratky (`shortcuts`, `shortcutIcon`, `startShortcut`). `data/IconCache.kt`: styl Ink s monochromatickou vrstvou na API 33+, jinak šedá ikona.
- `ui/ArcGesture.kt` (`ArcRings` a `ArcPick`, čistý model) a testy v `test/.../CoreTest.kt`.
- `ui/Ink.kt` (téma, palety, `LocalHomeInk`, pohyb, haptika, `SystemBarIcons`), `ui/Kit.kt` (komponenty), `ui/Glyphs.kt`, `ui/Icons.kt` (`AppIcon` s `tint`, `ShortcutIcon`), `ui/Lens.kt` (`LensArc`, `LensLift`, `lensShift`), `ui/Pulse.kt`, `ui/HomeViewModel.kt`. Manifest má `<queries>` pro výchozí aplikace a budíky.

Zbývá:
1. **Přepsat `ui/HomeRoot.kt`** a rozdělit ho na `Search.kt`, `FlowPages.kt`, `Studio.kt`, `Onboarding.kt` a přepsat `Sheets.kt`. Stávající soubory používají staré API (`Lens(...)`, `ClockFace`, `FlowEditorSheet`, `SettingsSheet`, `saveAlias(alias)` bez `inArc`, `Glyph.Settings`).
   - Home: bez pozadí. Hodiny a datum nahoře, rozvržení Pure / Essentials (4 oblíbené kolem LENS, pozice z `LensLift`/`lensShift`) / Custom (seznam oblíbených v serifu). `LensArc` (při otevřeném ARC zeslabit hodiny), `Pulse` když je zapnutý. Swipe nahoru = šuplík, podržení plochy = Studio. Při `ON_START` zavolat `model.refreshShortcuts()`.
   - Hledání: pole dole u palce, výsledky přes `LazyColumn(reverseLayout = true)` rostou nahoru, nad polem blok přesné shody FLOW (název 40sp kurzívou, nit, kapsle Spustit). Prázdný dotaz = šuplík: řádek „Moje“, A–Z s písmeny a abecední lištou vpravo.
   - FLOW: seznam a editor přes celou obrazovku (Uložit nahoře, kroky s pořadím, přepínač „V oblouku ARC“, Smazat).
   - Studio místo spodního panelu: živý náhled nahoře přímo na tapetě, pod ním vzhled, plocha a oblíbené, ARC (prstence 1–3 a výběr zkratek), PULSE, pohyb a haptika, výchozí plocha, skryté aplikace a O aplikaci.
   - Onboarding ve 3 krocích: úvod s ilustrací orbit, volba rozvržení s mini náhledem, nastavení jako plocha + krátký průvodce gesty → `finishOnboarding(layout)`.
   - Možnosti aplikace: ARC, oblíbené, skrýt, informace.
2. Doplnit chybějící řetězce do `values/strings.xml` a `values-cs/strings.xml`. Už používané a zatím neexistující: `arc_ring_apps`, `arc_ring_shortcuts`, `arc_release`, `arc_further`, `arc_cancel_hint`, `arc_empty_title`, `close`, `pulse`, `today`, `tomorrow`, `no_alarm`, `charging`, `on_battery`, `torch`, `collapse`. Uprav i `arc_empty` a dál všechny nové texty obrazovek.
3. Zelený build + testy (16 starých, část nahrazena + nové testy ARC a nastavení), bez nových varování z našeho kódu.
4. Instalace `.lab` (`adb install -r`), live test: Home, návrat z aplikací, **Recents bez obdélníku**, tapeta, LENS, hledání, šuplík, ARC včetně zrušení, FLOW, klávesnice a insets, ukládání nastavení, světlý a tmavý režim, PULSE, logcat. Měř studený a teplý start HOME (`am start -W`). Na plné ověření požádej o dočasné přepnutí HOME na Lab.
5. `docs/DECISIONS.md` D5 (FLOW release a Enter, swipe = šuplík, prstenec 3 = zkratky, PULSE jen na ploše, jazyk „Prstenec a nit“), `docs/STATUS.md` jen s ověřenými fakty, úprava `AGENTS.md` (ARC už není 5 slotů) a `README.md`.
6. Malé commity, push větve a PR do `main` až po ověření na telefonu. Nemergovat.

Na konci mi dej stručný report: příkazy s PASS/FAIL, co je ověřené na telefonu a co ne, a jen nezbytné otázky.

# Prompt pro další session — Intelia Veil V2, části 4–6

Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

---

Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky, stručně. Tvrdý limit 250 000 tokenů na session, cíl ~180k. Žádní agenti, žádné dlouhé shrnutí, soubory nečti celé opakovaně (jen potřebné řádky), nepiš dlouhé texty do chatu. Když se blížíš ke 200k, zastav se, commitni hotové a napiš prompt na pokračování.

Směr Intelia Veil (D12) je schválený i s úpravami majitele (viz D12 v docs/DECISIONS.md). Podklady (čti jen relevantní části): docs/DESIGN_SYSTEM.md §5.2 (hustoty Light/Dense/Focus), §5.4–5.8; horní záznam docs/STATUS.md; vizuální reference docs/design/veil-prototype.html (13 scén). Nevymýšlej jinou podobu.

Hotovo (části 1–3, commity `9f2b215`, `fc1c7a4`, `4af8723`):
- `ui/Veil.kt`: `VeilLevel` Light/Dense/Focus, `veilTokens()`, `Modifier.veilSurface(tokens, origin, shape)`. Studio = Focus, sheet aplikace = Dense (80–85 % + scrim 40 %).
- `data/HomeModel.kt`: `HomeLayouts`/`LayoutSpec`/`HomeItem`/`Anchor` (plochá kotva: Cell/Free/LensRow/Lens/Bound/Stack), `HomeTemplates` (Pure/Essentials/Balanced), `HomeLayouts.save()`. Schema 2; migrace a validace jen v `LauncherSettings.sanitized()`. Propojená předloha (`linked`, id `preset`) se vždy skládá ze starých polí (`layout`, `favorites`, `showClock`, `showDate`); ✓ v editoru zapíše vlastní kopii (id `mine`). Studio → Rozvržení přepne zpět na předlohu, vlastní rozložení zůstane.
- `ui/HomeEditorState.kt`: čistý Kotlin — `HomeGeometry` (oblast obsahu, buňky, sloty LENS ±62/±108 dp, zóna LENS), tažení, přichycení, vodítka, uhnutí, výměna v řadě LENS, blokované zóny (hodiny), undo/redo ≤ 50. JVM testy.
- `ui/HomeEditor.kt` + `Home()` v `ui/HomeRoot.kt`: edit mode (podržení plochy / ikony; ikona se táhne hned), horní lišta (undo, redo, umístění, Studio, ✓), body mřížky, cílový kroužek, zóna LENS, dialog Pokračovat/Zahodit/Uložit. Řetězec modifikátorů rodiče se v edit mode nesmí měnit (jinak se zruší tažení z ikony).
- Prozatím: klepnutí na ikonu v editoru otevře starý sheet místo inspektoru; LENS se v modelu jen rezervuje (pozici dál určuje Reach); `Flow`/`Widget` položky se nekreslí.

Úpravy majitele, které platí dál:
- Panely klidné a matné (Light / Dense ~75–85 % / Focus ~88–95 %), ostrý text, žádné neonové hrany, jemná hrana od původu. Musí fungovat na světlé, tmavé i barevné tapetě.
- Kontextové editory s progressive disclosure: první úroveň jen nejčastější volby, zbytek pod „Pokročilé“. Inspektor nesmí zakrýt upravovaný prvek (hodiny i ikona vidět se změnou živě).
- Horní lišta: vlevo předlohy + zpět/reset, vpravo Studio + ✓; nesmí překážet hodinám ani sobě. Předlohy jako vizuální miniatury s náhledem před potvrzením; vlastní rozložení se nesmí smazat bez varování.
- Hitbox LENS 84 × 100 dp nepřebírat bez testu na telefonu (tap, hold, swipe up, ARC vs. vnitřní Essentials).
- Nedělat: redesign ARC, PULSE 2.0, přepis FLOW, nesouvisející systémové integrace, velké frameworky. Widgety jen rezervované.

Git: větev `claude/crystal-redesign`, na začátku `git fetch` a `git status`. Malé commity + push, žádný merge do `main`, nic destruktivního. Data majitele (Essentials, hodiny Crystal/Ink dle jeho volby, FLOW, ARC) se nesmí ztratit — před testem ukládání zazálohuj `shared_prefs/intelia.xml` přes `run-as dev.intelia.launcher.lab` a po testu ho vrať.

Tato session = části 4–6 (každá: testy → build → telefon → commit):

4. Kontextové editory + předlohy: inspektor (Veil Dense, 312 dp, nad prvkem když se vejde, jinak pod, hrana od prvku) pro aplikaci (ikona, velikost 32–64 dp, popisek, Odebrat z plochy — nikdy neodinstalovat), hodiny (písmo, velikost vč. úchytu, tloušťka, barva, krytí, datum; zbytek pod „Pokročilé“) a LENS (velikost, styl; ghost ARC). Add menu po klepnutí na prázdné místo (App, FLOW; Widget jen rezervovaný). Předlohy vlevo v liště: miniatury Pure/Essentials/Balanced + vlastní, náhled před potvrzením, reset s potvrzením, přepnutí s neuloženými změnami se zeptá. Vše přes `HomeEditorState` (rozšiř o úpravy položek, testy).
5. LENS → Search s reálným IME: morf z kroužku LENS do pole (§5.6), pole jede s `WindowInsets.ime` ve stejném snímku, Back opačně; psaní funguje od prvního snímku, nic se nespouští při psaní. Ověřit na telefonu se skutečnou klávesnicí. Rozhodnout a otestovat hitbox LENS 84 × 100 dp.
6. Polish, TalkBack (vlastní akce Přesunout / Upravit / Odebrat z plochy, posun tlačítky v inspektoru), Reduced Motion, výkon: `adb shell dumpsys gfxinfo dev.intelia.launcher.lab` při LENS → Search a tažení, PSS (`dumpsys meminfo`), studený start. Žádná tvrzení o výkonu bez měření.

Build: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` (JAVA_HOME = `C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`; v Git Bash s `MSYS_NO_PATHCONV=1` zadávej JAVA_HOME Windows cestou), instalace `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Telefon: Nothing Phone (3), Android 16, 1080×2392, 420 dpi (411 × 911 dp), LENS na (540, 2193) px. adb `C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe`. Před každým `input` ověř `topResumedActivity` (`dev.intelia.launcher.lab/`); když telefon používá majitel, počkej a nic neklikej. Gesta přes `input motionevent DOWN/MOVE/UP`; texty přes `uiautomator dump` místo screenshotů. MT Capsule, tapetu a jiné aplikace neměnit bez souhlasu (světlou/barevnou tapetu jen se souhlasem majitele).

Na konci: docs/STATUS.md jen ověřená fakta, doplnit docs/DESIGN_SYSTEM.md §5 o to, co se změnilo, nový prompt v docs/NEXT_SESSION.md (stejný limit 250k), krátký report: co je hotové, screenshoty před/po, testy, omezení, commit.

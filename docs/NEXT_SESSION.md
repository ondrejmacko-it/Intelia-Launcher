# Prompt pro další session — Intelia Veil V2, část 7 (dokončení a měření)

Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

---

Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky, stručně. Tvrdý limit 250 000 tokenů na session, cíl ~180k. Žádní agenti, žádné dlouhé shrnutí, soubory nečti celé opakovaně (jen potřebné řádky), nepiš dlouhé texty do chatu. Když se blížíš ke 200k, zastav se, commitni hotové a napiš prompt na pokračování.

Směr Intelia Veil (D12) je schválený i s úpravami majitele (viz D12 v docs/DECISIONS.md). Podklady (čti jen relevantní části): docs/DESIGN_SYSTEM.md §5.5 (odstavec „As built“), §5.6; horní záznam docs/STATUS.md; vizuální reference docs/design/veil-prototype.html. Nevymýšlej jinou podobu.

Hotovo (části 1–6, poslední commity `00d62f5` … `2fa6217` + docs):

* Editor: inspektory (ui/HomeInspector.kt) pro aplikaci/FLOW (velikost, Posunout ← ↑ ↓ →, Popisek, Odebrat z plochy, Další volby), hodiny (písmo, velikost + úchyt, datum; Pokročilé: tloušťka, barva, krytí) a LENS (velikost, Kroužek/Plný, ghost ARC). Add menu (Aplikace, FLOW, Widget rezervovaný). Lišta: Předlohy + undo/redo vlevo, Studio + ✓ vpravo; pruh předloh nad řadou LENS s náhledem, Obnovit a varováním „Přepsat vlastní rozložení?“. Vše přes `HomeEditorState` (`edit`, `remove`, `addAt`, `setDate`, `preview/confirmPreview/cancelPreview`, `nudge`), 70 JVM testů.
* LENS → Search: morf kroužek → pole (clip + obrys nad plným polem, IME a fokus od prvního snímku), Back opačně.
* Hitbox LENS 84 × 100 dp ověřen na telefonu (tap −44 dp = Zprávy, −40 dp = Hledání, swipe, hold → ARC).
* TalkBack: vlastní akce Přesunout / Upravit / Odebrat z plochy na ikonách (v sémantice, TalkBack nezapnutý).

Úpravy majitele, které platí dál: panely klidné a matné (Light / Dense ~80–85 % / Focus ~88–95 %), ostrý text, žádné neonové hrany; progressive disclosure; inspektor nesmí zakrýt upravovaný prvek ani hodiny; vlastní rozložení se nesmí smazat bez varování. Nedělat: redesign ARC, PULSE 2.0, přepis FLOW, nesouvisející systémové integrace, velké frameworky. Widgety jen rezervované.

Git: větev `claude/crystal-redesign`, na začátku `git fetch` a `git status`. Malé commity + push, žádný merge do `main`, nic destruktivního. Data majitele (Essentials, hodiny dle jeho volby, FLOW, ARC, vlastní rozložení `mine`) se nesmí ztratit — před testem ukládání zazálohuj `shared_prefs/intelia.xml` přes `run-as dev.intelia.launcher.lab` (`adb exec-out run-as … cat`) a po testu ho vrať (`adb push` do /data/local/tmp, `am force-stop`, `run-as … cp`, ověř `cmp`).

Tato session = část 7 (každý bod: testy → build → telefon → commit):

1. Výkon LENS → Search: změřeno 12,85 % janky snímků (p99 133 ms, 76 slow UI thread) při 3 × LENS → Search → Back. Najdi příčinu (pravděpodobně první kompozice šuplíku s ~221 aplikacemi / ikony) a sniž ji; před a po `adb shell dumpsys gfxinfo dev.intelia.launcher.lab reset` + stejný postup. Prověř jednorázových 320 MB PSS ~2 s po studeném startu (`dumpsys meminfo` v čase 1/2/5/15 s). Bez měření žádná tvrzení.
2. Ověřit na telefonu, co zbylo: přidání FLOW na plochu (Add menu → FLOW → klepnutí spustí FLOW jen na explicitní akci), Reduced Motion (Studio) → morf a inspektor 0 ms, popisek pod ikonou, velikost ikony, krytí/barva hodin. TalkBack zapínat jen se souhlasem majitele.
3. Světlá a barevná tapeta jen se souhlasem majitele: čitelnost Veil Light/Dense, inspektory, lišta.
4. Pokud zbude rozpočet: tažení LENS v editoru (x 60–300 dp, přichycení na střed) podle §5.5, s testy v `HomeEditorState`; wallpaper zoom-out (§5.5) jen pokud ho tapeta Atmosphere podporuje — ověř a zapiš.

Build: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` (JAVA_HOME = `C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`; v Git Bash s `MSYS_NO_PATHCONV=1` zadávej JAVA_HOME Windows cestou), instalace `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Telefon: Nothing Phone (3), Android 16, 1080×2392, 420 dpi (411 × 911 dp), LENS na (540, 2193) px. Výchozí plocha telefonu je Nothing launcher; Lab se otevírá `am start -n dev.intelia.launcher.lab/dev.intelia.launcher.MainActivity`. adb `C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe`. Když je telefon zamčený (`isKeyguardShowing=true`), požádej majitele o odemčení. Před každým `input` ověř `topResumedActivity` (`dev.intelia.launcher.lab/`); když telefon používá majitel, počkej a nic neklikej. Gesta přes `input motionevent DOWN/MOVE/UP`; texty přes `uiautomator dump` místo screenshotů. Python skripty piš přes heredoc do scratchpadu (inline `python - <<EOF` s trojitými uvozovkami Bash nástroj občas rozbije). MT Capsule, tapetu a jiné aplikace neměnit bez souhlasu.

Na konci: docs/STATUS.md jen ověřená fakta, docs/DESIGN_SYSTEM.md §5 doplnit o změny, nový prompt v docs/NEXT_SESSION.md (stejný limit 250k), krátký report: co je hotové, měření před/po, testy, omezení, commit.

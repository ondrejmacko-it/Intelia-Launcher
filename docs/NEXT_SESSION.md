# Prompt pro další session — Intelia Veil V2, část 8 (Search ve Veil + měření)

Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

```
Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky, stručně. Tvrdý limit 250 000 tokenů na session,
cíl ~180k. Žádní agenti, soubory nečti celé opakovaně, nepiš dlouhé texty do chatu. Když se blížíš ke 200k, zastav
se, commitni hotové a napiš prompt na pokračování.

Podklady (jen relevantní části): docs/DESIGN_SYSTEM.md §5.5 (odstavce „As built“ a „Customisation lives on Home“),
§5.6; horní záznam docs/STATUS.md; prototyp docs/design/veil-prototype.html (Search: CSS `.morph/.res/.kb`,
JS `openSearch/closeSearch/renderResults`, ř. ~210–240 a ~859–910).

Hotovo minule: přizpůsobení přesunuto do editoru plochy (panel Vzhled, inspektor data, skrytí hodin, PULSE inspektor,
± u posuvníků, pinch), Studio bez vzhledu, oprava Mono ikon. Pinch a styl Veil ikon na telefonu neověřeny.

Tato session (každý bod: testy → build → telefon → commit + push):
1. LENS → Search podle prototypu, ve Veil, jednotně a čistě: plocha zůstane vidět (ztlumená), pole jako Veil pilulka
   se rozvine z kroužku LENS a jede s klávesnicí (WindowInsets.ime), výsledky ve Veil Dense kartě nad polem
   (max ~6, první zvýrazněný = Enter, FLOW karta s řetězcem ikon, prázdný dotaz = Nedávné/oblíbené). Žádné italic
   serif ani podtržené pole. Nic se nespouští při psaní. Back obráceně. Reduced Motion = 0 ms.
2. Šuplík (swipe nahoru) jako Veil Focus přes ztlumenou plochu (majitel schválil), abecední seznam, pole dole;
   šuplík se nesmí skládat při LENS → Search (pravděpodobná příčina 12,85 % janku).
3. Měření před/po: `adb shell dumpsys gfxinfo dev.intelia.launcher.lab reset`, 3× LENS → Search → Back,
   janky %, p99; `dumpsys meminfo` 1/2/5/15 s po studeném startu. Bez měření žádná tvrzení.
4. Zbytek, pokud zbude: požádat majitele o test pinch a Veil ikon; LENS tažení v editoru (§5.5).

Git: větev `claude/crystal-redesign`, na začátku `git fetch` a `git status`, malé commity + push, žádný merge do
`main`. Před testy zazálohuj shared_prefs/intelia.xml (`adb exec-out run-as dev.intelia.launcher.lab cat …`), po
testu vrať (push do /data/local/tmp, `am force-stop`, `run-as … cp`, ověř `cmp`) — změny v panelu Vzhled se ukládají
hned. Build: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` (JAVA_HOME =
`C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`; v Git Bash `MSYS_NO_PATHCONV=1`), adb
`C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe`. Telefon Nothing Phone (3), 1080×2392,
LENS (540, 2193) px; Lab: `am start -n dev.intelia.launcher.lab/dev.intelia.launcher.MainActivity`. Před `input`
ověř `topResumedActivity`; klepnutí u kamery chytá overlay PULSE všude. Texty přes `uiautomator dump`. Python
skripty piš do souboru ve scratchpadu (inline heredoc s uvozovkami se rozbije). Na konci STATUS jen ověřená fakta,
DESIGN_SYSTEM §5, nový prompt v docs/NEXT_SESSION.md, krátký report.
```

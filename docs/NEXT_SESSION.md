Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky, stručně. Tvrdý limit 250 000 tokenů na session,
cíl ~180k. Žádní agenti, soubory nečti celé opakovaně, výstupy příkazů filtruj. Když se blížíš ke 200k, zastav se,
commitni hotové a napiš prompt na pokračování.

Podklady: horní záznam docs/STATUS.md; docs/DECISIONS.md D13, D14; docs/DESIGN_SYSTEM.md §5.5.2, §5.12.1;
docs/screens/README.md; kód `ui/HomeInspector.kt` (`InspectorFrame`, `InspectorCard`, `CardHeader`, `ResizeHandle`,
`PlateControls`), `ui/HomeEditor.kt` (`EditorBar` editing, `PlateHandles`), `ui/HomeRoot.kt` (`atFx`, `DateText`),
`ui/Veil.kt` (`plateTokens`), `data/HomeModel.kt` (`TextLook.padX/padY`, `ClockBackground`).

Hotovo minule (`b856da0` + navazující commit): tažení hodin/volného data 1:1 s prstem (umístění středem na `fx`);
karta editoru se umístí jednou a drží (posun data už neutíká pod prstem), vždy celá na obrazovce nad navigační lištou,
hlavička s názvem prvku (i pod lištou editoru), přesun tažením hlavičky, šipky v rozích a po stranách mění velikost,
⟲ vrací; deska hodin/data: Veil / Krystal / Mráz / Kapka + roztažení úchyty ↔ ↕ i posuvníky; čipy se zalamují,
písma na čipech ve vlastním fontu; plocha zamčená na výšku (`nosensor`); 66 snímků v docs/screens/v0.1.0-2026-10-17.

Na začátku se zeptej majitele, jak mu sedí přesouvatelná karta a desky ze skla (a zda chce kartu pamatovat i po
uložení rozložení).

Tato session (každý bod: testy → build → telefon → commit + push):
1. Ověřit neověřené: otočení telefonu na ploše (zůstane na výšku), čeština ěščřžýáíéůú v každém písmu (dočasně
   vlastní formát s dlouhým dnem „čtvrtek/středa“ nebo zkušební text), Studio (nejdřív uložit nebo zahodit změny),
   TalkBack na hlavičce karty a úchytech.
2. „Všude“ podle majitele: panel Vzhled a Předlohy jako stejná přesouvatelná/roztažitelná karta s hlavičkou;
   úchyty velikosti i u ikon a LENS (místo jen posuvníku).
3. Bod ③ programu D13: dok/LENS kdekoli + tvary ikon (squircle, zaoblený čtverec, kapka), pořadí 4 slotů,
   ukotvení dole/nahoře/vlevo/vpravo (na boku svisle, ARC do středu).
4. Nová sada snímků do nové složky `docs/screens/<verze>-<datum>/`, starou složku smazat (zůstane v historii),
   aktualizovat docs/screens/README.md.

Git: větev `claude/crystal-redesign`, na začátku `git fetch` a `git status`, malé commity + push, žádný merge do `main`.
V telefonu je debug Lab (run-as funguje), HOME role = Lab. Zálohuj shared_prefs/intelia.xml
(`adb exec-out run-as dev.intelia.launcher.lab cat …`), po testu vrať (push do /data/local/tmp, `am force-stop`,
`run-as … cp`, ověř shodu bajtů). Build: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`
(JAVA_HOME = `C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`), adb
C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe. Telefon Nothing Phone (3), 1080×2392,
LENS (540, 2193) px; editor = dlouhý stisk na prázdné ploše (540, 1300); hodiny majitele vlevo nahoře (~(230, 700)),
volné datum leží na horní hraně desky hodin; PULSE (600, 60). Před KAŽDÝM `input` ověř `topResumedActivity`.
Osvědčený postup: Python ovladač ve scratchpadu (adb přes subprocess, `uiautomator dump /dev/tty`, hledání podle textu,
klepnutí na střed bounds, snímek → JPEG 50 % do docs/screens + náhled 40 %, kontaktní listy po 6 snímcích);
pozor, uiautomator vrací i neořezané bounds prvků mimo viditelnou část karty — klepnout jen když leží uvnitř.
Na konci STATUS jen ověřená fakta, DESIGN_SYSTEM §5, nový prompt v docs/NEXT_SESSION.md, krátký report.

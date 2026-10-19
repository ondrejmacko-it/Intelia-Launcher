Pokračujeme na Intelia Launcheru — **P1 Onboarding V2: implementace** (`AGENTS.md` platí; česky, stručně,
~200k tokenů, žádní agenti). Větev `claude/pulse-reliability` (majitel: pokračovat na ní). Zdroj pravdy:
`docs/product/ONBOARDING_V2.md` (§4 kroky, §5 komponenty, §10 rozhodnutí majitele, **§11 finální revize = požadavky**),
prototyp `docs/design/onboarding-v2-prototype.html`, `docs/DESIGN_SYSTEM.md` §5 (Veil tokeny, pohyb).

## Stav (2026-10-19)
- Majitel schválil směr „onboarding na živé ploše“ a chce rovnou implementaci, grafika musí sedět k launcheru
  (`ui/Veil.kt`, `VeilKit.kt`, `Motion.kt`, `HomeScene.kt`), náhled = skutečná Home podle aktuálního nastavení.
- `e788977`: nové instalace startují ze `HomeTemplates.signature()` (plocha majitele bez FLOW), akcent Silver,
  systémová tapeta, Ink ikony. Testy 243/243 PASS, debug nainstalován na Phone (3) (`install -r`).
- Lokální commity `e36d707`, `e788977` **nejsou pushnuté** (push zablokoval auto-mode). Majitel pushne sám
  (`git push origin claude/pulse-reliability`) a PR #6 do `main` mergne v GitHubu.

## Plán po etapách (každá = commit + `./gradlew :app:testDebugUnitTest :app:assembleDebug` + `install -r`)
1. `onboarding/OnboardingDraft.kt` (čistý Kotlin): kroky podle voleb (Pure bez Aplikací), next/back/skip/skipAll,
   JSON v `noBackupFilesDir`, `commit(settings) → LauncherSettings` (jeden `sanitized()`); `OnboardingDraftTest`.
2. `OnboardingHost` + `HomeStage` (skutečná `Home` v režimu preview: full / mini 0.43 / blur, křivka Home Editoru),
   `StepSheet`/`StepBar`; kroky Vítej, Plocha, Styl (16 akcentů + Dynamic + vlastní), Aplikace (`AppSearch`).
3. LENS + ARC trénink (`sandbox`, `PreviewActionHost` nic nespouští) a PULSE krok (výchozí Všude, kompaktní kapsle,
   neměnit `PulseAccessibilityService`).
4. Odemkni možnosti (`PermissionRow`, stav na `ON_RESUME`, RoleManager), Hotovo (commit z obou tlačítek).
5. Discovery Hub ve Studiu (Studio › Prozkoumat), „Spustit průvodce znovu“ bez resetu.
6. Ověření na telefonu: nový profil / `pm clear` **jen na testovacím balíčku, nikdy Lab majitele**; process death
   na každém kroku; TalkBack, velké písmo, Reduced Motion. Aktualizovat `STATUS.md` jen ověřenými fakty.

Nikdy neodinstalovat Lab, nesahat na nastavení majitele, `MSYS_NO_PATHCONV=1` pro adb
(`C:/Users/Ondra/AppData/Local/lesnik-toolchain/sdk/platform-tools/adb`).

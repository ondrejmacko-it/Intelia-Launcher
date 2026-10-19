Pokračujeme na Intelia Launcheru — **dokončení P0 PULSE, pak P1 Onboarding V2** (`AGENTS.md` platí; česky,
stručně, ~200k tokenů, žádní agenti). Zdroj pravdy: `docs/product/MASTER_PLAN.md` (§0 vize V2 + pořadí majitele,
§14.1 etapy P0–P10), `docs/product/PULSE_RELIABILITY.md`, `docs/product/CONTROL_SYSTEM.md`, `DECISIONS.md` D28–D29.

## Stav (2026-10-19)
- Větev `claude/pulse-reliability` (pushnutá, PR otevřít/zkontrolovat; **nemergovat bez souhlasu majitele**).
- **Příčina P0 prokázaná:** `USER_PRESENT` posílá SystemUI (vlastní uid) → `RECEIVER_NOT_EXPORTED` ho nedostal →
  `locked` zůstal `true` po každém odemknutí. Oprava: exported receiver (chráněné broadcasty), stav vždy z
  `KeyguardManager`/`PowerManager` (`pulse/PulseLifecycle.kt`), omezený re-check po SCREEN_ON, `resync()` z Home,
  bezpečné `addView` s 3 retry, `PulseAccessibilityService.health` pro Studio, diagnostika `dump()`.
- Na telefonu ověřen 1 skutečný unlock (18:27:11, USER_PRESENT doručen, lens zpět). 241 testů PASS.

## Úkol 1 — dokončit P0 (PULSE_RELIABILITY §5, §7)
1. Diagnostika kdykoli: `adb shell dumpsys activity service dev.intelia.launcher.lab/dev.intelia.launcher.ui.PulseAccessibilityService`
   (health, keyguard, okno, `connects/attaches/touches/unlockChecks/lastEvent`). Jen stavy, žádný obsah.
2. Majitel udělá ≥ 20 zamknutí/odemknutí (krátké, dlouhé, rychlé) + po každém klepne na kameru; ověř
   `attaches` roste, `touches` roste, `connects` stejné (= nebylo potřeba přepínat službu).
3. Matice: Home → app → Home, Recents, hudba hraje/nehraje, rotace, kalibrace, Global ARC nad aplikací, remap kláves,
   TalkBack, MT Capsule konflikt. Výsledky do `PULSE_RELIABILITY.md` §5 a `STATUS.md`.
4. Latence touch → první frame na Lab buildu (Perfetto nebo `gfxinfo`), median/P95, žádná čísla bez měření.
5. Gesture testy P0.2 (§6). Pak PR zpráva s důkazy; dlouhodobá stabilita = zvlášť po dnech používání.

## Úkol 2 — P1 Onboarding V2 (priorita majitele hned po P0)
Nejdřív návrh (obrazovky, texty CZ/EN, stavový model, resumable po process death, vše přeskočitelné, nic povinného)
→ náhled majiteli → schválení → kód. Nahrazuje `Onboarding.kt` (3 kroky). Viz MASTER_PLAN §10.

## Otevřené otázky majiteli (D29, Blocked)
Kanál vydání (Google Play / GitHub Releases / web / F-Droid), Play účet a cesta pro accessibility deklaraci,
privacy policy URL, crash reporting (jen opt-in), produkční package `dev.intelia.launcher`, kontakt podpory.

## Build a telefon
JAVA_HOME `C:/Users/Ondra/AppData/Local/lesnik-toolchain/jdk-17.0.20.1+1`; adb
`C:/Users/Ondra/AppData/Local/lesnik-toolchain/sdk/platform-tools/adb.exe`, `MSYS_NO_PATHCONV=1`.
APK `app/build/outputs/apk/lab/app-lab.apk` (ověř čas), `install -r`, **NIKDY uninstall**, pak
`pm compile -m speed-profile -f dev.intelia.launcher.lab`. Před každým vstupem ověř top activity; PIN nikdy nezadávat
(odemyká majitel). Majitelovy vazby, FLOW, ikony, akcent, haptiku, rozložení a hudbu neměnit.

## Git
Jedna větev na úkol, žádný push do `main`, force push ani přepis historie. Nesledované `docs/screens/*` a `hs_err_*`
necommitovat. `.signing/` nikdy nečíst do výstupu ani necommitovat.

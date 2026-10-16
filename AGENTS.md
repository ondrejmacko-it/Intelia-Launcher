# Intelia Launcher — agent instructions

Applies to every coding agent and human. Read this first, then only the docs your task needs.

## What this is
A fast, energy-efficient, local-first Android home launcher written from scratch
(Kotlin + Jetpack Compose, single `:app` module, package `dev.intelia.launcher`).
It is **not** a fork: do not copy code from Kvaesitso or any other launcher. Code
in this repository is Apache-2.0; any third-party source needs a licence check
and an entry in `docs/DECISIONS.md` before it lands.

Pillars: **LENS** (one lower control: tap/swipe = search, hold = ARC),
**ARC** (five fixed quick-launch slots, hold-slide-release), **FLOW** (exact
named commands opening 1–4 apps in order, always previewed, never auto-run),
local search with diacritics-insensitive ranking.

## Layout
- `data/` — `AppRepository` (public `LauncherApps` only), `IconCache`, `SettingsStore` (one bounded JSON document).
- `flow/`, `search/` — pure Kotlin, unit tested on the JVM.
- `ui/` — Compose screens; `ArcGesture` is a pure state machine with tests.

## Rules
- One task per branch (`claude/`, `codex/`, `feature/`); never push to `main`, force push or rewrite history without the owner's approval.
- No network, accounts, telemetry, background workers, accessibility services or new permissions without an explicit decision.
- Nothing executes while typing. Multi-step FLOW requires the explicit Run button.
- Treat app labels, intents and restored settings as untrusted; keep `LauncherSettings.sanitized()` the single validation gate.
- No performance or battery claims without measurements on the target phone.

## Build and device loop
Requires JDK 17+ and an Android SDK with platform 37.1 (`local.properties` → `sdk.dir`).
```
./gradlew :app:testDebugUnitTest :app:assembleDebug
scripts/dev.sh            # install the debug build on the USB phone and open it
```
The debug build is `dev.intelia.launcher.lab`, so it coexists with other Intelia builds.

## Handoff
Report exact commands with PASS/FAIL, what was verified on the device and what was not.
Update `docs/STATUS.md` only with verified facts.

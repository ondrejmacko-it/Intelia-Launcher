# Intelia — Master Plan (after 0.1.0 Beta 1)

> **Your phone. Your rules.** Maximum customisation, minimum clutter. Everything optional is opt-in; the core works
> fast, offline and without privileged permissions.

Written 2026-10-19, **V2 merged 2026-10-19** (owner brief "Master Product Vision & Roadmap V2 — P0 PULSE reliability
first"). Status words: **Built** (in 0.1.0, see `docs/audit/BETA_0.1.0_BASELINE.md`), **Approved** (owner said yes, not
built yet), **Planned** (designed here, no code), **Research** (feasibility unknown), **Blocked** (waits for an owner
decision or an Android limit). Nothing here is a promise or a date. Each plan becomes code only after its decision
entry in `DECISIONS.md` is approved by the owner. Detail specs: [`PULSE_RELIABILITY.md`](PULSE_RELIABILITY.md) (P0),
[`CONTROL_SYSTEM.md`](CONTROL_SYSTEM.md) (Action Engine).

## 0. Vision V2 (owner, 2026-10-19 — stays until the owner changes it)
**YOUR PHONE. YOUR RULES. Maximum minimalism · maximum capability.** Intelia is meant to become one calm, elegant,
deeply customisable environment for controlling the phone: a clean Home, faster app launch than a normal launcher,
nearly every element adjustable, supported system functions from one place, own FLOW commands, global gestures and
PULSE over other apps, hardware buttons with actions, own looks, layouts and profiles — while the original Android
behaviour stays available. *Simplicity is the default; advanced options are available, never imposed.* Every
optional feature can be switched off, changed or reset.

**Owner priority order (2026-10-19, overrides the brief's P-order where they differ):**
1. **P0 PULSE reliability** — absolute priority; nothing PULSE-visual before it is proven.
2. **Onboarding V2** — complete, new and better first run (moved up from the brief's P9).
3. **Public release** — the owner wants to ship to people ("nasadit online do produkce"); channel and Play policy are
   open questions (§14, Blocked items).
4. Then the rest of the roadmap.

Brief sections and where they live here: philosophy → §0/§1 · PULSE P0 → `PULSE_RELIABILITY.md` · one Action System
→ §8 + `CONTROL_SYSTEM.md` · Capability System → §6 · PULSE 2.0+ → §5a · LENS/ARC → §5b · Home & editor → §5c · Veil
→ §3 · Design Lab → §5 · hardware keys → §7 · global control → §6/§11 · FLOW V2 → §8 · Studio V5 → §4 · scenes → §9 ·
onboarding → §10 · security → §11 · performance → §12 · roadmap → §14.

Contents: 0 Vision V2 · 1 Principles · 2 Architecture · 3 Veil V4 · 4 Studio V5 · 5 Design Lab · 6 Device capabilities & Intelia
Bridge · 7 Hardware Control · 8 Action Engine & FLOW V2 · 9 Scenes & presets · 10 Onboarding V2 · 11 Security &
privacy · 12 Performance · 13 Research backlog · 14 Roadmap R0–R12 · 15 First task.

---

## 1. Product principles
1. **Calm by default.** A new user sees wallpaper, clock, a few icons and LENS. Depth is one tap away, never in the way.
2. **One source of truth per setting.** A setting lives on one Studio page; other places link to it (search, previews).
3. **Explicit execution.** Nothing runs while typing, previewing or importing. FLOW and actions run only on an
   explicit trigger the user bound.
4. **Capabilities, not assumptions.** Every feature declares what it needs; the UI shows what works on *this* phone
   and why something does not.
5. **Local first.** No account, no network for the core. Any future cloud feature is a separate opt-in module.
6. **Measured.** No performance or battery claim without a measurement on the target phone.
7. **Own code.** Inspiration from other apps is functional only; no copied code or assets (AGENTS.md).

## 2. Architecture direction (keeps the single `:app` module until a split pays off)
- **Settings document:** stays one bounded JSON (`SettingsStore`), `sanitized()` the only gate. New areas get their own
  sub-objects with version fields (`design`, `scenes`, `capabilities`) — never parallel stores.
- **Pure core, thin Android edge:** models and decisions (`flow/`, `search/`, `action/`, geometry, `PreviewPlan`)
  remain pure Kotlin with JVM tests; Android calls live in hosts (`AndroidActionHost`, services).
- **Capability Registry** (new, R5): `Capability` enum + `CapabilityProbe` (pure decision from probed facts) +
  `Provider` order (§6). Studio, Action Picker, onboarding and FLOW read it; nobody probes Android on their own.
- **Lazy modules:** Design Lab editors, Bridge and widgets are composed / started only when opened or bound.
- **Module split trigger:** if build time or APK size makes it worthwhile, split `:core` (pure) first, then optional
  `:bridge`. Not before R6.

## 3. Design system — Intelia Veil V4 (Planned; evolves Veil 3.0, D15 / `DESIGN_SYSTEM.md`)
Keep: tinted matte glass, accent threads, fine optical edges, readable type, minimal Home, LENS/ARC/PULSE identity.
Avoid: neon, glow for its own sake, huge cards, frames around everything, duplicated options, hard cuts, blur on
every element.

| Topic | Rule |
|---|---|
| Colour roles | Semantic tokens only (`surface`, `surfaceVeil`, `onSurface`, `muted`, `faint`, `accent`, `onAccent`, `danger`, `outline`); components never use raw colours. `Ink` stays the single resolver. |
| Dynamic colour | Optional source "From wallpaper": `WallpaperManager.getWallpaperColors` (API 27+) and Material You (API 31+); derived through `AccentColors.derive` so contrast stays ≥ 4.5:1. |
| Custom accent | Built (D25): HSL sliders + hex. V4 adds RGB entry and recent colours. |
| Multi-colour palettes | `CustomColors.places` (ARC, PULSE, LENS, clock, Studio) → then full palettes (accent + secondary + surface tint). Each place: "Follows main" or own. |
| Typography | Roboto Flex UI; 6 display families for clock/date. Imported fonts (R3) only from the user's files, stored privately, licence acknowledgement shown, never re-shared in presets without the file's licence flag. |
| Icons | System / Ink / Veil / Tinted + icon packs (R3), per-app overrides, monochrome style as Intelia's own legal alternative to Nothing's look. |
| Shape & edges | 4 radii tokens (8/14/22/full); 1 px optical edge at 8–12 % alpha; no double borders. |
| Veil material | One material with 3 strengths. Blur only on large panes (Studio sheet, Search, drawer, PULSE glance); RenderEffect blur on API 31+, tinted solid fallback below and in battery saver / Reduced Motion. |
| Motion | Spring tokens (`quick`, `calm`, `expressive`), ≤ 300 ms for UI, interruptible; Reduced Motion = cross-fades or jumps. |
| Haptics | Built (D24) global strength; V4 per surface (ARC, PULSE, gestures, keys) via `HapticPrefs.per`. |
| Accessibility | 48 dp minimum targets, TalkBack labels + custom actions for every gesture, headings, pane titles, 200 % font layouts (wrap, then stack, never clip). |
| Light / dark | Both first-class; every preview renders in the chosen theme. |
| Adaptive sizes | Compact / medium / expanded widths; side-dock layouts; foldables Research. |
| Settings components | `SettingRow`, `VeilGroup`, `ChoiceRow`, `SliderRow`, `Fold`, `ConfirmDialog` only; new kinds go into `VeilKit` first. |
| Action Picker | One picker for every binding (gestures, keys, ARC slots, PULSE, FLOW steps, widgets), showing capability state. |

## 4. Studio V5 (Planned; builds on Studio Veil 3.0 + PreviewHost D23)
- **Five areas stay:** *Vzhled* (motifs, Design Lab, icons, wallpaper, type, Veil, colours) · *Plocha* (editor, presets,
  grid / free, favourites, scenes) · *Ovládání* (LENS, ARC, PULSE, gestures, keys, haptics, sensitivity) · *Akce a
  FLOW* (Action Library, automations, shortcuts, bindings) · *Systém a data* (Bridge, permissions, device, backups,
  language, about).
- **Contextual previews for every page** through `PreviewKind`: add Drawer, Flow (step simulation), Fonts, Icons,
  Wallpaper, Widgets, Haptics (safe test button), Keys (live diagnostics). Rule: a preview never executes an action.
- **Sheet:** Preview / Balanced / Settings stops stay; page declares its preferred stop (short pages → more preview).
  Handle-only drag (M0) stays; scroll never moves the sheet.
- **State:** real back stack (`StudioNav`), scroll + selection + drafts kept per page via `rememberSaveableStateHolder`.
- **Search:** every row indexed (`StudioIndex`), synonyms in both languages, test that every `SettingRow` has an entry.
- **Clean-up list from the audit:** U2 duplicate group title, U3 missing index entry, one name per concept.

## 5. Intelia Design Lab (Planned)
Shared foundation (R2): `DesignDoc` (layers, each with transform, style, data binding), command-based undo/redo,
snap guides, preset save/load, export through SAF, all inside a Studio full-screen editor. Labs are thin editors on it.

| Lab | Scope (first release → later) | Android basis / limits |
|---|---|---|
| Wallpaper Lab | Import own image (SAF, copy kept private, original untouched), crop/position, blur, tint, gradient, grain, light layers, presets → apply to Home, optionally to the system after confirmation; later procedural abstract generators (local), live wallpaper | `WallpaperManager.setBitmap` (needs `SET_WALLPAPER`, have it); live = `WallpaperService` (separate component, opt-in). Cap bitmap size to screen × 1.25; no cloud AI. |
| Clock Lab | Clock/date compositions: fonts, variable axes, weight, spacing, format, layout, colour, subtle tick animation | Compose text + `FontVariation`; animation off under Reduced Motion. |
| Icon Lab | Icon packs (ADW/Nova-style `appfilter.xml` convention), keep system icons, monochrome style, per-app image import, shape, size, labels (font, spacing, colour), global rules + exceptions | Pack discovery via `<queries>` for the pack intents; read resources of the pack package; no permission. |
| Widget Lab | Intelia's own Home widgets built from layers: text, date, clock, calendar, battery, media, images, shapes, actions, dynamic values; grid/free + snap; presets; export | Internal widgets render inside Intelia only. Hosting other apps' widgets = `AppWidgetHost` + `ACTION_APPWIDGET_BIND` consent (separate track). Publishing Intelia widgets for *other* launchers (`AppWidgetProvider`) is Research. Weather needs a data source → opt-in, offline-first. |
| Theme Lab | Full palettes, per-element colours, Veil strength, motion style, typography sets, share/export | Uses §3 tokens; one resolver. |
| Interface Lab (long term) | Intelia Keyboard, own volume panel, selected system-function panels | Keyboard = `InputMethodService` (feasible, highest privacy bar, separate opt-in). Volume panel = accessibility overlay catching volume keys (fragile per OEM). **Not possible:** replacing the secure lockscreen, Quickstep, system dialogs. |

## 5a. PULSE 2.0+ (Planned — only after P0 DoD)
States: **Hidden** (nothing visible; invisible touch target stays when Android and the state allow) · **Live** (small
capsule joined to the camera for music, timer or another relevant event; never covers system icons, never repeats
battery/alarm) · **Quick View** (compact translucent panel: one context card + a few own actions, no big settings
list) · **Global ARC** (apps / FLOW / actions from a camera gesture, over supported apps). Personalisation: action per
tap / double / triple (only if reliable) / hold / swipes, action set, Live content and allowed events, touch area
position and size, capsule look (material, opacity, colour, expand animation), haptics, timeouts, lock and over-apps
behaviour. Simple working default; advanced in details. Functions over time: media (only actions the MediaSession
offers), apps, FLOW, shortcuts, timers, quick actions, torch, screenshot, sound modes, system panels, notifications
(voluntary permission only). Built today: Home/Everywhere modes, gestures + quick menu, music capsule, Global ARC,
calibration (M3–M3.3, S3).

## 5b. LENS and ARC — one control language (Built core, Planned polish)
LENS default: tap → Search, swipe up → drawer, hold → ARC (Built). Advanced remap of more gestures (Planned) while
app access and settings recovery always stay reachable. ARC: one shallow active arc (not three big rings), real or
chosen icons, clear current choice, short label, light haptics, predictable zones, cancel by returning to LENS, run
only on a confirmed release (Built). More sets and ergonomic set switching with stable positions (Planned; if the
current end-of-arc switching fails testing, propose an alternative for approval). Global ARC may differ in geometry
but shares the Action Engine.

## 5c. Home — clean and fully editable (Built base, Planned depth)
Real Android Home with the system wallpaper, no visible app rectangle, no permanent Intelia panels. Modes **Pure ·
Essentials · Custom**. Home Editor (Built: long-press, slots, editor bar): add / remove / move / resize / restyle /
icons / labels / opacity / per-component toggle / behaviour / save / undo. **Grid / Free** (Planned): classic grid,
snap, free placement, optional guides, alignment, own grid size, relative positions per display; layouts may never
cover navigation areas or leave the screen. Elements over time: apps, shortcuts, FLOW, clock, date, LENS, texts,
shapes, internal widgets, system widgets, groups, action buttons. Internal widgets and third-party widget hosting are
separate stages with separate designs.

## 6. Device capabilities and Intelia Bridge (Planned + Research)
**Provider order per capability:** `Action (in-app) → Android public API → Accessibility → Shizuku (external) →
Intelia Bridge → Root → Unavailable`. The registry picks the first provider that is *available and enabled by the
user*; the UI names it ("via Android", "needs PULSE service", "needs Shizuku").

| Option | What it is | Pros | Cons / risks |
|---|---|---|---|
| A Native Android | Public APIs (`LauncherApps`, `MediaSession`, intents, panels) | Safe, Play-friendly | Limited (no toggling Wi-Fi etc. since API 29) |
| B Accessibility | Our existing PULSE service: overlay, key filter, global actions (lock, screenshot, notifications) | Already approved (D9, D20a, D22) | Play policy scrutiny; OEM kills; must stay transparent, no content reading |
| C External Shizuku | User runs official Shizuku; Intelia uses its API (Apache-2.0 library) | Proven, no own privileged code | Extra app; restart after reboot (wireless debugging) |
| D Integrated Bridge | Intelia starts its own helper via `app_process` from an ADB shell (pairing through Wireless Debugging, Android 11+) | One app for power users | Large security surface; must re-start after reboot; OEM differences; Play risk; needs ADB pairing implementation (licence check of any library) |
| E Root | Only if `su` exists and the user enables it | Full power | Rare; never assumed |

Known limits: Shizuku via Wireless Debugging must be restarted after every reboot (F4); some banking apps refuse to run
while any accessibility service is on (F3). Rules: no automatic privilege escalation; ADB shell ≠ root; Bridge exposes a **fixed allow-list of typed commands**
(e.g. `setWifi(Boolean)`, `grantWriteSecureSettings` for self only) — never a free shell, never callable from
imported/untrusted FLOW without a per-action confirmation; visible status + one-tap off; everything still works
without it. Recommendation: **C first** (R6), D only after C proves which commands users actually need.

## 7. Hardware Control Lab (Planned; extends D20/D20a, inspired functionally by KEA / Key Mapper)
- **Device profile:** manufacturer, model, Android version, declared features, *observed* key events (diagnostics),
  Bridge state, permissions; manual override. A model-based guess is shown as "expected", never "available".
- **Keys:** volume ±, Essential Key (only if events actually reach the service — unverified on Phone (3)), external
  keyboards/buttons. Power key is not interceptable by apps.
- **Patterns:** single, double, triple, long, double+hold — pure `KeyPattern` recogniser with configurable timing,
  JVM-tested; default **passthrough** keeps the system function unless the user explicitly replaces it.
- **Assignment:** the common Action Picker (app, FLOW, ARC, PULSE, search, system action, shortcut, widget set).
- **Lock screen:** off by default; per binding opt-in, never for actions exposing private data.
- **Safety:** "panic" — holding both volume keys 5 s disables all remaps; remaps auto-disable if the service crashes
  repeatedly.

## 8. Action Library and FLOW V2 (Planned; extends `action/Action.kt`)
**Action contract:** stable id, name, icon, description, typed parameters, `requirement` (capability), provider,
fallback, safe result (`ActionResult` with reason), tests. Categories: apps, app shortcuts, navigation, media, sound,
display, camera, notifications, network (panels), system settings pages, Bluetooth/devices (panels), Nothing-specific
(Research: Glyph etc. only via public APIs), widgets, LENS, ARC, PULSE, gestures, keys, FLOW, scenes, links, custom.

**FLOW V2:** steps (actions) · wait (bounded) · confirm step · condition (time, charging, headphones, scene — only
locally readable facts) · branch · repeat (hard cap, e.g. 10) · reusable sub-FLOW · trigger bindings (gesture, key,
ARC, PULSE, LENS name) · error policy (stop / skip / ask) · visual editor · **dry-run simulation** (records what would
happen, executes nothing) · debug log (local, redacted) · import/export. Existing simple aliases stay valid as
"FLOW V1 = sequence of app steps" (migration test). FLOW never becomes an autonomous agent operating other apps.

## 9. Scenes and presets (Planned)
**Scene** = named overlay on settings: wallpaper, Home layout, widgets, clock, icons, ARC sets, PULSE actions,
gestures, FLOW bindings, accent, haptics, look. Stored as a diff against the base so switching back is exact. Switch:
manually (Studio, LENS name, PULSE action, FLOW step) or by user-approved local rules (time, charging). A scene never
changes system settings by itself; "Back to previous scene" always available.

**Preset format** (`intelia-preset`, JSON, versioned): kinds = theme, clock, widget, wallpaper, icons, ARC set, PULSE,
FLOW, scene, full config. Import = parse → sanitise → **preview** → apply with undo. Missing apps become placeholders,
geometry is stored in dp/fractions, images carry a licence flag, imported actions arrive **disabled** until the user
reviews them. Online community library = optional future stage, not a dependency.

## 10. Onboarding V2 (Approved as next after P0, owner 2026-10-19; replaces 3-step `Onboarding.kt`)
Brief V2 order (short, clear, optional): welcome → look → Home Pure / Essentials / Custom → favourite apps → LENS and
ARC → optional PULSE → permissions with clear explanation → done + short interactive tutorial. Advanced control, key
remaps etc. can be switched on later; nothing is required for the launcher to work. Detailed step list:
1. **Welcome:** restore backup · (sign in only if sync ever exists) · start without account (default).
2. **Device:** detected profile, confirm or change; capability scan (no prompts yet).
3. **Permissions:** default home, PULSE service, notification access, Shizuku/Bridge — each with *what it unlocks,
   what stops working without it, what keeps working, safety note, state, Skip*.
4. **Look:** motif, colour, wallpaper, icons, clock, type — live preview.
5. **Home:** preset, favourites, LENS position.
6. **Controls:** ARC, PULSE, gestures, available keys — overview, optional.
7. **Done:** 30-second practical tutorial (tap/swipe/hold LENS), "Studio is always here".
Every step skippable; resumable after process death; works without any permission.

## 11. Security, privacy and compatibility (Planned spec; current rules in AGENTS.md)
- **Permission Center** (Studio › Systém a data): every grant, what uses it, last use, link to revoke.
- Normal vs privileged split: privileged features live behind the Capability Registry and are listed separately.
- Keys / notifications / keyboard: never log content; the keyboard (if ever) has no network permission, no history
  by default, password fields bypass suggestions entirely.
- Bridge: allow-list, caller check (own UID only), no shell from FLOW, visible running indicator, auto-stop.
- Imports: sanitised, previewed, actions disabled until reviewed, size limits (as `BackupCodec`).
- Backups: local SAF files; optional encryption with a user password (Research). Cloud sync (D19) stays proposed.
- Signing keys: release key only on the owner's machine + offline backups (D26); CI signing only via encrypted secrets
  if CI is ever added.
- Compatibility: test matrix per release (Android 12–16, Pixel + Samsung + Nothing at minimum before 1.0); Play policy
  review before any store listing (accessibility use declaration, no Bridge in a Play build if disallowed).

## 12. Performance and battery (Planned; baseline first)
Targets are set **after** measuring 0.1.0 on Phone (3) with the Lab build:
| Metric | Tool | Target rule |
|---|---|---|
| Cold start / Home resume | Macrobenchmark `StartupTimingMetric` | no regression > 10 % per release |
| Jank: drawer, Studio sheet, ARC | `FrameTimingMetric`, `gfxinfo` | P90 frame < 16.7 ms at 60 Hz |
| Search latency | JVM benchmark of `AppSearch` + on-device trace | < 16 ms for 300 apps |
| ARC / Global ARC gesture latency | Perfetto trace touch→frame | measured, then −20 % goal |
| Idle CPU / battery | `dumpsys batterystats`, 1 h idle Home | no wakelocks, no alarms, zero background work |
| Memory | `dumpsys meminfo` | baseline + budget per module |
Practices: lazy init, event-driven updates, no polling, Baseline Profiles (in place via `profileinstaller`, D16),
Compose stability checks, blur fallback on weak devices.

## 13. Research
First round done 2026-10-19: [`docs/research/COMPETITOR_RESEARCH.md`](../research/COMPETITOR_RESEARCH.md) — Quickstep limits
(F1), Essential Key remapping on Nothing (F2), banking apps vs accessibility (F3), Shizuku reboot behaviour (F4), Google
Play accessibility rules (F5). Consequences are folded into §6, §7, §10 and §11. Still open (R0.5): full competitor and
community review (Niagara, Nova, Smart Launcher, Lawnchair, Kvaesitso, Total Launcher, Square Home, KWGT/KLWP,
Material Capsule, Key Mapper, MacroDroid, Tasker, Reddit / GitHub issues) and a re-check of Android API facts in §5–§7.
Credits for functional inspiration: [`CREDITS.md`](../../CREDITS.md).

## 14. Roadmap
### 14.1 V2 stages (current order — owner priority from §0 applied)
| Stage | Scope | Status | Maps to | Gate |
|---|---|---|---|---|
| **P0 PULSE Reliability** | lifecycle fix, diagnostics, tests, ≥ 20 lock/unlock on device, latency baseline | **in progress** — cause proven, fix built (2026-10-19) | — | `PULSE_RELIABILITY.md` §7 |
| **P1 Onboarding V2** | new complete first run (§10): welcome, look, Pure/Essentials/Custom, favourites, LENS+ARC, optional PULSE, permissions explained, short interactive tutorial | Approved (owner 2026-10-19), Planned design | R10 (moved up) | design preview approved by owner before code |
| **P1.5 Public release** | distribution channel, signing, privacy policy, Play accessibility declaration or sideload page, crash-free beta | **Blocked** on owner answers (channel, store account) | R12 part | D-entry approved |
| **P2 Control System Hardening** | Action Engine, bindings, Capability Registry, honest unavailable states | Planned | R5, R8 part | `CONTROL_SYSTEM.md` |
| **P3 UX & Veil V4** | one visual language; preview + owner approval first | Planned | §3 | owner approval |
| **P4 Studio V5** | navigation, one place per setting, capability-aware UI | ~60 % (R1) | R1 | |
| **P5 Home & Design Lab** | free editor, grid/free, icons, clock, wallpaper, widgets | Planned | R2–R4 | |
| **P6 PULSE & ARC advanced UX** | §5a/§5b, only after P0 DoD | Planned | — | P0 done |
| **P7 Hardware Control** | key detection tool, Essential Key (if events arrive), patterns, picker, fallbacks | Planned | R7 | |
| **P8 FLOW V2 & Scenes** | multi-step, conditions, dry run, profiles, presets, import/export | Planned | R8, R9 | |
| **P9 Advanced capabilities** | Shizuku / Bridge research, only with an approved security design | Research | R6, R11 | security D-entry |
| **P10 Release quality** | stability, compatibility, accessibility, performance, regression matrix | Planned | R12 | |
Each stage must state: scope · acceptance criteria · technical risk · data/permission impact · test plan · real
results · handoff (`NEXT_SESSION.md`). New permission, sensitive system change or big UX decision → owner approval first.

### 14.2 Original R-stages (detail, still valid)
Order optimised for dependencies: the Capability Registry (R5) moves before the Labs that need it only partly; Studio
V5 first because every later feature lands in Studio. Sizes: S ≈ 1 session, M ≈ 2–3, L ≈ 4–6, XL > 6 (~200k tokens each).

| Stage | Goal / user value | Ready | Depends on | Risk | Permissions | Size | DoD (short) |
|---|---|---|---|---|---|---|---|
| **R0** Beta stabilisation | 0.1.0-beta.1 released, baseline | done (signed prerelease 2026-10-19) | — | low | — | S | signed APK published, U1 checked |
| **R0.5** Research | Facts for R3–R8 decisions | first round (F1–F5) | — | low | — | S | research doc with sources |
| **R1** Studio V5 | Every page clear, previewed, searchable | ~60 % (PreviewHost, nav, sheet) | R0 | low | — | M | all pages have `PreviewKind`, index test, U2/U3 fixed, back stack + state tests |
| **R2** Design Lab foundation | One editor model for all labs | 0 % | R1 | medium | — | M | `DesignDoc`, undo/redo, snap, presets, JVM tests |
| **R3** Wallpaper / Clock / Icon Lab | Creative customisation without privileges | wallpapers, fonts, icon styles exist | R2 | medium (icon packs) | none new | L | import + crop + effects, clock compositions, icon packs + overrides, device-verified |
| **R4** Widget Lab | Own widgets; host other apps' widgets | `Widget` item reserved | R2, R3 | medium-high | bind consent | L | internal widgets + AppWidgetHost track |
| **R5** Device capabilities | Show what works on this phone | key diagnostics exist | R1 | low | — | M | registry + probe tests, Studio device page, Action Picker states |
| **R6** Intelia Bridge | Power features via Shizuku, Bridge feasibility | 0 % | R5, R0.5 | high | Shizuku | L | Shizuku provider + allow-list; Bridge go/no-go decision |
| **R7** Hardware Control | Key patterns and assignments | remap exists (D20a) | R5 | medium | accessibility | M | pattern recogniser tests, passthrough default, panic off |
| **R8** Action Engine / FLOW V2 | Rich actions, safe automation | 13 actions | R5 (R6 for privileged) | medium | per action | L | action contract, dry run, editor, V1 migration |
| **R9** Scenes & presets | Profiles and sharing | backup codec | R3, R8 | medium | — | M | scene diff model, preset import preview + undo |
| **R10** Onboarding V2 | Good first run | basic 3-step | R5, R9 | low | — | M | 7 steps, skippable, resumable |
| **R11** Advanced interface | Keyboard, volume panel, Nothing-specific | 0 % | R6, R7 | high | IME / a11y | XL | each module separate decision + opt-in |
| **R12** Release hardening | Wider distribution | partial benchmarks | all | medium | — | M | benchmarks vs baseline, a11y pass, matrix, Play review |

Per stage, the implementing session must also keep working: LENS pillars, explicit FLOW execution, `sanitized()` as the
only gate, backup compatibility, the owner's settings on the phone. Never rewrite: Action Engine contracts, settings
document shape (only additive with migration), ArcGesture state machine (extend, don't replace).

## 15. First task (next session)
**Finish P0** (device lock/unlock matrix, latency baseline, PR), then **P1 Onboarding V2 design** — see
`docs/NEXT_SESSION.md`. The previous R1 step 1 tasks (U1–U3) stay open inside P4.

# Intelia 0.1.0 Beta 1 — baseline audit

What exists in the code at `0.1.0-beta.1` (2026-10-19), so the next agent does not rediscover it. Status:
**Full** = implemented and verified on the device · **Partial** = works with gaps · **UI only** / **Reserved** = no
behaviour yet. "Verified" points to `docs/STATUS.md` entries. All data lives in one JSON document
(`data/SettingsStore.kt`, validated only by `LauncherSettings.sanitized()`); nothing leaves the phone.

## Categories (do not mix them up)
1. **Implemented and tested** — this file and `STATUS.md`.
2. **Approved decisions** — `DECISIONS.md` (D1–D26). D5's "PULSE on Home only" and "three fixed ARC rings" are
   **superseded**: PULSE everywhere is D9/D22 (opt-in service), ARC sets are Control System V1 (M2 ARC V2).
3. **Superseded designs** — `REDESIGN_BRIEF.md`, `DESIGN_HANDOFF.md`, `docs/design/*-prototype.html` (Crystal, Veil 2)
   are history; Veil 3.0 (D15, `DESIGN_SYSTEM.md`) is current.
4. **Future** — `docs/product/MASTER_PLAN.md`, `KEA_PARITY.md`.

## Feature inventory

| Feature | What / where | Data & defaults | Permissions / API | Status | Key files · tests |
|---|---|---|---|---|---|
| Home | Wallpaper-only Home, clock + date, dock row with LENS, favourites, presets (Essentials default) | `layout`, `showClock/Date`, `clockStyle` | `LauncherApps`, HOME role (optional) | Full | `HomeRoot`, `HomeScene`, `HomeModel` · `DockTest` |
| LENS | Tap search, swipe up drawer, hold ARC; movable to any edge in the editor | locked pillars in `Controls` | — | Full | `Lens.kt` · `LensFlickTest` |
| ARC | Hold-slide-release; named sets (3 on the test phone), apps / FLOW / shortcuts, set arrows, cancel on LENS | `controls.arcSets` (+ legacy `arc*`) | shortcuts need HOME role (`LauncherApps.getShortcuts`) | Full | `ArcGesture`, `CrystalArcGeometry` · `ArcBowTest`, `CrystalArcGeometryTest` |
| Global ARC | Hold the camera area over any app → ARC under the camera | own set binding | PULSE accessibility service (D22) | Full on Phone (3); other OEMs unverified | `GlobalArc.kt` · `GlobalArcGeometryTest` |
| PULSE | Capsule at the camera: charge, near alarm, ring on return, glance panel, Quick Actions, gestures, calibration (0.5 dp), width/opacity | `pulse`, calibration, quick actions | Home: none; everywhere: accessibility overlay (D9) | Full (Home + service) | `Pulse*.kt`, `PulseAccessibilityService` · `PulseMenuTest`, `PulseSwipeTest`, `PulseGesturesTest` |
| Music capsule | Now-playing in PULSE, play/pause/next/prev | none stored | notification access → `MediaSessionManager` (D21) | Full | `pulse/MediaWatch`, `NowPlaying` · `NowPlayingTest` |
| Search | Local, diacritics-insensitive ranking; FLOW preview; Enter runs exact FLOW only | none | — | Full | `search/AppSearch`, `Search.kt` |
| Drawer | Alphabetical list, favourites strip, app options (hide, info, favourite) | `hidden`, `favorites` (max 7) | — | Full | `Search.kt`, `AppRepository` |
| FLOW | Named alias → 1–4 apps in order; explicit run only | `aliases` | — | Full | `flow/Flow.kt`, `FlowPages.kt` |
| Action Engine | 13 action types (app, FLOW, search, shortcut, media, torch, screenshot, sound mode, system panel, card, lock, alarms…) with requirement + result reasons | in bindings | some need PULSE service (lock, screenshot) | Full for the listed types | `action/Action.kt` · `ActionTest` |
| Gestures | Home (swipe up/down/sides, hold), LENS, PULSE gestures, rebindable via Action Picker | `controls.bindings` | — / service for PULSE outside Home | Full | `action/Controls.kt`, `ControlsPage` · `ControlsTest` |
| Hardware keys | Diagnostics (foreground), remap while Intelia is in front, everywhere with the service (D20/D20a) | `controls.keys` | accessibility key filtering | Partial — only keys Android delivers; Essential Key not guaranteed | `KeyRemap`, `KeyProbe` · `KeyRemapTest`, `KeyProbeTest` |
| Studio | 5 areas, settings search to the row, back stack, 3-stop sheet (Preview / Balanced / Settings) | none | — | Full | `Studio*.kt` · `StudioNavTest`, `StudioIndexTest` |
| PreviewHost | Non-interactive live previews: Home, Arc, Global ARC, Lens, Search, Pulse | none | — | Full (D23) | `PreviewHost.kt` · `PreviewPlanTest` |
| Home Editor | Grid / free / hybrid, presets, inspectors (icon, label, clock, date, LENS), undo/redo, Globální/Vlastní inheritance | `layout` | — | Full | `HomeEditor*`, `HomeInspector` · `HomeEditorStateTest`, `InheritanceTest` |
| Themes & colours | System/light/dark, preset accents, per-place palette, one custom accent with contrast derivation (D25) | `theme`, `accent`, `custom` | — | Full | `Ink.kt`, `AccentColors`, `CustomColors` · `HapticAndCustomColorTest`, `VeilTest` |
| Fonts | Roboto Flex UI + 6 OFL families for clock/date | per item | — | Full (D11, D14) | `res/font` · `FontCoverageTest` |
| Icons & labels | System / Ink / Veil / Tinted styles, disc shapes, label defaults with exceptions | `iconStyle`, look | — | Full; **no icon packs** | `IconCache`, `IconLabelControls` |
| Wallpapers | Intelia wallpapers + dim; save as system wallpaper after confirmation | look | `SET_WALLPAPER` | Full; no import of own images | `Wallpaper*.kt` · `WallpaperExportTest` |
| Widgets | `HomeItem.Widget` reserved for an `AppWidgetHost` id | — | — | **Reserved, nothing renders** | `HomeModel.kt:19` |
| Haptics | Strength 0–100 % or 4 stops (fallback), follows Android's touch-feedback switch | `haptic` | `VIBRATE` (D24) | Partial — amplitude path unverified on a device | `HapticPrefs`, `HapticEngine` |
| Motion & a11y | Reduced motion, text scale, 48 dp targets, TalkBack actions, pane titles | `reduceMotion`, `motion`, `textScale` | — | Partial — TalkBack speech never verified | `Motion.kt` · `MotionTest` |
| Onboarding | 3 steps: what Intelia is, how much Home shows, gestures + "set as home" | `onboarded` | HOME role request | Full but basic (V2 planned) | `Onboarding.kt` |
| Backup | Export / restore one JSON via SAF; undo until Studio closes; size & version checks | file | none (SAF) | Full (D17) | `BackupCodec` |
| Permissions page | Default home, PULSE service, notification access — states + links to Android pages | — | — | Full; U1 state glitch | `Studio.kt` `SystemPage` |
| Language | EN (`values`) + CS (`values-cs`), Android 13+ per-app language row | system | `localeConfig` | Full; English UI not walked on device | D26 |

## Measured baseline (only what exists)
- Startup after reboot, drawer jank A/B and `gfxinfo` motion runs: see STATUS 2026-10-17 entries (B6, startup). No
  battery, idle-CPU or memory baseline exists yet → first task of R12 / measurement plan in the master plan.
- APK sizes: Lab 3.80 MB, release (unsigned) 3.80 MB.

## Screens
`docs/screens/v0.1.0-beta.1/` (3 new: ARC, Studio, language) + the full sweep `docs/screens/v0.1.0-2026-10-17c/`
(Home, Search, drawer, editor, every Studio page of that day). PULSE states, Action Picker, calibration and haptics
screenshots of the current build were **not** retaken this session.

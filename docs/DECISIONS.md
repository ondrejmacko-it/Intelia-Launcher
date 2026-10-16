# Decisions

## D1 — 2026-10-15 — Standalone codebase instead of a Kvaesitso fork
The previous Intelia work lived inside a Kvaesitso fork (GPL-3.0, ~60 modules). The owner chose a
clean repository built our own way. The core is new: public `LauncherApps` for apps and icons,
one JSON settings document, pure-Kotlin FLOW/search. No upstream source is copied; only our own
Intelia concepts and UI geometry were re-implemented. Licence: Apache-2.0.

## D2 — 2026-10-15 — Single `:app` module
Logic that needs tests (`flow/`, `search/`, `ui/ArcGesture`, settings codec) is pure Kotlin
inside `:app` and runs as JVM unit tests. Split into modules only when build time or reuse
justifies it.

## D3 — 2026-10-15 — Debug package `dev.intelia.launcher.dev`
The test phone already runs the older fork build as `dev.intelia.launcher.debug` (current HOME).
The new debug build uses a different suffix so installing it never replaces that app or its data.

## D4 — 2026-10-16 — Shared debug signing key, debug package `dev.intelia.launcher.lab`
The `.dev` build on the phone was signed by the old PC's per-user debug key, which no longer exists, so it
cannot be updated. A debug-only key now lives in `keystore/debug.keystore` (well-known `android` passwords,
never used for release) and every PC signs debug builds identically. The debug package moved to `.lab`
(label "Intelia Lab") and installs beside `.dev` without touching it or its data. Revert: point
`signingConfigs.debug` back to the default key and restore the suffix in `app/build.gradle.kts`.

## D5 — 2026-10-16 — Ink rework: explicit FLOW triggers, three ARC rings, PULSE on Home only
- **Explicit FLOW run.** A FLOW still never runs while typing. Three gestures count as an explicit run:
  the *Spustit* button, Enter in search when the query is an exact FLOW name, and releasing on a FLOW
  slot in ARC ring 2.
- **Swipe up on LENS opens the app drawer**; tap opens search with the keyboard.
- **ARC has up to three rings** (`arcRings`, default 2): ring 1 = apps (5 slots), ring 2 = FLOW (7),
  ring 3 = app shortcuts (7, `package/id`). Shortcuts come from `LauncherApps` and are only available
  while Intelia is the default home, so ring 3 and the Studio shortcut picker work only then.
- **PULSE lives on Home only** — a capsule around the camera (charging, near alarm) and a ring echoing out of the
  camera when Home returns. It is not drawn over other apps and adds no permission or overlay.
- **Visual language "Prstenec a nit"** (ring and thread): wallpaper-only Home, controls drawn as rings
  and thin lines, system fonts (bundled fonts need a separate decision).
- **Camera calibration.** `pulseLens = null` means automatic detection from the display cutout
  (`DisplayCutout.cutoutPath`); a saved value is a manual override adjusted in 0.5 dp steps, clamped by
  `LauncherSettings.sanitized()`, with non-finite values falling back to automatic.

## D6 — 2026-10-16 — ARC ring 2 fills from the centre outwards
FLOW slots in ring 2 used to fill strictly from the left, so one or two FLOWs sat at the far left edge.
Ring 2 now places the n-th FLOW of `arcFlows` into the n-th slot from the centre: centre, then left
and right alternately (7 slots: 3, 2, 4, 1, 5, 0, 6). The first FLOW sits straight above LENS. Slots
stay fixed and the stored order is unchanged, so no migration is needed; only the mapping in
`HomeViewModel.arcFlows` (`fillFromCentre`) moved. Rings 1 and 3 keep left-to-right order.
Revert: map `arcFlows` index-to-slot again.

## D7 — 2026-10-16 — PULSE capsule width and opacity
Studio → PULSE gets two thread-and-bead sliders, as in MT Capsule: *Šířka kapsle* adds 0–40 dp on each
side of the camera (2 dp steps, default 0) and *Neprůhlednost kapsle* sets the background to 40–100 %
(5 % steps, default 100); text stays opaque. The 40 % floor keeps text readable over a light wallpaper.
Both are `LauncherSettings` fields clamped in `sanitized()`. While PULSE is on, Studio draws the real
capsule at the camera in preview mode (always shown as charging, no echo, no panel, no touch) so changes
are visible while dragging. Revert: drop `pulseWidth`/`pulseOpacity`; old documents decode unchanged.

## D8 — 2026-10-16 — PULSE capsule sits in the status bar and shows no text at rest
The D7 capsule showed a bell + "10:00" on the left and "90%" on the right and covered the system alarm
icon. Both facts are already in the status bar, so at rest PULSE now only adds what the system lacks:
- **Charging:** a disc around the camera; the ring on the lens rim is the battery gauge (terracotta arc).
- **Alarm within `pulseAlarmHours`:** the same disc plus one terracotta dot to the right of the lens.
- **Both:** gauge and dot. **Neither:** nothing (only the echo when Home returns).
- **Size:** height = lens diameter + 2 × 6 dp, capped at `WindowInsets.statusBars` top − 2 dp and kept
  inside the status bar; width = height, + 10 dp for the dot (on the right only, the lens stays centred).
  On Phone (3) that is 28 × 28 dp (38 dp with the dot), x 191.5–229.5 dp; status icons end ≈145 dp and
  start ≈290 dp. Pure logic in `ui/PulseShape.kt` (`restCapsule`, `pulseRest`), JVM tested.
- **Tap** on the lens opens the existing `PulsePanel` from the camera downwards (alarm
  time and day, battery % and state, torch); tap outside or Back closes it. TalkBack reads the rest state.
  The status bar window takes touches inside it, so the target is 64 dp wide and reaches 28 dp below the
  status bar. The panel's top strip is as tall as the status bar and carries no text (system icons draw there).
- **D7:** the width slider is removed (`pulseWidth` dropped; old documents still decode because unknown
  keys are ignored). Extra width would only cover status icons. The opacity slider stays.
- MT Capsule was only looked at: with its service on it drew no window at rest while charging, so there
  was nothing to copy; this design is ours ("ring and thread", terracotta, system fonts).
Revert: restore the D7 capsule body in `Pulse.kt` and `pulseWidth`.

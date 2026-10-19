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
The test phone already runs the older fork build as `dev.intelia.launcher.test` (current HOME).
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

## D9 — 2026-10-16 — PULSE everywhere: an opt-in accessibility service with one lens overlay
A tap on the camera lens should always do something, also in other apps. Touches inside the status bar
go to SystemUI, so the only window that can sit on the lens is `TYPE_ACCESSIBILITY_OVERLAY`, which needs
an accessibility service. MT Capsule and the old `.debug` fork do the same; nothing is copied from either.
- **Service:** `ui/PulseAccessibilityService`, off by default. Studio → PULSE → *PULSE everywhere* opens the
  system accessibility settings; the owner switches it on there. Intelia never enables it itself.
- **Scope:** `canRetrieveWindowContent=false`, no `accessibilityEventTypes` (it receives no events), no key
  filtering, no gestures, no network, no new permissions in the manifest. It reads nothing on screen.
- **Windows:** one overlay on the lens (square, lens diameter + 2 × 10 dp, at least 32 dp, from the cutout
  path or the Studio calibration); a tap adds a full-screen overlay with the menu growing down from the
  lens. Tap outside, *Collapse* or Back closes it and removes that window again.
- **Menu and the API behind each control:**
  - alarm time → `AlarmManager.nextAlarmClock`, tap opens `AlarmClock.ACTION_SHOW_ALARMS`;
  - battery % and state → sticky `ACTION_BATTERY_CHANGED`;
  - previous / play-pause / next → `AudioManager.dispatchMediaKeyEvent` (goes to the active session; no
    title or artwork, because `MediaSessionManager.getActiveSessions` needs notification access, which the
    old `.debug` fork asked for and we do not);
  - volume −/+ and level → `AudioManager.adjustStreamVolume(STREAM_MUSIC)`; sound ↔ vibrate →
    `AudioManager.setRingerMode` (silent is left out: it toggles Do Not Disturb, which needs notification
    policy access);
  - torch → `CameraManager.setTorchMode`;
  - screenshot, lock, notifications, quick settings → `AccessibilityService.performGlobalAction`
    (screenshot and lock need API 28; the menu closes first so it is not in the picture);
  - links → `Settings.Panel` Wi-Fi / internet (API 29) and the Bluetooth, sound and display settings.
- **Lab Home:** the existing in-app panel stays. While the service runs, Lab does not draw its own tap target
  so the two never fight over the same touch.
- Brightness slider, contacts, barcodes, notification content and app shortcuts (all in MT Capsule) are out:
  they need `WRITE_SETTINGS`, contacts, camera or notification access.
Revert: remove the `<service>` entry and `PulseAccessibilityService`; settings are unchanged.

## D10 — 2026-10-16 — Redesign direction: Quiet Crystal (APPROVED by the owner 2026-10-16)
Phase A of `docs/REDESIGN_BRIEF.md`. Full spec in `docs/DESIGN_SYSTEM.md`, prototype in
`docs/design/crystal-prototype.html` (published privately as https://claude.ai/artifact/BPhBEVCpmzktdFPXDGDEta).
- **Direction A "Quiet Crystal"** over B "Matte Ambient" and C "Minimal System": empty at rest, one matte
  material with a 1 dp light edge, no blur, one font family (Roboto Flex, OFL — licence entry needed before
  bundling), frost-blue accent by default; terracotta stays as an accent option for existing users.
- **ARC = one low arc, 5 fixed slots per set, up to 4 sets; the arc's two ends switch sets** (option C of
  the brief). Rejected A (distance bands): the second band sits ~190 dp above LENS, out of thumb reach.
  Rejected plain B without a switch gesture: needs a second control.
- **PULSE 2.0**: idle draws nothing; live capsule from the camera; tap = context or quick set; hold =
  Global ARC with the same model; the half-screen quick-settings menu of D9 retires.
- **One Action Engine**: a sealed `Action` type with `isAvailable`/`run` returning `Done | Opened |
  Unavailable`, plus bindings per surface. No new permissions for defaults; notification access only as a
  later opt-in decision.
- Settings migration (ARC rings → sets, accent, PULSE toggles) in `LauncherSettings.sanitized()` with tests.
Owner approved direction A, arc-end set switching and PULSE 2.0 on 2026-10-16; phase B1 may start.

## D11 — 2026-10-16 — Bundle Roboto Flex (SIL OFL 1.1)
Phase B1 of D10. The UI family of the Crystal design is Roboto Flex, bundled unmodified as one variable font.
- **Source:** `ofl/robotoflex/RobotoFlex[GRAD,XOPQ,XTRA,YOPQ,YTAS,YTDE,YTFI,YTLC,YTUC,opsz,slnt,wdth,wght].ttf`
  from https://github.com/google/fonts (main, fetched 2026-10-16), stored as `app/src/main/res/font/roboto_flex.ttf`
  (1 787 292 bytes). Copyright 2017 The Roboto Flex Project Authors.
- **Licence:** SIL Open Font License 1.1. Bundling in an Apache-2.0 app is allowed; the font may not be sold
  on its own and a modified font must not use a Reserved Font Name. We do not modify or subset it. The licence
  text ships in the APK as `assets/licenses/RobotoFlex-OFL.txt`.
- **Cost:** about +1.7 MB in the APK. Subsetting (Latin + Latin Extended for Czech) can come later; a subset is
  a modified font, which OFL allows under a different name.
- **Use:** width and weight axes through `FontVariation.Settings` (API 26+, equal to `minSdk`), see `ui/Crystal.kt`.
Revert: delete the font and licence asset; `CrystalType` falls back to `FontFamily.SansSerif`.

## D12 — 2026-10-16 — Intelia Veil material and Home Studio V2 (PROPOSED, awaiting owner approval)
Phase A of the owner's "Intelia Veil & Home Studio V2" brief. Spec: `docs/DESIGN_SYSTEM.md` §5; prototype
`docs/design/veil-prototype.html` (private artifact https://claude.ai/artifact/5oVfKwJrveYy1d9YkZN1Ss).
- **Material:** Clear / Veil (default) / Frosted / Solid with one token set; the signature is the *origin edge*,
  a 1 dp accent-tinted outline lit only where the surface came from. Revises D10's "no blur": blur is allowed
  only on transient surfaces and only where it is real.
- **Blur truth on Nothing Phone (3):** `ro.surface_flinger.supports_background_blur=0`, `mBlurEnabled=false`,
  live wallpaper (Atmosphere). Wallpaper blur is impossible there; Frosted blurs only Intelia's own Home layer
  (API 31+) and uses a denser Veil. Devices with cross-window blur get real window blur with a live fallback.
- **Home layout model:** layouts with Grid / Free / Hybrid placement, anchors (cell, safe-area fractions, LENS
  row, LENS), built-in presets as templates (Pure, Essentials, Balanced), user layouts as copies, Widget kind
  reserved. Schema 1 → 2 migration without data loss.
- **Home Editor** on Home itself (long-press), one inspector per element, undo/redo, atomic save on ✓.
- **LENS touch target 84 × 100 dp** (fixes the overlap with the inner Essentials).
- **LENS → Search** morph from the LENS ring, field riding the IME.
Rejected: a blur imitation presented as blur; a separate full-screen editor page; storing absolute px positions.
Open: does the Atmosphere wallpaper honour `setWallpaperZoomOut`; icon-pack support needs its own entry.
**Approved by the owner 2026-10-16** with changes: calmer matte panels in three densities (Veil Light / Dense ~75–85 % / Focus ~88–95 %), progressive-disclosure inspectors that never cover the edited element, preset thumbnails, LENS 84 × 100 dp only after a phone test; ARC, PULSE 2.0 and FLOW untouched now, widgets reserved only.

## D13 — 2026-10-17 — Customisation programme: glass styles, wallpapers, fonts (APPROVED by the owner 2026-10-17)
Owner asked for the widest customisation: glass styles that drive every surface, edgeless panels, clock/date
typography and orientation, icon shapes (squircle / rounded square), a movable dock that can sit on a side, and
10+ minimal blurred wallpapers.
Decided by the owner:
- **Wallpapers: both** — drawn by the launcher (procedural, no permission, no assets) and optionally saved as the
  system wallpaper, which needs `android.permission.SET_WALLPAPER` (normal permission, changes the lock screen too
  when chosen). The permission lands only with that feature, in its own commit.
- **Fonts: bundle 4–6 SIL OFL 1.1 families** for clock and date (≈ 1 MB), each with its licence file in the APK
  assets and listed here before it lands (same rule as D11).
- **Order:** glass styles + settings audit first (done 2026-10-17), then clock/date, dock/LENS anywhere + icon
  shapes, wallpapers.
Settled 2026-10-17 (owner): ARC from a side or top row opens towards the screen centre; the row is part of each
layout (`LayoutSpec.dock`), icon disc shape is global (`LauncherSettings.iconShape`); "order of the four slots" =
LENS first / between / last (swapping slots was already a drag). Done: ③ (DESIGN_SYSTEM §5.5.3, §5.8).
2026-10-17 (B3): ④ drawn wallpapers done (13 presets + dim, no permission); system wallpaper export not started.
2026-10-17: ④ export done in its own commit — `SET_WALLPAPER` added; Vzhled › Tapeta › Nastavit jako tapetu systému
asks Plocha / Plocha i zámek / Zrušit, renders the preset with its dim at the screen's portrait size and saves it off
the main thread. Intelia keeps drawing its own wallpaper; nothing changes before the owner picks a target.


## D14 — 2026-10-17 — Bundle six OFL font families for the clock and the date (follows D13)

Six families under SIL OFL 1.1, taken from the google/fonts repository (`ofl/<family>/`), subset with fontTools
to Latin + Latin Extended (U+0000–024F, combining marks, U+1E00–1EFF, general punctuation, €, ™, −) with layout
features kept (`kern`, `liga`, `tnum`, `lnum`, `calt`, `ccmp`, `mark`, `mkmk`, `locl`) and hinting dropped:
Inter (opsz, wght), Space Grotesk (wght), DM Serif Display (Regular), JetBrains Mono (wght), Bodoni Moda (opsz, wght),
Unbounded (wght). Files in `res/font/`, licences in `assets/licenses/<Family>-OFL.txt`; about 1.05 MB together.
Subsetting is a modification under the OFL; none of the families' names is a Reserved Font Name (DM Serif Display
reserves only "Source"), so the names stay. Used only for the clock and the date (owner's choice per item).

## D15 — 2026-10-17 — Intelia Veil 3.0 (APPROVED by the owner 2026-10-17, after revision 2)
Phase A of the owner's "Veil 3.0" brief. Prototype `docs/design/veil3-prototype.html` (private artifact
https://claude.ai/artifact/UCthmYs5TCqSjsdSFbh9EF). Builds on D10/D12/D13; nothing that exists is rebuilt.
- **One material, three densities** (Veil Light / Dense / Focus); one crystal edge (1 dp, light at the top, fading,
  faint accent at the bottom). Veil / Crystal / Frost / Liquid / Matte stay as presets of the same surface.
  Phone (3) has no cross-window blur, so the no-blur opacities are the default and Studio says so.
- **Home:** default clock for new installs = "Čisté" (type straight on the wallpaper); stacked and plate stay styles.
  Labels off by default and never overlap.
- **Editor:** Home scales to 86 %, a 48 dp top bar (cancel, undo/redo, edited name, Hotovo) and a bottom tray
  (Přidat, Předlohy, Rozložení, Vzhled) use the freed margins, so the bar never covers a top clock. Inspectors grow
  from the element on the opposite side; common options first, the rest under "Další možnosti".
- **Inheritance:** System → global theme → layout → element; every element property shows Globální / Vlastní ↺.
  Studio and inspectors draw one shared setting definition.
- **Studio IA:** Vzhled · Plocha (enters the editor) · Ovládání · Akce a FLOW · Systém, with settings search that
  jumps to the exact row.
Not decided yet: implementation order after approval (prototype's list), icon packs and custom fonts (need own entries).
**Revision 1 (owner feedback 2026-10-17: Studio and overlays not dense enough):** depth comes from one shared
backdrop under the open surface, not from each panel: Dense (sheets, dialogs) blurs Home 5 dp and mutes it;
Focus (Studio, Search) blurs Home 18 dp, scales it to 97 %, darkens and tints it with the accent; inspectors leave
Home sharp so the edited element stays visible. Home is Intelia's own layer, so the blur is real on API 31+
(`RenderEffect`), Phone (3) included; only the wallpaper behind the window stays sharp there. Focus panel opacity
84 % (72 % with cross-window blur), Dense 80 / 66 %, Light 64 / 48 %. The keyboard layer is Veil Focus, the old
separate Search scrim is gone, Studio rises 22 dp with a 1.5 % scale, the status bar fades under Focus.
**Revision 2 (owner test of the prototype, 2026-10-17):** every material surface carries the base `v` class
(the density classes alone drew no surface); the Search field is fully drawn with a placeholder and takes focus
while it unfolds (no 320 ms delay); inspectors are placed from the element's settled position (after the editor's
86 % scale), on the side with more room, and keep still while a slider is dragged (they move only if the element
would grow under them); sliders update value and preview without rebuilding the inspector. Focus opacity lowered to
74 % (60 % with cross-window blur) so it stays faintly crystalline over the shared Focus backdrop; Light 64 / 48 %,
Dense 80 / 66 % unchanged. Checked in a desktop browser only: 9 scenes × dark/light/colour wallpaper × dark/light
UI × Reduced Motion (materials have a computed background, no inspector overlaps its element, Search focused),
a real pointer drag of the clock size slider min→max, typing "rano" into Search and Esc. Not verified: phone-width
page, Android blur, permissions, performance.

## D16 — 2026-10-17 — Optimised `lab` build for everyday use, baseline profile via `profileinstaller`
The owner runs the debug Lab as the daily home app. A debug build is `DEBUGGABLE`, not shrunk and never AOT
compiled (`status=verify`), so Compose runs interpreted/JIT; after a reboot that competes with the whole system
starting. New build type `lab` = release (R8, not debuggable) under the `.lab` package, signed with the shared
debug key (D4), so `install -r` swaps it with debug either way and keeps data, HOME role and PULSE. It is an
internal build, not a release. `androidx.profileinstaller` (Apache-2.0, no permission, no network; its receiver
is guarded by the system DUMP permission) installs the merged baseline profile (library profiles +
`app/src/main/baseline-prof.txt`) on sideloaded builds, so ART compiles the startup path ahead of time on any
phone. Revert: drop the `lab` block, the dependency and the profile file.

## D17 — 2026-10-17 — Settings backup: export and import through the system file picker (APPROVED by the owner 2026-10-17)
Studio › Systém › Záloha (prototype row "Záloha · Export a import nastavení"). All of Intelia's state is the one
`LauncherSettings` JSON document, so a backup is that document.
- Export: "Exportovat nastavení" opens the system picker (`ACTION_CREATE_DOCUMENT`, `application/json`,
  suggested name `intelia-YYYY-MM-DD.json`); Intelia writes an envelope
  `{"format":"intelia-settings","version":1,"settings":{…}}` to the file the owner chose. Nothing else is written.
- Import: "Obnovit ze souboru" opens `ACTION_OPEN_DOCUMENT`; the file is untrusted. Read at most 256 KB as UTF-8,
  require the envelope `format`, refuse `version` or `settings.schema` newer than this build, decode leniently
  (unknown keys ignored) and pass it through `LauncherSettings.sanitized()` — still the single validation gate.
  A bad file changes nothing and says why (unlike `SettingsCodec.decode`, which falls back to fresh settings).
- Before applying: a sheet with a short summary (FLOW commands, favourites, hidden apps, wallpaper) and
  "Nahradit nastavení". After applying, "Vrátit import" stays in Systém until Studio closes (previous document
  kept in memory only).
- Kept from this phone, not imported: `pulseLens` (camera cutout) and `editorCard` (editor position) — both are
  device geometry. `onboarded` becomes true. Apps missing on this phone stay as keys and are skipped as today.
- No permission, no network, no background work: the Storage Access Framework grants access to one file per
  action. `android:allowBackup` stays `false` (no silent Google auto-backup). The file is plain JSON with app
  package names and FLOW names; where it goes (local folder, a USB drive, or a cloud provider shown in the
  picker, such as Google Drive) is the owner's choice per export.
- Tests: JVM `BackupCodecTest` — round trip, wrong format, garbage, oversize, newer version/schema, device fields
  kept, out-of-range values clamped by `sanitized()`.
- Revert: remove the Záloha fold, `BackupCodec` and the strings; stored settings are untouched.

Future, not part of D17 (owner's wish 2026-10-17): a Google account with automatic sync / backup and local use of
Google APIs. That needs its own decision: the `INTERNET` permission, an account and OAuth flow (Credential
Manager / Google Identity), a Drive appDataFolder or similar, the licences and size of Play services libraries,
conflict handling between phones, and what leaves the phone. D17's versioned envelope is the payload such a sync
would carry, so nothing in D17 has to change for it.

## D18 — 2026-10-17 — Accessibility: reduced motion, larger text, 48 dp targets (APPROVED by the owner 2026-10-17)
Owner: "udělej vše co máš" after the D18 proposal.
- **Reduce motion** stays what already exists: Pohyb › Vyp (`Motion.Off`) and the system's "Remove animations"
  both make every `motionMs` duration 0 ms. No second switch.
- **Larger text:** Nastavení › Pohyb, text a dotek › Velikost textu 100 / 115 / 130 %. `LauncherSettings.textScale`
  multiplies the system font scale for Intelia's own UI (`LocalDensity` in the theme), so it adds to, never replaces,
  the system setting. `sanitized()` snaps any stored value to the three offered sizes (non-finite → 100 %).
  Home miniatures and the PULSE overlay keep their own sizes.
- **Touch targets:** every icon-only control is at least 48 × 48 dp (editor bar icons, inspector arrows and the
  card reset, PULSE Quick View buttons).
- Revert: drop the field and the row; stored documents with `textScale` decode with the key ignored.

## D19 — 2026-10-17 — Google account sync: PROPOSED, not built
The offline alternative is D17 (export/import through the system picker; a cloud provider such as Google Drive can
be chosen in that picker). Automatic sync needs, before any code: the `INTERNET` permission (Intelia has none
today), a Google Cloud project with an OAuth client owned by the owner, Credential Manager / Google Identity and a
Drive `appDataFolder` client (licences and APK size to check), conflict rules between phones, and a list of what
leaves the phone (D17 envelope: package names, FLOW names, wallpaper choice). Waiting for the owner's decision.

## D20 — 2026-10-18 — Hardware keys: diagnostics only, foreground only (no new capability)
M4 adds Studio › Ovládání › Gesta › Hardwarová tlačítka: a focused screen that shows which key codes reach
Intelia (and consumes them there, so volume does not change); nothing runs and nothing is bound. Keys reach an
app only while it is in front; Power, Home and Recents never do. Catching keys in other apps would need
`flagRequestFilterKeyEvents` on the PULSE accessibility service, which lets it see every key press system-wide:
not enabled, waiting for an explicit owner decision. `Trigger.Key` stays valid in the model, but no surface reads
it until a key was proven here and the owner decided where it may act.

## D20a — 2026-10-18 — APPROVED by the owner: hardware-key remap outside Intelia
The owner asked for remapping "like Essential Key Remap", also outside Intelia. Scope:
- `android:canRequestFilterKeyEvents="true"` on the PULSE accessibility service; `flagRequestFilterKeyEvents` is not
  in the XML but requested at run time only while Studio › Ovládání › „Tlačítka i mimo Intelii“ is on **and** a
  proven key has a binding. Off by default; the switch needs the PULSE service on.
- The filter takes only keys with a binding; every other key goes on untouched. Back, Home, Power and Recents are
  never accepted. Only key codes proven in the diagnostics page can be bound (`Controls.keys`, sanitized).
- Nothing is logged or stored about presses; no text is ever seen, only key codes and two timers (hold, double).
- Nothing runs while typing: while Intelia's search is open every key passes. Text fields of other apps cannot be
  detected without window content, which the service does not request; that limit is accepted and stated here.
- With the screen off (not interactive) every key passes; screen-off gestures are not offered.
- Unbound presses of a taken key keep their job (volume step, media key); with only a double press bound a single
  press acts one double-tap timeout later.

## D21 — 2026-10-18 — APPROVED by the owner: notification access for Music PULSE (M3.2)
The owner approved a `NotificationListenerService` (`pulse.PulseMediaListener`) for one purpose only: Android allows
`MediaSessionManager.getActiveSessions` only to an enabled listener. Scope:
- The listener has no code beyond telling `MediaWatch` it was bound; no notification is read, logged or stored.
- Off twice by default: Studio › PULSE › „Ukázat, co hraje“ (`pulseMusic`, default false) and Android's notification
  access, which only the owner switches on (Intelia opens the page, never toggles it).
- Event driven: active-sessions listener + `MediaController.Callback`, no polling. Watched only while it can show:
  Home while started (Jen na ploše), the PULSE service while Všude + unlocked + music on.
- Controls exist only for actions the session reports (prev / play-pause / next, seek only with `ACTION_SEEK_TO`).
- Accepted side effect: an enabled listener stays bound by the system, which keeps Intelia's process alive.

## D22 — 2026-10-18 — APPROVED by the owner: Global ARC from the camera (M3.3)
The owner chose: PULSE **hold → Global ARC** (set 1 by default, any set selectable in Studio › Ovládání), **tap then
hold → torch**, everything rebindable. Scope:
- New `Action.Arc(set)`; only `Trigger.Pulse(Hold | DoubleTapHold)` may carry it (`Controls.opensArc`, enforced in
  `bind` and `sanitized`); it never sits in an ARC slot. A missing set falls back to the first set.
- Geometry is ARC V2 `ArcBow` with LENS on the top edge (`GlobalArcGeometry`), one set (no room for dots/arrows above
  the camera). Release on a slot runs it; release near the camera, a cancel or a second finger runs nothing. A FLOW
  shows its steps while picked and runs only on release (the explicit action of AGENTS.md).
- Outside Home the PULSE service now starts apps, FLOWs and shortcuts **only from Global ARC**, through Intelia's own
  `AppRepository` (public `LauncherApps`; shortcuts only while Intelia is the default home). Search and cards bring
  Home forward and run there via an in-process request (never an intent extra, so no other app can inject actions).
  Every other PULSE gesture keeps the M3 rule: system actions only outside Home. No new permission.

## D23 — 2026-10-18 — PreviewHost: one way to show live previews in Studio (M1)
- Every Studio preview is the renderer the real surface uses, fed with the owner's settings, wrapped in
  `PreviewCover`: all touches are consumed on the Initial pass, TalkBack hears one label, `LocalPreviewMode` is true.
  Renderers that start side effects (focus, keyboard, haptics, actions, `PulseAccessibilityService.typing`) must skip
  them when `LocalPreviewMode` is set. Sample data (music, alarm, a query) is always marked as a sample.
- `PreviewPlan.kind(section)` (pure, JVM tested) is the only place that says which page shows which preview.
  M1 step 1: Home miniature (main page, Vzhled, Plocha, Oblíbené) moved onto it, unchanged look.
- Next kinds, same rules: `Arc` (ARC page: the owner's first set drawn open at LENS, no slot picked), `Lens` (LENS
  calibration: the dock strip), `Search` (Search card with a marked sample query and the owner's apps, no field focus,
  no keyboard), `Pulse` (move the in-list S3.1 state preview into the host). No new permission, service or network.
- M1 step 2 (built): Arc on ARC, Pulse on PULSE, Lens on Gesta a tlačítka (what LENS's gestures act on), Search on
  Akce a FLOW (FLOWs run from Search by exact name; the sample query is the owner's first FLOW name). Each preview shows
  only the strip it is about (`PreviewPlan.stripTop`).

## D24 — 2026-10-18 — APPROVED by the owner: haptic strength (B, VIBRATE) with automatic fallback to A
M2 order: (1) counters in binding-group captions and (2) Motiv preview tiles need no decision (built 2026-10-18);
(3) haptic strength and (4) a custom colour follow D24 / D25.
The owner chose B with a fallback: maximum control without losing compatibility. Scope:
- New permission `android.permission.VIBRATE` (normal, granted at install). Used only for touch feedback Intelia
  already gives (ARC, PULSE, LENS, Search, Studio preview of the strength); never for notifications or patterns.
- Model `HapticPrefs` (settings `haptic`): one global `strength` 0–100 (null = taken from the old `haptics` choice on its
  stop: Off 0, Jemná 25, Výrazná 85, so nothing changes for an existing install) and a `per` map for ARC, PULSE, gestures and
  keys — sanitized now, no UI yet (later step). Every write of `strength` also writes the old `haptics` enum, so an
  older build or backup reader keeps a close value.
- Device with amplitude control (`Vibrator.hasAmplitudeControl`): a continuous slider, one-shot pulses whose
  amplitude follows the strength. Without it: four stops (Vypnuto / Jemná / Střední / Silná) mapped to the system's
  own effects through `View.performHapticFeedback`. `Haptic.plan` (pure, JVM tested) picks the path.
- The system setting wins: with Android's touch feedback off nothing vibrates on either path (the amplitude path
  reads `HAPTIC_FEEDBACK_ENABLED` and vibrates with `USAGE_TOUCH`, which follows the touch-intensity setting).
  Strength 0 is off. Global ARC's own on/off switch stays.

## D25 — 2026-10-18 — APPROVED by the owner: one custom accent colour, architecture ready for full palettes
Step 1 is one owner-made accent for the whole launcher. Scope:
- Settings `custom: CustomColors` — today only `accent` (opaque ARGB, null = none). It is the main accent while set
  and overrides the preset `accent`, which is kept untouched, so choosing a preset again (or clearing the custom
  colour) restores the old look exactly. Places of a per-place palette that follow the main colour follow it too.
- Later steps extend `CustomColors` (per-place colours, component colours, whole themes, a Design Lab) without
  touching the preset enum: every renderer gets colours through one resolver (`mainAccent` in `Ink.kt`).
- The stored colour is the owner's base; what is drawn is derived per theme by `AccentColors.derive` (pure, JVM
  tested): the hue and saturation are kept, lightness moves until it reaches 4.5:1 against the Veil base of that theme
  and the text on the accent keeps 4.5:1 too. No eyedropper (would need screen capture), no permission, no network.

## D26 — 2026-10-19 — Versioning, release signing and app language for 0.1.0-beta.1
- **Version:** `versionName = "0.1.0-beta.1"`, `versionCode = 2` (was 1). Build types add their suffix (`-lab`, `-dev`).
  `versionCode` only ever goes up; every public build gets a new one.
- **Packages:** `dev.intelia.launcher` is the distribution package; `dev.intelia.launcher.lab` (Lab, debug) is the
  development package signed with the shared, public debug key (D4). The two install side by side and never update
  each other. The debug key is public in git and must never sign a distributed build.
- **Release signing:** `app/build.gradle.kts` reads an optional `.signing/release.properties` (`storeFile`,
  `storePassword`, `keyAlias`, `keyPassword`; `.signing/` and `*.jks` are git-ignored). Without it
  `assembleRelease` produces `app-release-unsigned.apk`, which cannot be installed or published by mistake.
  Creating the long-lived key is the **owner's decision** (who holds it, where the backup lives); losing it means no
  update path for anyone who installed a build signed with it.
- **Language:** Intelia ships English (`values`) and Czech (`values-cs`). `res/xml/locales_config.xml` +
  `android:localeConfig` enable Android 13+'s per-app language; Studio › Systém a data › Jazyk opens that system
  page. No in-app locale override, no new dependency. Older Android follows the system language.

## D27 — 2026-10-19 — Repository hardening for the public beta (owner request: "security first")
- **Release key** created by the owner (RSA 4096, PKCS12, `CN=Intelia Launcher, O=Intelia, C=CZ`, certificate SHA-256
  `5ddb08b20d7f3eb02c0ae26a7d8b6a75fb284fe97616f42260ded24cd6dd99d3`), stored only in `.signing/` with offline backups.
- **Debug key out of the repo:** `keystore/debug.keystore` is no longer tracked (supersedes the shared-key part of D4).
  Gradle uses `.signing/debug.keystore` when present (same key, so `install -r` of the Lab keeps working on the
  owner's phone), else the PC's own Android debug key. The old key stays in git history and is treated as **public**:
  it only ever signed `dev.intelia.launcher.lab`, which is never distributed.
- **History not rewritten:** removing the key or the author e-mail from history needs a force push to `main`
  (AGENTS.md: owner approval) and would not reach existing clones, forks or GitHub's PR refs. Proposed, not done.
- **Visible refs:** only `main`, tags and Releases; merged and backed-up work branches are deleted on GitHub after a
  local `git bundle` backup outside the repo.
- **Legal:** Apache-2.0 stays (already granted for every published commit; relicensing future versions is a separate
  owner decision). Added `NOTICE` (must be kept in redistributions, Apache §4), `TRADEMARKS.md` (name and logo not
  licensed, Apache §6), `SECURITY.md` (private reporting, verifying downloads), `CREDITS.md` (functional inspiration,
  no copied code; third-party licences).
- `.gitignore` also blocks `*.p12`, `*.pem`, `*.apk`, `*.aab`, `*.bundle`, `*.rar`, crash dumps.

## D28 — 2026-10-19 — P0 PULSE reliability: state from facts, exported screen receiver (owner brief V2: "PULSE first")
- **Cause (device evidence, `docs/product/PULSE_RELIABILITY.md` §2):** `USER_PRESENT` comes from SystemUI's uid, so
  the `RECEIVER_NOT_EXPORTED` screen receiver never received it; the stored `locked` flag stayed true after every
  keyguard unlock and the lens overlay was never re-added until the service was rebound.
- **Decision:** the receiver is `RECEIVER_EXPORTED` for `SCREEN_OFF`, `SCREEN_ON`, `USER_PRESENT` only (protected
  broadcasts: no app can spoof them). The service stores no lock state; every evaluation reads
  `PowerManager.isInteractive` + `KeyguardManager.isKeyguardLocked` through the pure `PulseLifecycle` model.
- **Recovery without polling:** a bounded keyguard re-check after `SCREEN_ON` (7 checks, ≈ 54 s, cancelled on unlock),
  `resync()` from Home's `onStart`, 3 bounded attach retries; no wake locks, no loops, no automatic re-enabling of
  accessibility.
- **Studio truth:** `PulseAccessibilityService.health` (`Disconnected · Off · Locked · Attaching · Active · Error`)
  drives the PULSE page detail, not the stored switch.
- **Diagnostics:** `Service.dump()` with states and counters only (no content, text, keys, touch positions).
- No new permission, no settings-format change.

## D29 — 2026-10-19 — PROPOSED: priority order after P0 and public release
- Owner: "nejdřív PULSE, pak komplet onboarding nově a lépe, pak zbytek; chci to nasadit do produkce a dávat lidem."
- Proposed order: P0 PULSE → P1 Onboarding V2 (design preview first) → P1.5 public release → P2+ (MASTER_PLAN §14.1).
- Open owner decisions (Blocked until answered): channel (Google Play / GitHub Releases / own web page / F-Droid),
  Play developer account and policy route for the accessibility service (F5: `isAccessibilityTool=false` → needs the
  Play declaration + prominent disclosure), privacy policy URL, crash reporting (none today; any reporting needs
  explicit opt-in per AGENTS.md), package name for production (`dev.intelia.launcher`), support contact.

## D30 — 2026-10-19 — Debug build gets its own package `dev.intelia.launcher.test` (owner request)
The owner wants a separate "Intelia Debug" app next to the everyday Lab, to open and test work in progress
themselves without touching the Lab's settings, HOME role or PULSE. The `debug` build type now uses the suffix
`.test` and the label "Intelia Debug"; the optimised `lab` build type keeps `dev.intelia.launcher.lab` and gets
the label "Intelia Lab" from `app/src/lab`. Both are signed with the Lab debug key (D27) and never distributed.
`install -r` between debug and Lab no longer applies (D16): update the Lab with `:app:assembleLab`. `.debug` and `.dev`
are not used: the test phone still keeps data of old uninstalled builds under those names with higher version
codes, so installs fail with `INSTALL_FAILED_VERSION_DOWNGRADE`. Revert: set the debug suffix back to `.lab`.

## D31 — 2026-10-19 — The guide's sandbox covers direct system effects (`LocalSandbox`)
Context: the guide's stage is the production `Home`; its callbacks were the sandbox, but PULSE Quick View toggled the
torch, the music card sent media keys and opened the player directly, outside any callback. Decision: a
`LocalSandbox` composition local, set only by `OnboardingHost`; while set, those surfaces hand the `Action` they stand
for to it and do nothing. Every new direct side effect on a Home surface must check it. Revert: remove the local
(the guide would then act for real on those three).

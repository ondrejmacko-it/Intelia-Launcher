# Status

Only verified facts. Format: `date | branch/commit | evidence | change | open`.

## 2026-10-18 — Studio Veil 3.0, step 1: five areas, shared kit, search to the row (`feat/control-system-v1`)
- Owner approved the tree: Vzhled · Plocha · Ovládání (PULSE, ARC, Gesta a tlačítka, Animace a haptika) · Akce a FLOW ·
  Systém a data. Fixed gestures: only the LENS pillars stay locked (to be applied in the gestures step).
- `ui/VeilKit.kt`: `VeilGroup` (one accent-washed matte plate per group, crystal edge, hairlines between rows),
  `SettingRow`, `SwitchRow` + `VeilSwitch`, `VeilChoice` / `ChoiceSetting`, `SettingBlock`, `ResetDefaults` (says what it
  restores, asks first), `settingAnchor` (search target: scroll into view + accent glow; Motion Off = no fade).
- `StudioIndex`: sections with parents (max. two levels), 28 row-level targets with synonyms; the path never repeats a
  name (no more „Ovládání › Ovládání“; the gestures page is now „Gesta a tlačítka“).
- Pages rebuilt on the kit: main page (5 areas with summaries, quick access: Upravit plochu / PULSE / ARC), Vzhled
  (theme + colours, material, wallpaper, text size moved here, icons; reset), Plocha (editor, favourites, grid,
  placement), Ovládání hub, Akce a FLOW, Animace a haptika (reset), Systém a data (permissions with state: default
  home, PULSE service, notification access; hidden apps, backup, about). Editor: a tap on the camera opens
  Studio › PULSE (Back returns to the editor) instead of a second PULSE card.
- Not yet on the kit: PULSE, ARC, Gesta a tlačítka, Oblíbené, Záloha pages (old components); search targets on those
  pages open the page but do not scroll to the row yet (e.g. „Global ARC“ opens ARC on the LENS tab). Citlivost,
  accent in Search/Drawer/FLOW: later steps.
- Settings coverage (by reading, not by a test): every user key has a row; legacy/internal keys without one:
  showClock, showDate, reduceMotion, reach, layout, arc, arcFlows, arcShortcuts, arcRings, onboarded, editorCard, schema;
  `home` and per-item clock/date look stay in the Home editor.
- PASS: `./gradlew -q -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleLab` (StudioIndexTest +2), no warnings.
- Phone (Lab `install -r`), verified: main page renders the five areas with summaries; search „velikost textu“ →
  one result „Vzhled › Velikost textu“ → Vzhled scrolled to the row with the glow; ARC page via search; Global ARC
  read back as S + Silné and set to M / Střední / Střední (read back from the chips); Ovládání hub rows render.
- NOT verified: Systém a data page, Plocha page, reset dialogs, editor camera → Studio › PULSE, light theme, 130 % text,
  TalkBack. Testing stopped: an incoming call arrived on the phone.

## 2026-10-18 — Global ARC look of its own, bow and label fixes (`feat/control-system-v1`)
- `GlobalArcLook` in `Controls.globalArc` (size S/M/L, arc narrow/mid/wide, slot tick, dim over apps 30/45/65 %),
  sanitized with the rest; old configs decode to the defaults (= the look Global ARC had). Slot count and sets stay
  shared with LENS ARC (`GlobalArcLook.over`). Home and the service use `ArcContent.globalLook` for geometry and tick.
- Studio › ARC: chips „LENS ARC“ / „Global ARC“; the Global tab has Velikost, Oblouk, Ztmavení nad aplikacemi,
  Ťuknutí na každém slotu; sets below on both tabs. Search keywords: global, kamera, ztmavení.
- Fixes: the dashed bow's circle centre was mirrored below the camera (now `Dock.toScreen(Top, 0, drop)`, through the
  slots); the release hint wraps to two centred lines, up to 300 dp, instead of being cut off.
- Tests: `ControlsTest` +2 (separate look, sanitize + old JSON defaults).
- PASS: `./gradlew -q -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleDebug` and
  `:app:testDebugUnitTest :app:assembleLab`, no Kotlin warnings. Lab installed with `install -r`.
- Phone, verified: over Calculator the bow runs through all 8 slots and „Návrat ke kameře gesto zruší.“ is whole;
  Studio tabs render; Global set to S + Silné while LENS ARC stayed M / 8 slots (read back from the chips).
- Seen: right after an install the service shows „ARC je prázdný“ until its app list loads (a second or two).
- NOT verified: how S + Silné looks over an app (the owner took the phone over), release after the change, Home
  (Jen na ploše) drawing. Global ARC was left at S + Silné on the phone; set it back to M + Střední if unwanted.

## 2026-10-18 — M3.3 follow-up phone checks (`feat/control-system-v1`, `8cb8f81`, Lab installed 14:01)
- Verified (Všude, over Google Calculator): temporarily Search in FLOW slot 4 + Podržení → Global ARC · FLOW; release
  on the Search slot → Home with the Search sheet and keyboard open. Podržení → Global ARC · Zkratky; release on
  „Nová anonymní karta“ → Chrome incognito (shortcut outside Home works). Both test changes reverted and re-read
  (Podržení → Global ARC · Hlavní, FLOW 3/8, Zkratky 3/8).
- Seen: the hint „Návrat ke kameře gesto zruš…“ is cut off (220 dp label); slots sit ~120 px under the camera while the
  dashed bow is drawn far lower (y≈1000–1250 px); over Maps the app's chips show between slots under the .45 scrim.
- `uiautomator dump` makes the system unbind and rebind every accessibility service, so for ~1 s the Studio
  „Všude potřebuje službu PULSE“ hint is correct, not a bug; wait before gesture tests after a dump.
- NOT verified: TalkBack path (turning TalkBack on is a system setting, left to the owner), a really light app
  (system night mode: Calculator and Maps are dark), landscape.

## 2026-10-18 — PULSE 2.0 M3.3 Global ARC (`feat/control-system-v1`)
- D22: `Action.Arc(set)` bound only to held PULSE gestures; `Controls.opensArc` / `arcSetFor`, sanitized (not in slots,
  not on other triggers). `ui/GlobalArc.kt`: `GlobalArcGeometry` (ARC V2 `ArcBow`, LENS on the top edge) + `GlobalArc`
  composable (veil, dashed bow, thread, slots, "release opens/runs" label, FLOW bead preview; over other apps a dim
  scrim and light ink; TalkBack long press opens it with slots as buttons). `ArcContent.of` shared by Home and the service.
- Home (Jen na ploše): `pulseTouch` reports held moves and the end (lift = run, cancel = nothing). Service (Všude): the
  lens touch keeps driving a non-touchable `Intelia PULSE arc` window; outside Home `AndroidActionHost` resolves apps,
  FLOWs and shortcuts through `AppRepository`; Search / cards go to Home via `PulseAccessibilityService.homeRequest`.
- Studio › Ovládání: picker has a „Global ARC“ tab, one row per set („Global ARC · Hlavní / FLOW / Zkratky“); bound
  rows show the set. `DoubleTapHold` is labelled „Klepnutí a podržení“. PULSE hint updated.
- Tests: `ControlsTest` +4 (hold-only binding, sanitize, set fallback, JSON), `GlobalArcGeometryTest` 2.
- PASS: `./gradlew -q -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleDebug` and
  `:app:testDebugUnitTest :app:assembleLab --rerun-tasks`, no Kotlin warnings.
- Phone (debug, then Lab `install -r`): owner's bindings set through the new picker — Podržení → Global ARC · Hlavní,
  Klepnutí a podržení → Svítilna (FlashlightController true/false). Všude on Home: arc under the camera, slide back to
  the camera + lift → nothing (Home stays, arc window gone); release on Instagram → Instagram. Over Instagram: release
  on ChatGPT → ChatGPT (outside Home, `LauncherApps`). Temporarily Global ARC · FLOW over ChatGPT: „pocty“ previewed
  Kalkulačka → Hodiny, release ran both in order; binding set back to Hlavní. Jen na ploše (temporarily): Home draws
  the same arc, cancel → nothing, release → ChatGPT; mode set back to Všude.
- NOT verified: Search from Global ARC (no Search slot in the owner's sets), shortcut slots outside Home, cards,
  TalkBack path, light apps under the scrim, landscape. Hold on the Home target must start below the status bar
  (a touch at y≈120 px went to the shade). Optional M3.2 leftovers still open: YouTube Music, Reduced Motion, lock.

## 2026-10-18 — PULSE 2.0 M3.2 Music PULSE (`feat/control-system-v1`)
- D21 approved (notification access only for active media sessions). `pulse/NowPlaying.kt` (pure: state, actions →
  controls, extrapolated position, session pick) + `NowPlayingTest` 5; `pulse/MediaWatch.kt` (session listener +
  `MediaController.Callback`, ref-counted, no polling) and `PulseMediaListener`; `musicCapsule` geometry + test.
- Compact: bars left (move only while playing and motion on; paused = three low dots), lens ring (+ charge gauge on
  Home), round artwork right (app icon via `LauncherApps` without artwork). Expanded: artwork, title, artist,
  timeline (seek only with `ACTION_SEEK_TO`), ⏮ ⏯ ⏭ only when supported. Home first; the service draws the compact
  as a non-touchable `Intelia PULSE music` window (hidden while the menu is open) and the menu shows the card on top.
- Studio › PULSE: „Ukázat, co hraje“ toggle; switching it on without access opens Android's notification-access
  page for the listener and shows why. Fixes: Studio said the service was off after an update rebind (old instance
  reset `running`); the expanded Home panel was drawn under the Home clock (now above while expanded).
- PASS: `./gradlew -q -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleDebug :app:assembleLab`, no warnings.
- Phone (debug, `install -r`), verified: after unlock the lens is back (`Intelia PULSE` windows ≥ 1); double tap + hold
  with a test binding (→ Oznámení) opened the shade, hold still toggles the torch (FlashlightController), test binding
  removed again (owner's Spotify / Svítilna untouched). Owner enabled notification access by hand. Všude: compact over
  Home and over Spotify; tap → menu with card; pause, seek (→ 1:02), play, next all changed Spotify's session. Jen na
  ploše: service 0 windows, Home draws compact with charge gauge; tap → panel above the clock; pause → static dots.
  Mode set back to Všude, music resumed.
- NOT verified: YouTube Music, Reduced Motion (code path only), compact removal at lock (code path same as the lens),
  TalkBack on the timeline, battery/gfxinfo. A tap on the artwork itself inside the status bar goes to the system
  (only touches below the status bar reach the target).

## 2026-10-18 — PULSE 2.0 M3.1 Foundation (`feat/control-system-v1`, `6091d36`)
- `pulse/PulseGestures.kt` (pure): one recognizer for Home and the service — Tap, DoubleTap, Hold, new
  `Gesture.DoubleTapHold`, swipes. Tap waits only while DoubleTap/DoubleTapHold is bound, hold only while Hold
  (DoubleTapHold) is bound; cancel / second finger / unbound or diagonal swipe → nothing; a cancel without a lift
  does not swallow the next touch. Timings from `ViewConfiguration`. `PulseStage` (Hidden/Compact/Expanded/Arc) and
  `PulseMode` (Off/Home/Everywhere) models. `PulseGesturesTest` 11 tests. Old `pulseSwipe` removed.
- Home: `ui/PulseTouch.kt` feeds the recognizer and consumes the press (before, a hold under the camera also
  opened the Home editor — found and fixed on the phone). TalkBack: click, long click when bound, custom actions.
- Service: one `OnTouchListener` → recognizer (click/long click kept only for TalkBack). Lens target only in
  Everywhere mode and unlocked: `SCREEN_OFF` / `SCREEN_ON` / `USER_PRESENT` broadcasts + settings flow, no polling.
- `pulseEverywhere` (default true, old configs keep behaviour); Studio › PULSE and the Home inspector: chips
  Vyp / Jen na ploše / Všude (Všude without the service opens Android settings and says why).
- PASS: `./gradlew -q -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleDebug :app:assembleLab`,
  no Kotlin warnings. Debug, then Lab installed with `install -r`.
- Phone (debug build), verified: Všude — hold on Home toggles torch (FlashlightController log), double tap → Spotify
  (owner's binding), tap → menu; in Spotify hold toggles torch on/off, tap opens the menu, unbound left swipe does
  nothing. Jen na ploše — service lens window gone (0 windows), Home hold toggles torch 3× in a row, double tap →
  Spotify, tap opens the panel, editor no longer opens. Back to Všude → lens window back. Screen off → no lens window;
  screen on at keyguard → still none.
- Measured facts: a swipe down from the camera above apps always opens the notification shade (the system takes the
  touch; PULSE runs nothing). The torch goes off by itself ~10 s after `dumpsys media.camera` runs, so torch checks
  use `logcat FlashlightController` now. No swipe up from the camera (≈25 dp above it, threshold 36 dp).
- NOT verified: lens coming back after unlock (`USER_PRESENT`; phone left locked, owner unlocks), double tap + hold by
  hand, multi-touch by hand, TalkBack, gfxinfo/battery. Reference screenshots of Material Capsule / Essential Key
  Tools were not in the repo or the message, so none were reviewed.

## 2026-10-18 — Control System V1: M5 Studio „Ovládání“, PULSE swipes, Home side swipes, D20 key remap (`feat/control-system-v1`)
- Studio › Ovládání (`ui/ControlsPage.kt`, replaces the old Gesta page and the PULSE page's gesture rows): one search
  over gestures, surfaces and bound action names (diacritics-insensitive, `AppSearch.fold`); foldable groups PULSE
  kapsle · Home · LENS · ARC (link to the sets, filled-slot count) · Hardwarová tlačítka · one group per proven key,
  each with "n přiřazeno". A line is gesture → action (icon + name), tap = picker, cross = unbind; "Stejná akce je i
  jinde (n×)" is informative only. Pillar gestures (LENS tap/up/hold, Home up/hold) are shown locked as „Pevné“.
- `ActionPicker`: search always on top, category tabs Vše/Aplikace/FLOW/Zkratky/Systém/Média/Panely, „Už použité“
  first (actions bound anywhere), and for PULSE/key triggers a „funguje i mimo Home“ / „jen na Home“ tag.
- PULSE swipes down/left/right (`pulseSwipe`, ≥ 36 dp, dominant axis 1.5×): in the service via an `OnTouchListener`
  on the lens view (a recognised bound swipe cancels the view's tap/hold), on Home via `detectDragGestures` added only
  while a swipe is bound. TalkBack custom actions for every bound swipe (service and Home).
- Home swipe left/right (`homeSideSwipe`, ≥ 72 dp) through `detectHorizontalDragGestures`, so vertical drags stay the
  drawer / swipe down; TalkBack custom actions when bound.
- D20 approved and implemented (see DECISIONS): `canRequestFilterKeyEvents` in `pulse_accessibility.xml`; the service
  requests `FLAG_REQUEST_FILTER_KEY_EVENTS` at run time only while `keysEverywhere` is on and a proven key has a
  binding. `action/KeyRemap.kt` (pure): tap / double / hold per key, unbound presses replayed (volume via
  `adjustSuggestedStreamVolume`, media keys via `dispatchMediaKeyEvent`). Passes everything while Intelia search is
  open (`PulseAccessibilityService.typing`) or the screen is not interactive. `Controls.keys` (proven codes, max 8;
  Back/Home/Power/Recents refused), `withKey`/`withoutKey`/`keyBound`/`triggersOf`; key bindings on unproven codes or
  with other gestures than Tap/DoubleTap/Hold are dropped by `sanitized()`. The diagnostics page stores the seen code.
- PASS: `./gradlew -q -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleDebug :app:assembleLab`
  (new `KeyRemapTest` 7, `PulseSwipeTest` 3, `ControlsTest` +2); `compileDebugKotlin` without warnings.
- Phone (Nothing Phone (3)), verified: Lab and debug `install -r`; the PULSE service rebinds by itself after the update
  with `capabilities=8` (filter key events granted, no re-enable needed). With a test binding (key 24 double → torch,
  `keysEverywhere`) written into prefs via the debug build, the service logged `want=true has=false` and requested the
  filter (temporary log, removed before commit). Owner prefs restored from the pulled copy before Lab was reinstalled.
- Phone, measured: `getevent -pl`: `gpio-keys` (event0) reports KEY_VOLUMEUP and scan code `0x00fa` (250);
  `/system/usr/keylayout/gpio-keys.kl` maps 115/114/102/528/766 only, so 250 has no Android key code in that layout
  (likely the Essential Key, not proven). Another installed accessibility service (Kea, `com.techiboystudios.kea`)
  holds the key-filter capability (capabilities=41).
- NOT verified: any physical key press (the owner was away for the 15-minute `getevent -lt` capture, nothing came);
  whether the Essential Key reaches apps or the accessibility key filter at all; `input keyevent` injection never
  reaches accessibility key filters (onKeyEvent got nothing), `sendevent` is denied (`/dev/input` root/input only),
  so the remap itself is untested on the phone. Volume delay with only a double press bound not measured.
  The Ovládání page, the picker tabs and all new swipes were not seen on the phone: it was locked (keyguard) and
  unlocking needs the owner. Export/import of the new `keys`/`keysEverywhere` fields not checked.

## 2026-10-18 — Control System V1: M3 TalkBack actions, M4 Home/LENS gestures, key diagnostics (`feat/control-system-v1`)
- PULSE double tap for TalkBack: on Home a `CustomAccessibilityAction` when bound; in the service an
  `AccessibilityDelegateCompat` on the lens view, read at query time (double tap as custom action, long press only
  when hold is bound).
- `Trigger.Home(DoubleTap | SwipeDown)` and `Trigger.Lens(SwipeLeft | SwipeRight)` → `Controls.bound` → `perform`;
  all do nothing until bound, an unbound Home double tap adds no delay. LENS side flick (`lensSideFlick`, ≥ 56 dp,
  clearly horizontal) never overlaps the upward drawer flick (`LensFlickTest`). TalkBack gets custom actions for
  bound Home and LENS gestures. Studio › Gesta › Vlastní gesta: four rows with the shared picker (`GestureBindings`,
  also used for PULSE now).
- Studio › Gesta › Hardwarová tlačítka (D20): shows the keys that reach Intelia, with press count and hold;
  `KeyProbe` unit tested. No key binding, no new permission.
- `./gradlew :app:testDebugUnitTest :app:assembleLab` PASS; Lab installed with `install -r`.
- Phone: Home double tap → torch on, again → off; Home swipe down → notification shade (`mCurrentFocus`
  NotificationShade); LENS flick left → torch toggled; LENS flick right unbound → nothing; LENS tap still opens
  Search with the keyboard. In Android Settings a PULSE double tap bound to "Oznámení" opened the shade through the
  service. Key screen with `adb input keyevent` 24, 24, 25, long 25, 27: "Hlasitost + 2×", "Hlasitost − 2× a
  podrženo", "Fotoaparát 1×"; `volume_music_speaker` stayed 0. Physical keys not pressed (adb-injected only).
- Not verified: TalkBack itself (actions are in code, not tried with TalkBack on), LENS flick with LENS on a side edge,
  media/lock/screenshot through PULSE. Phone keeps PULSE Hold → torch, Double tap → Spotify (owner's), Home double
  tap → torch, Home swipe down → notifications, LENS left → torch (test bindings).

## 2026-10-18 — Control System V1: M3 PULSE gestures (`feat/control-system-v1`)
- `Trigger.Pulse(Tap | DoubleTap | Hold)` → `Controls.bound` → Home's `perform` (`Pulse.kt`, `combinedClickable`):
  an unbound tap still opens the panel; double tap and hold exist only when bound, so tap never waits.
- `PulseAccessibilityService`: the lens view takes the same three gestures. With Home resumed it hands the action
  to Home (`PulseAccessibilityService.home`); elsewhere only `Action.outsideHome` (media, torch, screenshot, sound,
  panels, lock) runs through an `AndroidActionHost` without Home's model; apps, FLOWs, Search, shortcuts and cards
  show "runs from PULSE only on Home". Double tap waits the system double-tap time only when bound.
- Studio › Ovládání › PULSE › Gesta kapsle: three rows with the shared `ActionPicker`, remove/reset per row.
- `Controls.bind` (replace/unbind, ARC slots refused) and `Action.outsideHome`, both unit tested.
- `./gradlew :app:testDebugUnitTest :app:assembleLab` PASS; Lab installed with `install -r`.
- Phone (Nothing Phone (3)): Studio row shows "Podržení · Svítilna" after picking via the picker; on Home a hold
  under the camera turned the torch on and a second hold off (`dumpsys media.camera`); an unbound tap opened
  the panel. ARC at font scale 1.3: arrow labels fit, no overlap.
- Follow-up, service path with PULSE everywhere on (owner switched it on): in Android Settings a hold toggled the
  torch on and off; a double tap bound to Allegro showed "Tahle akce se z PULSE spouští jen na Home." and launched
  nothing; on Home the same double tap launched Allegro through Home. Android suppressed Lab's Toast ("Suppressing
  toast … by user request", notifications off), so the service now shows its own note under the camera instead.
- ARC set arrows moved out of the thumb's way: (±72, +30) dp, below LENS centre, hit 22 dp (was ±94, +2, 30),
  smaller chevron and label; `ArcBowTest` checks the straight path from LENS to every slot never touches an arrow.
  Phone: the left arrow switched to "Zkratky"; a slide to the outermost left slot launched its app.
- Not verified: TalkBack for the new gestures (double tap has no TalkBack action), ARC with LENS on a side edge on
  the phone (geometry only covered by `ArcBowTest`). The phone keeps Hold → torch and Double tap → Allegro bound.

## 2026-10-18 — Control System V1: wide ARC, set arrows, set by taps (`feat/control-system-v1`)

- ARC is one flat arc just above LENS (middle slot 80/96/116 dp above the LENS centre for S/M/L; the circle's
  centre lies below LENS), spread across the room beside LENS, up to 8 slots (≥ 50 dp apart, ≤ 76 dp so 3 slots
  stay close). "Oblouk" (úzký/střední/široký) now sets how round it is. Set arrows `‹ name` / `name ›` sit beside
  LENS: sliding onto one shows the previous/next set (once per visit, re-arms when the finger leaves it); the old
  "slide past an end" switch is gone, the dots stay.
- Set by taps (`ArcLook.tapSets`, default on, toggle in Studio › Ovládání › ARC): hold = set 1, tap + hold = set 2,
  two taps + hold = set 3. A tap waits up to 280 ms for a next one only while there is more than one set, so
  Search then opens that much later. Three taps = Search.
- `Segments` targets are ≥ 48 dp wide (digits used to be ~10 dp); ARC "Sloty" row is stacked.
- `./gradlew -q :app:testDebugUnitTest :app:assembleLab` PASS (ArcBowTest rewritten for the new geometry).
- Phone (Nothing Phone (3), Lab, `install -r`, 411 dp wide): hold shows Hlavní with arrows "‹ Zkratky" / "FLOW ›";
  tap + hold opened FLOW; tap, tap + hold opened Zkratky (3/3 runs; one earlier combined run showed nothing, not
  reproduced); slide right arrow → FLOW, then left arrow → Hlavní; single tap opened Search; 8 slots span the width;
  release on the Instagram slot launched Instagram.
- Not verified: TalkBack on the arrows, LENS on a side/top edge with the new arc, 130 % text on the arrow labels,
  how 280 ms feels by hand (adb `input` taps are ~80 ms apart), gfxinfo.

## 2026-10-18 — Control System V1: M2 ARC V2 (`feat/control-system-v1`, `50edba3`, `558234f` + follow-up)

Verified (`./gradlew :app:testDebugUnitTest :app:assembleLab` PASS; Lab installed with `install -r` on Nothing Phone (3)):
- ARC is drawn from `settings.controls`: the legacy rings migrated to three sets (Hlavní 5 apps, FLOW 2, Zkratky 2),
  shown as one shallow arc with set dots; slot 1 straight above LENS.
- Hold + slide: slot pick with label (FLOW previewed as Kalkulačka → Hodiny); sliding past the right end switched to
  FLOW; sliding onto the third dot switched to Zkratky; back on LENS cancelled. Releasing on FLOW "pocty" opened
  Calculator then Clock.
- Studio › Ovládání › ARC: slots 3–7, size S/M/L, arc narrow/medium/wide, tick toggle, sets slot by slot (up, remove,
  add set). Shared ActionPicker: sections, "svit" → Svítilna (diacritics-insensitive); picked into FLOW slot 3.
  Releasing on it turned the torch on, again off (`dumpsys media.camera`).
- Keyboard: Tab to LENS + Up opens assisted ARC; slots are clickable nodes with their names, dots are separate
  48 dp tabs (Up used to move focus away before key-up; fixed by handling key-down).
- gfxinfo, 5 × hold/slide/cancel, 3 runs each: before M2 (`0e27fc9`) 7.6–8.6 % janky, p90 25–29 ms;
  M2 9.1–9.3 % janky, p90 23–29 ms. No clear regression; not a performance claim.

Not verified / not done:
- TalkBack itself (only the keyboard path, which uses the same assisted mode); Reduced Motion; LENS on a side or top
  edge on the phone (unit-tested geometry only); one-handed reach with the L size.
- Export/import of `controlConfig` on the phone. PULSE, Home, LENS and key bindings still not wired (M3/M4).

## 2026-10-18 — Control System V1: M0 fixes, M1 Action Engine core (`feat/control-system-v1`)

Verified (`./gradlew :app:testDebugUnitTest :app:assembleLab` PASS; Lab installed with `install -r` on Nothing Phone (3)):
- Add picker filter: "zpra" → Zprávy. Drawer "Moje" names ellipsize with a gutter at 130 % text. Settings at 130 %:
  no control under 48 dp. Tapping the camera in the Home editor opens the PULSE inspector.
- LENS ARC release runs through the Action Engine: app slot launched Claude; FLOW "pocty" launched Calculator then
  Clock; sliding back onto LENS launched nothing. No Intelia crashes in the crash buffer.

Not verified / not done:
- ARC content still comes from the legacy lists; PULSE, Home, LENS and key bindings are modelled but not wired.
- Torch, media, sound, panels, lock and screenshot actions in `AndroidActionHost` not exercised on the phone.
- Export/import of `controlConfig` on the phone; TalkBack, Reduced Motion and jank for the ARC path.

## 2026-10-17 — D18 accessibility, icon pass, full screenshot sweep (`claude/crystal-redesign`)

- `./gradlew :app:testDebugUnitTest :app:assembleLab` — PASS (150 JVM tests; new `CoreTest.textScaleSnapsToOfferedSizes`,
  `HomeEditorStateTest.previewIsOutsideHistoryUntilConfirmed` now checks `dirty`). Lab installed with `install -r`,
  profile broadcast, `speed-profile` compiled.
- Icons: the drawer header opens Settings with the same gear as the editor bar and the Settings title (was the
  Studio glyph). Search on Home/drawer, Settings search and the FLOW app filter show a magnifier at the left (was a
  bare ring or nothing). Verified on the phone (screens 10–12, 30).
- Duplicate removed: the placement switch ("Hybrid 4×6") was both in the Předlohy card and the editor bar; it stays
  only in the bar.
- "Neuložené změny" no longer asks after a preset was only previewed: `dirty` compares the layout under the
  preview. Verified: preview Vyvážená → ✕ closes at once, Home unchanged; a real change (placement) still asks
  (screen 71, then Zahodit).
- Selecting an element while a preset is previewed now drops the preview first (before, edits landed on the
  previewed layout and ✓ would have discarded them). Verified: preview → tap LENS → the own layout is shown.
- D18: Velikost textu 100/115/130 % in Pohyb, text a dotek; 130 % verified on Settings and the drawer (labels
  ellipsize, favourites' names touch at 130 %), set back to 100 %. Icon-only controls raised to 48 dp.
- Screenshots: `docs/screens/v0.1.0-2026-10-17c/` (42 shots).
- Not verified: TalkBack speech; PULSE inspector in the editor (tap on the camera opened nothing in this pass —
  not investigated); onboarding; Boční/Magazín save.
- Open: the editor's app picker has no filter (221 apps to scroll).

## 2026-10-17 — Preset thumbnails use their own LENS row; device pass (`claude/crystal-redesign`)

- `./gradlew :app:testDebugUnitTest :app:assembleLab` — PASS (149 JVM tests, new `DockTest.presetMiniatureUsesItsOwnLensRow`).
  Lab installed with `install -r`, profile broadcast, `speed-profile` compiled.
- `HomeGeometry.forLayout(spec)`: a preset thumbnail places LENS by the preset's own dock and offset, not the live
  one. On the phone Magazín's thumbnail shows LENS last while the live row is centred.
- On the phone (screenshots): previews of Terminál (mono clock, short date, filled LENS, named list), Plakát
  (huge slanted clock, weekday below), Sklo (clock on a frosted plate, row spread) and Vyvážená (light clock, four
  icons in a row above LENS) render; Použít closes the card and enables undo; ✓ after a preset asks "Přepsat vlastní
  rozložení?" (cancelled, then the editor closed via ✕ › Zahodit, Home unchanged); Mřížka plochy: + / − columns
  (4→5→4) and placement Mřížka / Hybrid switch and show at once (restored); Oblíbené lists 4/7 with ↑ and ✕;
  light theme on Vzhled, the main page and the drawer is legible (restored to Systém).
- Semantics tree (`uiautomator`) of the main Settings page: the decorative "→" on every row is exposed as text, and
  the miniature exposes the live time and date as content.

Fixed after the pass (owner approved), `:app:testDebugUnitTest :app:assembleLab` PASS (149), Lab reinstalled:
- The named app list takes the clock's own face at 22 sp (Terminál: JetBrains Mono); the legacy list keeps its
  30 sp serif. No schema change (app items carry no look).
- Editor cards (`InspectorCard`) raise the Dense body to at least .92 (`EDITOR_CARD_BODY`); Plakát's clock now only
  faintly shows behind the preset card.
- Edit corner ticks sit just outside each icon at its own size (`iconDp / 2 + 4`); LENS keeps 22 dp. On the phone
  all four Sklo icons, colour ones included, show their ticks.
- Oblíbené note: they head the drawer and presets build Home from them; the own layout stays.
- TalkBack: the "→" on Settings rows is cleared from semantics; the miniature is one node "Náhled plochy" (checked
  with `uiautomator`).
Not verified: real TalkBack speech; Boční / Magazín applied and saved.

## 2026-10-17 — Settings: one page per section, 50/50, distinct presets (`claude/crystal-redesign`)

- `./gradlew :app:testDebugUnitTest :app:assembleLab` — PASS (148 JVM tests). Lab installed with `install -r`,
  profile compiled (`speed-profile`).
- Studio is now *Nastavení* (gear in the header): search, then Vzhled first on a faint accent plate, then groups
  Plocha (Upravit plochu, Mřížka plochy, Ikony, Oblíbené), Ovládání (ARC, FLOW, PULSE, Gesta, Pohyb a dotek) and
  Data a systém (Záloha, Systém, O aplikaci); each is its own page. Each setting lives in one place: Ikony moved out
  of Vzhled; app shortcuts only under ARC; PULSE look, events, calibration and "everywhere" on one page.
- Preview / sheet 50/50 (`PREVIEW_SHARE` .5) on the main page, Vzhled, Ikony, Mřížka, Oblíbené and PULSE.
- Editor tray: *Nastavení* (gear) replaces Vzhled and opens the main page; Back returns to the editor
  (`StudioStart.Editor`).
- Icons up to 96 dp (`MAX_ICON`, `ItemSizes.ICON_MAX`); the miniature now draws LENS-row icons at their real size.
- Presets: eight (`HomeTemplates.PRESETS`): Prázdná, Kolem LENS, Vyvážená (apps in a grid row above a lone LENS),
  Magazín, Terminál, Plakát, Sklo, Boční. Each brings its own clock face / size / arrangement, date, LENS style and
  row edge or order; the wallpaper and the owner's row spacing stay. New `LayoutBase` values are appended (an older
  build reads them as `None`). Thumbnails draw clock size, plate, date, LENS style and row edge.
- Sliders: the bead stays inside the track's margins (it used to overhang onto the − button / label), rounded 4 dp
  track, halo while held, value in a pill, soft − / + discs. Editor card: only the end corner and end side show a
  grip mark; all five grips still size the card.
- On the phone (screenshots): Nastavení main page and Ikony page as above; editor tray shows Nastavení; preset strip
  previews Magazín (Bodoni stacked clock, capital date above, LENS last) and Boční (vertical row on the right).

Not verified: Terminál, Plakát, Sklo and Vyvážená previewed on the phone; applying a preset and ✓; grid sliders and
placement from Settings on the phone; favourites page; light theme; TalkBack on the new pages; thumbnails place
LENS by the current geometry, so after previewing Boční the others' LENS shifts right in their thumbnails.

## 2026-10-17 — Studio 60/40, Vzhled only in Studio (`claude/crystal-redesign`)

- Studio Main and Vzhled: under the header the live Home miniature takes 60 % of the room (scale fitted to it, about
  .5 on the Phone (3) instead of .22) and the settings sheet 40 %; the other pages keep the full sheet.
- The editor's Vzhled card is gone: the tray button asks about unsaved edits (as PULSE calibration does) and opens
  Studio › Vzhled; Back there returns to the editor. `StudioStart` (Main / Look / Lens) replaces `lensFirst`.
- Everything the card had now lives in Studio › Vzhled › Ikony: icon size for all (drops own sizes) and names under
  icons, applied to the active layout through `HomeLayouts.withIcons` (saved like the editor's ✓; on the linked
  preset a real change becomes the own copy). Removed the duplicate "Upravit plochu" row and hint from Vzhled, the
  editor-only `setLabels` / `resetIconSizes` and the strings `look_hint`, `studio_look_editor`.

`./gradlew :app:testDebugUnitTest :app:assembleLab` PASS (148 JVM tests: −1 editor test, +1 `withIcons` test).
Phone (Nothing Phone (3), lab build, `install -r`): Studio Main and Vzhled at 60/40 by screenshot; editor tray
Vzhled → Studio › Vzhled → Ikony open (style, size, names) → Back → the editor again.

Not verified: moving the icon-size slider and the names switch from Studio on the phone (owner's settings left as
they were), the unsaved-edits question on the way to Studio, TalkBack, the light theme at 60/40.

## 2026-10-17 — Studio preview drawn from the active layout (`claude/crystal-redesign`)

- `LayoutSpec.sketch()` (pure Kotlin, `HomeModel.kt`): the LENS row from the layout's `LensRow` items in screen
  order (slots −2, −1, LENS, 1, 2; LENS first / last for `lensAt` Start / End; empty slots stay gaps; no slots = LENS
  alone), the legacy text list from `Stack` items, and a free date above a free clock when its `fy` is smaller.
- Studio's `HomePreview` takes the active layout and `HomeViewModel.entry`; it no longer uses the favourites or the
  legacy `layout` field. A FLOW in the row draws a plain ring. Onboarding keeps the legacy path.
- Follow-up: the preview is a true miniature, the screen at 0.22 inside a phone outline to the right of the page
  title (was a 300 dp band at 0.78 with the clock cropped large). Clock and date are drawn at Home's size and shrunk as a
  picture (the date's size floor kept it large before); LENS 48 dp, icons 32 dp, row ~67 dp above the bottom edge.

Commands: `./gradlew -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleLab` PASS, 148 JVM tests, 0
failures (new `HomeSketchTest`, 5).

Verified on the Phone (3), lab build via `install -r` + profile + `speed-profile` (screenshots): Home shows the date
above the clock and Telefon / Zprávy / Fotoaparát / Chrome around LENS; Studio's preview now shows the same order,
and side by side with a Home screenshot the miniature keeps the clock and date widths (~78 % of the width) and the
date above the clock. The Studio sheet now starts at ~29 % of the screen height instead of ~33 % (a first try at
0.26 under the title, `4c13339`, pushed it down to ~38 %; corrected in the next commit).

Not verified: `lensAt` Start / End, a vertical dock edge (the preview stays a bottom row), a Pure or legacy-list
layout on the phone (JVM tests only). The preview still centres a free date that Home draws at its own `fx`.

## 2026-10-17 — D17: settings backup, Studio › Systém › Záloha (`claude/crystal-redesign`)

- `BackupCodec` (pure Kotlin): envelope `{"format":"intelia-settings","version":1,"settings":{…}}`; reads at most
  256 KB, refuses another format, a newer version or schema, damaged JSON; values go through `sanitized()`;
  `applyTo` keeps this phone's `pulseLens` and `editorCard`, sets `onboarded`.
- Studio › Systém › Záloha (fold, also found by search "záloha", "export", "obnovit"): Exportovat nastavení
  (`CreateDocument`), Obnovit ze souboru (`OpenDocument`) → a question with the file's FLOW / favourite / hidden
  counts → Nahradit nastavení; "Vrátit obnovení" until Studio closes. No permission, no network.

Commands: `./gradlew -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleLab` PASS, 143 JVM tests, 0
failures (new `BackupCodecTest`, 6).

Verified on the Phone (3), lab build via `install -r` (`uiautomator` + screenshots): export to Download
(`intelia-2026-10-17.json`, 4194 B; 2 FLOW, 4 favourites, wallpaper aurora, no `pulseLens`) → "Nastavení
exportováno."; restore of that file → question "FLOW příkazů: 2, oblíbených: 4, skrytých aplikací: 0" → "Nastavení
obnoveno."; a second export is byte-identical to the first; a JSON that is not a backup → "Tento soubor není záloha
Intelie." and nothing changed; Vrátit obnovení → the row goes away; Home bounds identical afterwards. The test files
were removed from Download; the owner's backup `intelia-2026-10-17.json` stays.

Not verified: a restore that really changes settings (only the same document was restored), a file from a cloud
provider in the picker, the 256 KB limit and a newer file on the phone (JVM tests only), TalkBack.

## 2026-10-17 — Regression pass: B5 labels, Studio in the light theme (`claude/crystal-redesign`, `da70807`)

Commands: `./gradlew -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleLab` PASS, 137 JVM tests, 0
failures. Lab build via `install -r`, profile broadcast, `pm compile -m speed-profile -f`.

Verified on the Phone (3) (`uiautomator` + screenshots; no setting kept changed):
- LENS inspector: size row "Vlastní ↺" (56 dp); tap → "Řídí se globální hodnotou", 44 dp; Zpět → 56 dp, Vlastní.
- Clock inspector: Font fold "Vlastní hodnota" above the faces; Colour fold "Řídí se globální hodnotou". Date
  inspector: same two folds, same labels. Editor left with ✕; Home bounds identical before and after.
- Light theme (Intelia theme is Systém, so only the system night mode was switched `no` → back to `yes`): drawer,
  Studio main, Vzhled and the settings search ("ztmaveni" → "Tapeta · Vzhled › Tapeta") legible on the light sheet.
- Semantics tree: the Studio search field is an EditText with the label "Hledat v nastavení"; a result row is one
  clickable, focusable node holding its title and path.

Found (fixed in "Studio preview drawn from the active layout"): Studio's live preview draws the favourites ("Moje") around LENS and the clock above the date,
while the owner's Home has the date above the clock and Telefon / Zprávy / Fotoaparát / Chrome in the LENS row.

Not verified: real TalkBack speech (not switched on; it is a system accessibility setting for the owner).

## 2026-10-17 — Home wallpaper drawn from one cached picture; drawer jank A/B; B5 on clock and date (`claude/crystal-redesign`)

- `WallpaperLayer`: an own wallpaper is rendered once per preset and size off the main thread
  (`WallpaperExport.render`, the same drawing) and drawn as one bitmap; the live lights + grain stand in until it is
  ready; half size on low-RAM phones. The dim stays a separate layer (slider stays live). Swatches keep live drawing.

Drawer open/close, 5 × swipe up on LENS + Back, two runs each, lab build, Phone (3) at 120 Hz (own wallpaper Aurora):
| variant | janky | p50 | p90 | GPU p50 |
|---|---|---|---|---|
| before (blur, live wallpaper) | 32.8 / 33.0 % | 25 / 28 ms | 40 / 42 ms | 16 / 15 ms |
| no backdrop blur (test only) | 22.9 / 21.5 % | 24 / 23 ms | 36 / 36 ms | 12 / 11 ms |
| no blur + cached wallpaper (test only) | 11.1 / 10.0 % | 16 / 16 ms | 27 / 27 ms | 11 / 11 ms |
| **after (blur + cached wallpaper)** | 23.1 / 20.9 % | 23 / 22 ms | 36 / 34 ms | 12 / 12 ms |
Every janky frame is "slow issue draw commands" (render thread / GPU), not the UI thread. Settled PSS 123 MB (was
107 MB; the picture is 10 MB at 1080 × 2392). Home looks the same (screenshot).

Follow-up on the owner's request: the backdrop blur radius is no longer animated — the level's full radius from the
first frame a surface opens, 0 once closed; the surface's scrim and fade carry the transition (`backdropBlurPx`,
`VeilTest`). Same drawer test: **16.9 / 17.1 % janky**, p50 19 / 19 ms, p90 30 / 32 ms, GPU p50 11 ms (was
20.9–23.1 %, p50 22–23 ms). A frame captured 280 ms into the opening shows the drawer almost fully in; the blur's jump
at the very start sits under the fading surface and was not judged frame by frame (no screen recording).

B5 on the phone: the clock's and the date's size rows show the label ("Vlastní hodnota" — the owner's own sizes);
editor left unchanged. Not checked: LENS inspector, Font and Colour folds.

Commands: `./gradlew -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleDebug :app:assembleLab` PASS.

## 2026-10-17 — Veil 3.0 B5: Globální / Vlastní in the inspectors (`claude/crystal-redesign`)

- One definition, `ItemProp` (`ui/Inheritance.kt`, pure Kotlin): Size, Font, Weight, Tint — where an element keeps
  its own value (`sizeDp`, clock `style` + `look.face` / date `look.face`, `weight`, `tint`), how it is dropped and
  where the global value is set (`StudioTarget.Icons` for app and FLOW icon size, else the kind's default).
  `HomeEditorState.inherit` returns it as its own undo step (key `…/inherit`, never merged with a slider drag).
- Inspectors draw it as one label (`InheritChip`): "Globální", or "Vlastní ↺" in the accent, which a tap returns to
  the global value. In the size row of app/FLOW icons, the clock, the date and LENS; the clock's weight row; at the
  top of the Font fold (clock, date) and the Colour fold (clock, date). TalkBack: "Řídí se: Vzhled › Ikony" /
  "Řídí se globální hodnotou" / "Vlastní hodnota, vrátit globální hodnotu".

Commands: `./gradlew -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleLab` PASS, 137 JVM tests, 0
failures (new `InheritanceTest`, 6).

Verified on the Phone (3), lab build via `install -r` (`uiautomator` + one screenshot): editor → Telefon → "Tato ikona"
shows Globální ("Řídí se: Vzhled › Ikony", 64 dp); − → 62 dp, label "Vlastní ↺" in the accent; tap on it → 64 dp,
Globální; − then Zpět → 64 dp; leaving the editor kept Home unchanged (Telefon bounds identical before and after).

Not verified: the labels on the clock, date and LENS inspectors on the phone, real TalkBack speech. Not done: the
editor tray "Vzhled" still opens the editor's own Look card, not Studio › Vzhled — that card carries the per-layout
rows (all-icons size, names, through the editor's undo) that Studio does not have, and leaving the editor mid-edit
needs the save/discard question; left for an owner decision.

## 2026-10-17 — Startup after a reboot: optimised `lab` build, package storm folded, icons at background priority (`claude/crystal-redesign`)

- Found on the Phone (3) 4 min after a reboot (debug Lab as HOME): 23 s of CPU in Intelia since boot (main thread 8 s,
  JIT 2 s), 19.7 % janky frames, p99 900 ms; the package is `DEBUGGABLE`, compile status `verify` (no AOT).
- D16: build type `lab` (R8, not debuggable, `.lab`, shared debug key) + `profileinstaller` with the merged baseline
  profile (`app/src/main/baseline-prof.txt` for the app package).
- `AppRepository`: package events within 400 ms fold into one list load (a reboot or an update reports dozens);
  the first `onStart` no longer loads the list a second time.
- `IconCache`: drawer warm-up runs one icon at a time on one background-priority thread and stops at 7/8 of the
  cache; the cache is ⅛ of the heap class (4–16 MB, halved on low-RAM phones); a package event redraws icons only
  if one of its icons was cached.
- Backdrop blur is off on low-RAM devices (`ActivityManager.isLowRamDevice`).

Commands: `./gradlew -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleDebug :app:assembleLab` PASS
(lab APK 3.4 MB, debug 15.7 MB).

Verified on the Phone (3), `install -r` each way, Home layout unchanged after (`uiautomator`): profile installed via
`androidx.profileinstaller.action.INSTALL_PROFILE` → `speed-profile`. Home cold start after `am force-stop`
(system relaunch, `Displayed`): debug 469 / 481 / 508 ms → lab 198 / 204 / 212 ms; CPU in the first ~13 s after start:
debug 5 s → lab 1 s; total PSS at rest ~148 MB (debug) → 107 MB (lab).

Reboot (`adb reboot`, lab, 4 min of uptime, same moment as the debug snapshot above): Intelia CPU since boot 3 s
(debug 23 s), flat from minute 1 to 4; 245 frames, 6.1 % janky, p50 17 ms, p90 23 ms, p99 200 ms, slow UI thread 9
(debug 689 frames, 19.7 %, p99 900 ms, slow UI 122 — the owner used the phone more then, so frames compare only
roughly); compile status survives the reboot (`speed-profile`); PSS 153 MB with 71 MB swapped under boot pressure.

Not verified: other phones; the 400 ms fold and warm-up priority are not measured separately from the build change. Open: opening the drawer is GPU-bound at 120 Hz (lab, 3× swipe: 29–39 % janky, p50 25–31 ms,
slow UI thread only 6 frames) — the backdrop blur and wallpaper, not CPU.

## 2026-10-17 — Veil 3.0 B4: Studio in five sections with search, one Look definition (`claude/crystal-redesign`)

- Studio main: live preview, "Hledat v nastavení", Upravit plochu (active layout name), then Vzhled / Ovládání /
  Akce a FLOW / Systém, each with its current state as the subtitle; Plocha is the editor (no copy of Home settings).
  Each section is a page that slides in (B6 motion); Back keeps the main page's query and scroll.
- One definition: `LookSettings` (Ikony, Panely, Tapeta, Motiv, Barvy) is drawn by both the editor's Vzhled panel and
  Studio › Vzhled; the editor adds only the per-layout rows (icon size, names, through undo).
- Search: `StudioIndex` over `StudioTarget` entries (name, "Sekce › Část", keywords cs/en), diacritics-insensitive via
  `AppSearch`, name hits first; a result opens its page with that part unfolded, only on a tap.
- Ovládání = ARC, PULSE, Pohyb a dotek; Akce a FLOW = Spravovat příkazy, Vybrat zkratky (Back returns to the page it
  came from); Systém = default home (only when not held), Skryté aplikace, O aplikaci a licence.

Commands: `./gradlew -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleDebug` PASS, 131 JVM tests, 0
failures (new `StudioIndexTest`).

Verified on the Phone (3), debug Lab via `install -r` (`uiautomator` + two screenshots; settings byte-identical
after): main shows the five rows with live subtitles; "ztmaveni" → one result "Vzhled › Tapeta" → Vzhled with Tapeta
open (swatches, Ztmavení, Nastavit jako tapetu systému); Back → main with the query kept; Ovládání shows ARC / PULSE /
Pohyb a dotek; Akce a FLOW → Spravovat příkazy → Back returns to Akce a FLOW; Systém shows Skryté aplikace and
O aplikaci a licence.

Not verified: TalkBack in the search field and rows, the editor tray "Vzhled" opening Studio › Vzhled (the tray still
opens the editor's own Look card, which draws the same definition), Studio in the light theme. Not done: the
Globální / Vlastní inheritance (B5), Systém › záloha and přístupnost rows from the prototype (no such features yet).

## 2026-10-17 — D13 ④: an own wallpaper saved as the system wallpaper (`claude/crystal-redesign`)

- `android.permission.SET_WALLPAPER` (normal, install-time; D13). `ui/WallpaperExport.kt` renders the preset as Home
  draws it (lights, the same 64 px grain, dim) at the screen's portrait size (≤ 4096 px) and calls
  `WallpaperManager.setBitmap` off the main thread with `FLAG_SYSTEM` or `FLAG_SYSTEM | FLAG_LOCK`.
- Editor › Vzhled › Tapeta, only with an own preset chosen: "Nastavit jako tapetu systému" → hint "Nahradí současnou
  tapetu systému…" + Plocha / Plocha i zámek / Zrušit → "Ukládám…" → "Uloženo jako tapeta systému." (or "Systém
  tapetu nepřijal."), a polite live region. Intelia keeps its own wallpaper selected.

Commands: `./gradlew -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleDebug` PASS, 126 JVM tests, 0
failures (new `WallpaperExportTest`: portrait size, cap keeps the shape, lock screen only when asked).

Verified on the Phone (3), debug Lab via `install -r`: the permission is granted at install; Zrušit returns to the
action and changes nothing; with the owner's consent Oceán → Plocha: "Uloženo…", `dumpsys wallpaper` shows the system
wallpaper as a static `ImageWallpaper` (`mWhich=1`, `SET_STATIC`) and the lock screen still on Nothing Atmosphere
(`mWhich=2`); Home on the system wallpaper looks like the drawn Oceán. The test choice of Oceán in Intelia was removed
again (the owner's other edits made meanwhile kept). The owner re-picks Atmosphere for Home by hand.

Not verified: Plocha i zámek, a refusal path (device policy), TalkBack reading the result, a light preset with dim.

## 2026-10-17 — Veil 3.0 B6: motion per the prototype, gfxinfo (`claude/crystal-redesign`)

- Owner's choice: Calm keeps the brisker durations; B6 adds the prototype's curve and the page motion (DESIGN_SYSTEM
  §5.6 "Veil 3.0 B6"). `ui/Motion.kt`: `VeilMotion.ease` (cubic-bezier .2/.9/.25/1.04) on Home → Editor and the
  inspector entry; `VeilMotion.slide/page` — pages above Home slide 180 ms deeper / back, Home ↔ page still fades.
- Příkazy opened from Studio return to Studio (from the drawer: Home). Studio's scroll and open section survive a
  detail page (Příkazy, Skryté aplikace, Zkratky, Čočka); cleared when leaving for Home. All through `motionMs`.

Commands: `./gradlew -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleDebug` PASS, 123 JVM tests, 0
failures (new `MotionTest`: slide only between deeper pages; curve ends at 0/1 with 0–2 % overshoot).

Verified on the Phone (3), debug Lab via `install -r`, 120 Hz, adb input:
- Studio with ARC open, scrolled to Spravovat příkazy → Příkazy → Back: Studio again, ARC still open, the row at the
  same bounds (`uiautomator`); same for Zobrazit skryté aplikace → Back.
- `gfxinfo`, 3 series of 3 × LENS → Search → Back → Back, warm: before (HEAD `75e7599`) 19–28 janky of ~294,
  p99 36–57 ms; after 23–26 janky of ~289, p99 46–53 ms — same within noise (first series after install, cold: 59).
- 3 series of 3 × Studio → Příkazy → Back (after only; before, Back went Home): 16–17 janky of ~127, p50 12–13 ms,
  p99 105–125 ms — the slow frames are composing the incoming page in a debug build (interpreted, ~2.5× UI time).

Not verified: a release-build measurement, the slide caught mid-way (180 ms is shorter than `screencap`), the
system Reduce motion switch, TalkBack. The owner's own Home edits made meanwhile (LENS row WhatsApp / Keep /
calculator, editor card position) were left as they are. Next: owner picks (system wallpaper, Studio B4, B5).

## 2026-10-17 — Veil 3.0 B3: Intelia wallpapers and dim, date on the plate, tray semantics (`claude/crystal-redesign`)

- B3 scope (owner's choice): D13 ④ wallpapers drawn by the launcher. Icons, fonts and colours were already in place.
- `data/Wallpapers.kt`: 13 presets (7 dark, 6 light), each a ground and 2–3 soft lights; drawn once and cached by
  `WallpaperLayer` with a faint grain against banding, nothing animated, no permission, no asset. The system
  wallpaper stays untouched; while an own one is shown the window drops `FLAG_SHOW_WALLPAPER`.
- `LauncherSettings.wallpaper` (empty = system) and `wallDim` (0–60 %, light presets 0–35 % — darker turns them mid
  grey, where neither type colour reaches 4.5:1); sanitized; absent in older documents = system, undimmed.
- Home type follows the own wallpaper exactly (`Wallpapers.wantsDarkText`); on the system one a dim ≥ 30 % flips a
  light wallpaper to light type. Editor › Vzhled › Tapeta: swatches (Systém + presets, radio semantics) and Ztmavení.
- Date on a plate: a bound date, or a free date whose centre lies on the clock's plate, takes the plate's ink at full
  opacity with no wallpaper shadow (was white on the light plate). Tray: Přidat and the placement are plain buttons,
  the placement speaks "Umístění: …" with the action "Změnit".

Commands: `./gradlew :app:testDebugUnitTest :app:assembleDebug` PASS (new `WallpapersTest`: type ≥ 4.5:1 on every
ground and ≥ 3:1 on every light at every allowed dim; `VeilTest.dateOnAPlateStaysLegible`; `Zone.centredIn`).

Verified on the Phone (3) (settings backed up and restored byte-identical): light theme — the owner's free date on
the light plate is dark and legible; editor with LENS on the right side and on top (Omezit pohyb = Motion Off): bar,
row and tray clear of each other; Vzhled › Tapeta › Oceán applies at once; Papír at 20 % gives dark type and dark
status bar icons; the window flag returns with the system wallpaper.

Follow-up check (same build, owner's document with `wallpaper=ocean`, `wallDim=10`, LENS row on top, pushed via
`run-as` and restored byte-identical): drawer, Search (typing "chr" → Chrome) and Studio are legible over Oceán;
drawer and Search blur the own wallpaper together with Home, Studio's live preview shows it. The owner's free date
sits between the top LENS row and the clock, clear of both — the overlap noted earlier did not reproduce.

Not verified: real TalkBack (only the semantics tree via `uiautomator`), the system Reduce motion switch (Compose
honours the animator scale; not toggled on the owner's phone), surfaces over a light own preset. Not done:
saving a preset as the system wallpaper (`SET_WALLPAPER`, its own commit per D13), own image. Next: owner picks.

## 2026-10-17 — Veil 3.0 B2: Home defaults, editor at 86 %, bar + tray, inspector placement (`claude/crystal-redesign`, `e440b0b`, `53355d0`)

- Home: new installs already get the pure clock (Crystal, row, no plate) and no icon names; now pinned by
  `HomeModelTest.freshInstallHasThePureClockAndNoNames`. Existing documents are not migrated (the owner's stacked
  plate clock stays). Icon names never overlap: `labelWidths` shares the gap between neighbours on one line and keeps
  clear of LENS; a name under 48 dp is left out (on the default ±62/±108 dp row the outer pair loses its names).
- Editor: Home is one layer scaled to 86 % around 50 %/52 % (`EditView`); gestures and zones stay in Home
  coordinates, PULSE stays at the camera. Top bar 48 dp: ×, undo/redo, edited name, Hotovo (no Studio button).
  Bottom tray (Veil Light): Přidat, Předlohy, placement (tap cycles), Vzhled; hidden while a card is open.
- Inspectors: `inspectorAbove` picks the side where the card fits whole, the roomier one otherwise; above never covers
  the clock when it fits below. Placed from the element's settled (86 %) position; no `VeilDepthSource`.

Commands: `./gradlew :app:testDebugUnitTest :app:assembleDebug` PASS (gradlew fails with `MSYS_NO_PATHCONV=1`;
set it only for adb).

Verified on the Phone (3), dark and light theme: editor bar clear of the top clock, tray below the LENS row, names
whole and apart, Chrome card above its icon and clear of the clock, clock card below the clock, Přidat opens the add
menu. The owner's saved card position (`editorCard`) still wins over automatic placement; `intelia.xml` unchanged.

Not verified: TalkBack in the bar and tray, Reduced Motion (scale animation uses `motionMs`), side LENS rows in the
editor, a top LENS row with the tray, fresh install on the phone. Known: in the light theme the date on a light
plate has low contrast (not changed here). Next: B3 per the prototype order (Studio B4 and inheritance B5 later).

## 2026-10-17 — Veil 3.0 B1: one material, shared depth backdrop (`claude/crystal-redesign`)

Change: `ui/Veil.kt` computes every panel from one formula (body + accent tint + 1 dp crystal edge light at the top →
accent at the bottom); Veil / Crystal / Frost / Liquid / Matte are `VeilPreset` data, not separate drawings. Opacity
Light 64 / Dense 80 / Focus 74 % (48 / 66 / 60 % only while the system blurs the wallpaper). New `ui/Depth.kt`: Home
under Search and the drawer is blurred 18 dp (`RenderEffect`), scaled to 97 % and covered by the tinted theme scrim;
the app options sheet over Home blurs it 5 dp with the scrim at 60 %. Blur is never stacked (one radius, the stronger).
Cross-window wallpaper blur is used only where `isCrossWindowBlurEnabled`. No settings change, no migration.
Verified:
- `./gradlew -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleDebug` — PASS; 112 JVM tests, 0 failures
  (VeilTest rewritten: approved opacities, one material across presets, ≥ 4.5:1 over the worst wallpaper on Light and
  Focus for every preset with its light included, plates, scrim, single blur radius).
- Phone (Nothing Phone (3), debug Lab via `install -r`; `intelia.xml` backed up, byte-identical after the tests):
  - `ro.surface_flinger.supports_background_blur=0`, `wm disable-blur` → "Blur supported on device: false": no
    cross-window blur, the no-blur opacities apply.
  - Search: Home behind is really blurred and scaled (screenshot), results card and field legible.
  - Drawer (Focus 74 %): legible over blurred Home, edge visible at the top.
  - Bug found and fixed: the app options sheet over the drawer let the rows below show through (74 % + scrim). Over a
    text layer the sheet now takes the Matte preset; screenshot shows no ghost text.
  - Studio panel (Focus 74 % straight over the wallpaper): legible.
  - `dumpsys gfxinfo`, 5× open/close Search (keyboard included): with blur 474 frames, janky 12.0 %, p50 13 / p90 25 /
    p95 34 / p99 61 ms; same build without blur 481 frames, 9.4 %, 13 / 21 / 27 / 48 ms; slow UI thread 34 both. The
    cost is GPU during the transition only, so blur is skipped in battery saver and with Reduce motion (scrim only).
Not verified: light theme, Reduce motion and battery-saver fallback on the phone, the Dense sheet over Home (Home's
long-press opens the editor), a device with cross-window blur, TalkBack.
Open: B2 (Home + Editor).

## 2026-10-17 — Settings as one-open folds, no duplicates, no dead options (`claude/crystal-redesign`, `811eae3`)

Verified:
- `.\gradlew.bat -Dorg.gradle.jvmargs=-Xmx2g -Pkotlin.compiler.execution.strategy=in-process :app:testDebugUnitTest
  :app:assembleDebug` — PASS; 111 JVM tests, 0 failures, no compiler warnings.
- Phone (Nothing Phone (3), debug Lab via `install -r`; owner's `intelia.xml` backed up and restored, byte-identical):
  - Vzhled opens with four closed folds (Ikony · Tónované, Panely · Veil, Motiv · Systém, Barvy · Podle míst); opening
    Panely closed Ikony; the open fold has an accent plate, accent title and ×.
  - Panely: „Matné“ is a fifth material; choosing it hid Hustota and kept Světelná hrana; Veil brought Hustota back.
  - Hodiny card: only Velikost and Datum open, then Písmo / Čas / Barva a krytí / Pozadí folds. Písmo is one list
    (styles + faces); „Strojové“ changed the clock to mono and the Tloušťka slider disappeared (no weights).
  - LENS card: Velikost and Kroužek/Plný open; Řada LENS / Rozestupy / Poloha folded with their values.
  - Studio: Upravit plochu, ARC (Kruhy: 3), Spravovat příkazy, PULSE (Jen na ploše), Pohyb a dotek, Skryté aplikace,
    O aplikaci; ARC opened in place with an accent plate. Palette, material and icon style are no longer in Studio.
Not verified: Date and PULSE cards on the phone; light theme; TalkBack on the folds.
- Follow-up: Studio's live preview now draws Home's own clock and date items (face, size, weight, colour, plate;
  a free date follows the clock) and shrinks them to the room above LENS. Phone: the preview showed the stacked
  Unbounded clock on its Veil plate with „SO 17. 10.“, as on Home; before it showed an Ink clock. Build PASS, 111 tests.
Open: D13 ④ wallpapers not started.

## 2026-10-17 — Editor card grows upwards, one size for all icons, clock under the status bar (`claude/crystal-redesign`, `8acce00`)

Verified:
- `.\gradlew.bat -Dorg.gradle.jvmargs=-Xmx2g -Pkotlin.compiler.execution.strategy=in-process :app:testDebugUnitTest
  :app:assembleDebug` — PASS; 111 JVM tests, 0 failures. Without the smaller heap the daemon died: the PC's commit
  charge was full (31.5 / 32.5 GB) and a stale Kotlin daemon held 2.3 GB (`hs_err`: "insufficient memory").
- Phone (Nothing Phone (3), debug Lab via `install -r`; owner's `intelia.xml` backed up and restored, byte-identical):
  - Bug found: an icon card in the LENS row opened above the icon, then grew downwards when Pokročilé opened and
    covered the edited icon. Fixed: with Pokročilé open, Fotoaparát stays visible below the card.
  - Icon card: „Tato ikona“ and „Velikost všech ikon“; Vzhled: „Velikost všech ikon“ 48 → 56 dp by four taps on +
    made all four row icons the same 56 dp (Zprávy, which had its own size, followed).
  - Styles Barevné / Mono / Veil / Tónované and shape Slza applied to the row icons.
  - Vzhled opens on Ikony; Panely (Veil) and Motiv (Systém) are folds; Motiv opens with theme and accents.
  - Home: the date/clock now start right under the status bar (date top at 136 px, before 251 px).
Not verified: clock text 12 dp from the side edges with a left/right fx (only the plate case was on screen); light
theme; TalkBack labels of the new sliders.
Open: D13 ④ wallpapers (not started); in the editor the bar overlaps a clock placed at the very top.

## 2026-10-17 — LENS row spacing, compact editor bar, text higher (`claude/crystal-redesign`, `6e60c45`, `11f3f1d`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 111 JVM tests, 0 failures (new in `DockTest`:
  wider row keeps its steps and a too-wide one shrinks to fit, spread fills the edge, spacing clamped on restore).
  The Gradle daemon crashed twice (JVM crash / "Test process encountered an unexpected problem"); `gradlew --stop`
  and a rerun passed.
- Phone (Nothing Phone (3), debug Lab via `install -r`; owner's `intelia.xml` backed up and restored, byte-identical):
  - LENS inspector shows „Rozprostřít po hraně“, „Odstup od LENS“ (62 dp) and „Rozestup aplikací“ (46 dp).
  - Rozprostřít on: the four bottom apps spread over the edge. Off, Rozestup 46 → 70 dp by twelve taps on +: the
    apps move apart and the names Fotoaparát / Chrome no longer overlap.
  - Editor bar: one row of 32 dp pills (Předlohy, Vzhled, undo/redo, the edited element's name, Studio, ✓) right
    below the status bar; the clock and date sit 48 dp higher on Home than before.
Not verified: undo of a slider run as one step on the phone (covered by the same code path as item sliders); spacing
on side and top rows; LENS první / poslední with spread on the phone; TalkBack on the smaller bar (32 dp targets).
Open: D13 ④ wallpapers (not started this session); free placement of single row apps beyond spacing.

## 2026-10-17 — LENS row on any edge, icon disc shapes, screenshot sweep b (`claude/crystal-redesign`, `7da1855`, `f78f659`, `db5d450`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 108 JVM tests, 0 failures (new: `DockTest` ×6 —
  bottom row unchanged, side row runs down the edge and keeps the content clear, top row pushes the area down,
  LENS first/last order, ARC faces the centre and input maps back, old/unknown dock names fall back; icon shape
  default/unknown/round trip).
- Phone (Nothing Phone (3), debug Lab via `install -r`; owner's `intelia.xml` backed up and restored after each test
  run, byte-identical):
  - LENS inspector → Řada LENS: Vlevo / Nahoře / Vpravo move LENS and its four slots to that edge; LENS první puts
    all four after it; the ghost ARC turns with the edge.
  - Saved, real ARC from the left, top and right rows: rings open towards the centre, slide-to-select picks the
    slot under the finger (Claude, Instagram), ring names readable; on top the ARC words sit at the bottom.
  - Veil icons drawn as Kruh, Squircle, Zaoblený čtverec, Slza.
  - TalkBack (switched on by the owner): double-tap-and-hold opens the editor, activating LENS selects it; the size dot's
    custom actions Větší / Menší were exercised by the owner on the phone — they work (not driven through adb).
  - Light and dark theme × Veil / Krystal / Mráz / Kapka on Home (screenshots 61-*).
- Screenshots: `docs/screens/v0.1.0-2026-10-17b/` (35 shots), old `v0.1.0-2026-10-17/` removed (in history).
Not verified: Search opening from a side or top row (it stays at the bottom by design, not checked for overlap);
a side row in landscape-sized windows; slot drag-and-drop between a side row and the grid by finger.
Open: D13 ④ wallpapers.

## 2026-10-17 — Remembered editor card, faint grips, bolder plate glass, Look/Presets as cards, size dots (`claude/crystal-redesign`, `5ac8c7f`, `a6b507b`, `780911a`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 101 JVM tests, 0 failures (new: editor card
  clamped + round trip, plate glass bolder and ≥ 4.5:1, every bundled font has Czech glyphs).
- Phone (Nothing Phone (3), debug Lab via `install -r`; owner's `intelia.xml` backed up and restored at the end,
  byte-identical; forced `user_rotation` restored to the owner's values):
  - Rotation: with `user_rotation 1` forced, Home stays ROTATION_0 (portrait).
  - Czech: „sobota 17. října“ drawn whole in Flex, Inter, Space Grotesk, DM Serif, Bodoni, Unbounded; in JetBrains
    Mono the last word was missing (wrapped onto a hidden second line) — fixed, now drawn whole, smaller.
  - Studio with unsaved changes asks Pokračovat / Zahodit / Uložit; Zahodit opens Studio and Home is unchanged;
    Uložit opens Studio and the change is kept.
  - Editor card moved by its header, saved with ✓, `am force-stop`, editor reopened: the card opens at the saved
    spot (`editorCard` x 83.5 / y 479.5 dp in the prefs).
  - Vzhled and Předlohy open as the same card (header, ⟲, shared position).
  - Size dots: icon 48 → 64 dp and LENS 44 → 56 dp by dragging the dot; the LENS dot lies outside the ring.
  - Crystal and Liquid plates: facets / outline clearly visible in the dark theme.
  - Accessibility tree (uiautomator): header „Přetažením posuneš editor“, „Vrátit editor na místo“, card marks
    „Šířka / Výška / Velikost editoru“, plate „Šířka / Výška pozadí“, „Změnit velikost hodin / ikony / LENS“.
Not verified: real TalkBack (custom actions Větší / Menší not exercised); light theme; new screenshot set (old
`docs/screens/v0.1.0-2026-10-17/` predates this entry).
Open: D13 ③ dock/LENS anywhere + icon shapes; new screenshot set.

## 2026-10-17 — Editor card that stays put, moves and sizes; 1:1 text drag; glass plates; screenshots (`claude/crystal-redesign`, `b856da0` + follow-up)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 98 JVM tests, 0 failures (sanitizer: plate
  stretch clamped, glass plate names kept, unknown dropped).
- Phone (Nothing Phone (3), debug Lab via `install -r`; owner's `intelia.xml` backed up, restored after every sweep,
  byte-identical). Screens in `docs/screens/v0.1.0-2026-10-17/` (66 shots):
  - Dragging the free date: the text now follows the finger 1:1 (centre at the drop point, ±0 px after the first
    drag); before, it moved ~2/3 of the finger's way and jumped.
  - Date → Font → Svisle: the card stays whole on screen above the navigation bar (before, half of it fell off the
    bottom); the card picks the roomier side and caps its height.
  - Card header names the element (Hodiny, Datum, PULSE, LENS, app label), also under the editor bar; dragging the
    header moved the card, the ↘ arrow widened and lengthened it, ⟲ put it back.
  - Clock plate: Veil / Krystal / Mráz / Kapka chips, stretched sideways by its ↔ handle (52 dp) and by sliders.
  - Seen: Pod sebou (no date between lines), Šikmo, all six separators (— = none), 12 h, alignment chips, every
    face incl. Unbounded (font chips each in their own face), date Dlouhé/Krátké/Den/Vlastní, ABC/abc, svisle,
    pinned under the clock with Posun od hodin, date plates Veil and Kapka; light and dark theme × four glasses.

Not verified: Home locked to natural orientation (`nosensor`) — phone not rotated; Czech letters ěščřž in the fonts
(the date only shows í); Studio (unsaved-changes dialog in the way); TalkBack on the card header and handles.
Changed behaviour: clock and free date are placed by their centre, so the owner's clock (fx 0.107) sits 25 dp further
left than before.

## 2026-10-17 — Distinct glasses, PULSE by tap, fonts, clock/date typography (`claude/crystal-redesign`, `dbacc69`, `a8d71ee`, `ae92400`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 98 JVM tests, 0 failures (new: TextLook clamped and
  kept only on clock/date, unknown face name decodes to Auto and a document without `look` to defaults, 12 h digits,
  own date format and case in Czech).
- Phone (Nothing Phone (3), debug Lab via `install -r`; owner's `intelia.xml` backed up, restored, `cmp` identical):
  - Editor → Vzhled, the four glasses side by side (dark theme): Veil with its accent edge, Krystal with a diagonal
    facet, Mráz grey and grainy, Kapka with a lit rim round the panel — told apart at 40 % screenshot size.
  - Editor: tap on the PULSE capsule beside the camera (status bar hidden in the editor) opens the PULSE inspector;
    the editor bar and clock did not move when the status bar hid.
  - Clock inspector shows Písmo / Čas folds; Čas → Pod sebou stacked the time. First try drew the date between the
    two lines (negative spacing); fixed with trimmed line boxes — the fix is built and installed but **not seen**.

Not verified: the fixed stacked layout, Šikmo, separators, 12 h, every face, the date options (Own format, case,
vertical, offset, own Veil plate) on the phone; light theme for the new glasses; PULSE tap with PULSE everywhere on
(service not enabled on the phone); TalkBack on the new folds.

## 2026-10-17 — Glass styles, clock accent on the wallpaper (`claude/crystal-redesign`, `31bd339`, `0a316ce`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 95 JVM tests, 0 failures (new: edgeless
  glasses keep text ≥ 4.5:1 over a white wallpaper with their light included, Veil keeps its edge, `glass` decodes
  and an unknown or absent value stays Veil).
- Phone (Nothing Phone (3), debug Lab via `install -r`; owner's `intelia.xml` backed up, restored, `cmp` identical):
  - Editor → Vzhled: chips Veil / Krystal / Mráz / Kapka switch the Look panel at once; "Světelná hrana" shows
    only for Veil. Studio → Barvy shows the glass and icon chips with their own headings.
  - Bug found and fixed: Light theme over the owner's dark wallpaper painted the clock in the dark accent (hard to
    read) while the date stayed light; after the fix the clock is light terracotta.
  - Light theme + Matné: editor panel, Search (Moje list, field) and drawer legible.

Not verified: how far the three glasses differ by eye at the owner's +10 % density (they look close; the owner to
judge); Frost grain and Liquid bloom only seen in the Look panel; TalkBack; release frame measurement.

## 2026-10-17 — Palette, Tinted icons, Veil clock, Clean Veil, TalkBack LENS in the editor (`claude/crystal-redesign`, `27c5a9b`, `f9d328c`, `d52226c`, `763891d`, `1554151`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 92 JVM tests, 0 failures (new: accent contrast
  ≥ 4.5:1 on both Veil bases, enum order kept, unified/per-place palette and old/odd documents, Tinted decode,
  clock background sanitizing, text contrast on Veil Light over white/black wallpaper).
- Phone (Nothing Phone (3), debug Lab via `install -r`; owner's `intelia.xml` backed up, restored and byte-identical):
  - Palette (test document, per place): clock in Rose, Search field and selected row in Amber, Studio toggles in
    Violet; Studio → Barvy shows the toggle, place chips and the 17-disc grid; choosing Hodiny → Sky recoloured the
    preview's colon at once.
  - Tinted icons in Teal on Home (dark theme, light part of the wallpaper) and in the drawer (light theme).
  - Clock inspector "Veil" → ✓: plate behind time and date; first try clipped a 126 sp clock, fixed by taking the
    plate inset out of the margin (screenshot after the fix: full time).
  - Clean Veil before/after screenshots of the drawer, Search and the clock plate: deeper, less grey fill, crisper
    edge; text legible.
  - Editor, keyboard Tab to LENS + Enter: LENS inspector opens, Search does not.
  - Editor icon drag (`input motionevent`, 300 px up, held): icon centre 257 px vs finger 255 px — no slop lag.
- At the end the release Lab (debug key, `compile -m speed`) is back on the phone; HOME role holder
  `dev.intelia.launcher.lab`.

Not verified: TalkBack itself (double tap goes through the semantics click, changed in code; Enter goes through the
key handler) — owner to try. Tinted icons on a truly light/dark wallpaper (wallpaper not changed; light vs dark
theme checked instead). No release frame measurement this session (drawing changes are fills and colours only).

## 2026-10-16 — Drawer composed ahead, brisker motion (`claude/crystal-redesign`, `130df1a`, `cf4f122`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 85 JVM tests, 0 failures.
- Release build (debug key, `.lab`, `compile -m speed`), Nothing Phone (3) at 120 Hz, `gfxinfo framestats` of one
  swipe up → drawer: before, the first frame spent 23.6 ms on the UI thread (whole `LazyColumn` + icon loads) and
  the first two frames took 33 / 25 ms; after (drawer composed 700 ms after Home rests and kept parked off-screen,
  icons warmed by `IconCache.warm`, `contentType`, `ModulateAlpha` fade) the first frame spends 2.8 ms on the UI
  thread, 13 / 20 ms in total.
- Same build, 3 series of 3 × swipe up → scroll → Back: before 6–12 janky of 532–540 (1.1–2.3 %), p99 27–44 ms;
  after 9–12 of 514–516 (1.7–2.3 %), p99 16–22 ms. GPU time per frame is 7–10 ms median in both and in Search too
  (Nothing's live blur wallpaper is in the frame); not drawer-specific.
- Brisker motion (Calm durations about −25 %, list in DESIGN_SYSTEM §5.6): 3 series of 3 × LENS → Search → Back →
  Back: 9–14 janky of 304 (3.0–4.6 %), p99 17–26 ms (previous entry: 4.4–5.0 %, p99 18–24 ms).
- Home after the change: `uiautomator dump` lists no drawer nodes while it is parked; swipe up opens it ("Aplikace").
- Owner's `intelia.xml` backed up and restored byte-identical (`cmp`).

Not verified: the owner's feel of the drawer in the debug build (debug is much slower than release). The debug
reinstall was done with `adb uninstall`, so the Lab lost its other app data; the HOME role holder is now
`com.nothing.launcher` and the Lab PULSE accessibility service is not enabled — the state before was not recorded.

## 2026-10-16 — LENS inspector clear of the ghost ARC, TalkBack moves, drag slop (`claude/crystal-redesign`, `7780a44`, `1861107`, `f361373`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 85 JVM tests, 0 failures (new: `ArcRings.reach`,
  `HomeEditorState.withSlop`).
- Phone (Nothing Phone (3), debug Lab; owner's `intelia.xml` backed up, restored and byte-identical at the end):
  - LENS selected in the editor, centred and at +104 dp (this phone's edge limit): the inspector card sits above
    the outermost ghost ring, all three rings visible (screenshots).
  - LENS saved at +104 dp, tap → Search recorded (`screenrecord --output-format=raw-frames`, 60 fps): the first
    changed frame shows the field growing from LENS on the right, then the card and IME; `SearchField` takes `dx`
    from the active layout.
  - Editor: `input swipe` LENS 131 px (50 dp) left from +104 dp ended at 55 dp, its centre 685 px vs the finger at
    682 px (previously it lagged by the slop).

Not verified: the new TalkBack actions — `uiautomator dump` does not list custom actions, so only build and code
review; owner to try with TalkBack. Icon drags in the editor share the slop fix but were not measured. No release
measurement this session (no rendering-path change).

## 2026-10-16 — LENS moves in the Home Editor (`claude/crystal-redesign`, `7dca980`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 83 JVM tests, 0 failures (new: `Reach` → `dx`
  migration and bounds, LENS drag/snap/cap/one-step undo, narrow-screen clamp, LENS-row slots folding at an edge,
  presets keep LENS).
- Phone (Nothing Phone (3), owner's `intelia.xml` backed up, restored and byte-identical at the end):
  - Editor: dragging LENS left moved it to −83 dp with the LENS row re-laid around it, inspector "Poloha" showed it;
    one Undo returned it to the centre ("střed") and disabled Undo; dragging right capped at +120 dp; ✓ saved.
  - After saving: LENS at the right, all four LENS-row apps folded to its left; tap opened Search; hold opened ARC
    with its rings bent around the right-hand LENS.
  - Release build (debug key, `.lab`, `compile -m speed`), LENS centred, 3 series of 3 × LENS → Search → Back →
    Back: 14–16 janky of ~316 (4.4–5.0 %), p99 18–24 ms, longest UI frame 10.9–11.7 ms — no regression against
    `61484cf` (15–16, p99 21–25, UI 10–19).

Not verified: the Search morph's start point with LENS off centre (only the end state seen), ghost arcs while the
inspector is open (partly covered by the card), TalkBack on the Poloha slider. Owner to try: LENS drag feel,
pinch, Veil icon style, the morph recording from the previous session.

## 2026-10-16 — Search open split, options sheet over layers, phone checks (`claude/crystal-redesign`, `61484cf`, `41eb836`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 78 JVM tests, 0 failures.
- Phone (Nothing Phone (3), Android 16; owner's `intelia.xml` backed up first, byte-identical at the end):
  - Debug (Lab) numbers are not representative: the debuggable build cannot be AOT-compiled (`dexopt` stays
    `verify`). A release build signed with the debug key and installed over Lab (`compile -m speed`) gives 14–18
    janky of ~310 (4.5–5.8 %), p99 28–29 ms at `7fc3707`.
  - systrace of the open frame (debug): composition 22 ms (Search 19: card 9.4, field 5.6; Home 0.3 ms, so Home
    does not recompose on `page`), apply 10 ms, measure/layout 20 ms; then focus + IME setup ~18 ms before the next
    frame. Accessibility services on the phone (Kea, MT Island) add 6–12 ms of semantics work after layout changes.
  - Card composed two frames after the field (`61484cf`, release, 3 runs): janky 15–16 (4.8–5.1 %), p99 21–25 ms,
    longest UI frame 10–19 ms (was 21–26). Tried and dropped: one row per frame (each frame re-measured the card,
    26 ms each in debug), a layer translation instead of `offset` for the IME ride, an offscreen layer for Home
    under Search (no measurable gain). Frames while the IME rides are GPU-bound (5–10 ms, also with Home cached),
    and the first frame after ~0.5 s idle pays a GPU context re-init (~16 ms, HWUI "GPU Context timeout").
  - Reduced Motion (Pohyb Off): screenshot ~120 ms after the tap already shows the full pill, card and scrim.
  - Long-press on a Search row opens the options sheet; Search stays (query kept, IME hidden), Back returns to it.
    The rows showed through the Dense sheet; now Focus + 60 % scrim over Search/drawer (`41eb836`), checked.
  - Enter: "ma" → Mapy (first lit row) launched; "pocty" → nothing ran while typing, Enter ran the FLOW
    (Kalkulačka, then Hodiny).
  - Light theme: Search card and pill readable over the dimmed Home (clock faintly visible through the card).
  - Morph ring → pill recorded with `screenrecord` (720p) and sent to the owner; not analysed frame by frame here.

Not verified: TalkBack, light wallpaper. Not done: LENS drag in the editor (needs a continuous LENS x instead of
`Reach`), 18 ms row stagger (the card fades in during the last 40 % of a 170 ms morph — no room for 6 × 18 ms
without lengthening it). Owner to try: pinch in the editor, Veil icon style.
During the session one measuring run tapped into an Instagram modal that was on top after an install (script now
aborts unless Lab is on top), and one test tap opened HBO Max from the Search card.

## 2026-10-16 — Search and drawer in Veil over Home (`claude/crystal-redesign`, `53b9769`, `a59a4a2`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 78 JVM tests, 0 failures (new `SearchPicksTest`:
  favourites on a blank query and Enter does nothing, exact FLOW first and not repeated, cap 6 with apps before
  FLOW names, a partial FLOW name completes and never runs).
- Phone (Nothing Phone (3), Android 16, Lab; owner's `intelia.xml` backed up first, byte-identical at the end):
  - LENS tap: Home (clock, date) stays visible dimmed; Veil pill above the keyboard; empty query shows MOJE
    (Chrome, Fotoaparát, Zprávy, Telefon). "ma" → 6 rows, first (Mapy) lit; "pocty" → FLOW card with 1 ⟶ 2 icon
    chain, Spustit and ENTER SPUSTÍ; nothing launched while typing (top activity stayed Lab).
  - One Back closes Search to Home (IME and layer together).
  - Swipe up: drawer as Veil Focus sheet over dimmed Home (Aplikace 221, MOJE row, A–Z with rail, FLOW and Studio
    buttons); its Hledat field opens Search, typing works, Back returns to Home.
  - Measurements: see DESIGN_SYSTEM §5.10 (janky frames 83–85 → 43–45 for 3 cycles, p99 150 → 61–65 ms).

Not verified: the ring → pill morph frame by frame (only end states seen), Reduced Motion, long-press options from
Search, Enter key on the phone, light theme / light wallpaper, TalkBack. Still open from before: pinch and Veil icon
style (owner to try), LENS drag in the editor.

## 2026-10-16 — Customisation moves into the Home editor (`claude/crystal-redesign`, `bfc5c8e` + data commit before it)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 74 JVM tests, 0 failures (new: date unpin/move/pin
  back, clock and date hide + add back once, labels and icon sizes as one undo step, Veil/icon settings clamped,
  date keeps its own size range and format).
- Phone (Nothing Phone (3), Android 16, Lab build; owner's `intelia.xml` backed up first, restored, `cmp` identical):
  - Editor bar shows Předlohy · Vzhled · undo/redo · Studio · ✓; Vzhled opens the Look panel above the LENS row
    (Panely Veil/Matné, Hustota with − / +, Světelná hrana, Motiv, accent swatches, Ikony Barevné/Mono/Veil,
    velikost, popisky, PULSE entry); clock and date stay visible.
  - Icon style Mono: before this change every monochrome icon was an opaque white disc (AdaptiveIconDrawable
    wrapper on Android 16); after the fix the glyphs render (Telefon, Zprávy, Fotoaparát, Chrome checked).
  - Tap on the date opens its own inspector (Dlouhé/Krátké/Den, size 9–32 with − / +, Připnout pod hodiny,
    Pokročilé, Skrýt datum). Unpinning keeps it in place (653 px vs 672 px pinned); dragging moved it to 1069 px.
  - PULSE inspector opens from the Look panel and hangs below the bar. A tap on the camera ring itself is taken by
    the PULSE everywhere overlay (accessibility service on) and opens its Quick View instead.
  - Studio now lists PLOCHA (hint + Upravit plochu → opens the editor on Home), ARC, PULSE (PULSE všude +
    poloha kamery), pohyb, výchozí plocha, skryté aplikace, o aplikaci; Upravit plochu checked.

Not verified: pinch to resize (adb `input` has no multi-touch — owner to try), Solid panels and density on the
phone,
Veil icon style after the fix, clock hide/add back on the phone, theme switch from the panel, TalkBack, light
wallpapers. Not done: LENS → Search in Veil (prototype), drawer as Veil over Home, part 7 measurements.

## 2026-10-16 — Intelia Veil V2, parts 4–6 (`claude/crystal-redesign`, `00d62f5` … `2fa6217`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 70 JVM tests, 0 failures (new: item look
  clamping, slider edits collapse into one undo step, remove never takes LENS, add-at snaps without pushing aside,
  preset preview outside history, date toggle, nudge by cell and on the LENS row).
- Phone (Nothing Phone (3), Android 16, Lab build, dark wallpaper unchanged; the owner's `intelia.xml` was backed up
  first and is byte-identical afterwards):
  - Clock inspector (Veil Dense, 312 dp) opens below the clock, the clock and date stay visible; typeface chips
    change the clock live; size handle drawn at the clock corner; date toggle; weight / colour / opacity under
    "Pokročilé".
  - App inspector opens above the icon: size 32–64 dp, Posunout ← ↑ ↓ →, Popisek, Odebrat z plochy, "Další volby"
    (old sheet). → on the LENS-row phone icon swaps it with Messages; undo in the bar restores it.
  - LENS inspector: size, Kroužek / Plný, faint ghost ARC rings.
  - Tap on empty Home → "Přidat sem" (Aplikace, FLOW, Widget · později), placed below the point when above would
    cover the clock; Aplikace → list → adidas lands at the tapped spot; Back → Zahodit removes it again.
  - Předlohy: strip above the LENS row with thumbnails Prázdná / Kolem LENS / Vyvážená / Vlastní, tap previews in
    place, Zrušit returns; after Použít, ✓ asks "Přepsat vlastní rozložení?" (Zrušit keeps editing). Editing the
    active own layout and ✓ saves without a question (date removed in the saved document; then restored).
  - LENS tap → Search: field focused and the IME up in the same transition, field above the keyboard; typing
    "chr" shows Chrome and launches nothing; Back hides the IME, Back again returns Home.
  - LENS touch area now 84 × 100 dp: tap at −44 dp from the LENS centre opens Messages (before: Search, measured on
    100 × 100); −40 dp and centre → Search; swipe up → drawer; hold at −38 dp → ARC, release on LENS cancels.
  - `dumpsys gfxinfo` (adb input, 120 Hz): 3 × LENS → Search → Back: 599 frames, 12.85 % janky, p50 8 ms,
    p90 24 ms, p99 133 ms, 76 slow UI thread. Icon drag with 22 `input motionevent` moves: 88 frames, 26 % janky,
    p50 6 ms, p90 25 ms (adb spawns a process per move, so this is not a finger drag).
  - PSS settled 15 s after a cold start: 112.9 MB (Graphics 46 MB); about 2 s after start 320 MB was read once.
    Cold start `am start -W`: 622 / 466 / 439 ms.

Fixed during the phone check: the editor bar's undo/redo did not redraw (plain-Kotlin state); the preset strip
covered the clock it was previewing; the add menu could cover the date; → on a LENS-row icon did nothing.

Not done / not verified: light and coloured wallpapers; TalkBack itself (custom actions Přesunout / Upravit /
Odebrat z plochy are in the semantics, TalkBack was not switched on); the morph frames themselves (screencap is too
slow to catch 170 ms); Reduced Motion on the phone; wallpaper zoom-out; dragging LENS; FLOW items on Home are drawn
and run on tap but were not added on the phone; the 12.85 % jank and the 320 MB transient are not analysed.

## 2026-10-16 — Intelia Veil V2, parts 1–3 (`claude/crystal-redesign`, `9f2b215`, `fc1c7a4`, `4af8723`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 62 JVM tests, 0 failures (new: `VeilTest`,
  `HomeModelTest` — schema 1 → 2, empty, corrupted, round trip, save, grid re-clamp; `HomeEditorStateTest` — grid
  snap, displacement, LENS-row swap, LENS zone, blocked clock, guides, Hybrid, undo/redo ≤ 50, clock band).
- Phone (Nothing Phone (3), Android 16, Lab build, dark wallpaper unchanged):
  - Studio panel is Veil Focus, the app sheet Veil Dense; text sharp, edge faint. At Dense 78 % the Home icons
    showed through the sheet, so Dense is now 80–85 % with a 40 % scrim; the sheet now reaches under the nav bar.
  - Home after the schema-2 migration looks identical to before (screenshots compared; Essentials, clock, date).
  - LENS tap → Search, swipe up → drawer, LENS hold → ARC: unchanged.
  - Long-press empty Home → editor (bar: undo, redo, "Hybrid 4×6", Studio, ✓); dragging an icon shows grid dots,
    target ring and the LENS zone; holding an icon opens the editor and drags it at once (first attempt was
    cancelled by swapping the parent's modifiers mid-gesture — fixed by a stable chain); LENS hold in the editor
    does not open ARC; Back with changes asks Pokračovat / Zahodit / Uložit, without changes it just leaves.
  - ✓ saves once: document `schema 2`, active `mine`, favourites intact, position survives force-stop. Afterwards
    the owner's original document was restored from a backup taken before the test (Home back on the preset).

Not done / not verified: light and coloured wallpapers (need the owner's consent to change the wallpaper);
LENS target still 100 × 100 dp (84 × 100 not changed, not measured); inspectors, preset thumbnails, add menu,
wallpaper zoom-out, TalkBack actions in the editor, `gfxinfo`/PSS. Tapping an icon in the editor opens the old
app sheet as a stand-in for the inspector. A saved own layout no longer follows favourite changes from the sheet;
choosing a preset in Studio switches back to the linked preset (the own layout is kept).

## 2026-10-16 — Intelia Veil + Home Studio V2, design phase A (`claude/crystal-redesign`)

Verified (no Android code changed, no build run):
- Phone (read-only `getprop` / `dumpsys`, no input): `ro.surface_flinger.supports_background_blur=0`,
  `mBlurEnabled=false`, battery saver off, SDK 36; wallpaper is the live
  `com.nothing.blurwallpaper.AtmosphereWallpaperService`. Cross-window blur is not available on this phone.
- One phone screenshot (FLOW list, the owner was using the phone): still Ink style (serif, terracotta, mono caps).
- Prototype `docs/design/veil-prototype.html` in the built-in browser: no console errors; scenes clock
  inspector, icon move demo (grid dots, target ring, drop with label), LENS → Search (field above the keyboard,
  FLOW `pocty` card), ARC on the light wallpaper. Published privately as
  https://claude.ai/artifact/5oVfKwJrveYy1d9YkZN1Ss.

Proposed then (approved later the same day, see D12): D12 (Veil material, origin edge, blur policy, Home layout model, Home Editor,
LENS target 84 × 100 dp). Not tested: whether the Atmosphere wallpaper honours `setWallpaperZoomOut`.

## 2026-10-16 — Redesign phase B2, Home + LENS (`claude/crystal-redesign`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 44 JVM tests, 0 failures
  (new: `newInstallGetsCrystalClockExistingUsersKeepTheirs`).
- Installed on Nothing Phone (3), Android 16 (`dev.intelia.launcher.lab`, default HOME), dark wallpaper:
  - Before: serif split clock, LENS ring with the terracotta dash, Essentials 30 dp icons.
  - After install the owner's document kept `clockStyle` Serif (shown as "Ink"); date is Roboto Flex caps at 62 %.
    Studio → Písmo hodin shows Crystal · Ink · Grotesk · Strojové; picking Crystal gives a one-voice `14:17` clock.
    The owner's clock style is now Crystal (set in this test; the owner chose to keep it).
  - LENS: ring measured 114 px = 43.4 dp at 420 dpi, no dash. Tap → search with keyboard; swipe up → drawer
    without keyboard; hold → existing ARC (rings, LENS settles to 86 %), release on LENS cancels.
  - Essentials: centres measured at ±62 / ±108 dp from LENS on its baseline, 40 dp icons, no backing.
    Tap Telefon → Dialer, tap Fotoaparát → Nothing Camera; Home key returns to Lab.
  - Recents (Nothing quickstep) → Home key → Lab `MainActivity` on top.
- The phone is 420 dpi = 411 × 911 dp, not the 360 dp of the prototype; the Essentials x values of 4.3 are
  therefore implemented as offsets from LENS (±62 / ±108 dp), and LENS stays at bottom inset + 52 dp.

Not verified on the phone: light wallpaper (dark ink via `HINT_SUPPORTS_DARK_TEXT`; the owner's wallpaper was
not changed), Reduced Motion with the new LENS press (durations go through `motionMs`, so 0 ms when Off),
Pure and Custom layouts after the change, TalkBack. Known overlap: the 100 dp LENS target covers the inner
8 dp of the two inner Essentials icons (same as the prototype's lens zone); LENS wins there.

## 2026-10-16 — Redesign phase B1, design system and components (`claude/crystal-redesign`, 9fbc7cf)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; `testDebugUnitTest --rerun`: 43 JVM tests,
  0 failures (new: `ActionTest` ×6, `CrystalArcGeometryTest` ×2, `newInstallGetsFrostExistingUsersKeepTerracotta`).
- Installed on Nothing Phone (3), Android 16 (`dev.intelia.launcher.lab`, update 13:29). Home screenshots before
  (13:28) and after (13:29) are identical apart from the time: serif clock, date, Essentials, LENS, PULSE ring;
  the owner's existing settings kept the Terracotta accent (schema 0 → 1 migration on a real document).
- D11: Roboto Flex (OFL 1.1) bundled as `res/font/roboto_flex.ttf`, licence in `assets/licenses/`.

Not verified on the phone: the Frost/Silver swatches in Studio (no touch input this session; the owner was
using the phone), rendering of the Crystal components and Roboto Flex axes (no screen uses them yet; B2).
Not measured: APK size impact of the font (+1.7 MB file; debug APK is 15.0 MB).

## 2026-10-16 — Redesign phase A, design review (`claude/crystal-redesign`)

Verified (no Android code changed, no build run):
- Phone screenshots (Lab build, default HOME): Search with query, FLOW exact match `pocty`, drawer (223 apps),
  PULSE tapped over Calculator with `PulseAccessibilityService` running; only one overlay window
  (`Intelia PULSE`, 94 × 94 px at 493,21, `ACCESSIBILITY_OVERLAY`).
- MT Capsule 17.2 inspected read-only: its accessibility and notification services run, portrait screen
  profile is off so it draws no window; granted permissions include INTERNET, AD_ID, notification listener.
  Nothing in it was changed.
- Prototype `docs/design/crystal-prototype.html`: no console errors; scripted pointer test in the browser —
  hold LENS → 5 slots, slide to the right end → set FLOW, slide up → slot 3 `běh`, release → run;
  hold the camera on the app screen → Global ARC, slide down → FLOW `vlak`, release → run; Studio ARC
  shows 3 sets.

Owner approved D10 (direction A, arc-end set switching, PULSE 2.0) on 2026-10-16.
Open: ergonomics of the arc ends and of
Global ARC not tested on the phone (prototype only). Roboto Flex licence entry before bundling (done in B1, D11).

## 2026-10-16 — Ink rework (`claude/ink-rework`, 402d10a)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS (21 JVM tests, incl. new
  `pulseLensIsClampedAndNonFiniteFallsBackToAuto` and `pulseAlarmHoursStayWithinADay`).
- On Nothing Phone (3), Android 16, `dev.intelia.launcher.lab` debug build:
  - Onboarding (3 steps); Home shows only the wallpaper; Essentials around LENS.
  - Tap LENS → search with keyboard; swipe up → drawer; results grow upwards; Enter opens the first app.
  - Exact FLOW match shows *Spustit*; Enter runs the FLOW in the right order; typing runs nothing.
  - App options; ARC ring 1 (apps) and ring 2 (FLOW launched on release); Studio with live preview; light theme.
  - PULSE capsule with battery while charging.
  - Camera calibration (Studio → PULSE → Poloha kamery): ‹ › change the value by exactly 0.5 dp; *Uložit* →
    Studio shows "Vlastní" and values survive reopening; *Automaticky* → "Automaticky", detected from
    `cutoutPath` as 205.5 / 26.0 / 8.0 dp. Capsule sits around the camera on Home (1 screenshot, deleted).
  - With Lab as HOME: Recents (Nothing quickstep fallback) shows only wallpaper behind the cards, no rectangle;
    Home from Recents, from an app (Calculator launched from LENS) and from the drawer returns a clean Home;
    Studio shortcut picker works only as HOME; ARC ring 3 ("ZKRATKY") appears and selects a shortcut by angle
    (−40° = 1st shortcut); returning to LENS cancels. Test shortcuts removed, rings set back to 2.
- Debug build timings: cold start ~845 ms, warm ~57 ms; PSS ~131 MB.
- PULSE toggles (Lab started with `am start`, not HOME; on AC power): *Ukázat nabíjení* off → no capsule
  while charging, on → capsule with 90 % is back. *Ozvěna při návratu* on → a ring leaves the camera right after
  returning to Lab from the system home; off → no ring at the same moment. All PULSE toggles left on.
- D6 (ring 2 from the centre, `ac65740`): `:app:testDebugUnitTest :app:assembleDebug` PASS (22 tests). On the phone
  the only ARC FLOW (`pocty`) sits straight above LENS in ring 2 (before: far left) and is selected by sliding
  straight up; sliding back onto LENS and releasing cancelled it (nothing launched). Not run by release this time.
- D7 (capsule width/opacity): `:app:testDebugUnitTest :app:assembleDebug` PASS (23 tests, new
  `pulseCapsuleWidthAndOpacityAreBounded`). On the phone, tapping the Studio sliders set +20 dp and 70 %; the
  live capsule at the camera in Studio grew and turned translucent, and Home showed the same capsule. Both set
  back to +0 dp / 100 %. Dragging the bead (rather than tapping) and TalkBack slider actions not tested.
- Near-alarm capsule (owner set an alarm for 10:00; checked 09:46 while charging): the capsule shows bell + 10:00
  on the left and the battery ring + 90 % on the right. **Problem:** its left half covers the system status-bar
  alarm icon and the 90 % repeats the system battery. Status bar is 126 px (48 dp) high.
- D8 (status-bar capsule, no text at rest): `:app:testDebugUnitTest :app:assembleDebug` PASS (26 tests, new
  `PulseShapeTest`). On the phone (AC power, 90 %, Lab via `am start`): at rest only a 28 dp disc around the
  camera with the terracotta battery ring, x ≈ 503–577 px, clear of the status icons (left end ≈ 445 px,
  right start ≈ 770 px). Studio preview shows the disc plus the alarm dot (38 dp). Tap just below the camera
  (540,165) opens the panel from the lens (alarm 5:40 po 19. 10., 90 % nabíjí se, torch) with the status icons
  readable on top; Back closes it; a tap outside closes it.
- MT Capsule (`com.pryshedko.mtisland`): its accessibility service is on but it had no window at rest while
  charging; its expanded state was not seen (needs the owner). Taps at the camera itself (y < 126 px) go to
  SystemUI's status bar, or to the old `dev.intelia.launcher.debug` "Intelia Pulse" accessibility overlay
  (frame 498,24–582,108 px), which opened that build's drawer once; nothing was changed in either.
- MT Capsule explored (owner's permission): single tap can be scroll, screenshot, launch app, torch, a card of
  shortcuts / sliders / barcodes, or app search; slider cards are volume, brightness, torch strength with toggles
  for auto brightness, volume mode, torch, screenshot and the Wi-Fi panel. With its service on it still had no
  window, so its own lens tap was never seen working. Its single tap was switched to "Shortcuts / Sliders" and
  a Volume card exists from the exploration. `.debug` "Intelia Pulse": a lens tap opens its drawer activity;
  media control there needs notification access.
- D9 (PULSE everywhere): `:app:testDebugUnitTest :app:assembleDebug` PASS (33 tests, new `PulseMenuTest`).
  The owner switched off `.debug` "Intelia Pulse" and switched on "Intelia PULSE" (`.lab`); MT Capsule stayed on.
  Overlay `TYPE_ACCESSIBILITY_OVERLAY` at 493,21, 94 × 94 px. A tap on the lens (540,66) opens the menu on Lab
  Home, in Lab Studio and in Calculator; Back closes it (the app below keeps its state), a tap outside and
  *Sbalit* close it. On Lab Home the old tap target below the status bar is gone while the service runs.
  Each control on the phone: volume + / − (music 8 → 9 → 8), sound ↔ vibrate (VIBRATE → NORMAL → VIBRATE), torch on/off,
  play/pause (a media session went to playing and back to paused), alarm row → Clock, Wi-Fi panel, Internet
  dialog, Bluetooth, sound and display settings, notifications and quick settings shade, screenshot (the menu
  is not in the picture; test file deleted), lock (phone went to doze with lockscreen).
  Studio → PULSE → *PULSE všude* opens the system accessibility list. The first build opened the per-service
  details page instead, which needs the privileged `OPEN_ACCESSIBILITY_DETAILS_SETTINGS` permission and crashed Lab; fixed.
- PULSE menu header (3c37b28): `:app:testDebugUnitTest :app:installDebug` PASS (34 tests, new `menuStartsBelowStatusBarAndLens`).
  The sheet now starts below the status bar; a ring around the camera and a thin thread join it to the lens.
  Checked in Calculator: the clock, notification and system icons stay visible, Back closes the menu.
  `uiautomator dump --windows` does not list accessibility overlays, so the menu nodes could not be read from
  adb; `dumpsys accessibility` does list the window as `TYPE_ACCESSIBILITY_OVERLAY`.
- Owner checked by hand (reported "all OK"): PULSE menu previous / next track with music playing, rotation to
  landscape with the menu open, TalkBack in the menu and on the Studio sliders.
- Studio sliders (0ed5e3d): `:app:testDebugUnitTest :app:installDebug` PASS (34 tests). Dragging the opacity bead
  set 70 % at x = 540 px, 55 % at 300 px and 100 % past the end (left at 100 %); dragging *Svisle* in camera
  calibration showed 59.5 dp, Back stored nothing (prefs still 206 / 26 / 8). The slider node is now a focusable
  `SeekBar`; it reads the shown value ("70 %", "26.0 dp") and steps by 5 % / 0.5 dp, instead of a percentage of
  its range (70 % on 40–100 was 50 %). TalkBack itself was not re-run on this build by Claude.
- PULSE menu strip (b05d826, owner chose a translucent strip over the thread): a translucent band as wide as the
  lens ring runs from the camera into the sheet; on Lab Home and in Calculator the clock, notification and
  system icons stay uncovered.
- D8 rest state with a real alarm (owner set 12:30, checked 11:32, AC, 90 %): on Lab Home only the disc with the
  battery ring and the alarm dot, no text, the system alarm icon uncovered; the PULSE menu reads "12:30 dnes".
- Lab is now the default HOME (`cmd package set-home-activity`, owner's choice). ARC ring 3: rings set to 3,
  Clock shortcut *Spořič obrazovky* added; releasing on it opened `deskclock/ScreensaverActivity`, Home returned
  to Lab. a735638 fills ring 3 from the centre like ring 2: the shortcut moved from the far-left slot to straight
  above LENS and was launched by sliding straight up and releasing. Left as set (3 rings, 1 shortcut).
  Its icon covers the "FLOW" ring caption while selected.

Not verified:
- A phone without a cutout.
- Performance on a release build; battery.

## 2026-10-16 — new PC setup (`claude/ink-rework` from `main` 012140f)

Verified:
- `main` = merged PR #1 (012140f). The docs commit `4ac6078` (DESIGN_HANDOFF.md) was pushed after the merge
  and was not in `main`; cherry-picked onto `claude/ink-rework`.
- New Windows PC: JDK 17.0.20.1, Android SDK with platform 37.1 + build-tools 36.0.0/37.0.0, Gradle 9.5.0 wrapper.
  `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS (16/16 JVM tests).
- Phone: Nothing Phone (3) A059, Nothing OS 4.1 (B4.1-260810-1153), Android 16 / API 36, USB debugging authorised.
- Installed: `dev.intelia.launcher.debug` (old fork build) and `dev.intelia.launcher.dev` (this repo).
  **Default HOME is now `dev.intelia.launcher.dev`.**
- Installed `.dev` APK is signed by a debug key with SHA-256 `730a1bbe…59f8`; this PC's debug key is `FA:62:AA:87…5E:D5`.
  A plain `adb install -r` from this PC would fail on signature mismatch; nothing was reinstalled or uninstalled.
- Baseline on the installed `.dev` (debug build): HOME cold start `am start -W` TotalTime 678 ms; warm HOME 8 ms wait;
  PSS ≈123 MB after settling. No Intelia crashes in the crash buffer.

## 2026-10-15 — standalone foundation (`claude/standalone-foundation`)

Rewritten from scratch as a standalone repository (no Kvaesitso code). Concepts and our own
Intelia UI code (clock glyphs, LENS/ARC gesture, FLOW rules) were re-implemented on a new core.

Verified:
- `./gradlew :app:testDebugUnitTest :app:assembleDebug` — PASS (16 JVM tests: FLOW, search, settings codec, ARC gesture).
- Installed `dev.intelia.launcher.dev` on Nothing Phone (3) A059, Android 16 (API 36), via USB and started it with `am start`:
  - Home renders over the system wallpaper with drawn clock and Czech date.
  - LENS tap opens search; `map` returns Mapy and Mapy.com with icons.
  - Hold on a result → *Přidat do ARC*; hold LENS, slide up, release → Google Maps launched (top resumed activity).
  - FLOW editor created `vlak` = Mapy → Mapy.com; typing `vlak` shows the preview card; *Spustit* launched both in order (Mapy.com on top).
  - Saved FLOW survived reinstall.

Not verified / not done:
- Intelia was **not** set as the default home on the phone; HOME-button reset, Recents and rotation untested.
- Claude Design project (`Intelia Launcher.dc.html`) not yet imported — visuals are the previous Intelia direction.
- Work-profile and private-space apps, TalkBack, large font scale, light theme on device.
- No performance or battery measurements.
- Icon styles/icon packs, backup/restore, Pulse — not ported.

# Status

Only verified facts. Format: `date | branch/commit | evidence | change | open`.

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

Not verified:
- Launching an app shortcut by releasing in ring 3 (available shortcuts led into personal apps).
- PULSE near-alarm capsule: the next alarm is >2 days away (outside the 24 h window), so it was not shown;
  no alarm was set for the test.
- Performance on a release build; battery.
- The PULSE panel cannot be opened via adb while MT Capsule's overlay is running (it catches taps on the camera).

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

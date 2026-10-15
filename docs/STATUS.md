# Status

Only verified facts. Format: `date | branch/commit | evidence | change | open`.

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

# Intelia Launcher

A fast, quiet, local-first Android home screen. Written from scratch in Kotlin and
Jetpack Compose; not a fork of another launcher.

- **LENS** — the single control at the bottom of Home. Tap to search, swipe up for the app drawer, hold to open ARC.
- **ARC** — up to three rings above LENS: five apps, your FLOW commands, and app shortcuts (shortcuts need Intelia as the default home). Hold, slide, release.
- **FLOW** — name a command (for example `vlak`) that opens up to four apps in your order. Typing shows a preview; it runs only when you press Run, press Enter on the exact name, or release on it in ARC.
- **PULSE** — a quiet capsule around the camera on Home for charging and a near alarm, plus an echo from LENS when you return. Camera position is detected automatically or calibrated in Studio in 0.5 dp steps.
- **Search** — local, diacritics-insensitive (`kalkulack` finds *Kalkulačka*), ranked by prefix, word start, initials, then substring.
- Wallpaper-only Home in the "Prstenec a nit" (ring and thread) style, Studio with live preview, light/dark/system theme, Material You, mono, sage, slate or terracotta accent, left/centre/right reach, hidden apps.

No accounts, network access, analytics or background work.

## Build

JDK 17+ and Android SDK platform 37.1.

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

```bash
scripts/dev.sh
```

`scripts/dev.sh` installs the debug build (`dev.intelia.launcher.lab`) on a USB-connected phone and opens it.
To use it as your home screen, open Studio in Intelia and choose *Use Intelia as home screen*.

## Licence

Apache License 2.0 — see [LICENSE](LICENSE).

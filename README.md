# Intelia Launcher

A fast, quiet, local-first Android home screen. Written from scratch in Kotlin and
Jetpack Compose; not a fork of another launcher.

- **LENS** — the single control at the bottom of Home. Tap or swipe up to search; hold to open ARC.
- **ARC** — five quick-launch slots on an arc above LENS. Hold, slide to an app, release.
- **FLOW** — name a command (for example `vlak`) that opens up to four apps in your order. Typing shows a preview; it only runs when you press Run.
- **Search** — local, diacritics-insensitive (`kalkulack` finds *Kalkulačka*), ranked by prefix, word start, initials, then substring.
- Wallpaper-backed Home with drawn clock, light/dark/system theme, neutral, Material You, sage or slate accent, left/centre/right reach, hidden apps.

No accounts, network access, analytics or background work.

## Build

JDK 17+ and Android SDK platform 37.1.

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

```bash
scripts/dev.sh
```

`scripts/dev.sh` installs the debug build (`dev.intelia.launcher.dev`) on a USB-connected phone and opens it.
To use it as your home screen, open Settings in Intelia (hold the clock area) and choose *Use Intelia as home screen*.

## Licence

Apache License 2.0 — see [LICENSE](LICENSE).

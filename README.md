# Intelia Launcher

**English** · [Čeština](README.cs.md)

> **Your phone. Your rules.** A fast, quiet, local-first Android home screen, written from scratch in Kotlin and
> Jetpack Compose. Not a fork of another launcher.

**Status: 0.1.0 Beta 1 — prerelease.** Tested on one phone (Nothing Phone (3), Android 16). Expect bugs; see
[Known limitations](#known-limitations) and the [release notes](docs/release/v0.1.0-beta.1/RELEASE_NOTES.md).

No accounts, no network access, no analytics, no background workers. Everything optional is off until you turn it on.

## What it does

| Pillar | What you get |
|---|---|
| **LENS** | One control at the bottom of Home. Tap = search, swipe up = app drawer, hold = ARC. |
| **ARC** | Hold LENS, slide to a slot, release. Up to several named sets of apps, FLOW commands and app shortcuts (shortcuts need Intelia as the default home). Releasing back on LENS cancels. |
| **FLOW** | Named commands (for example `train`) that open 1–4 apps in your order. Typing only previews; a FLOW runs on **Run**, **Enter** on the exact name, or release on its ARC slot. Never while typing. |
| **PULSE** | A quiet capsule around the camera: charging, a near alarm, music (with notification access), quick actions and gestures. On Home by default; everywhere only if you enable the optional accessibility service. Camera position is detected or calibrated in 0.5 dp steps. |
| **Global ARC** | With the PULSE service on: hold the camera area over any app and slide to an ARC slot. |
| **Search** | Local and diacritics-insensitive (`kalkulack` finds *Kalkulačka*), ranked by prefix, word start, initials, then substring. |
| **Studio** | All settings in five areas (Look, Home, Controls, Actions and FLOW, System and data), each with a live, non-interactive preview, and a search that jumps to the row. |
| **Home Editor** | Grid, free or hybrid placement, presets, per-item inspectors (icon, label, clock, date, LENS position), undo/redo. |
| **Look** | Light/dark/system theme, Intelia Veil glass, preset accents or your own colour (contrast-checked), Intelia wallpapers, seven bundled OFL fonts, icon shapes and labels. |
| **Controls** | Home and LENS gestures, PULSE gestures, haptic strength, hardware-key diagnostics and remap while Intelia is in front. |
| **Backup** | Export and restore all settings as one JSON file through the system file picker. |
| **Languages** | English and Czech. On Android 13+ pick the language per app (Studio › System and data › Language). |

## Install

1. Download the APK from the [Releases](https://github.com/ondrejmacko-it/Intelia-Launcher/releases) page and check
   its SHA-256 against the release notes.
2. Allow installing from your browser or file manager when Android asks, and install.
3. Open Intelia → Studio → System and data → **Home screen** to make it your home screen (optional; you can always
   switch back in Android's settings).

Requires Android 8.0 (API 26) or newer. Only tested on Android 16.

### Packages

| Package | What it is |
|---|---|
| `dev.intelia.launcher` | The distribution build. Signed with the project's release key (certificate SHA-256 `5ddb08b2…dd99d3`, see [SECURITY.md](SECURITY.md)). |
| `dev.intelia.launcher.lab` | The development build (Lab). Development build, never published. Its old debug key was public in the git history — never install a Lab APK from anyone. |

They install side by side and never update each other. Settings are not shared; move them with Backup.

## Permissions

| Permission | Why | Without it |
|---|---|---|
| `SET_WALLPAPER` (normal) | Saving an Intelia wallpaper as the system wallpaper, only after you confirm. | Everything else works. |
| `VIBRATE` (normal) | Touch feedback at the strength you choose. | Falls back to system haptics. |
| Default home role (optional) | Being your home screen; app shortcuts in ARC. | Intelia works as a normal app. |
| Accessibility service *Intelia PULSE* (optional) | PULSE and Global ARC over other apps, hardware-key remap. It reads no screen content. | PULSE and keys work on Home only. |
| Notification access (optional) | Reading the active media session for music in PULSE. No notification is read or stored. | No music in PULSE. |

## Known limitations

- Beta, tested on a single device (Nothing Phone (3)). Other OEMs, screen sizes and Android versions are unverified.
- No widgets from other apps yet, no icon packs, no cloud sync.
- Hardware keys and PULSE outside Home need the optional accessibility service; OEMs may restrict it.
- Quickstep (recents/gesture animations) stays the system's; third-party launchers cannot replace it.
- No performance or battery figures are claimed until they are measured.

Full list: [docs/audit/UX_BUGS.md](docs/audit/UX_BUGS.md) and the
[0.1.0 baseline](docs/audit/BETA_0.1.0_BASELINE.md).

## Roadmap

See [docs/product/MASTER_PLAN.md](docs/product/MASTER_PLAN.md): Studio V5, Design Lab (wallpapers, clocks, icons,
widgets), device capabilities and Intelia Bridge, hardware control, Action Engine / FLOW V2, scenes and presets,
onboarding V2. Nothing on the roadmap is promised until it ships.

## Build

JDK 17+ and Android SDK platform 37.1 (`local.properties` → `sdk.dir`).

```bash
./gradlew :app:testDebugUnitTest :app:assembleLab
```

`:app:assembleLab` builds the optimised Lab (`dev.intelia.launcher.lab`); `scripts/dev.sh` installs the debug build on a
USB phone. `:app:assembleRelease` is signed only when `.signing/release.properties` exists (never committed); see
[DECISIONS D26](docs/DECISIONS.md).

## Reporting bugs

Open an [issue](https://github.com/ondrejmacko-it/Intelia-Launcher/issues) with your phone model, Android version,
Intelia version (Studio › System and data › About and licence) and steps to reproduce. Please do not attach screenshots with
private notifications or messages.

## Contributing

Read [AGENTS.md](AGENTS.md) first. Code is Apache-2.0; no code copied from other launchers.

## Licence and legal

- **Code:** Apache License 2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE). You may use, modify and redistribute it,
  but every copy must keep the licence, the NOTICE and the copyright notices and mark what was changed. Provided
  **without warranty**.
- **Name and logo:** "Intelia", the logo and the LENS / ARC / PULSE / FLOW product names are not licensed — see
  [TRADEMARKS.md](TRADEMARKS.md). Forks must use their own name and application ID.
- **Official builds** come only from this repository's Releases and are signed with the project key; verify them as in
  [SECURITY.md](SECURITY.md).
- **Inspiration and third-party components:** [CREDITS.md](CREDITS.md). Bundled fonts: SIL Open Font License 1.1.

Copyright 2026 The Intelia Launcher Authors.

# Intelia Launcher v0.1.0 Beta 1

> **Prerelease.** First public beta. Tested on one phone. Not on Google Play.
> Signed with the project's release key (RSA 4096, `CN=Intelia Launcher, O=Intelia, C=CZ`), certificate SHA-256
> `5ddb08b20d7f3eb02c0ae26a7d8b6a75fb284fe97616f42260ded24cd6dd99d3`.

## What Intelia is
A fast, quiet, local-first Android home screen written from scratch in Kotlin and Jetpack Compose. No accounts, no
network, no analytics, no background workers; everything optional is off by default. — *Your phone. Your rules.*

## What 0.1.0 Beta 1 can do
- **LENS** — one control: tap = search, swipe up = app drawer, hold = ARC.
- **ARC** — hold-slide-release onto named sets of apps, FLOW commands and app shortcuts; release on LENS cancels.
- **FLOW** — named commands opening 1–4 apps in order; previewed while typing, run only on Run, Enter on the exact
  name or release on an ARC slot.
- **PULSE** — capsule around the camera: charging, near alarm, music (optional notification access), quick actions,
  gestures; over other apps and **Global ARC** only with the optional *Intelia PULSE* accessibility service.
- **Search** — local, diacritics-insensitive ranking.
- **Studio** — five areas with live, non-interactive previews and settings search; three sheet heights.
- **Home Editor** — grid / free / hybrid placement, presets, inspectors, undo/redo.
- **Personalisation** — themes, Intelia Veil glass, preset or custom accent (contrast-checked), Intelia wallpapers,
  seven bundled OFL fonts for clock and date, icon shapes and labels, haptic strength.
- **Controls** — Home/LENS/PULSE gestures, hardware-key diagnostics, key remap while Intelia is in front (or
  everywhere with the PULSE service).
- **Backup** — export / restore one JSON file via the system file picker.
- **Languages** — English and Czech; per-app language on Android 13+.

## Requirements and testing
- Android 8.0 (API 26)+; target SDK 37. **Tested only on Nothing Phone (3), Android 16** (1080 × 2392, 420 dpi).
- 236 JVM unit tests pass. Device smoke test: see `RELEASE_CHECKLIST.md`.

## Install
Download `intelia-0.1.0-beta.1.apk`, compare its SHA-256 with the value below, install, then optionally set
Intelia as home in Studio › System and data › Home screen.

| File | SHA-256 |
|---|---|
| `intelia-0.1.0-beta.1.apk` (3 820 198 B, `dev.intelia.launcher`, versionCode 2) | `3dd122a75f72df6b6fe3ecf595cbbd3281cbb5a675d675a1a230b3243adcdff7` |

## Permissions
`SET_WALLPAPER` and `VIBRATE` (normal). Optional, granted by you in Android: default home role, accessibility service
*Intelia PULSE* (overlay + key events, no screen content read), notification access (media session only).

## Lab vs release
| | `dev.intelia.launcher` | `dev.intelia.launcher.lab` |
|---|---|---|
| Purpose | distribution | development / testing |
| Signing | project release key (owner-held) | development key, never distributed |
| Updates | only from builds signed with the same release key | from any Lab build |

They install side by side, never update each other, and do not share settings (use Backup).

## Known limitations
- Single test device; other OEMs and Android versions unverified.
- No third-party widgets, icon packs or cloud sync yet.
- PULSE outside Home, Global ARC and key remap outside Intelia depend on the accessibility service; OEMs may kill or
  restrict it. Not every hardware key is visible to apps (for example Nothing's Essential Key is not guaranteed).
- Quickstep (recents and back-to-home animations) belongs to the system.
- No performance or battery claims.

## Known bugs
See [`docs/audit/UX_BUGS.md`](../../audit/UX_BUGS.md).

## Beta notice
Settings format may still change; export a backup before updating. Security issues: private report (SECURITY.md). Report other bugs as GitHub issues with phone model,
Android version, Intelia version and steps — no private screenshots.

## Legal
Apache-2.0 code ([LICENSE](../../../LICENSE), [NOTICE](../../../NOTICE)); name and logo not licensed
([TRADEMARKS.md](../../../TRADEMARKS.md)); inspiration and components in [CREDITS.md](../../../CREDITS.md).

## Roadmap
[`docs/product/MASTER_PLAN.md`](../../product/MASTER_PLAN.md). Roadmap items are plans, not promises.

# Release checklist — v0.1.0-beta.1

Verified 2026-10-19 on branch `feat/control-system-v1` (base `e6b0c84` + release commit). PASS / FAIL / BLOCKED / NOT VERIFIED.

## Build
| Check | Result |
|---|---|
| `./gradlew -q --max-workers=1 -Dorg.gradle.jvmargs=-Xmx1536m :app:testDebugUnitTest :app:assembleLab :app:assembleRelease` | PASS, 145 s, no warnings printed |
| JVM tests | PASS — 235 tests, 0 failures, 0 skipped |
| Lab APK `app-lab.apk` | `dev.intelia.launcher.lab`, versionCode 2, versionName `0.1.0-beta.1-lab`, 3 803 826 B, SHA-256 `321aa38b1a152ea39c981ec944149770262a1086fd5fe538dc6f74d6c082d57f`, APK Signature v2, `CN=Intelia Debug` (public debug key — test only) |
| Release APK | `app-release-unsigned.apk`, `dev.intelia.launcher`, 3 795 622 B — **unsigned** (no release key) |

## Signing — BLOCKED
- No production key exists. `.signing/release.properties` is read if present (D26); it and `*.jks` are git-ignored.
- Owner decision needed: create a long-lived key (RSA 4096, validity ≥ 25 years), a strong unique password kept in a
  password manager, and **two offline backups** (key file + password). Losing the key = no updates for installed
  builds; leaking it = anyone can ship "updates".
- Suggested command (run by the owner, interactive password prompt, nothing on the command line):
  `keytool -genkeypair -v -keystore .signing/intelia-release.jks -alias intelia -keyalg RSA -keysize 4096 -validity 10000`
  then `.signing/release.properties` with `storeFile=.signing/intelia-release.jks`, `storePassword`, `keyAlias=intelia`,
  `keyPassword`. Then `:app:assembleRelease`, `apksigner verify --print-certs`, rename to `intelia-0.1.0-beta.1.apk`.

## Licences and secrets
| Check | Result |
|---|---|
| Project licence | PASS — Apache-2.0 `LICENSE` |
| Fonts | PASS — 7 families (Roboto Flex, Inter, Space Grotesk, DM Serif Display, JetBrains Mono, Bodoni Moda, Unbounded), SIL OFL 1.1, texts in `assets/licenses/` (D11, D14) |
| Libraries | AndroidX, Compose, Kotlin, kotlinx — Apache-2.0; no third-party code copied |
| Secrets | PASS — only the intentionally public debug key (`keystore/debug.keystore`, D4). No tokens. `hs_err_*`, `replay_*`, `build.log` and `docs/screens/*.rar` + unreviewed screen folders stay untracked |

## Permissions
PASS — manifest: `SET_WALLPAPER`, `VIBRATE`; optional accessibility service, notification listener, HOME role. No
`INTERNET`. `allowBackup=false` (own export only).

## Device — Nothing Phone (3) `A059`, Android 16
| Check | Result |
|---|---|
| `adb install -r` of Lab over 0.1.0-lab (vc 1 → 2) | PASS 00:08; no uninstall |
| HOME role kept | PASS `dev.intelia.launcher.lab` |
| PULSE service still enabled + bound | PASS (`dumpsys accessibility`) |
| Home after HOME key | PASS (dump: PULSE, clock, dock, LENS) |
| LENS tap → Search + keyboard | PASS (`mInputShown=true`) |
| LENS swipe up → drawer | PASS (221 apps) |
| LENS hold → ARC; release on LENS cancels | PASS (screenshot `docs/screens/v0.1.0-beta.1/01-lens-arc.jpg`) |
| Home hold → editor → Settings → Studio | PASS (`02-studio.jpg`) |
| Studio › Systém a data › Jazyk → system per-app language page (Čeština / English offered) → Back | PASS (`03-system-language.jpg`) |
| Settings / FLOW / bindings / accent unchanged | PASS — nothing was edited |
| Lock/unlock, Recents → Home, PULSE overlay over apps, Global ARC, process restart, TalkBack, light theme, English UI on device | NOT VERIFIED this session |

## Git / GitHub
See `docs/STATUS.md` (2026-10-19) for the PR, merge commit and the draft release.

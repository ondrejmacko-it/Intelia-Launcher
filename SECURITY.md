# Security policy

## Supported versions
Only the latest release on [GitHub Releases](https://github.com/ondrejmacko-it/Intelia-Launcher/releases) is supported.

## Reporting a vulnerability
Please **do not** open a public issue. Use GitHub's private reporting:
*Security → Report a vulnerability* in this repository. Expect an answer within 14 days.

## Verifying a download
Official APKs are signed with the project's release key. Check before installing:

```bash
apksigner verify --print-certs intelia-0.1.0-beta.1.apk
```

The certificate SHA-256 must be
`5ddb08b20d7f3eb02c0ae26a7d8b6a75fb284fe97616f42260ded24cd6dd99d3` and the file SHA-256 must match the release notes.
Builds with the package `dev.intelia.launcher.lab` are development builds and are never published.

## What Intelia does with data
No network permission, no accounts, no analytics. Settings stay in the app's private storage (encrypted at rest by
Android's file-based encryption); Android cloud backup is disabled (`allowBackup=false`); exports happen only through
the system file picker to a file you choose. The optional accessibility service and notification access read no screen
content and no notification text.

## Project secrets
Signing keys and their passwords never enter this repository (`.signing/`, `*.jks`, `*.keystore` are ignored). The
development key that was in the history until 2026-10-19 is considered public and only ever signed development builds.

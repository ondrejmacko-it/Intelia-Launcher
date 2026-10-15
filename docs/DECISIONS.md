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
The test phone already runs the older fork build as `dev.intelia.launcher.debug` (current HOME).
The new debug build uses a different suffix so installing it never replaces that app or its data.

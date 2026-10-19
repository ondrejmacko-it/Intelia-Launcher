# P1 — Onboarding V2

Status: **Implemented 2026-10-19** on `claude/pulse-reliability` (owner: "dodělej to komplet", Kea as the reference
for step clarity). §12 records what was built; §4–§5 are the earlier design and differ where §12 says so.
- Prototype: `docs/design/onboarding-v2-prototype.html` (Artifact "Intelia Onboarding V2", private to the owner).
- Design 1 (8 static artboards, Artifact canvas "Intelia Onboarding V2") is superseded; kept for comparison.
- Replaces `ui/Onboarding.kt` (3 steps). Does not touch PULSE P0 work or existing user settings.
- KEA (Essential Key remapper) screenshots were used only as a reference for permission explanations, device
  detection and guidance; no visuals, colours, copy or layout are taken from it.

## 1. Goals
- In the first minutes the user understands what Intelia is, why it is different, how to use and how to adapt it,
  without being made to configure everything.
- A working, personal Home in about a minute; every step skippable, the whole flow skippable.
- Works with zero permissions. Each permission states benefit, reason, privacy, what keeps working without it,
  and its real current state.
- Feels like part of Intelia Veil: the onboarding happens on the real Home, not on separate black screens.

## 2. Review of design 1
1. Eight near-identical dark screens (title, bordered cards, button); nothing looked like Intelia.
2. The Home preview was a drawn mock; layout and accent choices did not show on the real Home.
3. LENS/ARC "practice" was tapping list rows, not gestures.
4. PULSE explained in text only; capsule, Quick View and Global ARC never shown.
5. Permissions had no state, no re-check after Settings, no "not now".
6. Fixed "step X of 8" although Pure and PULSE Off shorten the flow.
7. Accent hard-coded, no light theme, large text or Reduced Motion.
8. Done was a text list; Studio a sentence; no path to the other features.

## 3. Concept: three environments, one Home
| Environment | Home | Used by |
|---|---|---|
| **Sheet over a live preview** | real `Home` in preview mode, scaled to 43 % with the Home Editor zoom-out | Plocha, Styl, Aplikace, Hotovo |
| **Full Home with a small coach** | real `Home`, sandboxed LENS / PULSE | LENS + ARC, PULSE |
| **Veil Focus page** | blurred Home behind (`Depth`) | Odemkni možnosti, Discovery Hub |
"Přejít na plochu" zooms the preview back to full size: the onboarding ends where Home already is.

## 4. Steps (matches the prototype)
| # | Screen | Shown when | Draft fields | Notes |
|---|---|---|---|---|
| 1 | Vítej — "Tvůj telefon. Tvoje pravidla." · Začít · Obnovit ze zálohy · Přeskočit průvodce | always | — | no name, account or network; restore = `BackupCodec` via SAF → preview of what is restored → applied at once → step 7 (permissions are never restored) |
| 2 | Tvoje plocha — Pure / Essentials (recommended) / Custom | always | `layout` | live on the preview |
| 3 | Tvůj styl — System / Dark / Light, accent (Dynamic + 6) | always | `themeMode`, `accent` | typography, material, per-place colours stay in Studio |
| 4 | Aplikace po ruce — installed apps, diacritics-insensitive search, picked row | layout ≠ Pure | `favorites` | pre-selected from `essentialPackages()`; limit 4 (Essentials) / 8 (Custom) |
| 5 | LENS + ARC — "Jedno gesto. Mnoho možností." | always | `trained` (local only) | real LENS in sandbox mode; tap / swipe up / hold-drag; release on a slot shows what *would* open; "Ukázat" button per gesture as the accessible alternative; optional |
| 6 | PULSE — "Ovládání odkudkoliv." Off / Home only (recommended) / Everywhere | always | `pulse`, `pulseEverywhere` | capsule at the real camera; tap = Quick View, hold-drag = Global ARC, both in demo mode; Everywhere only explains the service |
| 7 | Odemkni možnosti | always (rows vary) | — (system state) | Default Home always; PULSE service only for Everywhere; notification access only if PULSE ≠ Off (music) |
| 8 | Hotovo — preview + summary rows (each opens its step) · Přejít na plochu · Prozkoumat Intelii | always | `onboarded = true` | one `updateSettings { … }.sanitized()` commit |
Progress shows only the steps that apply (Pure: 5 segments, otherwise 6).

## 5. Components
| Component | Role |
|---|---|
| `OnboardingHost` | owns `OnboardingDraft`, picks the environment per step, Back = previous step, system Back on step 1 leaves the app as today |
| `OnboardingDraft` (pure Kotlin, JVM tested) | `step`, choices, `trained`, `later`; `steps()` filters by choices; `next/back/skip/skipAll` |
| `HomeStage` | the real `Home` with `mode = Preview` (no launches, no editor), states `Full / Mini / Blurred`; reuses the Home Editor zoom curve |
| `StepSheet` + `StepBar` | Veil Dense sheet: back, segmented progress, skip; title; scrollable body; one primary action |
| `ChoiceRow`, `Segmented`, `AccentDiscs` | radio groups shared with Studio (no onboarding-only copies) |
| `AppPicker` | `AppSearch` ranking, picked row, limit by layout |
| `GestureCoach` | small Veil card over full Home; LENS `sandbox = true` recognises gestures and reports them, runs nothing |
| `PulseDemo` | capsule / Quick View / Global ARC drawn by the existing PULSE UI with a demo `ActionHost` that only describes |
| `PermissionRow` | icon, title, state chip, benefit, expandable *Proč · Soukromí · Bez toho*, one action, "Teď ne" |
| `DiscoveryHub`, `FeatureSheet` | catalogue rows with real state; detail with what / state / what you need / one action |
Sandboxing rule: every demo goes through a `PreviewActionHost` whose `run()` returns `Unavailable(Preview)` and
emits a description; nothing in onboarding calls the real `AndroidActionHost`.

## 6. Discovery Hub ("Prozkoumat Intelii")
Optional, reachable from Done and from Studio; never part of the first-run path.
| Item | State source | Action |
|---|---|---|
| LENS a ARC | `trained` | Vyzkoušet (full Home, coach) |
| PULSE | `pulse`, service health | Vyzkoušet |
| FLOW a akce | FLOW count | Ukázat FLOW (preview card, Run disabled) |
| Fyzická tlačítka | service health | Najít tlačítko: waits for a real `KeyEvent` through the PULSE service; never inferred from the model |
| Úprava plochy | `layout` | opens the Home Editor |
| Motivy a personalizace | accent, glass | opens Studio › Vzhled |
| Pokročilé ovládání (Shizuku / ADB) | — | **Připravujeme**; voluntary, reversible, never disables system apps automatically |
| Design Lab | — | **Připravujeme** |
States: Nastaveno / Zapnuto (ok), Připraveno (neutral), Potřebuje službu (warning), Připravujeme (outline). Read from the
Capability Registry once it exists (MASTER_PLAN §6); until then from the same checks as step 7.

## 7. UX decisions
- **Saving:** `OnboardingDraft` is persisted after every change (small JSON in `noBackupFilesDir`, or a separate
  DataStore key; not inside `LauncherSettings`), so a permission round-trip or process death resumes at the same step
  with the same choices. Cleared on Done or Skip all.
- **Commit:** active settings change only on Done (one `sanitized()` gate). Exceptions: restore from backup (applied
  at once) and explicit system actions (default Home role, opening Settings).
- **Restore** stays as the secondary action on step 1.
- **Name:** not asked.
- **Permissions:** all optional; state re-read on `ON_RESUME` (`isDefaultHome`, `PulseAccessibilityService.health`,
  `MediaWatch.allowed`), "Ověřuji…" until read; never a stored flag; never auto-granted. Default Home uses the
  `RoleManager` dialog on API 29+.
- **Hardware keys:** no setup in the basic flow; Discovery Hub only, with real `KeyEvent` detection.
- **Shizuku / ADB:** not in the basic flow.
- **Release channel:** not assumed; Play / GitHub requirements belong to the release plan (D29).
- **Studio:** long-press empty Home → Studio; Studio › Systém a data › "Spustit průvodce znovu" starts from current
  settings (draft = current values) and resets nothing; "Prozkoumat Intelii" opens the hub.

## 8. Accessibility and platform
- Android 12–16 test matrix; Kotlin + Compose; `SettingsStore` unchanged, no destructive migration.
- TalkBack: each step a pane title; choices as radio groups; progress as `progressbar`; every gesture has a "Ukázat"
  button; LENS/PULSE demos expose custom actions.
- Reduced Motion: all `motionMs` = 0, no breathing ring, no equaliser.
- Large text: sheets scroll, primary action stays pinned.
- Light and dark from the same `VeilTokens`; accent from the user's choice, never hard-coded.
- Strings EN + CS.

## 9. Acceptance criteria
- Fresh install (new package on a test profile, never the owner's Lab) → Done in ≤ 8 screens, Home matches choices.
- Kill the process at each step → resumes at that step with the same draft.
- Every step skippable; Skip all → working Essentials Home.
- No permission requested without a tap on its row; denying any keeps the launcher fully usable.
- No demo launches an app, runs a FLOW or changes a system setting (test: `PreviewActionHost` is the only host bound).
- Rerun from Studio leaves `LauncherSettings` unchanged until Done.
- `OnboardingDraftTest`: step list per choice, back / skip / skipAll, save/restore round-trip, Pure skips apps.

## 10. Owner decisions (2026-10-19, after design 2)
1. Concept "onboarding on the live Home" — **approved** as the direction (production code after the final revision below).
2. Order Plocha → Styl — **approved**.
3. Accents in step 3 — **all 16 + Dynamic + the custom colour** (D25), not a subset.
4. Notification access row — **always shown while PULSE is on**.
5. Default PULSE mode — **Všude** (Everywhere is what PULSE is for); the step still offers Jen na ploše / Vypnuto.
6. New installs start from the owner's Home as the **signature template** (`HomeTemplates.signature`, built in
   `e788977`): slanted Unbounded clock, spaced date, 56 dp LENS, four apps beside it, no FLOW item; neutral Silver
   accent, the user's system wallpaper, Ink icons. Existing installs unchanged.

## 11. Final revision required before implementation (owner review 2026-10-19)
1. **Finish:** both "Přejít na plochu" and "Prozkoumat Intelii" commit the validated draft (one atomic
   `SettingsStore.update { … }.sanitized()`, `onboarded = true`) and clear the draft. The hub opens *after* the commit.
2. **PULSE step:** a title that fits every mode (not "Ovládání odkudkoliv"); clear difference between Všude / Jen na
   ploše / Vypnuto; show the compact capsule as one of Intelia's strongest parts; no decoration.
3. **Custom Home:** preview and copy match what exists today (grid, free placement in the Home Editor, no widgets —
   `ItemKind.Widget` renders nothing yet). Ideally "Upravit" opens the real Home Editor in a safe preview mode.
4. **Favourites:** fix layout switching and limits. Essentials = the four slots around LENS; Custom is not limited to
   eight items for the whole Home (picker limit = `MAX_FAVORITES`, more via the Home Editor).
5. **Permissions:** KEA-level clarity without a crowded form: per capability benefit, real state, requirement,
   reason, skip; details on demand; nothing requested early.
6. **Discovery Hub:** a permanent part of Studio (Studio › Prozkoumat), long after the first run; LENS, ARC, PULSE,
   FLOW, hardware keys, Home Editor, Labs; states Hotovo / Nastavit / Vyzkoušet / Připravujeme; unavailable features
   never look functional.
7. **Visual:** keep Veil; tune type, preview size, Home motion, sheet proportions, legibility on light / busy wallpapers.
8. **Accessibility:** large text, TalkBack, Reduced Motion, small display, soft keyboard; every gesture has an alternative.
9. **Saving:** the draft survives real process death (own file in `noBackupFilesDir`), settings applied atomically on
   finish, migrations and safe backup restore kept.
10. **PULSE P0 first:** do not touch `PulseAccessibilityService` or its lifecycle for onboarding UX.

## 12. As built (2026-10-19)
- **Files:** `onboarding/OnboardingDraft.kt` (draft, `DraftCodec`), `ui/OnboardingHost.kt` (stage + panel + steps),
  `HomeViewModel` (`guide`, `beginGuide`, `updateGuide`, `finishGuide`, `skipGuide`, `explore`), `HomeRoot` (draft
  present → guide, else Home). The old `ui/Onboarding.kt` and its strings are gone.
- **Steps (6):** Vítej (Přizpůsobit / Použít výchozí nastavení) · Plocha (Prázdná / Základní / Vlastní; LENS apps
  with search; "Upravit rozložení" opens the real Home Editor whose ✓ writes into the draft) · Vzhled (Motiv, Barva =
  Studio's `PaletteEditor` with every accent + Dynamic + custom, Hodiny = clock face, Ikony, Materiál) · Ovládání
  (LENS / ARC / PULSE / FLOW tabs on the full Home) · Odemkni víc (default home, PULSE service when Všude, music when
  PULSE is on; state read on resume) · Hotovo (summary rows back to their step; Přejít na plochu / Prozkoumat Intelii,
  both commit first).
- **Stage:** the production `Home` composable fed with the draft (or the start for "Výchozí"), scaled into the room
  above the panel on the look steps (touches blocked) and full and interactive on Controls or with the panel hidden.
  Its callbacks are the sandbox: Search and the drawer open as the real layers, every launch, FLOW run and ARC /
  PULSE action only shows "Ukázka · otevřelo by se …". An empty ARC and a user without FLOWs get a demo set / the
  demo FLOW "Cesta domů", both marked "Ukázka" and never stored.
- **Draft:** whole `LauncherSettings` copies (`start`, `settings`), sanitized on every change, saved as JSON in
  `noBackupFilesDir/onboarding-draft.json` (temp file + rename); commit = `settings.copy(onboarded = true)` once.
  A rerun writes Empty/Basic to its own layout `guide`, so the user's own layout `mine` is never replaced.
- **Deviations from §4/§5:** no separate Aplikace step (apps live in Plocha), no restore-from-backup on Vítej (Studio
  keeps it), no Discovery Hub yet ("Prozkoumat Intelii" opens Studio), no "Teď ne" per permission row (Pokračovat /
  Přeskočit skip the step), PULSE states shown with Studio's `PulseStage` in the panel.

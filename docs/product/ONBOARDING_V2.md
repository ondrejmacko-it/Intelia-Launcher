# P1 — Onboarding V2 (design, awaiting owner approval)

Status: **Planned, design proposed 2026-10-19.** No code until the owner approves the preview
(Artifact "Intelia Onboarding V2", 8 phone screens, private to the owner). Replaces `ui/Onboarding.kt` (3 steps).

## 1. Goals
- A new user reaches a working, personal Home in about a minute; nothing is mandatory.
- Every step skippable; Back works; resumes at the same step after process death or a permission round-trip.
- Works with zero permissions. Each permission explains *what it unlocks, what keeps working without it, privacy*.
- Live preview: choices apply to a preview at once, are committed only on Done (or per step on Continue — see §5 Q2).
- Existing users (`onboarded = true`) never see it again unless they start it from Studio.

## 2. Steps (matches the preview)
| # | Screen | Writes | Notes |
|---|---|---|---|
| 1 | Vítej — "Tvůj telefon. Tvoje pravidla." · Začít · Obnovit ze zálohy | — | Restore = existing `BackupCodec` import via SAF → sanitised → skip to Done |
| 2 | Vzhled — mode System/Dark/Light, accent (from wallpaper on Android 12+, 4 presets) | `themeMode`, accent | live mini-Home preview |
| 3 | Plocha — Pure / Essentials (recommended) / Custom | `layout` | same `HomeLayout` enum as today |
| 4 | Oblíbené — up to 6 apps, pre-selected from `essentialPackages()`, search | `favorites` | only on Essentials/Custom; Pure skips |
| 5 | LENS a ARC — practice tap / swipe up / hold-slide on a real LENS | — | each gesture ticks when done; "Continue without trying" allowed |
| 6 | PULSE — Off / Home (recommended) / Everywhere | `pulse`, `pulseEverywhere` | Everywhere shows the accessibility note; service enabled in step 7 |
| 7 | Oprávnění — default Home (recommended), PULSE service (only if Everywhere chosen), notification access (only if music wanted) | — (system dialogs) | state re-read on resume; never auto-enables anything |
| 8 | Hotovo — summary, "Studio = long-press empty Home" | `onboarded = true` | |

## 3. Technical design
- `OnboardingState` (pure Kotlin, JVM tested): `step`, draft choices, `next()/back()/skip()` and which steps apply
  (e.g. Pure → no favourites step; PULSE Off → no PULSE row in permissions). Saved through `rememberSaveable`
  (`Saver` over a small data class) so a permission round-trip or process death resumes.
- Draft lives in memory; commit is one `updateSettings { … }.sanitized()` at Done (single validation gate).
- Permission rows read real state on `ON_RESUME` (`isDefaultHome`, `PulseAccessibilityService.health`,
  `MediaWatch.allowed`) — never a stored flag (P0 lesson).
- Gesture practice reuses the real LENS component in a sandbox mode: recognises gestures, runs nothing.
- Studio › Systém a data › "Spustit průvodce znovu" sets a transient flag; it does not reset settings.
- Strings EN + CS; TalkBack: each step a pane title, choices as radio groups, practice gestures with custom actions.
- Reduced Motion: cross-fades only.

## 4. Acceptance criteria
- Fresh install (new package on a test profile, never the owner's Lab) → Done in ≤ 8 screens, Home matches choices.
- Kill the process at each step → resumes at that step with the same draft.
- Every step skippable; Skip all → a working Pure-or-Essentials Home.
- No permission is requested without a tap on its row; denying any keeps the launcher fully usable.
- `OnboardingStateTest` covers step order per choice, back/skip, saver round-trip.

## 5. Open questions for the owner
1. Approve the visual direction (Veil dark, one cool accent, Manrope-like type in the mockup; the app keeps Roboto Flex)?
2. Commit choices only at Done (safer, current proposal) or per step?
3. Step 1 "Obnovit ze zálohy" — keep it on the welcome screen?
4. Ask for the user's name (personal greeting on Home)? Not in the proposal — minimalism.

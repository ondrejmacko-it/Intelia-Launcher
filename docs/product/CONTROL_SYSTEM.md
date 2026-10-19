# Intelia Control System — one Action Engine, many inputs

> Living spec for stage **P2 Control System Hardening** (see `MASTER_PLAN.md` §14). Extend, never rewrite:
> `action/Action.kt`, `action/Controls.kt` (`Trigger` → `Action` bindings), `ui/AndroidActionHost.kt`.

## 1. What exists (Built, 0.1.0-beta.1)
- `Action` (sealed): app, FLOW (by stable id), app shortcut, torch, screenshot, system panels, card, lock, ARC set,
  search … with `requirement` (`None · DefaultHome · PulseService`), `outsideHome`, `run(host) → ActionResult`.
- `ActionResult`: `Done · Opened · Unavailable(Reason)`; `Reason`: `Invalid, AppMissing, FlowMissing, ShortcutMissing,
  NotDefaultHome, CardMissing, NeedsPulseService, Unsupported, Failed`. A refused action is never reported as done.
- `Trigger`: LENS gestures, PULSE gestures (`Trigger.Pulse`), keys (`Trigger.Key(code, gesture)`), ARC slots; all bound
  in one `Controls` document inside `LauncherSettings` (`sanitized()` is the only gate).
- Inputs that already share it: LENS, Home ARC, Global ARC, PULSE gestures + Quick Actions, key remap (D20a), FLOW.

## 2. Rules
1. **One definition, many bindings.** A FLOW `vlak` exists once; Search, Home, ARC, Global ARC, PULSE quick action,
   PULSE gesture, key, widget or scene button only store its id.
2. **Inputs never execute on their own.** Each input produces a `Trigger`; `Controls.bound(trigger)` resolves an
   `Action`; only `Action.run(host)` executes.
3. **Honest results.** Every UI shows `Unavailable(reason)` with a concrete message; no fake confirmation.
4. **Preview never runs.** Studio previews and the Action Picker only resolve/describe.
5. **Nothing runs while typing** (`PulseAccessibilityService.typing`, search field).

## 3. Planned extensions (P2, Planned)
| Item | Change | Notes |
|---|---|---|
| Action metadata | `ActionInfo(id, label, icon, category, params, requirement, capability)` derived, not stored | keeps stored JSON unchanged |
| Capability link | `requirement` → `Capability` from the Capability Registry (`MASTER_PLAN` §6) | providers: Android API · permission · Accessibility · Shizuku · Bridge · root · unsupported · unverified |
| Availability | `Action.availability(host, caps)` → `Available · NeedsPermission(x) · Unsupported(reason) · Unverified` | Action Picker greys + explains |
| Fallback | optional `fallback: Action?` per binding (e.g. screenshot → system panel) | only safe, non-privileged fallbacks |
| Action Picker | one picker for every binding surface | shows capability state |
| Tests | contract test per `Action` kind: missing app, not default home, no service, bad ids | JVM |

## 4. Out of scope until approved
Privileged providers (Shizuku, Bridge, root), new permissions, autonomous FLOW triggers — each needs a `DECISIONS.md`
entry approved by the owner first.

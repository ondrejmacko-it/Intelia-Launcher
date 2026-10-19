# P0 — PULSE Reliability

> **PULSE first. Reliability before features.** Until this document's DoD (§7) is met, no PULSE redesign or new PULSE
> feature has priority. Any PULSE regression sends work back here. Living document — update with verified facts only.

Status: **fix built and partly verified (2026-10-19)**; long-term stability **not yet verified** (needs days of real use).

## 1. Reported bug (owner, Nothing Phone (3), Android 16, Nothing OS)
1. PULSE works after the accessibility service is switched on.
2. Lock the phone.
3. After unlocking, PULSE ignores touches at the camera.
4. The system accessibility switch stays on.
5. Mode Everywhere → Home → Everywhere does not help.
6. Switching the service off and on fixes it until the next lock.

## 2. Root cause (confirmed by device evidence, 2026-10-19)
`PulseAccessibilityService` kept a `locked` flag set by a screen receiver:
`SCREEN_OFF → true`, `USER_PRESENT → false`, `SCREEN_ON → KeyguardManager.isKeyguardLocked`.
The receiver was registered with `ContextCompat.RECEIVER_NOT_EXPORTED`.

- `SCREEN_ON` / `SCREEN_OFF` are sent by `system_server` (uid 1000) → delivered.
- `USER_PRESENT` is sent by **SystemUI** (`com.android.systemui`, uid 10217) → a not-exported receiver only accepts its
  own uid and system, so Android **filtered it out**.

Evidence (`adb shell dumpsys activity broadcasts history`, old build, 18:13–18:14):
- Record `USER_PRESENT/u0`, `caller=com.android.systemui … uid=10217`, 26 terminal receivers (NFC, GMS, Nothing
  launcher, Instagram, …) — **no `dev.intelia.launcher.lab` receiver**, although `dumpsys activity broadcasts`
  showed our `ReceiverList{ba8141b 329 dev.intelia.launcher.lab … class:kp1}` filtering exactly
  `SCREEN_OFF, SCREEN_ON, USER_PRESENT`.
- The same receiver *was* delivered `SCREEN_ON` (18:14:31) and `SCREEN_OFF` (18:13:20).
- Live state on the old build after unlock: service bound (`dumpsys accessibility`), zero `Intelia PULSE` windows.

Sequence: screen off → `locked = true` → screen on at the keyguard → `isKeyguardLocked = true` → unlock →
`USER_PRESENT` never arrives → `locked` stays `true` forever → `updateLens()` removes the overlay on every later
settings change (why toggling the mode did not help) → only `onServiceConnected` re-read the keyguard (why toggling the
service did). It worked occasionally when the keyguard was already gone at `SCREEN_ON` (fast face/fingerprint unlock).

Classification (§2.2 of the brief): **B — the service runs, the overlay does not exist**, caused by a lost lifecycle
event. Not A (service stayed bound), not C/D/E (no window to touch), F partly (Studio showed "on" correctly for the
service, but had no notion of the overlay state).

## 3. Fix (branch `claude/pulse-reliability`)
1. **Receiver exported** for the three actions. All three are protected broadcasts (only the platform can send them),
   so `RECEIVER_EXPORTED` adds no attack surface.
2. **No stored lock flag.** `locked` is now a getter: `!PowerManager.isInteractive || KeyguardManager.isKeyguardLocked`,
   read at every evaluation. Broadcasts only *trigger* a re-evaluation.
3. **Pure decision model** `pulse/PulseLifecycle.kt`: `PulseFacts → wantsLens / PulseHealth`
   (`Disconnected · Off · Locked · Attaching · Active · Error`), JVM tests in `PulseLifecycleTest`.
4. **Safety net without polling:** after `SCREEN_ON` while the keyguard is up, the keyguard is re-read at
   0.4 / 0.8 / 1.5 / 3 / 6 / 12 / 30 s (bounded, ≈ 54 s total, cancelled on unlock / screen off); Home's
   `MainActivity.onStart` also calls `PulseAccessibilityService.resync()`.
5. **Safe attach:** `addView` / `updateViewLayout` failures are caught, recorded, retried 3× (0.25 / 1 / 4 s), then
   `PulseHealth.Error`; Studio › PULSE shows a concrete explanation (`pulse_overlay_error`) instead of a false "on".
6. **Teardown guard:** late posts after `onUnbind`/`onDestroy` are ignored (`scope == null`), timers removed; only the
   current instance publishes `Disconnected`.
7. **Privacy-safe diagnostics:** `Service.dump()` →
   `adb shell dumpsys activity service dev.intelia.launcher.lab/dev.intelia.launcher.ui.PulseAccessibilityService`
   prints health, facts, lens window geometry/flags, attach failures and counters (connects, attaches, touch-down
   count, unlock checks, last screen event). No screen content, text, notifications, keys or touch positions.

## 4. State model
| State | Meaning | Lens window | Studio |
|---|---|---|---|
| Disconnected | no service instance | none | "needs the PULSE service" |
| Off | mode ≠ Everywhere | none | mode choice |
| Locked | screen off or keyguard up | none (never above the lock screen) | — |
| Attaching | wanted, being added | adding | — |
| Active | attached and touchable | yes | — |
| Error | WindowManager refused, retries bounded | none | `pulse_overlay_error` |
Permission "enabled but not bound" is visible as Disconnected (Android has not connected the service).

## 5. Test matrix (device = Nothing Phone (3), Lab build `assembleLab`)
| # | Case | Result |
|---|---|---|
| 1 | Install fix over the old build (`install -r`), service rebinds, lens attached | **PASS** 2026-10-19 18:23 (`health=Active`, `attaches=1`) |
| 2 | `KEYCODE_SLEEP` → health `Locked`, lens removed | **PASS** |
| 3 | `KEYCODE_WAKEUP` at keyguard → stays `Locked`, unlock checks run (bounded) | **PASS** (`unlockChecks` 2 → 4, then stops) |
| 4 | Owner unlocks (PIN / fingerprint) → lens back without toggling anything | **PASS** 18:27:11 (`USER_PRESENT` delivered) |
| 5 | ≥ 20 lock/unlock cycles + tap on the camera after each | **PASS** 18:42–18:43: owner did 20 cycles; one service instance (`connects=1`), `attaches` 1 → 17, `touches` 21, `retries=0`, no error; every unlock ended `Active` (incl. lock-delay cycles with `keyguardLocked=false` and keyguard cycles via `USER_PRESENT`); a mode Off → Everywhere switch in between also re-attached |
| 6 | Screen off several minutes | open |
| 7 | Home → app → Home, Recents | open |
| 8 | Music playing / not, music capsule on/off | open |
| 9 | Rotation, calibration change, lens position change | open |
| 10 | APK update (`install -r`), process restart (`am kill` not possible for bound a11y; use force-stop only with consent) | 1 done |
| 11 | Phone reboot | only with owner consent |
| 12 | Global ARC over another app, hardware key remap, TalkBack, MT Capsule overlay conflict | open |
| 13 | Latency on the Lab build (`adb input tap` at the camera, Home in front, in-app clock from the input event time) | touch→gesture **median 304 ms, P95 306, max 306 (n=10)** — the 300 ms double-tap wait, because the owner bound a double tap; touch→menu first frame ≈ 363 ms median (n=20 draws, first build counted two draws per menu; fixed to one) → menu render ≈ 60 ms after the gesture. Excludes touch-panel/kernel latency. |
Automated: `PulseLifecycleTest` (5 tests), existing `PulseGesturesTest`, `PulseSwipeTest`, `PulseMenuTest`.

## 6. Gesture recogniser follow-ups (P0.2, after the lifecycle is proven)
**Measured issue:** with a double tap bound, every single tap waits the full system double-tap timeout (300 ms) before
anything happens. Options for owner approval: (a) configurable PULSE double-tap window (e.g. 180–300 ms), (b) instant
haptic/visual acknowledgement on touch-down while the recogniser waits, (c) both. No change made yet.
Existing `PulseGestures` already: taps at once when no double is bound, waits only while a double can follow.
Still to add as JVM tests: very fast tap, tap right after `Locked → Active`, multi-touch interruption, system
`ACTION_CANCEL`, finger leaving the hitbox, rotation/calibration change mid-gesture.

## 7. Definition of done (P0)
Technically fixed when all hold: cause proven (§2 ✔) · fix covers it (§3 ✔) · unit tests PASS (✔) · ≥ 20 real
lock/unlock cycles PASS · PULSE works over supported apps · Studio state = reality · no manual a11y toggling after a
normal lock · no lost bindings · no key-remap regression · diagnostics for later incidents (✔ `dump()`).
**Long-term stability** is a separate mark, only after several days of the owner's normal use without a manual reset.

## 8. If it happens again
1. Do **not** toggle anything first. Run the `dumpsys activity service …PulseAccessibilityService` command above.
2. `adb shell dumpsys window windows | grep "Intelia PULSE"` and `adb shell dumpsys accessibility | grep -A2 "Bound services"`.
3. `adb shell dumpsys activity broadcasts history` → check `USER_PRESENT` / `SCREEN_ON` reached `dev.intelia.launcher.lab`.
4. Record the output in `STATUS.md`, then recover.

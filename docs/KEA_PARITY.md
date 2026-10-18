# KEA review → Intelia feature map

Source: 30 owner screenshots of KEA (button remapper, Nothing Phone (3a)), 2026-10-18, kept outside the repo.
KEA is **organisational inspiration only**: no code, assets, icons, wording, colours (amber accent) or layouts are
copied. Every item below is re-designed in Intelia's own Veil language and must pass `AGENTS.md` rules (no network,
accounts, telemetry, new permissions or accessibility use without a `DECISIONS.md` entry).

Legend: **Have** = already in Intelia · **M#** = milestone of the approved plan · **Decide** = needs an owner decision
(permission / service) · **No** = out of scope, with the reason.

## 1. What KEA does well (structure to learn from)
- Three top-level areas in a floating bottom switcher: *Bindings* (what each press does), *Customise* (behaviour,
  feedback, look), *Pro* (catalogue). → Intelia: Studio root stays one list, but groups the same way:
  **Ovládání** (bindings), **Vzhled / Pohyb** (feedback, look), **Systém a data** (backup, setup). No paywall tab.
- Every row = icon, title, one-line *current value* subtitle, chevron. Intelia already does this (S4); keep it strict.
- Sections are small spaced caps captions above one rounded group. Same as our `VeilCaption` + group — keep.
- Counters in captions: „ESSENTIAL KEY · 4 of 4 bound“. → Intelia: „GESTA · 6 přiřazeno“ style (we have „Události ·
  3 ze 3 zapnuto“) — apply to every binding group (M2).
- Action picker is one long categorised list with a one-line explanation and the requirement inline („Needs
  permission“, „needs Shizuku“), unavailable items dimmed, the current one ticked. → Intelia Action Picker (have)
  gets the same categories and the tick (M6).
- Onboarding = one question per screen, a progress bar, a live status chip („running“, „can draw overlay“), a single
  primary button, every step skippable when optional. → Onboarding V2 (M7).
- Theme picker shows real mini-previews (System / Light / Dark / Translucent light / dark) instead of words. →
  Intelia Vzhled › Motiv gets preview tiles (M2).

## 2. Feature map

### Bindings (hardware keys and gestures)
| KEA | Intelia | Status |
|---|---|---|
| Single / double / triple / long press on one key | PULSE capsule: tap, double tap, hold, tap+hold (have); hardware key gestures | Have (PULSE) · M5 (keys) |
| Volume up/down remapping, 4 gestures per key | Volume keys as Intelia triggers, only while a chosen context is active | M5 · Decide (accessibility key filter) |
| Power long-press via default Assistant role | „Intelia as assistant“: long-press power opens LENS Search / ARC | M5 · Decide (assistant role) |
| Teach a key (learns any key's code) | Key learning step in M5 setup, stored as a key code in settings | M5 |
| Gesture timing (hold 0.4 s, window 0.2 s) | Studio › Ovládání › Gesta › Časování (hold, double-tap window) | M5 (PULSE already has fixed timings → make them settings) |
| Pocket mode (ignore presses face down / in pocket) | Proximity + face-down check before a key action | M5 (sensor read only while a key is pressed) |
| Lock screen policy (which gestures work locked) | Per binding „funguje i zamčeno“ switch, default off | M5 |
| Stop the stock handler (Shizuku / ADB disables Essential Space) | Optional Shizuku step that only *shows* `pm disable-user` for the owner to run, or runs it with Shizuku when granted | M4 · Decide |
| Remapping on/off master switch | One switch at the top of Ovládání › Tlačítka | M5 |

### Actions (KEA „Choose action“ list → Intelia Action Library, M6)
Grouped exactly once, each with a one-line explanation and its requirement shown only while unmet (S4 rule).
- **Spuštění:** open an app (have), app shortcut (have, while default home), open a link / URL scheme (new: validated
  `https`/custom scheme, label optional, never fetched), run a FLOW (have).
- **Rychlé akce:** torch (have), rotation lock, auto-brightness (both need `WRITE_SETTINGS` → Decide), ring/vibrate,
  Do Not Disturb (notification-policy access → Decide), voice assistant, lock screen, wake screen, volume panel,
  power menu (PULSE service global actions — have the service), set a timer / alarms (have „Budíky“), calculator.
- **Text:** copy, paste (accessibility focus actions → only with the PULSE service; Decide).
- **Snímání:** open camera (have via intent), screenshot (service global action), screen recording (MediaProjection,
  asks every time → Decide), camera shutter (Shizuku only → M4).
- **Média:** play/pause, next, previous, mute (media key events / notification listener we already use for „Co hraje“).
- **Navigace:** Home, Back, Recents, previous app, notifications, quick settings (service global actions).
- **Kombinace (KEA „Action Lab“):** our **FLOW** already chains 1–4 apps; M6 extends FLOW steps with library actions
  and an optional delay (100 ms – 5 s), still always previewed, never auto-run.
- **Nothing extras:** Glyph torch / timer → research the public Glyph Developer Kit licence first (Decide); End call
  (`ANSWER_PHONE_CALLS` → Decide); QR scanner (open the system/camera scanner intent — no permission); Circle to
  Search (no public API → No unless a supported intent exists); voice / call recording (No — depends on another
  vendor's private widget and on legal regions).

### Feedback and look
| KEA | Intelia | Status |
|---|---|---|
| Visual overlay confirming a press, 5 themes, precise position | PULSE already confirms on the capsule; add a small „echo“ chip next to the pressed key | Have (PULSE echo) · M5 |
| Overlay animations / colour wheel | Veil motion presets + accent colours (have); free colour picker | Have · M2 |
| Haptic intensity 0–100 % | Animace a haptika › Síla (have: Jemná/…) → add a slider | M2 |
| App appearance: System / Light / Dark / Translucent light / dark + accents + match wallpaper | Vzhled › Motiv + Barvy + „Jedna barva všude“ (have) → preview tiles | M2 |
| Floating app sidebar (edge dock of apps and actions) | Edge ARC: ARC ring opened from a screen edge (Global ARC already exists in PULSE) | M6 |
| Quick Settings tile | „Intelia ARC“ tile opening Global ARC / Search | M6 (TileService, no permission) |

### System
| KEA | Intelia | Status |
|---|---|---|
| Backup & restore (export/import config) | Systém a data › Záloha: export/import of the one settings JSON via SAF, through `LauncherSettings.sanitized()` | M8 (have the single document) |
| Key setup / device detection („Nothing Phone (3a) fully supported“) | **DeviceCapabilities**: model, keys, Glyph, camera cutout, role states — one screen that says what works here | M3 |
| Keep running (battery optimisation) | Onboarding step that opens the battery settings, with a live status chip | M7 |
| Draw over other apps | Only if the echo chip needs it outside Home → Decide (PULSE service overlay may suffice) | M5 |
| Free / Pro tiers, purchase, restore | **No** — no accounts, payments or network | No |

## 3. Order of work (fits the approved milestones)
M0 sheet stability (done 2026-10-18) → M1 PreviewHost → M2 Studio UX (counters in captions, theme tiles, haptic slider,
colour picker) → M3 DeviceCapabilities → M4 Shizuku (optional) → M5 hardware keys (+ timing, pocket, lock policy,
echo chip) → M6 Action Library + FLOW steps + Edge ARC + QS tile → M7 Onboarding V2 (one question per screen, status
chips, progress) → M8 backup/restore and integration.

Each **Decide** above needs a `DECISIONS.md` entry before code: what permission or service, why, what it never does.

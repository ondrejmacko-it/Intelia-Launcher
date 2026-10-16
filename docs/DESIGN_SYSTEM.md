# Design system — Crystal Matte Minimalism (phase A proposal, 2026-10-16)

**Status: proposal, waiting for the owner's approval. Android UI is unchanged.**
Source of truth for the requirements: `docs/REDESIGN_BRIEF.md`.
Interactive prototype (private, open signed in on claude.ai): https://claude.ai/artifact/BPhBEVCpmzktdFPXDGDEta
Prototype source: `docs/design/crystal-prototype.html` (1 px = 1 dp on a 360 × 797 dp phone).

---

## 1. Audit completion (phone screenshots 2026-10-16, Lab build `3d15d73`)

Home, ARC, PULSE menu and Studio are described in the brief. Added today:

- **Search**: opaque dark sheet; header row "← VÝSLEDKY · 4 ⊸ 💡"; results grow upwards above the field
  (good, keep); serif result labels. The header (count, two icons) and the empty upper half carry no
  information while typing.
- **FLOW exact match**: large italic serif name, chain "Kalkulačka —•— Hodiny", terracotta *Spustit*
  pill, and a permanent hint "ENTER SPUSTÍ" inside the field. Works and is explicit, but it is the
  loudest element in the whole app and the hint is a permanent text instruction (brief: not wanted).
- **Drawer**: big serif title "Aplikace", count 223, a "MOJE" row of four favourites in white circles,
  alphabet index on the right, section letters on the left, search field at the bottom. Readable, but
  three typographic levels (serif title, mono labels, sans captions) plus terracotta labels.
- **PULSE over another app** (Calculator, `PulseAccessibilityService` running): at rest nothing is drawn
  except a hairline ring around the camera when tapped. The tap opens the same half-screen menu as on Home
  (alarm, battery, media, volume slider, 5 buttons, 5 links, *Sbalit*): a quick-settings clone. Only one
  overlay window exists (`Intelia PULSE`, 94 × 94 px at 493,21, `ACCESSIBILITY_OVERLAY`).

## 2. MT Capsule (Material Capsule: Island-Style, `com.pryshedko.mtisland` 17.2)

Inspected read-only on the phone (main screen, *Gestures & Cards*, *Capsule settings*); nothing changed.
Public info: Google Play listing (Pryshedko Apps s.r.o., 1M+ installs, 4.6★, updated 2026-10-16).

What it is: an accessibility-service overlay around the camera with "mini capsule events" (charging,
Bluetooth, Wi-Fi, vibration), "dynamic cards" (media, volume, downloads, flashlight), notifications with
reply, gestures (tap, long press, swipe left/right, double tap), contacts, barcodes, brightness slider,
50+ expand animations, Material You colours, per-orientation size/position profiles.

On this phone: `GlobalActionBarService` and `NotificationService` run, but the portrait screen profile is
**off**, so it draws no window; Intelia's overlay is the only one on the lens. Granted: notification
listener, notification policy, Bluetooth, `QUERY_ALL_PACKAGES`, **INTERNET, AD_ID and ad attribution**.
Play data-safety: may share app info and device IDs with third parties; collects contacts; data cannot be
deleted.

| | MT Capsule | PULSE today (D8/D9) | PULSE 2.0 (proposal) |
|---|---|---|---|
| Idle | mini capsule events, optional | 28 dp disc + battery ring on Home; nothing elsewhere | nothing; invisible 68 × 48 dp touch area |
| Live | many event types, 50+ animations | charging/alarm on Home only | music, timer, FLOW event, opt-in charging and picked notifications; auto-return after 6 s |
| Tap | dynamic card or configured action | half-screen quick-settings menu | context block (media) or own quick set |
| Hold | configurable action | — | **Global ARC** (same Action Engine as LENS) |
| Actions | own list per gesture | fixed menu | one Action Engine: app, FLOW, Search, shortcut, media, torch, screenshot, sound mode, system panel, card |
| Data | notifications, contacts, internet, ads | none beyond battery/alarm; no notification access | notification access only if the owner turns on picked notifications / media titles |

MT Capsule weaknesses we avoid: ads and ad-ID with an accessibility service; internet + contacts; a very
large settings surface (three tabs, guides, a mascot); expressive animation as the product rather than the
content; its capsule is a separate app with no link to the launcher's actions (no FLOW, no ARC). We copy
nothing: no code, graphics, animations or layout.

## 3. Visual directions (A2)

Each direction is switchable in the prototype (*Vizuální směr*).

**A · Quiet Crystal (recommended).** Nothing at rest except type and LENS. Transient surfaces are one
matte material: near-black at 90 % with a single 1 dp light top edge (the "crystal" edge) and no blur.
One family, Roboto Flex, with precision coming from width and weight axes (thin wide clock, normal UI).
Accent: cold frost blue, used only for the active state. ARC = one hairline arc fading at both ends,
icons without pods. Motion: short, from the finger or the camera, one slight overshoot on selection.

**B · Matte Ambient.** Surfaces are warm, wallpaper-tinted slabs (94 % opaque, 30 dp radius), the ARC
sits on a visible matte band, accent derived from the wallpaper (sage on this wallpaper). Softer and more
"object-like", but every state shows a surface, which works against "invisible until needed", and the
band hides the wallpaper behind the thumb.

**C · Minimal System.** Plain Material 3: solid tonal surfaces, M3 radii, dynamic-colour primary, ARC
slots on tonal pods. Most native and cheapest, least distinctive; pods are exactly the "podložky ikon"
the brief removes.

**Why A:** it is the only one where idle Home and idle PULSE are literally empty, it needs no blur and no
per-element backing (cheap to render, no permanent effects), it reads on dark and light wallpapers with
one material, and its identity comes from geometry and type rather than colour, so terracotta stops being
mandatory and the accent can be the owner's choice. B's material and C's dynamic colour stay as options
inside A (Studio → Vzhled → Materiál: Crystal / Matte; Akcent: Mráz / Stříbrná / Z tapety / vlastní).

## 4. Specification (A5) — direction A

### 4.1 Colour tokens (dark default; light mirrors)
| token | dark | light | use |
|---|---|---|---|
| `fg` | #F1F3F5 | #15181C | primary text on wallpaper/material |
| `fg2` | fg @ 62 % | fg @ 62 % | secondary text, summaries |
| `fg3` | fg @ 38 % | fg @ 38 % | hints, empty slots, chevrons |
| `mat` | #111317 @ 90 % | #F6F7F8 @ 92 % | the only surface (PULSE card, toasts, sheets) |
| `matEdge` | white @ 11 % | white @ 70 % | 1 dp top edge of `mat` |
| `line` | white @ 8 % | black @ 8 % | dividers, outlines |
| `acc` | #A9C4E0 (Mráz) | #3F6E9C | selection, toggles on, primary button |
| `accInk` | #0D1620 | #FFFFFF | content on `acc` |
| capsule | #000000 | #000000 | PULSE live capsule (must match the cutout) |
Contrast: `fg` on `mat` ≥ 15:1, `fg2` on `mat` ≥ 7:1, `accInk` on `acc` ≥ 9:1. Text on wallpaper gets a
0–1 dp shadow only; Home must also pass on a light wallpaper (clock switches to `fg` light when the
wallpaper's top third luminance > 0.6, via `WallpaperColors.HINT_SUPPORTS_DARK_TEXT`).

### 4.2 Typography (one family: Roboto Flex, OFL, bundled as a variable font)
| role | size / line | weight · width |
|---|---|---|
| clock | 84 sp / 1.0, tabular | 220 · 118 |
| date | 12 sp, caps, +2.2 tracking | 450 · 100 |
| title (Studio) | 28 sp | 500 · 112 |
| ARC name | 17 sp | 550 · 110 |
| body | 15–16 sp | 400–500 · 100 |
| caption | 11–13 sp | 400–600 · 100, caps only for section labels |
Roboto Mono is dropped from the UI (kept only for debug). Serif Newsreader/Instrument Sans retire unless
the owner keeps "Ink" as a clock style option.

### 4.3 Sizes, spacing, radii
- Grid 4 dp; screen gutter 16 dp (20 dp in Studio lists); touch targets ≥ 48 dp.
- Icons: 40 dp Home/ARC, 36 dp search rows, 28 dp previews. No backing; system or icon-pack icons as is,
  monochrome option tinted `fg`.
- LENS: 44 dp ring, 1.5 dp stroke, centre 731 dp from top on this phone (= bottom inset + 52 dp); touch
  target 100 dp. No underline.
- Essentials: 4 icons in two pairs at x = 72/118 and 242/288 dp (centre gap 124 dp), same baseline as LENS.
- Radii: sheet/card 22 dp, control 12 dp, capsule = height/2, field 24 dp.

### 4.4 Components
- **Material `mat`**: fill + 1 dp top edge + shadow 0/18/40 @ 35 % for floating cards only. No blur.
- **Arc**: 1 dp stroke `white @ 22 %`, masked with a linear fade at both ends; radius 118 dp (LENS),
  104 dp (Global ARC); spans 8°…172°.
- **Slot**: 44 dp; selected = scale 1.2, −3 dp towards the outside, 5 dp `acc` dot 30 dp inside the arc;
  missing target = 35 % + 14 dp "!" badge; empty = 6 dp `fg3` dot; FLOW = name text, no icon.
- **Set handle**: ‹ › at both arc ends (13 sp, `fg3`, `acc` + scale 1.25 when hot). Set name + dots only
  while ARC is open.
- **Live capsule**: height 30 dp, centred on the camera, width = content (min 30, typical 132, max 220 dp).
- **PULSE card**: 12 dp side margins, top 8 dp, 44 dp top padding for the camera; blocks: context, 4
  actions (44 dp keys, 11 sp labels), 3 quick targets.
- **Studio row**: 36 dp glyph tile (outline), title 16 sp, one-line summary 13 sp, chevron.
- **Search**: field 48 dp at the thumb, results upwards, first result tinted (Enter target); FLOW match card
  with numbered order and *Spustit*; no count header, no permanent "Enter" hint (shown once, first time).

### 4.5 Interaction model
**ARC (LENS)**
1. Hold LENS 260 ms (configurable 200–400 ms) → haptic `LONG_PRESS`, LENS scales to 86 %, arc
   with the last-used set appears (60 ms fade, no scale).
2. Neutral zone: distance < 40 dp from LENS centre → nothing selected; release here = cancel.
3. Select: 70–200 dp and within ±15° of a slot angle (150/120/90/60/30°) → slot selected, `CLOCK_TICK` on
   each change, name shown above the arc.
4. Switch set: distance > 70 dp and angle < 14° (right end, down to −35°) or > 166° (left end) → next /
   previous set, `SEGMENT_TICK`; the next switch needs leaving the end zone first. Positions in each set
   are fixed (muscle memory). Up to 4 sets × 5 slots.
5. Release on a slot → run via Action Engine, `CONFIRM`; missing app or empty slot → nothing runs,
   `REJECT` + a 1.6 s toast saying why. Release anywhere else, or `ACTION_CANCEL` → cancel, no haptic.
6. Left hand: mirrored angles (Studio → Ruka). TalkBack/keyboard alternative: Studio lists the same sets;
   long-press LENS with TalkBack opens a list dialog of the current set.
Ergonomics (to verify on the phone in phase B): the right end handle is 118 dp to the right of LENS on a
360 dp screen (x ≈ 298 dp), within the right thumb's comfortable arc; the left end (x ≈ 62 dp) is a
stretch for the right thumb, hence the mirrored option.

**PULSE 2.0**
- Idle: no pixels drawn. Touch area 68 × 48 dp around the cutout (from `cutoutPath` or calibration).
- Live: event → capsule grows from the camera (260 ms, slight overshoot), returns to idle 6 s after the
  event ends. Sources: media session playing (with notification access: artwork; without: generic glyph
  of the media app is not possible → a neutral note glyph), timer, FLOW running, opt-in charging, opt-in
  picked notifications.
- Tap: expanded card grows from the camera (320 ms). Content: media if a session is active, otherwise the
  user's quick set. Close: tap outside, swipe up, Back.
- Double tap (default: torch), swipe left/right (default: previous/next track if media, else nothing),
  hold 320 ms → **Global ARC**: same model as ARC, arc hangs below the camera (radius 104 dp, one set by
  default, more sets possible). The top is outside the one-hand zone: hold PULSE is meant for the index
  finger / two hands; every Global ARC action is also assignable to tap/double tap.
- Honesty: actions that Android cannot toggle (Wi-Fi, Bluetooth, mobile data) open the system panel;
  nothing is shown as "done" unless the API returned success.

**Action Engine** (one small sealed type, no framework)
`Action = App(component) | Flow(name) | Search | Shortcut(pkg,id) | Media(PlayPause|Next|Prev) | Torch |
Screenshot | SoundMode | SystemPanel(kind) | Card(id)`, each with `isAvailable(context)` and `run()`
returning `Done | Opened | Unavailable(reason)`. Bindings: `Binding(surface, gesture/slot) → Action`
for LENS-ARC sets, Global ARC, PULSE gestures, Search exact match. FLOW `vlak` can be bound anywhere.

### 4.6 Motion and haptics
| moment | duration / curve | haptic |
|---|---|---|
| LENS hold → ARC | 60 ms fade | `LONG_PRESS` |
| slot change | 140 ms spring (damping 0.7) | `CLOCK_TICK` |
| set switch | 0 ms content swap | `SEGMENT_TICK` |
| run | 120 ms fade out | `CONFIRM` |
| reject | — | `REJECT` |
| PULSE live / expand | 260 / 320 ms, overshoot ≤ 4 % | none / `CONTEXT_CLICK` |
| return to Home | none | none |
No idle animation (the equaliser runs only while media plays and the capsule is visible). Reduced Motion
(system or Studio): all durations 0, no overshoot.

### 4.7 Studio (progressive disclosure)
Level 1: Vzhled · Domovská plocha · Gesta a ARC · PULSE · FLOW · Systém a pokročilé, each with a one-line
summary. Live preview only inside Vzhled (compact, collapsible). PULSE: switch, service state, calibration,
gestures, live events (with the reason a permission is needed), return delay. ARC: set selector, mini arc
with fixed slots (tap a slot → Action picker), set switch, hand, haptics, sensitivity.

### 4.8 Accessibility
Every gesture has a list alternative (Studio, TalkBack action). Large font: summaries wrap to two lines,
ARC name max 2 lines. Light/dark both specified. No cutout: PULSE anchors to the top centre (32 dp target)
or can be turned off. Overlay hides over secure windows (`FLAG_SECURE` apps and system dialogs keep their
own touches; the overlay is a 68 × 48 dp area only).

### 4.9 Feasibility and migration
- Compose only, no blur, no new permissions for the default set. Media titles/artwork and picked
  notifications need notification access (opt-in, off by default) → separate decision in phase B.
- Settings migration: `arc` → set 1, `arcFlows` → set 2, `arcShortcuts` → set 3; `arcRings` (1–3) → number
  of sets; `accent = Terracotta` keeps terracotta for existing users, new installs get Mráz; `clockStyle`
  Serif stays an option. `pulseCharging`/`pulseAlarm` map to live events; `pulseEcho` retires (it is an
  animation on every return to Home, which the brief rules out). Migration in `sanitized()` with tests.
- Not covered by the prototype: drawer redesign, Vzhled/Home/FLOW/System detail screens, light theme of
  the material, landscape.

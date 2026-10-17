# Design system — Crystal Matte Minimalism (phase A proposal, 2026-10-16)

**Status: approved by the owner 2026-10-16 (D10). Implementation starts with phase B1.**
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
  (Phase B2: the target phone is 420 dpi = 411 dp wide, so these are applied as ±62 / ±108 dp from LENS.)
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

---

# 5. Intelia Veil and Home Studio V2 (phase A proposal, 2026-10-16, awaiting owner approval)

Supersedes the "no blur" line of D10 only where stated below; everything else in sections 1–4 stays.
Interactive prototype (private): https://claude.ai/artifact/5oVfKwJrveYy1d9YkZN1Ss
Source: `docs/design/veil-prototype.html` (1 px = 1 dp, 360 × 797 dp). Decision draft: D12.

## 5.1 Review of the current state (phone, Lab build `2e4331c`)
- Two visual languages: Home and LENS are Crystal (Roboto Flex, frost), but FLOW, Search, drawer and Studio
  still use Ink (serif titles, italic FLOW names, terracotta, Roboto Mono caps such as "ARC · LENS").
- The FLOW list puts an accent *Spustit* on every row; app chains are names, not icons.
- Customisation is one long Studio page; nothing can be changed where it is on Home. Long-press on empty Home
  opens Studio, long-press on an icon opens a sheet.
- LENS → Search cuts to a new opaque sheet: no spatial origin.
- Known overlap: the 100 dp LENS target covers the inner 8 dp of the two inner Essentials icons.

## 5.2 Material system
Four levels, one token set (`VeilTokens`), chosen in Studio → Vzhled and overridden by accessibility.

| level | fill (dark) | edge | blur | use |
|---|---|---|---|---|
| Clear | none | none | none | Home content, Search results on the wallpaper (text shadow 0/1 dp) |
| Veil (default) | top→bottom `mix(acc 9 %, #16181D @ 70 %)` → `#0E1014 @ 78 %` | origin edge | none | all transient surfaces |
| Frosted | `mix(acc 8 %, #1C1E24 @ 40 %)` → `#121418 @ 48 %` | origin edge | see 5.3 | Search, Studio, editor tools |
| Solid | `#16181C` | 1 dp white @ 15 % + accent origin | none | accessibility, weak devices |

Light mirrors: Veil `mix(acc 9 %, #FAFBFC @ 74 %)` → `#F2F4F6 @ 82 %`, cold edge white @ 80 %, hot edge `acc @ 85 %`.
Editor tools never go below Veil (Clear applies to Home and Search only). System "high contrast text" or
Studio → Přístupnost → Solid forces Solid.

**Densities as approved (D12, 2026-10-16) — implemented in `ui/Veil.kt` as `VeilLevel` + `veilSurface(tokens, origin)`.**
The owner asked for calmer, matte panels; the "Veil" row above is split into three densities. Neutral bases
(`#15171B`→`#0E1013` dark, `#F5F6F7`→`#EDEFF1` light) with 6 % accent, no gradients beyond top→bottom:

| level | fill opacity (top → bottom) | use |
|---|---|---|
| Veil Light | 66 → 72 % | small controls: editor bar pills |
| Veil Dense | 80 → 85 % (+ 40 % scrim for sheets) | contextual editors, app sheet, dialogs |
| Veil Focus | 90 → 94 % | Studio |

Edge: 1 dp, accent mixed 35 % toward white (dark) / black (light) at 55 % / 45 % alpha at the origin, fading to
7 % within 150 dp; inner glow ≤ 7 %. Measured on the phone: 78 % let Home icons show through a sheet, hence 80 %.
Light and coloured wallpapers not yet checked on the device.

**Clean Veil (part 13, 2026-10-17)** — less grey film, crisper outline, same legibility. Bases `#101216`→`#0B0C0F`
dark, `#F7F8F9`→`#F0F2F4` light, 3 % accent; Light 62 → 68 %, Dense 77 → 83 %, Focus 88 → 93 %; cold edge
white @ 14 % (black @ 10 % light), plain edge 18 % / 14 %, glow 6 % / 4 %. `VeilTest` keeps fg text ≥ 4.5:1 on
Veil Light over a white wallpaper (dark theme) and a black one (light theme). Every card, sheet, pill and
inspector draws through `veilTokens`, so the change is uniform.

**Origin edge (the Veil signature).** A 1 dp outline whose colour is a radial gradient: `acc` mixed 78 % with
white at the point the surface came from (LENS for Search, the selected element for an inspector, the camera
for PULSE, the button for a menu), fading to white @ 8 % within ~150 dp. Plus an inner glow `acc @ 15 %` over
~240 × 150 dp at the same origin. Static (no animation), one `Brush` per surface, recomputed only when the
surface moves. ARC uses the same idea: the arc segment under the selected slot lights in `acc`.

### 5.2.1 Glass styles (owner request 2026-10-17)

One `Glass` setting restyles every Veil surface (panels, sheets, Studio, Search field, clock plate). `Veil` keeps the
lit origin edge; the other three have **no outline at all** and set a panel apart by light:
- **Crystal** — clear neutral fill, white sheen falling from the top edge (to 45 % of the height), faint accent glow.
- **Frost** — milky base (dark `#1C1F25`, light `#FAFBFC`), +8 points density, a tiled 64 px grain @ 4–5 %.
- **Liquid** — a bright, short sheen (22 %) at the top and the accent pooling at the bottom (bloom @ 10–12 %).
In the dark theme the edgeless fills are 6–9 points denser than Veil, since light added on top lowers the contrast
of white text; `VeilTest` checks ≥ 4.5:1 over a white wallpaper with sheen, glow, bloom and grain included
(density shift 0 and +5). Solid (Matné) still wins over every glass. Older documents decode as `Veil`.

Text drawn straight on the wallpaper (clock, date without a plate) takes the palette's variant for the wallpaper
(`LocalWallPlaces`), not for the theme; on a Veil plate the theme's variant is used.

## 5.3 Blur feasibility (measured on Nothing Phone (3), Android 16, SDK 36)
- `ro.surface_flinger.supports_background_blur = 0`, `WindowManagerService mBlurEnabled=false`, battery saver
  off: **cross-window blur is not available**. `Window.setBackgroundBlurRadius` / `FLAG_BLUR_BEHIND` do nothing.
- The wallpaper is a live wallpaper (`com.nothing.blurwallpaper.AtmosphereWallpaperService`), so there is no
  bitmap to blur ourselves (and `WallpaperManager.getDrawable` is not available to third-party apps on 13+).
- Therefore Frosted means, per device:
  1. `windowManager.isCrossWindowBlurEnabled` true → window background blur while Search/Studio is open
     (whole window, transient), with `addCrossWindowBlurEnabledListener` falling back to Veil live.
  2. Otherwise (Phone (3)) → blur only Intelia's own Home layer (clock, icons) with
     `graphicsLayer { renderEffect = BlurEffect(9.dp) }` on API 31+, panels use a denser Veil. Below API 31 → Veil.
  Studio states this honestly ("Rozostření tapety toto zařízení nepodporuje"), never labels the fallback as blur.
- Blur runs only while a transient surface is open; idle Home renders no effect.

## 5.4 Home layout model (data)
One bounded JSON document in `SettingsStore`, validated only by `LauncherSettings.sanitized()`:
```
HomeLayouts(active: String, layouts: List<HomeLayout>)          // ≤ 8 layouts
HomeLayout(id, name, base: Pure|Essentials|Balanced|None, placement: Grid|Free|Hybrid,
           grid: GridSpec(cols 3..6, rows 4..8), guides: Boolean, items: List<HomeItem>)   // ≤ 40 items
HomeItem(id, kind: App|Flow|Clock|Date|Lens|Widget /*reserved*/, anchor, sizeDp, z, style)
anchor = Cell(col,row) | Free(fx 0..1, fy 0..1 of the safe content area) | LensRow(slot -2,-1,1,2)
         | Lens(fx 0..1, bottomDp)            // LENS only, clamped above the gesture inset
style  = App(component, iconOverride?, label: Boolean, labelText?, action: Action) | Flow(name, icon)
       | Clock(font, sizeSp, wght, wdth, colour, alpha, fmt24, date: Bound|Free|Off, align, background)
       | Lens(sizeDp, style Ring|Dot|Bar, tap: Action, hold: Arc|None, holdMs 200..400, haptics)
```
- Positions survive a different screen: Cell by index (re-clamped when cols/rows shrink), Free as fractions of
  the safe area (`WindowInsets.safeDrawing` minus the LENS row), LensRow relative to LENS (±62 / ±108 dp, clamped
  to 24 dp from the edges).
- Built-in presets are code (templates), not stored; "Obnovit výchozí" rebuilds from the template. User layouts
  are full copies. Switching keeps the previous layout in undo and asks when there are unsaved edits.
- Widgets: the `Widget` kind and `AppWidgetHost` id are reserved in the schema; no UI in this phase.
- Migration schema 1 → 2: `layout`, `favorites`, `showClock`, `showDate`, `clockStyle` become one active layout
  "Moje plocha"; nothing is dropped; tests for old, empty, corrupted and round-trip documents.
- Editing works on an in-memory copy in `HomeEditorState` (undo/redo ≤ 50 immutable snapshots); ✓ writes the
  document once, atomically; Back with changes asks Pokračovat / Zahodit / Uložit.

## 5.5 Home Editor interaction
- **Enter:** long-press empty Home 450 ms (`LONG_PRESS`), long-press an icon (enter + drag immediately),
  Studio → Plocha → Upravit plochu. Today's long-press → Studio moves behind the gear.
- **Transform, not a new screen:** wallpaper zoom-out via `WallpaperManager.setWallpaperZoomOut(token, 0→0.6)`
  over 320 ms (visible only if the wallpaper engine supports zoom; ImageWallpaper does, Atmosphere to verify) and
  a 20 % scrim; elements stay in place and get four 7 dp corner ticks (`fg2`, accent when selected).
- **Controls:** top-left preset pill, below it Undo / Redo and the placement pill (Mřížka c×r / Volně / Hybrid);
  top-right gear (Studio), below it ✓ (accent). 36–40 dp, Veil, clear of the camera cutout.
- **Gestures in the editor:** tap element → inspector; drag (> 8 dp) → move; tap empty → "+" at the snapped spot
  → add menu (App, FLOW, hidden clock, Widget reserved). LENS hold does **not** open ARC; dragging LENS moves it
  within the lower band (x 60–300 dp, snaps to centre) and the ghost arc shows the adapted ARC.
  Implemented: the position is `Anchor.dx` of the layout's LENS (dp from the centre, ±120, at least 60 dp from an
  edge; absent → migrated from `Reach` ±56); snap within 10 dp with a tick; one drop = one undo step; the inspector's
  Poloha slider (4 dp steps) is the accessible twin. Search and ARC follow the active layout. LENS-row slots that
  would come within 24 dp of an edge continue on the other side at the same 46 dp pitch.
  Drags in the editor add the touch slop back on their first step, so LENS and icons stay under the finger. The
  LENS inspector's zone reaches up to the outermost ghost ring, so the card sits above the arcs (it may cover the
  clock while LENS is edited).
- **Drag feedback:** element follows the finger; accent ring shows the target; grid dots appear only while
  dragging (Grid, Hybrid); accent guides on alignment with the centre or another element (Free, Hybrid, can be
  switched off); safe bands and the LENS zone are drawn faintly; a dashed ring shows where a displaced icon goes
  (swap in the LENS row, nearest free cell elsewhere). Clock, date and the LENS zone are never covered.
- **Inspector:** one at a time, 312 dp wide, above the element if it fits, otherwise below, clamped to the
  screen and kept above the IME; its origin edge points at the element.
  App: aplikace, ikona (výchozí / systém / mono / Veil), velikost 32–64 dp, popisek + vlastní text, akce
  (Action engine), posun po 4 dp, Odebrat z plochy (never uninstall). Clock: písmo, velikost (also by the ⤡
  handle), tloušťka, šířka, barva, krytí, formát, datum, zarovnání, pozadí, skrýt. LENS: velikost, styl, klepnutí,
  podržení + prodleva, haptika, poloha, link to ARC sets. PULSE (tap the camera ring): poloha vůči průstřelu,
  kapsle, gesta, události → Studio · PULSE.
- **LENS zone fix:** touch target 84 × 100 dp instead of 100 × 100 (still ≥ 48 dp), so the inner Essentials at
  ±62 dp are no longer covered.
- **Accessibility:** every editor action is also reachable from the inspector (posun buttons instead of drag),
  TalkBack custom actions on elements ("Přesunout", "Upravit", "Odebrat z plochy").
  LENS in the editor: "Posunout LENS vlevo / vpravo" (24 dp, clamped like a drag) and "LENS na střed", each one
  undo step via `setLensDx`.
- **As built (V2 parts 4–6, phone-checked):** bar = left "Předlohy" pill + Undo/Redo, right Studio + ✓; the
  placement pill moved into the preset strip. The preset strip sits above the LENS row (not under the bar) so the
  previewed clock stays visible; tap = preview outside the undo history, Použít = one undo step, Obnovit asks.
  ✓ asks "Přepsat vlastní rozložení?" when a saved own layout would be replaced from a preset or from the linked
  preset. Inspectors and the add menu go above their element only when that leaves the clock uncovered, else
  below. App inspector today: size, Posunout (one cell, or the next LENS-row slot), Popisek, Odebrat z plochy,
  Další volby (old sheet); icon variants, custom label text and actions are not built. Clock: typeface, size
  (+ handle), date; Pokročilé: weight, colour (Auto/Akcent/Světlá/Tmavá), opacity. LENS: size 36–56 dp, Kroužek /
  Plný, ghost ARC; LENS cannot be moved yet. Add menu: Aplikace, FLOW (drawn on Home as the FLOW glyph, tap runs
  it), Widget reserved. LENS touch area is 84 × 100 dp (checked on the phone).

- **Customisation lives on Home (owner, 2026-10-16):** Studio no longer styles anything; it links to the editor.
  Bar pill **Vzhled** = Look panel (Veil Dense, above the LENS row): Panely Veil / Matné (= Solid), Hustota −10…+10
  percent points on every Veil level, Světelná hrana on/off (off = neutral 12 % hairline), Motiv, accent, Ikony
  Barevné / Mono / Veil (§5.8 disc + ring), global icon size 32–64 dp (an item's own size wins; "Stejná velikost"
  clears them), popisky for all, entry to PULSE. Settings in the panel apply at once; labels/sizes are layout edits
  with undo. Every slider has − / + buttons of one step. Pinch with two fingers resizes the element under them
  (or the selected one), coalesced into one undo step. Date is its own element: format Dlouhé / Krátké / Den,
  size 9–32 sp, pinned under the clock or free (dragged like the clock), colour, opacity, hide. Clock: Skrýt hodiny;
  the Add menu offers Hodiny / Datum when hidden. LENS inspector adds Poloha (Vlevo / Střed / Vpravo). PULSE
  inspector: on/off, krytí, ozvěna, nabíjení, budík + okno, Poloha kamery → Studio calibration.

## 5.6 Motion
| moment | spec | haptic |
|---|---|---|
| LENS → Search | 0 ms LENS 90 %; 70 ms ring → field at LENS position (width/height/radius, 170 ms, emphasized decel); field rides the IME (`WindowInsets.ime`, same frame, ~220 ms); results grow upwards, 18 ms stagger, 140 ms | `VIRTUAL_KEY` |
| Search → Home (Back) | results 90 ms out; IME + field down; field → ring 160 ms; LENS reappears | none |
| Home → Editor | wallpaper zoom-out 320 ms; marks fade 180 ms; controls slide 8 dp from the top 240 ms | `LONG_PRESS` |
| element → inspector | scale 0.86 → 1 from the element, 220 ms, overshoot ≤ 3 % | none |
| drop | 220 ms to the target, overshoot ≤ 4 % | `CONFIRM` |
| ARC | unchanged from 4.5/4.6; selected arc segment lit in `acc` | as 4.6 |

As built: the ring → field morph is drawn as a clip + outline over the full-size field (layout never changes, so
focus and the IME start in the first frame); Back runs it in reverse after hiding the IME. Inspector entry is
scale 0.86 → 1 from the element edge. All durations go through `motionMs`, so Reduced Motion makes them 0 ms.

As built (brisker motion, 2026-10-16): Calm durations cut by about a quarter — Search ring → field 40 ms delay +
140 ms, back 120 ms; drawer in 160 ms / out 110 ms; page fade 140 / 100 ms; inspector 160 ms; ARC reveal 130 ms,
slot 80 ms; Home quiet 130 ms; LENS press 110 ms; ring toggle 120 ms. The table above keeps the original intent.

As built (Search in Veil, `53b9769`): Search and the drawer are layers over Home, not pages; Home stays composed
under a black 42 % scrim. Field = Veil Dense pill, centred on LENS while the IME is down, then riding it via an
`offset {}` that reads `WindowInsets.ime` (placement only). Card = Veil Dense, ≤ 6 rows (`SearchPicks`), first row
lit with the accent at 16 % and run by Enter; exact FLOW = card with name, numbered app-icon chain, Spustit; a FLOW
name that only starts with the query completes on Enter, never runs; blank query = Moje (favourites; there is no
launch history, so no "recent"). Back, or the IME closing, plays the morph in reverse (one Back). Drawer = Veil
Focus sheet (8 dp inset) with A–Z, rail and Moje; its bottom field opens Search. The card is composed two
frames after the field (it is invisible until the morph is 60 % through), so the open cost is split. App options
over Search or the drawer use Veil Focus and a 60 % scrim. Not built: 18 ms row stagger (no room inside the
card's 40 % fade window of a 170 ms morph). Drawer kept (2026-10-16): composed 700 ms after Home rests and then parked off-screen
(alpha 0, no semantics, no hit testing) while closed, so a swipe up only animates it; icons are warmed in
`IconCache` while Home rests.

Input is never delayed: typing works from the first frame, the IME opens with the morph, the action runs on
release. Reduced Motion → all 0 ms. No idle animation; nothing animates while Home is at rest.

## 5.7 Studio (progressive disclosure)
Home Editor = direct visual changes. Studio = advanced, opened from the gear or system settings. Level 1 lists:
Vzhled a materiál · Ikony a text · Plocha a předlohy · Gesta a ARC · PULSE · FLOW · Systém a pokročilé, each
with its current value. FLOW, Search, drawer and Studio adopt Crystal/Veil type: one family (Roboto Flex), no
italic serif, no mono caps; FLOW chains shown as 28 dp icons with step numbers.

## 5.8 Icons
`LauncherApps` icons; monochrome layer of `AdaptiveIconDrawable` (API 33+) tinted `fg`, silhouette fallback
below; Veil style = monochrome glyph on `fg @ 9 %` disc with a 1 dp `fg @ 22 %` ring. Icon packs: ADW/Nova-style
`appfilter.xml` packs found through a `<queries>` intent entry (no permission); needs a D-entry before landing.
A Nothing look only through an installed compatible pack, never copied. Missing icon → letter on a Veil disc.
**Tinted** (part 13): the monochrome glyph in the palette's Icons colour (§5.12) on a disc of the theme's Veil
base @ 62 % washed with that colour @ 16 %, ring @ 40 %; apps without a monochrome layer keep the grey
silhouette on the same disc. Chosen in the Look panel, Studio and the icon inspector (with its colour).

## 5.9 PULSE continuity (B4)
Same Veil material and origin edge (origin = camera), same type and motion: idle draws nothing, the live
capsule and Quick View grow from the camera and return into it; hold = Global ARC with the lit-segment arc.

## 5.10 Performance and cost
Measured 2026-10-16 on the Nothing Phone (3) (120 Hz), Lab build, warm (one open/close before
`gfxinfo reset`), 3 × LENS → Search → Back → Back: old full-page Search (`c2e4b5b`) 83–85 janky frames of ~590,
14.0–14.5 %, p99 150 ms; Veil layer (`a59a4a2`) 43–45 janky of ~295, 14.5–15.3 %, p99 61–65 ms. The percentage is
flat because the new path draws half the frames; janky count and p99 halved. First open frame still costs ~46 ms
UI (composition of the layer); frames while the IME rides take 10–13 ms (> 8.3 ms budget). PSS 5 s after a cold
start: 107–109 MB old, 108–112 MB new (same within noise).
Release build (R8 + `compile -m speed`, debug key, same `.lab` id; debug builds stay interpreted/JIT and inflate
UI time ~2.5×), same procedure: `7fc3707` 14–18 janky of ~310 (4.5–5.8 %), p99 28–29 ms, longest UI frame
21–26 ms; card two frames later (`61484cf`) 15–16 janky, p99 21–25 ms, longest UI frame 10–19 ms. After the LENS drag
(`7dca980`, LENS centred): 14–16 janky, p99 18–24 ms, longest UI frame 11–12 ms. Remaining jank:
the open frame (~12 ms UI + first-use GPU), focus/IME setup (~16 ms), IME ride frames GPU 5–10 ms, and one
GPU context re-init after ~0.5 s idle. Third-party accessibility services add semantics work after every layout.

Veil = one gradient fill + one gradient stroke per open surface; Clear/idle Home = no extra draw. Own-layer blur
only while Search/Studio is open (API 31+). Editor marks and guides exist only in edit mode. Expected cost is
small but must be measured (frame time with `gfxinfo` during LENS → Search and drag, PSS, cold start).

## 5.11 Implementation plan after approval (one small commit each, build + phone check per step)
1. `VeilTokens` + `VeilSurface` modifier (fill, origin edge, inner glow), Clear/Veil/Frosted/Solid switch,
   cross-window-blur detection and fallback; Studio → Vzhled shows the truth.
2. Data: `HomeLayouts` model, templates, schema 1 → 2 migration in `sanitized()` with tests; Home renders from
   the model (no visual change yet). LENS target 84 × 100 dp.
3. `HomeEditorState` (pure Kotlin: drag resolve, snap, guides, collision, undo/redo) with JVM tests.
4. Edit mode UI: entry gestures, wallpaper zoom-out, corner marks, top controls, save/cancel.
5. Inspectors: app, clock (incl. resize handle), date, LENS (ghost ARC), PULSE link; add menu.
6. Presets: switch, save as, revert, reset with confirmation.
7. LENS → Search morph synced with the IME; Back reverse.
8. Unify FLOW, Search, drawer and Studio typography/material (Ink → Veil).
9. Regression on the phone (list in the brief §13), `gfxinfo` measurements, STATUS.

## 5.12 Palette and Veil clock (part 13, 2026-10-17)
**Accents.** 16 hues + Dynamic (`Accent`, new names only appended): Frost, Silver, Ink (Mono), Slate, Sky, Teal,
Mint, Sage, Lime, Amber, Terracotta, Coral, Rose, Orchid, Violet, Indigo. Each has a pale dark-theme and a deep
light-theme variant (`data/AccentColors.kt`), ≥ 4.5:1 against the Veil base of its theme (`PaletteTest`).
Dynamic is the wallpaper's Material You primary (API 31+), Terracotta below.

**Places.** `LauncherSettings.palette`: `unified` (default true = the main accent everywhere) and an optional
accent per place — Clock (accent tint and colon ring), Text (date accent tint), Icons (Tinted style), LENS (ARC
highlights), Search (Search and the drawer), Studio. `AccentScope(place)` re-provides Ink, Crystal and Material
`primary`, so texts, toggles and sliders inside follow the place. Unset place = main accent; the places are
kept while unified, so switching back restores them. Edited in Studio → Barvy and the Look panel (toggle, place
chips, 17-disc grid), in the clock and LENS inspectors (under Pokročilé) with "Hlavní barva" first.

**Veil clock.** `HomeItem.background = "Veil"` (clock only; `sanitized()` drops anything else) puts one Veil
Light plate, radius 26 dp, padding 14/20/16 dp, behind the time and its bound date (prototype `.clock.bgv`).
Text on it takes the theme fg without shadow; the outer margin shrinks 28 → 8 dp so the time keeps its width.
Clock inspector: "Bez pozadí" / "Veil".

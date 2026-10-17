# Status

Only verified facts. Format: `date | branch/commit | evidence | change | open`.

## 2026-10-17 — Glass styles, clock accent on the wallpaper (`claude/crystal-redesign`, `31bd339`, `0a316ce`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 95 JVM tests, 0 failures (new: edgeless
  glasses keep text ≥ 4.5:1 over a white wallpaper with their light included, Veil keeps its edge, `glass` decodes
  and an unknown or absent value stays Veil).
- Phone (Nothing Phone (3), debug Lab via `install -r`; owner's `intelia.xml` backed up, restored, `cmp` identical):
  - Editor → Vzhled: chips Veil / Krystal / Mráz / Kapka switch the Look panel at once; "Světelná hrana" shows
    only for Veil. Studio → Barvy shows the glass and icon chips with their own headings.
  - Bug found and fixed: Light theme over the owner's dark wallpaper painted the clock in the dark accent (hard to
    read) while the date stayed light; after the fix the clock is light terracotta.
  - Light theme + Matné: editor panel, Search (Moje list, field) and drawer legible.

Not verified: how far the three glasses differ by eye at the owner's +10 % density (they look close; the owner to
judge); Frost grain and Liquid bloom only seen in the Look panel; TalkBack; release frame measurement.

## 2026-10-17 — Palette, Tinted icons, Veil clock, Clean Veil, TalkBack LENS in the editor (`claude/crystal-redesign`, `27c5a9b`, `f9d328c`, `d52226c`, `763891d`, `1554151`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 92 JVM tests, 0 failures (new: accent contrast
  ≥ 4.5:1 on both Veil bases, enum order kept, unified/per-place palette and old/odd documents, Tinted decode,
  clock background sanitizing, text contrast on Veil Light over white/black wallpaper).
- Phone (Nothing Phone (3), debug Lab via `install -r`; owner's `intelia.xml` backed up, restored and byte-identical):
  - Palette (test document, per place): clock in Rose, Search field and selected row in Amber, Studio toggles in
    Violet; Studio → Barvy shows the toggle, place chips and the 17-disc grid; choosing Hodiny → Sky recoloured the
    preview's colon at once.
  - Tinted icons in Teal on Home (dark theme, light part of the wallpaper) and in the drawer (light theme).
  - Clock inspector "Veil" → ✓: plate behind time and date; first try clipped a 126 sp clock, fixed by taking the
    plate inset out of the margin (screenshot after the fix: full time).
  - Clean Veil before/after screenshots of the drawer, Search and the clock plate: deeper, less grey fill, crisper
    edge; text legible.
  - Editor, keyboard Tab to LENS + Enter: LENS inspector opens, Search does not.
  - Editor icon drag (`input motionevent`, 300 px up, held): icon centre 257 px vs finger 255 px — no slop lag.
- At the end the release Lab (debug key, `compile -m speed`) is back on the phone; HOME role holder
  `dev.intelia.launcher.lab`.

Not verified: TalkBack itself (double tap goes through the semantics click, changed in code; Enter goes through the
key handler) — owner to try. Tinted icons on a truly light/dark wallpaper (wallpaper not changed; light vs dark
theme checked instead). No release frame measurement this session (drawing changes are fills and colours only).

## 2026-10-16 — Drawer composed ahead, brisker motion (`claude/crystal-redesign`, `130df1a`, `cf4f122`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 85 JVM tests, 0 failures.
- Release build (debug key, `.lab`, `compile -m speed`), Nothing Phone (3) at 120 Hz, `gfxinfo framestats` of one
  swipe up → drawer: before, the first frame spent 23.6 ms on the UI thread (whole `LazyColumn` + icon loads) and
  the first two frames took 33 / 25 ms; after (drawer composed 700 ms after Home rests and kept parked off-screen,
  icons warmed by `IconCache.warm`, `contentType`, `ModulateAlpha` fade) the first frame spends 2.8 ms on the UI
  thread, 13 / 20 ms in total.
- Same build, 3 series of 3 × swipe up → scroll → Back: before 6–12 janky of 532–540 (1.1–2.3 %), p99 27–44 ms;
  after 9–12 of 514–516 (1.7–2.3 %), p99 16–22 ms. GPU time per frame is 7–10 ms median in both and in Search too
  (Nothing's live blur wallpaper is in the frame); not drawer-specific.
- Brisker motion (Calm durations about −25 %, list in DESIGN_SYSTEM §5.6): 3 series of 3 × LENS → Search → Back →
  Back: 9–14 janky of 304 (3.0–4.6 %), p99 17–26 ms (previous entry: 4.4–5.0 %, p99 18–24 ms).
- Home after the change: `uiautomator dump` lists no drawer nodes while it is parked; swipe up opens it ("Aplikace").
- Owner's `intelia.xml` backed up and restored byte-identical (`cmp`).

Not verified: the owner's feel of the drawer in the debug build (debug is much slower than release). The debug
reinstall was done with `adb uninstall`, so the Lab lost its other app data; the HOME role holder is now
`com.nothing.launcher` and the Lab PULSE accessibility service is not enabled — the state before was not recorded.

## 2026-10-16 — LENS inspector clear of the ghost ARC, TalkBack moves, drag slop (`claude/crystal-redesign`, `7780a44`, `1861107`, `f361373`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 85 JVM tests, 0 failures (new: `ArcRings.reach`,
  `HomeEditorState.withSlop`).
- Phone (Nothing Phone (3), debug Lab; owner's `intelia.xml` backed up, restored and byte-identical at the end):
  - LENS selected in the editor, centred and at +104 dp (this phone's edge limit): the inspector card sits above
    the outermost ghost ring, all three rings visible (screenshots).
  - LENS saved at +104 dp, tap → Search recorded (`screenrecord --output-format=raw-frames`, 60 fps): the first
    changed frame shows the field growing from LENS on the right, then the card and IME; `SearchField` takes `dx`
    from the active layout.
  - Editor: `input swipe` LENS 131 px (50 dp) left from +104 dp ended at 55 dp, its centre 685 px vs the finger at
    682 px (previously it lagged by the slop).

Not verified: the new TalkBack actions — `uiautomator dump` does not list custom actions, so only build and code
review; owner to try with TalkBack. Icon drags in the editor share the slop fix but were not measured. No release
measurement this session (no rendering-path change).

## 2026-10-16 — LENS moves in the Home Editor (`claude/crystal-redesign`, `7dca980`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 83 JVM tests, 0 failures (new: `Reach` → `dx`
  migration and bounds, LENS drag/snap/cap/one-step undo, narrow-screen clamp, LENS-row slots folding at an edge,
  presets keep LENS).
- Phone (Nothing Phone (3), owner's `intelia.xml` backed up, restored and byte-identical at the end):
  - Editor: dragging LENS left moved it to −83 dp with the LENS row re-laid around it, inspector "Poloha" showed it;
    one Undo returned it to the centre ("střed") and disabled Undo; dragging right capped at +120 dp; ✓ saved.
  - After saving: LENS at the right, all four LENS-row apps folded to its left; tap opened Search; hold opened ARC
    with its rings bent around the right-hand LENS.
  - Release build (debug key, `.lab`, `compile -m speed`), LENS centred, 3 series of 3 × LENS → Search → Back →
    Back: 14–16 janky of ~316 (4.4–5.0 %), p99 18–24 ms, longest UI frame 10.9–11.7 ms — no regression against
    `61484cf` (15–16, p99 21–25, UI 10–19).

Not verified: the Search morph's start point with LENS off centre (only the end state seen), ghost arcs while the
inspector is open (partly covered by the card), TalkBack on the Poloha slider. Owner to try: LENS drag feel,
pinch, Veil icon style, the morph recording from the previous session.

## 2026-10-16 — Search open split, options sheet over layers, phone checks (`claude/crystal-redesign`, `61484cf`, `41eb836`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 78 JVM tests, 0 failures.
- Phone (Nothing Phone (3), Android 16; owner's `intelia.xml` backed up first, byte-identical at the end):
  - Debug (Lab) numbers are not representative: the debuggable build cannot be AOT-compiled (`dexopt` stays
    `verify`). A release build signed with the debug key and installed over Lab (`compile -m speed`) gives 14–18
    janky of ~310 (4.5–5.8 %), p99 28–29 ms at `7fc3707`.
  - systrace of the open frame (debug): composition 22 ms (Search 19: card 9.4, field 5.6; Home 0.3 ms, so Home
    does not recompose on `page`), apply 10 ms, measure/layout 20 ms; then focus + IME setup ~18 ms before the next
    frame. Accessibility services on the phone (Kea, MT Island) add 6–12 ms of semantics work after layout changes.
  - Card composed two frames after the field (`61484cf`, release, 3 runs): janky 15–16 (4.8–5.1 %), p99 21–25 ms,
    longest UI frame 10–19 ms (was 21–26). Tried and dropped: one row per frame (each frame re-measured the card,
    26 ms each in debug), a layer translation instead of `offset` for the IME ride, an offscreen layer for Home
    under Search (no measurable gain). Frames while the IME rides are GPU-bound (5–10 ms, also with Home cached),
    and the first frame after ~0.5 s idle pays a GPU context re-init (~16 ms, HWUI "GPU Context timeout").
  - Reduced Motion (Pohyb Off): screenshot ~120 ms after the tap already shows the full pill, card and scrim.
  - Long-press on a Search row opens the options sheet; Search stays (query kept, IME hidden), Back returns to it.
    The rows showed through the Dense sheet; now Focus + 60 % scrim over Search/drawer (`41eb836`), checked.
  - Enter: "ma" → Mapy (first lit row) launched; "pocty" → nothing ran while typing, Enter ran the FLOW
    (Kalkulačka, then Hodiny).
  - Light theme: Search card and pill readable over the dimmed Home (clock faintly visible through the card).
  - Morph ring → pill recorded with `screenrecord` (720p) and sent to the owner; not analysed frame by frame here.

Not verified: TalkBack, light wallpaper. Not done: LENS drag in the editor (needs a continuous LENS x instead of
`Reach`), 18 ms row stagger (the card fades in during the last 40 % of a 170 ms morph — no room for 6 × 18 ms
without lengthening it). Owner to try: pinch in the editor, Veil icon style.
During the session one measuring run tapped into an Instagram modal that was on top after an install (script now
aborts unless Lab is on top), and one test tap opened HBO Max from the Search card.

## 2026-10-16 — Search and drawer in Veil over Home (`claude/crystal-redesign`, `53b9769`, `a59a4a2`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 78 JVM tests, 0 failures (new `SearchPicksTest`:
  favourites on a blank query and Enter does nothing, exact FLOW first and not repeated, cap 6 with apps before
  FLOW names, a partial FLOW name completes and never runs).
- Phone (Nothing Phone (3), Android 16, Lab; owner's `intelia.xml` backed up first, byte-identical at the end):
  - LENS tap: Home (clock, date) stays visible dimmed; Veil pill above the keyboard; empty query shows MOJE
    (Chrome, Fotoaparát, Zprávy, Telefon). "ma" → 6 rows, first (Mapy) lit; "pocty" → FLOW card with 1 ⟶ 2 icon
    chain, Spustit and ENTER SPUSTÍ; nothing launched while typing (top activity stayed Lab).
  - One Back closes Search to Home (IME and layer together).
  - Swipe up: drawer as Veil Focus sheet over dimmed Home (Aplikace 221, MOJE row, A–Z with rail, FLOW and Studio
    buttons); its Hledat field opens Search, typing works, Back returns to Home.
  - Measurements: see DESIGN_SYSTEM §5.10 (janky frames 83–85 → 43–45 for 3 cycles, p99 150 → 61–65 ms).

Not verified: the ring → pill morph frame by frame (only end states seen), Reduced Motion, long-press options from
Search, Enter key on the phone, light theme / light wallpaper, TalkBack. Still open from before: pinch and Veil icon
style (owner to try), LENS drag in the editor.

## 2026-10-16 — Customisation moves into the Home editor (`claude/crystal-redesign`, `bfc5c8e` + data commit before it)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 74 JVM tests, 0 failures (new: date unpin/move/pin
  back, clock and date hide + add back once, labels and icon sizes as one undo step, Veil/icon settings clamped,
  date keeps its own size range and format).
- Phone (Nothing Phone (3), Android 16, Lab build; owner's `intelia.xml` backed up first, restored, `cmp` identical):
  - Editor bar shows Předlohy · Vzhled · undo/redo · Studio · ✓; Vzhled opens the Look panel above the LENS row
    (Panely Veil/Matné, Hustota with − / +, Světelná hrana, Motiv, accent swatches, Ikony Barevné/Mono/Veil,
    velikost, popisky, PULSE entry); clock and date stay visible.
  - Icon style Mono: before this change every monochrome icon was an opaque white disc (AdaptiveIconDrawable
    wrapper on Android 16); after the fix the glyphs render (Telefon, Zprávy, Fotoaparát, Chrome checked).
  - Tap on the date opens its own inspector (Dlouhé/Krátké/Den, size 9–32 with − / +, Připnout pod hodiny,
    Pokročilé, Skrýt datum). Unpinning keeps it in place (653 px vs 672 px pinned); dragging moved it to 1069 px.
  - PULSE inspector opens from the Look panel and hangs below the bar. A tap on the camera ring itself is taken by
    the PULSE everywhere overlay (accessibility service on) and opens its Quick View instead.
  - Studio now lists PLOCHA (hint + Upravit plochu → opens the editor on Home), ARC, PULSE (PULSE všude +
    poloha kamery), pohyb, výchozí plocha, skryté aplikace, o aplikaci; Upravit plochu checked.

Not verified: pinch to resize (adb `input` has no multi-touch — owner to try), Solid panels and density on the
phone,
Veil icon style after the fix, clock hide/add back on the phone, theme switch from the panel, TalkBack, light
wallpapers. Not done: LENS → Search in Veil (prototype), drawer as Veil over Home, part 7 measurements.

## 2026-10-16 — Intelia Veil V2, parts 4–6 (`claude/crystal-redesign`, `00d62f5` … `2fa6217`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 70 JVM tests, 0 failures (new: item look
  clamping, slider edits collapse into one undo step, remove never takes LENS, add-at snaps without pushing aside,
  preset preview outside history, date toggle, nudge by cell and on the LENS row).
- Phone (Nothing Phone (3), Android 16, Lab build, dark wallpaper unchanged; the owner's `intelia.xml` was backed up
  first and is byte-identical afterwards):
  - Clock inspector (Veil Dense, 312 dp) opens below the clock, the clock and date stay visible; typeface chips
    change the clock live; size handle drawn at the clock corner; date toggle; weight / colour / opacity under
    "Pokročilé".
  - App inspector opens above the icon: size 32–64 dp, Posunout ← ↑ ↓ →, Popisek, Odebrat z plochy, "Další volby"
    (old sheet). → on the LENS-row phone icon swaps it with Messages; undo in the bar restores it.
  - LENS inspector: size, Kroužek / Plný, faint ghost ARC rings.
  - Tap on empty Home → "Přidat sem" (Aplikace, FLOW, Widget · později), placed below the point when above would
    cover the clock; Aplikace → list → adidas lands at the tapped spot; Back → Zahodit removes it again.
  - Předlohy: strip above the LENS row with thumbnails Prázdná / Kolem LENS / Vyvážená / Vlastní, tap previews in
    place, Zrušit returns; after Použít, ✓ asks "Přepsat vlastní rozložení?" (Zrušit keeps editing). Editing the
    active own layout and ✓ saves without a question (date removed in the saved document; then restored).
  - LENS tap → Search: field focused and the IME up in the same transition, field above the keyboard; typing
    "chr" shows Chrome and launches nothing; Back hides the IME, Back again returns Home.
  - LENS touch area now 84 × 100 dp: tap at −44 dp from the LENS centre opens Messages (before: Search, measured on
    100 × 100); −40 dp and centre → Search; swipe up → drawer; hold at −38 dp → ARC, release on LENS cancels.
  - `dumpsys gfxinfo` (adb input, 120 Hz): 3 × LENS → Search → Back: 599 frames, 12.85 % janky, p50 8 ms,
    p90 24 ms, p99 133 ms, 76 slow UI thread. Icon drag with 22 `input motionevent` moves: 88 frames, 26 % janky,
    p50 6 ms, p90 25 ms (adb spawns a process per move, so this is not a finger drag).
  - PSS settled 15 s after a cold start: 112.9 MB (Graphics 46 MB); about 2 s after start 320 MB was read once.
    Cold start `am start -W`: 622 / 466 / 439 ms.

Fixed during the phone check: the editor bar's undo/redo did not redraw (plain-Kotlin state); the preset strip
covered the clock it was previewing; the add menu could cover the date; → on a LENS-row icon did nothing.

Not done / not verified: light and coloured wallpapers; TalkBack itself (custom actions Přesunout / Upravit /
Odebrat z plochy are in the semantics, TalkBack was not switched on); the morph frames themselves (screencap is too
slow to catch 170 ms); Reduced Motion on the phone; wallpaper zoom-out; dragging LENS; FLOW items on Home are drawn
and run on tap but were not added on the phone; the 12.85 % jank and the 320 MB transient are not analysed.

## 2026-10-16 — Intelia Veil V2, parts 1–3 (`claude/crystal-redesign`, `9f2b215`, `fc1c7a4`, `4af8723`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 62 JVM tests, 0 failures (new: `VeilTest`,
  `HomeModelTest` — schema 1 → 2, empty, corrupted, round trip, save, grid re-clamp; `HomeEditorStateTest` — grid
  snap, displacement, LENS-row swap, LENS zone, blocked clock, guides, Hybrid, undo/redo ≤ 50, clock band).
- Phone (Nothing Phone (3), Android 16, Lab build, dark wallpaper unchanged):
  - Studio panel is Veil Focus, the app sheet Veil Dense; text sharp, edge faint. At Dense 78 % the Home icons
    showed through the sheet, so Dense is now 80–85 % with a 40 % scrim; the sheet now reaches under the nav bar.
  - Home after the schema-2 migration looks identical to before (screenshots compared; Essentials, clock, date).
  - LENS tap → Search, swipe up → drawer, LENS hold → ARC: unchanged.
  - Long-press empty Home → editor (bar: undo, redo, "Hybrid 4×6", Studio, ✓); dragging an icon shows grid dots,
    target ring and the LENS zone; holding an icon opens the editor and drags it at once (first attempt was
    cancelled by swapping the parent's modifiers mid-gesture — fixed by a stable chain); LENS hold in the editor
    does not open ARC; Back with changes asks Pokračovat / Zahodit / Uložit, without changes it just leaves.
  - ✓ saves once: document `schema 2`, active `mine`, favourites intact, position survives force-stop. Afterwards
    the owner's original document was restored from a backup taken before the test (Home back on the preset).

Not done / not verified: light and coloured wallpapers (need the owner's consent to change the wallpaper);
LENS target still 100 × 100 dp (84 × 100 not changed, not measured); inspectors, preset thumbnails, add menu,
wallpaper zoom-out, TalkBack actions in the editor, `gfxinfo`/PSS. Tapping an icon in the editor opens the old
app sheet as a stand-in for the inspector. A saved own layout no longer follows favourite changes from the sheet;
choosing a preset in Studio switches back to the linked preset (the own layout is kept).

## 2026-10-16 — Intelia Veil + Home Studio V2, design phase A (`claude/crystal-redesign`)

Verified (no Android code changed, no build run):
- Phone (read-only `getprop` / `dumpsys`, no input): `ro.surface_flinger.supports_background_blur=0`,
  `mBlurEnabled=false`, battery saver off, SDK 36; wallpaper is the live
  `com.nothing.blurwallpaper.AtmosphereWallpaperService`. Cross-window blur is not available on this phone.
- One phone screenshot (FLOW list, the owner was using the phone): still Ink style (serif, terracotta, mono caps).
- Prototype `docs/design/veil-prototype.html` in the built-in browser: no console errors; scenes clock
  inspector, icon move demo (grid dots, target ring, drop with label), LENS → Search (field above the keyboard,
  FLOW `pocty` card), ARC on the light wallpaper. Published privately as
  https://claude.ai/artifact/5oVfKwJrveYy1d9YkZN1Ss.

Proposed then (approved later the same day, see D12): D12 (Veil material, origin edge, blur policy, Home layout model, Home Editor,
LENS target 84 × 100 dp). Not tested: whether the Atmosphere wallpaper honours `setWallpaperZoomOut`.

## 2026-10-16 — Redesign phase B2, Home + LENS (`claude/crystal-redesign`)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; 44 JVM tests, 0 failures
  (new: `newInstallGetsCrystalClockExistingUsersKeepTheirs`).
- Installed on Nothing Phone (3), Android 16 (`dev.intelia.launcher.lab`, default HOME), dark wallpaper:
  - Before: serif split clock, LENS ring with the terracotta dash, Essentials 30 dp icons.
  - After install the owner's document kept `clockStyle` Serif (shown as "Ink"); date is Roboto Flex caps at 62 %.
    Studio → Písmo hodin shows Crystal · Ink · Grotesk · Strojové; picking Crystal gives a one-voice `14:17` clock.
    The owner's clock style is now Crystal (set in this test; the owner chose to keep it).
  - LENS: ring measured 114 px = 43.4 dp at 420 dpi, no dash. Tap → search with keyboard; swipe up → drawer
    without keyboard; hold → existing ARC (rings, LENS settles to 86 %), release on LENS cancels.
  - Essentials: centres measured at ±62 / ±108 dp from LENS on its baseline, 40 dp icons, no backing.
    Tap Telefon → Dialer, tap Fotoaparát → Nothing Camera; Home key returns to Lab.
  - Recents (Nothing quickstep) → Home key → Lab `MainActivity` on top.
- The phone is 420 dpi = 411 × 911 dp, not the 360 dp of the prototype; the Essentials x values of 4.3 are
  therefore implemented as offsets from LENS (±62 / ±108 dp), and LENS stays at bottom inset + 52 dp.

Not verified on the phone: light wallpaper (dark ink via `HINT_SUPPORTS_DARK_TEXT`; the owner's wallpaper was
not changed), Reduced Motion with the new LENS press (durations go through `motionMs`, so 0 ms when Off),
Pure and Custom layouts after the change, TalkBack. Known overlap: the 100 dp LENS target covers the inner
8 dp of the two inner Essentials icons (same as the prototype's lens zone); LENS wins there.

## 2026-10-16 — Redesign phase B1, design system and components (`claude/crystal-redesign`, 9fbc7cf)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS; `testDebugUnitTest --rerun`: 43 JVM tests,
  0 failures (new: `ActionTest` ×6, `CrystalArcGeometryTest` ×2, `newInstallGetsFrostExistingUsersKeepTerracotta`).
- Installed on Nothing Phone (3), Android 16 (`dev.intelia.launcher.lab`, update 13:29). Home screenshots before
  (13:28) and after (13:29) are identical apart from the time: serif clock, date, Essentials, LENS, PULSE ring;
  the owner's existing settings kept the Terracotta accent (schema 0 → 1 migration on a real document).
- D11: Roboto Flex (OFL 1.1) bundled as `res/font/roboto_flex.ttf`, licence in `assets/licenses/`.

Not verified on the phone: the Frost/Silver swatches in Studio (no touch input this session; the owner was
using the phone), rendering of the Crystal components and Roboto Flex axes (no screen uses them yet; B2).
Not measured: APK size impact of the font (+1.7 MB file; debug APK is 15.0 MB).

## 2026-10-16 — Redesign phase A, design review (`claude/crystal-redesign`)

Verified (no Android code changed, no build run):
- Phone screenshots (Lab build, default HOME): Search with query, FLOW exact match `pocty`, drawer (223 apps),
  PULSE tapped over Calculator with `PulseAccessibilityService` running; only one overlay window
  (`Intelia PULSE`, 94 × 94 px at 493,21, `ACCESSIBILITY_OVERLAY`).
- MT Capsule 17.2 inspected read-only: its accessibility and notification services run, portrait screen
  profile is off so it draws no window; granted permissions include INTERNET, AD_ID, notification listener.
  Nothing in it was changed.
- Prototype `docs/design/crystal-prototype.html`: no console errors; scripted pointer test in the browser —
  hold LENS → 5 slots, slide to the right end → set FLOW, slide up → slot 3 `běh`, release → run;
  hold the camera on the app screen → Global ARC, slide down → FLOW `vlak`, release → run; Studio ARC
  shows 3 sets.

Owner approved D10 (direction A, arc-end set switching, PULSE 2.0) on 2026-10-16.
Open: ergonomics of the arc ends and of
Global ARC not tested on the phone (prototype only). Roboto Flex licence entry before bundling (done in B1, D11).

## 2026-10-16 — Ink rework (`claude/ink-rework`, 402d10a)

Verified:
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS (21 JVM tests, incl. new
  `pulseLensIsClampedAndNonFiniteFallsBackToAuto` and `pulseAlarmHoursStayWithinADay`).
- On Nothing Phone (3), Android 16, `dev.intelia.launcher.lab` debug build:
  - Onboarding (3 steps); Home shows only the wallpaper; Essentials around LENS.
  - Tap LENS → search with keyboard; swipe up → drawer; results grow upwards; Enter opens the first app.
  - Exact FLOW match shows *Spustit*; Enter runs the FLOW in the right order; typing runs nothing.
  - App options; ARC ring 1 (apps) and ring 2 (FLOW launched on release); Studio with live preview; light theme.
  - PULSE capsule with battery while charging.
  - Camera calibration (Studio → PULSE → Poloha kamery): ‹ › change the value by exactly 0.5 dp; *Uložit* →
    Studio shows "Vlastní" and values survive reopening; *Automaticky* → "Automaticky", detected from
    `cutoutPath` as 205.5 / 26.0 / 8.0 dp. Capsule sits around the camera on Home (1 screenshot, deleted).
  - With Lab as HOME: Recents (Nothing quickstep fallback) shows only wallpaper behind the cards, no rectangle;
    Home from Recents, from an app (Calculator launched from LENS) and from the drawer returns a clean Home;
    Studio shortcut picker works only as HOME; ARC ring 3 ("ZKRATKY") appears and selects a shortcut by angle
    (−40° = 1st shortcut); returning to LENS cancels. Test shortcuts removed, rings set back to 2.
- Debug build timings: cold start ~845 ms, warm ~57 ms; PSS ~131 MB.
- PULSE toggles (Lab started with `am start`, not HOME; on AC power): *Ukázat nabíjení* off → no capsule
  while charging, on → capsule with 90 % is back. *Ozvěna při návratu* on → a ring leaves the camera right after
  returning to Lab from the system home; off → no ring at the same moment. All PULSE toggles left on.
- D6 (ring 2 from the centre, `ac65740`): `:app:testDebugUnitTest :app:assembleDebug` PASS (22 tests). On the phone
  the only ARC FLOW (`pocty`) sits straight above LENS in ring 2 (before: far left) and is selected by sliding
  straight up; sliding back onto LENS and releasing cancelled it (nothing launched). Not run by release this time.
- D7 (capsule width/opacity): `:app:testDebugUnitTest :app:assembleDebug` PASS (23 tests, new
  `pulseCapsuleWidthAndOpacityAreBounded`). On the phone, tapping the Studio sliders set +20 dp and 70 %; the
  live capsule at the camera in Studio grew and turned translucent, and Home showed the same capsule. Both set
  back to +0 dp / 100 %. Dragging the bead (rather than tapping) and TalkBack slider actions not tested.
- Near-alarm capsule (owner set an alarm for 10:00; checked 09:46 while charging): the capsule shows bell + 10:00
  on the left and the battery ring + 90 % on the right. **Problem:** its left half covers the system status-bar
  alarm icon and the 90 % repeats the system battery. Status bar is 126 px (48 dp) high.
- D8 (status-bar capsule, no text at rest): `:app:testDebugUnitTest :app:assembleDebug` PASS (26 tests, new
  `PulseShapeTest`). On the phone (AC power, 90 %, Lab via `am start`): at rest only a 28 dp disc around the
  camera with the terracotta battery ring, x ≈ 503–577 px, clear of the status icons (left end ≈ 445 px,
  right start ≈ 770 px). Studio preview shows the disc plus the alarm dot (38 dp). Tap just below the camera
  (540,165) opens the panel from the lens (alarm 5:40 po 19. 10., 90 % nabíjí se, torch) with the status icons
  readable on top; Back closes it; a tap outside closes it.
- MT Capsule (`com.pryshedko.mtisland`): its accessibility service is on but it had no window at rest while
  charging; its expanded state was not seen (needs the owner). Taps at the camera itself (y < 126 px) go to
  SystemUI's status bar, or to the old `dev.intelia.launcher.debug` "Intelia Pulse" accessibility overlay
  (frame 498,24–582,108 px), which opened that build's drawer once; nothing was changed in either.
- MT Capsule explored (owner's permission): single tap can be scroll, screenshot, launch app, torch, a card of
  shortcuts / sliders / barcodes, or app search; slider cards are volume, brightness, torch strength with toggles
  for auto brightness, volume mode, torch, screenshot and the Wi-Fi panel. With its service on it still had no
  window, so its own lens tap was never seen working. Its single tap was switched to "Shortcuts / Sliders" and
  a Volume card exists from the exploration. `.debug` "Intelia Pulse": a lens tap opens its drawer activity;
  media control there needs notification access.
- D9 (PULSE everywhere): `:app:testDebugUnitTest :app:assembleDebug` PASS (33 tests, new `PulseMenuTest`).
  The owner switched off `.debug` "Intelia Pulse" and switched on "Intelia PULSE" (`.lab`); MT Capsule stayed on.
  Overlay `TYPE_ACCESSIBILITY_OVERLAY` at 493,21, 94 × 94 px. A tap on the lens (540,66) opens the menu on Lab
  Home, in Lab Studio and in Calculator; Back closes it (the app below keeps its state), a tap outside and
  *Sbalit* close it. On Lab Home the old tap target below the status bar is gone while the service runs.
  Each control on the phone: volume + / − (music 8 → 9 → 8), sound ↔ vibrate (VIBRATE → NORMAL → VIBRATE), torch on/off,
  play/pause (a media session went to playing and back to paused), alarm row → Clock, Wi-Fi panel, Internet
  dialog, Bluetooth, sound and display settings, notifications and quick settings shade, screenshot (the menu
  is not in the picture; test file deleted), lock (phone went to doze with lockscreen).
  Studio → PULSE → *PULSE všude* opens the system accessibility list. The first build opened the per-service
  details page instead, which needs the privileged `OPEN_ACCESSIBILITY_DETAILS_SETTINGS` permission and crashed Lab; fixed.
- PULSE menu header (3c37b28): `:app:testDebugUnitTest :app:installDebug` PASS (34 tests, new `menuStartsBelowStatusBarAndLens`).
  The sheet now starts below the status bar; a ring around the camera and a thin thread join it to the lens.
  Checked in Calculator: the clock, notification and system icons stay visible, Back closes the menu.
  `uiautomator dump --windows` does not list accessibility overlays, so the menu nodes could not be read from
  adb; `dumpsys accessibility` does list the window as `TYPE_ACCESSIBILITY_OVERLAY`.
- Owner checked by hand (reported "all OK"): PULSE menu previous / next track with music playing, rotation to
  landscape with the menu open, TalkBack in the menu and on the Studio sliders.
- Studio sliders (0ed5e3d): `:app:testDebugUnitTest :app:installDebug` PASS (34 tests). Dragging the opacity bead
  set 70 % at x = 540 px, 55 % at 300 px and 100 % past the end (left at 100 %); dragging *Svisle* in camera
  calibration showed 59.5 dp, Back stored nothing (prefs still 206 / 26 / 8). The slider node is now a focusable
  `SeekBar`; it reads the shown value ("70 %", "26.0 dp") and steps by 5 % / 0.5 dp, instead of a percentage of
  its range (70 % on 40–100 was 50 %). TalkBack itself was not re-run on this build by Claude.
- PULSE menu strip (b05d826, owner chose a translucent strip over the thread): a translucent band as wide as the
  lens ring runs from the camera into the sheet; on Lab Home and in Calculator the clock, notification and
  system icons stay uncovered.
- D8 rest state with a real alarm (owner set 12:30, checked 11:32, AC, 90 %): on Lab Home only the disc with the
  battery ring and the alarm dot, no text, the system alarm icon uncovered; the PULSE menu reads "12:30 dnes".
- Lab is now the default HOME (`cmd package set-home-activity`, owner's choice). ARC ring 3: rings set to 3,
  Clock shortcut *Spořič obrazovky* added; releasing on it opened `deskclock/ScreensaverActivity`, Home returned
  to Lab. a735638 fills ring 3 from the centre like ring 2: the shortcut moved from the far-left slot to straight
  above LENS and was launched by sliding straight up and releasing. Left as set (3 rings, 1 shortcut).
  Its icon covers the "FLOW" ring caption while selected.

Not verified:
- A phone without a cutout.
- Performance on a release build; battery.

## 2026-10-16 — new PC setup (`claude/ink-rework` from `main` 012140f)

Verified:
- `main` = merged PR #1 (012140f). The docs commit `4ac6078` (DESIGN_HANDOFF.md) was pushed after the merge
  and was not in `main`; cherry-picked onto `claude/ink-rework`.
- New Windows PC: JDK 17.0.20.1, Android SDK with platform 37.1 + build-tools 36.0.0/37.0.0, Gradle 9.5.0 wrapper.
  `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — PASS (16/16 JVM tests).
- Phone: Nothing Phone (3) A059, Nothing OS 4.1 (B4.1-260810-1153), Android 16 / API 36, USB debugging authorised.
- Installed: `dev.intelia.launcher.debug` (old fork build) and `dev.intelia.launcher.dev` (this repo).
  **Default HOME is now `dev.intelia.launcher.dev`.**
- Installed `.dev` APK is signed by a debug key with SHA-256 `730a1bbe…59f8`; this PC's debug key is `FA:62:AA:87…5E:D5`.
  A plain `adb install -r` from this PC would fail on signature mismatch; nothing was reinstalled or uninstalled.
- Baseline on the installed `.dev` (debug build): HOME cold start `am start -W` TotalTime 678 ms; warm HOME 8 ms wait;
  PSS ≈123 MB after settling. No Intelia crashes in the crash buffer.

## 2026-10-15 — standalone foundation (`claude/standalone-foundation`)

Rewritten from scratch as a standalone repository (no Kvaesitso code). Concepts and our own
Intelia UI code (clock glyphs, LENS/ARC gesture, FLOW rules) were re-implemented on a new core.

Verified:
- `./gradlew :app:testDebugUnitTest :app:assembleDebug` — PASS (16 JVM tests: FLOW, search, settings codec, ARC gesture).
- Installed `dev.intelia.launcher.dev` on Nothing Phone (3) A059, Android 16 (API 36), via USB and started it with `am start`:
  - Home renders over the system wallpaper with drawn clock and Czech date.
  - LENS tap opens search; `map` returns Mapy and Mapy.com with icons.
  - Hold on a result → *Přidat do ARC*; hold LENS, slide up, release → Google Maps launched (top resumed activity).
  - FLOW editor created `vlak` = Mapy → Mapy.com; typing `vlak` shows the preview card; *Spustit* launched both in order (Mapy.com on top).
  - Saved FLOW survived reinstall.

Not verified / not done:
- Intelia was **not** set as the default home on the phone; HOME-button reset, Recents and rotation untested.
- Claude Design project (`Intelia Launcher.dc.html`) not yet imported — visuals are the previous Intelia direction.
- Work-profile and private-space apps, TalkBack, large font scale, light theme on device.
- No performance or battery measurements.
- Icon styles/icon packs, backup/restore, Pulse — not ported.

# S3 — PULSE UX: review pack

Branch `feat/control-system-v1`, after S2 (`eeddc53`). Device: Nothing Phone (3), Lab build (`install -r`),
PULSE mode Všude with the service on, music display on, Spotify playing during the whole test (never paused).

Screenshots (local, not committed — they show the owner's status bar and music): `docs/screens/s3/`.

| # | File | What it shows |
|---|------|---------------|
| 1 | `01-home-music.png` | Home with the compact music capsule at the camera (bars · lens · artwork) |
| 2 | `02-glance-music.png` | First build of the new glance (card still slightly see-through — fixed in 9) |
| 3 | `03-studio-hidden.png` | Studio › PULSE, preview **Skrytá**: top of the real Home, dashed 48 dp touch target |
| 4 | `04-studio-music.png` | Preview **Hudba**: the real `MusicCompact` at the real lens |
| 5 | `05-studio-expanded.png` | Preview **Rozbalená**: the real glance card with a sample song |
| 6 | `06-studio-arc.png` | Preview **Global ARC**: the owner's own set, hint on its own plate under the slots |
| 7 | `07-calibration.png` | Calibration: lens (unchanged) + new **Dotyková plocha** section, target drawn dashed at the camera |
| 8 | `08-calibration-test.png` | After the test taps: „Test: 3 na ploše · 1 vedle“ |
| 9 | `09-glance-music-solid.png` | Final glance with music: solid card, song only |

## 1. Touch target (hitbox), separate from the capsule

**Before:** Home's target was 64 dp wide and grew to the whole music capsule; the service's overlay was a square
of lens radius + 10 dp (≈ 36 dp on Phone 3). Both depended on what PULSE drew.

**Now:**
- One pure function `pulseHitbox()` (`ui/PulseShape.kt`) for Home and the service: a square of
  `PulseHit.size` (default **48 dp**) centred on the lens, moved by the owner's `dx` / `dy`, kept on screen.
  It never follows the rest disc, the music artwork or the status icons beside the camera.
- Home only: the status bar window swallows touches inside it, so Home's target also reaches 24 dp under the bar
  (`REACH_BELOW`); the service overlay sits above the status bar and is exactly the square.
- New setting `pulseHit` (`size` 40–72, `dx` ±48, `dy` −16…+40 dp), bounded in `sanitized()` (the single gate).
- The service now re-places the overlay at once when the lens calibration or the target changes (before, only
  the mode / music switches moved it).
- **Calibration** (Studio › PULSE › Pokročilé › Poloha kamery): a new section *PULSE · dotyková plocha* — size,
  horizontal, vertical; the target is drawn dashed at the camera. **Test**: while the screen is open the real PULSE
  overlay takes the draft target and only counts touches (nothing runs, nothing is saved until Uložit); taps beside
  the target are counted by the screen. Back = nothing saved, the overlay returns to the saved target.

Verified on the phone: overlay frame `[476,5][602,131]` px = 126 px = 48 dp, centred on the lens (68 px).
Taps at (485,120) and (595,15) open PULSE; (640,75) — beside, towards the status icons — and (540,150) — below —
do not. Calibration test counted 3 on target / 1 beside for 3 camera taps and 1 tap elsewhere.

## 2. Expanded PULSE = minimal context panel („glance“)

The old black dashboard (player + alarm row + battery row + torch row + volume + ringer + 5 tiles + 5 links +
„Sbalit“) is gone on Home and above apps. One shared `PulseGlance` (`ui/PulseGlance.kt`):

- **Music playing → only the song**: artwork, title, artist (tap = opens the player app), timeline, ⏮ ⏯ ⏭.
  Nothing else. Controls exist only when the session supports them (unchanged rule).
- **No music → one row of what fits now** (`glanceItems()`): Svítilna (if there is a flash), Budík (shows the time of
  the next alarm, opens alarms), Baterie only while charging, and above apps also Oznámení and Snímek.
- Grows from the lens below the status bar (status icons stay visible), a quiet stem joins it to the camera.
- **Quiet close**: tap beside, Back, flick up on the card, or the small handle at its foot (48 dp target, TalkBack
  label „Zavřít“). No „Sbalit“ button.
- The card is always solid; the opacity setting tunes only the capsule (a see-through card let the date show
  through the text — seen on the phone, fixed).

Verified on the phone with Spotify playing: tap on the camera opens the song card (4/4 taps), tap beside closes,
flick up closes, music kept playing throughout. **Not verified:** the no-music row (music was not stopped on
purpose), open-player tap, torch/alarm/notifications/screenshot buttons, TalkBack.

## 3. Faithful previews in Studio › PULSE

The preview no longer draws sketches. It lays out the top 340 dp of the real screen and scales it to the card:
the same `HomeMiniature` as Home (clock, date, wallpaper show through) and the real PULSE pieces at the real lens:

- **Skrytá** — at rest PULSE is invisible; the dashed square is the touch target.
- **Hudba** — the real compact music capsule (sample song, Intelia icon as artwork).
- **Rozbalená** — the real glance card (song when music display is on, else the action row).
- **Global ARC** — the real `GlobalArc` with the set bound to the hold gesture (or the first set).

A cover takes every touch, so a preview button can never reach the real player. The bottom fades out instead of
cutting the clock (fade added after the screenshots; not re-checked on the phone).

## 4. Global ARC hint over the clock

„Návrat ke kameře gesto zruší“ (and the release label) used to sit 64 dp under the bow's middle, on top of Home's
clock. It now hangs under the **lowest** slot on its own dark plate (white text), so it never prints over the clock
or an app. Seen in the preview (6); live Global ARC on Home/over apps not re-tested — its gesture is the owner's
and I did not trigger it.

## Unchanged on purpose
Owner bindings (Podržení → Svítilna, Dvojí klepnutí → Spotify) — not touched, not triggered. PULSE gestures,
recognizer, Global ARC geometry, music capsule, rest capsule.

## Commands
- `./gradlew -q -Dorg.gradle.jvmargs=-Xmx2g :app:testDebugUnitTest :app:assembleLab` — PASS, 217 tests, 0 failures,
  no warnings (new: glance items ×2, hitbox ×4, bounds ×1; removed: menu tiles/links, old lens target, ringer, volume).
- `adb install -r …/app-lab.apk` — Success (×3).

## Open questions for review
1. Default target 48 dp and Home's extra 24 dp under the status bar — OK?
2. Music glance: is „artwork/title opens the player“ the one extra you want, or something else (volume, like)?
3. No-music row: Svítilna · Budík · (Baterie) · Oznámení · Snímek — keep, cut or reorder?
4. Removed from the old panel: volume ± / ringer, Wi-Fi, Internet, Bluetooth, Zvuk, Displej links, Zámek,
   Rychlé nastavení. Anything to bring back?
5. Preview stage height 340 dp — enough, or should it show the whole clock?

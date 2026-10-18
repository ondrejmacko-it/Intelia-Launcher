package dev.intelia.launcher.ui

import android.os.Build
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import dev.intelia.launcher.pulse.NowPlaying
import dev.intelia.launcher.pulse.Playing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.intelia.launcher.R
import dev.intelia.launcher.action.Action
import dev.intelia.launcher.action.Controls
import dev.intelia.launcher.action.Gesture
import dev.intelia.launcher.action.Trigger
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.pulseMode
import dev.intelia.launcher.pulse.MediaWatch
import dev.intelia.launcher.pulse.PulseMode

/** What [PulsePreview] shows; a drawing of each state, never the live capsule, so nothing here can run. */
private enum class PulseLook { Rest, Music, Expanded, Arc }

/**
 * Studio › PULSE (Veil 3.0): an interactive preview of the capsule's states, then Obsah a události, Gesta, Vzhled,
 * Chování and Pokročilé on the shared kit. Settings only; PULSE 2.0's own logic stays in [Pulse] and the service.
 */
@Composable
internal fun PulsePage(model: HomeViewModel, settings: LauncherSettings, set: SettingsEdit, onLens: () -> Unit, onControls: () -> Unit) {
    val context = LocalContext.current
    val service by PulseAccessibilityService.running.collectAsStateWithLifecycle()
    var access by remember { mutableStateOf(MediaWatch.allowed(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { access = MediaWatch.allowed(context) }
    val mode = settings.pulseMode()
    PulsePreview(model, settings)
    if (settings.pulse) {
        val events = listOf(settings.pulseEcho, settings.pulseCharging, settings.pulseAlarm).count { it }
        VeilGroup(stringResource(R.string.pulse_group_content)) {
            SwitchRow(stringResource(R.string.pulse_music), settings.pulseMusic, { v ->
                set { it.copy(pulseMusic = v) }
                if (v && !access) MediaWatch.openSettings(context)
            }, detail = if (settings.pulseMusic && !access) stringResource(R.string.pulse_music_needs_access) else null,
                glyph = Glyph.Play, anchor = StudioTarget.PulseMusic.anchor)
            if (settings.pulseMusic && !access) SettingRow(stringResource(R.string.pulse_music_open_settings), null, { MediaWatch.openSettings(context) })
            SwitchRow(stringResource(R.string.pulse_echo), settings.pulseEcho, { v -> set { it.copy(pulseEcho = v) } },
                glyph = Glyph.Wave, anchor = StudioTarget.PulseEvents.anchor)
            SwitchRow(stringResource(R.string.pulse_charging), settings.pulseCharging, { v -> set { it.copy(pulseCharging = v) } }, glyph = Glyph.Bolt)
            SwitchRow(stringResource(R.string.pulse_alarm), settings.pulseAlarm, { v -> set { it.copy(pulseAlarm = v) } },
                detail = stringResource(R.string.pulse_events_count, events), glyph = Glyph.Bell)
            if (settings.pulseAlarm) ChoiceSetting(stringResource(R.string.pulse_alarm_hours),
                listOf(3 to "3 h", 6 to "6 h", 12 to "12 h", 24 to "24 h"), settings.pulseAlarmHours, { v -> set { it.copy(pulseAlarmHours = v) } })
        }
        VeilGroup(stringResource(R.string.pulse_group_gestures)) {
            SettingRow(stringResource(R.string.pulse_gestures), pulseGestureSummary(model, settings.controls), onControls, glyph = Glyph.Touch)
            SwitchRow(stringResource(R.string.pulse_tap_player), settings.pulseTapPlayer, { v -> set { it.copy(pulseTapPlayer = v) } },
                detail = stringResource(R.string.pulse_tap_player_detail), glyph = Glyph.Play)
        }
        VeilGroup(stringResource(R.string.pulse_group_look)) {
            SettingBlock(StudioTarget.PulseLook.anchor) {
                SliderRow(stringResource(R.string.pulse_opacity), settings.pulseOpacity.toFloat(), LauncherSettings.MIN_PULSE_OPACITY.toFloat(),
                    100f, 5f, "${settings.pulseOpacity} %") { v -> set { it.copy(pulseOpacity = v.toInt()) } }
            }
        }
    }
    VeilGroup(stringResource(R.string.pulse_group_behaviour)) {
        // M3.1: one choice for where PULSE lives; Everywhere still needs the service the owner switches on in Android.
        ChoiceSetting(stringResource(R.string.pulse_mode), listOf(PulseMode.Off to stringResource(R.string.off),
            PulseMode.Home to stringResource(R.string.pulse_home_only), PulseMode.Everywhere to stringResource(R.string.pulse_everywhere_short)), mode,
            { m ->
                set { it.copy(pulse = m != PulseMode.Off, pulseEverywhere = if (m == PulseMode.Off) it.pulseEverywhere else m == PulseMode.Everywhere) }
                if (m == PulseMode.Everywhere && !service) PulseAccessibilityService.openSettings(context)
            }, detail = if (mode == PulseMode.Everywhere && !service) stringResource(R.string.pulse_mode_needs_service) else null,
            anchor = StudioTarget.Pulse.anchor)
    }
    if (settings.pulse) VeilGroup(stringResource(R.string.pulse_group_advanced)) {
        SettingRow(stringResource(R.string.pulse_lens), stringResource(R.string.pulse_lens_detail), onLens, glyph = Glyph.Pulse,
            anchor = StudioTarget.PulseLens.anchor)
        SettingRow(stringResource(R.string.permission_pulse), stringResource(if (service) R.string.permission_on else R.string.permission_pulse_off),
            { PulseAccessibilityService.openSettings(context) }, glyph = Glyph.Shield)
    }
}

/** "Klepnutí: menu · Podržení: Global ARC · Hlavní · +2": what the capsule's gestures do, read from the bindings. */
@Composable
private fun pulseGestureSummary(model: HomeViewModel, controls: Controls): String {
    @Composable
    fun name(a: Action): String = when (a) {
        is Action.Arc -> arcActionLabel(controls, a)
        is Action.App -> model.entry(a.component)?.label ?: stringResource(R.string.arc_slot_unavailable)
        is Action.Flow -> model.settings.value.aliases.firstOrNull { it.id == a.id }?.name ?: stringResource(R.string.arc_slot_unavailable)
        is Action.Shortcut -> stringResource(R.string.arc_ring_shortcuts)
        else -> systemActionLabel(a)
    }
    val tap = controls.bound(Trigger.Pulse(Gesture.Tap))
    val hold = controls.bound(Trigger.Pulse(Gesture.Hold))
    val others = controls.bindings.count { (it.trigger as? Trigger.Pulse)?.gesture.let { g -> g != null && g != Gesture.Tap && g != Gesture.Hold } }
    val parts = listOf(
        stringResource(R.string.pulse_gesture_tap) + ": " + (tap?.let { name(it) } ?: stringResource(R.string.pulse_gesture_menu)),
        stringResource(R.string.pulse_gesture_hold) + ": " + (hold?.let { name(it) } ?: stringResource(R.string.pulse_gesture_none)),
    ) + listOfNotNull(if (others > 0) "+$others" else null)
    return parts.joinToString(" · ")
}

/**
 * S3: the capsule's states as the phone shows them, not a sketch: the top of Home (the same [HomeMiniature] as Home)
 * laid out at the screen's real size and scaled down, with the real PULSE pieces on top at the real lens: Skrytá (the
 * invisible rest, its touch target dashed), Hudba ([MusicCompact]), Rozbalená ([GlanceCard]) and Global ARC
 * ([GlobalArc] with the owner's own set). Sample music only; a cover takes every touch, so nothing here plays or runs.
 */
@Composable
private fun PulsePreview(model: HomeViewModel, settings: LauncherSettings) {
    val ink = LocalInk.current
    val context = LocalContext.current
    val density = LocalDensity.current
    var look by rememberSaveable { mutableStateOf(PulseLook.Rest) }
    val now by rememberNow()
    val arc by model.arc.collectAsStateWithLifecycle()
    val label = stringResource(R.string.pulse_preview_label)
    val note = stringResource(when {
        !settings.pulse -> R.string.pulse_preview_off
        look == PulseLook.Rest -> R.string.pulse_preview_rest_note
        look == PulseLook.Music -> R.string.pulse_preview_music_note
        look == PulseLook.Expanded -> if (settings.pulseMusic) R.string.pulse_preview_expanded_note else R.string.pulse_preview_expanded_plain
        else -> R.string.pulse_preview_arc_note
    })
    val sample = remember(context) {
        Playing(NowPlaying(context.packageName, context.getString(R.string.pulse_sample_title), context.getString(R.string.pulse_sample_artist),
            NowPlaying.STATE_PLAYING, NowPlaying.ACTION_SKIP_TO_PREVIOUS or NowPlaying.ACTION_SKIP_TO_NEXT or NowPlaying.ACTION_PLAY_PAUSE,
            215_000, 81_000, SystemClock.elapsedRealtime(), 0f), null)
    }
    val lens = rememberLens(settings.pulseLens)
    val statusBar = with(density) { WindowInsets.statusBarsIgnoringVisibility.getTop(this).toDp().value }
    val shape = RoundedCornerShape(22.dp)
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val screen = LocalConfiguration.current
            val screenW = screen.screenWidthDp.toFloat()
            val screenH = (screen.screenHeightDp + statusBar).coerceAtLeast(PREVIEW_STAGE_DP)
            val k = (maxWidth.value / screenW).coerceAtMost(1f)
            Box(Modifier.size((screenW * k).dp, (PREVIEW_STAGE_DP * k).dp).clip(shape).background(Color.Black.copy(alpha = .28f))
                .border(1.dp, ink.accent.copy(alpha = .30f), shape).clearAndSetSemantics { contentDescription = "$label. $note" }) {
                Box(Modifier.stage(k, screenW, screenH)) {
                    val home = settings.home.activeLayout()
                    HomeMiniature(settings, home, now, model::entry, 1f, Modifier.fillMaxSize())
                    val hx = with(density) { lens.x.toDp().value }
                    val hy = with(density) { lens.y.toDp().value }
                    val hr = with(density) { lens.radius.toDp().value }
                    if (settings.pulse) when (look) {
                        PulseLook.Rest -> Canvas(Modifier.fillMaxSize()) {
                            val box = pulseHitbox(hx, hy, settings.pulseHit, screenW)
                            drawRoundRect(ink.accent.copy(alpha = .9f), Offset(box.left.dp.toPx(), box.top.dp.toPx()),
                                Size(box.width.dp.toPx(), box.height.dp.toPx()), CornerRadius(12.dp.toPx()),
                                style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))))
                        }
                        PulseLook.Music -> {
                            val mc = musicCapsule(hx, hy, hr, statusBar)
                            Box(Modifier.offset(mc.left.dp, mc.top.dp)) { MusicCompact(sample, mc, lens.radius, settings.pulseOpacity, null) }
                        }
                        PulseLook.Expanded -> {
                            val state = GlanceState(if (settings.pulseMusic) sample else null,
                                glanceItems(settings.pulseMusic, torch = true, charging = false, global = settings.pulseEverywhere, sdk = Build.VERSION.SDK_INT),
                                alarm = "6:30")
                            val (left, width) = menuLeftAndWidth(screenW, hx, 12f, glanceWidth(state).value)
                            val top = menuTop(statusBar, hy, hr, 4f, 8f)
                            Canvas(Modifier.fillMaxSize()) {
                                val half = (hr + 4f).dp.toPx()
                                drawRoundRect(ink.night.copy(alpha = .5f), Offset(hx.dp.toPx() - half, hy.dp.toPx() - half),
                                    Size(2f * half, (top + 24f).dp.toPx() - (hy.dp.toPx() - half)), CornerRadius(half))
                            }
                            GlanceCard(state, width.dp, {}, {}, {}, {}, Modifier.offset(left.dp, top.dp))
                        }
                        PulseLook.Arc -> {
                            val hold = settings.controls.bound(Trigger.Pulse(Gesture.Hold)) as? Action.Arc
                            val set = arc.sets.firstOrNull { s -> hold != null && s.id == settings.controls.arcSetFor(hold).id } ?: arc.sets.firstOrNull()
                            GlobalArc(set, settings.controls.globalArc.over(settings.controls.arc), Pt(hx, hy), open = true, pick = null,
                                flowSteps = { emptyList() })
                        }
                    }
                    // The camera itself, so the picture reads like the phone.
                    Canvas(Modifier.fillMaxSize()) { drawCircle(Color.Black, lens.radius, Offset(lens.x, lens.y)) }
                }
                // The stage ends softly instead of cutting Home's clock in half.
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(56.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF16141C)))))
                // Nothing in the preview takes a touch: its music buttons must never reach the real player.
                Box(Modifier.matchParentSize().pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false).consume() } })
            }
        }
        Text(note, Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp), style = InkText.small, color = ink.muted)
        if (settings.pulse) VeilChoice(listOf(PulseLook.Rest to stringResource(R.string.pulse_preview_rest),
            PulseLook.Music to stringResource(R.string.pulse_preview_music), PulseLook.Expanded to stringResource(R.string.pulse_preview_expanded),
            PulseLook.Arc to stringResource(R.string.global_arc)), look, { look = it }, Modifier.padding(top = 8.dp))
    }
}

/** Lays the content out at [w] × [h] dp (the real screen) and draws it scaled by [k] from the top start. */
private fun Modifier.stage(k: Float, w: Float, h: Float) = layout { m, c ->
    val p = m.measure(Constraints.fixed(w.dp.roundToPx(), h.dp.roundToPx()))
    layout(c.maxWidth, c.maxHeight) { p.placeWithLayer(0, 0) { scaleX = k; scaleY = k; transformOrigin = TransformOrigin(0f, 0f) } }
}

/** How much of the screen's top the preview shows, dp: the camera, the expanded glance and Global ARC with its label. */
private const val PREVIEW_STAGE_DP = 340f

package dev.intelia.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.Dp
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
import dev.intelia.launcher.pulse.PulseHealth
import dev.intelia.launcher.pulse.PulseMode

/** What [PulseStage] shows; a drawing of each state, never the live capsule, so nothing here can run. */
internal enum class PulseLook { Rest, Music, Expanded, Arc }

/**
 * Studio › PULSE (S3.1): the preview with the mode, then Obsah a rychlé akce, Gesta, Vzhled, Události and, on request,
 * Pokročilé, all on the shared kit with a short summary of what is on. Settings only; PULSE's own logic stays in
 * [Pulse] and the service.
 */
@Composable
internal fun PulsePage(model: HomeViewModel, settings: LauncherSettings, set: SettingsEdit, look: PulseLook, onLook: (PulseLook) -> Unit,
    onLens: () -> Unit, onControls: () -> Unit) {
    val context = LocalContext.current
    val service by PulseAccessibilityService.running.collectAsStateWithLifecycle()
    val health by PulseAccessibilityService.health.collectAsStateWithLifecycle()
    var access by remember { mutableStateOf(MediaWatch.allowed(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { access = MediaWatch.allowed(context) }
    val mode = settings.pulseMode()
    var quickOpen by rememberSaveable { mutableStateOf(false) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    PulseLookPicker(settings, look, onLook)
    VeilGroup(stringResource(R.string.pulse_group_mode)) {
        // M3.1: one choice for where PULSE lives; Everywhere still needs the service the owner switches on in Android.
        ChoiceSetting(stringResource(R.string.pulse_mode), listOf(PulseMode.Off to stringResource(R.string.off),
            PulseMode.Home to stringResource(R.string.pulse_home_only), PulseMode.Everywhere to stringResource(R.string.pulse_everywhere_short)), mode,
            { m ->
                set { it.copy(pulse = m != PulseMode.Off, pulseEverywhere = if (m == PulseMode.Off) it.pulseEverywhere else m == PulseMode.Everywhere) }
                if (m == PulseMode.Everywhere && !service) PulseAccessibilityService.openSettings(context)
            }, detail = when {
                mode != PulseMode.Everywhere -> null
                !service -> stringResource(R.string.pulse_mode_needs_service)
                // P0: the real overlay state, not the stored switch.
                health == PulseHealth.Error -> stringResource(R.string.pulse_overlay_error)
                else -> null
            },
            anchor = StudioTarget.Pulse.anchor)
    }
    if (!settings.pulse) return
    VeilGroup(stringResource(R.string.pulse_group_content)) {
        SwitchRow(stringResource(R.string.pulse_music), settings.pulseMusic, { v ->
            set { it.copy(pulseMusic = v) }
            if (v && !access) MediaWatch.openSettings(context)
        }, detail = if (settings.pulseMusic && !access) stringResource(R.string.pulse_music_needs_access) else stringResource(R.string.pulse_music_detail),
            glyph = Glyph.Play, anchor = StudioTarget.PulseMusic.anchor)
        if (settings.pulseMusic && !access) SettingRow(stringResource(R.string.pulse_music_open_settings), null, { MediaWatch.openSettings(context) })
        SettingRow(stringResource(R.string.pulse_quick), quickSummary(settings.controls, model), { quickOpen = !quickOpen }, glyph = Glyph.Bolt,
            trailing = { Text(if (quickOpen) "⌃" else "⌄", style = InkText.body, color = LocalInk.current.muted) })
        if (quickOpen) QuickActionsEditor(model, settings)
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
    val events = listOf(settings.pulseEcho, settings.pulseCharging, settings.pulseAlarm).count { it }
    VeilGroup(stringResource(R.string.pulse_group_events) + " · " + stringResource(R.string.pulse_events_short, events)) {
        SwitchRow(stringResource(R.string.pulse_echo), settings.pulseEcho, { v -> set { it.copy(pulseEcho = v) } },
            glyph = Glyph.Wave, anchor = StudioTarget.PulseEvents.anchor)
        SwitchRow(stringResource(R.string.pulse_charging), settings.pulseCharging, { v -> set { it.copy(pulseCharging = v) } }, glyph = Glyph.Bolt)
        SwitchRow(stringResource(R.string.pulse_alarm), settings.pulseAlarm, { v -> set { it.copy(pulseAlarm = v) } }, glyph = Glyph.Bell)
        if (settings.pulseAlarm) ChoiceSetting(stringResource(R.string.pulse_alarm_hours),
            listOf(3 to "3 h", 6 to "6 h", 12 to "12 h", 24 to "24 h"), settings.pulseAlarmHours, { v -> set { it.copy(pulseAlarmHours = v) } })
    }
    VeilGroup(stringResource(R.string.pulse_group_advanced)) {
        val touch = stringResource(R.string.pulse_touch_summary, settings.pulseHit.size.toInt(),
            stringResource(if (settings.pulseLens == null) R.string.pulse_lens_auto_short else R.string.pulse_lens_custom))
        SettingRow(stringResource(R.string.pulse_lens), touch, onLens, glyph = Glyph.Pulse, anchor = StudioTarget.PulseLens.anchor)
        if (advanced || !service) SettingRow(stringResource(R.string.permission_pulse),
            stringResource(if (service) R.string.permission_on else R.string.permission_pulse_off),
            { PulseAccessibilityService.openSettings(context) }, glyph = Glyph.Shield)
        if (!advanced && service) SettingRow(stringResource(R.string.show_advanced), null, { advanced = true })
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

/** What the state shown in [PulseStage] is, said under the picture and to TalkBack. */
@Composable
private fun pulseNote(settings: LauncherSettings, look: PulseLook): String = stringResource(when {
    !settings.pulse -> R.string.pulse_preview_off
    look == PulseLook.Rest -> R.string.pulse_preview_rest_note
    look == PulseLook.Music -> R.string.pulse_preview_music_note
    look == PulseLook.Expanded -> if (settings.pulseMusic) R.string.pulse_preview_expanded_note else R.string.pulse_preview_expanded_plain
    else -> R.string.pulse_preview_arc_note
})

/** The state chips at the top of the PULSE page; the picture itself is the page's preview in the host (M1). */
@Composable
private fun PulseLookPicker(settings: LauncherSettings, look: PulseLook, onLook: (PulseLook) -> Unit) {
    val ink = LocalInk.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(pulseNote(settings, look), Modifier.padding(horizontal = 6.dp), style = InkText.small, color = ink.muted)
        if (settings.pulse) VeilChoice(listOf(PulseLook.Rest to stringResource(R.string.pulse_preview_rest),
            PulseLook.Music to stringResource(R.string.pulse_preview_music), PulseLook.Expanded to stringResource(R.string.pulse_preview_expanded),
            PulseLook.Arc to stringResource(R.string.global_arc)), look, onLook, Modifier.padding(top = 8.dp))
    }
}

/**
 * S3.1: the capsule's states as the phone shows them: the top of Home (the same [HomeMiniature] as Home) laid out at
 * the screen's real size and scaled, with the real PULSE pieces at the real lens. Each state shows only the strip it
 * needs (the camera's surroundings at rest, down to the card or the lowest ARC slot otherwise) and ends in a soft
 * fade, never on half a clock. Sample music and alarm are marked as such; Global ARC uses the owner's own geometry and
 * set, without its cancel hint. A cover takes every touch, so nothing here plays or runs.
 */
@Composable
internal fun PulseStage(model: HomeViewModel, settings: LauncherSettings, look: PulseLook, room: Dp, screenWidth: Dp, modifier: Modifier = Modifier) {
    val ink = LocalInk.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val now by rememberNow()
    val arc by model.arc.collectAsStateWithLifecycle()
    val apps by model.visibleApps.collectAsStateWithLifecycle()
    val service by PulseAccessibilityService.running.collectAsStateWithLifecycle()
    val label = stringResource(R.string.pulse_preview_label)
    val note = pulseNote(settings, look)
    val sample = remember(context) {
        Playing(NowPlaying(context.packageName, context.getString(R.string.pulse_sample_title), context.getString(R.string.pulse_sample_artist),
            NowPlaying.STATE_PLAYING, NowPlaying.ACTION_SKIP_TO_PREVIOUS or NowPlaying.ACTION_SKIP_TO_NEXT or NowPlaying.ACTION_PLAY_PAUSE,
            215_000, 81_000, SystemClock.elapsedRealtime(), 0f), null)
    }
    val lens = rememberLens(settings.pulseLens)
    val statusBar = with(density) { WindowInsets.statusBarsIgnoringVisibility.getTop(this).toDp().value }
    val shape = RoundedCornerShape(22.dp)
    val hx = with(density) { lens.x.toDp().value }
    val hy = with(density) { lens.y.toDp().value }
    val hr = with(density) { lens.radius.toDp().value }
    val screen = LocalConfiguration.current
    val screenW = screen.screenWidthDp.toFloat()
    val screenH = screen.screenHeightDp + statusBar
    val arcLook = settings.controls.globalArc.over(settings.controls.arc)
    val quick = remember(settings.controls, apps, settings.aliases, service) { quickEntries(settings.controls, apps, settings.aliases, service) }
    val glance = GlanceState(if (settings.pulseMusic) sample else null, quick, alarm = "6:30")
    val glanceTop = menuTop(statusBar, hy, hr, 4f, 8f)
    // How much of the screen's top each state needs, dp, plus room for the fade.
    val stageH = when {
        !settings.pulse -> hy + hr + 56f
        look == PulseLook.Rest -> pulseHitbox(hx, hy, settings.pulseHit, screenW).let { it.top + it.height } + 48f
        look == PulseLook.Music -> musicCapsule(hx, hy, hr, statusBar).let { it.top + it.height } + 48f
        look == PulseLook.Expanded -> glanceTop + (if (glance.music != null) GLANCE_MUSIC_DP else GLANCE_ROW_DP) + 16f
        else -> GlobalArcGeometry.bow(arcLook, Pt(hx, hy), screenW, screenH).let { bow ->
            (0 until bow.count).maxOfOrNull { GlobalArcGeometry.slot(bow, Pt(hx, hy), it).y } ?: (hy + bow.lift)
        } + 64f
    }.coerceIn(PREVIEW_MIN_DP, screenH)
    // How wide a window around the camera each state needs, dp: the capsule is zoomed in, not shown at full width.
    val stageW = when {
        !settings.pulse -> hr * 2f + 160f
        look == PulseLook.Rest -> pulseHitbox(hx, hy, settings.pulseHit, screenW).width + 120f
        look == PulseLook.Music -> musicCapsule(hx, hy, hr, statusBar).width + 96f
        look == PulseLook.Expanded -> glanceWidth(glance).value + 40f
        else -> screenW
    }.coerceIn(minOf(200f, screenW), screenW)
    val shownH by animateFloatAsState(stageH, tween(motionMs(220)), label = "pulse-preview-height")
    val shownW by animateFloatAsState(stageW, tween(motionMs(220)), label = "pulse-preview-width")
    val left = (hx - shownW / 2f).coerceIn(0f, (screenW - shownW).coerceAtLeast(0f))
    val sampled = settings.pulse && (look == PulseLook.Music || (look == PulseLook.Expanded))
    // The window fills the room above the sheet and the width less a margin, never past the phone's real size.
    val k = minOf((screenWidth.value - 16f) / shownW, room.value / shownH, 1f).coerceAtLeast(.05f)
    PreviewCover("$label. $note", modifier.size((shownW * k).dp, (shownH * k).dp).clip(shape).background(Color.Black.copy(alpha = .28f))
        .border(1.dp, ink.accent.copy(alpha = .30f), shape)) {
        Box(Modifier.stage(k, screenW, screenH, left)) {
            val home = settings.home.activeLayout()
            HomeMiniature(settings, home, now, model::entry, 1f, Modifier.fillMaxSize())
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
                    val (left, width) = menuLeftAndWidth(screenW, hx, 12f, glanceWidth(glance).value)
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .22f)))
                    Canvas(Modifier.fillMaxSize()) {
                        val half = (hr + 4f).dp.toPx()
                        drawCircle(ink.accent.copy(alpha = .55f), half, Offset(hx.dp.toPx(), hy.dp.toPx()), style = Stroke(1.dp.toPx()))
                    }
                    GlanceCard(glance, width.dp, {}, {}, {}, {}, Modifier.offset(left.dp, glanceTop.dp))
                }
                PulseLook.Arc -> {
                    val hold = settings.controls.bound(Trigger.Pulse(Gesture.Hold)) as? Action.Arc
                    val set = arc.sets.firstOrNull { s -> hold != null && s.id == settings.controls.arcSetFor(hold).id } ?: arc.sets.firstOrNull()
                    GlobalArc(set, arcLook, Pt(hx, hy), open = true, pick = null, flowSteps = { emptyList() }, explain = false)
                }
            }
            // The camera itself, so the picture reads like the phone.
            Canvas(Modifier.fillMaxSize()) { drawCircle(Color.Black, lens.radius, Offset(lens.x, lens.y)) }
        }
        // The strip ends softly instead of cutting Home in half.
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(32.dp)
            .background(Brush.verticalGradient(listOf(Color.Transparent, ink.paper.copy(alpha = .92f)))))
        if (sampled) SampleBadge(Modifier.align(Alignment.BottomEnd).padding(8.dp))
    }
}

/** Lays the content out at [w] × [h] dp (the real screen) and draws it scaled by [k] from the top start, [left] dp in. */
private fun Modifier.stage(k: Float, w: Float, h: Float, left: Float = 0f) = layout { m, c ->
    val p = m.measure(Constraints.fixed(w.dp.roundToPx(), h.dp.roundToPx()))
    layout(c.maxWidth, c.maxHeight) {
        p.placeWithLayer(0, 0) { scaleX = k; scaleY = k; transformOrigin = TransformOrigin(0f, 0f); translationX = -left.dp.toPx() * k }
    }
}

/** The shortest strip the preview shows, dp, and the glance card's height with the song or with a row of actions. */
private const val PREVIEW_MIN_DP = 110f
private const val GLANCE_MUSIC_DP = 190f
private const val GLANCE_ROW_DP = 116f

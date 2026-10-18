package dev.intelia.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    PulsePreview(settings)
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
 * The capsule's states drawn at the camera, switched by the tabs below: rest, compact music, the expanded panel and
 * Global ARC (with the real slot count and opening angle). A drawing, so nothing plays or runs.
 */
@Composable
private fun PulsePreview(settings: LauncherSettings) {
    val ink = LocalInk.current
    var look by rememberSaveable { mutableStateOf(PulseLook.Rest) }
    val opacity = settings.pulseOpacity / 100f
    val accent = ink.accent
    val arc = settings.controls.globalArc.over(settings.controls.arc)
    val screen = androidx.compose.ui.platform.LocalConfiguration.current
    val screenW = screen.screenWidthDp.toFloat()
    // Global ARC's real bow (ARC V2 under the camera) in screen dp, camera at the preview's own camera height.
    val camera = Pt(screenW / 2f, PREVIEW_CAMERA_DP)
    val slots = remember(arc, screenW) {
        val bow = GlobalArcGeometry.bow(arc, camera, screenW, screen.screenHeightDp.toFloat())
        List(bow.count) { GlobalArcGeometry.slot(bow, camera, it) }
    }
    val label = stringResource(R.string.pulse_preview_label)
    val note = stringResource(when {
        !settings.pulse -> R.string.pulse_preview_off
        look == PulseLook.Rest -> R.string.pulse_preview_rest_note
        look == PulseLook.Music -> R.string.pulse_preview_music_note
        look == PulseLook.Expanded -> R.string.pulse_preview_expanded_note
        else -> R.string.pulse_preview_arc_note
    })
    val shape = RoundedCornerShape(22.dp)
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Canvas(Modifier.fillMaxWidth().height(188.dp).clip(shape).background(Color(0xFF0B0B12)).border(1.dp, accent.copy(alpha = .30f), shape)
            .clearAndSetSemantics { contentDescription = "$label. $note" }) {
            val cx = size.width / 2f
            val cy = PREVIEW_CAMERA_DP.dp.toPx()
            val hole = 9.dp.toPx()
            val light = Color.White
            val fit = (size.width / screenW.dp.toPx()).coerceAtMost(1f)
            if (settings.pulse) when (look) {
                PulseLook.Rest -> drawCircle(light.copy(alpha = .55f * opacity), hole + 6.dp.toPx(), Offset(cx, cy), style = Stroke(1.5.dp.toPx()))
                PulseLook.Music -> drawMusic(cx, cy, hole, opacity, accent)
                PulseLook.Expanded -> drawPanel(cx, cy, hole, opacity, accent)
                PulseLook.Arc -> drawArc(cx, cy, slots.map { Offset(cx + (it.x - camera.x).dp.toPx() * fit, cy + (it.y - camera.y).dp.toPx() * fit) },
                    accent, fit)
            }
            drawCircle(Color.Black, hole, Offset(cx, cy))
            drawCircle(light.copy(alpha = .18f), hole, Offset(cx, cy), style = Stroke(1.dp.toPx()))
        }
        Text(note, Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp), style = InkText.small, color = ink.muted)
        if (settings.pulse) VeilChoice(listOf(PulseLook.Rest to stringResource(R.string.pulse_preview_rest),
            PulseLook.Music to stringResource(R.string.pulse_preview_music), PulseLook.Expanded to stringResource(R.string.pulse_preview_expanded),
            PulseLook.Arc to stringResource(R.string.global_arc)), look, { look = it }, Modifier.padding(top = 8.dp))
    }
}

private fun DrawScope.drawMusic(cx: Float, cy: Float, hole: Float, opacity: Float, accent: Color) {
    val h = 2 * hole + 10.dp.toPx()
    val w = 150.dp.toPx()
    drawRoundRect(Color(0xFF1A1A22).copy(alpha = .4f + .6f * opacity), Offset(cx - w / 2f, cy - h / 2f), Size(w, h), CornerRadius(h / 2f))
    val bar = 3.dp.toPx()
    listOf(.45f, .9f, .6f, .75f).forEachIndexed { i, f ->
        val bh = (h - 12.dp.toPx()) * f
        val x = cx - w / 2f + 16.dp.toPx() + i * (bar + 3.dp.toPx())
        drawRoundRect(accent, Offset(x, cy - bh / 2f), Size(bar, bh), CornerRadius(bar / 2f))
    }
    drawCircle(accent.copy(alpha = .55f), hole, Offset(cx + w / 2f - hole - 5.dp.toPx(), cy))
}

private fun DrawScope.drawPanel(cx: Float, cy: Float, hole: Float, opacity: Float, accent: Color) {
    val left = 14.dp.toPx()
    val top = cy - hole - 6.dp.toPx()
    val w = size.width - 2 * left
    val h = size.height - top - 12.dp.toPx()
    drawRoundRect(Color(0xFF1A1A22).copy(alpha = .5f + .5f * opacity), Offset(left, top), Size(w, h), CornerRadius(22.dp.toPx()))
    val art = 52.dp.toPx()
    val y = cy + hole + 16.dp.toPx()
    drawRoundRect(accent.copy(alpha = .5f), Offset(left + 16.dp.toPx(), y), Size(art, art), CornerRadius(12.dp.toPx()))
    val tx = left + 16.dp.toPx() + art + 14.dp.toPx()
    drawRoundRect(Color.White.copy(alpha = .75f), Offset(tx, y + 8.dp.toPx()), Size(w * .42f, 7.dp.toPx()), CornerRadius(4.dp.toPx()))
    drawRoundRect(Color.White.copy(alpha = .35f), Offset(tx, y + 24.dp.toPx()), Size(w * .28f, 6.dp.toPx()), CornerRadius(3.dp.toPx()))
    val line = y + art + 16.dp.toPx()
    drawLine(Color.White.copy(alpha = .2f), Offset(left + 16.dp.toPx(), line), Offset(left + w - 16.dp.toPx(), line), 3.dp.toPx())
    drawLine(accent, Offset(left + 16.dp.toPx(), line), Offset(left + 16.dp.toPx() + (w - 32.dp.toPx()) * .38f, line), 3.dp.toPx())
    listOf(-1, 0, 1).forEach { i -> drawCircle(Color.White.copy(alpha = if (i == 0) .85f else .5f), (if (i == 0) 9 else 6).dp.toPx(),
        Offset(cx + i * 44.dp.toPx(), line + 22.dp.toPx())) }
}

/** Global ARC: the beads where the real bow puts them ([slots], priority order: 0 is the middle), a dashed bow through them. */
private fun DrawScope.drawArc(cx: Float, cy: Float, slots: List<Offset>, accent: Color, fit: Float) {
    drawRect(Color.Black.copy(alpha = .35f))
    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 6.dp.toPx()))
    slots.sortedBy { it.x }.zipWithNext { a, b -> drawLine(Color.White.copy(alpha = .35f), a, b, 1.dp.toPx(), pathEffect = dash) }
    val bead = 20.dp.toPx() * fit
    slots.forEachIndexed { i, p ->
        val chosen = i == 0
        drawCircle(if (chosen) accent else Color.White.copy(alpha = .22f), if (chosen) bead * 1.2f else bead, p)
        drawCircle(Color.White.copy(alpha = .55f), if (chosen) bead * 1.2f else bead, p, style = Stroke(1.dp.toPx()))
    }
    slots.firstOrNull()?.let { drawLine(accent.copy(alpha = .7f), Offset(cx, cy + 12.dp.toPx()), it, 1.5.dp.toPx()) }
}

/** Where the preview draws the camera, dp from its top. */
private const val PREVIEW_CAMERA_DP = 22f

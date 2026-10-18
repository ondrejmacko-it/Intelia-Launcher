package dev.intelia.launcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.intelia.launcher.R
import dev.intelia.launcher.pulse.Playing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** What the glance card shows; built live by [PulseGlance], or by Studio's preview with sample values. */
internal data class GlanceState(
    val music: Playing?,
    val items: List<GlanceItem>,
    val torch: Boolean = false,
    /** The next alarm as "6:30", null without one. */
    val alarm: String? = null,
    val battery: Battery? = null,
)

/**
 * S3 PULSE glance, the expanded PULSE on Home and above apps: a small card grown out of the lens, below the status
 * bar so the system icons stay visible. With music it is the song (artwork and title open the player, ⏮ ⏯ ⏭, the
 * timeline) and nothing more; without music one row of what is relevant now ([glanceItems]). It closes quietly: a tap
 * outside, Back, a flick up on the card or the small handle at its foot. [hole] and sizes are window px.
 */
@Composable
internal fun PulseGlance(hole: Hole, statusBar: Float, screenWidth: Float, music: Playing?, global: Boolean,
    onItem: (GlanceItem) -> Unit, onOpenPlayer: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val ink = LocalInk.current
    val cameras = remember(context) { context.getSystemService(CameraManager::class.java) }
    val torchId = remember(cameras) { cameras?.let(::torchCamera) }
    var torch by remember { mutableStateOf(false) }
    var battery by remember { mutableStateOf<Battery?>(null) }
    val alarm = remember { nextAlarm(context) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) { battery = intent.battery() }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
            ?.battery()?.let { battery = it }
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) { if (cameraId == torchId) torch = enabled }
        }
        if (torchId != null) cameras?.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
        onDispose {
            context.unregisterReceiver(receiver)
            cameras?.unregisterTorchCallback(callback)
        }
    }
    val state = GlanceState(music, glanceItems(music != null, torchId != null, battery?.charging == true, global, Build.VERSION.SDK_INT),
        torch, alarm?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("H:mm")) },
        battery)

    val margin = with(density) { 12.dp.toPx() }
    val wanted = with(density) { glanceWidth(state).toPx() }
    val (left, width) = menuLeftAndWidth(screenWidth, hole.x, margin, wanted)
    val ring = with(density) { 4.dp.toPx() }
    val top = with(density) { menuTop(statusBar, hole.y, hole.radius, ring, 8.dp.toPx()) }
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }

    Box(Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
        onClickLabel = stringResource(R.string.close), onClick = onClose)) {
        // A quiet stem from the lens into the card: PULSE grows out of the camera, the status icons stay uncovered.
        AnimatedVisibility(shown, enter = fadeIn(tween(motionMs(160)))) {
            Canvas(Modifier.fillMaxSize()) {
                val half = hole.radius + ring
                drawRoundRect(ink.night.copy(alpha = .5f), Offset(hole.x - half, hole.y - half),
                    Size(2f * half, top + 24.dp.toPx() - (hole.y - half)), CornerRadius(half))
                drawCircle(ink.onNight.copy(alpha = .4f), half, Offset(hole.x, hole.y), style = Stroke(1.dp.toPx()))
            }
        }
        AnimatedVisibility(shown, Modifier.offset { IntOffset(left.toInt(), top.toInt()) },
            enter = fadeIn(tween(motionMs(160))) + expandIn(tween(motionMs(260)), expandFrom = Alignment.TopCenter)) {
            GlanceCard(state, with(density) { width.toDp() },
                onTorch = { on ->
                    try {
                        if (torchId != null) cameras?.setTorchMode(torchId, on)
                    } catch (_: CameraAccessException) {
                    } catch (_: IllegalArgumentException) {
                    }
                },
                onItem = onItem, onOpenPlayer = { music?.let { onOpenPlayer(it.info.pkg) } }, onClose = onClose)
        }
    }
}

/** The card's width: the song needs room for its title, a row of actions only its buttons. */
internal fun glanceWidth(state: GlanceState) =
    if (state.music != null) 360.dp else (state.items.size.coerceAtLeast(2) * GLANCE_SLOT + 24).dp

private const val GLANCE_SLOT = 76

/**
 * The card itself, stateless: Studio's preview draws the very same one with sample values. Always solid: the PULSE
 * opacity setting tunes the capsule, text on a see-through card would mix with whatever lies under it.
 */
@Composable
internal fun GlanceCard(state: GlanceState, width: androidx.compose.ui.unit.Dp, onTorch: (Boolean) -> Unit,
    onItem: (GlanceItem) -> Unit, onOpenPlayer: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val ink = LocalInk.current
    val shape = RoundedCornerShape(28.dp)
    val glance = stringResource(R.string.pulse_glance)
    Column(modifier.width(width).clip(shape).background(ink.night)
        .border(1.dp, ink.onNight.copy(alpha = .10f), shape)
        // Taps inside stay inside; a flick up sends the card back into the lens.
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
        .pointerInput(Unit) {
            var pulled = 0f
            detectVerticalDragGestures(onDragStart = { pulled = 0f }, onDragEnd = { if (pulled < -36.dp.toPx()) onClose() }) { change, dy ->
                pulled += dy; change.consume()
            }
        }
        .semantics { contentDescription = glance }
        .padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
        val music = state.music
        if (music != null) MusicCard(music, onOpen = onOpenPlayer)
        else Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            state.items.forEach { item -> GlanceButton(item, state, onTorch, onItem) }
        }
        // The quiet way out: a short handle, a 48 dp target around it.
        val close = stringResource(R.string.close)
        Box(Modifier.fillMaxWidth().height(28.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
            role = Role.Button, onClickLabel = close, onClick = onClose).semantics { contentDescription = close },
            contentAlignment = Alignment.Center) {
            Box(Modifier.size(32.dp, 4.dp).clip(CircleShape).background(ink.onNight.copy(alpha = .28f)))
        }
    }
}

@Composable
private fun GlanceButton(item: GlanceItem, state: GlanceState, onTorch: (Boolean) -> Unit, onItem: (GlanceItem) -> Unit) {
    val ink = LocalInk.current
    val on = item == GlanceItem.Torch && state.torch
    val glyph = when (item) {
        GlanceItem.Torch -> Glyph.Torch
        GlanceItem.Alarm -> Glyph.Bell
        GlanceItem.Battery -> Glyph.Bolt
        GlanceItem.Notifications -> Glyph.Layers
        GlanceItem.Screenshot -> Glyph.Shot
    }
    val text = when (item) {
        GlanceItem.Torch -> stringResource(R.string.torch)
        GlanceItem.Alarm -> state.alarm ?: stringResource(R.string.pulse_alarm_short)
        GlanceItem.Battery -> state.battery?.let { "${it.percent} %" } ?: stringResource(R.string.charging)
        GlanceItem.Notifications -> stringResource(R.string.pulse_notifications)
        GlanceItem.Screenshot -> stringResource(R.string.pulse_screenshot)
    }
    val act = when (item) {
        GlanceItem.Battery -> Modifier
        GlanceItem.Torch -> Modifier.toggleable(on, role = Role.Switch) { onTorch(it) }
            .semantics { stateDescription = if (on) "on" else "off" }
        else -> Modifier.clickable(role = Role.Button) { onItem(item) }
    }
    Column(Modifier.width(GLANCE_SLOT.dp).clip(RoundedCornerShape(18.dp)).then(act).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(if (on) ink.accent else ink.onNight.copy(alpha = .08f)),
            contentAlignment = Alignment.Center) {
            HomeGlyph(glyph, null, tint = if (on) ink.night else ink.onNight, size = 20.dp)
        }
        Text(text, Modifier.padding(top = 6.dp), style = InkText.small, color = ink.onNight.copy(alpha = .78f), maxLines = 1,
            textAlign = TextAlign.Center)
    }
}

/** The playing app's launcher activity through public `LauncherApps`, or null when it has none. */
internal fun playerIntent(context: Context, pkg: String): Intent? = try {
    context.getSystemService(LauncherApps::class.java)?.getActivityList(pkg, Process.myUserHandle())?.firstOrNull()?.componentName?.let {
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(it)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    }
} catch (_: SecurityException) {
    null
}

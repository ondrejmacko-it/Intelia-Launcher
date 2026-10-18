package dev.intelia.launcher.ui

import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.draw.drawWithCache
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import dev.intelia.launcher.action.Action
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.Offset
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
    /** The owner's visible Quick Actions, resolved on this device ([quickEntries]). */
    val quick: List<QuickEntry>,
    val torch: Boolean = false,
    /** The next alarm as "6:30", null without one. */
    val alarm: String? = null,
    /** Shown first while charging, when the charging event is on. */
    val battery: Battery? = null,
)

/**
 * S3.1 PULSE glance, the expanded PULSE on Home and above apps: a small card grown out of the lens, below the status
 * bar so the system icons stay visible. With music it is the song (artwork and title open the player, ⏮ ⏯ ⏭, the
 * timeline) and nothing more; without music the owner's Quick Actions (`Controls.quickActions`), run through the
 * Action Engine by [onAction]. It closes quietly: a tap outside, Back, a flick up on the card or the handle at its
 * foot, and leaves the way it came. [hole] and sizes are window px.
 */
@Composable
internal fun PulseGlance(hole: Hole, statusBar: Float, screenWidth: Float, music: Playing?, quick: List<QuickEntry>, events: Boolean,
    onAction: (Action) -> Unit, onOpenPlayer: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
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
    val state = GlanceState(music, quick.filter { it.slot.action != Action.Torch || torchId != null }, torch,
        alarm?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("H:mm")) },
        battery?.takeIf { events && it.charging })

    val margin = with(density) { 12.dp.toPx() }
    val wanted = with(density) { glanceWidth(state).toPx() }
    val (left, width) = menuLeftAndWidth(screenWidth, hole.x, margin, wanted)
    val ring = with(density) { 4.dp.toPx() }
    val top = with(density) { menuTop(statusBar, hole.y, hole.radius, ring, 8.dp.toPx()) }
    // Comes out of the lens and goes back into it; Reduce motion makes both instant.
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }
    var leaving by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun leave(then: () -> Unit) { if (leaving == null) { leaving = then; shown.targetState = false } }
    LaunchedEffect(shown.isIdle, shown.currentState) { if (shown.isIdle && !shown.currentState) leaving?.invoke() }
    val close = { leave(onClose) }
    val ink = LocalInk.current
    val scrim by animateFloatAsState(if (shown.targetState) 1f else 0f, tween(motionMs(200)), label = "glance-scrim")

    Box(Modifier.fillMaxSize().drawBehind { drawRect(Color.Black.copy(alpha = .22f * scrim)) }
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
            onClickLabel = stringResource(R.string.close), onClick = close)) {
        // Veil Trace: a fine ring round the lens and a short fading thread into the card, nothing more.
        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = scrim }) {
            val half = hole.radius + ring
            drawCircle(ink.accent.copy(alpha = .55f), half, Offset(hole.x, hole.y), style = Stroke(1.dp.toPx()))
            val from = hole.y + half
            if (top > from) drawLine(Brush.verticalGradient(listOf(ink.accent.copy(alpha = .5f), ink.accent.copy(alpha = .08f)), from, top),
                Offset(hole.x, from), Offset(hole.x, top + 2.dp.toPx()), 1.5.dp.toPx(), StrokeCap.Round)
        }
        AnimatedVisibility(shown, Modifier.offset { IntOffset(left.toInt(), top.toInt()) },
            enter = fadeIn(tween(motionMs(160))) + scaleIn(tween(motionMs(240)), initialScale = .86f, transformOrigin = TransformOrigin(.5f, 0f)),
            exit = fadeOut(tween(motionMs(140))) + scaleOut(tween(motionMs(160)), targetScale = .9f, transformOrigin = TransformOrigin(.5f, 0f))) {
            GlanceCard(state, with(density) { width.toDp() },
                onTorch = { on ->
                    try {
                        if (torchId != null) cameras?.setTorchMode(torchId, on)
                    } catch (_: CameraAccessException) {
                    } catch (_: IllegalArgumentException) {
                    }
                },
                onAction = { a -> leave { onAction(a) } }, onOpenPlayer = { music?.let { m -> leave { onOpenPlayer(m.info.pkg) } } }, onClose = close)
        }
    }
}

/** The card's width: the song needs room for its title, a row of actions only its buttons. */
internal fun glanceWidth(state: GlanceState) =
    if (state.music != null) 360.dp else ((state.quick.size + if (state.battery != null) 1 else 0).coerceIn(2, 4) * GLANCE_SLOT + 24).dp

private const val GLANCE_SLOT = 76

/**
 * Veil's glance material: a dense matte card tinted by the accent, lit softly from its top edge, a hairline rim and a
 * shadow that lifts it off Home. Never see-through (text over the clock would mix with it) and never blurred, so
 * nothing costs anything while PULSE rests.
 */
internal fun Modifier.veilGlass(shape: Shape, ink: InkPalette): Modifier {
    val base = lerp(ink.night, ink.accent, .12f)
    return shadow(18.dp, shape, ambientColor = ink.accent.copy(alpha = .35f), spotColor = Color.Black.copy(alpha = .6f))
        .clip(shape).background(base)
        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .07f), Color.Transparent), endY = 220f))
        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = .16f), Color.White.copy(alpha = .04f))), shape)
        .drawWithCache {
            // The same two-corner glint and accent bloom as every Veil glass pane (S4).
            val outline = shape.createOutline(size, layoutDirection, this)
            val bloom = glassBloom(ink.accent.copy(alpha = .06f), size.width, size.height)
            val glint = glassGlint(Color.White.copy(alpha = .16f), size.width, size.height)
            val stroke = Stroke(2.dp.toPx())
            onDrawBehind { bloom?.let { drawBloom(it) }; glint?.let { drawOutline(outline, it, style = stroke) } }
        }
}

/**
 * The card itself, stateless: Studio's preview draws the very same one with sample values. The PULSE opacity setting
 * tunes the capsule only.
 */
@Composable
internal fun GlanceCard(state: GlanceState, width: androidx.compose.ui.unit.Dp, onTorch: (Boolean) -> Unit,
    onAction: (Action) -> Unit, onOpenPlayer: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val ink = LocalInk.current
    val shape = RoundedCornerShape(28.dp)
    val glance = stringResource(R.string.pulse_glance)
    Column(modifier.width(width).veilGlass(shape, ink)
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
            state.battery?.let { b -> GlanceSlot(Glyph.Bolt, "${b.percent} %", on = false, act = Modifier) }
            state.quick.take(if (state.battery != null) MAX_GLANCE - 1 else MAX_GLANCE).forEach { entry -> QuickButton(entry, state, onTorch, onAction) }
            if (state.quick.isEmpty() && state.battery == null) Text(stringResource(R.string.pulse_quick_empty),
                Modifier.padding(vertical = 14.dp), style = InkText.small, color = ink.onNight.copy(alpha = .7f))
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
private fun QuickButton(entry: QuickEntry, state: GlanceState, onTorch: (Boolean) -> Unit, onAction: (Action) -> Unit) {
    val action = entry.slot.action
    val torch = action == Action.Torch
    val on = torch && state.torch
    val label = if (action == Action.Alarms && state.alarm != null) state.alarm else slotLabel(entry.slot)
    val act = if (torch) Modifier.toggleable(on, role = Role.Switch) { onTorch(it) }.semantics { stateDescription = if (on) "on" else "off" }
    else Modifier.clickable(role = Role.Button) { onAction(action) }
    GlanceSlot(entry.glyph, label, on, act, entry.slot)
}

@Composable
private fun GlanceSlot(glyph: Glyph?, text: String, on: Boolean, act: Modifier, slot: ArcEntry? = null) {
    val ink = LocalInk.current
    Column(Modifier.width(GLANCE_SLOT.dp).clip(RoundedCornerShape(18.dp)).then(act).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(if (on) ink.accent else ink.onNight.copy(alpha = .09f))
            .border(1.dp, ink.onNight.copy(alpha = if (on) 0f else .06f), CircleShape), contentAlignment = Alignment.Center) {
            val tint = if (on) ink.night else ink.onNight
            if (glyph != null || slot == null) HomeGlyph(glyph ?: Glyph.Bolt, null, tint = tint, size = 20.dp)
            else ActionIcon(slot.action, text, 24.dp, tint, app = slot.app, shortcut = slot.shortcut)
        }
        Text(text, Modifier.padding(top = 6.dp), style = InkText.small, color = ink.onNight.copy(alpha = .8f), maxLines = 1,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
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

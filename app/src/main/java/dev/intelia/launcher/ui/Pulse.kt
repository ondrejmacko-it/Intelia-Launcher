@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.intelia.launcher.ui

import dev.intelia.launcher.data.HapticSurface
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.graphics.RectF
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import dev.intelia.launcher.R
import dev.intelia.launcher.action.Action
import dev.intelia.launcher.action.Gesture
import dev.intelia.launcher.action.Trigger
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.Motion
import dev.intelia.launcher.data.PulseLens
import dev.intelia.launcher.flow.FlowAlias
import dev.intelia.launcher.pulse.Playing
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal data class Battery(val percent: Int, val charging: Boolean)

/** Where the camera cut-out sits, in window px. */
internal data class Hole(val x: Float, val y: Float, val radius: Float)

internal val PULSE_SWIPES = setOf(Gesture.SwipeDown, Gesture.SwipeUp, Gesture.SwipeLeft, Gesture.SwipeRight)

/**
 * The camera hole as the system describes it. The cutout path is exact; some builds report a bounding
 * rect that covers only half of the hole (Nothing OS 4.1 on Phone (3)), so the rect is the fallback.
 */
@Composable
internal fun rememberDetectedHole(): Hole? {
    val view = LocalView.current
    var hole by remember { mutableStateOf<Hole?>(null) }
    LaunchedEffect(view) {
        withFrameNanos { }
        val cutout = view.rootWindowInsets?.displayCutout ?: return@LaunchedEffect
        val fromPath = if (Build.VERSION.SDK_INT >= 31) cutout.cutoutPath?.let { path ->
            val b = RectF().also { path.computeBounds(it, true) }
            b.takeIf { it.width() > 0f && it.top < view.height / 4f && it.width() < view.width / 3f }
                ?.let { Hole(it.centerX(), it.centerY(), minOf(it.width(), it.height()) / 2f) }
        } else null
        hole = fromPath ?: cutout.boundingRects.firstOrNull { it.top < view.height / 4 && it.width() < view.width / 3 }
            ?.let { Hole(it.exactCenterX(), it.exactCenterY(), minOf(it.width(), it.height()) / 2f) }
    }
    return hole
}

/** The lens PULSE draws around: the user's calibration in portrait, else the detected cutout, else a guess. */
@Composable
internal fun rememberLens(lens: PulseLens?): Hole {
    val view = LocalView.current
    val density = LocalDensity.current
    val detected = rememberDetectedHole()
    val statusTop = with(density) { WindowInsets.statusBarsIgnoringVisibility.getTop(this).toFloat() }
    val portrait = view.height >= view.width
    return with(density) {
        when {
            lens != null && portrait -> Hole(lens.x.dp.toPx(), lens.y.dp.toPx(), lens.r.dp.toPx())
            detected != null -> detected
            else -> Hole(view.width / 2f, statusTop / 2f, 11.dp.toPx())
        }
    }
}

internal fun Intent.battery(): Battery? {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return null
    val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    // Plugged in counts too: with a charge limit the phone sits on power without "charging".
    val plugged = getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
    return Battery(level * 100 / scale, plugged || status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
}

internal fun nextAlarm(context: Context): Long? =
    context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime

internal fun torchCamera(manager: CameraManager): String? = try {
    manager.cameraIdList.firstOrNull { id ->
        val c = manager.getCameraCharacteristics(id)
        c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
            c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
    }
} catch (_: CameraAccessException) {
    null
}

/**
 * PULSE: the camera cut-out as Intelia's second focus. At rest it is invisible. Returning Home sends
 * one echo out of the lens; charging or a near alarm shows a small disc around the camera inside the
 * status bar (D8): the ring is the battery gauge, one dot marks the alarm, no text. Tap opens the panel. Home only, no permissions,
 * no background work: everything is read while Home is started.
 *
 * [preview] (Studio) always shows the capsule as if charging (with the alarm dot when alarms are on),
 * without echo or panel, so opacity can be tuned live.
 */
@Composable
fun Pulse(settings: LauncherSettings, modifier: Modifier = Modifier, preview: Boolean = false, onGesture: (Gesture) -> Boolean = { false },
    /** Global ARC (M3.3): what its sets hold now, and where a released slot goes (null = no Global ARC here). */
    arc: ArcContent? = null, onArcAction: (Action) -> Unit = {}, flowSteps: (FlowAlias) -> List<String?> = { emptyList() },
    /** S3.1: the glance's Quick Actions, resolved by the caller ([quickEntries]). */
    quick: List<QuickEntry> = emptyList()) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val ink = LocalInk.current
    val homeInk = LocalHomeInk.current
    val motion = LocalMotion.current
    val scope = rememberCoroutineScope()
    val h = rememberLens(settings.pulseLens)
    var battery by remember { mutableStateOf<Battery?>(null) }
    var alarm by remember { mutableStateOf<Long?>(null) }
    var expanded by remember { mutableStateOf(false) }
    /** The open Global ARC set index, the picked slot and whether TalkBack opened it (slots are buttons then). */
    var arcSet by remember { mutableStateOf<Int?>(null) }
    var arcPick by remember { mutableStateOf<Int?>(null) }
    var arcAssisted by remember { mutableStateOf(false) }
    /** The set last shown, so the slots stay while Global ARC fades out. */
    var arcShown by remember { mutableStateOf(0) }
    val haptics = rememberInkHaptics(HapticSurface.Pulse)
    fun closeArc() { arcSet = null; arcPick = null; arcAssisted = false }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { closeArc() }
    BackHandler(enabled = arcSet != null) { closeArc() }
    val global by PulseAccessibilityService.running.collectAsState()
    val echo = remember { Animatable(1f) }
    var started by remember { mutableStateOf(false) }

    LifecycleStartEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_BATTERY_CHANGED -> battery = intent.battery()
                    AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED -> alarm = nextAlarm(c)
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED).apply { addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED) }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)?.battery()?.let { battery = it }
        alarm = nextAlarm(context)
        started = true
        onStopOrDispose { context.unregisterReceiver(receiver); expanded = false; started = false }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (!preview && motion != Motion.Off && settings.pulseEcho) scope.launch { echo.snapTo(0f); echo.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
    }
    BackHandler(enabled = expanded) { expanded = false }

    val rest = pulseRest(preview || settings.pulseCharging && battery?.charging == true,
        alarm?.takeIf { settings.pulseAlarm }, System.currentTimeMillis(), settings.pulseAlarmHours)
        .let { if (preview) it.copy(alarmMark = settings.pulseAlarm) else it }
    val label = stringResource(R.string.pulse)
    // No text at rest, so TalkBack reads what the dot and the gauge mean.
    val restState = listOfNotNull(
        battery?.takeIf { rest.gauge }?.let { "${it.percent} %" },
        alarm?.takeIf { rest.alarmMark }?.let { stringResource(R.string.pulse_alarm_near,
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("H:mm"))) },
    ).joinToString(", ")
    val statusBar = with(density) { WindowInsets.statusBarsIgnoringVisibility.getTop(this).toDp().value }
    // M3.2: with PULSE everywhere on and the service running, the service shows the music; Home only while it owns the lens.
    val owns = !(global && settings.pulseEverywhere)
    val music = rememberPlaying(!preview && started && owns && settings.pulseMusic)?.takeIf { it.info.visible }

    // Home draws its content after PULSE; while expanded the panel (and its close layer) must sit above it.
    Box(modifier.zIndex(if (expanded || arcSet != null) 1f else 0f).fillMaxSize()) {
        val hx = with(density) { h.x.toDp() }
        val hy = with(density) { h.y.toDp() }
        val hr = with(density) { h.radius.toDp() }

        // The echo: one ring leaving the lens when Home comes back.
        Canvas(Modifier.fillMaxSize()) {
            val t = echo.value
            if (t < 1f) {
                val center = Offset(h.x, h.y)
                drawCircle(ink.accent.copy(alpha = .55f * (1f - t)), h.radius + 150.dp.toPx() * t, center, style = Stroke(1.5.dp.toPx()))
                val t2 = ((t - .18f) / .82f).coerceIn(0f, 1f)
                if (t2 > 0f) drawCircle(homeInk.copy(alpha = .3f * (1f - t2)), h.radius + 110.dp.toPx() * t2, center, style = Stroke(1.dp.toPx()))
            }
        }

        // At rest (D8): a disc around the lens inside the status bar, no wider than the cut-out plus one dot,
        // so it never covers status icons and repeats nothing the system already shows.
        val capsule = restCapsule(hx.value, hy.value, hr.value, statusBar, rest.alarmMark)
        AnimatedVisibility(rest.visible && music == null && !expanded, Modifier.offset(x = capsule.left.dp, y = capsule.top.dp),
            enter = fadeIn(tween(motionMs(200))) + expandIn(tween(motionMs(260)), expandFrom = Alignment.CenterStart),
            exit = fadeOut(tween(motionMs(160))) + shrinkOut(tween(motionMs(200)), shrinkTowards = Alignment.CenterStart)) {
            Canvas(Modifier.size(capsule.width.dp, capsule.height.dp).clip(RoundedCornerShape(50))
                .background(ink.night.copy(alpha = settings.pulseOpacity / 100f))) {
                val c = Offset(capsule.lensX.dp.toPx(), size.height / 2f)
                val r = h.radius + 3.dp.toPx()
                // The ring: a quiet track, and while charging the thread of the battery gauge.
                drawCircle(ink.onNight.copy(alpha = .18f), r, c, style = Stroke(1.5.dp.toPx()))
                if (rest.gauge) drawArc(ink.accent, -90f, 360f * (battery?.percent ?: 0) / 100f, false, Offset(c.x - r, c.y - r),
                    Size(2 * r, 2 * r), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                if (rest.alarmMark) drawCircle(ink.accent, 2.5.dp.toPx(), Offset(capsule.markX.dp.toPx(), size.height / 2f))
            }
        }

        // Compact music (M3.2): bars on the left (moving only while playing), the round artwork on the right.
        val mc = musicCapsule(hx.value, hy.value, hr.value, statusBar)
        AnimatedVisibility(music != null && !expanded, Modifier.offset(x = mc.left.dp, y = mc.top.dp),
            enter = fadeIn(tween(motionMs(200))) + expandIn(tween(motionMs(260)), expandFrom = Alignment.Center),
            exit = fadeOut(tween(motionMs(160))) + shrinkOut(tween(motionMs(200)), shrinkTowards = Alignment.Center)) {
            val shown = music ?: return@AnimatedVisibility
            MusicCompact(shown, mc, h.radius, settings.pulseOpacity, battery?.percent?.takeIf { rest.gauge })
        }

        // With PULSE everywhere on (D9) the service owns the lens; Lab draws no second target under it.
        // M3: tap opens the panel unless the owner bound it; double tap and hold exist only when bound, so an
        // unbound capsule never waits for a second tap.
        val controls = settings.controls
        val bound = { g: Gesture -> controls.bound(Trigger.Pulse(g)) != null }
        val hold = bound(Gesture.Hold)
        val holdLabel = stringResource(R.string.pulse_gesture_hold)
        val gestureLabels = mapOf(Gesture.DoubleTap to stringResource(R.string.pulse_gesture_double),
            Gesture.DoubleTapHold to stringResource(R.string.pulse_gesture_double_hold),
            Gesture.SwipeDown to stringResource(R.string.pulse_gesture_down),
            Gesture.SwipeLeft to stringResource(R.string.pulse_gesture_left), Gesture.SwipeRight to stringResource(R.string.pulse_gesture_right))
        val extras = gestureLabels.keys.filter(bound)
        val swipes = extras.any { it in PULSE_SWIPES }
        // With music shown the tap opens the player first (owner can turn that off); otherwise its action, else the menu.
        val tap = { if ((music != null && settings.pulseTapPlayer) || !onGesture(Gesture.Tap)) expanded = true }
        val camera = Pt(hx.value, hy.value)
        val window = LocalWindowInfo.current.containerSize
        val screenW = window.width / density.density
        val screenH = window.height / density.density
        // M3.3: a held gesture bound to Global ARC opens it here; every other gesture goes to the caller.
        fun openArc(g: Gesture, assisted: Boolean): Boolean {
            val a = controls.bound(Trigger.Pulse(g)) as? Action.Arc ?: return false
            val content = arc ?: return false
            arcSet = content.sets.indexOfFirst { it.id == controls.arcSetFor(a).id }.coerceAtLeast(0)
            arcShown = arcSet ?: 0
            arcPick = null
            arcAssisted = assisted
            haptics.confirm()
            return true
        }
        fun gesture(g: Gesture) { if (!openArc(g, false)) { if (g == Gesture.Tap) tap() else onGesture(g) } }
        fun assisted(g: Gesture) { if (!openArc(g, true)) onGesture(g) }
        // S3: the touch target is its own square on the lens ([pulseHitbox], owner-calibrated), whatever the capsule
        // draws. The status bar window takes touches inside it, so on Home the target also reaches under the bar.
        // With PULSE everywhere on (D9) and the service running, the service owns the lens; Lab draws no second target.
        val hit = pulseHitbox(hx.value, hy.value, settings.pulseHit, screenW, blockedTop = statusBar)
        val targetLeft = hit.left.dp
        val targetTop = hit.top.dp
        if (!preview && !expanded && owns) Box(Modifier.offset(x = targetLeft, y = targetTop)
            .size(hit.width.dp, hit.height.dp)
            // M3.1: one recognizer for every capsule gesture (shared with the PULSE service); a shorter gesture
            // waits only while a longer bound one can still follow.
            .pulseTouch(bound, swipes, ::gesture, doubleTapMs = settings.pulseDoubleTapMs,
                onDown = { if (settings.pulseTouchHaptic) haptics.tick() }, onHeld = { x, y ->
                val k = arcSet ?: return@pulseTouch
                val content = arc ?: return@pulseTouch
                val bow = GlobalArcGeometry.bow(content.globalLook, camera, screenW, screenH)
                val next = GlobalArcGeometry.pick(bow, camera, targetLeft.value + x, targetTop.value + y, content.filled(k), arcPick)
                if (next != null && next != arcPick && content.globalLook.haptics) haptics.tick()
                arcPick = next
            }, onHeldEnd = { release ->
                val chosen = if (release) arcSet?.let { k -> arcPick?.let { arc?.sets?.getOrNull(k)?.slots?.getOrNull(it) } } else null
                if (arcSet != null) closeArc()
                chosen?.let { haptics.confirm(); onArcAction(it.action) }
            })
            .semantics {
                role = Role.Button
                contentDescription = label
                val playingState = music?.info?.let { i -> listOf(i.title, i.artist).filter(String::isNotEmpty).joinToString(" – ") }.orEmpty()
                listOf(restState, playingState).filter(String::isNotEmpty).joinToString(", ").takeIf { it.isNotEmpty() }?.let { stateDescription = it }
                onClick(label) { tap(); true }
                if (hold) onLongClick(holdLabel) { assisted(Gesture.Hold); true }
                // TalkBack has no double tap or swipe of its own; bound ones are offered as custom actions.
                if (extras.isNotEmpty()) customActions = extras.map { g -> CustomAccessibilityAction(gestureLabels.getValue(g)) { assisted(g); true } }
            })

        // S3: the glance replaces the old dashboard: the song while music shows, else one row of what matters now.
        if (expanded) PulseGlance(h, with(density) { statusBar.dp.toPx() }, window.width.toFloat(), music, quick, settings.pulseCharging,
            onAction = { action ->
                expanded = false
                // The card has left the screen by now, so a screenshot shows what lies under it.
                onArcAction(action)
            },
            onOpenPlayer = { pkg ->
                playerIntent(context, pkg)?.let { try { context.startActivity(it) } catch (_: ActivityNotFoundException) { } }
                expanded = false
            },
            onClose = { expanded = false })
        arc?.let { content ->
            val shown = content.sets.getOrNull(arcSet ?: arcShown)
            GlobalArc(shown, content.globalLook, camera, arcSet != null, arcPick, flowSteps,
                onSlot = if (arcAssisted) { i -> val chosen = shown?.slots?.getOrNull(i); closeArc(); chosen?.let { onArcAction(it.action) } } else null,
                onDismiss = ::closeArc)
        }
    }
}

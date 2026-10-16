package dev.intelia.launcher.ui

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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import dev.intelia.launcher.R
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.Motion
import dev.intelia.launcher.data.PulseLens
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class Battery(val percent: Int, val charging: Boolean)

/** Where the camera cut-out sits, in window px. */
internal data class Hole(val x: Float, val y: Float, val radius: Float)

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
    val statusTop = with(density) { WindowInsets.statusBars.getTop(this).toFloat() }
    val portrait = view.height >= view.width
    return with(density) {
        when {
            lens != null && portrait -> Hole(lens.x.dp.toPx(), lens.y.dp.toPx(), lens.r.dp.toPx())
            detected != null -> detected
            else -> Hole(view.width / 2f, statusTop / 2f, 11.dp.toPx())
        }
    }
}

private fun Intent.battery(): Battery? {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return null
    val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    // Plugged in counts too: with a charge limit the phone sits on power without "charging".
    val plugged = getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
    return Battery(level * 100 / scale, plugged || status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
}

private fun nextAlarm(context: Context): Long? =
    context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime

private fun torchCamera(manager: CameraManager): String? = try {
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
 * one echo out of the lens; a near alarm or charging shows a capsule around the camera, and the ring
 * around the lens becomes the battery gauge. Tap opens the panel. Home only, no permissions,
 * no background work: everything is read while Home is started.
 */
@Composable
fun Pulse(settings: LauncherSettings, modifier: Modifier = Modifier) {
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
    val echo = remember { Animatable(1f) }

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
        onStopOrDispose { context.unregisterReceiver(receiver); expanded = false }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (motion != Motion.Off && settings.pulseEcho) scope.launch { echo.snapTo(0f); echo.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
    }
    BackHandler(enabled = expanded) { expanded = false }

    val now = System.currentTimeMillis()
    val soonAlarm = alarm?.takeIf { settings.pulseAlarm && it - now in 0..settings.pulseAlarmHours * 3_600_000L }
    val charging = settings.pulseCharging && battery?.charging == true
    val time = DateTimeFormatter.ofPattern("H:mm")
    val alarmText = soonAlarm?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime().format(time) }
    val label = stringResource(R.string.pulse)

    Box(modifier.fillMaxSize()) {
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

        // Capsule around the camera when there is something worth a glance.
        val compact = (soonAlarm != null || charging) && !expanded
        // Each half is only as wide as what it says, so the capsule hides as few status icons as possible.
        val left = if (alarmText != null) 58.dp else 12.dp
        val right = if (charging) 48.dp else 12.dp
        val pillHeight = hr * 2 + 10.dp
        AnimatedVisibility(compact, Modifier.offset(x = hx - hr - 5.dp - left, y = hy - pillHeight / 2),
            enter = fadeIn(tween(motionMs(200))) + expandIn(tween(motionMs(260)), expandFrom = Alignment.Center),
            exit = fadeOut(tween(motionMs(160))) + shrinkOut(tween(motionMs(200)), shrinkTowards = Alignment.Center)) {
            Row(Modifier.height(pillHeight).clip(RoundedCornerShape(50)).background(ink.night)
                .clickable(onClickLabel = label, role = Role.Button) { expanded = true }
                .semantics { contentDescription = label },
                verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.width(left).padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (alarmText != null) {
                        HomeGlyph(Glyph.Bell, null, tint = ink.onNight, size = 12.dp)
                        Text(alarmText, Modifier.padding(start = 3.dp), style = InkText.label.copy(fontSize = 11.sp, letterSpacing = 0.sp),
                            color = ink.onNight, maxLines = 1)
                    }
                }
                // The lens itself; while charging its rim is the battery gauge.
                Canvas(Modifier.size(hr * 2 + 10.dp)) {
                    val r = h.radius + 3.dp.toPx()
                    val c = Offset(size.width / 2f, size.height / 2f)
                    if (charging) {
                        drawCircle(ink.onNight.copy(alpha = .18f), r, c, style = Stroke(1.5.dp.toPx()))
                        drawArc(ink.accent, -90f, 360f * (battery?.percent ?: 0) / 100f, false, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r),
                            style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
                Row(Modifier.width(right).padding(end = 10.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (charging) Text("${battery?.percent ?: 0}%", style = InkText.label.copy(fontSize = 11.sp, letterSpacing = 0.sp),
                        color = ink.onNight, maxLines = 1)
                }
            }
        }

        // Invisible touch target on the lens at rest.
        if (!compact && !expanded) Box(Modifier.offset(x = hx - 24.dp, y = hy - 24.dp).size(48.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClickLabel = label,
                role = Role.Button) { expanded = true }
            .semantics { contentDescription = label })

        if (expanded) Box(Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() },
            indication = null, onClickLabel = stringResource(R.string.close)) { expanded = false })
        AnimatedVisibility(expanded, Modifier.fillMaxWidth().padding(horizontal = 12.dp).offset(y = (hy - hr - 6.dp).coerceAtLeast(4.dp)),
            enter = fadeIn(tween(motionMs(160))) + expandIn(tween(motionMs(300)), expandFrom = Alignment.TopCenter),
            exit = fadeOut(tween(motionMs(140))) + shrinkOut(tween(motionMs(220)), shrinkTowards = Alignment.TopCenter)) {
            PulsePanel(alarm, battery, hr, onClose = { expanded = false })
        }
    }
}

@Composable
private fun PulsePanel(alarm: Long?, battery: Battery?, holeRadius: androidx.compose.ui.unit.Dp, onClose: () -> Unit) {
    val context = LocalContext.current
    val ink = LocalInk.current
    val manager = remember(context) { context.getSystemService(CameraManager::class.java) }
    val camera = remember(manager) { manager?.let(::torchCamera) }
    var torch by remember { mutableStateOf(false) }
    DisposableEffect(manager, camera) {
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) { if (cameraId == camera) torch = enabled }
        }
        if (camera != null) manager?.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
        onDispose { manager?.unregisterTorchCallback(callback) }
    }
    val zone = ZoneId.systemDefault()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(ink.night)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
        .padding(start = 22.dp, end = 14.dp, bottom = 10.dp)) {
        // Keep the camera clear: the panel grows out of it.
        Box(Modifier.fillMaxWidth().height(holeRadius * 2 + 18.dp).drawBehind {
            drawCircle(ink.onNight.copy(alpha = .18f), holeRadius.toPx() + 4.dp.toPx(), Offset(size.width / 2f - 4.dp.toPx(), size.height / 2f),
                style = Stroke(1.dp.toPx()))
        }) {
            Kicker(stringResource(R.string.pulse), Modifier.align(Alignment.CenterStart), color = ink.onNight.copy(alpha = .62f))
        }
        val alarmRow = alarm?.let {
            val at = Instant.ofEpochMilli(it).atZone(zone)
            val day = when (at.toLocalDate()) {
                LocalDate.now(zone) -> stringResource(R.string.today)
                LocalDate.now(zone).plusDays(1) -> stringResource(R.string.tomorrow)
                else -> at.format(DateTimeFormatter.ofPattern("EEE d. M."))
            }
            at.toLocalTime().format(DateTimeFormatter.ofPattern("H:mm")) to day
        }
        PanelRow(Glyph.Bell, alarmRow?.first ?: stringResource(R.string.no_alarm), alarmRow?.second, onClick = {
            try {
                context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: ActivityNotFoundException) {
            }
            onClose()
        })
        battery?.let {
            PanelRow(Glyph.Bolt, "${it.percent} %", stringResource(if (it.charging) R.string.charging else R.string.on_battery), onClick = null)
        }
        if (camera != null) {
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).toggleable(torch, role = Role.Switch) { on ->
                try {
                    manager?.setTorchMode(camera, on)
                } catch (_: CameraAccessException) {
                } catch (_: IllegalArgumentException) {
                }
            }.semantics { stateDescription = if (torch) "on" else "off" }, verticalAlignment = Alignment.CenterVertically) {
                HomeGlyph(Glyph.Torch, null, tint = ink.onNight, size = 20.dp)
                Text(stringResource(R.string.torch), Modifier.weight(1f).padding(start = 14.dp), style = InkText.body, color = ink.onNight)
                Canvas(Modifier.padding(end = 8.dp).size(24.dp)) {
                    val r = size.minDimension / 2f - 1.5.dp.toPx()
                    drawCircle(if (torch) ink.accent else ink.onNight.copy(alpha = .5f), r, style = Stroke(1.5.dp.toPx()))
                    if (torch) drawCircle(ink.accent, r * .55f)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Quiet(stringResource(R.string.collapse), onClose, Modifier.padding(end = 4.dp), color = ink.onNight.copy(alpha = .7f))
        }
    }
}

@Composable
private fun PanelRow(glyph: Glyph, title: String, detail: String?, onClick: (() -> Unit)?) {
    val ink = LocalInk.current
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically) {
        HomeGlyph(glyph, null, tint = ink.onNight, size = 20.dp)
        Text(title, Modifier.padding(start = 14.dp), style = InkText.item.copy(fontSize = 22.sp), color = ink.onNight)
        Spacer(Modifier.width(10.dp))
        if (detail != null) Text(detail, style = InkText.small.copy(fontWeight = FontWeight.Normal), color = ink.onNight.copy(alpha = .66f))
    }
}

package dev.intelia.launcher.ui

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.RectF
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.intelia.launcher.InteliaApp
import dev.intelia.launcher.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * PULSE everywhere (D9). Off until the owner enables it in the system accessibility settings. It receives
 * no accessibility events and cannot read windows; it only owns one small overlay on the camera lens and,
 * after a tap, a menu that grows down from the lens.
 */
class PulseAccessibilityService : AccessibilityService() {
    private val main = Handler(Looper.getMainLooper())
    private lateinit var windows: WindowManager
    private var lens: View? = null
    private var menu: View? = null
    private var owner: OverlayOwner? = null

    override fun onServiceConnected() {
        windows = getSystemService(WindowManager::class.java)
        _running.value = true
        placeLens()
        relayout = { main.post { if (lens != null) placeLens() } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        closeMenu()
        if (lens != null) placeLens()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        tearDown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        tearDown()
        super.onDestroy()
    }

    private fun tearDown() {
        closeMenu()
        lens?.let { runCatching { windows.removeView(it) } }
        lens = null
        relayout = null
        _running.value = false
    }

    private val dp get() = resources.displayMetrics.density

    private fun screenWidth(): Int =
        if (Build.VERSION.SDK_INT >= 30) windows.currentWindowMetrics.bounds.width() else resources.displayMetrics.widthPixels

    private fun statusBarPx(): Float {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id != 0) resources.getDimensionPixelSize(id).toFloat() else 24f * dp
    }

    /** Lens in screen px: Studio calibration in portrait, else the cutout path, else the cutout rect, else a guess. */
    private fun hole(): Hole {
        val calibrated = (application as InteliaApp).settings.state.value.pulseLens
        val portrait = resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        if (calibrated != null && portrait) return Hole(calibrated.x * dp, calibrated.y * dp, calibrated.r * dp)
        if (Build.VERSION.SDK_INT >= 30) {
            val cutout = windows.currentWindowMetrics.windowInsets.displayCutout
            val width = screenWidth()
            if (cutout != null) {
                val fromPath = if (Build.VERSION.SDK_INT >= 31) cutout.cutoutPath?.let { path ->
                    RectF().also { path.computeBounds(it, true) }.takeIf { it.width() > 0f && it.width() < width / 3f }
                        ?.let { Hole(it.centerX(), it.centerY(), minOf(it.width(), it.height()) / 2f) }
                } else null
                val fromRect = cutout.boundingRects.firstOrNull { it.width() < width / 3 }
                    ?.let { Hole(it.exactCenterX(), it.exactCenterY(), minOf(it.width(), it.height()) / 2f) }
                (fromPath ?: fromRect)?.let { return it }
            }
        }
        return Hole(screenWidth() / 2f, statusBarPx() / 2f, 11f * dp)
    }

    private fun overlay(width: Int, height: Int, focusable: Boolean) = WindowManager.LayoutParams(
        width, height, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            if (focusable) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        if (Build.VERSION.SDK_INT >= 30) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            fitInsetsTypes = 0
        } else if (Build.VERSION.SDK_INT >= 28) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        title = "Intelia PULSE"
    }

    private fun placeLens() {
        val h = hole()
        val box = lensTarget(h.x, h.y, h.radius, 10f * dp, 32f * dp)
        val params = overlay(box.width, box.height, focusable = false).apply {
            x = box.left; y = box.top
            // While Home's editor is open the camera ring there edits PULSE, so the lens lets taps through.
            if (editing) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        val view = lens ?: View(this).apply {
            contentDescription = getString(R.string.pulse)
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                openMenu()
            }
        }
        if (lens == null) {
            windows.addView(view, params)
            lens = view
        } else {
            windows.updateViewLayout(view, params)
        }
    }

    private fun openMenu() {
        if (menu != null) return
        val hole = hole()
        val statusBar = statusBarPx()
        val width = screenWidth().toFloat()
        val o = OverlayOwner().also { it.start() }
        val root = object : FrameLayout(this) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode != KeyEvent.KEYCODE_BACK) return super.dispatchKeyEvent(event)
                if (event.action == KeyEvent.ACTION_UP) closeMenu()
                return true
            }
        }
        root.setViewTreeLifecycleOwner(o)
        root.setViewTreeSavedStateRegistryOwner(o)
        root.addView(ComposeView(this).apply {
            setContent {
                val settings by (application as InteliaApp).settings.state.collectAsState()
                InteliaTheme(settings) {
                    PulseMenuSheet(hole, statusBar, width, ::act, ::open, ::closeMenu)
                }
            }
        })
        windows.addView(root, overlay(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, focusable = true))
        menu = root
        owner = o
    }

    private fun closeMenu() {
        menu?.let { runCatching { windows.removeView(it) } }
        owner?.stop()
        menu = null
        owner = null
    }

    private fun act(action: PulseAction) {
        closeMenu()
        when (action) {
            PulseAction.Torch -> Unit // toggled inside the menu
            // Wait for the menu window to leave the screen so it is not in the picture.
            PulseAction.Screenshot -> if (Build.VERSION.SDK_INT >= 28) main.postDelayed({ performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT) }, 350)
            PulseAction.Lock -> if (Build.VERSION.SDK_INT >= 28) main.postDelayed({ performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) }, 150)
            PulseAction.Notifications -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            PulseAction.QuickSettings -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        }
    }

    private fun open(intent: Intent) {
        closeMenu()
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
        } catch (_: SecurityException) {
        }
    }

    companion object {
        private val _running = MutableStateFlow(false)

        /** True while the service is bound in this process; Lab then hides its own lens target. */
        val running: StateFlow<Boolean> = _running.asStateFlow()

        @Volatile private var editing = false
        @Volatile private var relayout: (() -> Unit)? = null

        /** Home's editor is open (true) or closed: the lens overlay stops or resumes taking taps. */
        fun setEditing(on: Boolean) {
            if (editing == on) return
            editing = on
            relayout?.invoke()
        }

        /**
         * The system accessibility list, where the owner switches the service on or off. The per-service
         * details page needs the privileged OPEN_ACCESSIBILITY_DETAILS_SETTINGS permission, so it is not used.
         */
        fun openSettings(context: Context) {
            try {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
    }
}

/** Lifecycle and saved state for a ComposeView that lives in a service window, not an activity. */
private class OverlayOwner : SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val saved = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry

    fun start() {
        saved.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}

private fun linkIntent(link: PulseLink): Intent = when (link) {
    PulseLink.Wifi -> Intent(Settings.Panel.ACTION_WIFI)
    PulseLink.Internet -> Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
    PulseLink.Bluetooth -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
    PulseLink.Sound -> Intent(Settings.ACTION_SOUND_SETTINGS)
    PulseLink.Display -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
}

private val PulseAction.label: Int get() = when (this) {
    PulseAction.Torch -> R.string.torch
    PulseAction.Screenshot -> R.string.pulse_screenshot
    PulseAction.Lock -> R.string.pulse_lock
    PulseAction.Notifications -> R.string.pulse_notifications
    PulseAction.QuickSettings -> R.string.pulse_quick_settings
}

private val PulseLink.label: Int get() = when (this) {
    PulseLink.Wifi -> R.string.pulse_wifi
    PulseLink.Internet -> R.string.pulse_internet
    PulseLink.Bluetooth -> R.string.pulse_bluetooth
    PulseLink.Sound -> R.string.pulse_sound
    PulseLink.Display -> R.string.pulse_display
}

/** The menu: a dark sheet that grows out of the lens; everything outside it closes. */
@Composable
private fun PulseMenuSheet(
    hole: Hole, statusBar: Float, screenWidth: Float,
    onAction: (PulseAction) -> Unit, onOpen: (Intent) -> Unit, onClose: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val ink = LocalInk.current
    val audio = remember(context) { context.getSystemService(AudioManager::class.java) }
    val cameras = remember(context) { context.getSystemService(CameraManager::class.java) }
    val torchId = remember(cameras) { cameras?.let(::torchCamera) }
    var torch by remember { mutableStateOf(false) }
    var battery by remember { mutableStateOf<Battery?>(null) }
    val alarm = remember { nextAlarm(context) }
    var volume by remember { mutableFloatStateOf(0f) }
    var ringer by remember { mutableIntStateOf(audio?.ringerMode ?: RINGER_NORMAL) }
    var playing by remember { mutableStateOf(audio?.isMusicActive == true) }
    fun readVolume() {
        audio?.let { volume = volumeFraction(it.getStreamVolume(AudioManager.STREAM_MUSIC), it.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) }
    }

    DisposableEffect(context) {
        readVolume()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) { battery = intent.battery() }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
            ?.battery()?.let { battery = it }
        val torchCallback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) { if (cameraId == torchId) torch = enabled }
        }
        if (torchId != null) cameras?.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
        onDispose {
            context.unregisterReceiver(receiver)
            cameras?.unregisterTorchCallback(torchCallback)
        }
    }

    val margin = with(density) { 12.dp.toPx() }
    val (left, width) = menuLeftAndWidth(screenWidth, hole.x, margin, with(density) { 420.dp.toPx() })
    val ring = with(density) { 4.dp.toPx() }
    val top = with(density) { menuTop(statusBar, hole.y, hole.radius, ring, 10.dp.toPx()) }
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }

    Box(Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
        onClickLabel = stringResource(R.string.close)) { onClose() }) {
        // The status bar stays visible: a translucent strip, as wide as the lens ring, leads from the camera
        // down into the sheet; the icons on both sides are never covered.
        AnimatedVisibility(shown, enter = fadeIn(tween(motionMs(160)))) {
            Canvas(Modifier.fillMaxSize()) {
                val half = hole.radius + ring
                drawRoundRect(ink.night.copy(alpha = .55f), Offset(hole.x - half, hole.y - half),
                    Size(2f * half, top + 30.dp.toPx() - (hole.y - half)), CornerRadius(half))
                drawCircle(ink.onNight.copy(alpha = .45f), half, Offset(hole.x, hole.y), style = Stroke(1.dp.toPx()))
            }
        }
        AnimatedVisibility(shown, Modifier.offset { IntOffset(left.toInt(), top.toInt()) },
            enter = fadeIn(tween(motionMs(160))) + expandIn(tween(motionMs(280)), expandFrom = Alignment.TopCenter)) {
            Column(Modifier.width(with(density) { width.toDp() }).clip(RoundedCornerShape(30.dp)).background(ink.night)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }) {
                Column(Modifier.padding(start = 20.dp, top = 8.dp, end = 14.dp, bottom = 10.dp)) {
                    val zone = ZoneId.systemDefault()
                    val alarmText = alarm?.let {
                        val at = Instant.ofEpochMilli(it).atZone(zone)
                        val day = when (at.toLocalDate()) {
                            LocalDate.now(zone) -> stringResource(R.string.today)
                            LocalDate.now(zone).plusDays(1) -> stringResource(R.string.tomorrow)
                            else -> at.format(DateTimeFormatter.ofPattern("EEE d. M."))
                        }
                        at.toLocalTime().format(DateTimeFormatter.ofPattern("H:mm")) to day
                    }
                    PanelRow(Glyph.Bell, alarmText?.first ?: stringResource(R.string.no_alarm), alarmText?.second,
                        onClick = { onOpen(Intent(AlarmClock.ACTION_SHOW_ALARMS)) })
                    battery?.let {
                        PanelRow(Glyph.Bolt, "${it.percent} %", stringResource(if (it.charging) R.string.charging else R.string.on_battery), onClick = null)
                    }
                    MediaRow(playing) { code ->
                        val now = SystemClock.uptimeMillis()
                        audio?.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0))
                        audio?.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0))
                        if (code == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) playing = !playing
                    }
                    VolumeRow(volume, ringer,
                        onStep = { raise ->
                            audio?.adjustStreamVolume(AudioManager.STREAM_MUSIC, if (raise) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, 0)
                            readVolume()
                        },
                        onRinger = {
                            try {
                                audio?.ringerMode = nextRinger(ringer)
                            } catch (_: SecurityException) {
                            }
                            ringer = audio?.ringerMode ?: ringer
                        })
                    Spacer(Modifier.height(6.dp))
                    pulseActions(Build.VERSION.SDK_INT, torchId != null).chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { action ->
                                val on = action == PulseAction.Torch && torch
                                Chip(stringResource(action.label), on, Modifier.weight(1f)) {
                                    if (action == PulseAction.Torch) {
                                        try {
                                            if (torchId != null) cameras?.setTorchMode(torchId, !torch)
                                        } catch (_: CameraAccessException) {
                                        } catch (_: IllegalArgumentException) {
                                        }
                                    } else {
                                        onAction(action)
                                    }
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    pulseLinks(Build.VERSION.SDK_INT).chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { link ->
                                Text(stringResource(link.label) + " ↗", Modifier.weight(1f).heightIn(min = 44.dp)
                                    .clickable(role = Role.Button) { onOpen(linkIntent(link)) }.padding(top = 12.dp),
                                    style = InkText.small, color = ink.onNight.copy(alpha = .72f), textAlign = TextAlign.Center)
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Quiet(stringResource(R.string.collapse), onClose, Modifier.padding(end = 4.dp), color = ink.onNight.copy(alpha = .7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaRow(playing: Boolean, onKey: (Int) -> Unit) {
    val ink = LocalInk.current
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        listOf(
            Triple(Glyph.Prev, R.string.pulse_prev, KeyEvent.KEYCODE_MEDIA_PREVIOUS),
            Triple(if (playing) Glyph.Pause else Glyph.Play, R.string.pulse_play_pause, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE),
            Triple(Glyph.Next, R.string.pulse_next, KeyEvent.KEYCODE_MEDIA_NEXT),
        ).forEach { (glyph, label, code) ->
            val text = stringResource(label)
            Box(Modifier.size(52.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = text) { onKey(code) }
                .semantics { contentDescription = text }, contentAlignment = Alignment.Center) {
                HomeGlyph(glyph, null, tint = ink.onNight, size = 22.dp)
            }
        }
    }
}

/** Volume as a thread with a bead: − and + step the media stream, the chip flips sound and vibrate. */
@Composable
private fun VolumeRow(level: Float, ringer: Int, onStep: (Boolean) -> Unit, onRinger: () -> Unit) {
    val ink = LocalInk.current
    val label = stringResource(R.string.pulse_volume)
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
        StepButton("−", stringResource(R.string.pulse_volume_down)) { onStep(false) }
        Canvas(Modifier.weight(1f).height(24.dp).semantics { contentDescription = label; stateDescription = "${(level * 100).toInt()} %" }) {
            val y = size.height / 2f
            val x = size.width * level
            drawLine(ink.onNight.copy(alpha = .25f), Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(), StrokeCap.Round)
            drawLine(ink.accent, Offset(0f, y), Offset(x, y), 2.dp.toPx(), StrokeCap.Round)
            drawCircle(ink.accent, 5.dp.toPx(), Offset(x, y))
        }
        StepButton("+", stringResource(R.string.pulse_volume_up)) { onStep(true) }
        Spacer(Modifier.width(6.dp))
        Chip(stringResource(if (ringer == RINGER_NORMAL) R.string.pulse_ring else R.string.pulse_vibrate), ringer == RINGER_NORMAL,
            Modifier.width(96.dp), onRinger)
    }
}

@Composable
private fun StepButton(text: String, label: String, onClick: () -> Unit) {
    val ink = LocalInk.current
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Text(text, style = InkText.item, color = ink.onNight)
    }
}

/** A ring-and-thread chip: an outline at rest, terracotta outline and bead while on. */
@Composable
private fun Chip(text: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val ink = LocalInk.current
    Row(modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp))
        .border(1.dp, if (on) ink.accent else ink.onNight.copy(alpha = .28f), RoundedCornerShape(22.dp))
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (on) Box(Modifier.padding(end = 6.dp).size(6.dp).clip(CircleShape).background(ink.accent))
        Text(text, style = InkText.small, color = ink.onNight, maxLines = 1)
    }
}

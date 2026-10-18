package dev.intelia.launcher.ui

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.PowerManager
import android.view.MotionEvent
import dev.intelia.launcher.action.KeyRemap
import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.TextView
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
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.intelia.launcher.InteliaApp
import dev.intelia.launcher.MainActivity
import dev.intelia.launcher.R
import dev.intelia.launcher.pulse.MediaWatch
import dev.intelia.launcher.action.Action
import dev.intelia.launcher.action.ActionResult
import dev.intelia.launcher.action.Gesture
import dev.intelia.launcher.action.Reason
import dev.intelia.launcher.action.Trigger
import dev.intelia.launcher.data.pulseMode
import dev.intelia.launcher.pulse.PulseGestures
import dev.intelia.launcher.pulse.PulseMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
    /** System actions only: without Home's model, apps and FLOWs report missing and stay on Home (M3). */
    private var host: AndroidActionHost? = null
    private var scope: CoroutineScope? = null
    /** Keyguard up or screen off: no lens target, nothing above the lock screen (M3.1). */
    private var locked = false
    private var screenReceiver: BroadcastReceiver? = null
    private var note: View? = null
    /** M3.2 compact music window (not touchable; the lens target under it takes the touches) and its owner. */
    private var music: View? = null
    private var musicOwner: OverlayOwner? = null
    private var watching = false
    /** D20 key remap: only key codes and timers, nothing about what is typed; see [KeyRemap]. */
    private val remap = KeyRemap { code, g -> controls().bound(Trigger.Key(code, g)) != null }
    private val keyTimers = HashMap<Int, Runnable>()
    /** M3.3 Global ARC window (not touchable while a finger drives it: the lens keeps that touch) and what it shows. */
    private var arcView: View? = null
    private var arcOwner: OverlayOwner? = null
    private val arcUi = MutableStateFlow<GlobalArcUi?>(null)

    override fun onServiceConnected() {
        windows = getSystemService(WindowManager::class.java)
        _running.value = true
        instance = this
        host = AndroidActionHost(this, null) { openOnHome(Action.Search) }.also { h ->
            getSystemService(CameraManager::class.java)?.registerTorchCallback(h.torchCallback, main)
        }
        locked = getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                locked = when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> true
                    Intent.ACTION_USER_PRESENT -> false
                    else -> getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
                }
                updateLens()
            }
        }.also { r ->
            ContextCompat.registerReceiver(this, r, IntentFilter(Intent.ACTION_SCREEN_OFF).apply {
                addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
            }, ContextCompat.RECEIVER_NOT_EXPORTED)
        }
        // Event driven: the lens follows the owner's mode (Off / Home / Everywhere), no polling.
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { s ->
            s.launch {
                (application as InteliaApp).settings.state.map { (it.pulseMode() == PulseMode.Everywhere) to it.pulseMusic }.distinctUntilChanged()
                    .collect { updateLens() }
            }
            s.launch { MediaWatch.now.collect { showMusic() } }
        }
        relayout = { main.post { if (lens != null) placeLens() } }
        applyKeyFilter()
    }

    private fun controls() = (application as InteliaApp).settings.state.value.controls

    /** D20: the key filter is requested only while the owner has it on and a key is bound; otherwise no key passes us. */
    private fun applyKeyFilter() {
        val c = controls()
        val want = c.keysEverywhere && c.keys.any(c::keyBound)
        val info = serviceInfo ?: return
        val has = info.flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS != 0
        if (want == has) return
        info.flags = if (want) info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            else info.flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS.inv()
        serviceInfo = info
        if (!want) keyEffects(remap.reset())
    }

    /**
     * D20: a bound key is taken and turned into its gesture; every other key, and every key while Intelia's search
     * is being typed in or the screen is off, goes on untouched. Nothing is logged or stored.
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        val c = controls()
        val code = event.keyCode
        if (!c.keysEverywhere || typing || !remap.takes(code)) return false
        if (getSystemService(PowerManager::class.java)?.isInteractive != true) { keyEffects(remap.reset()); return false }
        when (event.action) {
            KeyEvent.ACTION_DOWN -> keyEffects(remap.down(code, event.repeatCount))
            KeyEvent.ACTION_UP -> keyEffects(remap.up(code))
        }
        return true
    }

    private fun keyEffects(effects: List<KeyRemap.Effect>): Unit = effects.forEach { e ->
        when (e) {
            is KeyRemap.Effect.Run -> controls().bound(Trigger.Key(e.code, e.gesture))?.let { a ->
                lens?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                runBound(a)
            }
            is KeyRemap.Effect.Replay -> replay(e.code)
            is KeyRemap.Effect.StartHold -> keyTimer(HOLD, ViewConfiguration.getLongPressTimeout().toLong()) { keyEffects(remap.hold(e.code)) }
            KeyRemap.Effect.CancelHold -> keyTimers.remove(HOLD)?.let(main::removeCallbacks)
            is KeyRemap.Effect.StartTap -> keyTimer(TAP, ViewConfiguration.getDoubleTapTimeout().toLong()) { keyEffects(remap.tapTimeout(e.code)) }
            KeyRemap.Effect.CancelTap -> keyTimers.remove(TAP)?.let(main::removeCallbacks)
        }
    }

    private fun keyTimer(slot: Int, ms: Long, run: () -> Unit) {
        keyTimers.remove(slot)?.let(main::removeCallbacks)
        val r = Runnable { keyTimers.remove(slot); run() }
        keyTimers[slot] = r
        main.postDelayed(r, ms)
    }

    /** What a taken key does without Intelia, for the presses the owner left unbound: volume steps, media keys. */
    private fun replay(code: Int) {
        val audio = getSystemService(AudioManager::class.java) ?: return
        when (code) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> audio.adjustSuggestedStreamVolume(
                if (code == KeyEvent.KEYCODE_VOLUME_UP) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI)
            KeyEvent.KEYCODE_VOLUME_MUTE -> audio.adjustSuggestedStreamVolume(AudioManager.ADJUST_TOGGLE_MUTE,
                AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI)
            else -> if (KeyEvent.isMediaSessionKey(code)) {
                audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
                audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
            }
        }
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
        closeArc()
        closeMenu()
        watch(false)
        hideNote()
        lens?.let { runCatching { windows.removeView(it) } }
        lens = null
        main.removeCallbacks(gestureTimer)
        gestures.reset()
        screenReceiver?.let { runCatching { unregisterReceiver(it) } }
        screenReceiver = null
        scope?.cancel()
        scope = null
        keyEffects(remap.reset())
        host?.let { h -> getSystemService(CameraManager::class.java)?.unregisterTorchCallback(h.torchCallback) }
        host = null
        relayout = null
        // A rebind after an update can connect the new instance before the old one is destroyed; only the current
        // instance may report the service gone, or Studio would say "off" while the lens runs.
        if (instance === this) {
            instance = null
            _running.value = false
        }
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

    /** The lens target exists only in Everywhere mode with the phone unlocked; otherwise there is no overlay at all. */
    private fun updateLens() {
        val settings = (application as InteliaApp).settings.state.value
        val everywhere = settings.pulseMode() == PulseMode.Everywhere && !locked
        if (everywhere) placeLens() else removeLens()
        watch(everywhere && settings.pulseMusic)
    }

    /** M3.2: the media session is watched only while the compact capsule can show (Everywhere, unlocked, music on). */
    private fun watch(on: Boolean) {
        if (on != watching) {
            watching = on
            if (on) MediaWatch.acquire(this) else MediaWatch.release()
        }
        showMusic()
    }

    private fun showMusic() {
        val shown = MediaWatch.now.value?.takeIf { watching && it.info.visible && menu == null }
        if (shown == null) {
            music?.let { runCatching { windows.removeView(it) } }
            musicOwner?.stop()
            music = null
            musicOwner = null
            return
        }
        if (music != null) return // the ComposeView follows MediaWatch by itself
        val h = hole()
        val mc = musicCapsule(h.x / dp, h.y / dp, h.radius / dp, statusBarPx() / dp)
        val o = OverlayOwner().also { it.start() }
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(o)
            setViewTreeSavedStateRegistryOwner(o)
            setContent {
                val settings by (application as InteliaApp).settings.state.collectAsState()
                val now by MediaWatch.now.collectAsState()
                InteliaTheme(settings) { now?.let { MusicCompact(it, mc, h.radius, settings.pulseOpacity, null) } }
            }
        }
        val params = overlay((mc.width * dp).toInt(), (mc.height * dp).toInt(), focusable = false).apply {
            x = (mc.left * dp).toInt(); y = (mc.top * dp).toInt()
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            title = "Intelia PULSE music"
        }
        windows.addView(view, params)
        music = view
        musicOwner = o
    }

    private fun removeLens() {
        closeArc()
        main.removeCallbacks(gestureTimer)
        gestures.reset()
        closeMenu()
        lens?.let { runCatching { windows.removeView(it) } }
        lens = null
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
            // M3.1: every touch goes through the shared recognizer (as on Home); click and long click are only the
            // TalkBack paths, the touch listener takes all real touches.
            setOnTouchListener { _, e -> touch(e); true }
            setOnClickListener { gesture(Gesture.Tap) }
            setOnLongClickListener { if (bound(Gesture.Hold) == null) false else { gesture(Gesture.Hold, assisted = true); true } }
            // Read at query time, so TalkBack offers exactly the gestures bound now: double tap as a custom action,
            // long press only when hold is bound.
            ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    if (bound(Gesture.Hold) == null) {
                        info.removeAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK)
                        info.isLongClickable = false
                    }
                    PULSE_A11Y.forEach { (g, id, label) ->
                        if (bound(g) != null) info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat(id, getString(label)))
                    }
                }

                override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
                    val g = PULSE_A11Y.firstOrNull { it.second == action }?.first ?: return super.performAccessibilityAction(host, action, args)
                    gesture(g, assisted = true)
                    return true
                }
            })
        }
        if (lens == null) {
            windows.addView(view, params)
            lens = view
        } else {
            windows.updateViewLayout(view, params)
        }
    }

    private val gestures by lazy {
        val vc = ViewConfiguration.get(this)
        PulseGestures({ bound(it) != null }, PulseGestures.Timing(ViewConfiguration.getDoubleTapTimeout().toLong(),
            ViewConfiguration.getLongPressTimeout().toLong(), vc.scaledTouchSlop / dp))
    }
    private val gestureTimer = Runnable { gestureOut(gestures.timeout()) }

    private fun touch(e: MotionEvent) {
        val x = e.rawX / dp
        val y = e.rawY / dp
        gestureOut(when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> gestures.down(x, y)
            MotionEvent.ACTION_MOVE -> gestures.move(x, y)
            MotionEvent.ACTION_UP -> gestures.up(x, y)
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> gestures.cancel()
            else -> return
        })
        // M3.3: while Global ARC is open the held finger picks a slot; lifting runs it, anything else runs nothing.
        val ui = arcUi.value?.takeIf { !it.assisted } ?: return
        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> if (gestures.holding) {
                val next = GlobalArcGeometry.pick(ui.bow, ui.camera, x, y, ui.content.filled(ui.set), ui.pick)
                if (next != null && next != ui.pick && ui.content.look.haptics) lens?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                arcUi.value = ui.copy(pick = next)
            }
            MotionEvent.ACTION_UP -> {
                val chosen = ui.pick?.let { ui.content.sets.getOrNull(ui.set)?.slots?.getOrNull(it) }
                closeArc()
                chosen?.let { lens?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); runFromArc(it.action) }
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> closeArc()
        }
    }

    /**
     * M3.3: opens Global ARC on [arc]'s set under the camera, in every app and on Home. Slots resolve through the app
     * repository and the system's shortcuts, read now; a TalkBack long press opens it with slots as buttons.
     */
    private fun openArc(arc: Action.Arc, assisted: Boolean) {
        val h = host ?: return
        closeMenu()
        val app = application as InteliaApp
        val settings = app.settings.state.value
        val controls = settings.controls
        val shortcuts = if (h.isDefaultHome()) h.refreshShortcuts() else emptyList()
        val content = ArcContent.of(controls, app.apps.apps.value.orEmpty(), settings.aliases, shortcuts)
        val set = content.sets.indexOfFirst { it.id == controls.arcSetFor(arc).id }.coerceAtLeast(0)
        val hole = hole()
        val camera = Pt(hole.x / dp, hole.y / dp)
        val (w, hgt) = if (Build.VERSION.SDK_INT >= 30) windows.currentWindowMetrics.bounds.let { it.width() to it.height() }
            else resources.displayMetrics.let { it.widthPixels to it.heightPixels }
        val bow = GlobalArcGeometry.bow(content.look, camera, w / dp, hgt / dp)
        arcUi.value = GlobalArcUi(content, set, camera, bow, null, assisted)
        if (arcView != null) return
        val o = OverlayOwner().also { it.start() }
        val root = object : FrameLayout(this) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode != KeyEvent.KEYCODE_BACK) return super.dispatchKeyEvent(event)
                if (event.action == KeyEvent.ACTION_UP) closeArc()
                return true
            }
        }
        root.setViewTreeLifecycleOwner(o)
        root.setViewTreeSavedStateRegistryOwner(o)
        root.addView(ComposeView(this).apply {
            setContent {
                val s by app.settings.state.collectAsState()
                val ui by arcUi.collectAsState()
                InteliaTheme(s) {
                    ui?.let { u ->
                        val labels = remember(u.content) { app.apps.apps.value.orEmpty().associate { it.key to it.label } }
                        val shown = u.content.sets.getOrNull(u.set)
                        GlobalArc(shown, u.content.look, u.camera, true, u.pick, { alias -> alias.targets.map(labels::get) },
                            onSlot = if (u.assisted) { i -> val chosen = shown?.slots?.getOrNull(i); closeArc(); chosen?.let { runFromArc(it.action) } }
                            else null,
                            onDismiss = ::closeArc, overApps = home == null)
                    }
                }
            }
        })
        // A finger drives it: the window lets every touch through. Opened by TalkBack it takes touches and Back instead.
        val params = overlay(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, focusable = assisted).apply {
            if (!assisted) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            title = "Intelia PULSE arc"
        }
        windows.addView(root, params)
        arcView = root
        arcOwner = o
    }

    private fun closeArc() {
        arcUi.value = null
        arcView?.let { runCatching { windows.removeView(it) } }
        arcOwner?.stop()
        arcView = null
        arcOwner = null
    }

    /**
     * A released Global ARC slot. With Home in front Home runs it exactly as LENS-ARC would; elsewhere apps, FLOWs and
     * shortcuts start through `LauncherApps`, Search and cards bring Home forward first. A FLOW was previewed step by
     * step while its slot was picked; release is the explicit action that runs it.
     */
    private fun runFromArc(action: Action) {
        home?.let { it(action); return }
        if (action is Action.Card) { openOnHome(action); return }
        val result = action.run(host ?: return)
        if (result is ActionResult.Unavailable) toast(when (result.reason) {
            Reason.AppMissing -> R.string.action_app_missing
            Reason.FlowMissing -> R.string.action_flow_missing
            Reason.NotDefaultHome -> R.string.action_not_default_home
            Reason.ShortcutMissing -> R.string.action_shortcut_missing
            Reason.Unsupported -> R.string.action_unsupported
            else -> R.string.launch_failed
        })
    }

    /** Hands [action] to Home and brings Home forward; Home runs it once in front (in-process, never an intent extra). */
    private fun openOnHome(action: Action): Boolean {
        _homeRequest.value = action
        return try {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            _homeRequest.value = null
            false
        }
    }

    private fun gestureOut(out: PulseGestures.Out) {
        if (out.timer != PulseGestures.KEEP) {
            main.removeCallbacks(gestureTimer)
            out.timer?.let { main.postDelayed(gestureTimer, it) }
        }
        val g = out.run ?: return
        lens?.performHapticFeedback(if (g == Gesture.Hold || g == Gesture.DoubleTapHold) HapticFeedbackConstants.LONG_PRESS
            else HapticFeedbackConstants.VIRTUAL_KEY)
        gesture(g)
    }

    private fun bound(g: Gesture): Action? = (application as InteliaApp).settings.state.value.controls.bound(Trigger.Pulse(g))

    /**
     * M3: a bound gesture runs its action; an unbound tap opens the menu as before. With Home in front Home runs
     * it (FLOW keeps its preview rules there); elsewhere only system actions run, the rest says it needs Home.
     */
    private fun gesture(g: Gesture, assisted: Boolean = false) {
        val action = bound(g) ?: return if (g == Gesture.Tap) openMenu() else Unit
        if (action is Action.Arc) openArc(action, assisted) else runBound(action)
    }

    private fun runBound(action: Action) {
        home?.let { it(action); return }
        if (!action.outsideHome) return toast(R.string.pulse_needs_home)
        val result = action.run(host ?: return)
        if (result is ActionResult.Unavailable) toast(if (result.reason == Reason.Unsupported) R.string.action_unsupported else R.string.launch_failed)
    }

    /**
     * A short note under the camera in the service's own overlay. A Toast is not used: Android drops toasts from an
     * app whose notifications are off, and then the owner would get no answer at all.
     */
    private fun toast(text: Int) {
        hideNote()
        val view = TextView(this).apply {
            setText(text)
            setTextColor(Color.WHITE)
            textSize = 13f
            val pad = (12 * dp).toInt()
            setPadding(pad + pad / 2, pad, pad + pad / 2, pad)
            background = GradientDrawable().apply { cornerRadius = 20 * dp; setColor(Color.argb(235, 16, 16, 24)) }
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        val params = overlay(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, focusable = false).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (statusBarPx() + 12 * dp).toInt()
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        windows.addView(view, params)
        note = view
        main.postDelayed({ if (note === view) hideNote() }, NOTE_MS)
    }

    private fun hideNote() {
        note?.let { runCatching { windows.removeView(it) } }
        note = null
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
                    PulseMenuSheet(hole, statusBar, width, settings.pulseMusic, ::act, ::open, ::closeMenu)
                }
            }
        })
        windows.addView(root, overlay(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, focusable = true))
        menu = root
        owner = o
        showMusic()
    }

    private fun closeMenu() {
        menu?.let { runCatching { windows.removeView(it) } }
        owner?.stop()
        menu = null
        owner = null
        if (watching) showMusic()
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

        private const val NOTE_MS = 2200L
        private const val HOLD = 0
        private const val TAP = 1

        /** True while Intelia's search field takes text: no key is remapped then (nothing runs while typing). */
        @Volatile var typing = false

        /** Settings changed (key bindings or the D20 switch): the running service re-requests or drops the key filter. */
        fun refreshKeys() { instance?.let { s -> s.main.post { s.applyKeyFilter() } } }
        @Volatile private var editing = false

        private val _homeRequest = MutableStateFlow<Action?>(null)

        /** An action the service handed to Home (Search or a card from Global ARC); Home runs it and calls [consumed]. */
        val homeRequest: StateFlow<Action?> = _homeRequest.asStateFlow()

        fun consumed() { _homeRequest.value = null }

        /** Set by Home while it is resumed: bound PULSE gestures then run through Home's Action Engine entry. */
        @Volatile var home: ((Action) -> Unit)? = null
        @Volatile private var instance: PulseAccessibilityService? = null

        /**
         * A system action for the Action Engine (lock, screenshot, notifications, Quick Settings). False when the
         * service is off or the platform refused; never queued for later.
         */
        fun performGlobal(action: Int): Boolean = instance?.performGlobalAction(action) ?: false
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
    hole: Hole, statusBar: Float, screenWidth: Float, musicOn: Boolean,
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
                    // M3.2: the followed session first, with its own controls; without it the media keys row stays below.
                    val now = rememberPlaying(musicOn)?.takeIf { it.info.visible }
                    now?.let { MusicCard(it) }
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
                    if (now == null) MediaRow(playing) { code ->
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
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
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

/** PULSE gestures TalkBack offers as custom actions while they are bound (tap is the click, hold the long click). */
private val PULSE_A11Y = listOf(
    Triple(Gesture.DoubleTap, R.id.pulse_double_tap, R.string.pulse_gesture_double),
    Triple(Gesture.SwipeDown, R.id.pulse_swipe_down, R.string.pulse_gesture_down),
    Triple(Gesture.SwipeLeft, R.id.pulse_swipe_left, R.string.pulse_gesture_left),
    Triple(Gesture.SwipeRight, R.id.pulse_swipe_right, R.string.pulse_gesture_right),
    Triple(Gesture.DoubleTapHold, R.id.pulse_double_hold, R.string.pulse_gesture_double_hold),
)

/** What the service's Global ARC shows: the content, the open set, the camera (dp), its bow and the picked slot. */
private data class GlobalArcUi(val content: ArcContent, val set: Int, val camera: Pt, val bow: ArcBow, val pick: Int?, val assisted: Boolean)

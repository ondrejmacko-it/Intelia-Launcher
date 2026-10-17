package dev.intelia.launcher.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import dev.intelia.launcher.data.Motion
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The one shared depth layer under an open surface (D15 revision 1): Light leaves Home as it is, Dense (sheets)
 * softens it by 5 dp, Focus (Search, drawer) blurs it by 18 dp and scales it to 97 %; [veilScrim] tints it.
 * Surfaces report how far they are open ([VeilDepthSource]); Home reads it only in its graphics layer, so an
 * opening surface redraws Home's layer without recomposing it.
 *
 * Two different blurs, never stacked: this one is [BlurEffect] (`RenderEffect`, API 31+) on Intelia's own Home
 * content, so it works on the Nothing Phone (3) too. The wallpaper lies behind the window and is blurred only by
 * the system's cross-window blur ([rememberCrossWindowBlur]), which the Phone (3) does not offer.
 */
@Stable
class VeilDepth {
    private val focus = mutableStateMapOf<Any, Float>()
    private val dense = mutableStateMapOf<Any, Float>()

    /** How far the most open Focus surface is, 0..1. */
    val focusAmount: Float get() = focus.values.maxOrNull() ?: 0f
    val denseAmount: Float get() = dense.values.maxOrNull() ?: 0f

    internal fun report(key: Any, level: VeilLevel, amount: Float) {
        val v = amount.coerceIn(0f, 1f)
        when (level) {
            VeilLevel.Focus -> focus[key] = v
            VeilLevel.Dense -> dense[key] = v
            VeilLevel.Light -> Unit // Light leaves Home unchanged
        }
    }

    internal fun clear(key: Any) { focus.remove(key); dense.remove(key) }
}

val LocalVeilDepth = staticCompositionLocalOf<VeilDepth?> { null }

private const val DENSE_BLUR_DP = 5f
private const val FOCUS_BLUR_DP = 18f
private const val FOCUS_SCALE = .97f
/** The scrim's strength under Dense; Focus draws it whole. */
const val DENSE_SCRIM = .6f

/** Blur radius in px for the backdrop, whole pixels so an animation does not build a new effect per sub-pixel. */
internal fun backdropBlurPx(focus: Float, dense: Float, density: Float): Int =
    (max(FOCUS_BLUR_DP * focus, DENSE_BLUR_DP * dense) * density).roundToInt()

/**
 * Home under an open surface: blurred ([blur]), and scaled back a little under Focus; the scrim is the surface's own.
 * Light surfaces leave it untouched.
 */
fun Modifier.veilBackdrop(depth: VeilDepth?, blur: Boolean): Modifier = if (depth == null) this else graphicsLayer {
    val f = depth.focusAmount
    val s = 1f - (1f - FOCUS_SCALE) * f
    scaleX = s; scaleY = s
    transformOrigin = TransformOrigin(.5f, .52f)
    val r = if (blur && Build.VERSION.SDK_INT >= 31) backdropBlurPx(f, depth.denseAmount, density).toFloat() else 0f
    renderEffect = if (r >= 1f) BlurEffect(r, r, TileMode.Decal) else null
}

/**
 * Whether the backdrop may blur Home. The blur costs GPU time only while a surface opens or closes (Phone (3), 5×
 * Search: janky frames 9.4 → 12.0 %, p95 27 → 34 ms), so it is skipped in battery saver and with Reduce motion;
 * the tinted scrim alone then gives the depth.
 */
@Composable
fun rememberBackdropBlur(): Boolean {
    if (Build.VERSION.SDK_INT < 31 || LocalMotion.current == Motion.Off) return false
    val context = LocalContext.current
    val power = remember(context) { context.getSystemService(PowerManager::class.java) }
    var saving by remember(power) { mutableStateOf(power?.isPowerSaveMode == true) }
    DisposableEffect(power) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { saving = power?.isPowerSaveMode == true }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return !saving
}

/** The tinted scrim of the depth layer: the theme's base at 52 % with 8 % of the accent (prototype `.scrim`). */
@Composable
fun veilScrim(): Color {
    val c = LocalCrystal.current
    return remember(c.dark, c.acc) { veilScrimColor(c.dark, c.acc) }
}

internal fun veilScrimColor(dark: Boolean, acc: Color): Color {
    val base = if (dark) Color(0xFF111317) else Color(0xFFF7F8FA)
    val t = .08f
    val alpha = t + .52f * (1f - t)
    return lerp(base, acc, t / alpha).copy(alpha = alpha)
}

/** Reports a surface's openness ([amount], read per frame) to the shared depth layer while it is composed. */
@Composable
fun VeilDepthSource(level: VeilLevel, amount: () -> Float) {
    val depth = LocalVeilDepth.current ?: return
    val key = remember { Any() }
    LaunchedEffect(depth) { snapshotFlow(amount).collect { depth.report(key, level, it) } }
    DisposableEffect(depth) { onDispose { depth.clear(key) } }
}

/**
 * Whether the system blurs what lies behind Intelia's window (API 31+, `WindowManager.isCrossWindowBlurEnabled`,
 * which also turns off in battery saver). False on the Nothing Phone (3), which reports no background blur.
 */
@Composable
fun rememberCrossWindowBlur(): Boolean {
    if (Build.VERSION.SDK_INT < 31) return false
    val activity = LocalContext.current.findActivity() ?: return false
    val wm = activity.windowManager
    var enabled by remember(wm) { mutableStateOf(wm.isCrossWindowBlurEnabled) }
    DisposableEffect(wm) {
        val listener = Consumer<Boolean> { enabled = it }
        wm.addCrossWindowBlurEnabledListener(listener)
        onDispose { wm.removeCrossWindowBlurEnabledListener(listener) }
    }
    return enabled
}

/** Blurs the wallpaper behind the window while [on]; only called where [rememberCrossWindowBlur] is true. */
@Composable
fun WindowBackdropBlur(on: Boolean, radiusPx: Int) {
    if (Build.VERSION.SDK_INT < 31) return
    val window = LocalContext.current.findActivity()?.window ?: return
    LaunchedEffect(on, radiusPx) { window.setBackgroundBlurRadius(if (on) radiusPx else 0) }
    DisposableEffect(window) { onDispose { window.setBackgroundBlurRadius(0) } }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

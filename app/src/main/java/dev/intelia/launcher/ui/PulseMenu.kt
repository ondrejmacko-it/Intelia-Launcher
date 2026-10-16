package dev.intelia.launcher.ui

/**
 * The global PULSE menu (D9) as plain Kotlin: which controls exist on this device and where the windows
 * sit. Every value is in px of the screen; the service converts dp before calling.
 */
internal enum class PulseAction(val minSdk: Int = 26) {
    Torch, Screenshot(28), Lock(28), Notifications, QuickSettings,
}

internal enum class PulseLink(val minSdk: Int = 26) {
    Wifi(29), Internet(29), Bluetooth, Sound, Display,
}

/** Tiles in menu order; the torch only with a back flash, global actions only where the API exists. */
internal fun pulseActions(sdk: Int, torch: Boolean): List<PulseAction> =
    PulseAction.entries.filter { sdk >= it.minSdk && (it != PulseAction.Torch || torch) }

internal fun pulseLinks(sdk: Int): List<PulseLink> = PulseLink.entries.filter { sdk >= it.minSdk }

internal data class PulseBox(val left: Int, val top: Int, val width: Int, val height: Int)

/** The lens overlay: a square centred on the lens, never above the screen top, at least [min] wide. */
internal fun lensTarget(x: Float, y: Float, r: Float, pad: Float, min: Float): PulseBox {
    val side = maxOf(2f * (r + pad), min).toInt()
    return PulseBox((x - side / 2f).toInt(), (y - side / 2f).toInt().coerceAtLeast(0), side, side)
}

/** The menu: centred under the lens, at most [maxWidth], kept [margin] off both screen edges. */
internal fun menuLeftAndWidth(screenWidth: Float, lensX: Float, margin: Float, maxWidth: Float): Pair<Float, Float> {
    val width = minOf(screenWidth - 2f * margin, maxWidth).coerceAtLeast(0f)
    val left = (lensX - width / 2f).coerceIn(margin, (screenWidth - margin - width).coerceAtLeast(margin))
    return left to width
}

/**
 * Top of the menu sheet: below the status bar and below the lens ring, plus [gap], so the system icons
 * stay visible. A thin thread joins the lens to this edge.
 */
internal fun menuTop(statusBar: Float, lensY: Float, lensRadius: Float, ring: Float, gap: Float): Float =
    maxOf(statusBar, lensY + lensRadius + ring) + gap

/** Sound ↔ vibrate. Silent is never chosen: it changes Do Not Disturb, which needs policy access. */
internal fun nextRinger(mode: Int): Int = if (mode == RINGER_NORMAL) RINGER_VIBRATE else RINGER_NORMAL

/** Volume level as a 0..1 fraction for the thread; guards against a zero or missing maximum. */
internal fun volumeFraction(level: Int, max: Int): Float = if (max <= 0) 0f else (level.toFloat() / max).coerceIn(0f, 1f)

// Same values as AudioManager.RINGER_MODE_*, kept here so the logic stays JVM-testable.
internal const val RINGER_SILENT = 0
internal const val RINGER_VIBRATE = 1
internal const val RINGER_NORMAL = 2

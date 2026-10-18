package dev.intelia.launcher.ui

// S3 PULSE glance geometry, as plain Kotlin. Every value is in px of the screen unless named.

/** The menu: centred under the lens, at most [maxWidth], kept [margin] off both screen edges. */
internal fun menuLeftAndWidth(screenWidth: Float, lensX: Float, margin: Float, maxWidth: Float): Pair<Float, Float> {
    val width = minOf(screenWidth - 2f * margin, maxWidth).coerceAtLeast(0f)
    val left = (lensX - width / 2f).coerceIn(margin, (screenWidth - margin - width).coerceAtLeast(margin))
    return left to width
}

/**
 * Top of the menu sheet: below the status bar and below the lens ring, plus [gap], so the system icons
 * stay visible. A translucent strip joins the lens to this edge.
 */
internal fun menuTop(statusBar: Float, lensY: Float, lensRadius: Float, ring: Float, gap: Float): Float =
    maxOf(statusBar, lensY + lensRadius + ring) + gap

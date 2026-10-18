package dev.intelia.launcher.ui

/**
 * S3 PULSE glance: the small context panel that opens from the lens, as plain Kotlin. With music it is the song and
 * nothing else; without music a single row of what is relevant now. Every value is in px of the screen unless named.
 */
internal enum class GlanceItem(val minSdk: Int = 26) { Torch, Alarm, Battery, Notifications, Screenshot(28) }

/**
 * What the glance offers besides the song: nothing while music shows; else the torch (with a back flash), the next
 * alarm, the battery only while charging, and above other apps (the service, [global]) notifications and screenshot.
 */
internal fun glanceItems(music: Boolean, torch: Boolean, charging: Boolean, global: Boolean, sdk: Int): List<GlanceItem> {
    if (music) return emptyList()
    return GlanceItem.entries.filter { item ->
        sdk >= item.minSdk && when (item) {
            GlanceItem.Torch -> torch
            GlanceItem.Alarm -> true
            GlanceItem.Battery -> charging
            GlanceItem.Notifications, GlanceItem.Screenshot -> global
        }
    }
}

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

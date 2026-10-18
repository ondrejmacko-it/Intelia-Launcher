package dev.intelia.launcher.ui

/**
 * What the PULSE capsule shows at rest (D8). The system already shows the battery percentage and the
 * alarm icon, so the capsule only adds a gauge on the lens rim and one dot for a near alarm.
 */
internal data class PulseRest(val gauge: Boolean, val alarmMark: Boolean) {
    val visible: Boolean get() = gauge || alarmMark
}

internal fun pulseRest(charging: Boolean, alarmAt: Long?, now: Long, alarmHours: Int): PulseRest =
    PulseRest(gauge = charging, alarmMark = alarmAt != null && alarmAt - now in 0..alarmHours * 3_600_000L)

/**
 * The rest capsule in dp, window coordinates. It is a disc around the camera, as tall as the lens plus
 * [PAD] on each side but never taller than the status bar, so it stays between the status icons. An
 * alarm mark adds [MARK] on the right only; the lens stays at [lensX] from the capsule's left edge.
 */
internal data class PulseCapsule(val left: Float, val top: Float, val width: Float, val height: Float) {
    val lensX: Float get() = height / 2f
    val markX: Float get() = width - MARK / 2f - 2f
    val right: Float get() = left + width
}

internal const val PAD = 6f
internal const val MARK = 10f

internal fun restCapsule(holeX: Float, holeY: Float, holeR: Float, statusBar: Float, mark: Boolean): PulseCapsule {
    val wanted = 2f * holeR + 2f * PAD
    // Stay inside the status bar (1 dp air) but never cover the lens itself.
    val height = if (statusBar > 0f) wanted.coerceAtMost(statusBar - 2f).coerceAtLeast(2f * holeR + 2f) else wanted
    val top = (holeY - height / 2f).let { if (statusBar >= height) it.coerceIn(0f, statusBar - height) else it }
    return PulseCapsule(holeX - height / 2f, top, height + if (mark) MARK else 0f, height)
}

internal const val BARS = 22f

/**
 * M3.2 compact music capsule in dp: the rest disc around the lens, [BARS] of sound bars on its left and a round
 * artwork as wide as the capsule is tall on its right. [lensX] is the lens centre from the capsule's left edge.
 */
internal data class MusicCapsule(val left: Float, val top: Float, val width: Float, val height: Float) {
    val lensX: Float get() = BARS + height / 2f
    val artX: Float get() = width - height / 2f
}

internal fun musicCapsule(holeX: Float, holeY: Float, holeR: Float, statusBar: Float): MusicCapsule {
    val rest = restCapsule(holeX, holeY, holeR, statusBar, mark = false)
    return MusicCapsule(rest.left - BARS, rest.top, BARS + 2f * rest.height, rest.height)
}

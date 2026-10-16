package dev.intelia.launcher.ui

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** A slot on an ARC ring. */
data class ArcPick(val ring: Int, val index: Int)

/**
 * Concentric ARC rings around LENS. Distance from LENS chooses the ring, angle chooses the slot.
 * Slots are fixed (an empty slot stays empty) so positions can become muscle memory.
 * Coordinates are dp relative to the LENS centre, negative y is up. Pure, no Android dependencies.
 *
 * @param slots per ring: true where a slot holds an item; the list size is the ring's capacity.
 * @param left distance from the LENS centre to the left screen edge.
 * @param right distance from the LENS centre to the right screen edge.
 */
class ArcRings(val slots: List<List<Boolean>>, left: Float, right: Float) {
    val angles: List<List<Float>> = slots.mapIndexed { ring, ringSlots ->
        val r = RADII[ring]
        val cap = SPANS[ring] / 2f
        val aL = min(cap, edgeAngle(left, r))
        val aR = min(cap, edgeAngle(right, r))
        val n = ringSlots.size
        if (n <= 1) listOf((aR - aL) / 2f) else List(n) { i -> -aL + i * (aL + aR) / (n - 1) }
    }

    fun radius(ring: Int) = RADII[ring]

    /** Slot centre in dp relative to LENS. */
    fun position(ring: Int, index: Int): Pair<Float, Float> {
        val a = angles[ring][index] * PI.toFloat() / 180f
        return RADII[ring] * sin(a) to -RADII[ring] * cos(a)
    }

    /** Which ring the finger is over, or null when back on LENS, below it or past the last ring. */
    fun ringAt(x: Float, y: Float): Int? {
        if (y > BELOW) return null
        val d = hypot(x, y)
        if (d < DEAD) return null
        val last = slots.lastIndex
        for (ring in 0..last) {
            val upper = if (ring == last) RADII[ring] + OUTER else (RADII[ring] + RADII[ring + 1]) / 2f
            if (d < upper) return ring
        }
        return null
    }

    /** Nearest filled slot under the finger; keeps [previous] while the finger stays near it. */
    fun select(x: Float, y: Float, previous: ArcPick? = null): ArcPick? {
        val ring = ringAt(x, y) ?: return null
        val a = (atan2(x, -y) * 180.0 / PI).toFloat()
        val ringAngles = angles[ring]
        val step = if (ringAngles.size > 1) abs(ringAngles[1] - ringAngles[0]) else SINGLE
        if (previous != null && previous.ring == ring && slots[ring].getOrElse(previous.index) { false } &&
            abs(ringAngles[previous.index] - a) <= step * KEEP
        ) return previous
        val best = ringAngles.indices.filter { slots[ring][it] }.minByOrNull { abs(ringAngles[it] - a) } ?: return null
        return if (abs(ringAngles[best] - a) <= step * HIT) ArcPick(ring, best) else null
    }

    companion object {
        val RADII = floatArrayOf(108f, 180f, 250f)
        private val SPANS = floatArrayOf(124f, 136f, 148f)
        const val DEAD = 54f
        const val BELOW = 28f
        private const val OUTER = 64f
        private const val EDGE = 34f
        private const val HIT = .6f
        private const val KEEP = .85f
        private const val SINGLE = 40f

        /** Largest angle from vertical that keeps a slot centre [EDGE] dp inside the screen. */
        private fun edgeAngle(extent: Float, r: Float): Float {
            val room = (extent - EDGE) / r
            return if (room >= 1f) 90f else if (room <= 0f) 0f else (asin(room) * 180.0 / PI).toFloat()
        }
    }
}

/** Slot indices of an [n]-slot ring from the centre outwards: centre first, then left and right alternately. */
fun centreOut(n: Int): List<Int> = (0 until n).sortedWith(compareBy({ abs(it - (n - 1) / 2f) }, { it }))

/** Places the i-th item into the i-th slot from the centre outwards; unused slots stay empty. */
fun <T : Any> fillFromCentre(items: List<T?>, n: Int): List<T?> {
    val slots = MutableList<T?>(n) { null }
    centreOut(n).zip(items).forEach { (slot, item) -> slots[slot] = item }
    return slots
}

/** Upward flick from LENS opens search; mostly vertical, tolerant of thumb arcs. */
fun lensSwipeSearch(x: Float, y: Float): Boolean = y <= -48f && abs(x) <= maxOf(72f, -y * .7f)

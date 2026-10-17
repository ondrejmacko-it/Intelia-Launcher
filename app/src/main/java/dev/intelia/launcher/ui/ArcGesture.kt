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

    /** Radius of the outermost ring that shows anything (dp); 0 when ARC is empty. */
    val reach: Float = angles.indices.filter { angles[it].isNotEmpty() }.maxOfOrNull { RADII[it] } ?: 0f

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

/**
 * The wide ARC (Control System M2): one flat arc of [count] slots just above LENS, spread across the room beside it,
 * plus set dots above it and two set arrows beside LENS. The arc is a segment of a large circle whose centre lies
 * [drop] dp below LENS, so its middle sits [lift] dp above LENS and its ends come down towards the LENS row.
 * Slots are addressed in priority order (0 = middle of the arc, then left and right alternately, [centreOut]);
 * an empty slot stays empty so positions become muscle memory. Coordinates are dp in ARC's local frame
 * (relative to the LENS centre, negative y away from the LENS edge). Pure, no Android dependencies.
 *
 * [radius] sets how high the middle sits (S/M/L), [span] how round the arc is. Every slot centre stays [EDGE] dp
 * inside the screen ([left]/[right] = room beside LENS); when slots would sit closer than [MIN_CHORD] the arc
 * first opens rounder, then grows, so targets stay ≥ 48 dp.
 */
class ArcBow(val count: Int, radius: Float, span: Float, val left: Float, val right: Float, val sets: Int = 1) {
    /** Height of the middle of the arc above LENS. */
    val lift: Float = radius - LIFT_OFFSET
    /** Radius of the circle the arc lies on. */
    val curve: Float
    /** How far below LENS that circle's centre lies. */
    val drop: Float get() = curve - lift
    /** Slot angles by position on screen, left to right, degrees from "up" around the circle's centre. */
    val angles: List<Float>

    init {
        val room = maxOf(left, right) - EDGE
        val half = if (count > 1) min(MAX_STEP * (count - 1) / 2f, room).coerceAtLeast(MIN_CHORD / 2f) else MIN_CHORD
        var theta = (span / 3f).coerceIn(20f, 60f)
        var r = maxOf(lift + 40f, half / sin(theta * PI.toFloat() / 180f))
        var a: List<Float>
        var guard = 0
        while (true) {
            val need = if (count > 1) (count - 1) * stepFor(r) else 0f
            val eL = min(MAX_HALF, edgeAngle(left, r))
            val eR = min(MAX_HALF, edgeAngle(right, r))
            var aL = min(theta, eL)
            var aR = min(theta, eR)
            // Clipped by one edge: the free side opens further so the slots keep their spacing.
            aR = min(maxOf(aR, need - aL), eR)
            aL = min(maxOf(aL, need - aR), eL)
            a = if (count <= 1) listOf((aR - aL) / 2f) else List(count) { i -> -aL + i * (aL + aR) / (count - 1) }
            val step = if (count > 1) (aL + aR) / (count - 1) else 360f
            if (chord(step, r) >= MIN_CHORD || ++guard > 60) break
            if (theta < MAX_HALF) theta += 4f else r += 12f
        }
        curve = r
        angles = a
    }

    private val order = centreOut(count)
    private val step = if (count > 1) abs(angles[1] - angles[0]) else SINGLE

    /** Angle of priority slot [slot]. */
    fun angle(slot: Int): Float = angles[order[slot]]

    /** Centre of priority slot [slot], dp relative to LENS. */
    fun position(slot: Int): Pair<Float, Float> = onArc(angle(slot))

    private fun onArc(degrees: Float): Pair<Float, Float> = polar(curve, degrees).let { (x, y) -> x to y + drop }

    /** Middle of the arc, where the dots and the label sit. */
    val middle: Float = if (angles.isEmpty()) 0f else (angles.first() + angles.last()) / 2f

    /** Centre of set dot [k]; the dots sit side by side above the middle of the arc, [gap] dp apart. */
    fun dot(k: Int, gap: Float = DOT_GAP): Pair<Float, Float> {
        val (mx, my) = onArc(middle)
        return mx + (k - (sets - 1) / 2f) * gap to my - DOTS
    }

    /** How far up the arc reaches (dp above LENS), including the dots when there is more than one set. */
    val reach: Float get() = lift + if (sets > 1) DOTS + 16f else OUTER

    /** How far sideways the arc reaches (dp from LENS). */
    val width: Float get() = angles.maxOfOrNull { abs(curve * sin(it * PI.toFloat() / 180f)) } ?: 0f

    /** Centre of the set arrow on side [side] (-1 left, +1 right), low beside LENS, or null (one set, or no room there). */
    fun arrow(side: Int): Pair<Float, Float>? {
        if (sets <= 1) return null
        val room = if (side < 0) left else right
        return if (room >= ARROW_X + ARROW_HIT) side * ARROW_X to ARROW_Y else null
    }

    /** -1 / +1 while the finger is on the left / right set arrow, otherwise 0. */
    fun arrowAt(x: Float, y: Float): Int =
        listOf(-1, 1).firstOrNull { side -> arrow(side)?.let { (ax, ay) -> hypot(x - ax, y - ay) <= ARROW_HIT } == true } ?: 0

    private fun angleOf(x: Float, y: Float) = (atan2(x, -(y - drop)) * 180.0 / PI).toFloat()

    private fun inBand(x: Float, y: Float): Boolean {
        if (y > BELOW || hypot(x, y) < DEAD) return false
        val d = hypot(x, y - drop)
        return d >= curve - INNER && d < curve + OUTER
    }

    /** Nearest filled slot under the finger ([filled] in priority order); keeps [previous] while the finger stays near it. */
    fun select(x: Float, y: Float, filled: List<Boolean>, previous: Int? = null): Int? {
        if (!inBand(x, y)) return null
        val a = angleOf(x, y)
        if (previous != null && filled.getOrElse(previous) { false } && abs(angle(previous) - a) <= step * KEEP) return previous
        val best = (0 until count).filter { filled.getOrElse(it) { false } }.minByOrNull { abs(angle(it) - a) } ?: return null
        return if (abs(angle(best) - a) <= step * HIT) best else null
    }

    /** The set dot under the finger, or null. */
    fun dotAt(x: Float, y: Float): Int? {
        if (sets <= 1) return null
        return (0 until sets).firstOrNull { k -> dot(k).let { (dx, dy) -> hypot(x - dx, y - dy) <= DOT_HIT } }
    }

    /**
     * One finger move while ARC is held: a set dot or a set arrow switches the set (an arrow only once per visit,
     * it re-arms when the finger leaves it); otherwise the slot under the finger. Back on LENS nothing is picked.
     */
    fun track(state: BowTrack, x: Float, y: Float, filled: (Int) -> List<Boolean>): BowTrack {
        dotAt(x, y)?.let { k -> return if (k == state.set) state.copy(pick = null) else BowTrack(k, null, armed = false) }
        val side = arrowAt(x, y)
        if (side != 0) return if (state.armed) BowTrack((state.set + side).mod(sets), null, armed = false) else state.copy(pick = null)
        return BowTrack(state.set, select(x, y, filled(state.set), state.pick), armed = true)
    }

    companion object {
        const val DEAD = 54f
        const val BELOW = 28f
        const val OUTER = 40f
        private const val INNER = 36f
        const val DOTS = 50f
        const val MIN_CHORD = 50f
        /** Widest spacing between neighbouring slots, so a short arc does not send the thumb across the screen. */
        private const val MAX_STEP = 76f
        private const val LIFT_OFFSET = 24f
        private const val MAX_HALF = 85f
        private const val DOT_GAP = 30f
        /** Dot spacing when they are buttons (TalkBack, keyboard): 48 dp targets that do not overlap. */
        const val DOT_GAP_ASSISTED = 52f
        private const val DOT_HIT = 18f
        /**
         * Set arrows sit low beside LENS, below its centre: the thumb travels up to the slots and never crosses
         * them on the way; reaching one takes a deliberate slide down and sideways.
         */
        const val ARROW_X = 72f
        const val ARROW_Y = 30f
        const val ARROW_HIT = 22f
        private const val EDGE = 30f
        private const val HIT = .6f
        private const val KEEP = .85f
        private const val SINGLE = 40f

        private fun polar(r: Float, degrees: Float): Pair<Float, Float> {
            val a = degrees * PI.toFloat() / 180f
            return r * sin(a) to -r * cos(a)
        }

        private fun chord(step: Float, r: Float) = 2f * r * sin(step / 2f * PI.toFloat() / 180f)

        /** Angle between slots whose centres are [MIN_CHORD] apart on radius [r]. */
        private fun stepFor(r: Float) = (2.0 * asin(min(1f, MIN_CHORD / 2f / r).toDouble()) * 180.0 / PI).toFloat()

        private fun edgeAngle(extent: Float, r: Float): Float {
            val room = (extent - EDGE) / r
            return if (room >= 1f) 90f else if (room <= 0f) 0f else (asin(room) * 180.0 / PI).toFloat()
        }
    }
}

/** Where a held ARC is: the shown set, the picked slot (priority order) and whether an end may switch the set again. */
data class BowTrack(val set: Int = 0, val pick: Int? = null, val armed: Boolean = true)

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

package dev.intelia.launcher.ui

import kotlin.math.abs
import kotlin.math.hypot

/**
 * Hold-and-slide selection over ARC slots. Coordinates are dp relative to the LENS centre,
 * negative y is up. Pure state machine, no Android dependencies.
 */
class ArcGesture(private val centers: List<Pair<Float, Float>?>) {
    var selected: Int? = null
        private set
    private var armed = false
    private var ended = false
    private var previous = 0f to 0f

    fun move(x: Float, y: Float, pointers: Int = 1): Int? {
        if (ended) return null
        if (pointers != 1) { cancel(); return null }
        if (y <= ARM_Y) armed = true
        val available = centers.indices.filter { centers[it] != null }
        val outside = y > ARM_Y || y < MAX_Y || available.none { abs(x - centers[it]!!.first) <= LANE }
        val direct = available.minByOrNull { distance(it, x, y) }?.takeIf { distance(it, x, y) <= HIT }
        // Fast drags are sampled; the pointer can cross a whole icon between two events.
        val crossed = available.minByOrNull { segmentDistance(centers[it]!!, previous, x to y) }
            ?.takeIf { segmentDistance(centers[it]!!, previous, x to y) <= CROSS }
        selected = if (!armed || outside) null
        else direct ?: selected?.takeIf { centers[it] != null && distance(it, x, y) <= KEEP } ?: crossed
        previous = x to y
        return selected
    }

    fun finish(): Int? {
        if (ended) return null
        ended = true
        return selected.also { selected = null }
    }

    fun cancel() { ended = true; selected = null }

    private fun distance(index: Int, x: Float, y: Float) = centers[index]!!.let { (cx, cy) -> hypot(x - cx, y - cy) }

    private companion object {
        const val ARM_Y = -28f
        const val MAX_Y = -280f
        const val LANE = 96f
        const val HIT = 38f
        const val CROSS = 30f
        const val KEEP = 150f
    }
}

private fun segmentDistance(point: Pair<Float, Float>, start: Pair<Float, Float>, end: Pair<Float, Float>): Float {
    val dx = end.first - start.first
    val dy = end.second - start.second
    val length = dx * dx + dy * dy
    val t = if (length == 0f) 0f else (((point.first - start.first) * dx + (point.second - start.second) * dy) / length).coerceIn(0f, 1f)
    return hypot(point.first - start.first - t * dx, point.second - start.second - t * dy)
}

/** Upward flick from LENS opens search; mostly vertical, tolerant of thumb arcs. */
fun lensSwipeSearch(x: Float, y: Float): Boolean = y <= -48f && abs(x) <= maxOf(72f, -y * .7f)

/** ARC slot centres in dp relative to the LENS centre: a shallow upward arc. */
fun arcCenters(count: Int, span: Float): List<Pair<Float, Float>> = List(count) { index ->
    val position = if (count <= 1) 0f else (index.toFloat() / (count - 1) - .5f) * 2f
    (position * span / 2f) to (-90f + 28f * position * position)
}

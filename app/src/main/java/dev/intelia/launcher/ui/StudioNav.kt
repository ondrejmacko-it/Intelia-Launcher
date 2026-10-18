package dev.intelia.launcher.ui

import kotlin.math.abs

/**
 * Studio's back stack (S1): Back returns to the page you came from, not to a fixed parent. Pure, so the JVM tests
 * cover it; an empty stack means Back leaves Studio for the place it was opened from (Home, the editor).
 */
object StudioStack {
    /** [stack] with [to] on top; a page already on the stack is returned to, never stacked twice. */
    fun <T> push(stack: List<T>, to: T): List<T> {
        val at = stack.indexOf(to)
        return if (at >= 0) stack.subList(0, at + 1).toList() else stack + to
    }

    /** [stack] without its top page. */
    fun <T> pop(stack: List<T>): List<T> = stack.dropLast(1)

    /** Saved as one string of page names, so it survives the activity being recreated. */
    fun save(stack: List<String>): String = stack.joinToString(",")

    /** The saved stack, keeping only names [known] accepts (a page renamed in an update is dropped, never a crash). */
    fun restore(saved: String, known: (String) -> Boolean): List<String> = saved.split(',').filter { it.isNotEmpty() && known(it) }
}

/** The three resting heights of the Studio sheet: a large preview, preview and settings together, settings only. */
enum class SheetStop { Preview, Balanced, Settings }

/**
 * Where the sheet's top edge rests for each [SheetStop], in px from the top of the room under Studio's header (0 = the
 * sheet fills the room, the preview is hidden). Derived from the real room, so no stop cuts a row in half.
 */
data class SheetStops(val preview: Float, val balanced: Float, val settings: Float = 0f) {
    fun top(stop: SheetStop): Float = when (stop) {
        SheetStop.Preview -> preview
        SheetStop.Balanced -> balanced
        SheetStop.Settings -> settings
    }

    /**
     * The stop a drag released at [top] settles on: a fling faster than [fling] px/s goes to the next stop its way
     * (positive velocity = down, towards the preview), a slow release to the nearest stop.
     */
    fun settle(top: Float, velocity: Float, fling: Float): SheetStop {
        val order = listOf(SheetStop.Settings to settings, SheetStop.Balanced to balanced, SheetStop.Preview to preview)
        if (abs(velocity) > fling) {
            val next = if (velocity > 0) order.firstOrNull { it.second > top + 1f } else order.lastOrNull { it.second < top - 1f }
            if (next != null) return next.first
        }
        return order.minBy { abs(it.second - top) }.first
    }

    companion object {
        /**
         * Stops for a room [room] px tall at [density] px per dp: Preview leaves the sheet at least 176 dp (its handle
         * and two rows), Balanced gives the preview a little under half but never less than 200 dp nor the sheet less
         * than 280 dp, Settings gives the sheet everything.
         */
        fun of(room: Float, density: Float): SheetStops {
            val peek = maxOf(176f * density, room * .24f)
            val preview = (room - peek).coerceAtLeast(0f)
            val lo = 200f * density
            val hi = room - 280f * density
            val balanced = (if (hi < lo) room / 2f else (room * .46f).coerceIn(lo, hi)).coerceAtMost(preview)
            return SheetStops(preview = preview, balanced = balanced)
        }
    }
}

package dev.intelia.launcher.ui

import dev.intelia.launcher.data.DockEdge
import dev.intelia.launcher.data.DockSpec
import dev.intelia.launcher.data.LensAt
import kotlin.math.abs

/**
 * The LENS row on any edge (§5.5, D13): where LENS sits, and how ARC turns so its rings always open towards the
 * screen centre. ARC is laid out in a local frame whose "up" points away from the edge; [toScreen] turns it.
 * Pure Kotlin, in dp.
 */
object Dock {
    /** LENS centre from the bottom edge (above the navigation bar) and from a side edge. */
    const val INSET = 52f
    /** LENS centre below the status bar on the top edge, clear of PULSE and of the editor's bar. */
    const val TOP_INSET = 96f
    /** Half the length of four slots beside LENS (62 + 3 × 46 dp by default): LENS at the start or end moves back by it. */
    fun halfRow(dock: DockSpec): Float = (dock.gap + 3f * dock.pitch) / 2f

    /**
     * LENS centre for [dock] with the owner's along-edge offset [dx] on a [width] × [height] screen whose status bar
     * ends at [top] and navigation bar starts [bottom] above the bottom edge.
     */
    fun centre(dock: DockSpec, dx: Float, width: Float, height: Float, top: Float, bottom: Float): Pt {
        val alongLen = if (dock.edge.vertical) height - top - bottom else width
        // Spread: LENS goes as far to its end as it may, the four slots share the rest of the edge.
        val half = if (dock.spread) alongLen else halfRow(dock)
        val shift = dx + when (dock.lensAt) { LensAt.Middle -> 0f; LensAt.Start -> -half; LensAt.End -> half }
        val room = (alongLen / 2f - LENS_EDGE).coerceAtLeast(0f)
        val along = shift.coerceIn(-room, room)
        return when (dock.edge) {
            DockEdge.Bottom -> Pt(width / 2f + along, height - bottom - INSET)
            DockEdge.Top -> Pt(width / 2f + along, top + TOP_INSET)
            DockEdge.Left -> Pt(INSET, top + alongLen / 2f + along)
            DockEdge.Right -> Pt(width - INSET, top + alongLen / 2f + along)
        }
    }

    /** A local ARC offset (x right, y down, "up" away from the edge) on screen. */
    fun toScreen(edge: DockEdge, x: Float, y: Float): Pt = when (edge) {
        DockEdge.Bottom -> Pt(x, y)
        DockEdge.Top -> Pt(-x, -y)
        DockEdge.Left -> Pt(-y, x)
        DockEdge.Right -> Pt(y, -x)
    }

    /** The inverse of [toScreen]: a finger offset from LENS in ARC's local frame. */
    fun toLocal(edge: DockEdge, x: Float, y: Float): Pt = when (edge) {
        DockEdge.Bottom -> Pt(x, y)
        DockEdge.Top -> Pt(-x, -y)
        DockEdge.Left -> Pt(y, -x)
        DockEdge.Right -> Pt(-y, x)
    }

    /** Clockwise screen rotation of the local frame, degrees (for arcs drawn with angles). */
    fun degrees(edge: DockEdge): Float = when (edge) {
        DockEdge.Bottom -> 0f
        DockEdge.Left -> 90f
        DockEdge.Top -> 180f
        DockEdge.Right -> -90f
    }

    /** Room beside LENS on ARC's local left and right, dp: what [ArcRings] fits its rings into. */
    fun room(edge: DockEdge, lens: Pt, width: Float, height: Float): Pair<Float, Float> = when (edge) {
        DockEdge.Bottom -> lens.x to width - lens.x
        DockEdge.Top -> width - lens.x to lens.x
        DockEdge.Left -> lens.y to height - lens.y
        DockEdge.Right -> height - lens.y to lens.y
    }

    fun rings(slots: List<List<Boolean>>, edge: DockEdge, lens: Pt, width: Float, height: Float): ArcRings =
        room(edge, lens, width, height).let { (l, r) -> ArcRings(slots, left = l, right = r) }

    /** The shallow ARC (M2) for [look] with [sets] sets around LENS at [lens]. */
    fun bow(look: dev.intelia.launcher.action.ArcLook, sets: Int, edge: DockEdge, lens: Pt, width: Float, height: Float): ArcBow =
        room(edge, lens, width, height).let { (l, r) -> ArcBow(look.slots, look.radius.toFloat(), look.span.toFloat(), l, r, sets) }

    /** LENS's touch box: 84 dp along its edge, 100 dp across (S4.4), at least [MIN_LENS_TOUCH] each way. */
    const val LENS_ALONG = 84f
    const val LENS_ACROSS = 100f
    const val MIN_LENS_TOUCH = 48f
    /** LENS reaches at least this far from its centre on every side, dp. */
    const val MIN_LENS_SIDE = 24f

    /**
     * LENS's touch box beside the [targets] around it, each a centre and its touch side (S4.4: at a large display size
     * the dock camera kept only 42 dp, an icon above LENS 45 dp). LENS gives way only on the side of each neighbour,
     * along the axis that costs it less, so the neighbour keeps its whole target. No side goes below [MIN_LENS_SIDE]
     * from the centre (a [MIN_LENS_TOUCH] box at least); a neighbour closer than that keeps a small overlap.
     */
    fun lensTouch(lens: Pt, vertical: Boolean, targets: List<Pair<Pt, Float>>): LensTouch {
        val across = LENS_ACROSS / 2f
        val along = LENS_ALONG / 2f
        val e = floatArrayOf(if (vertical) across else along, if (vertical) along else across,
            if (vertical) across else along, if (vertical) along else across) // left, top, right, bottom
        for ((c, side) in targets) {
            val half = side / 2f
            val dx = c.x - lens.x
            val dy = c.y - lens.y
            val h = if (dx < 0) 0 else 2
            val v = if (dy < 0) 1 else 3
            if (abs(dx) >= e[h] + half || abs(dy) >= e[v] + half) continue
            val wGap = abs(dx) - half
            val hGap = abs(dy) - half
            val wOk = wGap >= MIN_LENS_SIDE
            val hOk = hGap >= MIN_LENS_SIDE
            // Keep the larger of the two boxes; a way that would cut LENS below its minimum is no way.
            val wArea = (wGap + e[2 - h]) * (e[1] + e[3])
            val hArea = (hGap + e[4 - v]) * (e[0] + e[2])
            when {
                wOk && (!hOk || wArea >= hArea) -> e[h] = wGap
                hOk -> e[v] = hGap
                // Too close either way: LENS keeps its minimum on the side with more room, the overlap stays small.
                wGap >= hGap -> e[h] = minOf(e[h], MIN_LENS_SIDE)
                else -> e[v] = minOf(e[v], MIN_LENS_SIDE)
            }
        }
        return LensTouch(e[0], e[1], e[2], e[3])
    }
}

/** How far LENS's touch box reaches from its centre on each side, dp ([Dock.lensTouch]). */
data class LensTouch(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = left + right
    val height get() = top + bottom

    companion object {
        val Default = LensTouch(Dock.LENS_ALONG / 2f, Dock.LENS_ACROSS / 2f, Dock.LENS_ALONG / 2f, Dock.LENS_ACROSS / 2f)
    }
}

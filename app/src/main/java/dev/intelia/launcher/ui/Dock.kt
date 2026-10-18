package dev.intelia.launcher.ui

import dev.intelia.launcher.data.DockEdge
import dev.intelia.launcher.data.DockSpec
import dev.intelia.launcher.data.LensAt

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

    /**
     * LENS's touch box (width, height in screen dp) beside the [targets] around it, each a centre and its touch side:
     * LENS gives way so that a neighbour keeps its whole target (S4.4: at a large display size the dock camera kept
     * only 42 dp). It shrinks along the axis that costs it less and never below [MIN_LENS_TOUCH]; a neighbour closer
     * than that keeps the overlap rather than taking LENS's own target away.
     */
    fun lensTouch(lens: Pt, vertical: Boolean, targets: List<Pair<Pt, Float>>): Pair<Float, Float> {
        var hw = (if (vertical) LENS_ACROSS else LENS_ALONG) / 2f
        var hh = (if (vertical) LENS_ALONG else LENS_ACROSS) / 2f
        val min = MIN_LENS_TOUCH / 2f
        for ((c, side) in targets) {
            val half = side / 2f
            val dx = kotlin.math.abs(c.x - lens.x)
            val dy = kotlin.math.abs(c.y - lens.y)
            if (dx >= hw + half || dy >= hh + half) continue
            val w = (dx - half).coerceAtLeast(min)
            val h = (dy - half).coerceAtLeast(min)
            // Keep the larger box of the two ways out; a way that still overlaps counts as no way.
            val wFree = dx >= w + half
            val hFree = dy >= h + half
            when {
                wFree && (!hFree || w * hh >= hw * h) -> hw = w
                hFree -> hh = h
            }
        }
        return hw * 2f to hh * 2f
    }
}

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
    /** Half the length of four slots beside LENS (62 + 3 × 46 dp): LENS at the start or end moves back by it. */
    const val HALF_ROW = 100f

    /**
     * LENS centre for [dock] with the owner's along-edge offset [dx] on a [width] × [height] screen whose status bar
     * ends at [top] and navigation bar starts [bottom] above the bottom edge.
     */
    fun centre(dock: DockSpec, dx: Float, width: Float, height: Float, top: Float, bottom: Float): Pt {
        val shift = dx + when (dock.lensAt) { LensAt.Middle -> 0f; LensAt.Start -> -HALF_ROW; LensAt.End -> HALF_ROW }
        val alongLen = if (dock.edge.vertical) height - top - bottom else width
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
}

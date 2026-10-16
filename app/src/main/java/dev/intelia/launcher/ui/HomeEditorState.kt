package dev.intelia.launcher.ui

import dev.intelia.launcher.data.Anchor
import dev.intelia.launcher.data.AnchorType
import dev.intelia.launcher.data.HomeItem
import dev.intelia.launcher.data.ItemKind
import dev.intelia.launcher.data.LayoutSpec
import dev.intelia.launcher.data.Placement
import kotlin.math.abs
import kotlin.math.hypot

/** A point in dp from the top-left of Home. */
data class Pt(val x: Float, val y: Float) {
    operator fun plus(o: Pt) = Pt(x + o.x, y + o.y)
    operator fun minus(o: Pt) = Pt(x - o.x, y - o.y)
}

data class Zone(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    operator fun contains(p: Pt) = p.x in left..right && p.y in top..bottom
}

/**
 * Where anchors land on a screen of [width] × [height] dp (§5.4). The content area runs from below the status bar
 * to above the LENS row; positions are fractions or cells of it, so they survive another screen size.
 */
class HomeGeometry(
    val width: Float,
    val height: Float,
    val statusTop: Float,
    val lensX: Float,
    val lensY: Float,
) {
    val areaLeft = MARGIN
    val areaRight = width - MARGIN
    val areaTop = statusTop + 24f
    val areaBottom = lensY - LENS_BAND
    val areaW get() = areaRight - areaLeft
    val areaH get() = (areaBottom - areaTop).coerceAtLeast(1f)

    fun cellCentre(col: Int, row: Int, cols: Int, rows: Int) =
        Pt(areaLeft + (col + .5f) * areaW / cols, areaTop + (row + .5f) * areaH / rows)

    fun freePoint(fx: Float, fy: Float) = Pt(areaLeft + fx * areaW, areaTop + fy * areaH)

    fun toFree(p: Pt) = Anchor.free(((p.x - areaLeft) / areaW).coerceIn(0f, 1f), ((p.y - areaTop) / areaH).coerceIn(0f, 1f))

    fun nearestCell(p: Pt, cols: Int, rows: Int): Anchor {
        val c = ((p.x - areaLeft) / (areaW / cols)).toInt().coerceIn(0, cols - 1)
        val r = ((p.y - areaTop) / (areaH / rows)).toInt().coerceIn(0, rows - 1)
        return Anchor.cell(c, r)
    }

    /** ±62 / ±108 dp from LENS on its baseline, kept 24 dp from the edges (phase B2 placement). */
    fun lensSlot(slot: Int) = Pt((lensX + slotOffset(slot)).coerceIn(24f, width - 24f), lensY)

    /** The LENS-row slot under [p], if it is on the row (±36 dp) and within 30 dp of a slot. */
    fun lensSlotAt(p: Pt): Int? {
        if (abs(p.y - lensY) > 36f) return null
        return Anchor.LENS_SLOTS.minByOrNull { abs(lensSlot(it).x - p.x) }?.takeIf { abs(lensSlot(it).x - p.x) <= 30f }
    }

    /** Centre of an app-like item; null for kinds that are laid out specially (clock, date, LENS, legacy list). */
    fun position(item: HomeItem, layout: LayoutSpec): Pt? {
        val a = item.anchor
        return when (a.type) {
            AnchorType.Cell -> cellCentre(a.col, a.row, layout.grid.cols, layout.grid.rows)
            AnchorType.Free -> if (item.kind == ItemKind.Clock) null else freePoint(a.fx, a.fy)
            AnchorType.LensRow -> lensSlot(a.slot)
            else -> null
        }
    }

    /** The LENS zone that nothing may cover. */
    val lensZone get() = Zone(lensX - 42f, lensY - 50f, lensX + 42f, lensY + 50f)

    companion object {
        const val MARGIN = 16f
        const val LENS_BAND = 56f
        fun slotOffset(slot: Int) = when (slot) { -2 -> -108f; -1 -> -62f; 1 -> 62f; 2 -> 108f; else -> 0f }
    }
}

/** An alignment guide: a vertical line at x = [at] or a horizontal one at y = [at]. */
data class Guide(val vertical: Boolean, val at: Float)

data class DragPreview(
    val id: String,
    val point: Pt,
    val target: Anchor,
    /** An item that moves out of the way, and where to. */
    val displaced: Pair<String, Anchor>?,
    val guides: List<Guide>,
    val valid: Boolean,
)

/**
 * In-memory editing of one layout (§5.4, §5.5): drag resolve with snapping, guides, collisions, the LENS row,
 * and undo/redo of up to [MAX_UNDO] immutable snapshots. Pure Kotlin; the UI observes [revision].
 * [blocked] returns areas that apps may not cover (clock, date), measured by the UI.
 */
class HomeEditorState(
    initial: LayoutSpec,
    var geometry: HomeGeometry,
    var blocked: () -> List<Zone> = { emptyList() },
) {
    var layout: LayoutSpec = initial
        private set
    private var saved: LayoutSpec = initial
    private val undoStack = ArrayDeque<LayoutSpec>()
    private val redoStack = ArrayDeque<LayoutSpec>()
    var drag: DragPreview? = null
        private set
    private var dragOrigin: Pt = Pt(0f, 0f)
    var revision: Int = 0
        private set

    val dirty get() = layout != saved
    val canUndo get() = undoStack.isNotEmpty()
    val canRedo get() = redoStack.isNotEmpty()

    fun item(id: String) = layout.items.firstOrNull { it.id == id }

    /** Reference point of an item for dragging: its centre, or for the clock its free point. */
    fun pointOf(item: HomeItem): Pt? = geometry.position(item, layout)
        ?: if (item.kind == ItemKind.Clock && item.anchor.type == AnchorType.Free) geometry.freePoint(item.anchor.fx, item.anchor.fy) else null

    fun canDrag(item: HomeItem) = (item.kind == ItemKind.App || item.kind == ItemKind.Flow || item.kind == ItemKind.Clock) && pointOf(item) != null

    fun startDrag(id: String): Boolean {
        val item = item(id) ?: return false
        if (!canDrag(item)) return false
        dragOrigin = pointOf(item)!!
        drag = DragPreview(id, dragOrigin, item.anchor, null, emptyList(), true)
        bump()
        return true
    }

    /** Moves the dragged item by [delta] from where it started. */
    fun dragBy(delta: Pt): DragPreview? {
        val d = drag ?: return null
        val item = item(d.id) ?: return null
        drag = resolve(item, dragOrigin + delta)
        bump()
        return drag
    }

    fun drop(): Boolean {
        val d = drag ?: return false
        drag = null
        bump()
        if (!d.valid) return false
        val moved = layout.items.map {
            when (it.id) {
                d.id -> it.copy(anchor = d.target)
                d.displaced?.first -> it.copy(anchor = d.displaced.second)
                else -> it
            }
        }
        if (moved == layout.items) return false
        commit(layout.copy(items = moved))
        return true
    }

    fun cancelDrag() { drag = null; bump() }

    fun setPlacement(p: Placement) { if (p != layout.placement) commit(layout.copy(placement = p)) }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(layout); layout = prev; bump()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(layout); layout = next; bump()
    }

    /** After an atomic save: the current layout is the new baseline. */
    fun markSaved(baseline: LayoutSpec = layout) { layout = baseline; saved = baseline; bump() }

    /** Replaces the whole layout as one undoable step (e.g. a preset). */
    fun replace(next: LayoutSpec) { if (next != layout) commit(next) }

    private fun commit(next: LayoutSpec) {
        undoStack.addLast(layout)
        while (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        redoStack.clear()
        layout = next
        bump()
    }

    private fun bump() { revision++ }

    private fun resolve(item: HomeItem, p: Pt): DragPreview {
        val g = geometry
        if (item.kind == ItemKind.Clock) {
            // The clock moves freely in the upper part of the area and never into the LENS band.
            val a = g.toFree(p).let { Anchor.free(it.fx, it.fy.coerceAtMost(CLOCK_MAX_FY)) }
            return DragPreview(item.id, p, a, null, emptyList(), true)
        }
        val others = layout.items.filter { it.id != item.id }
        g.lensSlotAt(p)?.let { slot ->
            val occupant = others.firstOrNull { it.anchor.type == AnchorType.LensRow && it.anchor.slot == slot }
            // Swap in the LENS row: the occupant takes the dragged item's old place.
            return DragPreview(item.id, p, Anchor.lensRow(slot), occupant?.let { it.id to item.anchor }, emptyList(), true)
        }
        val invalid = DragPreview(item.id, p, item.anchor, null, emptyList(), false)
        if (p.y < g.areaTop || p.y > g.areaBottom || p.x < g.areaLeft || p.x > g.areaRight) return invalid
        if (p in g.lensZone || blocked().any { p in it }) return invalid
        val grid = layout.grid
        val cell = g.nearestCell(p, grid.cols, grid.rows)
        val cellPt = g.cellCentre(cell.col, cell.row, grid.cols, grid.rows)
        val useCell = when (layout.placement) {
            Placement.Grid -> true
            Placement.Free -> false
            Placement.Hybrid -> hypot(p.x - cellPt.x, p.y - cellPt.y) <= HYBRID_SNAP
        }
        if (useCell) {
            if (blocked().any { cellPt in it } || cellPt in g.lensZone) return invalid
            val occupant = others.firstOrNull { it.anchor.type == AnchorType.Cell && it.anchor.col == cell.col && it.anchor.row == cell.row }
                ?: return DragPreview(item.id, p, cell, null, emptyList(), true)
            val free = nearestFreeCell(cell, others.filter { it.id != occupant.id }, item)
                ?: return invalid
            return DragPreview(item.id, p, cell, occupant.id to free, emptyList(), true)
        }
        // Free placement with guides on the centre line and other items' axes.
        var x = p.x
        var y = p.y
        val guides = mutableListOf<Guide>()
        if (layout.guides) {
            val points = others.mapNotNull { g.position(it, layout) }
            val xs = listOf(g.width / 2) + points.map { it.x }
            val ys = points.map { it.y }
            xs.minByOrNull { abs(it - p.x) }?.takeIf { abs(it - p.x) <= GUIDE_SNAP }?.let { x = it; guides += Guide(true, it) }
            ys.minByOrNull { abs(it - p.y) }?.takeIf { abs(it - p.y) <= GUIDE_SNAP }?.let { y = it; guides += Guide(false, it) }
        }
        return DragPreview(item.id, p, g.toFree(Pt(x, y)), null, guides, true)
    }

    /** Nearest cell to [from] not taken by [others] nor blocked; the dragged item's own old cell counts as free. */
    private fun nearestFreeCell(from: Anchor, others: List<HomeItem>, dragged: HomeItem): Anchor? {
        val grid = layout.grid
        val taken = others.filter { it.anchor.type == AnchorType.Cell }.mapTo(HashSet()) { it.anchor.col to it.anchor.row }
        taken += from.col to from.row
        if (dragged.anchor.type == AnchorType.Cell) taken -= dragged.anchor.col to dragged.anchor.row
        val g = geometry
        return (0 until grid.cols).flatMap { c -> (0 until grid.rows).map { r -> c to r } }
            .filter { it !in taken }
            .filter { (c, r) -> g.cellCentre(c, r, grid.cols, grid.rows).let { pt -> blocked().none { pt in it } && pt !in g.lensZone } }
            .minWithOrNull(compareBy({ (c, r) -> abs(c - from.col) + abs(r - from.row) }, { abs(it.second - from.row) }, { it.first }))
            ?.let { (c, r) -> Anchor.cell(c, r) }
    }

    companion object {
        const val MAX_UNDO = 50
        const val GUIDE_SNAP = 8f
        const val HYBRID_SNAP = 20f
        const val CLOCK_MAX_FY = .6f
    }
}

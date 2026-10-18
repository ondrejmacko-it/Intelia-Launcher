package dev.intelia.launcher.ui

import dev.intelia.launcher.data.Anchor
import dev.intelia.launcher.data.AnchorType
import dev.intelia.launcher.data.DockEdge
import dev.intelia.launcher.data.DockSpec
import dev.intelia.launcher.data.LensAt
import dev.intelia.launcher.data.HomeItem
import dev.intelia.launcher.data.ItemKind
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.LayoutSpec
import dev.intelia.launcher.data.lensDx
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

    /** This zone's centre lies in [other] shrunk by [inset] dp: a free date placed over the clock's plate. */
    fun centredIn(other: Zone, inset: Float) =
        (left + right) / 2f in other.left + inset..other.right - inset && (top + bottom) / 2f in other.top + inset..other.bottom - inset
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
    val dock: DockSpec = DockSpec(),
    /** Height of the navigation bar, dp; the content area stops above it when the row is not at the bottom. */
    val bottomInset: Float = 0f,
    /** S2: the largest icon on the LENS row, dp; slots move apart so icons this big do not overlap (0 = the spec's steps). */
    val rowIcon: Float = 0f,
) {
    private val edge get() = dock.edge
    val areaLeft = if (edge == DockEdge.Left) lensX + SIDE_BAND else MARGIN
    val areaRight = if (edge == DockEdge.Right) lensX - SIDE_BAND else width - MARGIN
    val areaTop = if (edge == DockEdge.Top) lensY + LENS_BAND else statusTop + 24f
    val areaBottom = if (edge == DockEdge.Bottom) lensY - LENS_BAND else height - bottomInset - FOOT
    val areaW get() = areaRight - areaLeft
    val areaH get() = (areaBottom - areaTop).coerceAtLeast(1f)

    fun cellCentre(col: Int, row: Int, cols: Int, rows: Int) =
        Pt(areaLeft + (col + .5f) * areaW / cols, areaTop + (row + .5f) * areaH / rows)

    fun freePoint(fx: Float, fy: Float) = Pt(areaLeft + fx * areaW, areaTop + fy * areaH)

    /** The same screen with [spec]'s own LENS row (edge, end, offset): a preset's miniature must not follow the live one. */
    fun forLayout(spec: LayoutSpec): HomeGeometry {
        val c = Dock.centre(spec.dock, spec.lensDx(), width, height, statusTop, bottomInset)
        return HomeGeometry(width, height, statusTop, c.x, c.y, spec.dock, bottomInset, rowIcon)
    }

    fun toFree(p: Pt) = Anchor.free(((p.x - areaLeft) / areaW).coerceIn(0f, 1f), ((p.y - areaTop) / areaH).coerceIn(0f, 1f))

    fun nearestCell(p: Pt, cols: Int, rows: Int): Anchor {
        val c = ((p.x - areaLeft) / (areaW / cols)).toInt().coerceIn(0, cols - 1)
        val r = ((p.y - areaTop) / (areaH / rows)).toInt().coerceIn(0, rows - 1)
        return Anchor.cell(c, r)
    }

    /**
     * [DockSpec.gap] and then [DockSpec.pitch] apart from LENS on its baseline (by default ±62 / ±108 dp, phase B2).
     * A row longer than the edge shrinks to fit; a slot that would come closer than 24 dp to the edge, because LENS
     * sits off centre (§5.5), continues outwards on the other side at the same pitch. [DockSpec.spread] spaces the
     * slots evenly over the room beyond LENS instead.
     */
    fun lensSlot(slot: Int): Pt {
        val vertical = edge.vertical
        val at = if (vertical) lensY else lensX
        // A big icon keeps its whole disc on the screen.
        val edge = maxOf(EDGE, rowIcon / 2f + 4f)
        val lo = if (vertical) statusTop + edge else edge
        val hi = if (vertical) height - bottomInset - edge else width - edge
        // On a side the row runs down the edge; "start" is the top. LENS before or after all four: one pitch apart.
        val k = Anchor.LENS_SLOTS.sorted().indexOf(slot).coerceAtLeast(0)
        val n = Anchor.LENS_SLOTS.size
        val (gap, pitch) = rowSteps(at, lo, hi)
        val along = when (dock.lensAt) {
            LensAt.Start -> at + gap + k * pitch
            LensAt.End -> at - gap - (n - 1 - k) * pitch
            LensAt.Middle -> if (dock.spread) spreadSlot(slot, at, lo, hi) else middleSlot(slot, at, lo, hi, gap, pitch)
        }.coerceIn(lo, hi)
        return if (vertical) Pt(lensX, along) else Pt(along, lensY)
    }

    /** LENS to the first slot and slot to slot, dp, fitted to the edge between [lo] and [hi]. */
    private fun rowSteps(at: Float, lo: Float, hi: Float): Pair<Float, Float> {
        val n = Anchor.LENS_SLOTS.size
        if (dock.spread && dock.lensAt != LensAt.Middle) {
            // LENS and the four slots evenly over the room beyond LENS, the last one at the edge limit.
            val room = if (dock.lensAt == LensAt.Start) hi - at else at - lo
            val step = (room / n).coerceAtLeast(MIN_STEP)
            return step to step
        }
        // Big icons push the slots apart: LENS's half and the icon's half, and icon to icon, keep [ROW_AIR] between them.
        val gap = maxOf(dock.gap.toFloat(), LENS_HALF + rowIcon / 2f + ROW_AIR)
        val pitch = maxOf(dock.pitch.toFloat(), rowIcon + ROW_AIR)
        val need = if (dock.lensAt == LensAt.Middle) 2 * (gap + pitch) else gap + (n - 1) * pitch
        val fit = ((hi - lo) / need).coerceAtMost(1f)
        return gap * fit to pitch * fit
    }

    /** Spread with LENS between: each side's two slots evenly in that side's room. */
    private fun spreadSlot(slot: Int, at: Float, lo: Float, hi: Float): Float {
        val side = if (slot > 0) 1 else -1
        val room = if (side > 0) hi - at else at - lo
        val step = room / 2f
        return if (step >= MIN_STEP) at + side * step * abs(slot)
        else rowSteps(at, lo, hi).let { (gap, pitch) -> middleSlot(slot, at, lo, hi, gap, pitch) }
    }

    private fun middleSlot(slot: Int, at: Float, lo: Float, hi: Float, gap: Float, pitch: Float): Float {
        val steps = listOf(gap, gap + pitch)
        val side = if (slot > 0) 1 else -1
        val room = if (side > 0) hi - at else at - lo
        val fit = steps.count { it <= room }
        val i = abs(slot) - 1
        if (i < fit) return at + side * steps[i]
        // Overflow: after the other side's own slots that fit there.
        val otherRoom = if (side > 0) at - lo else hi - at
        val k = steps.count { it <= otherRoom } + (i - fit)
        return at - side * (gap + k * pitch)
    }

    /** The LENS-row slot under [p], if it is on the row (±36 dp) and within 30 dp of a slot. */
    fun lensSlotAt(p: Pt): Int? {
        val vertical = edge.vertical
        if (abs(if (vertical) p.x - lensX else p.y - lensY) > 36f) return null
        fun gap(slot: Int) = lensSlot(slot).let { abs(if (vertical) it.y - p.y else it.x - p.x) }
        return Anchor.LENS_SLOTS.minByOrNull(::gap)?.takeIf { gap(it) <= 30f }
    }

    /** Centre of an app-like item; null for kinds that are laid out specially (clock, date, LENS, legacy list). */
    fun position(item: HomeItem, layout: LayoutSpec): Pt? {
        val a = item.anchor
        return when (a.type) {
            AnchorType.Cell -> cellCentre(a.col, a.row, layout.grid.cols, layout.grid.rows)
            AnchorType.Free -> if (item.kind.isText()) null else freePoint(a.fx, a.fy)
            AnchorType.LensRow -> lensSlot(a.slot)
            else -> null
        }
    }

    /** The LENS zone that nothing may cover. */
    val lensZone get() = if (edge.vertical) Zone(lensX - 50f, lensY - 42f, lensX + 50f, lensY + 42f)
        else Zone(lensX - 42f, lensY - 50f, lensX + 42f, lensY + 50f)

    /** The whole LENS row, which an editor card keeps clear of. */
    val rowZone get() = when (edge) {
        DockEdge.Bottom, DockEdge.Top -> Zone(0f, lensY - 40f, width, lensY + 40f)
        DockEdge.Left, DockEdge.Right -> Zone(lensX - 40f, 0f, lensX + 40f, height)
    }

    /** Length of the edge LENS moves along, dp. */
    val alongLength get() = if (edge.vertical) height - statusTop - bottomInset else width

    companion object {
        const val MARGIN = 16f
        const val LENS_BAND = 56f
        const val SIDE_BAND = 44f
        /** Room kept above the navigation bar when the LENS row is elsewhere: an icon's name still fits. */
        const val FOOT = 40f
        private const val EDGE = 24f
        /** The closest two slots may come in a spread row, dp. */
        private const val MIN_STEP = 46f
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

    /** A preset only previewed is not a change: leaving after it discards the preview without a question. */
    val dirty get() = (previewOf ?: layout) != saved
    val canUndo get() = undoStack.isNotEmpty()
    val canRedo get() = redoStack.isNotEmpty()

    fun item(id: String) = layout.items.firstOrNull { it.id == id }

    /** Reference point of an item for dragging: its centre, or for the clock and an unpinned date their free point. */
    fun pointOf(item: HomeItem): Pt? = geometry.position(item, layout)
        ?: if (item.kind.isText() && item.anchor.type == AnchorType.Free) geometry.freePoint(item.anchor.fx, item.anchor.fy) else null

    fun canDrag(item: HomeItem) = (item.kind == ItemKind.App || item.kind == ItemKind.Flow || item.kind.isText()) && pointOf(item) != null

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

    /** While LENS is being dragged: its live offset from the centre (dp); null otherwise. */
    var lensDrag: Float? = null
        private set
    private var lensOrigin = 0f

    /** LENS centre on the current geometry; read by gesture handlers that outlive a recomposition. */
    fun lensAt() = Pt(geometry.lensX, geometry.lensY)

    fun lensId() = layout.items.firstOrNull { it.kind == ItemKind.Lens }?.id

    /** Where LENS is drawn now: the live drag, else the layout's own position. */
    fun lensDx(): Float = lensDrag ?: layout.lensDx()

    fun startLensDrag() { lensOrigin = layout.lensDx(); lensDrag = lensOrigin; bump() }

    /**
     * Moves LENS sideways by [deltaX] from where the drag started, within the lower band (§5.5) and snapping to the
     * centre within [LENS_SNAP] dp. Returns true when it just snapped (for a haptic tick).
     */
    fun dragLensBy(deltaX: Float): Boolean {
        val before = lensDrag ?: return false
        val next = clampLens(lensOrigin + deltaX).let { if (abs(it) < LENS_SNAP) 0f else it }
        lensDrag = next
        bump()
        return next == 0f && before != 0f
    }

    /** Ends a LENS drag as one undo step; false if it did not move. */
    fun dropLens(): Boolean {
        val dx = lensDrag ?: return false
        lensDrag = null
        return setLensDx(dx).also { if (!it) bump() }
    }

    fun cancelLensDrag() { lensDrag = null; bump() }

    /** Moves the LENS row to another edge or LENS within it (§5.5), as one undo step; false if nothing changed. */
    /** Changes the LENS row; steps of one slider ([key]) make one undo step, like [edit]. */
    fun setDock(dock: DockSpec, key: String? = null): Boolean {
        val next = dock.sanitized()
        if (next == layout.dock) return false
        if (key != null && coalesce == "dock/$key" && undoStack.isNotEmpty()) { layout = layout.copy(dock = next); redoStack.clear(); bump() }
        else { commit(layout.copy(dock = next)); if (key != null) coalesce = "dock/$key" }
        return true
    }

    /** Places LENS [dx] dp from the centre (inspector, TalkBack), clamped like a drag, as one undo step. */
    fun setLensDx(dx: Float): Boolean {
        val v = clampLens(dx)
        if (v == layout.lensDx() && layout.items.firstOrNull { it.kind == ItemKind.Lens }?.anchor?.dx != null) return false
        commit(withLens(layout, v))
        return true
    }

    /** Room on either side of the centre for this screen: never past [LauncherSettings.MAX_LENS_DX] or [LENS_EDGE]. */
    fun lensRoom() = (geometry.alongLength / 2f - LENS_EDGE).coerceIn(0f, LauncherSettings.MAX_LENS_DX)

    private fun clampLens(dx: Float) = LauncherSettings.clampLensDx(dx).coerceIn(-lensRoom(), lensRoom())

    private fun withLens(spec: LayoutSpec, dx: Float) = spec.copy(items = spec.items.map {
        if (it.kind == ItemKind.Lens) it.copy(anchor = Anchor(AnchorType.Lens, fx = it.anchor.fx, bottomDp = it.anchor.bottomDp, dx = dx)) else it
    })

    /** A preset keeps LENS where it is now. */
    private fun keepLens(spec: LayoutSpec) = if (spec.lensDx() == layout.lensDx()) spec else withLens(spec, layout.lensDx())

    /**
     * Changes one item (inspector). Calls with the same [key] in a row, e.g. a slider being dragged,
     * collapse into one undo step; any other change starts a new one.
     */
    fun edit(id: String, key: String, transform: (HomeItem) -> HomeItem) {
        val before = item(id) ?: return
        val after = transform(before).copy(id = before.id, kind = before.kind)
        if (after == before) return
        val next = layout.copy(items = layout.items.map { if (it.id == id) after else it })
        if (coalesce == "$id/$key" && undoStack.isNotEmpty()) { layout = next; redoStack.clear(); bump() }
        else { commit(next); coalesce = "$id/$key" }
    }

    /** Removes an item from Home only (never uninstalls); LENS always stays. The clock and date come back from the Add menu. */
    fun remove(id: String): Boolean {
        val item = item(id) ?: return false
        if (item.kind == ItemKind.Lens) return false
        commit(layout.copy(items = layout.items.filter { it.id != id }))
        return true
    }

    /** Shows or hides the date under the clock. */
    fun setDate(on: Boolean) {
        val has = layout.items.any { it.kind == ItemKind.Date }
        if (on == has) return
        commit(layout.copy(items = if (on) layout.items + HomeItem("date", ItemKind.Date, Anchor.Bound)
            else layout.items.filter { it.kind != ItemKind.Date }))
    }

    /**
     * Pins the date under the clock or unpins it to move on its own. Unpinning keeps it where it was ([keepFy],
     * measured by the caller), else just under the clock's free point.
     */
    fun pinDate(pinned: Boolean, keepFy: Float? = null) {
        val date = layout.items.firstOrNull { it.kind == ItemKind.Date } ?: return
        if (pinned == (date.anchor.type == AnchorType.Bound)) return
        val clock = layout.items.firstOrNull { it.kind == ItemKind.Clock }?.anchor?.takeIf { it.type == AnchorType.Free }
        val anchor = if (pinned) Anchor.Bound
            else Anchor.free(clock?.fx ?: .5f, (keepFy ?: ((clock?.fy ?: 0f) + DATE_BELOW)).coerceIn(0f, CLOCK_MAX_FY))
        commit(layout.copy(items = layout.items.map { if (it.id == date.id) it.copy(anchor = anchor) else it }))
    }

    /** Adds the hidden clock or date back at [p] (Add menu); false if it is already on Home. */
    fun addText(kind: ItemKind, p: Pt): Boolean {
        if (!kind.isText() || layout.items.any { it.kind == kind }) return false
        val anchor = resolve(HomeItem(kind.name.lowercase(), kind), p).target
        commit(layout.copy(items = layout.items + HomeItem(kind.name.lowercase(), kind, anchor)))
        return true
    }

    /**
     * Adds an app or FLOW where Home was tapped: the same snapping as a drop, but nothing is pushed aside;
     * a taken or blocked spot falls back to the nearest free cell. False if it is already on Home or nothing is free.
     */
    fun addAt(kind: ItemKind, target: String, p: Pt): Boolean {
        val id = (if (kind == ItemKind.Flow) "flow:" else "app:") + target
        if (item(id) != null || layout.items.size >= dev.intelia.launcher.data.HomeLayouts.MAX_ITEMS) return false
        val fresh = HomeItem(id, kind, Anchor.cell(-1, -1), target = target)
        val r = resolve(fresh, p)
        val anchor = if (r.valid && r.displaced == null) r.target
            else nearestFreeCell(geometry.nearestCell(p, layout.grid.cols, layout.grid.rows), layout.items, fresh) ?: return false
        commit(layout.copy(items = layout.items + fresh.copy(anchor = anchor)))
        return true
    }

    /** Before a preset preview: the layout to return to; null when no preview is shown. */
    var previewOf: LayoutSpec? = null
        private set

    /** Shows [spec] in place without touching history; [confirmPreview] keeps it as one undoable step. */
    fun preview(spec: LayoutSpec) {
        if (previewOf == null) previewOf = layout
        layout = keepLens(spec)
        bump()
    }

    fun confirmPreview() {
        val before = previewOf ?: return
        previewOf = null
        val next = layout
        layout = before
        if (next != before) commit(next) else bump()
    }

    fun cancelPreview() {
        layout = previewOf ?: return
        previewOf = null
        bump()
    }

    /**
     * Moves an icon one cell (24 dp when free) for TalkBack and the inspector's move buttons,
     * with exactly the rules of a drag and drop.
     */
    fun nudge(id: String, dx: Int, dy: Int): Boolean {
        val item = item(id) ?: return false
        val free = layout.placement == Placement.Free || item.anchor.type == AnchorType.Free
        val stepX = if (free) 24f else geometry.areaW / layout.grid.cols
        val stepY = if (free) 24f else geometry.areaH / layout.grid.rows
        val delta = if (item.anchor.type == AnchorType.LensRow) {
            // On the LENS row: along it to the neighbouring slot, or off it into the nearest grid cell.
            val g = geometry
            val from = g.lensSlot(item.anchor.slot)
            val (along, across) = if (g.dock.edge.vertical) dy to dx else dx to dy
            val inward = when (g.dock.edge) { DockEdge.Bottom, DockEdge.Right -> -1; DockEdge.Top, DockEdge.Left -> 1 }
            if (across == inward) when (g.dock.edge) {
                DockEdge.Bottom -> Pt(0f, g.areaBottom - g.areaH / layout.grid.rows / 2f - from.y)
                DockEdge.Top -> Pt(0f, g.areaTop + g.areaH / layout.grid.rows / 2f - from.y)
                DockEdge.Left -> Pt(g.areaLeft + g.areaW / layout.grid.cols / 2f - from.x, 0f)
                DockEdge.Right -> Pt(g.areaRight - g.areaW / layout.grid.cols / 2f - from.x, 0f)
            }
            else if (across != 0) return false
            else {
                val slots = Anchor.LENS_SLOTS.sorted()
                val next = slots.getOrNull(slots.indexOf(item.anchor.slot) + along) ?: return false
                val to = g.lensSlot(next)
                Pt(to.x - from.x, to.y - from.y)
            }
        } else Pt(dx * stepX, dy * stepY)
        if (!startDrag(id)) return false
        dragBy(delta)
        return drop()
    }

    fun setPlacement(p: Placement) { if (p != layout.placement) commit(layout.copy(placement = p)) }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        coalesce = null
        redoStack.addLast(layout); layout = prev; bump()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        coalesce = null
        undoStack.addLast(layout); layout = next; bump()
    }

    /** After an atomic save: the current layout is the new baseline. */
    fun markSaved(baseline: LayoutSpec = layout) { layout = baseline; saved = baseline; bump() }

    /** Replaces the whole layout as one undoable step (e.g. a preset). */
    fun replace(next: LayoutSpec) { keepLens(next).let { if (it != layout) commit(it) } }

    private var coalesce: String? = null

    private fun commit(next: LayoutSpec) {
        coalesce = null
        undoStack.addLast(layout)
        while (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        redoStack.clear()
        layout = next
        bump()
    }

    private fun bump() { revision++ }

    private fun resolve(item: HomeItem, p: Pt): DragPreview {
        val g = geometry
        if (item.kind.isText()) {
            // The clock (and an unpinned date) moves freely in the upper part of the area and never into the LENS band.
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
        const val LENS_SNAP = 10f

        /**
         * The first delta of a drag only counts movement past the touch slop; giving the slop back along the same
         * direction keeps what is dragged under the finger instead of [slop] behind it (all in the same unit).
         */
        fun withSlop(delta: Pt, slop: Float): Pt {
            val d = hypot(delta.x, delta.y)
            if (d == 0f) return delta
            val k = (d + slop) / d
            return Pt(delta.x * k, delta.y * k)
        }
        const val MAX_UNDO = 50
        const val GUIDE_SNAP = 8f
        const val HYBRID_SNAP = 20f
        const val CLOCK_MAX_FY = .6f
        /** How far below the clock's free point an unpinned date starts, as a fraction of the area. */
        const val DATE_BELOW = .12f
    }
}

/** The clock and the date: text laid out by a free point, never by the grid. */
fun ItemKind.isText() = this == ItemKind.Clock || this == ItemKind.Date

/**
 * Width of each icon name under its icon, dp, so that no two names overlap and none runs under LENS (D15).
 * Names on one line share the gap between their centres; a name narrower than [LABEL_MIN] is left out.
 * [labelled] maps item ids to icon centres; unlabelled icons sit above the name line and never meet it.
 */
/** One icon on Home as the label layout sees it; [labelled] icons get a name, the others are only obstacles. */
data class LabelIcon(val id: String, val at: Pt, val icon: Float, val labelled: Boolean = false, val gap: Float = 4f,
    val lineH: Float = 15f, val lines: Int = 1)

/** Where a name goes (S2): [width] wide, centred on [x]; its top edge at [y], or its bottom edge when [above]. */
data class LabelBox(val x: Float, val y: Float, val width: Float, val lines: Int, val above: Boolean)

/**
 * Names under Home icons, laid out once for Home and its Studio miniature. A name keeps clear of neighbours on its row
 * (half the distance to a named one, up to the edge of a plain one), of LENS and of the screen edges, and wraps to
 * the lines that fit before the next icon below or [bottom] (above the navigation bar). With no line of room below,
 * it goes above the icon; with none there either, or narrower than [LABEL_MIN], it is not shown.
 */
fun labelBoxes(icons: List<LabelIcon>, lens: Pt?, width: Float, top: Float, bottom: Float): Map<String, LabelBox> =
    icons.filter { it.labelled }.mapNotNull { p ->
        var half = maxOf(LABEL_MAX, p.icon + 16f) / 2f
        half = minOf(half, p.at.x - LABEL_EDGE, width - LABEL_EDGE - p.at.x)
        val others = icons.filter { it.id != p.id }
        others.forEach { q ->
            if (abs(q.at.y - p.at.y) < (q.icon + p.icon) / 2f) {
                val dx = abs(q.at.x - p.at.x)
                half = minOf(half, if (q.labelled) dx / 2f - LABEL_GAP else dx - q.icon / 2f - LABEL_GAP)
            }
        }
        if (lens != null && abs(lens.y - p.at.y) < p.icon / 2f + LENS_HALF) half = minOf(half, abs(lens.x - p.at.x) - LENS_HALF - LABEL_GAP)
        if (half * 2 < LABEL_MIN) return@mapNotNull null
        // Obstacles straight below or above the name: other icons (with the room their own name may take below them) and LENS.
        val column = others.filter { abs(it.at.x - p.at.x) < half + maxOf(it.icon / 2f, if (it.labelled) LABEL_MAX / 2f else 0f) }
            .map { Triple(it.at.y - it.icon / 2f, it.at.y + it.icon / 2f + if (it.labelled) it.gap + it.lines * it.lineH else 0f, it.at.y) } +
            listOfNotNull(lens?.takeIf { abs(it.x - p.at.x) < half + LENS_HALF }?.let { Triple(it.y - LENS_HALF, it.y + LENS_HALF, it.y) })
        val belowY = p.at.y + p.icon / 2f + p.gap
        val floor = column.filter { it.third > p.at.y }.minOfOrNull { it.first - LABEL_GAP }?.let { minOf(it, bottom) } ?: bottom
        val below = ((floor - belowY) / p.lineH).toInt().coerceAtMost(p.lines)
        if (below >= 1) return@mapNotNull p.id to LabelBox(p.at.x, belowY, half * 2, below, above = false)
        val aboveY = p.at.y - p.icon / 2f - p.gap
        val ceiling = column.filter { it.third < p.at.y }.maxOfOrNull { it.second + LABEL_GAP }?.let { maxOf(it, top) } ?: top
        val above = ((aboveY - ceiling) / p.lineH).toInt().coerceAtMost(p.lines)
        if (above >= 1) p.id to LabelBox(p.at.x, aboveY, half * 2, above, above = true) else null
    }.toMap()

const val LABEL_MAX = 76f
const val LABEL_MIN = 48f
private const val LABEL_GAP = 2f
private const val LABEL_EDGE = 4f
private const val LENS_HALF = 28f
/** Room between neighbours on the LENS row, dp. */
private const val ROW_AIR = 6f

/**
 * The editor's view of Home (D15): the same Home drawn 1:1 in proportion, scaled to fit the frame between the
 * camera-row bar ([BAR_HEIGHT]) and the tray, with [FRAME_GAP] of air on both sides. Home keeps its own
 * coordinates; cards, the bar and the tray are placed on the screen, from an element's settled position ([toScreen]).
 */
data class EditView(val width: Float, val height: Float, val editing: Boolean = true) {
    private val frameTop get() = BAR_HEIGHT + FRAME_GAP
    private val frameScale get() = ((height - TRAY_BOTTOM - TRAY_HEIGHT - FRAME_GAP - frameTop) / height).coerceIn(.5f, 1f)
    val scale get() = if (editing) frameScale else 1f
    /** Where the scaling is centred, a fraction of the height; the same in and out of the editor. */
    val originY get() = if (frameScale >= 1f) .5f else frameTop / (1f - frameScale) / height
    private val ox get() = width / 2f
    private val oy get() = height * originY
    fun toScreen(p: Pt) = Pt(ox + (p.x - ox) * scale, oy + (p.y - oy) * scale)
    fun toScreen(z: Zone): Zone {
        val a = toScreen(Pt(z.left, z.top))
        val b = toScreen(Pt(z.right, z.bottom))
        return Zone(a.x, a.y, b.x, b.y)
    }
    fun toHome(p: Pt) = Pt(ox + (p.x - ox) / scale, oy + (p.y - oy) / scale)

    companion object {
        /** Air between the framed Home and the bar above / the tray below, dp. */
        const val FRAME_GAP = 8f
    }
}

/**
 * Whether an inspector of height [h] opens above its element ([target], screen dp) rather than below (D15):
 * the side where it fits whole, the roomier one when it fits on both or on neither. Above never covers [avoid]
 * (the clock) when the card fits below instead.
 */
fun inspectorAbove(target: Zone, h: Float, topLimit: Float, screenH: Float, avoid: Zone? = null, left: Float = 0f, width: Float = 0f): Boolean {
    val aboveRoom = target.top - 12f - topLimit
    val belowRoom = screenH - 12f - target.bottom - 12f
    val aboveTop = target.top - 12f - h
    val clear = avoid == null || avoid.bottom <= aboveTop || avoid.top >= target.top || avoid.right <= left || avoid.left >= left + width
    val fitsAbove = h <= aboveRoom
    val fitsBelow = h <= belowRoom
    return when {
        fitsAbove && fitsBelow -> if (clear) aboveRoom >= belowRoom else false
        fitsAbove -> true
        fitsBelow -> false
        else -> aboveRoom > belowRoom
    }
}

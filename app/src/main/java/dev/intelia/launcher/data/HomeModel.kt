package dev.intelia.launcher.data

import kotlinx.serialization.Serializable

/**
 * Home layout model (DESIGN_SYSTEM §5.4, schema 2). Validated only through [LauncherSettings.sanitized].
 *
 * The *linked* layout ([LayoutSpec.linked]) is the built-in preset: it is rebuilt from the legacy fields
 * ([LauncherSettings.layout], [LauncherSettings.favorites], [LauncherSettings.showClock], [LauncherSettings.showDate])
 * on every sanitize, so Studio and the app sheet keep working as before. Saving from the Home Editor writes
 * an independent copy ([linked] = false); a preset switch never overwrites it.
 */
@Serializable enum class LayoutBase { Pure, Essentials, Balanced, None }
@Serializable enum class Placement { Grid, Free, Hybrid }
/** [Widget] is reserved (an `AppWidgetHost` id in [HomeItem.target]); nothing renders it yet. */
@Serializable enum class ItemKind { App, Flow, Clock, Date, Lens, Widget }

/**
 * Where an item sits. Flat on purpose: an unknown or damaged anchor degrades to defaults instead of
 * failing the whole document.
 * - [AnchorType.Cell]: [col], [row] of the layout grid.
 * - [AnchorType.Free]: [fx], [fy] as fractions (0..1) of the safe content area.
 * - [AnchorType.LensRow]: [slot] -2, -1, 1, 2 beside LENS.
 * - [AnchorType.Lens]: LENS itself, [dx] dp from the screen centre (null in documents before the continuous position:
 *   migrated from [LauncherSettings.reach]); [fx] and [bottomDp] are reserved.
 * - [AnchorType.Bound]: under the clock (the date); an unpinned date uses [AnchorType.Free] like the clock.
 * - [AnchorType.Stack]: [row]-th line of the legacy text list (preset "Vlastní").
 */
@Serializable enum class AnchorType { Cell, Free, LensRow, Lens, Bound, Stack }

@Serializable
data class Anchor(
    val type: AnchorType = AnchorType.Free,
    val col: Int = 0,
    val row: Int = 0,
    val fx: Float = .5f,
    val fy: Float = .5f,
    val slot: Int = 0,
    val bottomDp: Float = 0f,
    val dx: Float? = null,
) {
    companion object {
        fun cell(col: Int, row: Int) = Anchor(AnchorType.Cell, col = col, row = row)
        fun free(fx: Float, fy: Float) = Anchor(AnchorType.Free, fx = fx, fy = fy)
        fun lensRow(slot: Int) = Anchor(AnchorType.LensRow, slot = slot)
        fun stack(index: Int) = Anchor(AnchorType.Stack, row = index)
        val Lens = Anchor(AnchorType.Lens)
        val Bound = Anchor(AnchorType.Bound)
        val LENS_SLOTS = listOf(-2, -1, 1, 2)
    }
}

@Serializable data class GridSpec(val cols: Int = 4, val rows: Int = 6)

@Serializable
data class HomeItem(
    val id: String,
    val kind: ItemKind = ItemKind.App,
    val anchor: Anchor = Anchor(),
    /** 0 = the kind's default size. */
    val sizeDp: Int = 0,
    val z: Int = 0,
    /** App key, FLOW id or reserved widget id; empty for Clock, Date and Lens. */
    val target: String = "",
    val label: Boolean = false,
    /** Clock: a [ClockStyle] name; LENS: a [LensStyle] name; date: a [DateFormat] name; empty = the default (the clock follows Studio). */
    val style: String = "",
    /** Clock font weight 100..900; 0 = the style's own. */
    val weight: Int = 0,
    /** One of [ItemTint]; empty = the wallpaper-derived ink. */
    val tint: String = "",
    /** 0.3..1. */
    val alpha: Float = 1f,
    /** Clock or date: a [ClockBackground] name; empty = none. */
    val background: String = "",
    /** Typography of the clock and the date (§5.12); the defaults draw them as before. */
    val look: TextLook = TextLook(),
)

/** Faces for the clock and the date (D14); [Style] = the clock style's own (Roboto Flex for Crystal). */
@Serializable enum class TypeFace { Style, Flex, Inter, SpaceGrotesk, DmSerif, JetBrainsMono, BodoniModa, Unbounded }
@Serializable enum class ClockSeparator { Colon, Dot, Bullet, Bar, Space, None }
/** [Stack]: hours above minutes; [Slant]: hours above-left, minutes below-right. */
@Serializable enum class ClockLayout { Row, Stack, Slant }
/** [Auto] follows the item's position (left third = start, …). */
@Serializable enum class ItemAlign { Auto, Start, Center, End }
/** [Auto]: caps for the long and weekday formats, as written for the others. */
@Serializable enum class TextCase { Auto, Upper, Lower }
@Serializable enum class DatePart { None, Number, Short, Long }

/**
 * Text look of a clock or date item. Unknown names decode to the defaults (`coerceInputValues`); numbers are
 * clamped by [LauncherSettings.sanitized]. Date-only fields are ignored on the clock and the other way round.
 */
@Serializable
data class TextLook(
    val face: TypeFace = TypeFace.Style,
    /** Letter spacing in hundredths of an em, [MIN_TRACKING]..[MAX_TRACKING]. */
    val tracking: Int = 0,
    val align: ItemAlign = ItemAlign.Auto,
    val separator: ClockSeparator = ClockSeparator.Colon,
    val layout: ClockLayout = ClockLayout.Row,
    val hours12: Boolean = false,
    val case: TextCase = TextCase.Auto,
    /** [DateFormat.Custom] parts: weekday (None/Short/Long), day, month (None/Number/Short/Long), year. */
    val weekday: DatePart = DatePart.Long,
    val day: Boolean = true,
    val month: DatePart = DatePart.Long,
    val year: Boolean = false,
    /** Date letters one under another, top to bottom. */
    val vertical: Boolean = false,
    /** Offset of a date bound to the clock, dp, ±[MAX_OFFSET]. */
    val dx: Float = 0f,
    val dy: Float = 0f,
    /** Extra room inside a background plate, dp each side, 0..[MAX_PAD]: the plate stretched by its handles. */
    val padX: Float = 0f,
    val padY: Float = 0f,
) {
    companion object {
        const val MIN_TRACKING = -10
        const val MAX_TRACKING = 40
        const val MAX_OFFSET = 96f
        const val MAX_PAD = 96f
    }
}

@Serializable enum class LensStyle { Ring, Filled }

/** LENS offset in dp from the screen centre in [this] layout; 0 when it has none yet. */
fun LayoutSpec.lensDx(): Float = items.firstOrNull { it.kind == ItemKind.Lens }?.anchor?.dx ?: 0f

/**
 * What Studio's miniature of Home takes from a layout. [row] is the LENS row in screen order, [LENS_SLOT] marking
 * LENS and null an empty slot; [list] the targets of the legacy text list; [dateAbove] a free date placed above the clock.
 */
data class HomeSketch(val dateAbove: Boolean, val row: List<HomeItem?>, val list: List<String>) {
    companion object {
        val LENS_SLOT = HomeItem("lens", ItemKind.Lens, Anchor.Lens)
    }
}

fun LayoutSpec.sketch(): HomeSketch {
    val clock = items.firstOrNull { it.kind == ItemKind.Clock }
    val date = items.firstOrNull { it.kind == ItemKind.Date }
    val dateAbove = clock != null && date != null && date.anchor.type == AnchorType.Free &&
        clock.anchor.type == AnchorType.Free && date.anchor.fy < clock.anchor.fy
    val bySlot = items.filter { it.anchor.type == AnchorType.LensRow }.associateBy { it.anchor.slot }
    val slots = Anchor.LENS_SLOTS.map(bySlot::get)
    val lens = HomeSketch.LENS_SLOT
    val row = when {
        slots.all { it == null } -> listOf(lens)
        else -> when (dock.lensAt) {
            LensAt.Middle -> slots.take(2) + lens + slots.drop(2)
            LensAt.Start -> listOf(lens) + slots
            LensAt.End -> slots + lens
        }
    }
    val list = items.filter { it.kind == ItemKind.App && it.anchor.type == AnchorType.Stack }.sortedBy { it.anchor.row }.map { it.target }
    return HomeSketch(dateAbove, row, list)
}

/** Date line formats, stored by name in [HomeItem.style]; empty = [Long]. */
enum class DateFormat { Long, Short, Weekday, Custom }

/**
 * What sits behind the clock and its bound date (§5.12); stored by name in [HomeItem.background].
 * [Veil] follows the glass chosen in Look; the others pin one glass to this plate.
 */
enum class ClockBackground { None, Veil, Crystal, Frost, Liquid }

/** Colour choices of the clock inspector, stored by name. */
enum class ItemTint { Accent, Light, Dark }

/** The screen edge the LENS row sits on (§5.5); on [Left] and [Right] the row runs vertically. */
@Serializable enum class DockEdge {
    Bottom, Top, Left, Right;
    val vertical get() = this == Left || this == Right
}

/** Where LENS stands in its row of four slots: between them, or before / after all four (start = left or top). */
@Serializable enum class LensAt { Middle, Start, End }

/**
 * The LENS row of a layout; absent in older documents = the bottom row with LENS in the middle, as before.
 * [gap] is LENS centre to the nearest slot and [pitch] slot to slot, dp; [spread] spaces the four slots evenly over
 * the edge instead. The geometry shrinks both when the row would not fit the edge.
 */
@Serializable data class DockSpec(
    val edge: DockEdge = DockEdge.Bottom,
    val lensAt: LensAt = LensAt.Middle,
    val gap: Int = GAP,
    val pitch: Int = PITCH,
    val spread: Boolean = false,
) {
    fun sanitized() = copy(gap = gap.coerceIn(MIN_GAP, MAX_GAP), pitch = pitch.coerceIn(MIN_PITCH, MAX_PITCH))

    companion object {
        const val GAP = 62
        const val PITCH = 46
        const val MIN_GAP = 52
        const val MAX_GAP = 140
        const val MIN_PITCH = 46
        const val MAX_PITCH = 140
    }
}

@Serializable
data class LayoutSpec(
    val id: String,
    val name: String = "",
    val base: LayoutBase = LayoutBase.None,
    val placement: Placement = Placement.Hybrid,
    val grid: GridSpec = GridSpec(),
    val guides: Boolean = true,
    val items: List<HomeItem> = emptyList(),
    val linked: Boolean = false,
    val dock: DockSpec = DockSpec(),
)

@Serializable
data class HomeLayouts(val active: String = LINKED_ID, val layouts: List<LayoutSpec> = emptyList()) {
    fun activeLayout(): LayoutSpec = layouts.firstOrNull { it.id == active } ?: layouts.first()

    /** Where LENS sits in the active layout, in dp from the screen centre (§5.5); Search and ARC follow it. */
    fun lensDx(): Float = layouts.firstOrNull { it.id == active }?.lensDx() ?: 0f

    /**
     * The Home Editor's ✓: one replacement of the document. An edited preset becomes the user's own copy
     * ([USER_ID], unlinked) and the preset itself stays; a user layout is replaced in place.
     */
    fun save(edited: LayoutSpec): HomeLayouts {
        val own = if (edited.linked || edited.id == LINKED_ID) edited.copy(id = USER_ID, linked = false) else edited
        val rest = layouts.filter { it.id != own.id }
        val kept = if (rest.size >= MAX_LAYOUTS) rest.take(MAX_LAYOUTS - 1) else rest
        return HomeLayouts(own.id, kept + own)
    }

    /**
     * Studio › Vzhled: one change to every app and FLOW icon of the active layout (names on or off, own sizes
     * dropped), saved as the editor's ✓ would; nothing is saved when no icon changes.
     */
    fun withIcons(transform: (HomeItem) -> HomeItem): HomeLayouts {
        val layout = activeLayout()
        val items = layout.items.map { if (it.kind == ItemKind.App || it.kind == ItemKind.Flow) transform(it) else it }
        return if (items == layout.items) this else save(layout.copy(items = items))
    }

    companion object {
        const val LINKED_ID = "preset"
        const val USER_ID = "mine"
        const val MAX_LAYOUTS = 8
        const val MAX_ITEMS = 40
        const val MAX_NAME = 40
    }
}

/** Built-in presets as code (§5.4): never stored except as the linked layout or a user's copy. */
object HomeTemplates {
    const val DEFAULT_NAME = "Moje plocha"

    fun build(base: LayoutBase, apps: List<String>, showClock: Boolean, showDate: Boolean, legacyList: Boolean = false): List<HomeItem> {
        val items = mutableListOf<HomeItem>()
        // Legacy "Vlastní" keeps the clock at the start edge above the text list.
        if (showClock) items += HomeItem("clock", ItemKind.Clock, Anchor.free(if (legacyList) 0f else .5f, 0f))
        if (showDate) items += HomeItem("date", ItemKind.Date, Anchor.Bound)
        items += HomeItem("lens", ItemKind.Lens, Anchor.Lens)
        if (legacyList) {
            apps.forEachIndexed { i, key -> items += HomeItem("app:$key", target = key, anchor = Anchor.stack(i), label = true) }
            return items
        }
        when (base) {
            LayoutBase.Pure, LayoutBase.None -> Unit
            LayoutBase.Essentials, LayoutBase.Balanced -> {
                val around = apps.take(4)
                // Same placement as phase B2: one app left of LENS, two either side, three fill the left pair first.
                val slots = when (around.size) {
                    1 -> listOf(-1)
                    2 -> listOf(-1, 1)
                    3 -> listOf(-2, -1, 1)
                    else -> Anchor.LENS_SLOTS
                }
                around.zip(slots).forEach { (key, slot) -> items += HomeItem("app:$key", target = key, anchor = Anchor.lensRow(slot)) }
                // Balanced: the rest sit on the lowest grid row above LENS.
                if (base == LayoutBase.Balanced) apps.drop(4).take(GridSpec().cols).forEachIndexed { i, key ->
                    items += HomeItem("app:$key", target = key, anchor = Anchor.cell(i, GridSpec().rows - 1))
                }
            }
        }
        return items
    }

    /**
     * A preset as an editor preview of [current]: same grid, placement and LENS, the clock keeps its look.
     * [LayoutBase.None] has no template; it stays as it is.
     */
    fun preview(base: LayoutBase, apps: List<String>, current: LayoutSpec): LayoutSpec {
        val clock = current.items.firstOrNull { it.kind == ItemKind.Clock }
        val lens = current.items.firstOrNull { it.kind == ItemKind.Lens }
        val items = build(base, apps, showClock = true, showDate = true).map {
            when (it.kind) {
                ItemKind.Clock -> clock?.copy(anchor = it.anchor) ?: it
                ItemKind.Lens -> lens ?: it
                else -> it
            }
        }
        return current.copy(id = HomeLayouts.LINKED_ID, base = base, items = items, linked = true)
    }

    /** The linked preset for the legacy fields of [s]. */
    fun linked(s: LauncherSettings): LayoutSpec {
        val (base, list) = when (s.layout) {
            HomeLayout.Pure -> LayoutBase.Pure to false
            HomeLayout.Essentials -> LayoutBase.Essentials to false
            HomeLayout.Custom -> LayoutBase.None to true
        }
        val items = build(base, s.favorites, s.showClock, s.showDate, list)
            .map { if (it.kind == ItemKind.Lens) it.copy(anchor = Anchor(AnchorType.Lens, dx = s.reach.dx)) else it }
        return LayoutSpec(HomeLayouts.LINKED_ID, DEFAULT_NAME, base, Placement.Hybrid, items = items, linked = true)
    }
}

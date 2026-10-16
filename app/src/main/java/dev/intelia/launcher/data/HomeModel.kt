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
 * - [AnchorType.Lens]: LENS itself, [fx] across, [bottomDp] above the gesture inset (reserved; LENS still follows Reach).
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
)

@Serializable enum class LensStyle { Ring, Filled }

/** Date line formats, stored by name in [HomeItem.style]; empty = [Long]. */
enum class DateFormat { Long, Short, Weekday }

/** Colour choices of the clock inspector, stored by name. */
enum class ItemTint { Accent, Light, Dark }

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
)

@Serializable
data class HomeLayouts(val active: String = LINKED_ID, val layouts: List<LayoutSpec> = emptyList()) {
    fun activeLayout(): LayoutSpec = layouts.firstOrNull { it.id == active } ?: layouts.first()

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
        return LayoutSpec(HomeLayouts.LINKED_ID, DEFAULT_NAME, base, Placement.Hybrid,
            items = build(base, s.favorites, s.showClock, s.showDate, list), linked = true)
    }
}

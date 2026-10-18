package dev.intelia.launcher.ui

import dev.intelia.launcher.data.HomeItem
import dev.intelia.launcher.data.ItemKind
import dev.intelia.launcher.data.TypeFace

/**
 * Veil 3.0 B5: a property of a Home element that follows a global value (Systém → motiv → rozložení → prvek) until the
 * element sets its own. One definition per property — where the own value is kept, how it is dropped and where the
 * global value is set ([source], a Studio part; null = the element kind's default) — drawn by every inspector as the
 * Globální / Vlastní label. Pure Kotlin, tested on the JVM.
 */
enum class ItemProp(val key: String) {
    /** App and FLOW icons follow Vzhled › Ikony (one size for all); clock, date and LENS their kind's default size. */
    Size("size"),
    /** Clock: the clock style of the theme and the style's face; date: the system face. */
    Font("font"),
    /** Clock: the face's own weight. */
    Weight("weight"),
    /** Clock and date: the ink taken from the wallpaper. */
    Tint("tint"),
    /** S2, app and FLOW: whether the name shows follows Vzhled › Ikony › Popisky. */
    Label("label"),
    /** S2, app and FLOW: the name's size, font, colour, alignment, gap and lines. */
    LabelLook("labelLook"),
    /** S2, app and FLOW: icon style, shape, colour and opacity. */
    Icon("icon");

    /** Undo key of a return to the global value: its own step, never merged with the slider drag before it. */
    val inheritKey: String get() = "$key/inherit"

    /** Where the global value is set, for the label's spoken hint; null = the element kind's default. */
    fun source(kind: ItemKind): StudioTarget? = when {
        (this == Size || this == Label || this == LabelLook || this == Icon) && (kind == ItemKind.App || kind == ItemKind.Flow) -> StudioTarget.Icons
        else -> null
    }

    fun isOwn(item: HomeItem): Boolean = when (this) {
        Size -> item.sizeDp != 0
        // A date's style is its format, not a face.
        Font -> item.look.face != TypeFace.Style || (item.kind == ItemKind.Clock && item.style.isNotEmpty())
        Weight -> item.weight != 0
        Tint -> item.tint.isNotEmpty()
        Label -> item.label != null
        LabelLook -> item.labelLook != null
        Icon -> item.icon != null
    }

    /** [item] with this property following the global value again; everything else unchanged. */
    fun inherit(item: HomeItem): HomeItem = when (this) {
        Size -> item.copy(sizeDp = 0)
        Font -> item.copy(look = item.look.copy(face = TypeFace.Style), style = if (item.kind == ItemKind.Clock) "" else item.style)
        Weight -> item.copy(weight = 0)
        Tint -> item.copy(tint = "")
        Label -> item.copy(label = null)
        LabelLook -> item.copy(labelLook = null)
        Icon -> item.copy(icon = null)
    }
}

/** Returns [prop] of [id] to its global value as one undo step; false when it already follows it. */
fun HomeEditorState.inherit(id: String, prop: ItemProp): Boolean {
    val before = item(id) ?: return false
    if (!prop.isOwn(before)) return false
    edit(id, prop.inheritKey, prop::inherit)
    return true
}

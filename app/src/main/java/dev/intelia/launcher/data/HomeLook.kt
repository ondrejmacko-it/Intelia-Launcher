package dev.intelia.launcher.data

import kotlinx.serialization.Serializable

/**
 * S2: how Home names its app and FLOW icons. One global default ([LauncherSettings.labels]); an item overrides single
 * properties with [LabelOverride], and whether its name shows at all with [HomeItem.label].
 */
@Serializable
data class LabelStyle(
    /** Names under the icons by default. */
    val show: Boolean = false,
    val sizeSp: Int = 12,
    /** [TypeFace.Style] = the launcher's own body font. */
    val face: TypeFace = TypeFace.Style,
    /** 100..900. */
    val weight: Int = 400,
    /** One of [ItemTint]; empty = the wallpaper-derived ink. */
    val tint: String = "",
    /** Text alignment under the icon; [ItemAlign.Auto] = centred. */
    val align: ItemAlign = ItemAlign.Center,
    /** Room between the icon and its name, dp. */
    val gap: Int = 4,
    /** Lines a long name may take before it ends with "…". */
    val lines: Int = 1,
) {
    fun sanitized() = copy(
        sizeSp = sizeSp.coerceIn(MIN_SIZE, MAX_SIZE),
        weight = (weight.coerceIn(100, 900) / 100) * 100,
        tint = tint.takeIf { t -> ItemTint.entries.any { it.name == t } }.orEmpty(),
        gap = gap.coerceIn(0, MAX_GAP),
        lines = lines.coerceIn(1, MAX_LINES),
    )

    companion object {
        const val MIN_SIZE = 9
        const val MAX_SIZE = 20
        const val MAX_GAP = 24
        const val MAX_LINES = 3
    }
}

/** An item's own label properties; null = the global [LabelStyle]'s. */
@Serializable
data class LabelOverride(
    val sizeSp: Int? = null,
    val face: TypeFace? = null,
    val weight: Int? = null,
    val tint: String? = null,
    val align: ItemAlign? = null,
    val gap: Int? = null,
    val lines: Int? = null,
) {
    fun over(base: LabelStyle): LabelStyle = base.copy(
        sizeSp = sizeSp ?: base.sizeSp, face = face ?: base.face, weight = weight ?: base.weight, tint = tint ?: base.tint,
        align = align ?: base.align, gap = gap ?: base.gap, lines = lines ?: base.lines,
    ).sanitized()

    /** Bounds each set value; null when nothing is left to override. */
    fun sanitized(): LabelOverride? = LabelOverride(
        sizeSp = sizeSp?.coerceIn(LabelStyle.MIN_SIZE, LabelStyle.MAX_SIZE),
        face = face,
        weight = weight?.let { (it.coerceIn(100, 900) / 100) * 100 },
        tint = tint?.takeIf { t -> t.isEmpty() || ItemTint.entries.any { it.name == t } },
        align = align,
        gap = gap?.coerceIn(0, LabelStyle.MAX_GAP),
        lines = lines?.coerceIn(1, LabelStyle.MAX_LINES),
    ).takeIf { it != LabelOverride() }
}

/** An item's own icon look; null = the global default (style, shape, [LauncherSettings.iconTint], [LauncherSettings.iconAlpha]). */
@Serializable
data class IconOverride(
    val style: IconStyle? = null,
    val shape: IconShape? = null,
    /** One of [ItemTint], or empty for the wallpaper ink. Colours the glyph of Mono, Veil and Tinted icons. */
    val tint: String? = null,
    val alpha: Float? = null,
) {
    fun sanitized(): IconOverride? = copy(
        tint = tint?.takeIf { t -> t.isEmpty() || ItemTint.entries.any { it.name == t } },
        alpha = alpha?.takeIf { it.isFinite() }?.coerceIn(MIN_ALPHA, 1f),
    ).takeIf { it != IconOverride() }

    companion object {
        const val MIN_ALPHA = .3f
    }
}

/** What an icon on Home finally looks like after the global defaults and its own overrides. */
data class IconLook(val sizeDp: Int, val style: IconStyle, val shape: IconShape, val tint: String, val alpha: Float)

fun HomeItem.iconLook(s: LauncherSettings): IconLook = IconLook(
    sizeDp = if (sizeDp == 0) s.iconSize else sizeDp.coerceIn(LauncherSettings.MIN_ICON, LauncherSettings.MAX_ICON),
    style = icon?.style ?: s.iconStyle,
    shape = icon?.shape ?: s.iconShape,
    tint = icon?.tint ?: s.iconTint,
    alpha = icon?.alpha ?: s.iconAlpha,
)

/** Whether this item's name shows on Home: its own choice, else the global default. */
fun HomeItem.labelShown(global: LabelStyle): Boolean =
    (kind == ItemKind.App || kind == ItemKind.Flow) && (label ?: global.show)

fun HomeItem.labelStyle(global: LabelStyle): LabelStyle = labelLook?.over(global) ?: global

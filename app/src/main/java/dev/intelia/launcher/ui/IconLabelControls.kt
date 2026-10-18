package dev.intelia.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.intelia.launcher.R
import dev.intelia.launcher.data.IconLook
import dev.intelia.launcher.data.IconOverride
import dev.intelia.launcher.data.IconShape
import dev.intelia.launcher.data.IconStyle
import dev.intelia.launcher.data.ItemAlign
import dev.intelia.launcher.data.ItemTint
import dev.intelia.launcher.data.LabelOverride
import dev.intelia.launcher.data.LabelStyle
import dev.intelia.launcher.data.TypeFace
import kotlin.math.roundToInt

/**
 * S2: one set of controls for the names under Home icons, used for the global default (Vzhled › Ikony) and for one
 * item's exception (the editor's card). [onChange] gets the whole new style.
 */
@Composable
internal fun LabelStyleControls(style: LabelStyle, onChange: (LabelStyle) -> Unit) {
    SliderRow(stringResource(R.string.label_size), style.sizeSp.toFloat(), LabelStyle.MIN_SIZE.toFloat(), LabelStyle.MAX_SIZE.toFloat(), 1f,
        "${style.sizeSp} sp") { onChange(style.copy(sizeSp = it.roundToInt())) }
    Kick(stringResource(R.string.label_font))
    Chips(LABEL_FACES, style.face, family = { faceFamily(it, style.weight) ?: flex(style.weight, 100f) }) { onChange(style.copy(face = it)) }
    Kick(stringResource(R.string.inspect_weight))
    Chips(listOf(300 to "300", 400 to "400", 500 to "500", 600 to "600", 700 to "700"), style.weight) { onChange(style.copy(weight = it)) }
    Kick(stringResource(R.string.inspect_color))
    Chips(tintChoices(), style.tint) { onChange(style.copy(tint = it)) }
    Kick(stringResource(R.string.label_align))
    Chips(listOf(ItemAlign.Start to stringResource(R.string.align_start), ItemAlign.Center to stringResource(R.string.align_center),
        ItemAlign.End to stringResource(R.string.align_end)), if (style.align == ItemAlign.Auto) ItemAlign.Center else style.align) {
        onChange(style.copy(align = it))
    }
    SliderRow(stringResource(R.string.label_gap), style.gap.toFloat(), 0f, LabelStyle.MAX_GAP.toFloat(), 1f, "${style.gap} dp") {
        onChange(style.copy(gap = it.roundToInt()))
    }
    Kick(stringResource(R.string.label_lines))
    Chips((1..LabelStyle.MAX_LINES).map { it to it.toString() }, style.lines) { onChange(style.copy(lines = it)) }
}

/** S2: style, disc shape, glyph colour and opacity of Home icons; the size has its own slider beside. */
@Composable
internal fun IconLookControls(look: IconLook, onChange: (IconLook) -> Unit) {
    Kick(stringResource(R.string.icons_style))
    Chips(listOf(IconStyle.System to stringResource(R.string.icons_system), IconStyle.Ink to stringResource(R.string.icons_ink),
        IconStyle.Veil to stringResource(R.string.look_veil), IconStyle.Tinted to stringResource(R.string.icons_tinted)), look.style) {
        onChange(look.copy(style = it))
    }
    if (look.style == IconStyle.Veil || look.style == IconStyle.Tinted) {
        Kick(stringResource(R.string.icons_shape))
        Chips(listOf(IconShape.Circle to stringResource(R.string.shape_circle), IconShape.Squircle to stringResource(R.string.shape_squircle),
            IconShape.Rounded to stringResource(R.string.shape_rounded), IconShape.Drop to stringResource(R.string.shape_drop)), look.shape) {
            onChange(look.copy(shape = it))
        }
    }
    // Colour reaches the glyph of Mono, Veil and Tinted icons; full-colour system icons keep their own.
    if (look.style != IconStyle.System) {
        Kick(stringResource(R.string.inspect_color))
        Chips(tintChoices(), look.tint) { onChange(look.copy(tint = it)) }
    }
    val pct = (look.alpha * 100).roundToInt()
    SliderRow(stringResource(R.string.inspect_opacity), pct.toFloat(), IconOverride.MIN_ALPHA * 100, 100f, 5f, "$pct %") {
        onChange(look.copy(alpha = it / 100f))
    }
}

@Composable
private fun tintChoices() = listOf("" to stringResource(R.string.tint_auto), ItemTint.Accent.name to stringResource(R.string.tint_accent),
    ItemTint.Light.name to stringResource(R.string.tint_light), ItemTint.Dark.name to stringResource(R.string.tint_dark))

private val LABEL_FACES = listOf(TypeFace.Style to "Auto", TypeFace.Inter to "Inter", TypeFace.Flex to "Flex",
    TypeFace.SpaceGrotesk to "Space Grotesk", TypeFace.JetBrainsMono to "JetBrains Mono", TypeFace.DmSerif to "DM Serif")

/** An item's exception: the properties of [own] that differ from [global]; null when none do. */
internal fun labelOverride(own: LabelStyle, global: LabelStyle): LabelOverride? = LabelOverride(
    sizeSp = own.sizeSp.takeIf { it != global.sizeSp }, face = own.face.takeIf { it != global.face },
    weight = own.weight.takeIf { it != global.weight }, tint = own.tint.takeIf { it != global.tint },
    align = own.align.takeIf { it != global.align }, gap = own.gap.takeIf { it != global.gap }, lines = own.lines.takeIf { it != global.lines },
).sanitized()

/** An item's exception: style, shape, colour and opacity of [own] that differ from [global]; null when none do. */
internal fun iconOverride(own: IconLook, global: IconLook): IconOverride? = IconOverride(
    style = own.style.takeIf { it != global.style }, shape = own.shape.takeIf { it != global.shape },
    tint = own.tint.takeIf { it != global.tint }, alpha = own.alpha.takeIf { it != global.alpha },
).sanitized()

/** Glyph colour choices for icons (Auto = the wallpaper ink). */
@Composable
internal fun IconTintChips(selected: String, onSelect: (String) -> Unit) = Chips(tintChoices(), selected, onSelect = onSelect)

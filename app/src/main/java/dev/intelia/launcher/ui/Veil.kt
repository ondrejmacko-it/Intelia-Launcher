package dev.intelia.launcher.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Density of a Veil surface (D12 as approved, DESIGN_SYSTEM §5.2): matte, calm, sharp text.
 * Light = small controls, Dense = contextual editors and sheets, Focus = Studio.
 * Not a blur: the Nothing Phone (3) has no cross-window blur, so density is what keeps text legible.
 */
enum class VeilLevel { Light, Dense, Focus }

/** Point the surface came from, as a fraction of its own size; may lie outside 0..1 (e.g. below a sheet). */
@Immutable
data class VeilOrigin(val fx: Float, val fy: Float) {
    companion object {
        val Top = VeilOrigin(.5f, 0f)
        val Bottom = VeilOrigin(.5f, 1f)
    }
}

@Immutable
data class VeilTokens(
    val top: Color,
    val bottom: Color,
    /** Edge far from the origin. */
    val edgeCold: Color,
    /** Edge at the origin: the accent, softened. */
    val edgeHot: Color,
    /** Inner light at the origin; very faint. */
    val glow: Color,
)

/** Fill opacity per level: Light 66–72 %, Dense 80–85 %, Focus 90–94 % (top → bottom). */
internal fun veilAlpha(level: VeilLevel): Pair<Float, Float> = when (level) {
    VeilLevel.Light -> .66f to .72f
    VeilLevel.Dense -> .80f to .85f
    VeilLevel.Focus -> .90f to .94f
}

fun veilTokens(level: VeilLevel, dark: Boolean, acc: Color): VeilTokens {
    val (aTop, aBottom) = veilAlpha(level)
    // Neutral matte bases with a trace (6 %) of the accent, so the material belongs to the theme
    // without turning into a coloured glass.
    val base = if (dark) Color(0xFF15171B) else Color(0xFFF5F6F7)
    val deep = if (dark) Color(0xFF0E1013) else Color(0xFFEDEFF1)
    val tinted = lerp(base, acc, .06f)
    return VeilTokens(
        top = tinted.copy(alpha = aTop),
        bottom = deep.copy(alpha = aBottom),
        edgeCold = if (dark) Color.White.copy(alpha = .07f) else Color.Black.copy(alpha = .07f),
        edgeHot = lerp(acc, if (dark) Color.White else Color.Black, .35f).copy(alpha = if (dark) .55f else .45f),
        glow = acc.copy(alpha = if (dark) .07f else .05f),
    )
}

/**
 * The Veil material: a vertical fill, a 1 dp edge lit gently only near [origin], and a faint inner glow there.
 * Static; brushes are rebuilt only when the size changes. Clips content to [shape].
 */
fun Modifier.veilSurface(
    tokens: VeilTokens,
    origin: VeilOrigin = VeilOrigin.Top,
    shape: Shape = RoundedCornerShape(CrystalDims.radiusSheet),
): Modifier = clip(shape).drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val fill = Brush.verticalGradient(listOf(tokens.top, tokens.bottom))
    val o = Offset(origin.fx * size.width, origin.fy * size.height)
    val reach = max(150.dp.toPx(), 1f)
    val edge = Brush.radialGradient(listOf(tokens.edgeHot, tokens.edgeCold), center = o, radius = reach)
    val glow = Brush.radialGradient(listOf(tokens.glow, Color.Transparent), center = o, radius = 1.6f * reach)
    val stroke = Stroke(1.dp.toPx())
    onDrawWithContent {
        drawOutline(outline, fill)
        drawOutline(outline, glow)
        drawContent()
        drawOutline(outline, edge, style = stroke)
    }
}

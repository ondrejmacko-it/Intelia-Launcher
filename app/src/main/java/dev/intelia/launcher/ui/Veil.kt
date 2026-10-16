package dev.intelia.launcher.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
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

/**
 * Fill opacity per level (Clean Veil, §5.2): Light 62–68 %, Dense 77–83 %, Focus 88–93 % (top → bottom).
 * Text stays ≥ 4.5:1 on Light even over a white (dark theme) or black (light theme) wallpaper; a JVM test checks it.
 */
internal fun veilAlpha(level: VeilLevel): Pair<Float, Float> = when (level) {
    VeilLevel.Light -> .62f to .68f
    VeilLevel.Dense -> .77f to .83f
    VeilLevel.Focus -> .88f to .93f
}

/** The owner's Veil choices from the editor's Look panel (§5.2): Solid panels, density shift, origin edge. */
@Immutable
data class VeilPrefs(val solid: Boolean = false, val shift: Int = 0, val edge: Boolean = true)

val LocalVeil = staticCompositionLocalOf { VeilPrefs() }

/** Tokens for [level] in the current theme and the owner's Veil choices. */
@Composable
fun veil(level: VeilLevel): VeilTokens {
    val c = LocalCrystal.current
    return veilTokens(level, c.dark, c.acc, LocalVeil.current)
}

fun veilTokens(level: VeilLevel, dark: Boolean, acc: Color, prefs: VeilPrefs = VeilPrefs()): VeilTokens {
    if (prefs.solid) {
        // §5.2 Solid: opaque matte fill, 1 dp white @ 15 % (black in light), accent only at the origin.
        val solid = if (dark) Color(0xFF16181C) else Color(0xFFF5F6F7)
        val edge = if (dark) Color.White.copy(alpha = .15f) else Color.Black.copy(alpha = .15f)
        return VeilTokens(solid, solid, edge, if (prefs.edge) lerp(acc, edge, .3f) else edge, Color.Transparent)
    }
    val shift = prefs.shift.coerceIn(-10, 10) / 100f
    val (aTop, aBottom) = veilAlpha(level).let { (t, b) -> (t + shift).coerceIn(.5f, 1f) to (b + shift).coerceIn(.5f, 1f) }
    // Clean Veil: deeper, purer neutral bases with only a trace (3 %) of the accent, so the wallpaper shows through
    // as itself rather than under a grey film; a brighter cold edge keeps the panel's outline crisp.
    val base = if (dark) Color(0xFF101216) else Color(0xFFF7F8F9)
    val deep = if (dark) Color(0xFF0B0C0F) else Color(0xFFF0F2F4)
    val tinted = lerp(base, acc, .03f)
    return VeilTokens(
        top = tinted.copy(alpha = aTop),
        bottom = deep.copy(alpha = aBottom),
        edgeCold = if (dark) Color.White.copy(alpha = .14f) else Color.Black.copy(alpha = .10f),
        edgeHot = if (prefs.edge) lerp(acc, if (dark) Color.White else Color.Black, .35f).copy(alpha = if (dark) .55f else .45f)
            else if (dark) Color.White.copy(alpha = .18f) else Color.Black.copy(alpha = .14f),
        glow = acc.copy(alpha = if (dark) .06f else .04f),
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

package dev.intelia.launcher.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import dev.intelia.launcher.data.Glass
import kotlin.math.max

/**
 * Density of the one Veil material (D15): Light = editor bar, tray, toast, clock plate; Dense = inspectors, sheets,
 * dialogs; Focus = Studio, Search, the drawer. Depth under an open surface comes from the shared backdrop
 * ([veilBackdrop]), not from each panel.
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
    /** The crystal edge (1 dp): light at the top, fading by the middle, a faint accent at the bottom. */
    val edgeTop: Color,
    val edgeMid: Color,
    val edgeBottom: Color,
    /** Inner light at the origin; very faint. */
    val glow: Color,
    /** Milky light from the top edge, fading out by [sheenReach] of the height (Frost). */
    val sheen: Color = Color.Transparent,
    val sheenReach: Float = 1f,
    /** Opacity of the fine frosted grain (Frost); 0 = none. */
    val grain: Float = 0f,
    /** Diagonal facet streaks across the panel (Crystal). */
    val streak: Color = Color.Transparent,
    /** A soft glow just inside the outline (Liquid). */
    val innerGlow: Color = Color.Transparent,
)

/**
 * Body opacity per density (D15 revision 2). The no-blur column is the default: the Nothing Phone (3) has no
 * cross-window blur. [blurred] only where the system really blurs the wallpaper behind the window.
 * Text stays ≥ 4.5:1 on Light over a white (dark theme) or black (light theme) wallpaper; a JVM test checks it.
 */
internal fun veilAlpha(level: VeilLevel, blurred: Boolean = false): Float = when (level) {
    VeilLevel.Light -> if (blurred) .48f else .64f
    VeilLevel.Dense -> if (blurred) .66f else .80f
    VeilLevel.Focus -> if (blurred) .60f else .74f
}

/** Accent tint laid over the body per density; quieter as the surface grows. */
private fun veilTint(level: VeilLevel): Float = when (level) {
    VeilLevel.Light -> .05f
    VeilLevel.Dense -> .045f
    VeilLevel.Focus -> .035f
}

/**
 * A preset of the one surface (D15): Veil, Crystal, Frost, Liquid and Matte change only how the crystal edge is lit
 * and how milky the body is, never the material. Edge alphas are for the dark theme; the light theme lifts the top
 * light, since white on a pale body needs more to show.
 */
@Immutable
internal data class VeilPreset(
    val edgeTop: Float,
    val edgeMid: Float,
    val edgeAccent: Float,
    /** Extra body opacity. */
    val body: Float = 0f,
    val milk: Float = 0f,
    val grain: Float = 0f,
    val facets: Float = 0f,
    val innerGlow: Float = 0f,
)

internal fun veilPreset(glass: Glass, matte: Boolean): VeilPreset = if (matte) VeilPreset(.08f, .04f, .06f) else when (glass) {
    Glass.Veil -> VeilPreset(.16f, .035f, .24f)
    Glass.Crystal -> VeilPreset(.30f, .05f, .16f, facets = .07f)
    Glass.Frost -> VeilPreset(.12f, .06f, .10f, body = .05f, milk = .05f, grain = .05f)
    Glass.Liquid -> VeilPreset(.24f, .16f, .14f, innerGlow = .06f)
}

/** Any light a preset adds over a dark body lowers the contrast of white text, so the body grows by as much. */
private fun lift(p: VeilPreset, dark: Boolean): Float = if (dark) 2f * (p.facets + p.innerGlow + p.milk) else 0f

/** Matte (the "Solid panels" choice): near-opaque per density. */
private fun matteAlpha(level: VeilLevel): Float = when (level) {
    VeilLevel.Light -> .97f
    VeilLevel.Dense -> .99f
    VeilLevel.Focus -> 1f
}

/**
 * The owner's Veil choices: Matte panels, density shift, the accent at the bottom of the edge, the preset.
 * [blurred] is set by the backdrop only while the system blurs the wallpaper behind the window.
 */
@Immutable
data class VeilPrefs(
    val solid: Boolean = false,
    val shift: Int = 0,
    val edge: Boolean = true,
    val glass: Glass = Glass.Veil,
    val blurred: Boolean = false,
)

val LocalVeil = staticCompositionLocalOf { VeilPrefs() }

/** Tokens for [level] in the current theme and the owner's Veil choices. */
@Composable
fun veil(level: VeilLevel): VeilTokens {
    val c = LocalCrystal.current
    return veilTokens(level, c.dark, c.acc, LocalVeil.current)
}

/**
 * A clock or date plate (§5.12): [background] is a [dev.intelia.launcher.data.ClockBackground] name; Veil follows
 * the preset chosen in Look, the others pin their own preset to the plate.
 */
@Composable
fun plateTokens(background: String): VeilTokens {
    val c = LocalCrystal.current
    val prefs = LocalVeil.current
    val glass = when (background) {
        "Crystal" -> Glass.Crystal
        "Frost" -> Glass.Frost
        "Liquid" -> Glass.Liquid
        else -> null
    }
    return if (glass == null) veilTokens(VeilLevel.Light, c.dark, c.acc, prefs) else plateGlass(glass, c.dark, c.acc, prefs.shift)
}

/**
 * A plate's own preset: the same surface made bolder, since a plate is small and sits right on the wallpaper —
 * brighter edge and facets, a coarser grain, a denser body. Text on it keeps ≥ 4.5:1 (a JVM test checks it).
 */
fun plateGlass(glass: Glass, dark: Boolean, acc: Color, shift: Int = 0): VeilTokens {
    val t = veilTokens(VeilLevel.Light, dark, acc, VeilPrefs(shift = shift, glass = glass))
    if (glass == Glass.Veil) return t
    fun Color.more(k: Float, cap: Float = 1f) = copy(alpha = (alpha * k).coerceAtMost(cap))
    fun Color.denser(by: Float) = copy(alpha = (alpha + by).coerceIn(0f, .97f))
    return t.copy(
        top = t.top.denser(.06f), bottom = t.bottom.denser(.06f),
        edgeTop = t.edgeTop.more(1.6f, .95f), edgeMid = t.edgeMid.more(1.5f),
        streak = t.streak.more(2f, .2f), innerGlow = t.innerGlow.more(2f, .2f),
        grain = (t.grain * 1.5f).coerceAtMost(.12f),
    )
}

/** The one material: body + accent tint + crystal edge, then the preset's light. */
fun veilTokens(level: VeilLevel, dark: Boolean, acc: Color, prefs: VeilPrefs = VeilPrefs()): VeilTokens {
    val p = veilPreset(prefs.glass, prefs.solid)
    val shift = prefs.shift.coerceIn(-10, 10) / 100f
    val a = if (prefs.solid) matteAlpha(level)
        else (veilAlpha(level, prefs.blurred) + (if (!dark && level == VeilLevel.Light) .06f else 0f) + p.body + lift(p, dark) + shift).coerceIn(.45f, 1f)
    val base = if (dark) Color(0xFF0D0F12) else Color(0xFFF7F8FA)
    // The accent at [t] over the body at [a], as one colour: alpha t + a(1 - t), accent share t / alpha.
    val t = if (prefs.solid) 0f else veilTint(level)
    val alpha = t + a * (1f - t)
    val body = lerp(base, acc, t / alpha).copy(alpha = alpha)
    val hi = Color.White
    return VeilTokens(
        top = body, bottom = body,
        edgeTop = hi.copy(alpha = if (dark) p.edgeTop else minOf(.9f, p.edgeTop * 3f)),
        edgeMid = hi.copy(alpha = p.edgeMid),
        edgeBottom = if (prefs.edge) acc.copy(alpha = p.edgeAccent) else hi.copy(alpha = p.edgeMid),
        glow = if (prefs.solid) Color.Transparent else acc.copy(alpha = if (dark) .05f else .035f),
        sheen = hi.copy(alpha = p.milk),
        grain = p.grain,
        streak = hi.copy(alpha = p.facets),
        innerGlow = hi.copy(alpha = p.innerGlow),
    )
}

/** A 64 px tile of white and black specks, made once; tiled at [VeilTokens.grain] it reads as frosted glass. */
private val grainTile: ImageBitmap by lazy {
    val n = 64
    val pixels = IntArray(n * n)
    var seed = 0x2545F491L
    for (i in pixels.indices) {
        seed = (seed * 6364136223846793005L + 1442695040888963407L)
        val v = (seed ushr 33).toInt()
        val alpha = (v and 0xFF)
        pixels[i] = if (v and 0x100 != 0) (alpha shl 24) or 0xFFFFFF else alpha shl 24
    }
    android.graphics.Bitmap.createBitmap(pixels, n, n, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
}

/**
 * The Veil surface: body, the crystal edge, a faint glow at [origin] and the preset's light. Static; brushes are
 * rebuilt only when the size changes. Clips content to [shape].
 */
fun Modifier.veilSurface(
    tokens: VeilTokens,
    origin: VeilOrigin = VeilOrigin.Top,
    shape: Shape = RoundedCornerShape(CrystalDims.radiusSheet),
): Modifier = clip(shape).drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val fill = if (tokens.top == tokens.bottom) SolidColor(tokens.top) else Brush.verticalGradient(listOf(tokens.top, tokens.bottom))
    val o = Offset(origin.fx * size.width, origin.fy * size.height)
    val glow = if (tokens.glow.alpha > 0f) Brush.radialGradient(listOf(tokens.glow, Color.Transparent), center = o,
        radius = max(240.dp.toPx(), 1f)) else null
    val edge = Brush.verticalGradient(0f to tokens.edgeTop, .45f to tokens.edgeMid, 1f to tokens.edgeBottom)
    val stroke = Stroke(2.dp.toPx()) // half clipped by the shape: a 1 dp edge just inside
    val sheen = if (tokens.sheen.alpha > 0f) Brush.verticalGradient(listOf(tokens.sheen, tokens.sheen.copy(alpha = tokens.sheen.alpha * .4f)),
        0f, max(tokens.sheenReach * size.height, 1f)) else null
    val grain = if (tokens.grain > 0f) ShaderBrush(ImageShader(grainTile, TileMode.Repeated, TileMode.Repeated)) else null
    val streak = if (tokens.streak.alpha > 0f) {
        // Two facets across the diagonal: a broad one before the middle, a thin fainter one after it.
        val c = tokens.streak; val t = Color.Transparent
        Brush.linearGradient(0f to t, .28f to t, .36f to c, .44f to t, .62f to t, .68f to c.copy(alpha = c.alpha * .6f), .74f to t, 1f to t,
            start = Offset.Zero, end = Offset(size.width, size.height))
    } else null
    val innerGlow = if (tokens.innerGlow.alpha > 0f) Stroke(28.dp.toPx()) else null
    val innerCore = Stroke(12.dp.toPx())
    val bloom = glassBloom(tokens.glow, size.width, size.height)
    val glint = glassGlint(tokens.edgeTop, size.width, size.height)
    onDrawWithContent {
        drawOutline(outline, fill)
        glow?.let { drawOutline(outline, it) }
        bloom?.let { drawBloom(it) }
        grain?.let { drawOutline(outline, it, alpha = tokens.grain) }
        sheen?.let { drawOutline(outline, it) }
        streak?.let { drawOutline(outline, it) }
        innerGlow?.let {
            // Two half-clipped bands, 14 dp and 6 dp inside: light thickening toward the outline.
            drawOutline(outline, tokens.innerGlow, alpha = .5f, style = it)
            drawOutline(outline, tokens.innerGlow, alpha = .5f, style = innerCore)
        }
        drawContent()
        drawOutline(outline, edge, style = stroke)
        glint?.let { drawOutline(outline, it, style = stroke) }
    }
}

/**
 * Veil glass (S4): the owner's accent bleeding through from the top-left corner, as if a coloured light sat behind
 * the pane. [glow] is the tokens' faint accent; the bloom is a few times stronger but stays far from the text.
 */
internal fun glassBloom(glow: Color, w: Float, h: Float): Brush? = if (glow.alpha <= 0f) null else
    Brush.radialGradient(listOf(glow.copy(alpha = (glow.alpha * 2.6f).coerceAtMost(.16f)), Color.Transparent),
        center = Offset(w * .08f, 0f), radius = bloomRadius(w, h))

private fun bloomRadius(w: Float, h: Float) = max(minOf(w * .7f, h * 1.4f), 1f)

/**
 * The bloom is transparent beyond its radius, so it is filled only over the corner it reaches (S4.2: filling the
 * whole drawer surface with the gradient cost 3–5 % janky frames on the Phone (3)). Callers clip to their shape.
 */
internal fun DrawScope.drawBloom(bloom: Brush) {
    val r = bloomRadius(size.width, size.height)
    drawRect(bloom, size = Size(minOf(size.width, size.width * .08f + r), minOf(size.height, r)))
}

/**
 * Veil glass (S4): the hairline catches light at two corners — strong at the top-left, a smaller glint at the
 * bottom-right — and stays dark along the long sides. Drawn over the regular edge.
 */
internal fun glassGlint(edgeTop: Color, w: Float, h: Float): Brush? {
    if (edgeTop.alpha <= .05f) return null
    val t = Color.Transparent
    val hi = edgeTop.copy(alpha = (edgeTop.alpha * 2.4f).coerceAtMost(.7f))
    return Brush.linearGradient(0f to hi, .2f to t, .8f to t, 1f to hi.copy(alpha = hi.alpha * .8f),
        start = Offset.Zero, end = Offset(w, h))
}

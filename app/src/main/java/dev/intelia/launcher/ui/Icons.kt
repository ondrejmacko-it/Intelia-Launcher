package dev.intelia.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.intelia.launcher.data.IconShape
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.IconStyle
import dev.intelia.launcher.data.PaletteTarget
import dev.intelia.launcher.data.RenderedIcon
import dev.intelia.launcher.data.ShortcutEntry
import dev.intelia.launcher.intelia

private val grey = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * App icon in the chosen style. Ink style draws the app's themed glyph in [tint]; apps without one
 * fall back to a desaturated icon so the row still reads as one family.
 */
@Composable
fun AppIcon(app: AppEntry, modifier: Modifier = Modifier, tint: Color = LocalInk.current.ink) {
    val icons = LocalContext.current.intelia.icons
    val style = LocalIconStyle.current
    val ink = style != IconStyle.System
    val revision by icons.revision.collectAsStateWithLifecycle()
    // A cached icon is drawn in the first frame and needs no coroutine; only misses load in the background.
    val cached = remember(app.key, ink, revision) { icons.peek(app.key, ink) }
    val icon = cached ?: produceState<RenderedIcon?>(null, app.key, ink, revision) { value = icons.load(app, ink) }.value
    RenderedIconImage(icon, ink, tint, modifier, style)
}

@Composable
fun ShortcutIcon(shortcut: ShortcutEntry, modifier: Modifier = Modifier, tint: Color = LocalInk.current.ink) {
    val icons = LocalContext.current.intelia.icons
    val style = LocalIconStyle.current
    val ink = style != IconStyle.System
    val icon by produceState(icons.peek(shortcut.key, ink), shortcut.key, ink) { value = icons.loadShortcut(shortcut.key, ink) }
    RenderedIconImage(icon, ink, tint, modifier, style)
}

@Composable
private fun RenderedIconImage(icon: RenderedIcon?, ink: Boolean, inkTint: Color, modifier: Modifier, style: IconStyle) {
    val veil = style == IconStyle.Veil || style == IconStyle.Tinted
    val tint = if (style == IconStyle.Tinted) LocalPlaces.current[PaletteTarget.Icons] else inkTint
    // §5.8 Tinted: a Veil disc (theme base @ 62 %) washed with the colour (16 %), its ring @ 40 %; the base keeps
    // the glyph legible on light and dark wallpapers alike.
    val base = if (LocalCrystal.current.dark) Color(0xFF15171B) else Color(0xFFF5F6F7)
    val shape = iconShape(LocalIconShape.current)
    Box(if (!veil) modifier else modifier.drawBehind {
        val fill = shape.createOutline(size, layoutDirection, this)
        val inset = .5.dp.toPx()
        val ring = shape.createOutline(Size(size.width - 2 * inset, size.height - 2 * inset), layoutDirection, this)
        fun ring(color: Color) = translate(inset, inset) { drawOutline(ring, color, style = Stroke(1.dp.toPx())) }
        if (style == IconStyle.Tinted) {
            drawOutline(fill, base.copy(alpha = .62f))
            drawOutline(fill, tint.copy(alpha = .16f))
            ring(tint.copy(alpha = .40f))
        } else {
            // §5.8 Veil style: the glyph on a faint disc (tint @ 9 %) with a 1 dp ring (tint @ 22 %).
            drawOutline(fill, tint.copy(alpha = .09f))
            ring(tint.copy(alpha = .22f))
        }
    }, contentAlignment = Alignment.Center) {
        icon?.let {
            val filter = when {
                it.glyph -> ColorFilter.tint(tint)
                ink -> grey
                else -> null
            }
            Image(it.bitmap, contentDescription = null, modifier = if (veil) Modifier.fillMaxSize(.78f) else Modifier.fillMaxSize(), colorFilter = filter)
        }
    }
}

/** Squircle: a superellipse |x|⁴ + |y|⁴ = 1, softer than a rounded square, sampled finely enough for 64 dp. */
private val Squircle = GenericShape { size, _ ->
    val rx = size.width / 2f
    val ry = size.height / 2f
    for (i in 0 until 96) {
        val t = i / 96f * 2f * Math.PI.toFloat()
        val c = cos(t)
        val s = sin(t)
        val x = rx + rx * sign(c) * abs(c).pow(.5f)
        val y = ry + ry * sign(s) * abs(s).pow(.5f)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** The icon disc outline (§5.8): Drop keeps one tighter corner at the top start, like a falling drop. */
fun iconShape(shape: IconShape): Shape = when (shape) {
    IconShape.Circle -> CircleShape
    IconShape.Squircle -> Squircle
    IconShape.Rounded -> RoundedCornerShape(26)
    IconShape.Drop -> RoundedCornerShape(topStartPercent = 16, topEndPercent = 50, bottomEndPercent = 50, bottomStartPercent = 50)
}

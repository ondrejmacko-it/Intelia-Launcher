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
import androidx.compose.ui.graphics.drawscope.Stroke
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
    Box(if (!veil) modifier else modifier.drawBehind {
        val r = size.minDimension / 2f
        if (style == IconStyle.Tinted) {
            drawCircle(base.copy(alpha = .62f), r)
            drawCircle(tint.copy(alpha = .16f), r)
            drawCircle(tint.copy(alpha = .40f), r - .5.dp.toPx(), style = Stroke(1.dp.toPx()))
        } else {
            // §5.8 Veil style: the glyph on a faint disc (tint @ 9 %) with a 1 dp ring (tint @ 22 %).
            drawCircle(tint.copy(alpha = .09f), r)
            drawCircle(tint.copy(alpha = .22f), r - .5.dp.toPx(), style = Stroke(1.dp.toPx()))
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

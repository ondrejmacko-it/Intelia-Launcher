package dev.intelia.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.IconStyle
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
    val ink = LocalIconStyle.current == IconStyle.Ink
    val revision by icons.revision.collectAsStateWithLifecycle()
    val icon by produceState(icons.peek(app.key, ink), app.key, ink, revision) { value = icons.load(app, ink) }
    RenderedIconImage(icon, ink, tint, modifier)
}

@Composable
fun ShortcutIcon(shortcut: ShortcutEntry, modifier: Modifier = Modifier, tint: Color = LocalInk.current.ink) {
    val icons = LocalContext.current.intelia.icons
    val ink = LocalIconStyle.current == IconStyle.Ink
    val icon by produceState(icons.peek(shortcut.key, ink), shortcut.key, ink) { value = icons.loadShortcut(shortcut.key, ink) }
    RenderedIconImage(icon, ink, tint, modifier)
}

@Composable
private fun RenderedIconImage(icon: RenderedIcon?, ink: Boolean, tint: Color, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        icon?.let {
            val filter = when {
                it.glyph -> ColorFilter.tint(tint)
                ink -> grey
                else -> null
            }
            Image(it.bitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), colorFilter = filter)
        }
    }
}

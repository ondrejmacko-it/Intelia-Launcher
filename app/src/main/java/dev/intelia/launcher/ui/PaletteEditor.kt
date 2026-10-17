package dev.intelia.launcher.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.intelia.launcher.R
import dev.intelia.launcher.data.Accent
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.PaletteTarget

private val accentNames = mapOf(
    Accent.Frost to R.string.accent_frost, Accent.Silver to R.string.accent_silver, Accent.Mono to R.string.accent_mono,
    Accent.Slate to R.string.accent_slate, Accent.Sky to R.string.accent_sky, Accent.Teal to R.string.accent_teal,
    Accent.Mint to R.string.accent_mint, Accent.Sage to R.string.accent_sage, Accent.Lime to R.string.accent_lime,
    Accent.Amber to R.string.accent_amber, Accent.Terracotta to R.string.accent_terracotta, Accent.Coral to R.string.accent_coral,
    Accent.Rose to R.string.accent_rose, Accent.Orchid to R.string.accent_orchid, Accent.Violet to R.string.accent_violet,
    Accent.Indigo to R.string.accent_indigo, Accent.Dynamic to R.string.accent_dynamic,
)

private val placeNames = mapOf(
    PaletteTarget.Clock to R.string.place_clock, PaletteTarget.Text to R.string.place_text, PaletteTarget.Icons to R.string.place_icons,
    PaletteTarget.Lens to R.string.place_lens, PaletteTarget.Search to R.string.place_search, PaletteTarget.Studio to R.string.place_studio,
)

/**
 * The palette (§5.12): the main accent, and with "one colour everywhere" off, a colour per place. Used by Studio
 * and the editor's Look panel; settings apply at once.
 */
@Composable
fun PaletteEditor(settings: LauncherSettings, set: SettingsEdit, modifier: Modifier = Modifier) {
    var place by remember { mutableStateOf<PaletteTarget?>(null) }
    val unified = settings.palette.unified
    Column(modifier) {
        Toggle(stringResource(R.string.palette_unified), unified) { v -> set { it.copy(palette = it.palette.copy(unified = v)) } }
        val at = place.takeUnless { unified }
        if (!unified) Chips(listOf<Pair<PaletteTarget?, String>>(null to stringResource(R.string.palette_main)) +
            PaletteTarget.entries.map { it to stringResource(placeNames.getValue(it)) }, at) { place = it }
        if (at == null) Swatches(settings.accent, main = false) { v -> if (v != null) set { it.copy(accent = v) } }
        else PlaceSwatches(settings, at, set)
    }
}

/** One place's colours, "main colour" first; choosing one turns the palette per place (inspectors of clock and LENS). */
@Composable
fun PlaceSwatches(settings: LauncherSettings, place: PaletteTarget, set: SettingsEdit) {
    val chosen = settings.palette.takeUnless { it.unified }?.get(place)
    Swatches(chosen, main = true) { v ->
        set { it.copy(palette = it.palette.copy(unified = false).with(place, v)) }
    }
}

/** A wrapping grid of accent discs; with [main], a ring in the main accent stands for "follow the main colour" (null). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Swatches(selected: Accent?, main: Boolean, onSelect: (Accent?) -> Unit) {
    val ink = LocalInk.current
    val options: List<Accent?> = buildList {
        if (main) add(null)
        accentNames.keys.forEach { if (it != Accent.Dynamic || Build.VERSION.SDK_INT >= 31) add(it) }
    }
    val mainColor = LocalPlaces.current.main
    val mainName = stringResource(R.string.palette_follow)
    FlowRow(Modifier.selectableGroup().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEach { accent ->
            val color = if (accent == null) mainColor else swatchColor(accent, ink.dark)
            val on = accent == selected
            val name = accent?.let { stringResource(accentNames.getValue(it)) } ?: mainName
            Box(Modifier.size(44.dp).clip(CircleShape).selectable(on, role = Role.RadioButton) { onSelect(accent) }
                .semantics { contentDescription = name }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(32.dp).border(if (on) 2.dp else 0.dp, if (on) color else Color.Transparent, CircleShape).padding(4.dp)
                    .clip(CircleShape).then(if (accent == null) Modifier.border(2.dp, color, CircleShape) else Modifier.background(color)))
            }
        }
    }
}

@Composable
private fun swatchColor(accent: Accent, dark: Boolean): Color =
    if (accent == Accent.Mono) LocalInk.current.ink else accentColor(accent, dark)

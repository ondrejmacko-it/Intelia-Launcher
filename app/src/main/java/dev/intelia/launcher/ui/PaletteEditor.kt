package dev.intelia.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardCapitalization
import dev.intelia.launcher.data.AccentColors
import dev.intelia.launcher.data.CustomColors
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
        if (at == null) {
            // A preset clears the custom colour (D25); the preset kept underneath it is never changed by the custom one.
            Swatches(settings.accent.takeIf { settings.custom.accent == null }, main = false) { v ->
                if (v != null) set { it.copy(accent = v, custom = it.custom.copy(accent = null)) }
            }
            CustomAccent(settings, set)
        } else PlaceSwatches(settings, at, set)
    }
}

/**
 * D25: the owner's own main colour. Closed it is one row (disc, name, hex); open it mixes hue, saturation and
 * lightness or takes a hex code, and shows what each theme will draw (lightness corrected for legible text).
 */
@Composable
private fun CustomAccent(settings: LauncherSettings, set: SettingsEdit) {
    val ink = LocalInk.current
    val custom = settings.custom.accent
    var open by rememberSaveable { mutableStateOf(false) }
    // Mixing state lives here (a round trip through ARGB would make the sliders jump); it starts again from the colour
    // shown whenever a custom colour is set or cleared.
    val fresh = custom == null
    val start = remember(fresh) { AccentColors.hsl(custom ?: ink.accent.toArgb()) }
    var h by rememberSaveable(fresh) { mutableFloatStateOf(start.first) }
    var sat by rememberSaveable(fresh) { mutableFloatStateOf(start.second) }
    var l by rememberSaveable(fresh) { mutableFloatStateOf(start.third) }
    var hex by rememberSaveable { mutableStateOf(custom?.let(CustomColors::hex).orEmpty()) }
    fun store(argb: Int) = set { it.copy(custom = it.custom.copy(accent = argb)) }
    fun mix(nh: Float = h, ns: Float = sat, nl: Float = l) {
        h = nh; sat = ns; l = nl
        val c = AccentColors.fromHsl(h, sat, l)
        hex = CustomColors.hex(c)
        store(c)
    }
    val name = stringResource(R.string.palette_custom)
    val state = custom?.let(CustomColors::hex) ?: stringResource(R.string.palette_custom_none)
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button) { open = !open }
        .semantics(mergeDescendants = true) { stateDescription = state }.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        val shown = custom?.let { Color(AccentColors.derive(it, ink.dark)) }
        Box(Modifier.size(32.dp).clip(CircleShape).then(
            if (shown != null) Modifier.background(shown).border(2.dp, ink.ink.copy(alpha = .6f), CircleShape)
            else Modifier.background(Brush.sweepGradient(HUES.map { Color(AccentColors.fromHsl(it, .7f, .6f)) }))))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(name, style = InkText.body)
            Text(state, style = InkText.small, color = ink.muted)
        }
        Text(if (open) "⌃" else "⌄", style = InkText.body, color = ink.muted)
    }
    if (!open) return
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        SliderRow(stringResource(R.string.palette_hue), h, 0f, 359f, 1f, "${h.toInt()}°") { mix(nh = it) }
        Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(8.dp).clip(CircleShape)
            .background(Brush.horizontalGradient(HUES.map { Color(AccentColors.fromHsl(it, sat, l.coerceIn(.25f, .75f))) })))
        SliderRow(stringResource(R.string.palette_saturation), sat * 100f, 0f, 100f, 1f, "${(sat * 100).toInt()} %") { mix(ns = it / 100f) }
        SliderRow(stringResource(R.string.palette_lightness), l * 100f, 0f, 100f, 1f, "${(l * 100).toInt()} %") { mix(nl = it / 100f) }
        val hexLabel = stringResource(R.string.palette_hex)
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(hexLabel, Modifier.padding(end = 12.dp), style = InkText.small, color = ink.muted)
            BasicTextField(hex, { t ->
                hex = t.take(7)
                CustomColors.parseHex(hex)?.let { c -> AccentColors.hsl(c).let { (a, b, d) -> h = a; sat = b; l = d }; store(c) }
            }, Modifier.weight(1f).clip(CircleShape).background(ink.ink.copy(alpha = .06f)).padding(horizontal = 14.dp, vertical = 12.dp)
                .semantics { contentDescription = hexLabel }, singleLine = true, cursorBrush = SolidColor(ink.accent),
                textStyle = InkText.body.copy(color = ink.ink, fontFamily = InkFonts.mono),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false))
        }
        custom?.let { c ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.palette_custom_preview), Modifier.weight(1f), style = InkText.small, color = ink.muted)
                listOf(true, false).forEach { dark ->
                    val p = inkPalette(dark, Color(AccentColors.derive(c, dark)))
                    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(p.paper).padding(6.dp)) {
                        Text("Aa", Modifier.clip(RoundedCornerShape(8.dp)).background(p.accent).padding(horizontal = 10.dp, vertical = 2.dp),
                            style = InkText.small, color = p.onAccent)
                    }
                }
            }
            GroupNote(stringResource(R.string.palette_custom_detail))
            Quiet(stringResource(R.string.palette_custom_clear), {
                set { it.copy(custom = it.custom.copy(accent = null)) }
                hex = ""
            }, Modifier.padding(horizontal = 4.dp))
        }
    }
}

/** Hue stops for the gradients of [CustomAccent]. */
private val HUES = (0..360 step 30).map { it.toFloat() }

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

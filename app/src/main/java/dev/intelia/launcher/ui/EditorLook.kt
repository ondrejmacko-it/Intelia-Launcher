package dev.intelia.launcher.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.intelia.launcher.data.WallpaperPreset
import dev.intelia.launcher.data.Wallpapers
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.intelia.launcher.R
import dev.intelia.launcher.data.Glass
import dev.intelia.launcher.data.IconStyle
import dev.intelia.launcher.data.IconShape
import dev.intelia.launcher.data.ItemKind
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.ThemeMode

/** Changes the stored settings at once (not part of the layout's undo history). */
typealias SettingsEdit = ((LauncherSettings) -> LauncherSettings) -> Unit

/** The icon style for all of Home, Search, drawer and ARC; in the Look panel, the icon inspector and Studio. */
@Composable
fun IconStyleChips(settings: LauncherSettings, set: SettingsEdit) {
    Chips(listOf(IconStyle.System to stringResource(R.string.icons_system), IconStyle.Ink to stringResource(R.string.icons_ink),
        IconStyle.Veil to stringResource(R.string.look_veil), IconStyle.Tinted to stringResource(R.string.icons_tinted)),
        settings.iconStyle) { v -> set { it.copy(iconStyle = v) } }
    if (settings.iconStyle == IconStyle.Veil || settings.iconStyle == IconStyle.Tinted) {
        Chips(listOf(IconShape.Circle to stringResource(R.string.shape_circle), IconShape.Squircle to stringResource(R.string.shape_squircle),
            IconShape.Rounded to stringResource(R.string.shape_rounded), IconShape.Drop to stringResource(R.string.shape_drop)),
            settings.iconShape) { v -> set { it.copy(iconShape = v) } }
    }
}

private fun glassName(g: Glass) = when (g) {
    Glass.Veil -> R.string.look_veil
    Glass.Crystal -> R.string.glass_crystal
    Glass.Frost -> R.string.glass_frost
    Glass.Liquid -> R.string.glass_liquid
}

private fun themeName(t: ThemeMode) = when (t) {
    ThemeMode.System -> R.string.theme_system
    ThemeMode.Light -> R.string.theme_light
    ThemeMode.Dark -> R.string.theme_dark
}

/**
 * The panel material (§5.2) for every surface in one choice: the four glasses, or matte. Matte ignores the glass,
 * so it is a fifth option rather than a switch beside them.
 */
@Composable
fun MaterialChips(settings: LauncherSettings, set: SettingsEdit) {
    Chips(listOf<Pair<Glass?, String>>(Glass.Veil to stringResource(R.string.look_veil), Glass.Crystal to stringResource(R.string.glass_crystal),
        Glass.Frost to stringResource(R.string.glass_frost), Glass.Liquid to stringResource(R.string.glass_liquid), null to stringResource(R.string.look_solid)),
        settings.glass.takeUnless { settings.veilSolid }) { v ->
        set { if (v == null) it.copy(veilSolid = true) else it.copy(glass = v, veilSolid = false) }
    }
}

/**
 * The editor's Look panel (Vzhled): everything that styles the whole Home and every panel, each part folded and
 * naming its current choice, one open at a time. Settings apply immediately; labels and icon sizes go through undo.
 */
@Composable
fun LookPanel(settings: LauncherSettings, state: HomeEditorState, set: SettingsEdit, onChange: () -> Unit,
    modifier: Modifier = Modifier) {
    val c = LocalCrystal.current
    // The body of the editor card (InspectorCard): it brings the glass, the header, the scroll and the handles.
    Column(modifier.fillMaxWidth()) {
        Fold(stringResource(R.string.icons), stringResource(iconStyleName(settings.iconStyle))) {
            IconStyleChips(settings, set)
            AllIconsSize(settings, state, set, onChange)
            val icons = state.layout.items.filter { it.kind == ItemKind.App || it.kind == ItemKind.Flow }
            Toggle(stringResource(R.string.look_labels), icons.isNotEmpty() && icons.all { it.label }) { on -> state.setLabels(on); onChange() }
        }
        Fold(stringResource(R.string.look_material), stringResource(if (settings.veilSolid) R.string.look_solid else glassName(settings.glass))) {
            MaterialChips(settings, set)
            if (!settings.veilSolid) {
                val shift = settings.veilShift
                SliderRow(stringResource(R.string.look_density), shift.toFloat(), -LauncherSettings.MAX_VEIL_SHIFT.toFloat(),
                    LauncherSettings.MAX_VEIL_SHIFT.toFloat(), 1f, (if (shift > 0) "+" else "") + "$shift %") { v ->
                    set { it.copy(veilShift = v.toInt()) }
                }
            }
            // The accent edge exists on Veil and on matte panels; the other glasses have no outline to light.
            if (settings.veilSolid || settings.glass == Glass.Veil) Toggle(stringResource(R.string.look_edge), settings.veilEdge) { v ->
                set { it.copy(veilEdge = v) }
            }
        }
        val wall = Wallpapers.byId(settings.wallpaper)
        Fold(stringResource(R.string.wallpaper), stringResource(wallName(wall?.id))) {
            WallpaperChoice(settings.wallpaper) { id ->
                set { it.copy(wallpaper = id, wallDim = it.wallDim.coerceAtMost(Wallpapers.maxDim(Wallpapers.byId(id)))) }
            }
            SliderRow(stringResource(R.string.wallpaper_dim), settings.wallDim.toFloat(), 0f, Wallpapers.maxDim(wall).toFloat(), 5f,
                "${settings.wallDim} %") { v -> set { it.copy(wallDim = v.toInt()) } }
        }
        Fold(stringResource(R.string.theme), stringResource(themeName(settings.theme))) {
            Chips(listOf(ThemeMode.System to stringResource(R.string.theme_system), ThemeMode.Light to stringResource(R.string.theme_light),
                ThemeMode.Dark to stringResource(R.string.theme_dark)), settings.theme) { v -> set { it.copy(theme = v) } }
        }
        Fold(stringResource(R.string.palette_section), stringResource(if (settings.palette.unified) R.string.palette_one else R.string.palette_places)) {
            PaletteEditor(settings, set)
        }
        Text(stringResource(R.string.look_hint), Modifier.padding(vertical = 8.dp), style = CrystalType.body.copy(fontSize = 12.sp), color = c.fg2)
    }
}

/** The system wallpaper, then Intelia's own, as small swatches in one scrolling row; the chosen one is ringed. */
@Composable
private fun WallpaperChoice(chosen: String, onPick: (String) -> Unit) {
    val c = LocalCrystal.current
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (listOf<WallpaperPreset?>(null) + Wallpapers.all).forEach { p ->
            val id = p?.id.orEmpty()
            val on = id == chosen
            val name = stringResource(wallName(p?.id))
            Box(Modifier.size(44.dp, 72.dp).clip(RoundedCornerShape(12.dp))
                .border(if (on) 2.dp else 1.dp, if (on) c.acc else c.fg.copy(alpha = .18f), RoundedCornerShape(12.dp))
                .selectable(on, role = Role.RadioButton) { onPick(id) }.semantics { contentDescription = name },
                contentAlignment = Alignment.Center) {
                if (p != null) PresetWallpaper(p, Modifier.fillMaxSize())
                else Text(stringResource(R.string.theme_system), style = CrystalType.body.copy(fontSize = 10.sp), color = c.fg2, maxLines = 1)
            }
        }
    }
}

private fun wallName(id: String?) = when (id) {
    "night" -> R.string.wall_night
    "ocean" -> R.string.wall_ocean
    "forest" -> R.string.wall_forest
    "aurora" -> R.string.wall_aurora
    "dusk" -> R.string.wall_dusk
    "ember" -> R.string.wall_ember
    "graphite" -> R.string.wall_graphite
    "mist" -> R.string.wall_mist
    "paper" -> R.string.wall_paper
    "sand" -> R.string.wall_sand
    "dawn" -> R.string.wall_dawn
    "ice" -> R.string.wall_ice
    "sage" -> R.string.wall_sage
    else -> R.string.wallpaper_system
}

private fun iconStyleName(s: IconStyle) = when (s) {
    IconStyle.System -> R.string.icons_system
    IconStyle.Ink -> R.string.icons_ink
    IconStyle.Veil -> R.string.look_veil
    IconStyle.Tinted -> R.string.icons_tinted
}

/**
 * One size for every app and FLOW icon on Home: moving it drops each icon's own size (one undo step), so all of them
 * really follow it; in the Look panel and in every icon's inspector.
 */
@Composable
fun AllIconsSize(settings: LauncherSettings, state: HomeEditorState, set: SettingsEdit, onChange: () -> Unit) {
    SliderRow(stringResource(R.string.icons_all_size), settings.iconSize.toFloat(), LauncherSettings.MIN_ICON.toFloat(),
        LauncherSettings.MAX_ICON.toFloat(), 2f, "${settings.iconSize} dp") { v ->
        set { it.copy(iconSize = v.toInt()) }
        state.resetIconSizes(); onChange()
    }
}

@Composable
internal fun Kick(text: String) {
    Text(text.uppercase(), Modifier.padding(top = 10.dp, bottom = 2.dp), style = CrystalType.label, color = LocalCrystal.current.fg2)
}

/** PULSE (tap the camera ring in the editor, §5.5): the capsule's look and events; calibration lives in Studio. */
@Composable
fun PulseInspector(settings: LauncherSettings, set: SettingsEdit, onLens: () -> Unit) {
    val c = LocalCrystal.current
    Toggle(stringResource(R.string.pulse_toggle), settings.pulse) { v -> set { it.copy(pulse = v) } }
    if (!settings.pulse) return
    SliderRow(stringResource(R.string.pulse_opacity), settings.pulseOpacity.toFloat(), LauncherSettings.MIN_PULSE_OPACITY.toFloat(), 100f, 5f,
        "${settings.pulseOpacity} %") { v -> set { it.copy(pulseOpacity = v.toInt()) } }
    val events = listOf(settings.pulseEcho, settings.pulseCharging, settings.pulseAlarm).count { it }
    Fold(stringResource(R.string.pulse_events), "$events / 3") {
        Toggle(stringResource(R.string.pulse_echo), settings.pulseEcho) { v -> set { it.copy(pulseEcho = v) } }
        Toggle(stringResource(R.string.pulse_charging), settings.pulseCharging) { v -> set { it.copy(pulseCharging = v) } }
        Toggle(stringResource(R.string.pulse_alarm), settings.pulseAlarm) { v -> set { it.copy(pulseAlarm = v) } }
        if (settings.pulseAlarm) Chips(listOf(3 to "3 h", 6 to "6 h", 12 to "12 h", 24 to "24 h"), settings.pulseAlarmHours) { v ->
            set { it.copy(pulseAlarmHours = v) }
        }
    }
    Action(stringResource(R.string.pulse_lens), c.acc, onLens)
}

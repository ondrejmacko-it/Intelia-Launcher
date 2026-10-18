package dev.intelia.launcher.ui

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import dev.intelia.launcher.pulse.MediaWatch
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import dev.intelia.launcher.pulse.PulseMode
import dev.intelia.launcher.data.pulseMode
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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import dev.intelia.launcher.data.WallpaperPreset
import dev.intelia.launcher.data.Wallpapers
import androidx.compose.foundation.gestures.detectTapGestures
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

/** The icon style for all of Home, Search, drawer and ARC, and the disc shape where the style has a disc. */
@Composable
fun IconStyleChips(settings: LauncherSettings, set: SettingsEdit) {
    ChoiceSetting(stringResource(R.string.icons_style), listOf(IconStyle.System to stringResource(R.string.icons_system),
        IconStyle.Ink to stringResource(R.string.icons_ink), IconStyle.Veil to stringResource(R.string.look_veil),
        IconStyle.Tinted to stringResource(R.string.icons_tinted)), settings.iconStyle, { v -> set { it.copy(iconStyle = v) } },
        anchor = StudioTarget.Icons.anchor)
    if (settings.iconStyle == IconStyle.Veil || settings.iconStyle == IconStyle.Tinted) {
        ChoiceSetting(stringResource(R.string.icons_shape), listOf(IconShape.Circle to stringResource(R.string.shape_circle),
            IconShape.Squircle to stringResource(R.string.shape_squircle), IconShape.Rounded to stringResource(R.string.shape_rounded),
            IconShape.Drop to stringResource(R.string.shape_drop)), settings.iconShape, { v -> set { it.copy(iconShape = v) } })
    }
}

internal fun glassName(g: Glass) = when (g) {
    Glass.Veil -> R.string.look_veil
    Glass.Crystal -> R.string.glass_crystal
    Glass.Frost -> R.string.glass_frost
    Glass.Liquid -> R.string.glass_liquid
}

internal fun themeName(t: ThemeMode) = when (t) {
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
    ChoiceSetting(stringResource(R.string.look_material), listOf<Pair<Glass?, String>>(Glass.Veil to stringResource(R.string.look_veil),
        Glass.Crystal to stringResource(R.string.glass_crystal), Glass.Frost to stringResource(R.string.glass_frost),
        Glass.Liquid to stringResource(R.string.glass_liquid), null to stringResource(R.string.look_solid)),
        settings.glass.takeUnless { settings.veilSolid }, { v ->
            set { if (v == null) it.copy(veilSolid = true) else it.copy(glass = v, veilSolid = false) }
        }, anchor = StudioTarget.Material.anchor)
}

/** The fold title each Look part is keyed by; Studio's search opens a part by it. */
@Composable
fun lookFoldTitle(target: StudioTarget): String? = when (target) {
    StudioTarget.Icons -> stringResource(R.string.icons)
    StudioTarget.Material -> stringResource(R.string.look_material)
    StudioTarget.Wallpaper -> stringResource(R.string.wallpaper)
    StudioTarget.Theme -> stringResource(R.string.theme)
    StudioTarget.Colors -> stringResource(R.string.palette_section)
    else -> null
}

/**
 * Every app and FLOW icon at once (Settings › Vzhled › Ikony): style and disc shape, one size for all of Home (up to
 * [LauncherSettings.MAX_ICON]) and their names, applied to the active layout at once.
 */
@Composable
fun IconSettings(settings: LauncherSettings, set: SettingsEdit) {
    IconStyleChips(settings, set)
    SettingBlock { AllIconsSize(settings, set) }
    val icons = settings.home.activeLayout().items.filter { it.kind == ItemKind.App || it.kind == ItemKind.Flow }
    SwitchRow(stringResource(R.string.look_labels), icons.isNotEmpty() && icons.all { it.label }, { on ->
        set { it.copy(home = it.home.withIcons { i -> i.copy(label = on) }) }
    })
}

/** The look of the whole launcher in one place, Settings › Vzhled: theme and colours, material, wallpaper, text, icons. */
@Composable
fun LookSettings(settings: LauncherSettings, set: SettingsEdit) {
    VeilGroup(stringResource(R.string.look_group_theme)) {
        ChoiceSetting(stringResource(R.string.theme), listOf(ThemeMode.System to stringResource(R.string.theme_system),
            ThemeMode.Light to stringResource(R.string.theme_light), ThemeMode.Dark to stringResource(R.string.theme_dark)), settings.theme,
            { v -> set { it.copy(theme = v) } }, anchor = StudioTarget.Theme.anchor)
        SettingBlock(StudioTarget.Colors.anchor) {
            Text(stringResource(R.string.palette_section), Modifier.padding(start = 4.dp, top = 4.dp), style = InkText.body.copy(fontSize = 16.sp))
            PaletteEditor(settings, set)
        }
    }
    VeilGroup(stringResource(R.string.look_material)) {
        MaterialChips(settings, set)
        if (!settings.veilSolid) SettingBlock {
            val shift = settings.veilShift
            SliderRow(stringResource(R.string.look_density), shift.toFloat(), -LauncherSettings.MAX_VEIL_SHIFT.toFloat(),
                LauncherSettings.MAX_VEIL_SHIFT.toFloat(), 1f, (if (shift > 0) "+" else "") + "$shift %") { v ->
                set { it.copy(veilShift = v.toInt()) }
            }
        }
        // The accent edge exists on Veil and on matte panels; the other glasses have no outline to light.
        if (settings.veilSolid || settings.glass == Glass.Veil) SwitchRow(stringResource(R.string.look_edge), settings.veilEdge, { v ->
            set { it.copy(veilEdge = v) }
        })
    }
    val wall = Wallpapers.byId(settings.wallpaper)
    VeilGroup(stringResource(R.string.wallpaper)) {
        SettingBlock(StudioTarget.Wallpaper.anchor) {
            WallpaperChoice(settings.wallpaper) { id ->
                set { it.copy(wallpaper = id, wallDim = it.wallDim.coerceAtMost(Wallpapers.maxDim(Wallpapers.byId(id)))) }
            }
            SliderRow(stringResource(R.string.wallpaper_dim), settings.wallDim.toFloat(), 0f, Wallpapers.maxDim(wall).toFloat(), 5f,
                "${settings.wallDim} %") { v -> set { it.copy(wallDim = v.toInt()) } }
            if (wall != null) WallpaperExportRow(wall, settings.wallDim)
        }
    }
    VeilGroup(stringResource(R.string.look_group_text)) {
        ChoiceSetting(stringResource(R.string.text_size), LauncherSettings.TEXT_SCALES.map { it to "${(it * 100).toInt()} %" }, settings.textScale,
            { v -> set { it.copy(textScale = v) } }, anchor = StudioTarget.TextSize.anchor)
    }
    VeilGroup(stringResource(R.string.icons)) { IconSettings(settings, set) }
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

private enum class ExportStep { Idle, Ask, Saving, Done, Failed }

/**
 * D13 ④: save the chosen own wallpaper (with its dim) as the system one. Nothing happens before the owner picks
 * Plocha or Plocha i zámek; the answer stays until the wallpaper or the dim changes.
 */
@Composable
private fun WallpaperExportRow(preset: WallpaperPreset, dim: Int) {
    val c = LocalCrystal.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var step by androidx.compose.runtime.remember(preset.id, dim) { androidx.compose.runtime.mutableStateOf(ExportStep.Idle) }
    fun save(target: WallpaperExport.Target) {
        step = ExportStep.Saving
        scope.launch { step = if (WallpaperExport.save(context.applicationContext, preset, dim, target)) ExportStep.Done else ExportStep.Failed }
    }
    val small = CrystalType.body.copy(fontSize = 12.sp)
    when (step) {
        ExportStep.Ask -> {
            Text(stringResource(R.string.wall_export_hint), Modifier.padding(top = 4.dp), style = small, color = c.fg2)
            Action(stringResource(R.string.wall_export_home), c.acc) { save(WallpaperExport.Target.Home) }
            Action(stringResource(R.string.wall_export_both), c.acc) { save(WallpaperExport.Target.HomeAndLock) }
            Action(stringResource(R.string.preset_cancel), c.fg2) { step = ExportStep.Idle }
        }
        ExportStep.Saving -> Text(stringResource(R.string.wall_export_saving), Modifier.heightIn(min = 44.dp).padding(vertical = 12.dp),
            style = CrystalType.body.copy(fontSize = 14.sp), color = c.fg2)
        else -> {
            Action(stringResource(R.string.wall_export), c.acc) { step = ExportStep.Ask }
            if (step != ExportStep.Idle) Text(stringResource(if (step == ExportStep.Done) R.string.wall_export_done else R.string.wall_export_failed),
                Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }, style = small, color = c.fg2)
        }
    }
}

internal fun wallName(id: String?) = when (id) {
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

internal fun iconStyleName(s: IconStyle) = when (s) {
    IconStyle.System -> R.string.icons_system
    IconStyle.Ink -> R.string.icons_ink
    IconStyle.Veil -> R.string.look_veil
    IconStyle.Tinted -> R.string.icons_tinted
}

/** One size for every app and FLOW icon on Home: moving it drops each icon's own size, so all of them really follow it. */
@Composable
private fun AllIconsSize(settings: LauncherSettings, set: SettingsEdit) {
    SliderRow(stringResource(R.string.icons_all_size), settings.iconSize.toFloat(), LauncherSettings.MIN_ICON.toFloat(),
        LauncherSettings.MAX_ICON.toFloat(), 2f, "${settings.iconSize} dp") { v ->
        set { it.copy(iconSize = v.toInt(), home = it.home.withIcons { i -> i.copy(sizeDp = 0) }) }
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
    // M3.1: one choice for where PULSE lives; Everywhere still needs the service the owner switches on in Android.
    val mode = settings.pulseMode()
    val service by PulseAccessibilityService.running.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Kick(stringResource(R.string.pulse_mode))
    Chips(listOf(PulseMode.Off to stringResource(R.string.off), PulseMode.Home to stringResource(R.string.pulse_home_only),
        PulseMode.Everywhere to stringResource(R.string.pulse_everywhere_short)), mode) { m ->
        set { it.copy(pulse = m != PulseMode.Off, pulseEverywhere = if (m == PulseMode.Off) it.pulseEverywhere else m == PulseMode.Everywhere) }
        if (m == PulseMode.Everywhere && !service) PulseAccessibilityService.openSettings(context)
    }
    if (mode == PulseMode.Everywhere && !service) {
        Text(stringResource(R.string.pulse_mode_needs_service), Modifier.padding(vertical = 6.dp), style = CrystalType.label, color = c.fg2)
        Action(stringResource(R.string.pulse_mode_open_settings), c.acc) { PulseAccessibilityService.openSettings(context) }
    }
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
    // M3.2 (D21): music needs Android's notification access; asked only when the owner switches it on.
    var access by remember { mutableStateOf(MediaWatch.allowed(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { access = MediaWatch.allowed(context) }
    Toggle(stringResource(R.string.pulse_music), settings.pulseMusic) { v ->
        set { it.copy(pulseMusic = v) }
        if (v && !access) MediaWatch.openSettings(context)
    }
    if (settings.pulseMusic && !access) {
        Text(stringResource(R.string.pulse_music_needs_access), Modifier.padding(vertical = 6.dp), style = CrystalType.label, color = c.fg2)
        Action(stringResource(R.string.pulse_music_open_settings), c.acc) { MediaWatch.openSettings(context) }
    }
    Action(stringResource(R.string.pulse_lens), c.acc, onLens)
}

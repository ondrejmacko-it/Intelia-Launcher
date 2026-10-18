package dev.intelia.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlin.math.roundToInt
import dev.intelia.launcher.data.iconLook
import dev.intelia.launcher.data.IconOverride
import dev.intelia.launcher.data.HomeItem
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
    SettingBlock {
        AllIconsSize(settings, set)
        // S2: colour and opacity of every Home icon (style and shape are chosen above, for the whole launcher).
        val global = HomeItem("", ItemKind.App).iconLook(settings)
        if (settings.iconStyle != IconStyle.System) {
            Kick(stringResource(R.string.inspect_color))
            IconTintChips(global.tint) { v -> set { it.copy(iconTint = v) } }
        }
        val pct = (settings.iconAlpha * 100).roundToInt()
        SliderRow(stringResource(R.string.inspect_opacity), pct.toFloat(), IconOverride.MIN_ALPHA * 100, 100f, 5f, "$pct %") { v ->
            set { it.copy(iconAlpha = v / 100f) }
        }
    }
    // The switch sets the default and drops each icon's own choice, so all of them really follow it, as the size does.
    SwitchRow(stringResource(R.string.look_labels), settings.labels.show, { on ->
        set { it.copy(labels = it.labels.copy(show = on), home = it.home.withIcons { i -> i.copy(label = null) }) }
    }, detail = stringResource(R.string.labels_note))
    if (settings.labels.show) SettingBlock { LabelStyleControls(settings.labels) { v -> set { it.copy(labels = v.sanitized()) } } }
}

/** The look of the whole launcher in one place, Settings › Vzhled: theme and colours, material, wallpaper, text, icons. */
@Composable
fun LookSettings(settings: LauncherSettings, set: SettingsEdit) {
    VeilGroup(stringResource(R.string.look_group_theme)) {
        SettingBlock(StudioTarget.Theme.anchor) {
            Text(stringResource(R.string.theme), Modifier.padding(start = 4.dp, top = 4.dp), style = InkText.body.copy(fontSize = 16.sp))
            ThemeTiles(settings.theme, settings) { v -> set { it.copy(theme = v) } }
        }
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

/**
 * Motiv as preview tiles (M2): each tile is a tiny Studio sheet drawn with that theme's own ink and the owner's accent,
 * Systém split light / dark. Radio group; the chosen tile is ringed in the accent.
 */
@Composable
private fun ThemeTiles(chosen: ThemeMode, settings: LauncherSettings, onPick: (ThemeMode) -> Unit) {
    val ink = LocalInk.current
    val light = inkPalette(false, accentColor(settings.accent, false))
    val dark = inkPalette(true, accentColor(settings.accent, true))
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(ThemeMode.System to R.string.theme_system, ThemeMode.Light to R.string.theme_light, ThemeMode.Dark to R.string.theme_dark)
            .forEach { (mode, name) ->
                val on = mode == chosen
                val shape = RoundedCornerShape(14.dp)
                Column(Modifier.weight(1f).clip(shape).selectable(on, role = Role.RadioButton) { onPick(mode) }.padding(bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.fillMaxWidth().height(84.dp).clip(shape)
                        .border(if (on) 2.dp else 1.dp, if (on) ink.accent else ink.line, shape)) {
                        when (mode) {
                            ThemeMode.Light -> miniSheet(light)
                            ThemeMode.Dark -> miniSheet(dark)
                            ThemeMode.System -> {
                                clipRect(right = size.width / 2) { miniSheet(light) }
                                clipRect(left = size.width / 2) { miniSheet(dark) }
                            }
                        }
                    }
                    Text(stringResource(name), Modifier.padding(top = 6.dp), style = InkText.small, color = if (on) ink.ink else ink.muted,
                        maxLines = 1)
                }
            }
    }
}

/** A Studio sheet in miniature: paper, a title line, two rows and an accent switch. */
private fun DrawScope.miniSheet(p: InkPalette) {
    val u = size.width / 10f
    drawRect(p.paper)
    drawRoundRect(p.ink.copy(alpha = .75f), Offset(u, u * 1.2f), Size(u * 4.5f, u * .7f), CornerRadius(u * .35f))
    repeat(2) { i ->
        val y = u * (3f + i * 2.4f)
        drawRoundRect(p.line, Offset(u * .6f, y - u * .5f), Size(u * 8.8f, u * 2f), CornerRadius(u * .6f))
        drawRoundRect(p.muted, Offset(u * 1.2f, y + u * .2f), Size(u * 4f, u * .5f), CornerRadius(u * .25f))
        drawRoundRect(if (i == 0) p.accent else p.faint, Offset(u * 6.6f, y), Size(u * 2.2f, u), CornerRadius(u * .5f))
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

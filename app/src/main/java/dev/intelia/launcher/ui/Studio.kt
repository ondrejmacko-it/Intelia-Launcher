package dev.intelia.launcher.ui

import dev.intelia.launcher.data.pulseMode
import dev.intelia.launcher.pulse.MediaWatch
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import dev.intelia.launcher.pulse.PulseMode
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.ColumnScope
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.intelia.launcher.R
import dev.intelia.launcher.action.Action
import dev.intelia.launcher.action.ArcLook
import dev.intelia.launcher.action.GlobalArcLook
import dev.intelia.launcher.action.Controls
import dev.intelia.launcher.action.Gesture
import dev.intelia.launcher.action.Trigger
import dev.intelia.launcher.data.PaletteTarget
import dev.intelia.launcher.data.AnchorType
import dev.intelia.launcher.data.HomeItem
import dev.intelia.launcher.data.HomeSketch
import dev.intelia.launcher.data.LayoutSpec
import dev.intelia.launcher.data.sketch
import dev.intelia.launcher.data.ItemKind
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.layout
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.ClockStyle
import dev.intelia.launcher.data.Haptics
import dev.intelia.launcher.data.HomeLayout
import dev.intelia.launcher.data.HomeLayouts
import dev.intelia.launcher.data.IconStyle
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.Motion
import dev.intelia.launcher.data.ThemeMode
import dev.intelia.launcher.data.Placement
import java.time.LocalDateTime
import kotlin.math.roundToInt
import java.time.LocalDate
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import dev.intelia.launcher.data.BackupRead
import dev.intelia.launcher.data.BackupError
import dev.intelia.launcher.data.BackupCodec
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult

/** The main page, the five areas, the pages inside them and three tools; [depth] drives the slide. */
private enum class StudioPane(val depth: Int) {
    Main(0), Look(1), Home(1), Controls(1), Actions(1), System(1),
    Favorites(2), Pulse(2), Arc(2), Gestures(2), Motion(2), Backup(2),
    Hidden(3), Lens(3), Keys(3),
}

private fun StudioSection.pane() = StudioPane.valueOf(name)

/** Where Settings opens: its main page from Home, the same page from the editor's tray, PULSE calibration or PULSE (editor). */
enum class StudioStart { Main, Editor, Lens, Pulse }

/**
 * Settings (Studio, Veil 3.0): a search over every row and five areas — Vzhled, Plocha, Ovládání, Akce a FLOW,
 * Systém a data — each sliding in. A search result opens its page on the row and lights it. Every setting lives in
 * exactly one place; changes apply immediately and persist through SettingsStore.
 */
@Composable
fun Studio(model: HomeViewModel, settings: LauncherSettings, onBack: () -> Unit, onFlows: () -> Unit, onEditHome: () -> Unit,
    start: StudioStart = StudioStart.Main) {
    val root = when (start) {
        StudioStart.Lens -> StudioPane.Lens
        StudioStart.Pulse -> StudioPane.Pulse
        else -> StudioPane.Main
    }
    /** Where Back goes: the pages you came through, saved by name (S1). Empty = leave Studio. */
    var saved by rememberSaveable(start) { mutableStateOf(root.name) }
    val stack = StudioStack.restore(saved) { n -> StudioPane.entries.any { it.name == n } }.map(StudioPane::valueOf).ifEmpty { listOf(root) }
    val pane = stack.last()
    /** The sheet's resting height, kept across pages so a deeper page opens as the last one was left. */
    var sheet by rememberSaveable { mutableStateOf(SheetStop.Balanced) }
    var forward by remember { mutableStateOf(true) }
    /** The row a search result asked for; its page scrolls to it and lights it once. */
    val focus = remember { SettingFocus(null) }
    /** Settings before a restore from a backup file (D17); offered back until Studio closes, never stored. */
    var beforeRestore by remember { mutableStateOf<LauncherSettings?>(null) }
    // The page's scroll survives a deeper page too, so Back lands on the same row (Veil 3.0 motion).
    val panes = rememberSaveableStateHolder()
    val ms = motionMs(VeilMotion.PAGE_MS)
    /** Opened from the editor (its tray or the camera), leaving Settings goes back to the editor. */
    val leave = if (start == StudioStart.Editor || start == StudioStart.Pulse) onEditHome else onBack
    fun show(next: List<StudioPane>) {
        // Pages that leave the stack forget their scroll; the one returned to keeps it.
        stack.filter { it !in next }.forEach { panes.removeState(it.name) }
        forward = next.size > stack.size
        saved = StudioStack.save(next.map { it.name })
    }
    fun up() {
        val next = StudioStack.pop(stack)
        if (next.isEmpty()) leave() else show(next)
    }
    fun go(to: StudioPane) {
        if (to !in stack) panes.removeState(to.name)
        show(StudioStack.push(stack, to))
    }
    fun open(target: StudioTarget) {
        if (target == StudioTarget.EditHome) { onEditHome(); return }
        focus.key = target.anchor
        go(target.section.pane())
    }
    BackHandler(enabled = stack.size > 1 || start != StudioStart.Main) { up() }
    val sheetState = StudioSheetState(sheet, { sheet = it }, leave)
    val set: SettingsEdit = { model.updateSettings(it) }
    CompositionLocalProvider(LocalSettingFocus provides focus, LocalStudioSheet provides sheetState) {
    AnimatedContent(pane, transitionSpec = {
        VeilMotion.page(if (forward) 1 else -1, ms)
    }, label = "studio-pane") { shown -> panes.SaveableStateProvider(shown.name) { when (shown) {
        StudioPane.Main -> StudioMain(model, settings, leave, onEditHome, ::open) { go(it.pane()) }
        StudioPane.Look -> StudioFrame(settings, model, stringResource(R.string.look), ::up, preview = true) {
            LookSettings(settings, set)
            ResetDefaults(stringResource(R.string.reset_look_what)) { set { it.withLookDefaults() } }
        }
        StudioPane.Home -> StudioFrame(settings, model, stringResource(R.string.settings_group_home), ::up, preview = true) {
            HomePage(settings, set, onEditHome) { go(StudioPane.Favorites) }
        }
        StudioPane.Controls -> StudioFrame(settings, model, stringResource(R.string.settings_group_controls), ::up) {
            ControlsArea(settings) { go(it) }
        }
        StudioPane.Actions -> StudioFrame(settings, model, stringResource(R.string.studio_area_actions), ::up) { FlowPage(settings, onFlows) }
        StudioPane.System -> StudioFrame(settings, model, stringResource(R.string.studio_area_system), ::up) {
            SystemPage(model, onHidden = { go(StudioPane.Hidden) }, onBackup = { go(StudioPane.Backup) })
        }
        StudioPane.Favorites -> StudioFrame(settings, model, stringResource(R.string.favorites_title), ::up, preview = true) {
            FavoritesPage(model, settings)
        }
        StudioPane.Arc -> StudioFrame(settings, model, stringResource(R.string.arc_section), ::up) {
            ArcPage(model, settings)
        }
        StudioPane.Pulse -> StudioFrame(settings, model, stringResource(R.string.pulse), ::up) {
            PulsePage(model, settings, set, onLens = { go(StudioPane.Lens) }, onControls = { go(StudioPane.Gestures) })
        }
        StudioPane.Gestures -> StudioFrame(settings, model, stringResource(R.string.gestures_title), ::up) {
            ControlsPage(model, settings, onArc = { go(StudioPane.Arc) }, onKeys = { go(StudioPane.Keys) })
        }
        StudioPane.Keys -> StudioFrame(settings, model, stringResource(R.string.keys_title), ::up) {
            KeysPage(onSeen = { code -> model.editControls { it.withKey(code) } })
        }
        StudioPane.Motion -> StudioFrame(settings, model, stringResource(R.string.motion_section), ::up) { MotionPage(settings, set) }
        StudioPane.Backup -> StudioFrame(settings, model, stringResource(R.string.backup_title), ::up) {
            BackupPart(model, beforeRestore) { beforeRestore = it }
        }
        StudioPane.Hidden -> HiddenApps(model, onBack = ::up)
        StudioPane.Lens -> LensCalibration(settings.pulseLens, onSave = { lens ->
            model.updateSettings { it.copy(pulseLens = lens) }
            up()
        }, onBack = ::up)
    } } }
    }
}

/** Vzhled back to a new install's look: theme, accent and palette, material, wallpaper, text size and icons. */
private fun LauncherSettings.withLookDefaults(): LauncherSettings {
    val d = LauncherSettings.fresh()
    return copy(theme = d.theme, accent = d.accent, palette = d.palette, glass = d.glass, veilSolid = d.veilSolid, veilShift = d.veilShift,
        veilEdge = d.veilEdge, wallpaper = d.wallpaper, wallDim = d.wallDim, textScale = d.textScale, iconStyle = d.iconStyle,
        iconShape = d.iconShape, iconSize = d.iconSize)
}

/**
 * One Settings page: the header (gear and title) on the wallpaper and the Veil Focus sheet below it (S1). Where a
 * change shows on Home, the live Home miniature sits above the sheet and the sheet rests at the shared stop
 * (Preview / Balanced / Settings); elsewhere the sheet takes the whole room.
 */
@Composable
private fun StudioFrame(settings: LauncherSettings, model: HomeViewModel, title: String, onBack: () -> Unit, preview: Boolean = false,
    content: @Composable ColumnScope.() -> Unit) {
    val homeInk = LocalHomeInk.current
    val favorites by model.favoriteApps.collectAsStateWithLifecycle()
    val now by rememberNow()
    val sheet = LocalStudioSheet.current
    SystemBarIcons(darkIcons = homeInk.luminance() < .5f)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenW = maxWidth
        val screenH = maxHeight
        val density = androidx.compose.ui.platform.LocalDensity.current
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconAction(Glyph.Back, stringResource(R.string.back), onBack, tint = homeInk)
                HomeGlyph(Glyph.Settings, null, Modifier.padding(end = 10.dp), tint = homeInk, size = 18.dp)
                Text(title.uppercase(), style = InkText.label.copy(shadow = homeShadow(homeInk)), color = homeInk, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            val previewLabel = stringResource(R.string.home_preview)
            StudioSheet(sheet.stop, sheet.onStop, sheet.onClose, Modifier.weight(1f), preview = if (!preview) null else { full ->
                // Home as a true miniature of the screen: every size and distance at one scale, fitted to the Preview stop.
                val roomH = with(density) { full.toDp() } - 12.dp
                val scale = minOf(roomH / screenH, (screenW - 32.dp) / screenW).coerceAtLeast(.05f)
                val shape = RoundedCornerShape(16.dp)
                // TalkBack hears one "Home preview", not the live time and date as if they were content.
                Box(Modifier.padding(top = 4.dp).size(screenW * scale, screenH * scale).clip(shape).border(1.dp, homeInk.copy(alpha = .35f), shape)
                    .clearAndSetSemantics { contentDescription = previewLabel }) {
                    // The clock and date as Home draws them (font, size, colour, plate), not the old global clock style.
                    HomePreview(settings, favorites, now, homeInk, LocalWallPlaces.current[PaletteTarget.Clock], Modifier.fillMaxSize(),
                        scale = scale, onWallpaper = true, home = settings.home.activeLayout(), entry = model::entry)
                }
            }, content = content)
        }
        // Live capsule at the real camera while PULSE is on: width and opacity change as you drag.
        if (preview && settings.pulse) Pulse(settings, preview = true)
    }
}

/** Every searchable row, named in the owner's language, with its full path ("Ovládání › PULSE › Hudba"). */
@Composable
private fun studioEntries(): List<StudioEntry> {
    val areas = mapOf(StudioSection.Look to stringResource(R.string.look), StudioSection.Home to stringResource(R.string.settings_group_home),
        StudioSection.Controls to stringResource(R.string.settings_group_controls), StudioSection.Actions to stringResource(R.string.studio_area_actions),
        StudioSection.System to stringResource(R.string.studio_area_system), StudioSection.Favorites to stringResource(R.string.favorites_title),
        StudioSection.Pulse to stringResource(R.string.pulse), StudioSection.Arc to stringResource(R.string.arc_section),
        StudioSection.Gestures to stringResource(R.string.gestures_title), StudioSection.Motion to stringResource(R.string.motion_section),
        StudioSection.Backup to stringResource(R.string.backup_title))
    return StudioTarget.entries.map { t ->
        val (label, kw) = when (t) {
            StudioTarget.Theme -> R.string.theme to R.string.studio_kw_theme
            StudioTarget.Colors -> R.string.palette_section to R.string.studio_kw_colors
            StudioTarget.Material -> R.string.look_material to R.string.studio_kw_material
            StudioTarget.Wallpaper -> R.string.wallpaper to R.string.studio_kw_wallpaper
            StudioTarget.TextSize -> R.string.text_size to R.string.studio_kw_text
            StudioTarget.Icons -> R.string.icons to R.string.studio_kw_icons
            StudioTarget.EditHome -> R.string.edit_home to R.string.studio_kw_edit_home
            StudioTarget.Grid -> R.string.home_grid to R.string.studio_kw_grid
            StudioTarget.Placement -> R.string.home_placement to R.string.studio_kw_placement
            StudioTarget.Favorites -> R.string.favorites_title to R.string.studio_kw_favorites
            StudioTarget.Pulse -> R.string.pulse_mode to R.string.studio_kw_pulse
            StudioTarget.PulseLook -> R.string.pulse_opacity to R.string.studio_kw_pulse_look
            StudioTarget.PulseEvents -> R.string.pulse_events to R.string.studio_kw_pulse_events
            StudioTarget.PulseMusic -> R.string.pulse_music to R.string.studio_kw_pulse_music
            StudioTarget.PulseLens -> R.string.pulse_lens to R.string.studio_kw_pulse_lens
            StudioTarget.Arc -> R.string.arc_tab_lens to R.string.studio_kw_arc
            StudioTarget.GlobalArc -> R.string.arc_tab_global to R.string.studio_kw_global_arc
            StudioTarget.Shortcuts -> R.string.arc_ring_shortcuts to R.string.studio_kw_shortcuts
            StudioTarget.Gestures -> R.string.gestures_title to R.string.studio_kw_gestures
            StudioTarget.Keys -> R.string.keys_title to R.string.studio_kw_keys
            StudioTarget.Motion -> R.string.motion to R.string.studio_kw_motion
            StudioTarget.Haptics -> R.string.haptics to R.string.studio_kw_haptics
            StudioTarget.Flows -> R.string.flow_manage to R.string.studio_kw_flows
            StudioTarget.DefaultHome -> R.string.default_home_section to R.string.studio_kw_default_home
            StudioTarget.Permissions -> R.string.permissions_title to R.string.studio_kw_permissions
            StudioTarget.Hidden -> R.string.hidden_manage to R.string.studio_kw_hidden
            StudioTarget.Backup -> R.string.backup_title to R.string.studio_kw_backup
            StudioTarget.About -> R.string.about_title to R.string.studio_kw_about
        }
        val name = stringResource(label)
        // The path names every page on the way, never the row twice (no "Ovládání › Ovládání").
        val path = generateSequence(t.section) { it.parent }.toList().asReversed().map { areas.getValue(it) }
            .let { if (it.last() == name) it else it + name }.joinToString(" › ")
        StudioEntry(t, name, path, stringResource(kw))
    }
}

/** The main page: search, the five areas with what is set in each, and a few direct ways to what is used most. */
@Composable
private fun StudioMain(model: HomeViewModel, settings: LauncherSettings, onBack: () -> Unit, onEditHome: () -> Unit,
    onOpen: (StudioTarget) -> Unit, onArea: (StudioSection) -> Unit) {
    val hidden by model.hiddenApps.collectAsStateWithLifecycle()
    val favorites by model.favoriteApps.collectAsStateWithLifecycle()
    val everywhere by PulseAccessibilityService.running.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val entries = studioEntries()
    val hits = remember(query, entries) { StudioIndex.search(entries, query) }
    val active = settings.home.activeLayout()
    val controls = settings.controls
    StudioFrame(settings, model, stringResource(R.string.settings), onBack, preview = true) {
        StudioSearchField(query) { query = it }
        if (query.isNotBlank()) {
            if (hits.isEmpty()) Hint(stringResource(R.string.studio_no_results))
            // A result only opens on a tap; typing never navigates.
            else VeilGroup { hits.forEach { e -> SettingRow(e.label, e.path, { onOpen(e.target) }) } }
            return@StudioFrame
        }
        VeilGroup {
            SettingRow(stringResource(R.string.look), listOf(stringResource(themeName(settings.theme)),
                stringResource(if (settings.veilSolid) R.string.look_solid else glassName(settings.glass)),
                stringResource(wallName(settings.wallpaper.ifEmpty { null }))).joinToString(" · "), { onArea(StudioSection.Look) }, glyph = Glyph.Palette)
            SettingRow(stringResource(R.string.settings_group_home), listOf(active.name, "${active.grid.cols}×${active.grid.rows}",
                stringResource(R.string.studio_favorites_n, favorites.size)).joinToString(" · "), { onArea(StudioSection.Home) }, glyph = Glyph.Grid)
            SettingRow(stringResource(R.string.settings_group_controls), listOf(stringResource(R.string.pulse) + " " + pulseState(settings, everywhere),
                stringResource(R.string.studio_arc_sets_n, controls.arcSets.size.coerceAtLeast(1)),
                stringResource(R.string.studio_bound_n, controls.bindings.size)).joinToString(" · "), { onArea(StudioSection.Controls) }, glyph = Glyph.Touch)
            SettingRow(stringResource(R.string.studio_area_actions), stringResource(R.string.studio_flow_sub, settings.aliases.size,
                controls.arcSets.sumOf { set -> set.slots.count { it is Action.Flow } }), { onArea(StudioSection.Actions) }, glyph = Glyph.Flow)
            SettingRow(stringResource(R.string.studio_area_system), stringResource(R.string.settings_system_sub, hidden.size),
                { onArea(StudioSection.System) }, glyph = Glyph.Shield)
        }
        // The few places owners come back to most; one tap away instead of two.
        VeilCaption(stringResource(R.string.studio_quick))
        VeilChoice(listOf(0 to stringResource(R.string.edit_home), 1 to stringResource(R.string.pulse), 2 to stringResource(R.string.arc_section)),
            null, { when (it) { 0 -> onEditHome(); 1 -> onOpen(StudioTarget.Pulse); else -> onOpen(StudioTarget.Arc) } },
            Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun pulseState(settings: LauncherSettings, everywhere: Boolean) = stringResource(when (settings.pulseMode()) {
    PulseMode.Off -> R.string.off
    PulseMode.Home -> R.string.pulse_home_only
    PulseMode.Everywhere -> if (everywhere) R.string.pulse_everywhere_short else R.string.pulse_home_only
})

/** Ovládání: PULSE, ARC, every gesture and key, and how the launcher moves and answers touch. */
@Composable
private fun ControlsArea(settings: LauncherSettings, onPage: (StudioPane) -> Unit) {
    val everywhere by PulseAccessibilityService.running.collectAsStateWithLifecycle()
    val controls = settings.controls
    VeilGroup {
        SettingRow(stringResource(R.string.pulse), pulseState(settings, everywhere), { onPage(StudioPane.Pulse) }, glyph = Glyph.Pulse)
        SettingRow(stringResource(R.string.arc_section), stringResource(R.string.settings_arc_sub, controls.arcSets.size.coerceAtLeast(1),
            controls.arc.slots), { onPage(StudioPane.Arc) }, glyph = Glyph.Arc)
        SettingRow(stringResource(R.string.gestures_title), stringResource(R.string.studio_gestures_sub, controls.bindings.size, controls.keys.size),
            { onPage(StudioPane.Gestures) }, glyph = Glyph.Touch)
        SettingRow(stringResource(R.string.motion_section), stringResource(motionName(settings.motion)) + " · " +
            stringResource(hapticsName(settings.haptics)), { onPage(StudioPane.Motion) }, glyph = Glyph.Wave)
    }
    Hint(stringResource(R.string.studio_controls_hint))
}

@Composable
private fun StudioSearchField(query: String, onChange: (String) -> Unit) {
    val ink = LocalInk.current
    val hint = stringResource(R.string.studio_search)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 52.dp).clip(RoundedCornerShape(26.dp))
        .background(ink.ink.copy(alpha = .07f)).padding(start = 18.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        HomeGlyph(Glyph.Search, null, tint = ink.muted, size = 20.dp)
        androidx.compose.foundation.text.BasicTextField(query, onChange, Modifier.weight(1f).padding(start = 12.dp).semantics { contentDescription = hint },
            singleLine = true, textStyle = InkText.body.copy(color = ink.ink), cursorBrush = androidx.compose.ui.graphics.SolidColor(ink.accent),
            decorationBox = { inner -> Box { if (query.isEmpty()) Text(hint, style = InkText.body, color = ink.muted); inner() } })
        if (query.isNotEmpty()) IconAction(Glyph.Close, stringResource(R.string.studio_search_clear), { onChange("") }, tint = ink.muted)
    }
}

/** Plocha: the editor, the active layout's grid and placement without opening it, and the favourites. */
@Composable
private fun HomePage(settings: LauncherSettings, set: SettingsEdit, onEditHome: () -> Unit, onFavorites: () -> Unit) {
    val active = settings.home.activeLayout()
    VeilGroup {
        SettingRow(stringResource(R.string.edit_home), stringResource(R.string.home_edit_sub), onEditHome, glyph = Glyph.Layers,
            anchor = StudioTarget.EditHome.anchor)
        SettingRow(stringResource(R.string.favorites_title), stringResource(R.string.settings_favorites_sub, settings.favorites.size,
            LauncherSettings.MAX_FAVORITES), onFavorites, glyph = Glyph.Apps)
    }
    VeilGroup(stringResource(R.string.home_grid)) {
        SettingBlock(StudioTarget.Grid.anchor) {
            SliderRow(stringResource(R.string.home_grid_cols), active.grid.cols.toFloat(), 3f, 6f, 1f, active.grid.cols.toString()) { v ->
                set { it.copy(home = it.home.withLayout { l -> l.copy(grid = l.grid.copy(cols = v.roundToInt())) }) }
            }
            SliderRow(stringResource(R.string.home_grid_rows), active.grid.rows.toFloat(), 4f, 8f, 1f, active.grid.rows.toString()) { v ->
                set { it.copy(home = it.home.withLayout { l -> l.copy(grid = l.grid.copy(rows = v.roundToInt())) }) }
            }
            Text(stringResource(R.string.home_grid_hint), Modifier.padding(4.dp), style = InkText.small, color = LocalInk.current.muted)
        }
        ChoiceSetting(stringResource(R.string.home_placement), listOf(Placement.Grid to stringResource(R.string.placement_grid),
            Placement.Free to stringResource(R.string.placement_free), Placement.Hybrid to stringResource(R.string.placement_hybrid)),
            active.placement, { v -> set { it.copy(home = it.home.withLayout { l -> l.copy(placement = v) }) } },
            anchor = StudioTarget.Placement.anchor)
    }
}

/** Oblíbené: the favourites in order; the first four sit around LENS in the linked layout. */
@Composable
private fun FavoritesPage(model: HomeViewModel, settings: LauncherSettings) {
    val favorites by model.favoriteApps.collectAsStateWithLifecycle()
    Hint(stringResource(R.string.favorites_hint))
    Kicker(stringResource(R.string.favorites_count, settings.favorites.size, LauncherSettings.MAX_FAVORITES),
        Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp), color = LocalInk.current.muted)
    favorites.forEachIndexed { index, app ->
        OrderedAppRow(app, index, onUp = { model.moveFavorite(app.key, -1) }, onRemove = { model.toggleFavorite(app) })
    }
}

/**
 * Studio › Ovládání › ARC (M2): the arc's look, then every set slot by slot. Slot 1 sits in the middle of the arc,
 * the next ones left and right alternately; an empty slot stays where it is. Each edit writes `controlConfig`.
 */
@Composable
private fun ArcPage(model: HomeViewModel, settings: LauncherSettings) {
    val ink = LocalInk.current
    val arc by model.arc.collectAsStateWithLifecycle()
    val apps by model.visibleApps.collectAsStateWithLifecycle()
    val all by model.allApps.collectAsStateWithLifecycle()
    val shortcuts by model.shortcuts.collectAsStateWithLifecycle()
    val role = rememberHomeRole()
    val host = rememberActionHost(model) { }
    val controls = settings.controls
    val look = controls.arc
    var picking by remember { mutableStateOf<Pair<String, Int>?>(null) }
    // Two ARCs, one page: LENS ARC on Home and Global ARC under the camera each have their own look; sets are shared.
    var global by rememberSaveable { mutableStateOf(false) }
    fun look(transform: (ArcLook) -> ArcLook) = model.editControls { it.withLook(transform(it.arc)) }
    fun globalLook(transform: (GlobalArcLook) -> GlobalArcLook) = model.editControls { it.withGlobalLook(transform(it.globalArc)) }
    val sizes = listOf(ArcLook.RADIUS_S to "S", ArcLook.RADIUS_M to "M", ArcLook.RADIUS_L to "L")
    val spans = listOf(ArcLook.SPAN_S to stringResource(R.string.arc_angle_narrow), ArcLook.SPAN_M to stringResource(R.string.arc_angle_mid),
        ArcLook.SPAN_L to stringResource(R.string.arc_angle_wide))

    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Chips(listOf(false to stringResource(R.string.arc_tab_lens), true to stringResource(R.string.arc_tab_global)), global) { global = it }
    }
    if (!global) {
        Hint(stringResource(R.string.arc_explain_sets))
        Stacked(stringResource(R.string.arc_slots)) {
            Segments((ArcLook.MIN_SLOTS..ArcLook.MAX_VISIBLE).map { it to it.toString() }, look.slots, { v -> look { it.copy(slots = v) } })
        }
        ChoiceRow(stringResource(R.string.arc_size)) { Segments(sizes, look.radius, { v -> look { it.copy(radius = v) } }) }
        ChoiceRow(stringResource(R.string.arc_angle)) { Segments(spans, look.span, { v -> look { it.copy(span = v) } }) }
        ToggleRow(stringResource(R.string.arc_tap_sets), look.tapSets, { on -> look { it.copy(tapSets = on) } },
            detail = stringResource(R.string.arc_tap_sets_detail))
        ToggleRow(stringResource(R.string.arc_haptics), look.haptics, { on -> look { it.copy(haptics = on) } },
            detail = stringResource(R.string.arc_haptics_detail))
    } else {
        val g = controls.globalArc
        Hint(stringResource(R.string.global_arc_explain, look.slots))
        ChoiceRow(stringResource(R.string.arc_size)) { Segments(sizes, g.radius, { v -> globalLook { it.copy(radius = v) } }) }
        ChoiceRow(stringResource(R.string.arc_angle)) { Segments(spans, g.span, { v -> globalLook { it.copy(span = v) } }) }
        ChoiceRow(stringResource(R.string.global_arc_dim)) {
            Segments(listOf(GlobalArcLook.DIM_S to stringResource(R.string.global_arc_dim_low), GlobalArcLook.DIM_M to stringResource(R.string.arc_angle_mid),
                GlobalArcLook.DIM_L to stringResource(R.string.global_arc_dim_high)), g.dim, { v -> globalLook { it.copy(dim = v) } })
        }
        ToggleRow(stringResource(R.string.arc_haptics), g.haptics, { on -> globalLook { it.copy(haptics = on) } },
            detail = stringResource(R.string.global_arc_haptics_detail))
    }

    val stored = controls.arcSets.ifEmpty { listOf(dev.intelia.launcher.action.ArcSet(Controls.MAIN_SET)) }
    stored.forEachIndexed { setIndex, set ->
        val view = arc.sets.getOrNull(setIndex)?.takeIf { it.id == set.id } ?: ArcSetView(set.id, set.name, List(look.slots) { null })
        val name = setLabel(view, setIndex)
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Kicker("${setIndex + 1} · $name · ${set.slots.take(look.slots).count { it != null }}/${look.slots}", Modifier.weight(1f), color = ink.accent)
            if (setIndex > 0) IconAction(Glyph.Close, stringResource(R.string.arc_set_remove, name), { model.editControls { it.removeSet(set.id) } },
                tint = ink.muted)
        }
        repeat(look.slots) { index ->
            val raw = set.slots.getOrNull(index)
            val shown = view.slots.getOrNull(index)
            val label = when {
                shown != null -> slotLabel(shown)
                raw != null -> stringResource(R.string.arc_slot_unavailable)
                else -> stringResource(R.string.arc_slot_empty)
            }
            val position = stringResource(if (index == 0) R.string.arc_slot_middle else R.string.arc_slot_n, index + 1)
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button,
                onClickLabel = stringResource(R.string.arc_slot_choose)) { picking = set.id to index }
                .padding(start = 24.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text((index + 1).toString(), Modifier.padding(end = 10.dp).semantics { contentDescription = position },
                    style = InkText.label.copy(letterSpacing = 0.sp), color = ink.faint)
                if (shown != null) ActionIcon(shown.action, label, 28.dp, ink.ink, app = shown.app, shortcut = shown.shortcut)
                else HomeGlyph(Glyph.Plus, null, tint = ink.faint, size = 22.dp)
                Text(label, Modifier.padding(start = 14.dp).weight(1f), style = InkText.body.copy(
                    fontStyle = if (shown?.flow != null) FontStyle.Italic else FontStyle.Normal), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (shown != null) ink.ink else ink.faint)
                if (raw != null && index > 0) IconAction(Glyph.Up, stringResource(R.string.move_earlier), { model.editControls { it.moveSlot(set.id, index, -1) } },
                    tint = ink.muted)
                if (raw != null) IconAction(Glyph.Close, stringResource(R.string.remove), { model.editControls { it.setSlot(set.id, index, null) } }, tint = ink.muted)
            }
        }
    }
    if (stored.size < Controls.MAX_SETS) NavRow(stringResource(R.string.arc_set_add), null, { model.editControls { it.addSet() ?: it } })

    picking?.let { (setId, index) ->
        val inArc = remember(settings) { controls.arcSets.flatMap { it.slots }.filterNotNull().toSet() }
        val names = remember(all) { all.filter { it.mainProfile }.associate { it.component.packageName to it.label } }
        ActionPicker(stringResource(R.string.arc_slot_choose),
            apps = apps.filter { Action.App(it.key) !in inArc }.sortedBy { it.label.lowercase() },
            flows = settings.aliases.filter { Action.Flow(it.id) !in inArc },
            shortcuts = shortcuts.filter { Action.Shortcut(it.packageName, it.id) !in inArc },
            system = SystemActions.filter { it !in inArc },
            host = host, appNames = names,
            shortcutsNote = stringResource(if (role.held) R.string.shortcuts_none else R.string.shortcuts_need_home),
            onPick = { action -> model.editControls { it.setSlot(setId, index, action) }; picking = null },
            onDismiss = { picking = null })
    }
}

/** Akce a FLOW: the named commands; which of them ARC holds is chosen on the ARC page. */
@Composable
private fun FlowPage(settings: LauncherSettings, onFlows: () -> Unit) {
    VeilGroup {
        SettingRow(stringResource(R.string.flow_manage), stringResource(R.string.studio_flow_sub, settings.aliases.size,
            settings.controls.arcSets.sumOf { set -> set.slots.count { it is Action.Flow } }), onFlows, glyph = Glyph.Flow,
            anchor = StudioTarget.Flows.anchor)
    }
    Hint(stringResource(R.string.onboarding_gestures_body))
}

/**
 * M4: which hardware keys reach Intelia at all. A press here is only shown (and kept from changing the volume) and
 * its key code offered in Ovládání; nothing runs here. Back still goes back.
 */
@Composable
private fun KeysPage(onSeen: (Int) -> Unit) {
    val ink = LocalInk.current
    var seen by remember { mutableStateOf(emptyList<KeyHit>()) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Hint(stringResource(R.string.keys_hint))
    val last = seen.firstOrNull()
    Text(if (last == null) stringResource(R.string.keys_waiting) else stringResource(R.string.keys_caught, keyName(last.code)),
        Modifier.fillMaxWidth().focusRequester(focus).focusable()
            .onPreviewKeyEvent { e ->
                val key = e.nativeKeyEvent
                if (key.keyCode == android.view.KeyEvent.KEYCODE_BACK) return@onPreviewKeyEvent false
                if (e.type == KeyEventType.KeyDown) {
                    seen = KeyProbe.record(seen, key.keyCode, key.repeatCount)
                    // Only the code is kept, so Ovládání can offer this key; never what or when it was pressed.
                    if (key.repeatCount == 0) onSeen(key.keyCode)
                }
                true
            }
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        style = InkText.body, color = if (last == null) ink.muted else ink.ink)
    seen.forEach { hit ->
        Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { }.padding(horizontal = 24.dp, vertical = 10.dp)) {
            Text(keyName(hit.code), style = InkText.body)
            Text(stringResource(if (hit.held) R.string.keys_seen_held else R.string.keys_seen, hit.presses, hit.code),
                style = InkText.small, color = ink.muted)
        }
    }
    Section(R.string.keys_never)
    Hint(stringResource(R.string.keys_never_hint))
}

@Composable
internal fun keyName(code: Int): String = when (code) {
    android.view.KeyEvent.KEYCODE_VOLUME_UP -> stringResource(R.string.key_volume_up)
    android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> stringResource(R.string.key_volume_down)
    android.view.KeyEvent.KEYCODE_CAMERA -> stringResource(R.string.key_camera)
    android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, android.view.KeyEvent.KEYCODE_HEADSETHOOK -> stringResource(R.string.key_media)
    else -> android.view.KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_")
}

/** Animace a haptika: how the launcher moves (Vyp = Reduced Motion) and answers touch. */
@Composable
private fun MotionPage(settings: LauncherSettings, set: SettingsEdit) {
    VeilGroup {
        ChoiceSetting(stringResource(R.string.motion), listOf(Motion.Calm to stringResource(R.string.motion_calm),
            Motion.Brisk to stringResource(R.string.motion_brisk), Motion.Off to stringResource(R.string.off)), settings.motion,
            { v -> set { it.copy(motion = v) } }, detail = stringResource(R.string.motion_detail), anchor = StudioTarget.Motion.anchor)
        ChoiceSetting(stringResource(R.string.haptics), listOf(Haptics.Soft to stringResource(R.string.haptics_soft),
            Haptics.Strong to stringResource(R.string.haptics_strong), Haptics.Off to stringResource(R.string.off)), settings.haptics,
            { v -> set { it.copy(haptics = v) } }, detail = stringResource(R.string.haptics_detail), anchor = StudioTarget.Haptics.anchor)
    }
    ResetDefaults(stringResource(R.string.reset_motion_what)) {
        val d = LauncherSettings.fresh()
        set { it.copy(motion = d.motion, haptics = d.haptics) }
    }
}

private fun motionName(m: Motion) = when (m) {
    Motion.Calm -> R.string.motion_calm
    Motion.Brisk -> R.string.motion_brisk
    Motion.Off -> R.string.off
}

private fun hapticsName(h: Haptics) = when (h) {
    Haptics.Soft -> R.string.haptics_soft
    Haptics.Strong -> R.string.haptics_strong
    Haptics.Off -> R.string.off
}

/**
 * Systém a data: what Intelia may do in Android (default home, the PULSE service, notification access for music),
 * hidden apps, backup and about. Each permission row says its state and opens Android's own page; nothing is granted here.
 */
@Composable
private fun SystemPage(model: HomeViewModel, onHidden: () -> Unit, onBackup: () -> Unit) {
    val context = LocalContext.current
    val hidden by model.hiddenApps.collectAsStateWithLifecycle()
    val service by PulseAccessibilityService.running.collectAsStateWithLifecycle()
    var media by remember { mutableStateOf(MediaWatch.allowed(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { media = MediaWatch.allowed(context) }
    val role = rememberHomeRole()
    VeilGroup(stringResource(R.string.permissions_title), Modifier.settingAnchor(StudioTarget.Permissions.anchor)) {
        SettingRow(stringResource(R.string.default_home_section), stringResource(if (role.held) R.string.default_home_active else R.string.default_home_why),
            if (role.held) null else role.request, glyph = Glyph.Studio, anchor = StudioTarget.DefaultHome.anchor,
            trailing = if (role.held) { { HomeGlyph(Glyph.Check, null, tint = LocalInk.current.accent) } } else null)
        SettingRow(stringResource(R.string.permission_pulse), stringResource(if (service) R.string.permission_on else R.string.permission_pulse_off),
            { PulseAccessibilityService.openSettings(context) }, glyph = Glyph.Pulse)
        SettingRow(stringResource(R.string.permission_media), stringResource(if (media) R.string.permission_on else R.string.permission_media_off),
            { MediaWatch.openSettings(context) }, glyph = Glyph.Play)
    }
    VeilGroup(stringResource(R.string.settings_group_data)) {
        SettingRow(stringResource(R.string.hidden_manage), hidden.size.toString(), onHidden, glyph = Glyph.Close, anchor = StudioTarget.Hidden.anchor)
        SettingRow(stringResource(R.string.backup_title), stringResource(R.string.settings_backup_sub), onBackup, glyph = Glyph.Layers)
    }
    VeilGroup(stringResource(R.string.about_title), Modifier.settingAnchor(StudioTarget.About.anchor)) { About() }
}

/**
 * Export and restore of the one settings document through the system file picker (D17): no permission, one file
 * per action. A restore asks first, keeps this phone's geometry and can be undone until Studio closes.
 */
@Composable
private fun BackupPart(model: HomeViewModel, beforeRestore: LauncherSettings?, onRestored: (LauncherSettings?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<Int?>(null) }
    var pending by remember { mutableStateOf<LauncherSettings?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = BackupCodec.encode(model.settings.value)
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
                }.getOrDefault(false)
            }
            message = if (ok) R.string.backup_saved else R.string.backup_err_io
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val read = withContext(Dispatchers.IO) {
                // null = the file could not be opened or read; a too long file reads as null text → TooLarge.
                runCatching { context.contentResolver.openInputStream(uri)?.use { BackupCodec.decode(BackupCodec.readBounded(it)) } }
                    .getOrNull()
            }
            when (read) {
                null -> message = R.string.backup_err_io
                is BackupRead.Ok -> { message = null; pending = read.settings }
                is BackupRead.Failed -> message = when (read.error) {
                    BackupError.TooLarge -> R.string.backup_err_large
                    BackupError.NotABackup -> R.string.backup_err_not
                    BackupError.Newer -> R.string.backup_err_newer
                    BackupError.Damaged -> R.string.backup_err_damaged
                }
            }
        }
    }
    Hint(stringResource(R.string.backup_hint))
    NavRow(stringResource(R.string.backup_export), null, { export.launch("intelia-${LocalDate.now()}.json") })
    NavRow(stringResource(R.string.backup_import), null, {
        restore.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
    })
    if (beforeRestore != null) {
        Quiet(stringResource(R.string.backup_undo), {
            model.updateSettings { beforeRestore }
            onRestored(null)
            message = null
        }, Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
    }
    message?.let { Hint(stringResource(it)) }
    pending?.let { file ->
        ConfirmDialog(stringResource(R.string.backup_confirm, file.aliases.size, file.favorites.size, file.hidden.size),
            stringResource(R.string.backup_replace), onConfirm = {
                onRestored(model.settings.value)
                model.updateSettings { BackupCodec.applyTo(it, file) }
                pending = null
                message = R.string.backup_restored
            }, onDismiss = { pending = null })
    }
}

@Composable
private fun Section(title: Int) {
    Kicker(stringResource(title), Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 6.dp), color = LocalInk.current.accent)
    Hairline(Modifier.padding(horizontal = 24.dp))
}

@Composable
internal fun Hint(text: String) {
    Text(text, Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = InkText.small, color = LocalInk.current.muted)
}

/** A label with its choices on the line below, so long words never squeeze. */
@Composable
private fun Stacked(label: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp)) {
        Text(label, style = InkText.body)
        content()
    }
}

@Composable
private fun OrderedAppRow(app: AppEntry, index: Int, onUp: (() -> Unit)?, onRemove: () -> Unit) {
    val ink = LocalInk.current
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 24.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text((index + 1).toString(), Modifier.padding(end = 10.dp), style = InkText.label.copy(letterSpacing = 0.sp), color = ink.faint)
        AppIcon(app, Modifier.size(28.dp))
        Text(app.label, Modifier.padding(start = 14.dp).weight(1f), style = InkText.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (onUp != null && index > 0) IconAction(Glyph.Up, stringResource(R.string.move_earlier), onUp, tint = ink.muted)
        IconAction(Glyph.Close, stringResource(R.string.remove), onRemove, tint = ink.muted)
    }
}

@Composable
private fun About() {
    val context = LocalContext.current
    val ink = LocalInk.current
    val version = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }
    }
    Row(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        InteliaMark(Modifier.size(26.dp))
        Text("Intelia", Modifier.padding(start = 12.dp), style = InkText.item)
        Text(version, Modifier.padding(start = 10.dp), style = InkText.label.copy(letterSpacing = 0.sp), color = ink.faint)
    }
    Hint(stringResource(R.string.local_only))
    Hint(stringResource(R.string.about_text))
}

@Composable
private fun HiddenApps(model: HomeViewModel, onBack: () -> Unit) {
    val ink = LocalInk.current
    val hidden by model.hiddenApps.collectAsStateWithLifecycle()
    SystemBarIcons(darkIcons = !ink.dark)
    InkPage(stringResource(R.string.studio), onBack, title = stringResource(R.string.hidden_apps)) {
        if (hidden.isEmpty()) Hint(stringResource(R.string.no_hidden))
        LazyColumn(Modifier.weight(1f)) {
            items(hidden, key = { it.key }) { app ->
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 24.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(app, Modifier.size(30.dp))
                    Text(app.label, Modifier.padding(start = 14.dp).weight(1f), style = InkText.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Quiet(stringResource(R.string.unhide), { model.unhide(app) })
                }
            }
        }
    }
}

/**
 * Home in miniature: clock in its two voices, the layout and LENS. Used live in Studio (on the
 * wallpaper), drawn from the active layout [home] as Home draws it, and in onboarding (on paper,
 * from the legacy fields, with plain rings for apps not chosen yet).
 */
@Composable
fun HomePreview(
    settings: LauncherSettings,
    favorites: List<AppEntry>,
    now: LocalDateTime,
    ink: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    onWallpaper: Boolean = false,
    home: LayoutSpec? = null,
    entry: (String) -> AppEntry? = { null },
) {
    val sketch = remember(home) { home?.sketch() }
    val custom = if (sketch != null) sketch.list.isNotEmpty() else settings.layout == HomeLayout.Custom
    fun d(v: Float): Dp = (v * scale).dp
    // From a layout: Home's own distances (text under the status bar, LENS 48 dp, icons 32 dp, centre ~67 dp above the
    // bottom edge); the onboarding sketch keeps its looser card proportions.
    val real = sketch != null
    val top = if (real) 56f else 64f
    val ring = if (real) 48f else 44f
    BoxWithConstraints(modifier.semantics(mergeDescendants = true) { }) {
        val textRoom = maxHeight - d(top) - d(84f)
        Column(Modifier.fillMaxWidth().padding(top = d(top), start = d(28f), end = d(28f)),
            horizontalAlignment = if (custom) Alignment.Start else Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(d(8f))) {
            // A big or stacked clock is shrunk as a whole to the room above LENS, so it never runs into the row.
            if (home != null && sketch != null) Box(Modifier.fitHeight(textRoom, if (custom) 0f else .5f)) {
                // Drawn at Home's own size and shrunk as a picture: the date's size has a floor that would not scale.
                Box(Modifier.shrink(scale)) {
                    HomeClockPreview(home.items.firstOrNull { it.kind == ItemKind.Clock }, home.items.firstOrNull { it.kind == ItemKind.Date },
                        sketch.dateAbove, settings, now, ink, accent, 1f)
                }
            }
            else if (settings.showClock) SplitClock(now.toLocalTime(), settings.clockStyle, ink, accent, (84f * scale).sp, shadow = onWallpaper)
            if (home == null && settings.showDate) Text(dateLine(now), style = InkText.label.copy(fontSize = (11f * scale).coerceAtLeast(8f).sp,
                letterSpacing = (2.2f * scale).sp, shadow = if (onWallpaper) homeShadow(ink) else null), color = ink.copy(alpha = .8f), maxLines = 1)
        }
        if (custom) Column(Modifier.align(Alignment.BottomStart).padding(start = d(28f), bottom = d(92f))) {
            val names = (sketch?.list?.mapNotNull { entry(it)?.label } ?: favorites.map { it.label }).ifEmpty { listOf("—", "—", "—") }.take(4)
            names.forEach {
                Text(it, style = InkText.item.copy(fontSize = (26f * scale).sp, lineHeight = (30f * scale).sp,
                    shadow = if (onWallpaper) homeShadow(ink) else null), color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = d(if (real) 43f else 26f)), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d(if (real) 26f else 14f))) {
            if (sketch != null) sketch.row.forEach {
                when {
                    it == HomeSketch.LENS_SLOT -> LensMark(ink, ring = d(ring))
                    it == null -> Spacer(Modifier.size(d(32f)))
                    else -> PreviewSlot(if (it.kind == ItemKind.App) entry(it.target) else null, ink, scale, it.iconDp(settings.iconSize).toFloat())
                }
            } else {
                val around = settings.layout == HomeLayout.Essentials
                val slots: List<AppEntry?> = if (around) List(4) { favorites.getOrNull(it) } else emptyList()
                slots.take(2).forEach { PreviewSlot(it, ink, scale) }
                LensMark(ink, ring = d(44f))
                slots.drop(2).forEach { PreviewSlot(it, ink, scale) }
            }
        }
    }
}

/** Lays the content out [k] times larger than the room and draws it scaled by [k] from the top start. */
private fun Modifier.shrink(k: Float) = layout { m, c ->
    fun up(v: Int) = if (v == androidx.compose.ui.unit.Constraints.Infinity) v else (v / k).roundToInt()
    val p = m.measure(androidx.compose.ui.unit.Constraints(0, up(c.maxWidth), 0, up(c.maxHeight)))
    layout((p.width * k).roundToInt(), (p.height * k).roundToInt()) {
        p.placeWithLayer(0, 0) { scaleX = k; scaleY = k; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f) }
    }
}

/** Measures the content at its full height and, when taller than [max], draws it scaled down from the top. */
private fun Modifier.fitHeight(max: Dp, originX: Float) = layout { m, c ->
    val p = m.measure(c.copy(maxHeight = androidx.compose.ui.unit.Constraints.Infinity))
    val limit = max.roundToPx().coerceAtLeast(1)
    val k = if (p.height > limit) limit.toFloat() / p.height else 1f
    layout(p.width, minOf(p.height, limit)) {
        p.placeWithLayer(0, 0) { scaleX = k; scaleY = k; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(originX, 0f) }
    }
}

/**
 * The clock with its bound date, drawn with the item's own face, size, weight, colour, opacity and plate, scaled.
 * Either item may be missing (hidden on Home).
 */
@Composable
private fun HomeClockPreview(clock: HomeItem?, date: HomeItem?, dateAbove: Boolean, settings: LauncherSettings, now: LocalDateTime,
    ink: Color, accent: Color, scale: Float) {
    val bound = date?.takeIf { it.anchor.type == AnchorType.Bound }
    // A free date has its own place on Home; in the miniature it sits above or below the clock, as on Home.
    val free = date?.takeIf { it.anchor.type != AnchorType.Bound }
    fun scaled(d: HomeItem) = d.copy(sizeDp = (d.dateSp() * scale).roundToInt().coerceAtLeast(1),
        look = d.look.copy(padX = d.look.padX * scale, padY = d.look.padY * scale))
    val plate = !clock?.background.isNullOrEmpty()
    val clockInk = if (plate) LocalCrystal.current.fg else ink
    Column(verticalArrangement = Arrangement.spacedBy((10f * scale).dp)) {
        if (free != null && dateAbove) DateText(scaled(free), now, ink, accent, Modifier)
        if (clock != null || bound != null) Column(Modifier.then(if (plate) Modifier.veilSurface(plateTokens(clock.background), VeilOrigin.Top,
                RoundedCornerShape((26f * scale).dp)).padding(horizontal = ((20f + clock.look.padX) * scale).dp,
                vertical = ((15f + clock.look.padY) * scale).dp) else Modifier),
            verticalArrangement = Arrangement.spacedBy((10f * scale).dp)) {
            if (clock != null) {
                val size = if (clock.sizeDp == 0) ItemSizes.CLOCK else clock.sizeDp
                SplitClock(now.toLocalTime(), clock.clockStyle(settings.clockStyle), clock.tintColor(accent) ?: clockInk, accent, (size * scale).sp,
                    Modifier.graphicsLayer { alpha = clock.alpha }, shadow = !plate, weight = clock.weight, look = clock.look)
            }
            if (bound != null) DateText(scaled(bound), now, clockInk, accent, Modifier.offset((bound.look.dx * scale).dp, (bound.look.dy * scale).dp))
        }
        if (free != null && !dateAbove) DateText(scaled(free), now, ink, accent, Modifier)
    }
}

@Composable
private fun PreviewSlot(app: AppEntry?, ink: Color, scale: Float, dp: Float = 30f) {
    val size = (dp * scale).dp
    if (app != null) AppIcon(app, Modifier.size(size), tint = ink)
    else Canvas(Modifier.size(size)) { drawCircle(ink.copy(alpha = .5f), this.size.minDimension / 3f, style = Stroke(1.dp.toPx())) }
}

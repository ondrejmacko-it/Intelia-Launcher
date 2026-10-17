package dev.intelia.launcher.ui

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
import dev.intelia.launcher.action.Controls
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

/** The main page, one page per section (Home has none: it opens the editor) and their sub-pages; [depth] drives the slide. */
private enum class StudioPane(val depth: Int) {
    Main(1), Look(2), Icons(2), Favorites(2), Arc(2), Flow(2), Pulse(2), Gestures(2), Motion(2), Backup(2), System(2), About(2), Home(2),
    Hidden(3), Lens(3),
}

private fun StudioSection.pane() = when (this) {
    StudioSection.Look -> StudioPane.Look
    StudioSection.Icons -> StudioPane.Icons
    StudioSection.Home -> StudioPane.Home
    StudioSection.Favorites -> StudioPane.Favorites
    StudioSection.Arc -> StudioPane.Arc
    StudioSection.Flow -> StudioPane.Flow
    StudioSection.Pulse -> StudioPane.Pulse
    StudioSection.Gestures -> StudioPane.Gestures
    StudioSection.Motion -> StudioPane.Motion
    StudioSection.Backup -> StudioPane.Backup
    StudioSection.System -> StudioPane.System
    StudioSection.About -> StudioPane.About
}

/** Where Settings opens: its main page from Home, the same page from the editor's tray (Back returns to the editor), or PULSE calibration. */
enum class StudioStart { Main, Editor, Lens }

/**
 * Settings (Studio, Veil 3.0 B4): a live preview on the wallpaper, a search over every setting and one page per
 * section — Vzhled first, then Plocha, Ovládání and Data a systém — each sliding in; Back returns to the same row.
 * Every setting lives in exactly one place. Changes apply immediately and persist through SettingsStore.
 */
@Composable
fun Studio(model: HomeViewModel, settings: LauncherSettings, onBack: () -> Unit, onFlows: () -> Unit, onEditHome: () -> Unit,
    start: StudioStart = StudioStart.Main) {
    var pane by rememberSaveable(start) { mutableStateOf(if (start == StudioStart.Lens) StudioPane.Lens else StudioPane.Main) }
    /** The part a search result asked for; the page opens with it unfolded. */
    var focus by rememberSaveable { mutableStateOf<StudioTarget?>(null) }
    /** Settings before a restore from a backup file (D17); offered back until Studio closes, never stored. */
    var beforeRestore by remember { mutableStateOf<LauncherSettings?>(null) }
    // The page's scroll and open part survive a deeper page too, so Back lands on the same row (Veil 3.0 motion).
    val panes = rememberSaveableStateHolder()
    val ms = motionMs(VeilMotion.PAGE_MS)
    /** Opened from the editor, leaving Settings goes back to the editor. */
    val leave = if (start == StudioStart.Editor) onEditHome else onBack
    fun up() {
        pane = when (pane) {
            StudioPane.Main -> { leave(); return }
            StudioPane.Hidden -> StudioPane.System
            StudioPane.Lens -> if (start == StudioStart.Lens) StudioPane.Main else StudioPane.Pulse
            else -> { panes.removeState(pane.name); focus = null; StudioPane.Main }
        }
    }
    fun open(section: StudioSection, target: StudioTarget?) {
        if (target == StudioTarget.EditHome) { onEditHome(); return }
        focus = target; panes.removeState(section.pane().name); pane = section.pane()
    }
    BackHandler(enabled = pane != StudioPane.Main || start == StudioStart.Editor) { up() }
    val set: SettingsEdit = { model.updateSettings(it) }
    AnimatedContent(pane, transitionSpec = {
        VeilMotion.page(if (targetState.depth >= initialState.depth) 1 else -1, ms)
    }, label = "studio-pane") { shown -> panes.SaveableStateProvider(shown.name) { when (shown) {
        StudioPane.Main -> StudioMain(model, settings, leave, onEditHome, ::open)
        StudioPane.Look -> StudioFrame(settings, model, stringResource(R.string.look), ::up, preview = true) {
            LookPage(settings, focus, set)
        }
        StudioPane.Icons -> StudioFrame(settings, model, stringResource(R.string.icons), ::up, preview = true) {
            CrystalBody { IconSettings(settings, set) }
        }
        StudioPane.Home -> StudioFrame(settings, model, stringResource(R.string.home_grid), ::up, preview = true) {
            HomePage(settings, set, onEditHome)
        }
        StudioPane.Favorites -> StudioFrame(settings, model, stringResource(R.string.favorites_title), ::up, preview = true) {
            FavoritesPage(model, settings)
        }
        StudioPane.Arc -> StudioFrame(settings, model, stringResource(R.string.arc_section), ::up) {
            ArcPage(model, settings)
        }
        StudioPane.Flow -> StudioFrame(settings, model, stringResource(R.string.flow), ::up) { FlowPage(settings, onFlows) }
        StudioPane.Pulse -> StudioFrame(settings, model, stringResource(R.string.pulse), ::up, preview = true) {
            PulsePage(settings, set, onLens = { pane = StudioPane.Lens })
        }
        StudioPane.Gestures -> StudioFrame(settings, model, stringResource(R.string.gestures_title), ::up) { GesturesPage() }
        StudioPane.Motion -> StudioFrame(settings, model, stringResource(R.string.motion_section), ::up) { MotionPage(settings, set) }
        StudioPane.Backup -> StudioFrame(settings, model, stringResource(R.string.backup_title), ::up) {
            BackupPart(model, beforeRestore) { beforeRestore = it }
        }
        StudioPane.System -> StudioFrame(settings, model, stringResource(R.string.studio_system), ::up) {
            SystemPage(model, onHidden = { pane = StudioPane.Hidden })
        }
        StudioPane.About -> StudioFrame(settings, model, stringResource(R.string.about_title), ::up) { About() }
        StudioPane.Hidden -> HiddenApps(model, onBack = ::up)
        StudioPane.Lens -> LensCalibration(settings.pulseLens, onSave = { lens ->
            model.updateSettings { it.copy(pulseLens = lens) }
            up()
        }, onBack = ::up)
    } } }
}

/**
 * One Settings page: the header (gear and title) on the wallpaper and a Veil Focus sheet below it that scrolls. Where
 * a change shows on Home, the live Home miniature takes half the room under the header and the sheet the other half;
 * elsewhere the sheet takes it all.
 */
@Composable
private fun StudioFrame(settings: LauncherSettings, model: HomeViewModel, title: String, onBack: () -> Unit, preview: Boolean = false,
    content: @Composable ColumnScope.() -> Unit) {
    val homeInk = LocalHomeInk.current
    val favorites by model.favoriteApps.collectAsStateWithLifecycle()
    val now by rememberNow()
    SystemBarIcons(darkIcons = homeInk.luminance() < .5f)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenW = maxWidth
        val screenH = maxHeight
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconAction(Glyph.Back, stringResource(R.string.back), onBack, tint = homeInk)
                HomeGlyph(Glyph.Settings, null, Modifier.padding(end = 10.dp), tint = homeInk, size = 18.dp)
                Text(title.uppercase(), style = InkText.label.copy(shadow = homeShadow(homeInk)), color = homeInk, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            // Home as a true miniature of the screen: every size and distance at one scale, fitted to its half.
            if (preview) BoxWithConstraints(Modifier.weight(PREVIEW_SHARE).fillMaxWidth().padding(bottom = 10.dp), contentAlignment = Alignment.Center) {
                val scale = minOf(maxHeight / screenH, maxWidth / screenW)
                val shape = RoundedCornerShape(16.dp)
                // TalkBack hears one "Home preview", not the live time and date as if they were content.
                val previewLabel = stringResource(R.string.home_preview)
                Box(Modifier.size(screenW * scale, screenH * scale).clip(shape).border(1.dp, homeInk.copy(alpha = .35f), shape)
                    .clearAndSetSemantics { contentDescription = previewLabel }) {
                    // The clock and date as Home draws them (font, size, colour, plate), not the old global clock style.
                    HomePreview(settings, favorites, now, homeInk, LocalWallPlaces.current[PaletteTarget.Clock], Modifier.fillMaxSize(),
                        scale = scale, onWallpaper = true, home = settings.home.activeLayout(), entry = model::entry)
                }
            }
            Column(Modifier.weight(if (preview) 1f - PREVIEW_SHARE else 1f).fillMaxWidth()
                .veilSurface(veil(VeilLevel.Focus), VeilOrigin.Top, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .verticalScroll(rememberScrollState()).navigationBarsPadding().padding(top = 8.dp, bottom = 24.dp), content = content)
        }
        // Live capsule at the real camera while PULSE is on: width and opacity change as you drag.
        if (preview && settings.pulse) Pulse(settings, preview = true)
    }
}

/** Every searchable place, in the owner's language. */
@Composable
private fun studioEntries(): List<StudioEntry> {
    val look = stringResource(R.string.look)
    val home = stringResource(R.string.settings_group_home)
    val controls = stringResource(R.string.settings_group_controls)
    val data = stringResource(R.string.settings_group_data)
    return StudioTarget.entries.map { t ->
        val (label, section, kw) = when (t) {
            StudioTarget.Material -> Triple(R.string.look_material, look, R.string.studio_kw_material)
            StudioTarget.Wallpaper -> Triple(R.string.wallpaper, look, R.string.studio_kw_wallpaper)
            StudioTarget.Theme -> Triple(R.string.theme, look, R.string.studio_kw_theme)
            StudioTarget.Colors -> Triple(R.string.palette_section, look, R.string.studio_kw_colors)
            StudioTarget.Icons -> Triple(R.string.icons, home, R.string.studio_kw_icons)
            StudioTarget.EditHome -> Triple(R.string.edit_home, home, R.string.studio_kw_edit_home)
            StudioTarget.Grid -> Triple(R.string.home_grid, home, R.string.studio_kw_grid)
            StudioTarget.Favorites -> Triple(R.string.favorites_title, home, R.string.studio_kw_favorites)
            StudioTarget.Arc -> Triple(R.string.arc_section, controls, R.string.studio_kw_arc)
            StudioTarget.Shortcuts -> Triple(R.string.arc_ring_shortcuts, controls, R.string.studio_kw_shortcuts)
            StudioTarget.Flows -> Triple(R.string.flow_manage, controls, R.string.studio_kw_flows)
            StudioTarget.Pulse -> Triple(R.string.pulse, controls, R.string.studio_kw_pulse)
            StudioTarget.Gestures -> Triple(R.string.gestures_title, controls, R.string.studio_kw_gestures)
            StudioTarget.Motion -> Triple(R.string.motion_section, controls, R.string.studio_kw_motion)
            StudioTarget.Backup -> Triple(R.string.backup_title, data, R.string.studio_kw_backup)
            StudioTarget.Hidden -> Triple(R.string.hidden_manage, data, R.string.studio_kw_hidden)
            StudioTarget.DefaultHome -> Triple(R.string.default_home_section, data, R.string.studio_kw_default_home)
            StudioTarget.About -> Triple(R.string.about_title, data, R.string.studio_kw_about)
        }
        val name = stringResource(label)
        StudioEntry(t, name, "$section › $name", stringResource(kw))
    }
}

@Composable
private fun StudioMain(model: HomeViewModel, settings: LauncherSettings, onBack: () -> Unit, onEditHome: () -> Unit,
    onOpen: (StudioSection, StudioTarget?) -> Unit) {
    val ink = LocalInk.current
    val hidden by model.hiddenApps.collectAsStateWithLifecycle()
    val everywhere by PulseAccessibilityService.running.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val entries = studioEntries()
    val hits = remember(query, entries) { StudioIndex.search(entries, query) }
    val active = settings.home.activeLayout()
    StudioFrame(settings, model, stringResource(R.string.settings), onBack, preview = true) {
        StudioSearchField(query) { query = it }
        if (query.isNotBlank()) {
            if (hits.isEmpty()) Hint(stringResource(R.string.studio_no_results))
            // A result only opens on a tap; typing never navigates.
            hits.forEach { e -> SectionRow(e.label, e.path) { onOpen(e.target.section, e.target) } }
            return@StudioFrame
        }
        // Vzhled first, on its own faintly lit plate: the one place for the launcher's look.
        Group(null, lit = true) {
            SectionRow(stringResource(R.string.look), listOf(stringResource(themeName(settings.theme)),
                stringResource(if (settings.veilSolid) R.string.look_solid else glassName(settings.glass)),
                stringResource(wallName(settings.wallpaper.ifEmpty { null }))).joinToString(" · ")) { onOpen(StudioSection.Look, null) }
        }
        Group(R.string.settings_group_home) {
            SectionRow(stringResource(R.string.edit_home), active.name + " · " + placementLabel(active), onEditHome)
            SectionRow(stringResource(R.string.home_grid), "${active.grid.cols}×${active.grid.rows}") { onOpen(StudioSection.Home, null) }
            SectionRow(stringResource(R.string.icons), stringResource(R.string.settings_icons_sub, stringResource(iconStyleName(settings.iconStyle)),
                settings.iconSize)) { onOpen(StudioSection.Icons, null) }
            SectionRow(stringResource(R.string.favorites_title), stringResource(R.string.settings_favorites_sub, settings.favorites.size,
                LauncherSettings.MAX_FAVORITES)) { onOpen(StudioSection.Favorites, null) }
        }
        Group(R.string.settings_group_controls) {
            SectionRow(stringResource(R.string.arc_section), stringResource(R.string.settings_arc_sub, settings.controls.arcSets.size.coerceAtLeast(1), settings.controls.arc.slots)) {
                onOpen(StudioSection.Arc, null)
            }
            SectionRow(stringResource(R.string.flow), stringResource(R.string.studio_flow_sub, settings.aliases.size, settings.controls.arcSets.sumOf { set -> set.slots.count { it is Action.Flow } })) {
                onOpen(StudioSection.Flow, null)
            }
            SectionRow(stringResource(R.string.pulse), stringResource(if (!settings.pulse) R.string.off
                else if (everywhere) R.string.pulse_everywhere_short else R.string.pulse_home_only)) { onOpen(StudioSection.Pulse, null) }
            SectionRow(stringResource(R.string.gestures_title), stringResource(R.string.settings_gestures_sub)) { onOpen(StudioSection.Gestures, null) }
            SectionRow(stringResource(R.string.motion_section), stringResource(motionName(settings.motion)) + " · " +
                stringResource(hapticsName(settings.haptics)) + " · " + "${(settings.textScale * 100).toInt()} %") { onOpen(StudioSection.Motion, null) }
        }
        Group(R.string.settings_group_data) {
            SectionRow(stringResource(R.string.backup_title), stringResource(R.string.settings_backup_sub)) { onOpen(StudioSection.Backup, null) }
            SectionRow(stringResource(R.string.studio_system), stringResource(R.string.settings_system_sub, hidden.size)) { onOpen(StudioSection.System, null) }
            SectionRow(stringResource(R.string.about_title), stringResource(R.string.local_only)) { onOpen(StudioSection.About, null) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** A titled group of rows on a faint plate; [lit] gives the plate a hint of the accent (Vzhled). */
@Composable
private fun Group(title: Int?, lit: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val ink = LocalInk.current
    if (title != null) Kicker(stringResource(title), Modifier.padding(start = 28.dp, end = 24.dp, top = 18.dp, bottom = 6.dp), color = ink.muted)
    val shape = RoundedCornerShape(22.dp)
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = if (title == null) 6.dp else 0.dp).clip(shape)
        .background(if (lit) ink.accent.copy(alpha = .10f) else ink.ink.copy(alpha = .045f))
        .then(if (lit) Modifier.border(1.dp, ink.accent.copy(alpha = .35f), shape) else Modifier), content = content)
}

/** A section or a search result: its name and, below, its state or where it lives. */
@Composable
private fun SectionRow(title: String, subtitle: String, onClick: () -> Unit) {
    val ink = LocalInk.current
    Row(Modifier.fillMaxWidth().heightIn(min = 62.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = InkText.body.copy(fontSize = 17.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = InkText.small, color = ink.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text("  →", Modifier.clearAndSetSemantics { }, style = InkText.body, color = ink.muted)
    }
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

/** The Crystal controls (chips, sliders, folds) at the sheet's margins. */
@Composable
private fun CrystalBody(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
}

/** Vzhled: the launcher's look, unfolded at the part a search asked for. */
@Composable
private fun LookPage(settings: LauncherSettings, focus: StudioTarget?, set: SettingsEdit) {
    val first = focus?.let { lookFoldTitle(it) }
    val group = remember { FoldGroup().also { it.open = first } }
    CompositionLocalProvider(LocalFoldGroup provides group) { CrystalBody { LookSettings(settings, set) } }
}

/** Plocha: the editor, then the active layout's grid and placement without opening it. */
@Composable
private fun HomePage(settings: LauncherSettings, set: SettingsEdit, onEditHome: () -> Unit) {
    val active = settings.home.activeLayout()
    SectionRow(stringResource(R.string.edit_home), stringResource(R.string.home_edit_sub), onEditHome)
    CrystalBody {
        SliderRow(stringResource(R.string.home_grid_cols), active.grid.cols.toFloat(), 3f, 6f, 1f, active.grid.cols.toString()) { v ->
            set { it.copy(home = it.home.withLayout { l -> l.copy(grid = l.grid.copy(cols = v.roundToInt())) }) }
        }
        SliderRow(stringResource(R.string.home_grid_rows), active.grid.rows.toFloat(), 4f, 8f, 1f, active.grid.rows.toString()) { v ->
            set { it.copy(home = it.home.withLayout { l -> l.copy(grid = l.grid.copy(rows = v.roundToInt())) }) }
        }
        Text(stringResource(R.string.home_grid_hint), Modifier.padding(vertical = 4.dp), style = CrystalType.caption, color = LocalCrystal.current.fg2)
        Kick(stringResource(R.string.home_placement))
        Chips(listOf(Placement.Grid to stringResource(R.string.placement_grid), Placement.Free to stringResource(R.string.placement_free),
            Placement.Hybrid to stringResource(R.string.placement_hybrid)), active.placement) { v ->
            set { it.copy(home = it.home.withLayout { l -> l.copy(placement = v) }) }
        }
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
    fun look(transform: (ArcLook) -> ArcLook) = model.editControls { it.withLook(transform(it.arc)) }

    Hint(stringResource(R.string.arc_explain_sets))
    ChoiceRow(stringResource(R.string.arc_slots)) {
        Segments((ArcLook.MIN_SLOTS..ArcLook.MAX_VISIBLE).map { it to it.toString() }, look.slots, { v -> look { it.copy(slots = v) } })
    }
    ChoiceRow(stringResource(R.string.arc_size)) {
        Segments(listOf(ArcLook.RADIUS_S to "S", ArcLook.RADIUS_M to "M", ArcLook.RADIUS_L to "L"), look.radius, { v -> look { it.copy(radius = v) } })
    }
    ChoiceRow(stringResource(R.string.arc_angle)) {
        Segments(listOf(ArcLook.SPAN_S to stringResource(R.string.arc_angle_narrow), ArcLook.SPAN_M to stringResource(R.string.arc_angle_mid),
            ArcLook.SPAN_L to stringResource(R.string.arc_angle_wide)), look.span, { v -> look { it.copy(span = v) } })
    }
    ToggleRow(stringResource(R.string.arc_haptics), look.haptics, { on -> look { it.copy(haptics = on) } },
        detail = stringResource(R.string.arc_haptics_detail))

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

/** FLOW: the named commands; which of them ARC holds is chosen on the ARC page. */
@Composable
private fun FlowPage(settings: LauncherSettings, onFlows: () -> Unit) {
    Hint(stringResource(R.string.onboarding_gestures_body))
    NavRow(stringResource(R.string.flow_manage), settings.aliases.size.toString(), onFlows)
}

/** PULSE: the capsule, its events and calibration, and whether it works outside Intelia (D9). */
@Composable
private fun PulsePage(settings: LauncherSettings, set: SettingsEdit, onLens: () -> Unit) {
    val everywhere by PulseAccessibilityService.running.collectAsStateWithLifecycle()
    val context = LocalContext.current
    CrystalBody { PulseInspector(settings, set, onLens) }
    // D9: the switch only mirrors the service; Android settings are where it is turned on or off.
    if (settings.pulse) ToggleRow(stringResource(R.string.pulse_everywhere), everywhere, { PulseAccessibilityService.openSettings(context) },
        detail = stringResource(R.string.pulse_everywhere_detail))
}

/** Gesta: what each gesture does, so nobody has to guess; they are fixed. */
@Composable
private fun GesturesPage() {
    val ink = LocalInk.current
    Hint(stringResource(R.string.gestures_hint))
    listOf(R.string.gesture_tap_lens to R.string.gesture_tap_lens_detail, R.string.gesture_swipe_up to R.string.gesture_swipe_up_detail,
        R.string.gesture_hold_lens to R.string.gesture_hold_lens_detail, R.string.gesture_hold_home_edit to R.string.gesture_hold_home_edit_detail,
        R.string.gesture_tap_camera to R.string.gesture_tap_camera_detail).forEach { (what, does) ->
        Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { }.padding(horizontal = 24.dp, vertical = 10.dp)) {
            Text(stringResource(what), style = InkText.body)
            Text(stringResource(does), style = InkText.small, color = ink.muted)
        }
    }
}

/** Animace a dotek: motion and haptics. */
@Composable
private fun MotionPage(settings: LauncherSettings, set: SettingsEdit) {
    Stacked(stringResource(R.string.motion)) {
        Segments(listOf(Motion.Calm to stringResource(R.string.motion_calm), Motion.Brisk to stringResource(R.string.motion_brisk),
            Motion.Off to stringResource(R.string.off)), settings.motion, { v -> set { it.copy(motion = v) } })
    }
    Stacked(stringResource(R.string.text_size)) {
        Segments(LauncherSettings.TEXT_SCALES.map { it to "${(it * 100).toInt()} %" }, settings.textScale, { v -> set { it.copy(textScale = v) } })
    }
    Stacked(stringResource(R.string.haptics)) {
        Segments(listOf(Haptics.Soft to stringResource(R.string.haptics_soft), Haptics.Strong to stringResource(R.string.haptics_strong),
            Haptics.Off to stringResource(R.string.off)), settings.haptics, { v -> set { it.copy(haptics = v) } })
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

/** Systém: becoming the default home (only while it is not) and hidden apps. */
@Composable
private fun SystemPage(model: HomeViewModel, onHidden: () -> Unit) {
    val hidden by model.hiddenApps.collectAsStateWithLifecycle()
    val role = rememberHomeRole()
    Section(R.string.default_home_section)
    if (role.held) Hint(stringResource(R.string.default_home_active))
    else {
        Hint(stringResource(R.string.default_home_why))
        Capsule(stringResource(R.string.default_home), role.request, Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
    }
    NavRow(stringResource(R.string.hidden_manage), hidden.size.toString(), onHidden)
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
private fun Hint(text: String) {
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

/** The share of the room under Studio's header that the Home miniature takes; the settings sheet gets the rest. */
private const val PREVIEW_SHARE = .5f

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

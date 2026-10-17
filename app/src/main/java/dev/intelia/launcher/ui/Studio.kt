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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.intelia.launcher.R
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

/** Main, the five section pages (Home has none: it opens the editor) and their sub-pages; [depth] drives the slide. */
private enum class StudioPane(val depth: Int) { Main(1), Look(2), Controls(2), Flow(2), System(2), Shortcuts(3), Hidden(3), Lens(3) }

private fun StudioSection.pane() = when (this) {
    StudioSection.Look -> StudioPane.Look
    StudioSection.Controls -> StudioPane.Controls
    StudioSection.Flow -> StudioPane.Flow
    StudioSection.System -> StudioPane.System
    StudioSection.Home -> StudioPane.Main
}

/**
 * Studio (Veil 3.0 B4): a live preview on the wallpaper, a search over every setting and five sections — Vzhled,
 * Plocha, Ovládání, Akce a FLOW, Systém — each a page that slides in; Back returns to the same row. Vzhled draws the
 * same [LookSettings] as the editor's Vzhled panel. Changes apply immediately and persist through SettingsStore.
 */
@Composable
fun Studio(model: HomeViewModel, settings: LauncherSettings, onBack: () -> Unit, onFlows: () -> Unit, onEditHome: () -> Unit,
    lensFirst: Boolean = false) {
    var pane by rememberSaveable { mutableStateOf(if (lensFirst) StudioPane.Lens else StudioPane.Main) }
    /** The part a search result asked for; the page opens with it unfolded. */
    var focus by rememberSaveable { mutableStateOf<StudioTarget?>(null) }
    /** Shortcuts are reached from ARC (Ovládání) and from Akce a FLOW; Back returns to whichever it was. */
    var shortcutsBack by rememberSaveable { mutableStateOf(StudioPane.Flow) }
    /** Settings before a restore from a backup file (D17); offered back until Studio closes, never stored. */
    var beforeRestore by remember { mutableStateOf<LauncherSettings?>(null) }
    // The page's scroll and open part survive a deeper page too, so Back lands on the same row (Veil 3.0 motion).
    val panes = rememberSaveableStateHolder()
    val ms = motionMs(VeilMotion.PAGE_MS)
    fun up() {
        pane = when (pane) {
            StudioPane.Shortcuts -> shortcutsBack
            StudioPane.Hidden -> StudioPane.System
            else -> { panes.removeState(pane.name); focus = null; StudioPane.Main }
        }
    }
    fun open(section: StudioSection, target: StudioTarget?) {
        if (section == StudioSection.Home) { onEditHome(); return }
        focus = target; panes.removeState(section.pane().name); pane = section.pane()
    }
    fun shortcuts(from: StudioPane) { shortcutsBack = from; pane = StudioPane.Shortcuts }
    BackHandler(enabled = pane != StudioPane.Main) { up() }
    AnimatedContent(pane, transitionSpec = {
        VeilMotion.page(if (targetState.depth >= initialState.depth) 1 else -1, ms)
    }, label = "studio-pane") { shown -> panes.SaveableStateProvider(shown.name) { when (shown) {
        StudioPane.Main -> StudioMain(model, settings, onBack, onEditHome, ::open)
        StudioPane.Look -> StudioFrame(settings, model, stringResource(R.string.look), ::up, preview = true) {
            LookPage(settings, focus, onEditHome) { model.updateSettings(it) }
        }
        StudioPane.Controls -> StudioFrame(settings, model, stringResource(R.string.studio_controls), ::up) {
            ControlsPage(model, settings, focus, onShortcuts = { shortcuts(StudioPane.Controls) })
        }
        StudioPane.Flow -> StudioFrame(settings, model, stringResource(R.string.studio_flow), ::up) {
            FlowPage(settings, onFlows, onShortcuts = { shortcuts(StudioPane.Flow) })
        }
        StudioPane.System -> StudioFrame(settings, model, stringResource(R.string.studio_system), ::up) {
            SystemPage(model, focus, beforeRestore, { beforeRestore = it }, onHidden = { pane = StudioPane.Hidden })
        }
        StudioPane.Shortcuts -> ShortcutPicker(model, settings, onBack = ::up)
        StudioPane.Hidden -> HiddenApps(model, onBack = ::up)
        StudioPane.Lens -> LensCalibration(settings.pulseLens, onSave = { lens ->
            model.updateSettings { it.copy(pulseLens = lens) }
            pane = StudioPane.Main
        }, onBack = { pane = StudioPane.Main })
    } } }
}

/**
 * One Studio page: the wallpaper at the top — with the live Home preview on Main and Vzhled — and a Veil Focus sheet
 * below it that scrolls. The header names the page and goes back.
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
            Row(Modifier.statusBarsPadding().padding(start = 8.dp, top = 4.dp, bottom = if (preview) 0.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconAction(Glyph.Back, stringResource(R.string.back), onBack, tint = homeInk)
                Text(title.uppercase(), style = InkText.label.copy(shadow = homeShadow(homeInk)), color = homeInk)
            }
            // Home as a true miniature of the screen: every size and distance at one scale, inside a phone outline.
            if (preview) {
                val shape = RoundedCornerShape(12.dp)
                Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp).size(screenW * PREVIEW_SCALE, screenH * PREVIEW_SCALE)
                    .clip(shape).border(1.dp, homeInk.copy(alpha = .35f), shape)) {
                    // The clock and date as Home draws them (font, size, colour, plate), not the old global clock style.
                    HomePreview(settings, favorites, now, homeInk, LocalWallPlaces.current[PaletteTarget.Clock], Modifier.fillMaxSize(),
                        scale = PREVIEW_SCALE, onWallpaper = true, home = settings.home.activeLayout(), entry = model::entry)
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth()
                .veilSurface(veil(VeilLevel.Focus), VeilOrigin.Top, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .verticalScroll(rememberScrollState()).navigationBarsPadding().padding(top = 8.dp, bottom = 24.dp), content = content)
        }
        // Live capsule at the real camera while PULSE is on: width and opacity change as you drag.
        if (preview && settings.pulse) Pulse(settings, preview = true)
    }
}

/** Every searchable part, in the owner's language. */
@Composable
private fun studioEntries(): List<StudioEntry> {
    val look = stringResource(R.string.look)
    val home = stringResource(R.string.studio_home)
    val controls = stringResource(R.string.studio_controls)
    val flow = stringResource(R.string.studio_flow)
    val system = stringResource(R.string.studio_system)
    return StudioTarget.entries.map { t ->
        val (label, section, kw) = when (t) {
            StudioTarget.Icons -> Triple(R.string.icons, look, R.string.studio_kw_icons)
            StudioTarget.Material -> Triple(R.string.look_material, look, R.string.studio_kw_material)
            StudioTarget.Wallpaper -> Triple(R.string.wallpaper, look, R.string.studio_kw_wallpaper)
            StudioTarget.Theme -> Triple(R.string.theme, look, R.string.studio_kw_theme)
            StudioTarget.Colors -> Triple(R.string.palette_section, look, R.string.studio_kw_colors)
            StudioTarget.EditHome -> Triple(R.string.edit_home, home, R.string.studio_kw_edit_home)
            StudioTarget.Arc -> Triple(R.string.arc_section, controls, R.string.studio_kw_arc)
            StudioTarget.Pulse -> Triple(R.string.pulse, controls, R.string.studio_kw_pulse)
            StudioTarget.Motion -> Triple(R.string.motion_section, controls, R.string.studio_kw_motion)
            StudioTarget.Flows -> Triple(R.string.flow_manage, flow, R.string.studio_kw_flows)
            StudioTarget.Shortcuts -> Triple(R.string.arc_ring_shortcuts, flow, R.string.studio_kw_shortcuts)
            StudioTarget.Hidden -> Triple(R.string.hidden_manage, system, R.string.studio_kw_hidden)
            StudioTarget.DefaultHome -> Triple(R.string.default_home_section, system, R.string.studio_kw_default_home)
            StudioTarget.Backup -> Triple(R.string.backup_title, system, R.string.studio_kw_backup)
            StudioTarget.About -> Triple(R.string.about_title, system, R.string.studio_kw_about)
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
    StudioFrame(settings, model, stringResource(R.string.studio_live), onBack, preview = true) {
        StudioSearchField(query) { query = it }
        if (query.isNotBlank()) {
            if (hits.isEmpty()) Hint(stringResource(R.string.studio_no_results))
            // A result only opens on a tap; typing never navigates.
            hits.forEach { e -> SectionRow(e.label, e.path) { onOpen(e.target.section, e.target) } }
        } else {
            SectionRow(stringResource(R.string.edit_home), stringResource(R.string.studio_home_sub, settings.home.activeLayout().name), onEditHome)
            Hairline(Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            SectionRow(stringResource(R.string.look), listOf(stringResource(themeName(settings.theme)),
                stringResource(if (settings.veilSolid) R.string.look_solid else glassName(settings.glass)),
                stringResource(wallName(settings.wallpaper.ifEmpty { null }))).joinToString(" · ")) { onOpen(StudioSection.Look, null) }
            SectionRow(stringResource(R.string.studio_controls), "ARC " + stringResource(R.string.arc_rings_count, settings.arcRings) + " · PULSE " +
                stringResource(if (everywhere) R.string.pulse_everywhere_short else R.string.pulse_home_only)) { onOpen(StudioSection.Controls, null) }
            SectionRow(stringResource(R.string.studio_flow), stringResource(R.string.studio_flow_sub, settings.aliases.size, settings.arcShortcuts.size)) {
                onOpen(StudioSection.Flow, null)
            }
            SectionRow(stringResource(R.string.studio_system), stringResource(R.string.studio_hidden_sub, hidden.size)) { onOpen(StudioSection.System, null) }
            Text(stringResource(R.string.local_only), Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = InkText.small, color = ink.faint)
        }
    }
}

/** A section or a search result: its name and, below, its state or where it lives. */
@Composable
private fun SectionRow(title: String, subtitle: String, onClick: () -> Unit) {
    val ink = LocalInk.current
    Row(Modifier.fillMaxWidth().heightIn(min = 68.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = InkText.body.copy(fontSize = 17.sp))
            Text(subtitle, style = InkText.small, color = ink.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text("  →", style = InkText.body, color = ink.muted)
    }
}

@Composable
private fun StudioSearchField(query: String, onChange: (String) -> Unit) {
    val ink = LocalInk.current
    val hint = stringResource(R.string.studio_search)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 52.dp).clip(RoundedCornerShape(26.dp))
        .background(ink.ink.copy(alpha = .07f)).padding(start = 18.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.text.BasicTextField(query, onChange, Modifier.weight(1f).semantics { contentDescription = hint },
            singleLine = true, textStyle = InkText.body.copy(color = ink.ink), cursorBrush = androidx.compose.ui.graphics.SolidColor(ink.accent),
            decorationBox = { inner -> Box { if (query.isEmpty()) Text(hint, style = InkText.body, color = ink.muted); inner() } })
        if (query.isNotEmpty()) IconAction(Glyph.Close, stringResource(R.string.studio_search_clear), { onChange("") }, tint = ink.muted)
    }
}

/** Vzhled: the editor's Look definition, unfolded at the part a search asked for. */
@Composable
private fun LookPage(settings: LauncherSettings, focus: StudioTarget?, onEditHome: () -> Unit, set: SettingsEdit) {
    val first = focus?.let { lookFoldTitle(it) }
    val group = remember { FoldGroup().also { it.open = first } }
    CompositionLocalProvider(LocalFoldGroup provides group) {
        Column(Modifier.padding(horizontal = 14.dp)) { LookSettings(settings, set) }
    }
    Hint(stringResource(R.string.studio_look_editor))
    NavRow(stringResource(R.string.edit_home), null, onEditHome)
}

/** The Studio fold a search target opens on its page. */
private fun StudioTarget?.partTitle(): Int? = when (this) {
    StudioTarget.Arc -> R.string.arc_section
    StudioTarget.Pulse -> R.string.pulse
    StudioTarget.Motion -> R.string.motion_section
    StudioTarget.Backup -> R.string.backup_title
    StudioTarget.About -> R.string.about_title
    else -> null
}

/** Ovládání: ARC, PULSE, motion and touch; one part open at a time. */
@Composable
private fun ControlsPage(model: HomeViewModel, settings: LauncherSettings, focus: StudioTarget?, onShortcuts: () -> Unit) {
    val ink = LocalInk.current
    val arcApps by model.arcApps.collectAsStateWithLifecycle()
    var open by rememberSaveable { mutableStateOf(focus.partTitle()) }
    fun set(transform: (LauncherSettings) -> LauncherSettings) = model.updateSettings(transform)
    @Composable
    fun Part(title: Int, summary: String?, content: @Composable () -> Unit) =
        StudioFold(stringResource(title), summary, open == title, { open = if (it) title else null }, content)

    Part(R.string.arc_section, stringResource(R.string.arc_rings_count, settings.arcRings)) {
        Hint(stringResource(R.string.arc_explain))
        ChoiceRow(stringResource(R.string.arc_rings)) {
            Segments(listOf(1 to "1", 2 to "2", 3 to "3"), settings.arcRings, { v -> set { it.copy(arcRings = v) } })
        }
        Kicker("1 · " + stringResource(R.string.arc_ring_apps) + " · ${settings.arc.size}/${LauncherSettings.ARC_SLOTS}",
            Modifier.padding(start = 24.dp, top = 14.dp, bottom = 4.dp), color = ink.muted)
        val ringApps = arcApps.filterNotNull()
        if (ringApps.isEmpty()) Hint(stringResource(R.string.arc_apps_hint))
        ringApps.forEachIndexed { index, app -> OrderedAppRow(app, index, onUp = null, onRemove = { model.toggleArc(app) }) }
        // Rings beyond the chosen count are never drawn, so their contents are only shown when in use.
        if (settings.arcRings >= 2) {
            Kicker("2 · " + stringResource(R.string.flow) + " · ${settings.arcFlows.size}/${LauncherSettings.ARC_FLOW_SLOTS}",
                Modifier.padding(start = 24.dp, top = 14.dp, bottom = 4.dp), color = ink.muted)
            if (settings.aliases.isEmpty()) Hint(stringResource(R.string.flow_empty))
            settings.aliases.forEach { alias ->
                val on = alias.id in settings.arcFlows
                val full = settings.arcFlows.size >= LauncherSettings.ARC_FLOW_SLOTS
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).toggleable(on, enabled = on || !full, role = Role.Switch) {
                    model.toggleArcFlow(alias.id, it)
                }.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(alias.name, Modifier.weight(1f), style = InkText.item.copy(fontStyle = FontStyle.Italic), maxLines = 1,
                        overflow = TextOverflow.Ellipsis, color = if (on || !full) ink.ink else ink.faint)
                    RingToggle(on)
                }
            }
        }
        if (settings.arcRings >= 3) {
            Kicker("3 · " + stringResource(R.string.arc_ring_shortcuts) + " · ${settings.arcShortcuts.size}/${LauncherSettings.ARC_SHORTCUT_SLOTS}",
                Modifier.padding(start = 24.dp, top = 14.dp, bottom = 4.dp), color = ink.muted)
            NavRow(stringResource(R.string.shortcuts_pick), null, onShortcuts)
        }
    }

    // D9: the switch only mirrors the service; Android settings are where it is turned on or off.
    val everywhere by PulseAccessibilityService.running.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Part(R.string.pulse, stringResource(if (everywhere) R.string.pulse_everywhere_short else R.string.pulse_home_only)) {
        Hint(stringResource(R.string.studio_pulse_moved))
        ToggleRow(stringResource(R.string.pulse_everywhere), everywhere, { PulseAccessibilityService.openSettings(context) },
            detail = stringResource(R.string.pulse_everywhere_detail))
    }

    Part(R.string.motion_section, null) {
        Stacked(stringResource(R.string.motion)) {
            Segments(listOf(Motion.Calm to stringResource(R.string.motion_calm), Motion.Brisk to stringResource(R.string.motion_brisk),
                Motion.Off to stringResource(R.string.off)), settings.motion, { v -> set { it.copy(motion = v) } })
        }
        Stacked(stringResource(R.string.haptics)) {
            Segments(listOf(Haptics.Soft to stringResource(R.string.haptics_soft), Haptics.Strong to stringResource(R.string.haptics_strong),
                Haptics.Off to stringResource(R.string.off)), settings.haptics, { v -> set { it.copy(haptics = v) } })
        }
    }
}

/** Akce a FLOW: the commands and the app shortcuts ARC can hold. */
@Composable
private fun FlowPage(settings: LauncherSettings, onFlows: () -> Unit, onShortcuts: () -> Unit) {
    NavRow(stringResource(R.string.flow_manage), settings.aliases.size.toString(), onFlows)
    NavRow(stringResource(R.string.shortcuts_pick), "${settings.arcShortcuts.size}/${LauncherSettings.ARC_SHORTCUT_SLOTS}", onShortcuts)
}

/** Systém: default home, hidden apps, the settings backup (D17), about and licences. */
@Composable
private fun SystemPage(model: HomeViewModel, focus: StudioTarget?, beforeRestore: LauncherSettings?,
    onRestored: (LauncherSettings?) -> Unit, onHidden: () -> Unit) {
    val hidden by model.hiddenApps.collectAsStateWithLifecycle()
    val role = rememberHomeRole()
    var open by rememberSaveable { mutableStateOf(focus.partTitle()) }
    // Only while Intelia is not the default home is there something to do here; it stays in sight.
    if (!role.held) {
        Section(R.string.default_home_section)
        Hint(stringResource(R.string.default_home_why))
        Capsule(stringResource(R.string.default_home), role.request, Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
    }
    NavRow(stringResource(R.string.hidden_manage), hidden.size.toString(), onHidden)
    StudioFold(stringResource(R.string.backup_title), null, open == R.string.backup_title, { open = if (it) R.string.backup_title else null }) {
        BackupPart(model, beforeRestore, onRestored)
    }
    StudioFold(stringResource(R.string.about_title), null, open == R.string.about_title, { open = if (it) R.string.about_title else null }) {
        if (role.held) Hint(stringResource(R.string.default_home_active))
        About()
    }
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

/**
 * One Studio section that opens in place; while open it sits on an accent-tinted plate with an accent title, so it
 * is clear which part is being changed. The caller keeps one open at a time.
 */
@Composable
private fun StudioFold(title: String, summary: String?, open: Boolean, onToggle: (Boolean) -> Unit, content: @Composable () -> Unit) {
    val ink = LocalInk.current
    val turn by androidx.compose.animation.core.animateFloatAsState(if (open) 45f else 0f, label = "studioFold")
    val shape = RoundedCornerShape(20.dp)
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp).clip(shape)
        .then(if (open) Modifier.background(ink.accent.copy(alpha = .09f)).border(1.dp, ink.accent.copy(alpha = .4f), shape) else Modifier)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(open, role = Role.Button, onValueChange = onToggle).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(title.uppercase(), Modifier.weight(1f), style = InkText.label, color = if (open) ink.accent else ink.ink, maxLines = 1)
            if (summary != null && !open) Text(summary, Modifier.padding(horizontal = 10.dp), style = InkText.small, color = ink.muted, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text("+", Modifier.graphicsLayer { rotationZ = turn }, style = InkText.item, color = if (open) ink.accent else ink.muted)
        }
        androidx.compose.animation.AnimatedVisibility(open, enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()) {
            Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) { content() }
        }
    }
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
private fun ShortcutPicker(model: HomeViewModel, settings: LauncherSettings, onBack: () -> Unit) {
    val ink = LocalInk.current
    val shortcuts by model.shortcuts.collectAsStateWithLifecycle()
    val apps by model.allApps.collectAsStateWithLifecycle()
    val role = rememberHomeRole()
    SystemBarIcons(darkIcons = !ink.dark)
    val labels = remember(apps) { apps.filter { it.mainProfile }.associate { it.component.packageName to it.label } }
    val full = settings.arcShortcuts.size >= LauncherSettings.ARC_SHORTCUT_SLOTS
    InkPage(stringResource(R.string.studio), onBack, kicker = "ARC · 3", title = stringResource(R.string.arc_ring_shortcuts)) {
        Text(stringResource(R.string.shortcuts_explain, settings.arcShortcuts.size, LauncherSettings.ARC_SHORTCUT_SLOTS),
            Modifier.padding(horizontal = 24.dp, vertical = 6.dp), style = InkText.small, color = ink.muted)
        if (shortcuts.isEmpty()) {
            Hint(stringResource(if (role.held) R.string.shortcuts_none else R.string.shortcuts_need_home))
            if (!role.held) Capsule(stringResource(R.string.default_home), role.request, Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            return@InkPage
        }
        val groups = remember(shortcuts) { shortcuts.groupBy { it.packageName }.toList() }
        LazyColumn(Modifier.weight(1f)) {
            groups.forEach { (pkg, list) ->
                item(key = "pkg:$pkg") {
                    Kicker(labels[pkg] ?: pkg, Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 4.dp), color = ink.accent)
                    Hairline(Modifier.padding(horizontal = 24.dp))
                }
                items(list, key = { it.key }) { shortcut ->
                    val on = shortcut.key in settings.arcShortcuts
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).toggleable(on, enabled = on || !full, role = Role.Switch) {
                        model.toggleArcShortcut(shortcut.key)
                    }.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        ShortcutIcon(shortcut, Modifier.size(28.dp))
                        Text(shortcut.label, Modifier.padding(start = 14.dp).weight(1f), style = InkText.body,
                            color = if (on || !full) ink.ink else ink.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        RingToggle(on)
                    }
                }
            }
        }
    }
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
                    else -> PreviewSlot(if (it.kind == ItemKind.App) entry(it.target) else null, ink, scale, 32f)
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

/** Studio's Home miniature: the screen at this scale, so the clock, LENS row and gaps keep their real proportions. */
private const val PREVIEW_SCALE = .26f

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

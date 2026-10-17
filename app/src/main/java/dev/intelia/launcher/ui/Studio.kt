package dev.intelia.launcher.ui

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

private enum class StudioPane { Main, Shortcuts, Hidden, Lens }

/**
 * Studio replaces the settings sheet: a live preview drawn straight on the wallpaper at the top,
 * every choice on paper below it. Changes apply immediately and persist through SettingsStore.
 */
@Composable
fun Studio(model: HomeViewModel, settings: LauncherSettings, onBack: () -> Unit, onFlows: () -> Unit, onEditHome: () -> Unit,
    lensFirst: Boolean = false) {
    var pane by rememberSaveable { mutableStateOf(if (lensFirst) StudioPane.Lens else StudioPane.Main) }
    // Hoisted so coming back from a sub-page lands where you left the list.
    val scroll = rememberScrollState()
    // The open section survives a detail page too, so Back lands on the same row (Veil 3.0 motion).
    val panes = rememberSaveableStateHolder()
    val ms = motionMs(VeilMotion.PAGE_MS)
    BackHandler(enabled = pane != StudioPane.Main) { pane = StudioPane.Main }
    AnimatedContent(pane, transitionSpec = { VeilMotion.page(if (targetState == StudioPane.Main) -1 else 1, ms) },
        label = "studio-pane") { shown -> panes.SaveableStateProvider(shown.name) { when (shown) {
        StudioPane.Main -> StudioMain(model, settings, scroll, onBack, onFlows, onEditHome, onPane = { pane = it })
        StudioPane.Shortcuts -> ShortcutPicker(model, settings, onBack = { pane = StudioPane.Main })
        StudioPane.Hidden -> HiddenApps(model, onBack = { pane = StudioPane.Main })
        StudioPane.Lens -> LensCalibration(settings.pulseLens, onSave = { lens ->
            model.updateSettings { it.copy(pulseLens = lens) }
            pane = StudioPane.Main
        }, onBack = { pane = StudioPane.Main })
    } } }
}

@Composable
private fun StudioMain(model: HomeViewModel, settings: LauncherSettings, scroll: ScrollState, onBack: () -> Unit, onFlows: () -> Unit, onEditHome: () -> Unit,
    onPane: (StudioPane) -> Unit) {
    val ink = LocalInk.current
    val homeInk = LocalHomeInk.current
    val favorites by model.favoriteApps.collectAsStateWithLifecycle()
    val arcApps by model.arcApps.collectAsStateWithLifecycle()
    val hidden by model.hiddenApps.collectAsStateWithLifecycle()
    val now by rememberNow()
    val role = rememberHomeRole()
    fun set(transform: (LauncherSettings) -> LauncherSettings) = model.updateSettings(transform)
    SystemBarIcons(darkIcons = homeInk.luminance() < .5f)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Live preview on the real wallpaper: nothing is drawn behind it.
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                // The clock and date as Home draws them (font, size, colour, plate), not the old global clock style.
                val items = settings.home.activeLayout().items
                HomePreview(settings, favorites, now, homeInk, LocalWallPlaces.current[PaletteTarget.Clock], Modifier.fillMaxSize().statusBarsPadding(),
                    scale = .78f, onWallpaper = true, home = HomeText(items.firstOrNull { it.kind == ItemKind.Clock },
                        items.firstOrNull { it.kind == ItemKind.Date }))
                Row(Modifier.statusBarsPadding().padding(start = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconAction(Glyph.Back, stringResource(R.string.home), onBack, tint = homeInk)
                    Text(stringResource(R.string.studio_live).uppercase(), style = InkText.label.copy(shadow = homeShadow(homeInk)), color = homeInk)
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth()
                .veilSurface(veil(VeilLevel.Focus), VeilOrigin.Top, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .verticalScroll(scroll).navigationBarsPadding().padding(bottom = 24.dp)) {
                // Everything that changes how Home looks is edited on Home itself (owner, 2026-10-16); Studio keeps behaviour.
                Hint(stringResource(R.string.studio_look_moved))
                NavRow(stringResource(R.string.edit_home), null, onEditHome)
                // One section open at a time (owner, 2026-10-17): each names its state while closed.
                var open by rememberSaveable { mutableStateOf<Int?>(null) }
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
                        NavRow(stringResource(R.string.shortcuts_pick), null, { onPane(StudioPane.Shortcuts) })
                    }
                }
                NavRow(stringResource(R.string.flow_manage), settings.aliases.size.toString(), onFlows)

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

                // Only while Intelia is not the default home is there something to do here; it stays in sight.
                if (!role.held) {
                    Section(R.string.default_home_section)
                    Hint(stringResource(R.string.default_home_why))
                    Capsule(stringResource(R.string.default_home), role.request, Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                }

                NavRow(stringResource(R.string.hidden_manage), hidden.size.toString(), { onPane(StudioPane.Hidden) })

                Part(R.string.about_title, null) {
                    if (role.held) Hint(stringResource(R.string.default_home_active))
                    About()
                }
            }
        }
        // Live capsule at the real camera while PULSE is on: width and opacity change as you drag.
        if (settings.pulse) Pulse(settings, preview = true)
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
 * wallpaper) and in onboarding (on paper, with plain rings for apps not chosen yet).
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
    home: HomeText? = null,
) {
    val custom = settings.layout == HomeLayout.Custom
    fun d(v: Float): Dp = (v * scale).dp
    BoxWithConstraints(modifier.semantics(mergeDescendants = true) { }) {
        val textRoom = maxHeight - d(64f) - d(84f)
        Column(Modifier.fillMaxWidth().padding(top = d(64f), start = d(28f), end = d(28f)),
            horizontalAlignment = if (custom) Alignment.Start else Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(d(8f))) {
            // A big or stacked clock is shrunk as a whole to the room above LENS, so it never runs into the row.
            if (home != null) Box(Modifier.fitHeight(textRoom, if (custom) 0f else .5f)) {
                HomeClockPreview(home, settings, now, ink, accent, scale)
            }
            else if (settings.showClock) SplitClock(now.toLocalTime(), settings.clockStyle, ink, accent, (84f * scale).sp, shadow = onWallpaper)
            if (home == null && settings.showDate) Text(dateLine(now), style = InkText.label.copy(fontSize = (11f * scale).coerceAtLeast(8f).sp,
                letterSpacing = (2.2f * scale).sp, shadow = if (onWallpaper) homeShadow(ink) else null), color = ink.copy(alpha = .8f), maxLines = 1)
        }
        if (custom) Column(Modifier.align(Alignment.BottomStart).padding(start = d(28f), bottom = d(92f))) {
            val names = favorites.map { it.label }.ifEmpty { listOf("—", "—", "—") }.take(4)
            names.forEach {
                Text(it, style = InkText.item.copy(fontSize = (26f * scale).sp, lineHeight = (30f * scale).sp,
                    shadow = if (onWallpaper) homeShadow(ink) else null), color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = d(26f)), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d(14f))) {
            val around = settings.layout == HomeLayout.Essentials
            val slots: List<AppEntry?> = if (around) List(4) { favorites.getOrNull(it) } else emptyList()
            slots.take(2).forEach { PreviewSlot(it, ink, scale) }
            LensMark(ink, ring = d(44f))
            slots.drop(2).forEach { PreviewSlot(it, ink, scale) }
        }
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

/** Home's clock and date items for the Studio preview; either may be missing (hidden on Home). */
data class HomeText(val clock: HomeItem?, val date: HomeItem?)

/** The clock with its bound date, drawn with the item's own face, size, weight, colour, opacity and plate, scaled. */
@Composable
private fun HomeClockPreview(home: HomeText, settings: LauncherSettings, now: LocalDateTime, ink: Color, accent: Color, scale: Float) {
    val clock = home.clock
    val date = home.date
    val bound = date?.takeIf { it.anchor.type == AnchorType.Bound }
    // A free date has its own place on Home; in the miniature it simply follows the clock.
    val free = date?.takeIf { it.anchor.type != AnchorType.Bound }
    fun scaled(d: HomeItem) = d.copy(sizeDp = (d.dateSp() * scale).roundToInt().coerceAtLeast(8),
        look = d.look.copy(padX = d.look.padX * scale, padY = d.look.padY * scale))
    val plate = !clock?.background.isNullOrEmpty()
    val clockInk = if (plate) LocalCrystal.current.fg else ink
    Column(verticalArrangement = Arrangement.spacedBy((10f * scale).dp)) {
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
        if (free != null) DateText(scaled(free), now, ink, accent, Modifier)
    }
}

@Composable
private fun PreviewSlot(app: AppEntry?, ink: Color, scale: Float) {
    val size = (30f * scale).dp
    if (app != null) AppIcon(app, Modifier.size(size), tint = ink)
    else Canvas(Modifier.size(size)) { drawCircle(ink.copy(alpha = .5f), this.size.minDimension / 3f, style = Stroke(1.dp.toPx())) }
}

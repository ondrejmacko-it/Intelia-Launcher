package dev.intelia.launcher.ui

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import dev.intelia.launcher.data.Accent
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.ClockStyle
import dev.intelia.launcher.data.Haptics
import dev.intelia.launcher.data.HomeLayout
import dev.intelia.launcher.data.IconStyle
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.Motion
import dev.intelia.launcher.data.Reach
import dev.intelia.launcher.data.ThemeMode
import java.time.LocalDateTime

private enum class StudioPane { Main, Shortcuts, Hidden, Lens }

/**
 * Studio replaces the settings sheet: a live preview drawn straight on the wallpaper at the top,
 * every choice on paper below it. Changes apply immediately and persist through SettingsStore.
 */
@Composable
fun Studio(model: HomeViewModel, settings: LauncherSettings, onBack: () -> Unit, onFlows: () -> Unit) {
    var pane by rememberSaveable { mutableStateOf(StudioPane.Main) }
    // Hoisted so coming back from a sub-page lands where you left the list.
    val scroll = rememberScrollState()
    BackHandler(enabled = pane != StudioPane.Main) { pane = StudioPane.Main }
    when (pane) {
        StudioPane.Main -> StudioMain(model, settings, scroll, onBack, onFlows, onPane = { pane = it })
        StudioPane.Shortcuts -> ShortcutPicker(model, settings, onBack = { pane = StudioPane.Main })
        StudioPane.Hidden -> HiddenApps(model, onBack = { pane = StudioPane.Main })
        StudioPane.Lens -> LensCalibration(settings.pulseLens, onSave = { lens ->
            model.updateSettings { it.copy(pulseLens = lens) }
            pane = StudioPane.Main
        }, onBack = { pane = StudioPane.Main })
    }
}

@Composable
private fun StudioMain(model: HomeViewModel, settings: LauncherSettings, scroll: ScrollState, onBack: () -> Unit, onFlows: () -> Unit, onPane: (StudioPane) -> Unit) {
    val ink = LocalInk.current
    val homeInk = LocalHomeInk.current
    val favorites by model.favoriteApps.collectAsStateWithLifecycle()
    val arcApps by model.arcApps.collectAsStateWithLifecycle()
    val hidden by model.hiddenApps.collectAsStateWithLifecycle()
    val now by rememberNow()
    val role = rememberHomeRole()
    fun set(transform: (LauncherSettings) -> LauncherSettings) = model.updateSettings(transform)
    SystemBarIcons(darkIcons = homeInk.luminance() < .5f)

    Column(Modifier.fillMaxSize()) {
        // Live preview on the real wallpaper: nothing is drawn behind it.
        Box(Modifier.fillMaxWidth().height(300.dp)) {
            HomePreview(settings, favorites, now, homeInk, ink.accent, Modifier.fillMaxSize().statusBarsPadding(), scale = .78f, onWallpaper = true)
            Row(Modifier.statusBarsPadding().padding(start = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconAction(Glyph.Back, stringResource(R.string.home), onBack, tint = homeInk)
                Text(stringResource(R.string.studio_live).uppercase(), style = InkText.label.copy(shadow = homeShadow(homeInk)), color = homeInk)
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(ink.paper)
            .verticalScroll(scroll).navigationBarsPadding().padding(bottom = 24.dp)) {
            Section(R.string.appearance)
            Stacked(stringResource(R.string.clock_font)) {
                Segments(listOf(ClockStyle.Serif to stringResource(R.string.clock_serif), ClockStyle.Grotesk to stringResource(R.string.clock_grotesk),
                    ClockStyle.Mono to stringResource(R.string.clock_mono)), settings.clockStyle, { v -> set { it.copy(clockStyle = v) } })
            }
            Stacked(stringResource(R.string.theme)) {
                Segments(listOf(ThemeMode.System to stringResource(R.string.theme_system), ThemeMode.Light to stringResource(R.string.theme_light),
                    ThemeMode.Dark to stringResource(R.string.theme_dark)), settings.theme, { v -> set { it.copy(theme = v) } })
            }
            Stacked(stringResource(R.string.accent)) { Swatches(settings.accent) { v -> set { it.copy(accent = v) } } }
            Stacked(stringResource(R.string.icons)) {
                Segments(listOf(IconStyle.System to stringResource(R.string.icons_system), IconStyle.Ink to stringResource(R.string.icons_ink)),
                    settings.iconStyle, { v -> set { it.copy(iconStyle = v) } })
            }
            ToggleRow(stringResource(R.string.show_clock), settings.showClock, { v -> set { it.copy(showClock = v) } })
            ToggleRow(stringResource(R.string.show_date), settings.showDate, { v -> set { it.copy(showDate = v) } })

            Section(R.string.home_section)
            Stacked(stringResource(R.string.layout)) {
                Segments(listOf(HomeLayout.Pure to stringResource(R.string.layout_pure), HomeLayout.Essentials to stringResource(R.string.layout_essentials),
                    HomeLayout.Custom to stringResource(R.string.layout_custom)), settings.layout, { v -> set { it.copy(layout = v) } })
            }
            Stacked(stringResource(R.string.reach)) {
                Segments(listOf(Reach.Left to stringResource(R.string.reach_left), Reach.Center to stringResource(R.string.reach_center),
                    Reach.Right to stringResource(R.string.reach_right)), settings.reach, { v -> set { it.copy(reach = v) } })
            }
            Kicker(stringResource(R.string.favorites_count, favorites.size, LauncherSettings.MAX_FAVORITES), Modifier.padding(start = 24.dp, top = 18.dp, bottom = 4.dp),
                color = ink.muted)
            if (favorites.isEmpty()) Hint(stringResource(R.string.favorites_hint))
            favorites.forEachIndexed { index, app ->
                OrderedAppRow(app, index, onUp = { model.moveFavorite(app.key, -1) }, onRemove = { model.toggleFavorite(app) })
            }

            Section(R.string.arc_section)
            Hint(stringResource(R.string.arc_explain))
            ChoiceRow(stringResource(R.string.arc_rings)) {
                Segments(listOf(1 to "1", 2 to "2", 3 to "3"), settings.arcRings, { v -> set { it.copy(arcRings = v) } })
            }
            Kicker("1 · " + stringResource(R.string.arc_ring_apps) + " · ${settings.arc.size}/${LauncherSettings.ARC_SLOTS}",
                Modifier.padding(start = 24.dp, top = 14.dp, bottom = 4.dp), color = ink.muted)
            val ringApps = arcApps.filterNotNull()
            if (ringApps.isEmpty()) Hint(stringResource(R.string.arc_apps_hint))
            ringApps.forEachIndexed { index, app -> OrderedAppRow(app, index, onUp = null, onRemove = { model.toggleArc(app) }) }
            Kicker("2 · " + stringResource(R.string.flow) + " · ${settings.arcFlows.size}/${LauncherSettings.ARC_FLOW_SLOTS}",
                Modifier.padding(start = 24.dp, top = 14.dp, bottom = 4.dp), color = if (settings.arcRings >= 2) ink.muted else ink.faint)
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
            NavRow(stringResource(R.string.flow_manage), null, onFlows)
            Kicker("3 · " + stringResource(R.string.arc_ring_shortcuts) + " · ${settings.arcShortcuts.size}/${LauncherSettings.ARC_SHORTCUT_SLOTS}",
                Modifier.padding(start = 24.dp, top = 14.dp, bottom = 4.dp), color = if (settings.arcRings >= 3) ink.muted else ink.faint)
            NavRow(stringResource(R.string.shortcuts_pick), null, { onPane(StudioPane.Shortcuts) })

            Section(R.string.pulse)
            ToggleRow(stringResource(R.string.pulse_toggle), settings.pulse, { v -> set { it.copy(pulse = v) } },
                detail = stringResource(R.string.pulse_detail))
            if (settings.pulse) {
                NavRow(stringResource(R.string.pulse_lens), stringResource(if (settings.pulseLens == null) R.string.pulse_lens_auto_short
                    else R.string.pulse_lens_custom), { onPane(StudioPane.Lens) })
                ToggleRow(stringResource(R.string.pulse_echo), settings.pulseEcho, { v -> set { it.copy(pulseEcho = v) } },
                    detail = stringResource(R.string.pulse_echo_detail))
                ToggleRow(stringResource(R.string.pulse_alarm), settings.pulseAlarm, { v -> set { it.copy(pulseAlarm = v) } })
                if (settings.pulseAlarm) Stacked(stringResource(R.string.pulse_alarm_window)) {
                    Segments(listOf(3 to "3 h", 6 to "6 h", 12 to "12 h", 24 to "24 h"), settings.pulseAlarmHours,
                        { v -> set { it.copy(pulseAlarmHours = v) } })
                }
                ToggleRow(stringResource(R.string.pulse_charging), settings.pulseCharging, { v -> set { it.copy(pulseCharging = v) } },
                    detail = stringResource(R.string.pulse_charging_detail))
            }

            Section(R.string.motion_section)
            Stacked(stringResource(R.string.motion)) {
                Segments(listOf(Motion.Calm to stringResource(R.string.motion_calm), Motion.Brisk to stringResource(R.string.motion_brisk),
                    Motion.Off to stringResource(R.string.off)), settings.motion, { v -> set { it.copy(motion = v) } })
            }
            Stacked(stringResource(R.string.haptics)) {
                Segments(listOf(Haptics.Soft to stringResource(R.string.haptics_soft), Haptics.Strong to stringResource(R.string.haptics_strong),
                    Haptics.Off to stringResource(R.string.off)), settings.haptics, { v -> set { it.copy(haptics = v) } })
            }

            Section(R.string.default_home_section)
            if (role.held) Hint(stringResource(R.string.default_home_active))
            else {
                Hint(stringResource(R.string.default_home_why))
                Capsule(stringResource(R.string.default_home), role.request, Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            }

            Section(R.string.hidden_apps)
            NavRow(stringResource(R.string.hidden_manage), hidden.size.toString(), { onPane(StudioPane.Hidden) })

            Section(R.string.about_title)
            About()
        }
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
private fun Swatches(selected: Accent, onSelect: (Accent) -> Unit) {
    val ink = LocalInk.current
    val dark = ink.dark
    val options = buildList {
        add(Accent.Terracotta to R.string.accent_terracotta)
        add(Accent.Sage to R.string.accent_sage)
        add(Accent.Slate to R.string.accent_slate)
        add(Accent.Mono to R.string.accent_mono)
        if (Build.VERSION.SDK_INT >= 31) add(Accent.Dynamic to R.string.accent_dynamic)
    }
    Row(Modifier.selectableGroup().padding(top = 8.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { (accent, label) ->
            val color = swatchColor(accent, dark)
            val on = accent == selected
            val name = stringResource(label)
            Box(Modifier.size(44.dp).clip(CircleShape).selectable(on, role = Role.RadioButton) { onSelect(accent) }
                .semantics { contentDescription = name }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(32.dp).border(if (on) 2.dp else 0.dp, if (on) color else Color.Transparent, CircleShape).padding(4.dp)
                    .clip(CircleShape).background(color))
            }
        }
    }
}

@Composable
private fun swatchColor(accent: Accent, dark: Boolean): Color = when (accent) {
    Accent.Terracotta -> if (dark) Color(0xFFE39A78) else Color(0xFFA84B29)
    Accent.Sage -> if (dark) Color(0xFFA9C9B6) else Color(0xFF3F6B57)
    Accent.Slate -> if (dark) Color(0xFFAFC3E6) else Color(0xFF3C5A86)
    Accent.Mono -> LocalInk.current.ink
    Accent.Dynamic -> if (Build.VERSION.SDK_INT >= 31) {
        val context = LocalContext.current
        if (dark) androidx.compose.material3.dynamicDarkColorScheme(context).primary else androidx.compose.material3.dynamicLightColorScheme(context).primary
    } else LocalInk.current.accent
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
) {
    val custom = settings.layout == HomeLayout.Custom
    fun d(v: Float): Dp = (v * scale).dp
    BoxWithConstraints(modifier.semantics(mergeDescendants = true) { }) {
        Column(Modifier.fillMaxWidth().padding(top = d(64f), start = d(28f), end = d(28f)),
            horizontalAlignment = if (custom) Alignment.Start else Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(d(8f))) {
            if (settings.showClock) SplitClock(now.toLocalTime(), settings.clockStyle, ink, accent, (84f * scale).sp, shadow = onWallpaper)
            if (settings.showDate) Text(dateLine(now), style = InkText.label.copy(fontSize = (11f * scale).coerceAtLeast(8f).sp,
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
            LensMark(ink, accent, ring = d(26f))
            slots.drop(2).forEach { PreviewSlot(it, ink, scale) }
        }
    }
}

@Composable
private fun PreviewSlot(app: AppEntry?, ink: Color, scale: Float) {
    val size = (30f * scale).dp
    if (app != null) AppIcon(app, Modifier.size(size), tint = ink)
    else Canvas(Modifier.size(size)) { drawCircle(ink.copy(alpha = .5f), this.size.minDimension / 3f, style = Stroke(1.dp.toPx())) }
}

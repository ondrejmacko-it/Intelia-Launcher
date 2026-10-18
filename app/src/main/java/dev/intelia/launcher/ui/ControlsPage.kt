package dev.intelia.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.intelia.launcher.R
import dev.intelia.launcher.action.Action
import dev.intelia.launcher.action.Controls
import dev.intelia.launcher.action.Gesture
import dev.intelia.launcher.action.Trigger
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.search.AppSearch

/** One line of [ControlsPage]: a bindable gesture, or (with [fixed]) a pillar gesture shown locked with what it does. */
private class ControlLine(val name: String, val trigger: Trigger? = null, val fixed: String? = null)

/** A surface of [ControlsPage]: its lines and, for keys, the code the lines belong to. */
private class ControlGroup(val id: String, val title: String, val lines: List<ControlLine>, val hint: String? = null, val key: Int? = null)

/**
 * Studio › Ovládání (M5): every gesture of every surface in one place, grouped by where it happens (PULSE capsule,
 * Home, LENS, ARC, hardware keys), each group foldable with how many gestures are bound. A line is gesture → action:
 * tap picks with the shared [ActionPicker], the cross unbinds. Pillar gestures are shown locked, never rebindable.
 * The search on top filters gestures, surfaces and bound actions alike ("svítilna" → everywhere it is). Nothing runs
 * on this page.
 */
@Composable
internal fun ControlsPage(model: HomeViewModel, settings: LauncherSettings, onArc: () -> Unit, onKeys: () -> Unit) {
    val ink = LocalInk.current
    val context = LocalContext.current
    val apps by model.visibleApps.collectAsStateWithLifecycle()
    val all by model.allApps.collectAsStateWithLifecycle()
    val shortcuts by model.shortcuts.collectAsStateWithLifecycle()
    val service by PulseAccessibilityService.running.collectAsStateWithLifecycle()
    val role = rememberHomeRole()
    val host = rememberActionHost(model) { }
    val controls = settings.controls
    // The running PULSE service follows key bindings and the D20 switch at once.
    LaunchedEffect(controls.keys, controls.keysEverywhere, controls.bindings) { PulseAccessibilityService.refreshKeys() }
    var picking by remember { mutableStateOf<Trigger?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var folded by rememberSaveable { mutableStateOf(listOf<String>()) }

    fun entry(a: Action): ArcEntry? = when (a) {
        is Action.App -> apps.firstOrNull { it.key == a.component }?.let { ArcEntry(a, app = it) }
        is Action.Flow -> settings.aliases.firstOrNull { it.id == a.id }?.let { ArcEntry(a, flow = it) }
        is Action.Shortcut -> shortcuts.firstOrNull { it.packageName == a.pkg && it.id == a.id }?.let { ArcEntry(a, shortcut = it) }
        else -> ArcEntry(a)
    }

    val groups = controlGroups(settings)
    val fq = AppSearch.fold(query.trim())
    val labels = groups.flatMap { it.lines }.mapNotNull { l -> l.trigger?.let { t -> controls.bound(t)?.let(::entry)?.let { t to slotLabel(it) } } }.toMap()
    fun matches(g: ControlGroup, l: ControlLine) = fq.isEmpty() ||
        listOfNotNull(g.title, l.name, l.fixed, l.trigger?.let(labels::get)).any { AppSearch.fold(it).contains(fq) }

    PickerFilter(query, stringResource(R.string.controls_find)) { query = it }
    Hint(stringResource(R.string.controls_hint))
    var anything = false
    groups.forEach { g ->
        val lines = g.lines.filter { matches(g, it) }
        if (lines.isEmpty() && fq.isNotEmpty()) return@forEach
        anything = true
        val count = g.lines.count { l -> l.trigger?.let(controls::bound) != null }
        val open = fq.isNotEmpty() || g.id !in folded
        GroupHeader(g.title, count, open) { folded = if (g.id in folded) folded - g.id else folded + g.id }
        if (!open) return@forEach
        g.hint?.let { Hint(it) }
        lines.forEach { l ->
            if (l.fixed != null) FixedLine(l.name, l.fixed)
            else l.trigger?.let { t -> BoundLine(l.name, t, controls, ::entry, { picking = t }) { model.editControls { it.bind(t, null) } } }
        }
        when (g.id) {
            ARC -> NavRow(stringResource(R.string.controls_arc_sets), stringResource(R.string.controls_arc_count,
                controls.arcSets.sumOf { s -> s.slots.count { it != null } }), onArc)
            KEYS -> {
                ToggleRow(stringResource(R.string.keys_everywhere), controls.keysEverywhere && service,
                    { on -> if (on && !service) PulseAccessibilityService.openSettings(context) else model.editControls { it.copy(keysEverywhere = on) } },
                    detail = stringResource(if (service) R.string.keys_everywhere_detail else R.string.keys_everywhere_needs_service))
                NavRow(stringResource(R.string.keys_add), null, onKeys)
            }
        }
        g.key?.let { code ->
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.keys_forget_hint), Modifier.weight(1f), style = InkText.small, color = ink.faint)
                IconAction(Glyph.Close, stringResource(R.string.keys_forget), { model.editControls { it.withoutKey(code) } }, tint = ink.muted)
            }
        }
    }
    if (!anything) Hint(stringResource(R.string.pick_none))

    picking?.let { trigger ->
        val names = remember(all) { all.filter { it.mainProfile }.associate { it.component.packageName to it.label } }
        val outside = trigger is Trigger.Pulse || trigger is Trigger.Key
        ActionPicker(stringResource(R.string.pulse_gesture_choose),
            apps = apps.sortedBy { it.label.lowercase() }, flows = settings.aliases, shortcuts = shortcuts, system = SystemActions,
            host = host, appNames = names,
            shortcutsNote = stringResource(if (role.held) R.string.shortcuts_none else R.string.shortcuts_need_home),
            used = controls.bindings.map { it.action } + controls.arcSets.flatMap { it.slots.filterNotNull() },
            showReach = outside,
            onPick = { action -> model.editControls { it.bind(trigger, action) }; picking = null },
            onDismiss = { picking = null })
    }
}

private const val PULSE = "pulse"
private const val HOME = "home"
private const val LENS = "lens"
private const val ARC = "arc"
private const val KEYS = "keys"

@Composable
private fun controlGroups(settings: LauncherSettings): List<ControlGroup> {
    val tap = stringResource(R.string.pulse_gesture_tap)
    val double = stringResource(R.string.pulse_gesture_double)
    val hold = stringResource(R.string.pulse_gesture_hold)
    val left = stringResource(R.string.gesture_left)
    val right = stringResource(R.string.gesture_right)
    val down = stringResource(R.string.gesture_down)
    val up = stringResource(R.string.gesture_up)
    val pulse = ControlGroup(PULSE, stringResource(R.string.controls_pulse),
        if (!settings.pulse) emptyList() else listOf(ControlLine(tap, Trigger.Pulse(Gesture.Tap)), ControlLine(double, Trigger.Pulse(Gesture.DoubleTap)),
            ControlLine(hold, Trigger.Pulse(Gesture.Hold)), ControlLine(down, Trigger.Pulse(Gesture.SwipeDown)),
            ControlLine(left, Trigger.Pulse(Gesture.SwipeLeft)), ControlLine(right, Trigger.Pulse(Gesture.SwipeRight))),
        hint = stringResource(if (settings.pulse) R.string.pulse_gestures_hint else R.string.controls_pulse_off))
    val home = ControlGroup(HOME, stringResource(R.string.controls_home), listOf(
        ControlLine(up, fixed = stringResource(R.string.gesture_swipe_up_detail)),
        ControlLine(hold, fixed = stringResource(R.string.gesture_hold_home_edit_detail)),
        ControlLine(double, Trigger.Home(Gesture.DoubleTap)), ControlLine(down, Trigger.Home(Gesture.SwipeDown)),
        ControlLine(left, Trigger.Home(Gesture.SwipeLeft)), ControlLine(right, Trigger.Home(Gesture.SwipeRight))))
    val lens = ControlGroup(LENS, stringResource(R.string.controls_lens), listOf(
        ControlLine(tap, fixed = stringResource(R.string.gesture_tap_lens_detail)),
        ControlLine(up, fixed = stringResource(R.string.gesture_swipe_up_detail)),
        ControlLine(hold, fixed = stringResource(R.string.gesture_hold_lens_detail)),
        ControlLine(left, Trigger.Lens(Gesture.SwipeLeft)), ControlLine(right, Trigger.Lens(Gesture.SwipeRight))))
    val arc = ControlGroup(ARC, stringResource(R.string.arc_section), emptyList(), hint = stringResource(R.string.controls_arc_hint))
    val keys = ControlGroup(KEYS, stringResource(R.string.keys_title), emptyList(), hint = stringResource(R.string.keys_bind_hint))
    val perKey = settings.controls.keys.map { code ->
        ControlGroup("key$code", keyName(code), Controls.KEY_GESTURES.map { g ->
            ControlLine(when (g) { Gesture.Tap -> tap; Gesture.DoubleTap -> double; else -> hold }, Trigger.Key(code, g))
        }, key = code)
    }
    return listOf(pulse, home, lens, arc, keys) + perKey
}

@Composable
private fun GroupHeader(title: String, count: Int, open: Boolean, onToggle: () -> Unit) {
    val ink = LocalInk.current
    val state = stringResource(if (open) R.string.controls_open else R.string.controls_closed)
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onToggle)
        .semantics(mergeDescendants = true) { heading(); stateDescription = state }
        .padding(start = 24.dp, end = 16.dp, top = 22.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Kicker(title, Modifier.weight(1f), color = ink.accent)
            Text(if (count == 0) stringResource(R.string.controls_none_bound) else stringResource(R.string.controls_bound, count),
                Modifier.padding(end = 8.dp), style = InkText.small, color = ink.muted)
            HomeGlyph(Glyph.Up, null, Modifier.rotate(if (open) 0f else 180f), tint = ink.muted, size = 18.dp)
        }
        Hairline(Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun FixedLine(name: String, does: String) {
    val ink = LocalInk.current
    val fixed = stringResource(R.string.controls_fixed)
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).semantics(mergeDescendants = true) { stateDescription = fixed }
        .padding(start = 24.dp, end = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(name, style = InkText.body, color = ink.muted)
            Text(does, style = InkText.small, color = ink.faint, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Kicker(fixed, Modifier.padding(start = 12.dp), color = ink.faint)
    }
}

@Composable
private fun BoundLine(name: String, trigger: Trigger, controls: Controls, entry: (Action) -> ArcEntry?, onPick: () -> Unit, onRemove: () -> Unit) {
    val ink = LocalInk.current
    val menu = trigger == Trigger.Pulse(Gesture.Tap)
    val raw = controls.bound(trigger)
    val shown = raw?.let(entry)
    val elsewhere = raw?.let { controls.triggersOf(it).size - 1 } ?: 0
    val detail = when {
        shown != null -> slotLabel(shown)
        raw != null -> stringResource(R.string.arc_slot_unavailable)
        menu -> stringResource(R.string.pulse_gesture_menu)
        else -> stringResource(R.string.pulse_gesture_none)
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button,
        onClickLabel = stringResource(R.string.pulse_gesture_choose), onClick = onPick)
        .padding(start = 24.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (shown != null) ActionIcon(shown.action, detail, 28.dp, ink.ink, app = shown.app, shortcut = shown.shortcut)
        else HomeGlyph(Glyph.Plus, null, tint = ink.faint, size = 22.dp)
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(name, style = InkText.body)
            Text(detail, style = InkText.small, color = if (shown != null) ink.muted else ink.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // Informative only: one gesture has one action, but an action may hang on several gestures.
            if (elsewhere > 0) Text(stringResource(R.string.controls_elsewhere, elsewhere), style = InkText.small, color = ink.faint)
        }
        if (raw != null) IconAction(Glyph.Close, stringResource(if (menu) R.string.pulse_gesture_reset else R.string.remove),
            onRemove, tint = ink.muted)
    }
}

package dev.intelia.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.intelia.launcher.R
import dev.intelia.launcher.action.Action
import dev.intelia.launcher.action.ArcSet
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
 * Studio › Gesta a tlačítka (Veil 3.0): every gesture of every surface, grouped by where it happens (PULSE capsule,
 * Home, LENS, ARC, hardware keys) on one [VeilGroup] each. One gesture is one [GestureRow]: the action's icon, the
 * gesture, what it does; tap picks with the shared [ActionPicker], the cross unbinds (or brings a default back).
 * Unused gestures wait under "Další gesta". Only the LENS pillars are locked; Home's swipe up and hold have defaults
 * (drawer, editor) and can be rebound. The search on top filters gestures, surfaces and actions alike. Nothing runs here.
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
    var opened by rememberSaveable { mutableStateOf(listOf<String>()) }

    fun entry(a: Action): ArcEntry? = when (a) {
        is Action.App -> apps.firstOrNull { it.key == a.component }?.let { ArcEntry(a, app = it) }
        is Action.Flow -> settings.aliases.firstOrNull { it.id == a.id }?.let { ArcEntry(a, flow = it) }
        is Action.Shortcut -> shortcuts.firstOrNull { it.packageName == a.pkg && it.id == a.id }?.let { ArcEntry(a, shortcut = it) }
        else -> ArcEntry(a)
    }

    val groups = controlGroups(settings)
    val fq = AppSearch.fold(query.trim())
    val labels = groups.flatMap { it.lines }.mapNotNull { l -> l.trigger?.let { t -> controls.effective(t)?.let { a ->
        if (a is Action.Arc) t to arcActionLabel(controls, a) else entry(a)?.let { t to slotLabel(it) } } } }.toMap()
    fun matches(g: ControlGroup, l: ControlLine) = fq.isEmpty() ||
        listOfNotNull(g.title, l.name, l.fixed, l.trigger?.let(labels::get)).any { AppSearch.fold(it).contains(fq) }
    val pick: (Trigger) -> Unit = { picking = it }
    val unbind: (Trigger) -> Unit = { t -> model.editControls { it.bind(t, null) } }

    PickerFilter(query, stringResource(R.string.controls_find)) { query = it }
    Hint(stringResource(R.string.controls_hint))
    var anything = false
    groups.forEach { g ->
        val lines = g.lines.filter { matches(g, it) }
        if (lines.isEmpty() && fq.isNotEmpty()) return@forEach
        anything = true
        // Unused gestures wait under "Další gesta"; a search shows every match at once.
        fun used(l: ControlLine) = l.fixed != null || g.key != null || l.trigger == Trigger.Pulse(Gesture.Tap) ||
            l.trigger?.let(controls::effective) != null
        val (main, extra) = if (fq.isNotEmpty()) lines to emptyList() else lines.partition(::used)
        val more = g.id in opened
        VeilGroup(g.title) {
            g.hint?.let { GroupNote(it) }
            main.forEach { Line(it, controls, ::entry, pick, unbind) }
            if (extra.isNotEmpty()) {
                if (more) extra.forEach { Line(it, controls, ::entry, pick, unbind) }
                MoreRow(extra.size, more) { opened = if (more) opened - g.id else opened + g.id }
            }
            when (g.id) {
                ARC -> SettingRow(stringResource(R.string.controls_arc_sets), stringResource(R.string.controls_arc_count,
                    controls.arcSets.sumOf { s -> s.slots.count { it != null } }), onArc, glyph = Glyph.Arc)
                KEYS -> {
                    SwitchRow(stringResource(R.string.keys_everywhere), controls.keysEverywhere && service,
                        { on -> if (on && !service) PulseAccessibilityService.openSettings(context) else model.editControls { it.copy(keysEverywhere = on) } },
                        detail = stringResource(if (service) R.string.keys_everywhere_detail else R.string.keys_everywhere_needs_service))
                    SettingRow(stringResource(R.string.keys_add), null, onKeys, glyph = Glyph.Key)
                }
            }
            g.key?.let { code ->
                SettingRow(stringResource(R.string.keys_forget), stringResource(R.string.keys_forget_hint), null) {
                    IconAction(Glyph.Close, stringResource(R.string.keys_forget), { model.editControls { it.withoutKey(code) } }, tint = ink.muted)
                }
            }
        }
    }
    if (!anything) Hint(stringResource(R.string.pick_none))

    picking?.let { trigger ->
        val names = remember(all) { all.filter { it.mainProfile }.associate { it.component.packageName to it.label } }
        val outside = trigger is Trigger.Pulse || trigger is Trigger.Key
        // M3.3: a held PULSE gesture can open Global ARC on any set; one row per set, named after it.
        val arcs = if (Controls.opensArc(trigger)) controls.arcSets.ifEmpty { listOf(ArcSet(Controls.MAIN_SET)) }.map { Action.Arc(it.id) } else emptyList()
        val arcLabels = arcs.associate { a -> (a as Action) to arcActionLabel(controls, a) }
        ActionPicker(stringResource(R.string.pulse_gesture_choose),
            apps = apps.sortedBy { it.label.lowercase() }, flows = settings.aliases, shortcuts = shortcuts, system = arcs + SystemActions,
            labels = arcLabels,
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
            ControlLine(hold, Trigger.Pulse(Gesture.Hold)), ControlLine(stringResource(R.string.pulse_gesture_double_hold), Trigger.Pulse(Gesture.DoubleTapHold)),
            ControlLine(down, Trigger.Pulse(Gesture.SwipeDown)),
            ControlLine(left, Trigger.Pulse(Gesture.SwipeLeft)), ControlLine(right, Trigger.Pulse(Gesture.SwipeRight))),
        hint = stringResource(if (settings.pulse) R.string.pulse_gestures_hint else R.string.controls_pulse_off))
    val home = ControlGroup(HOME, stringResource(R.string.controls_home), listOf(
        ControlLine(up, Trigger.Home(Gesture.SwipeUp)), ControlLine(hold, Trigger.Home(Gesture.Hold)),
        ControlLine(double, Trigger.Home(Gesture.DoubleTap)), ControlLine(down, Trigger.Home(Gesture.SwipeDown)),
        ControlLine(left, Trigger.Home(Gesture.SwipeLeft)), ControlLine(right, Trigger.Home(Gesture.SwipeRight))),
        hint = stringResource(R.string.controls_home_hint))
    val lens = ControlGroup(LENS, stringResource(R.string.controls_lens), listOf(
        ControlLine(tap, fixed = stringResource(R.string.gesture_tap_lens_detail)),
        ControlLine(up, fixed = stringResource(R.string.gesture_swipe_up_detail)),
        ControlLine(hold, fixed = stringResource(R.string.gesture_hold_lens_detail)),
        ControlLine(left, Trigger.Lens(Gesture.SwipeLeft)), ControlLine(right, Trigger.Lens(Gesture.SwipeRight))),
        hint = stringResource(R.string.controls_lens_locked))
    val arc = ControlGroup(ARC, stringResource(R.string.arc_section), emptyList(), hint = stringResource(R.string.controls_arc_hint))
    val keys = ControlGroup(KEYS, stringResource(R.string.keys_title), emptyList(), hint = stringResource(R.string.keys_bind_hint))
    val perKey = settings.controls.keys.map { code ->
        ControlGroup("key$code", keyName(code), Controls.KEY_GESTURES.map { g ->
            ControlLine(when (g) { Gesture.Tap -> tap; Gesture.DoubleTap -> double; else -> hold }, Trigger.Key(code, g))
        }, key = code)
    }
    return listOf(pulse, home, lens, arc, keys) + perKey
}

/** "Další gesta": shows or hides the unused gestures of a group. */
@Composable
private fun MoreRow(count: Int, open: Boolean, onToggle: () -> Unit) {
    val ink = LocalInk.current
    val state = stringResource(if (open) R.string.controls_open else R.string.controls_closed)
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onToggle)
        .semantics(mergeDescendants = true) { stateDescription = state }
        .padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.controls_more), Modifier.weight(1f), style = InkText.body, color = ink.accent)
        Text(stringResource(R.string.controls_more_count, count), Modifier.padding(end = 8.dp), style = InkText.small, color = ink.muted)
        HomeGlyph(Glyph.Up, null, Modifier.rotate(if (open) 0f else 180f), tint = ink.muted, size = 18.dp)
    }
}

/** One gesture of a group: a locked pillar, or gesture → action with remove (or reset to its default). */
@Composable
private fun Line(line: ControlLine, controls: Controls, entry: (Action) -> ArcEntry?, onPick: (Trigger) -> Unit, onRemove: (Trigger) -> Unit) {
    val ink = LocalInk.current
    if (line.fixed != null) {
        val fixed = stringResource(R.string.controls_fixed)
        Box(Modifier.semantics(mergeDescendants = true) { stateDescription = fixed }) {
            GestureRow(line.name, line.fixed, { HomeGlyph(Glyph.Shield, null, tint = ink.faint, size = 20.dp) }, null, quiet = true) {
                Kicker(fixed, Modifier.padding(end = 10.dp), color = ink.faint)
            }
        }
        return
    }
    val trigger = line.trigger ?: return
    val menu = trigger == Trigger.Pulse(Gesture.Tap)
    val raw = controls.bound(trigger)
    val default = Controls.defaultOf(trigger)
    val action = raw ?: default
    val shown = action?.let(entry)
    val elsewhere = raw?.let { controls.triggersOf(it).size - 1 } ?: 0
    val detail = when {
        action is Action.Arc -> arcActionLabel(controls, action)
        shown != null -> slotLabel(shown)
        action != null -> stringResource(R.string.arc_slot_unavailable)
        menu -> stringResource(R.string.pulse_gesture_menu)
        else -> stringResource(R.string.pulse_gesture_none)
    }
    val note = when {
        raw == null && (default != null || menu) -> stringResource(R.string.controls_default)
        elsewhere > 0 -> stringResource(R.string.controls_elsewhere, elsewhere)
        else -> null
    }
    GestureRow(line.name, detail, {
        if (shown != null) ActionIcon(shown.action, detail, 28.dp, ink.ink, app = shown.app, shortcut = shown.shortcut)
        else HomeGlyph(if (menu) Glyph.Pulse else Glyph.Plus, null, tint = if (menu) ink.ink else ink.faint, size = 22.dp)
    }, { onPick(trigger) }, clickLabel = stringResource(R.string.pulse_gesture_choose), note = note, quiet = action == null && !menu) {
        if (raw != null) IconAction(Glyph.Close, stringResource(if (menu || default != null) R.string.pulse_gesture_reset else R.string.remove),
            { onRemove(trigger) }, tint = ink.muted)
    }
}

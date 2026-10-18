package dev.intelia.launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.intelia.launcher.R
import dev.intelia.launcher.action.Action
import dev.intelia.launcher.action.Controls
import dev.intelia.launcher.action.QuickAction
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.flow.FlowAlias

/** One Quick Action ready to draw: the action resolved on this device and the owner's icon, null = the action's own. */
data class QuickEntry(val slot: ArcEntry, val glyph: Glyph?)

/** Icons the owner can give a Quick Action instead of the action's own. */
internal val QuickGlyphs = listOf(Glyph.Torch, Glyph.Bell, Glyph.Bolt, Glyph.Layers, Glyph.Shot, Glyph.Play, Glyph.Search, Glyph.Apps,
    Glyph.Flow, Glyph.Settings, Glyph.Shield, Glyph.Wave, Glyph.Key, Glyph.Palette, Glyph.Grid, Glyph.Touch)

internal fun glyphNamed(name: String?): Glyph? = name?.let { n -> Glyph.entries.firstOrNull { it.name == n } }

/**
 * The glance's Quick Actions (S3.1), pure: the visible ones in the owner's order, resolved on this device. A removed
 * app or FLOW, an unknown shortcut and anything that needs the PULSE service while it is off are left out rather
 * than drawn as dead buttons. At most [max] items.
 */
internal fun quickEntries(controls: Controls, apps: List<AppEntry>, flows: List<FlowAlias>, service: Boolean, max: Int = MAX_GLANCE): List<QuickEntry> {
    val byKey = apps.associateBy { it.key }
    val byId = flows.associateBy { it.id }
    return controls.quickActions().asSequence().filterNot { it.hidden }.mapNotNull { q ->
        val a = q.action
        val slot = when (a) {
            is Action.App -> byKey[a.component]?.let { ArcEntry(a, app = it) }
            is Action.Flow -> byId[a.id]?.let { ArcEntry(a, flow = it) }
            is Action.Shortcut, is Action.Card, is Action.Arc -> null
            is Action.SystemPanel -> if (a.kind.viaService && !service) null else ArcEntry(a)
            Action.Screenshot, Action.Lock -> if (service) ArcEntry(a) else null
            else -> ArcEntry(a)
        }
        slot?.let { QuickEntry(it, glyphNamed(q.glyph)) }
    }.take(max).toList()
}

/** What fits on the glance in one row next to the charging state. */
internal const val MAX_GLANCE = 4

/**
 * Studio › PULSE › Rychlé akce: the owner's list in order. A row opens in place with what can change: swap the action
 * (the shared [ActionPicker]), its icon, hide, move, remove. Nothing runs here.
 */
@Composable
internal fun QuickActionsEditor(model: HomeViewModel, settings: LauncherSettings) {
    val ink = LocalInk.current
    val apps by model.visibleApps.collectAsStateWithLifecycle()
    val host = rememberActionHost(model) { }
    val controls = settings.controls
    val list = controls.quickActions()
    var open by rememberSaveable { mutableStateOf<Int?>(null) }
    var picking by remember { mutableStateOf<Int?>(null) } // index to replace, -1 = add
    fun entry(a: Action): ArcEntry = when (a) {
        is Action.App -> ArcEntry(a, app = apps.firstOrNull { it.key == a.component })
        is Action.Flow -> ArcEntry(a, flow = settings.aliases.firstOrNull { it.id == a.id })
        else -> ArcEntry(a)
    }
    val hiddenText = stringResource(R.string.pulse_quick_hidden)
    list.forEachIndexed { i, q ->
        val e = entry(q.action)
        val label = slotLabel(e)
        val why = q.action.unavailableReason(host)?.let { reasonNote(it) }
        GestureRow(label, listOfNotNull(if (q.hidden) hiddenText else null, why).joinToString(" · ").ifEmpty { stringResource(R.string.pulse_quick_shown) },
            icon = {
                val g = glyphNamed(q.glyph)
                val tint = if (q.hidden) ink.faint else ink.accent
                if (g != null) HomeGlyph(g, null, tint = tint, size = 24.dp) else ActionIcon(q.action, label, 30.dp, tint, app = e.app)
            },
            onClick = { open = if (open == i) null else i }, quiet = q.hidden,
            trailing = { Text(if (open == i) "⌃" else "⌄", Modifier.padding(end = 12.dp), style = InkText.body, color = ink.muted) })
        if (open == i) Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, bottom = 8.dp)) {
            SettingRow(stringResource(R.string.pulse_quick_swap), null, { picking = i })
            Text(stringResource(R.string.pulse_quick_icon), Modifier.padding(start = 4.dp, top = 4.dp, bottom = 6.dp), style = InkText.small, color = ink.muted)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { GlyphChoice(null, q.glyph == null) { model.editControls { c -> c.editQuick(i) { it.copy(glyph = null) } } } }
                items(QuickGlyphs) { g ->
                    GlyphChoice(g, q.glyph == g.name) { model.editControls { c -> c.editQuick(i) { it.copy(glyph = g.name) } } }
                }
            }
            SwitchRow(stringResource(R.string.pulse_quick_hide), q.hidden, { h -> model.editControls { c -> c.editQuick(i) { it.copy(hidden = h) } } })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (i > 0) IconAction(Glyph.Up, stringResource(R.string.pulse_quick_up), { model.editControls { it.moveQuick(i, -1) }; open = i - 1 }, tint = ink.muted)
                if (i < list.lastIndex) Box(Modifier.rotate(180f)) {
                    IconAction(Glyph.Up, stringResource(R.string.pulse_quick_down), { model.editControls { it.moveQuick(i, 1) }; open = i + 1 }, tint = ink.muted)
                }
                IconAction(Glyph.Close, stringResource(R.string.pulse_quick_remove), { model.editControls { c -> c.editQuick(i) { null } }; open = null }, tint = ink.muted)
            }
        }
    }
    if (list.isEmpty()) GroupNote(stringResource(R.string.pulse_quick_empty))
    if (list.size < Controls.MAX_QUICK) SettingRow(stringResource(R.string.pulse_quick_add), null, { picking = -1 }, glyph = Glyph.Plus)
    if (controls.quick != null) SettingRow(stringResource(R.string.reset_defaults), systemList(Controls.DEFAULT_QUICK),
        { model.editControls { it.copy(quick = null) }; open = null })
    GroupNote(stringResource(R.string.pulse_quick_note))

    picking?.let { index ->
        ActionPicker(stringResource(R.string.pulse_quick_choose), apps = apps.sortedBy { it.label.lowercase() }, flows = settings.aliases,
            shortcuts = emptyList(), system = SystemActions, host = host, used = list.map { it.action }, showReach = true,
            onPick = { a ->
                model.editControls { c -> if (index < 0) c.withQuick(c.quickActions() + QuickAction(a)) else c.editQuick(index) { it.copy(action = a) } }
                picking = null
            }, onDismiss = { picking = null })
    }
}

@Composable
private fun GlyphChoice(glyph: Glyph?, on: Boolean, onClick: () -> Unit) {
    val ink = LocalInk.current
    Box(Modifier.size(44.dp).clip(CircleShape).background(if (on) ink.accent else ink.ink.copy(alpha = .07f))
        .clickable(role = Role.RadioButton, onClick = onClick).semantics { selected = on }, contentAlignment = Alignment.Center) {
        if (glyph != null) HomeGlyph(glyph, glyph.name, tint = if (on) ink.onAccent else ink.ink, size = 20.dp)
        else Text(stringResource(R.string.pulse_quick_icon_auto), style = InkText.label, color = if (on) ink.onAccent else ink.muted)
    }
}

/** "Svítilna · Budík · +1": the visible Quick Actions, for the PULSE page's summary. */
@Composable
internal fun quickSummary(controls: Controls, model: HomeViewModel): String {
    val shown = controls.quickActions().filterNot { it.hidden }
    if (shown.isEmpty()) return stringResource(R.string.pulse_quick_none)
    val names = shown.take(2).map { q ->
        when (val a = q.action) {
            is Action.App -> model.entry(a.component)?.label ?: stringResource(R.string.arc_slot_unavailable)
            is Action.Flow -> model.settings.value.aliases.firstOrNull { it.id == a.id }?.name ?: stringResource(R.string.arc_slot_unavailable)
            else -> systemActionLabel(a)
        }
    }
    return (names + listOfNotNull(if (shown.size > 2) "+${shown.size - 2}" else null)).joinToString(" · ")
}

@Composable
private fun systemList(items: List<QuickAction>): String = items.map { systemActionLabel(it.action) }.joinToString(" · ")

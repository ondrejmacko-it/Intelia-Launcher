package dev.intelia.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.intelia.launcher.R
import dev.intelia.launcher.action.Action
import dev.intelia.launcher.action.ActionHost
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.ShortcutEntry
import dev.intelia.launcher.flow.FlowAlias
import dev.intelia.launcher.search.AppSearch

/** One row of [ActionPicker]: what it binds, how it is named and found, and what it still needs. */
private class PickRow(val action: Action, val label: String, val detail: String?, val section: Int, val fold: String,
    val app: AppEntry? = null, val shortcut: ShortcutEntry? = null)

/**
 * The shared action picker (Control System M2): Apps, FLOW, Shortcuts and System sections on Veil Dense from the
 * bottom, with a diacritics-insensitive filter ranked like Search. Picking only returns the action; nothing runs here.
 * With a [host], each row says why it cannot run now (or what it needs), so a choice is never a surprise later.
 *
 * @param appNames shortcut package → app label, shown under each shortcut.
 */
@Composable
fun ActionPicker(
    title: String,
    apps: List<AppEntry>,
    flows: List<FlowAlias>,
    onPick: (Action) -> Unit,
    onDismiss: () -> Unit,
    shortcuts: List<ShortcutEntry> = emptyList(),
    system: List<Action> = emptyList(),
    host: ActionHost? = null,
    appNames: Map<String, String> = emptyMap(),
    /** Shown in the Shortcuts section when the system offers none (e.g. Intelia is not the default home). */
    shortcutsNote: String? = null,
) {
    val c = LocalCrystal.current
    val sections = listOf(stringResource(R.string.arc_ring_apps), stringResource(R.string.flow), stringResource(R.string.arc_ring_shortcuts),
        stringResource(R.string.act_system))
    val systemRows = system.map { a ->
        val label = systemActionLabel(a)
        PickRow(a, label, host?.let { a.unavailableReason(it) }?.let { reasonNote(it) } ?: requirementNote(a.requirement), 3, AppSearch.fold(label))
    }
    val rows = remember(apps, flows, shortcuts, systemRows.map { it.detail }) {
        apps.map { PickRow(Action.App(it.key), it.label, null, 0, it.searchKey, app = it) } +
            flows.map { PickRow(Action.Flow(it.id), it.name, null, 1, AppSearch.fold(it.name)) } +
            shortcuts.map { PickRow(Action.Shortcut(it.packageName, it.id), it.label, appNames[it.packageName], 2, AppSearch.fold(it.label), shortcut = it) } +
            systemRows
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).veilSurface(veil(VeilLevel.Dense), VeilOrigin.Bottom).padding(vertical = 12.dp)) {
            Text(title, Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = CrystalType.body.copy(fontSize = 17.sp), color = c.fg)
            var filter by remember { mutableStateOf("") }
            if (rows.size > PICKER_FILTER_FROM) PickerFilter(filter) { filter = it }
            val shown = if (filter.isBlank()) rows else AppSearch.rank(rows, filter) { it.fold }
            if (shown.isEmpty()) Text(stringResource(R.string.pick_none), Modifier.padding(20.dp), style = CrystalType.body.copy(fontSize = 14.sp), color = c.fg2)
            // Sections only when the list is not filtered and holds more than one kind.
            val grouped = filter.isBlank() && (rows.map { it.section }.distinct().size > 1 || shortcutsNote != null)
            LazyColumn {
                (if (grouped) 0..3 else 0..0).forEach { section ->
                    val inSection = if (grouped) shown.filter { it.section == section } else shown
                    val note = if (grouped && section == 2 && inSection.isEmpty()) shortcutsNote else null
                    if (grouped && (inSection.isNotEmpty() || note != null)) item(key = "s$section") {
                        Kicker(sections[section], Modifier.padding(start = 20.dp, top = 14.dp, bottom = 2.dp), color = LocalInk.current.accent)
                    }
                    if (note != null) item(key = "n$section") {
                        Text(note, Modifier.padding(horizontal = 20.dp, vertical = 6.dp), style = CrystalType.body.copy(fontSize = 13.sp), color = c.fg2)
                    }
                    items(inSection, key = { it.section.toString() + it.action.toString() }) { row -> PickerRow(row) { onPick(row.action) } }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(row: PickRow, onClick: () -> Unit) {
    val c = LocalCrystal.current
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        ActionIcon(row.action, row.label, 32.dp, c.fg, app = row.app, shortcut = row.shortcut)
        Column(Modifier.padding(start = 14.dp)) {
            Text(row.label, style = CrystalType.body.copy(fontSize = 15.sp, fontStyle = if (row.action is Action.Flow) FontStyle.Italic else FontStyle.Normal),
                color = c.fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            row.detail?.let { Text(it, style = CrystalType.body.copy(fontSize = 12.sp), color = c.fg2, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
    }
}

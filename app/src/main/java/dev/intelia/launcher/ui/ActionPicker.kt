package dev.intelia.launcher.ui

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.draw.clip
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

/** Picker categories (M5): the order of the tabs and of the sections in "All". */
private const val CAT_ARC = 0
private const val CAT_APPS = 1
private const val CAT_FLOW = 2
private const val CAT_SHORTCUTS = 3
private const val CAT_SYSTEM = 4
private const val CAT_MEDIA = 5
private const val CAT_PANELS = 6
private const val CAT_COUNT = 7

private fun Action.category(): Int = when (this) {
    is Action.Arc -> CAT_ARC
    is Action.App -> CAT_APPS
    is Action.Flow -> CAT_FLOW
    is Action.Shortcut -> CAT_SHORTCUTS
    is Action.Media -> CAT_MEDIA
    is Action.SystemPanel -> CAT_PANELS
    else -> CAT_SYSTEM
}

/**
 * The shared action picker (Control System M2, M5): category tabs (All, Apps, FLOW, Shortcuts, System, Media, Panels)
 * over one diacritics-insensitive filter ranked like Search, on Veil Dense from the bottom. Actions already used
 * somewhere come first. Picking only returns the action; nothing runs here. With a [host], each row says why it
 * cannot run now (or what it needs), so a choice is never a surprise later; with [showReach] it also says whether the
 * action works outside Home (PULSE and keys run there).
 *
 * @param appNames shortcut package → app label, shown under each shortcut.
 * @param used actions bound somewhere already, offered first.
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
    used: List<Action> = emptyList(),
    showReach: Boolean = false,
    /** Names that say more than the action alone (Global ARC rows name their set). */
    labels: Map<Action, String> = emptyMap(),
) {
    val c = LocalCrystal.current
    val sections = listOf(stringResource(R.string.global_arc), stringResource(R.string.arc_ring_apps), stringResource(R.string.flow), stringResource(R.string.arc_ring_shortcuts),
        stringResource(R.string.act_system), stringResource(R.string.pick_media), stringResource(R.string.pick_panels))
    val everywhere = stringResource(R.string.pick_everywhere)
    val homeOnly = stringResource(R.string.pick_home_only)
    fun reach(a: Action): String? = if (!showReach) null else if (a.outsideHome) everywhere else homeOnly
    fun join(vararg parts: String?): String? = parts.filterNotNull().joinToString(" · ").ifEmpty { null }
    val systemRows = system.map { a ->
        val label = labels[a] ?: systemActionLabel(a)
        PickRow(a, label, join(host?.let { a.unavailableReason(it) }?.let { reasonNote(it) } ?: requirementNote(a.requirement), reach(a)),
            a.category(), AppSearch.fold(label))
    }
    val rows = remember(apps, flows, shortcuts, systemRows.map { it.detail }, showReach) {
        apps.map { PickRow(Action.App(it.key), it.label, reach(Action.App(it.key)), CAT_APPS, it.searchKey, app = it) } +
            flows.map { PickRow(Action.Flow(it.id), it.name, reach(Action.Flow(it.id)), CAT_FLOW, AppSearch.fold(it.name)) } +
            shortcuts.map { PickRow(Action.Shortcut(it.packageName, it.id), it.label, join(appNames[it.packageName], reach(Action.Shortcut(it.packageName, it.id))),
                CAT_SHORTCUTS, AppSearch.fold(it.label), shortcut = it) } +
            systemRows
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().heightIn(max = 600.dp).veilSurface(veil(VeilLevel.Dense), VeilOrigin.Bottom).padding(vertical = 12.dp)) {
            Text(title, Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = CrystalType.body.copy(fontSize = 17.sp), color = c.fg)
            var filter by remember { mutableStateOf("") }
            var tab by remember { mutableStateOf(-1) }
            PickerFilter(filter, stringResource(R.string.pick_find)) { filter = it }
            val present = rows.map { it.section }.toSet() + (if (shortcutsNote != null) setOf(CAT_SHORTCUTS) else emptySet())
            val tabs = listOf(-1) + (0 until CAT_COUNT).filter { it in present }
            if (tabs.size > 2) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEach { t -> PickTab(if (t < 0) stringResource(R.string.pick_all) else sections[t], tab == t) { tab = t } }
            }
            val inTab = if (tab < 0) rows else rows.filter { it.section == tab }
            val shown = if (filter.isBlank()) inTab else AppSearch.rank(inTab, filter) { it.fold }
            val first = if (filter.isBlank() && tab < 0) used.mapNotNull { a -> rows.firstOrNull { it.action == a } }.distinctBy { it.action } else emptyList()
            val note = if (tab == CAT_SHORTCUTS || (tab < 0 && filter.isBlank())) shortcutsNote?.takeIf { rows.none { it.section == CAT_SHORTCUTS } } else null
            if (shown.isEmpty() && note == null) Text(stringResource(R.string.pick_none), Modifier.padding(20.dp), style = CrystalType.body.copy(fontSize = 14.sp), color = c.fg2)
            // Sections only in "All" without a filter; a tab or a filter is one flat, ranked list.
            val grouped = filter.isBlank() && tab < 0
            LazyColumn {
                if (first.isNotEmpty()) {
                    item(key = "used") { PickCaption(stringResource(R.string.pick_used)) }
                    items(first, key = { "u" + it.action.toString() }) { row -> PickerRow(row) { onPick(row.action) } }
                }
                (if (grouped) 0 until CAT_COUNT else 0..0).forEach { section ->
                    val inSection = if (grouped) shown.filter { it.section == section } else shown
                    val sectionNote = if (grouped && section == CAT_SHORTCUTS && inSection.isEmpty()) note else if (!grouped && section == 0) note else null
                    if (grouped && (inSection.isNotEmpty() || sectionNote != null)) item(key = "s$section") {
                        PickCaption(sections[section])
                    }
                    if (sectionNote != null) item(key = "n$section") {
                        Text(sectionNote, Modifier.padding(horizontal = 20.dp, vertical = 6.dp), style = CrystalType.body.copy(fontSize = 13.sp), color = c.fg2)
                    }
                    items(inSection, key = { it.section.toString() + it.action.toString() }) { row -> PickerRow(row) { onPick(row.action) } }
                }
            }
        }
    }
}

/** A section caption in the picker, in the same quiet accent voice as Studio's group captions. */
@Composable
private fun PickCaption(text: String) {
    Text(text.uppercase(), Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
        style = InkText.label.copy(fontSize = 11.sp, letterSpacing = 1.4.sp), color = LocalInk.current.accent.copy(alpha = .9f), maxLines = 1)
}

/** A tab as one of Studio's soft choices: the chosen one washed and edged in the accent. */
@Composable
private fun PickTab(text: String, on: Boolean, onClick: () -> Unit) {
    val ink = LocalInk.current
    val shape = RoundedCornerShape(14.dp)
    Text(text, Modifier.clip(shape).background(if (on) ink.accent.copy(alpha = if (ink.dark) .30f else .22f) else ink.ink.copy(alpha = .06f))
        .then(if (on) Modifier.border(1.dp, ink.accent.copy(alpha = .65f), shape) else Modifier)
        .selectable(on, role = Role.Tab, onClick = onClick).heightIn(min = 40.dp).padding(horizontal = 14.dp, vertical = 10.dp),
        style = InkText.body.copy(fontSize = 14.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal),
        color = if (on) ink.ink else ink.muted, maxLines = 1)
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

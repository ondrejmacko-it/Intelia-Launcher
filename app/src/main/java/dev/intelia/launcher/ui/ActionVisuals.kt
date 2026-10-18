package dev.intelia.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.intelia.launcher.R
import dev.intelia.launcher.action.Action
import dev.intelia.launcher.action.ArcLook
import dev.intelia.launcher.action.GlobalArcLook
import dev.intelia.launcher.action.ArcSet
import dev.intelia.launcher.action.Controls
import dev.intelia.launcher.action.MediaKey
import dev.intelia.launcher.action.PanelKind
import dev.intelia.launcher.action.Reason
import dev.intelia.launcher.action.Requirement
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.ShortcutEntry
import dev.intelia.launcher.flow.FlowAlias

/** One ARC slot as shown: the bound action and what it resolves to on this device. */
data class ArcEntry(val action: Action, val app: AppEntry? = null, val flow: FlowAlias? = null, val shortcut: ShortcutEntry? = null)

/** One ARC set as shown; [slots] has exactly [ArcLook.slots] entries in priority order, null = empty. */
data class ArcSetView(val id: String, val name: String, val slots: List<ArcEntry?>)

/** Everything ARC can hold right now, set by set. */
class ArcContent(val sets: List<ArcSetView>, val look: ArcLook, val global: GlobalArcLook = GlobalArcLook()) {
    /** Geometry and tick of Global ARC: the shared slot count with its own size and angle. */
    val globalLook: ArcLook = global.over(look)

    val empty: Boolean = sets.all { set -> set.slots.all { it == null } }
    fun filled(set: Int): List<Boolean> = sets.getOrNull(set)?.slots?.map { it != null }.orEmpty()

    companion object {
        /**
         * Every set of [controls], each slot resolved on this device (Home and the PULSE service alike). A removed or
         * hidden app, a deleted FLOW or a shortcut the system no longer offers leaves its slot empty, never shifting others.
         */
        fun of(controls: Controls, apps: List<AppEntry>, flows: List<FlowAlias>, shortcuts: List<ShortcutEntry>): ArcContent {
            val byKey = apps.associateBy { it.key }
            val byId = flows.associateBy { it.id }
            val cuts = shortcuts.associateBy { it.key }
            fun resolve(a: Action?): ArcEntry? = when (a) {
                null -> null
                is Action.App -> byKey[a.component]?.let { ArcEntry(a, app = it) }
                is Action.Flow -> byId[a.id]?.let { ArcEntry(a, flow = it) }
                is Action.Shortcut -> cuts[a.pkg + "/" + a.id]?.let { ArcEntry(a, shortcut = it) }
                else -> ArcEntry(a)
            }
            val sets = controls.arcSets.ifEmpty { listOf(ArcSet(Controls.MAIN_SET)) }.map { set ->
                ArcSetView(set.id, set.name, List(controls.arc.slots) { i -> resolve(set.slots.getOrNull(i)) })
            }
            return ArcContent(sets, controls.arc, controls.globalArc)
        }
    }
}

/** System actions offered in pickers, in the order shown. */
val SystemActions: List<Action> = listOf(
    Action.Search, Action.Drawer, Action.EditHome, Action.Torch, Action.Media(MediaKey.PlayPause), Action.Media(MediaKey.Next), Action.Media(MediaKey.Previous),
    Action.SoundMode, Action.SystemPanel(PanelKind.Internet), Action.SystemPanel(PanelKind.Wifi), Action.SystemPanel(PanelKind.Bluetooth),
    Action.SystemPanel(PanelKind.Sound), Action.SystemPanel(PanelKind.Display), Action.SystemPanel(PanelKind.Notifications),
    Action.SystemPanel(PanelKind.QuickSettings), Action.Screenshot, Action.Lock,
)

/** Name of a system action; apps, FLOWs and shortcuts are named by what they resolve to. */
@Composable
fun systemActionLabel(action: Action): String = stringResource(when (action) {
    Action.Search -> R.string.search
    Action.Torch -> R.string.torch
    is Action.Media -> when (action.key) {
        MediaKey.PlayPause -> R.string.act_play_pause
        MediaKey.Next -> R.string.act_next
        MediaKey.Previous -> R.string.act_previous
    }
    Action.SoundMode -> R.string.act_sound_mode
    is Action.SystemPanel -> when (action.kind) {
        PanelKind.Internet -> R.string.act_panel_internet
        PanelKind.Wifi -> R.string.act_panel_wifi
        PanelKind.Bluetooth -> R.string.act_panel_bluetooth
        PanelKind.Sound -> R.string.act_panel_sound
        PanelKind.Display -> R.string.act_panel_display
        PanelKind.Notifications -> R.string.act_panel_notifications
        PanelKind.QuickSettings -> R.string.act_panel_quick
    }
    Action.Screenshot -> R.string.act_screenshot
    Action.Lock -> R.string.pulse_lock
    Action.Drawer -> R.string.act_drawer
    Action.EditHome -> R.string.act_edit_home
    is Action.Arc -> R.string.global_arc
    is Action.App, is Action.Flow, is Action.Shortcut, is Action.Card -> R.string.act_unknown
})

/** What a slot is called, for TalkBack, the picked-slot label and Studio rows. */
@Composable
fun slotLabel(slot: ArcEntry): String = slot.app?.label ?: slot.flow?.name ?: slot.shortcut?.label ?: systemActionLabel(slot.action)

/** "Global ARC · set name": which set a held PULSE gesture opens (M3.3). */
@Composable
fun arcActionLabel(controls: Controls, arc: Action.Arc): String {
    val set = controls.arcSetFor(arc)
    val index = controls.arcSets.indexOfFirst { it.id == set.id }.coerceAtLeast(0)
    return stringResource(R.string.global_arc_set, setLabel(ArcSetView(set.id, set.name, emptyList()), index))
}

/** A set's name: the owner's, the migrated ring's, or "Sada N". */
@Composable
fun setLabel(set: ArcSetView, index: Int): String = set.name.ifBlank {
    when (set.id) {
        Controls.MAIN_SET -> stringResource(R.string.arc_set_main)
        Controls.FLOW_SET -> stringResource(R.string.flow)
        Controls.SHORTCUT_SET -> stringResource(R.string.arc_ring_shortcuts)
        else -> stringResource(R.string.arc_set_n, index + 1)
    }
}

/** What the user has to grant first, said next to the action; null when nothing. */
@Composable
fun requirementNote(requirement: Requirement): String? = when (requirement) {
    Requirement.None -> null
    Requirement.DefaultHome -> stringResource(R.string.req_default_home)
    Requirement.PulseService -> stringResource(R.string.req_pulse)
}

@Composable
fun reasonNote(reason: Reason): String = stringResource(when (reason) {
    Reason.AppMissing -> R.string.action_app_missing
    Reason.FlowMissing -> R.string.action_flow_missing
    Reason.NotDefaultHome -> R.string.action_not_default_home
    Reason.ShortcutMissing -> R.string.action_shortcut_missing
    Reason.NeedsPulseService -> R.string.action_needs_pulse
    Reason.Unsupported -> R.string.action_unsupported
    Reason.Invalid, Reason.CardMissing, Reason.Failed -> R.string.launch_failed
})

private fun glyphOf(action: Action): Glyph? = when (action) {
    Action.Search -> Glyph.Search
    Action.Torch -> Glyph.Torch
    is Action.Media -> when (action.key) { MediaKey.PlayPause -> Glyph.Play; MediaKey.Next -> Glyph.Next; MediaKey.Previous -> Glyph.Prev }
    Action.SoundMode -> Glyph.Bell
    is Action.SystemPanel -> when (action.kind) {
        PanelKind.Notifications -> Glyph.Bell
        PanelKind.QuickSettings -> Glyph.Layers
        else -> Glyph.Settings
    }
    Action.Screenshot -> Glyph.Grid
    is Action.Arc -> Glyph.Apps
    Action.Drawer -> Glyph.Up
    Action.EditHome -> Glyph.Layers
    else -> null
}

/**
 * The icon of a slot or picker row: the app or shortcut icon, a FLOW monogram in a ring, a system glyph,
 * or a monogram of the name when there is no glyph. Decorative; the caller sets the description.
 */
@Composable
fun ActionIcon(action: Action, label: String, size: Dp, tint: Color, modifier: Modifier = Modifier, app: AppEntry? = null,
    shortcut: ShortcutEntry? = null) {
    when {
        app != null -> AppIcon(app, modifier.size(size), tint = tint)
        shortcut != null -> ShortcutIcon(shortcut, modifier.size(size * .86f), tint = tint)
        else -> Box(modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(size)) { drawCircle(tint.copy(alpha = .55f), this.size.minDimension / 2f - 1.dp.toPx(), style = Stroke(1.2.dp.toPx())) }
            val glyph = glyphOf(action)
            if (glyph != null && action !is Action.Flow) HomeGlyph(glyph, null, tint = tint, size = size * .5f)
            else Text(label.trim().take(2).ifEmpty { "·" }, style = InkText.item.copy(fontStyle = if (action is Action.Flow) FontStyle.Italic else FontStyle.Normal,
                fontSize = (size.value * .36f).sp), color = tint, maxLines = 1)
        }
    }
}

package dev.intelia.launcher.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.intelia.launcher.R
import dev.intelia.launcher.data.Glass
import dev.intelia.launcher.data.IconStyle
import dev.intelia.launcher.data.IconShape
import dev.intelia.launcher.data.ItemKind
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.ThemeMode

/** Changes the stored settings at once (not part of the layout's undo history). */
typealias SettingsEdit = ((LauncherSettings) -> LauncherSettings) -> Unit

/** The icon style for all of Home, Search, drawer and ARC; in the Look panel, the icon inspector and Studio. */
@Composable
fun IconStyleChips(settings: LauncherSettings, set: SettingsEdit) {
    Chips(listOf(IconStyle.System to stringResource(R.string.icons_system), IconStyle.Ink to stringResource(R.string.icons_ink),
        IconStyle.Veil to stringResource(R.string.look_veil), IconStyle.Tinted to stringResource(R.string.icons_tinted)),
        settings.iconStyle) { v -> set { it.copy(iconStyle = v) } }
    if (settings.iconStyle == IconStyle.Veil || settings.iconStyle == IconStyle.Tinted) {
        Chips(listOf(IconShape.Circle to stringResource(R.string.shape_circle), IconShape.Squircle to stringResource(R.string.shape_squircle),
            IconShape.Rounded to stringResource(R.string.shape_rounded), IconShape.Drop to stringResource(R.string.shape_drop)),
            settings.iconShape) { v -> set { it.copy(iconShape = v) } }
    }
}

/** The panel glass (§5.2) for every surface; in the Look panel and Studio. */
@Composable
fun GlassChips(settings: LauncherSettings, set: SettingsEdit) {
    Chips(listOf(Glass.Veil to stringResource(R.string.look_veil), Glass.Crystal to stringResource(R.string.glass_crystal),
        Glass.Frost to stringResource(R.string.glass_frost), Glass.Liquid to stringResource(R.string.glass_liquid)),
        settings.glass) { v -> set { it.copy(glass = v) } }
}

/**
 * The editor's Look panel (Vzhled): everything that styles the whole Home and every panel — Veil material,
 * theme, accent, icons, labels. Opened from the bar; sits above the LENS row so the clock stays visible.
 * Settings apply immediately; labels and icon sizes are layout edits and go through undo.
 */
@Composable
fun LookPanel(settings: LauncherSettings, state: HomeEditorState, set: SettingsEdit, onChange: () -> Unit,
    modifier: Modifier = Modifier) {
    val c = LocalCrystal.current
    // The body of the editor card (InspectorCard): it brings the glass, the header, the scroll and the handles.
    Column(modifier.fillMaxWidth()) {
        Kick(stringResource(R.string.look_material))
        GlassChips(settings, set)
        Chips(listOf(false to stringResource(R.string.look_glass_clear), true to stringResource(R.string.look_solid)), settings.veilSolid) { v ->
            set { it.copy(veilSolid = v) }
        }
        if (!settings.veilSolid) {
            val shift = settings.veilShift
            SliderRow(stringResource(R.string.look_density), shift.toFloat(), -LauncherSettings.MAX_VEIL_SHIFT.toFloat(),
                LauncherSettings.MAX_VEIL_SHIFT.toFloat(), 1f, (if (shift > 0) "+" else "") + "$shift %") { v ->
                set { it.copy(veilShift = v.toInt()) }
            }
        }
        if (settings.glass == Glass.Veil) Toggle(stringResource(R.string.look_edge), settings.veilEdge) { v -> set { it.copy(veilEdge = v) } }

        Kick(stringResource(R.string.theme))
        Chips(listOf(ThemeMode.System to stringResource(R.string.theme_system), ThemeMode.Light to stringResource(R.string.theme_light),
            ThemeMode.Dark to stringResource(R.string.theme_dark)), settings.theme) { v -> set { it.copy(theme = v) } }
        PaletteEditor(settings, set)

        Kick(stringResource(R.string.icons))
        IconStyleChips(settings, set)
        SliderRow(stringResource(R.string.inspect_size), settings.iconSize.toFloat(), LauncherSettings.MIN_ICON.toFloat(),
            LauncherSettings.MAX_ICON.toFloat(), 2f, "${settings.iconSize} dp") { v -> set { it.copy(iconSize = v.toInt()) } }
        val icons = state.layout.items.filter { it.kind == ItemKind.App || it.kind == ItemKind.Flow }
        if (icons.any { it.sizeDp != 0 }) Action(stringResource(R.string.look_icons_same), c.acc) { state.resetIconSizes(); onChange() }
        Toggle(stringResource(R.string.look_labels), icons.isNotEmpty() && icons.all { it.label }) { on -> state.setLabels(on); onChange() }
        Text(stringResource(R.string.look_hint), Modifier.padding(vertical = 8.dp), style = CrystalType.body.copy(fontSize = 12.sp), color = c.fg2)
    }
}

@Composable
internal fun Kick(text: String) {
    Text(text.uppercase(), Modifier.padding(top = 10.dp, bottom = 2.dp), style = CrystalType.label, color = LocalCrystal.current.fg2)
}

/** PULSE (tap the camera ring in the editor, §5.5): the capsule's look and events; calibration lives in Studio. */
@Composable
fun PulseInspector(settings: LauncherSettings, set: SettingsEdit, onLens: () -> Unit) {
    val c = LocalCrystal.current
    Toggle(stringResource(R.string.pulse_toggle), settings.pulse) { v -> set { it.copy(pulse = v) } }
    if (!settings.pulse) return
    SliderRow(stringResource(R.string.pulse_opacity), settings.pulseOpacity.toFloat(), LauncherSettings.MIN_PULSE_OPACITY.toFloat(), 100f, 5f,
        "${settings.pulseOpacity} %") { v -> set { it.copy(pulseOpacity = v.toInt()) } }
    Toggle(stringResource(R.string.pulse_echo), settings.pulseEcho) { v -> set { it.copy(pulseEcho = v) } }
    Toggle(stringResource(R.string.pulse_charging), settings.pulseCharging) { v -> set { it.copy(pulseCharging = v) } }
    Toggle(stringResource(R.string.pulse_alarm), settings.pulseAlarm) { v -> set { it.copy(pulseAlarm = v) } }
    if (settings.pulseAlarm) Chips(listOf(3 to "3 h", 6 to "6 h", 12 to "12 h", 24 to "24 h"), settings.pulseAlarmHours) { v ->
        set { it.copy(pulseAlarmHours = v) }
    }
    Action(stringResource(R.string.pulse_lens), c.acc, onLens)
}

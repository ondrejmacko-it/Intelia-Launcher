package dev.intelia.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.intelia.launcher.R
import dev.intelia.launcher.data.AnchorType
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.ClockStyle
import dev.intelia.launcher.data.DateFormat
import dev.intelia.launcher.data.Reach
import dev.intelia.launcher.data.HomeItem
import dev.intelia.launcher.data.ItemKind
import dev.intelia.launcher.data.ItemTint
import dev.intelia.launcher.data.LayoutSpec
import dev.intelia.launcher.data.LensStyle
import dev.intelia.launcher.flow.FlowAlias
import kotlin.math.roundToInt

/** Default sizes the inspector starts from when [HomeItem.sizeDp] is 0. */
object ItemSizes {
    const val ICON = 40
    const val ICON_MIN = 32
    const val ICON_MAX = 64
    const val CLOCK = 84
    const val CLOCK_MIN = 48
    const val CLOCK_MAX = 140
    const val LENS = 44
    const val LENS_MIN = 36
    const val LENS_MAX = 56
    const val DATE = 12
    const val DATE_MIN = 9
    const val DATE_MAX = 32
}

fun HomeItem.iconDp(default: Int = ItemSizes.ICON) = if (sizeDp == 0) default else sizeDp.coerceIn(ItemSizes.ICON_MIN, ItemSizes.ICON_MAX)
fun HomeItem.dateSp() = if (sizeDp == 0) ItemSizes.DATE else sizeDp.coerceIn(ItemSizes.DATE_MIN, ItemSizes.DATE_MAX)
fun HomeItem.dateFormat() = DateFormat.entries.firstOrNull { it.name == style } ?: DateFormat.Long

/** Size range of an item for the inspector slider and the pinch gesture. */
fun HomeItem.sizeRange(): IntRange = when (kind) {
    ItemKind.Clock -> ItemSizes.CLOCK_MIN..ItemSizes.CLOCK_MAX
    ItemKind.Date -> ItemSizes.DATE_MIN..ItemSizes.DATE_MAX
    ItemKind.Lens -> ItemSizes.LENS_MIN..ItemSizes.LENS_MAX
    else -> ItemSizes.ICON_MIN..ItemSizes.ICON_MAX
}
fun HomeItem.currentSize(iconDefault: Int): Int = when (kind) {
    ItemKind.Clock -> if (sizeDp == 0) ItemSizes.CLOCK else sizeDp.coerceIn(sizeRange())
    ItemKind.Date -> dateSp()
    ItemKind.Lens -> lensDp()
    else -> iconDp(iconDefault)
}
fun HomeItem.lensDp() = if (sizeDp == 0) ItemSizes.LENS else sizeDp.coerceIn(ItemSizes.LENS_MIN, ItemSizes.LENS_MAX)
fun HomeItem.clockStyle(fallback: ClockStyle) = ClockStyle.entries.firstOrNull { it.name == style } ?: fallback

/** The colour an item asks for; null = the wallpaper-derived ink. */
fun HomeItem.tintColor(accent: Color): Color? = when (tint) {
    ItemTint.Accent.name -> accent
    ItemTint.Light.name -> Color(0xFFF4F5F7)
    ItemTint.Dark.name -> Color(0xFF14161A)
    else -> null
}

/**
 * Contextual editor on Veil Dense (§5.5): 312 dp, above [target] when it fits, else below, never over it;
 * the edge lights from the element's side. [target] and [topLimit] are dp in Home coordinates.
 */
@Composable
fun InspectorCard(target: Zone, screenW: Float, topLimit: Float, avoid: Zone? = null, content: @Composable () -> Unit) {
    val c = LocalCrystal.current
    val density = LocalDensity.current
    var h by remember { mutableIntStateOf(0) }
    val width = minOf(312f, screenW - 24f)
    val hDp = h / density.density
    val x = ((target.left + target.right) / 2f - width / 2f).coerceIn(12f, screenW - 12f - width)
    // Above when it fits and leaves [avoid] (the clock) visible; otherwise below.
    val aboveTop = target.top - 12f - hDp
    val above = aboveTop >= topLimit && (avoid == null || avoid.bottom <= aboveTop || avoid.top >= target.top ||
        avoid.right <= x || avoid.left >= x + width)
    val y = if (above) target.top - 12f - hDp else target.bottom + 12f
    val fx = (((target.left + target.right) / 2f - x) / width).coerceIn(0f, 1f)
    // §5.6: scale 0.86 → 1 from the element, 220 ms (0 ms with Reduced Motion).
    val appear = remember { androidx.compose.animation.core.Animatable(0f) }
    val ms = motionMs(220)
    androidx.compose.runtime.LaunchedEffect(Unit) { appear.animateTo(1f, androidx.compose.animation.core.tween(ms)) }
    Column(Modifier.offset { IntOffset((x * density.density).roundToInt(), (y * density.density).roundToInt()) }
        .width(width.dp).onSizeChanged { h = it.height }.graphicsLayer {
            alpha = if (h == 0) 0f else appear.value
            val k = .86f + .14f * appear.value
            scaleX = k; scaleY = k
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(fx, if (above) 1f else 0f)
        }
        .veilSurface(veil(VeilLevel.Dense), VeilOrigin(fx, if (above) 1f else 0f))
        .pointerInput(Unit) { detectTapGestures() }
        .padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        content()
    }
}

@Composable
internal fun Title(text: String, trailing: (@Composable () -> Unit)? = null) {
    val c = LocalCrystal.current
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f), style = CrystalType.body.copy(fontSize = 15.sp), color = c.fg, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
        trailing?.invoke()
    }
}

/** Label and value above, then − slider + : the buttons move exactly one [step] for fine tuning. */
@Composable
internal fun SliderRow(label: String, value: Float, min: Float, max: Float, step: Float, valueText: String, onChange: (Float) -> Unit) {
    val c = LocalCrystal.current
    fun snap(v: Float) = (((v - min) / step).roundToInt() * step + min).coerceIn(min, max)
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = CrystalType.body.copy(fontSize = 13.sp), color = c.fg2, maxLines = 1)
        Text(valueText, style = CrystalType.body.copy(fontSize = 13.sp), color = c.fg, maxLines = 1)
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        StepButton("−", stringResource(R.string.step_less, label), value > min) { onChange(snap(value - step)) }
        BeadSlider(value, min, max, step, label, valueText, Modifier.weight(1f).padding(horizontal = 4.dp), onChange = { v -> onChange(snap(v)) },
            track = c.fg)
        StepButton("+", stringResource(R.string.step_more, label), value < max) { onChange(snap(value + step)) }
    }
}

@Composable
private fun StepButton(sign: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = LocalCrystal.current
    Box(Modifier.size(40.dp).clip(CircleShape).border(1.dp, if (enabled) c.fg3 else c.fg3.copy(alpha = .4f), CircleShape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center) {
        Text(sign, style = CrystalType.body.copy(fontSize = 18.sp, lineHeight = 18.sp), color = if (enabled) c.fg else c.fg3)
    }
}

@Composable
internal fun <T> Chips(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val c = LocalCrystal.current
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (value, label) ->
            val on = value == selected
            Text(label, Modifier.clip(CircleShape).then(if (on) Modifier.background(c.acc) else Modifier.border(1.dp, c.fg3, CircleShape))
                .selectable(on, role = Role.RadioButton) { onSelect(value) }.heightIn(min = 36.dp).padding(horizontal = 12.dp, vertical = 9.dp),
                style = CrystalType.body.copy(fontSize = 13.sp, lineHeight = 16.sp), color = if (on) c.accInk else c.fg, maxLines = 1)
        }
    }
}

@Composable
internal fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalCrystal.current
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).toggleable(checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = CrystalType.body.copy(fontSize = 14.sp), color = c.fg)
        Canvas(Modifier.size(22.dp)) {
            val s = 1.5.dp.toPx()
            val r = size.minDimension / 2f - s
            drawCircle(if (checked) c.acc else c.fg3, r, style = Stroke(s))
            if (checked) drawCircle(c.acc, r * .55f)
        }
    }
}

@Composable
internal fun Action(label: String, color: Color, onClick: () -> Unit) {
    Text(label, Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onClick).padding(vertical = 12.dp),
        style = CrystalType.body.copy(fontSize = 14.sp), color = color)
}

/** Step-by-step moving without dragging (TalkBack, switch access, fine placement). */
@Composable
private fun MoveButtons(onMove: (Int, Int) -> Unit) {
    val c = LocalCrystal.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.inspect_move), Modifier.width(76.dp), style = CrystalType.body.copy(fontSize = 13.sp), color = c.fg2, maxLines = 1)
        listOf(Triple("←", R.string.move_left, -1 to 0), Triple("↑", R.string.move_up, 0 to -1),
            Triple("↓", R.string.move_down, 0 to 1), Triple("→", R.string.move_right, 1 to 0)).forEach { (arrow, label, d) ->
            val text = stringResource(label)
            Text(arrow, Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button) { onMove(d.first, d.second) }
                .semantics { contentDescription = text }.padding(top = 11.dp),
                style = CrystalType.body.copy(fontSize = 17.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center), color = c.fg)
        }
    }
}

/** Progressive disclosure: the less common options stay folded. */
@Composable
internal fun Advanced(content: @Composable () -> Unit) {
    val c = LocalCrystal.current
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).toggleable(open, role = Role.Button) { open = it },
        verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.inspect_advanced), Modifier.weight(1f), style = CrystalType.body.copy(fontSize = 13.sp), color = c.fg2)
        Text(if (open) "−" else "+", style = CrystalType.body.copy(fontSize = 16.sp), color = c.fg2)
    }
    if (open) content()
}

/** App or FLOW on Home: size, label, remove from Home (never uninstall). */
@Composable
fun ItemInspector(state: HomeEditorState, item: HomeItem, title: String, iconDefault: Int, onMore: (() -> Unit)?, onRemoved: () -> Unit, onChange: () -> Unit) {
    val c = LocalCrystal.current
    Title(title) {
        if (onMore != null) Text(stringResource(R.string.inspect_more), Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onMore)
            .padding(horizontal = 10.dp, vertical = 10.dp), style = CrystalType.body.copy(fontSize = 13.sp), color = c.acc)
    }
    val size = item.iconDp(iconDefault)
    SliderRow(stringResource(R.string.inspect_size), size.toFloat(), ItemSizes.ICON_MIN.toFloat(), ItemSizes.ICON_MAX.toFloat(), 2f, "$size dp") { v ->
        state.edit(item.id, "size") { it.copy(sizeDp = v.roundToInt()) }; onChange()
    }
    MoveButtons { dx, dy -> state.nudge(item.id, dx, dy); onChange() }
    Toggle(stringResource(R.string.inspect_label), item.label) { on -> state.edit(item.id, "label") { it.copy(label = on) }; onChange() }
    Action(stringResource(R.string.inspect_remove), c.fg2) { if (state.remove(item.id)) onRemoved(); onChange() }
}

/** The clock: typeface, size, date first; weight, colour and opacity under Advanced. */
@Composable
fun ClockInspector(state: HomeEditorState, item: HomeItem, fallback: ClockStyle, onRemoved: () -> Unit, onChange: () -> Unit) {
    val style = item.clockStyle(fallback)
    Chips(listOf(ClockStyle.Crystal to stringResource(R.string.clock_crystal), ClockStyle.Serif to stringResource(R.string.clock_serif),
        ClockStyle.Grotesk to stringResource(R.string.clock_grotesk), ClockStyle.Mono to stringResource(R.string.clock_mono)), style) { v ->
        state.edit(item.id, "style") { it.copy(style = v.name) }; onChange()
    }
    val size = if (item.sizeDp == 0) ItemSizes.CLOCK else item.sizeDp.coerceIn(ItemSizes.CLOCK_MIN, ItemSizes.CLOCK_MAX)
    SliderRow(stringResource(R.string.inspect_size), size.toFloat(), ItemSizes.CLOCK_MIN.toFloat(), ItemSizes.CLOCK_MAX.toFloat(), 2f, "$size") { v ->
        state.edit(item.id, "size") { it.copy(sizeDp = v.roundToInt()) }; onChange()
    }
    Toggle(stringResource(R.string.inspect_date), state.layout.items.any { it.kind == ItemKind.Date }) { on -> state.setDate(on); onChange() }
    Advanced {
        val weight = if (item.weight == 0) defaultWeight(style) else item.weight
        SliderRow(stringResource(R.string.inspect_weight), weight.toFloat(), 100f, 700f, 50f, "$weight") { v ->
            state.edit(item.id, "weight") { it.copy(weight = v.roundToInt()) }; onChange()
        }
        TintAndOpacity(state, item, onChange)
    }
    Action(stringResource(R.string.clock_hide), LocalCrystal.current.fg2) { if (state.remove(item.id)) onRemoved(); onChange() }
}

@Composable
private fun TintAndOpacity(state: HomeEditorState, item: HomeItem, onChange: () -> Unit) {
    Chips(listOf("" to stringResource(R.string.tint_auto), ItemTint.Accent.name to stringResource(R.string.tint_accent),
        ItemTint.Light.name to stringResource(R.string.tint_light), ItemTint.Dark.name to stringResource(R.string.tint_dark)), item.tint) { v ->
        state.edit(item.id, "tint") { it.copy(tint = v) }; onChange()
    }
    val pct = (item.alpha * 100).roundToInt()
    SliderRow(stringResource(R.string.inspect_opacity), pct.toFloat(), 30f, 100f, 5f, "$pct %") { v ->
        state.edit(item.id, "alpha") { it.copy(alpha = v / 100f) }; onChange()
    }
}

/** The date on its own (§5.5): format, size, pinned under the clock or free; colour and opacity under Advanced. */
@Composable
fun DateInspector(state: HomeEditorState, item: HomeItem, shownFy: Float?, onRemoved: () -> Unit, onChange: () -> Unit) {
    Chips(listOf(DateFormat.Long to stringResource(R.string.date_long), DateFormat.Short to stringResource(R.string.date_short),
        DateFormat.Weekday to stringResource(R.string.date_weekday)), item.dateFormat()) { v ->
        state.edit(item.id, "style") { it.copy(style = if (v == DateFormat.Long) "" else v.name) }; onChange()
    }
    val size = item.dateSp()
    SliderRow(stringResource(R.string.inspect_size), size.toFloat(), ItemSizes.DATE_MIN.toFloat(), ItemSizes.DATE_MAX.toFloat(), 1f, "$size") { v ->
        state.edit(item.id, "size") { it.copy(sizeDp = v.roundToInt()) }; onChange()
    }
    Toggle(stringResource(R.string.date_pinned), item.anchor.type == AnchorType.Bound) { on -> state.pinDate(on, shownFy); onChange() }
    Advanced { TintAndOpacity(state, item, onChange) }
    Action(stringResource(R.string.date_hide), LocalCrystal.current.fg2) { if (state.remove(item.id)) onRemoved(); onChange() }
}

fun defaultWeight(style: ClockStyle) = if (style == ClockStyle.Crystal) 220 else 300

/** LENS: ring size and style only; its touch area and ARC are unchanged (the ghost rings show ARC). */
@Composable
fun LensInspector(state: HomeEditorState, item: HomeItem, reach: Reach, onReach: (Reach) -> Unit, onChange: () -> Unit) {
    val c = LocalCrystal.current
    val size = item.lensDp()
    SliderRow(stringResource(R.string.inspect_size), size.toFloat(), ItemSizes.LENS_MIN.toFloat(), ItemSizes.LENS_MAX.toFloat(), 2f, "$size dp") { v ->
        state.edit(item.id, "size") { it.copy(sizeDp = v.roundToInt()) }; onChange()
    }
    val style = LensStyle.entries.firstOrNull { it.name == item.style } ?: LensStyle.Ring
    Chips(listOf(LensStyle.Ring to stringResource(R.string.lens_ring), LensStyle.Filled to stringResource(R.string.lens_filled)), style) { v ->
        state.edit(item.id, "style") { it.copy(style = if (v == LensStyle.Ring) "" else v.name) }; onChange()
    }
    Chips(listOf(Reach.Left to stringResource(R.string.reach_left), Reach.Center to stringResource(R.string.reach_center),
        Reach.Right to stringResource(R.string.reach_right)), reach, onReach)
    Text(stringResource(R.string.lens_hint), Modifier.padding(vertical = 6.dp), style = CrystalType.body.copy(fontSize = 12.sp), color = c.fg2)
}

/** Tap on an empty spot: what can be added there (§5.5). Widgets are reserved. */
@Composable
fun AddMenu(at: Pt, screenW: Float, topLimit: Float, avoid: Zone?, onApp: () -> Unit, onFlow: () -> Unit, onClock: (() -> Unit)?, onDate: (() -> Unit)?) {
    val c = LocalCrystal.current
    InspectorCard(Zone(at.x - 4f, at.y - 4f, at.x + 4f, at.y + 4f), screenW, topLimit, avoid) {
        Text(stringResource(R.string.add_title), Modifier.padding(vertical = 6.dp), style = CrystalType.body.copy(fontSize = 13.sp), color = c.fg2)
        Action(stringResource(R.string.add_app), c.fg, onApp)
        Action(stringResource(R.string.flow), c.fg, onFlow)
        if (onClock != null) Action(stringResource(R.string.add_clock), c.fg, onClock)
        if (onDate != null) Action(stringResource(R.string.add_date), c.fg, onDate)
        Text(stringResource(R.string.add_widget), Modifier.padding(vertical = 12.dp), style = CrystalType.body.copy(fontSize = 14.sp), color = c.fg3)
    }
}

/** A list of apps or FLOWs to add, on Veil Dense from the bottom. */
@Composable
fun AddPicker(title: String, apps: List<AppEntry>, flows: List<FlowAlias>, onApp: (AppEntry) -> Unit, onFlow: (FlowAlias) -> Unit, onDismiss: () -> Unit) {
    val c = LocalCrystal.current
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).veilSurface(veil(VeilLevel.Dense), VeilOrigin.Bottom)
            .padding(vertical = 12.dp)) {
            Text(title, Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = CrystalType.body.copy(fontSize = 17.sp), color = c.fg)
            if (apps.isEmpty() && flows.isEmpty()) Text(stringResource(R.string.pick_none), Modifier.padding(20.dp),
                style = CrystalType.body.copy(fontSize = 14.sp), color = c.fg2)
            LazyColumn {
                items(apps, key = { "a" + it.key }) { app ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button) { onApp(app) }.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(app, Modifier.size(32.dp), tint = c.fg)
                        Text(app.label, Modifier.padding(start = 14.dp), style = CrystalType.body.copy(fontSize = 15.sp), color = c.fg, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
                items(flows, key = { "f" + it.id }) { flow ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button) { onFlow(flow) }.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        HomeGlyph(Glyph.Flow, null, tint = c.fg, size = 24.dp)
                        Text(flow.name, Modifier.padding(start = 18.dp), style = CrystalType.body.copy(fontSize = 15.sp), color = c.fg, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** A preset to choose in the editor bar: thumbnail + name. */
data class PresetChoice(val key: String, val name: String, val spec: LayoutSpec)

/**
 * Preset strip under the bar (§5.5): thumbnails; tapping one previews it in place, Apply keeps it, Cancel returns.
 * Reset goes back to the current preset (with a question, asked by the caller).
 */
@Composable
fun PresetStrip(choices: List<PresetChoice>, previewing: String?, geometry: HomeGeometry, placement: String, onPlacement: () -> Unit,
    onPick: (PresetChoice) -> Unit,
    onApply: () -> Unit, onCancel: () -> Unit, onReset: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalCrystal.current
    Column(modifier.padding(horizontal = 12.dp).fillMaxWidth().veilSurface(veil(VeilLevel.Dense), VeilOrigin(.5f, 1f))
        .pointerInput(Unit) { detectTapGestures() }.padding(12.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            choices.forEach { choice ->
                val on = choice.key == previewing
                Column(Modifier.selectable(on, role = Role.RadioButton) { onPick(choice) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    LayoutThumb(choice.spec, geometry, on)
                    Text(choice.name, Modifier.padding(top = 4.dp).width(64.dp), style = CrystalType.body.copy(fontSize = 11.sp, lineHeight = 14.sp),
                        color = if (on) c.fg else c.fg2, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.preset_reset), Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onReset)
                .padding(horizontal = 10.dp, vertical = 10.dp), style = CrystalType.body.copy(fontSize = 13.sp), color = c.fg2)
            Text(placement, Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onPlacement)
                .padding(horizontal = 10.dp, vertical = 10.dp), style = CrystalType.body.copy(fontSize = 13.sp), color = c.fg)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.preset_cancel), Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onCancel)
                .padding(horizontal = 12.dp, vertical = 10.dp), style = CrystalType.body.copy(fontSize = 13.sp), color = c.fg)
            if (previewing != null) Text(stringResource(R.string.preset_apply), Modifier.padding(start = 6.dp).clip(CircleShape).background(c.acc)
                .clickable(role = Role.Button, onClick = onApply).padding(horizontal = 14.dp, vertical = 10.dp),
                style = CrystalType.body.copy(fontSize = 13.sp), color = c.accInk)
        }
    }
}

/** A miniature of a layout drawn from its own geometry: clock bar, icons as dots, LENS as a ring. */
@Composable
fun LayoutThumb(spec: LayoutSpec, geometry: HomeGeometry, selected: Boolean) {
    val c = LocalCrystal.current
    Canvas(Modifier.size(56.dp, 100.dp).clip(RoundedCornerShape(10.dp)).background(c.fg.copy(alpha = .06f))
        .border(if (selected) 1.5.dp else 1.dp, if (selected) c.acc else c.fg3, RoundedCornerShape(10.dp))) {
        val k = size.width / geometry.width
        val ky = size.height / geometry.height
        fun at(p: Pt) = Offset(p.x * k, p.y * ky)
        spec.items.forEach { item ->
            when {
                item.kind == ItemKind.Clock -> {
                    val list = spec.items.any { it.anchor.type == AnchorType.Stack }
                    val w = size.width * .5f
                    val x = if (list) 6.dp.toPx() else (size.width - w) * item.anchor.fx
                    drawRoundRect(c.fg2, Offset(x, size.height * .14f + size.height * .5f * item.anchor.fy), Size(w, 7.dp.toPx()), CornerRadius(3.dp.toPx()))
                }
                item.kind == ItemKind.Lens -> drawCircle(c.fg, 4.dp.toPx(), at(Pt(geometry.lensX, geometry.lensY)), style = Stroke(1.dp.toPx()))
                item.anchor.type == AnchorType.Stack -> drawRoundRect(c.fg2, Offset(6.dp.toPx(), size.height * .55f + item.anchor.row * 6.dp.toPx()),
                    Size(size.width * .45f, 3.dp.toPx()), CornerRadius(1.5.dp.toPx()))
                else -> geometry.position(item, spec)?.let { drawCircle(c.fg, 2.6.dp.toPx(), at(it)) }
            }
        }
    }
}

/** A yes/no question on Veil Dense. */
@Composable
fun ConfirmDialog(title: String, action: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val c = LocalCrystal.current
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().veilSurface(veil(VeilLevel.Dense), VeilOrigin.Top)
            .padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(title, Modifier.padding(bottom = 12.dp), style = CrystalType.body.copy(fontSize = 17.sp), color = c.fg)
            Action(stringResource(R.string.preset_cancel), c.fg2, onDismiss)
            Action(action, c.acc, onConfirm)
        }
    }
}

/** Faint ARC rings around LENS while its inspector is open (radii 108 / 180 / 250 dp, as on the phone). */
@Composable
fun GhostArc(lens: Pt, modifier: Modifier = Modifier) {
    val c = LocalCrystal.current
    Canvas(modifier) {
        val o = Offset(lens.x * density, lens.y * density)
        listOf(108f, 180f, 250f).forEach { r -> drawCircle(c.fg.copy(alpha = .14f), r * density, o, style = Stroke(1.dp.toPx())) }
    }
}

package dev.intelia.launcher.ui

import androidx.compose.foundation.Canvas
import dev.intelia.launcher.search.AppSearch
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.intelia.launcher.R
import dev.intelia.launcher.action.Action
import dev.intelia.launcher.data.DockEdge
import dev.intelia.launcher.data.Anchor
import dev.intelia.launcher.data.LensAt
import dev.intelia.launcher.data.DockSpec
import dev.intelia.launcher.data.AnchorType
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.ClockBackground
import dev.intelia.launcher.data.ClockLayout
import dev.intelia.launcher.data.ClockSeparator
import dev.intelia.launcher.data.DatePart
import dev.intelia.launcher.data.ItemAlign
import dev.intelia.launcher.data.TextCase
import dev.intelia.launcher.data.TextLook
import dev.intelia.launcher.data.TypeFace
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.verticalScroll
import dev.intelia.launcher.data.ClockStyle
import dev.intelia.launcher.data.DateFormat
import dev.intelia.launcher.data.HomeItem
import dev.intelia.launcher.data.IconStyle
import dev.intelia.launcher.data.ItemKind
import dev.intelia.launcher.data.ItemTint
import dev.intelia.launcher.data.PaletteTarget
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.LayoutSpec
import dev.intelia.launcher.data.LensStyle
import dev.intelia.launcher.flow.FlowAlias
import kotlin.math.roundToInt

/** Default sizes the inspector starts from when [HomeItem.sizeDp] is 0. */
object ItemSizes {
    const val ICON = 40
    const val ICON_MIN = 32
    const val ICON_MAX = 96
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
 * Where the owner moved the editor card and how they sized it, dp. Starts from the saved [CardFrame], and is
 * written back when the layout is saved, so the card opens where the owner left it.
 */
class InspectorFrame(saved: dev.intelia.launcher.data.CardFrame? = null) {
    /** Top-left; null = placed by the element. */
    var pos by mutableStateOf(saved?.let { f -> if (f.x != null && f.y != null) Offset(f.x, f.y) else null })
    var width by mutableFloatStateOf(saved?.w ?: DEFAULT_W)
    /** Most the body may grow before it scrolls; 0 = automatic (58 % of the screen). */
    var maxH by mutableFloatStateOf(saved?.h ?: 0f)
    val moved get() = pos != null || width != DEFAULT_W || maxH != 0f
    fun reset() { pos = null; width = DEFAULT_W; maxH = 0f }
    /** What to keep in settings; null when the card is as placed by the element. */
    fun toCard() = if (!moved) null else dev.intelia.launcher.data.CardFrame(pos?.x, pos?.y, width, maxH)

    companion object {
        const val DEFAULT_W = 312f
        const val MIN_W = 240f
        const val MIN_H = 140f
    }
}

/**
 * An editor card's surface: Dense with its body raised to at least [EDITOR_CARD_BODY], since it floats over a live
 * preview (a huge poster clock must not show through its labels).
 */
@Composable
fun editorVeil(): VeilTokens = veil(VeilLevel.Dense).let { t ->
    t.copy(top = t.top.copy(alpha = maxOf(t.top.alpha, EDITOR_CARD_BODY)), bottom = t.bottom.copy(alpha = maxOf(t.bottom.alpha, EDITOR_CARD_BODY)))
}

const val EDITOR_CARD_BODY = .92f

/**
 * Contextual editor on Veil Dense (§5.5): 312 dp, on the side of [target] with more room ([inspectorAbove]);
 * the edge lights from the element's side. [target] and [topLimit] are screen dp (the editor's [EditView]).
 * The spot is chosen once, when the card opens: it then stays put while its element moves or grows (a slider
 * never runs away under the finger) and is always kept whole on the screen. With a [frame] the card has a
 * header naming what is edited: drag it to move the card, drag the arrows at its corners and sides to size it.
 */
@Composable
fun InspectorCard(target: Zone, screenW: Float, topLimit: Float, avoid: Zone? = null, title: String? = null,
                  frame: InspectorFrame? = null, content: @Composable () -> Unit) {
    val d = LocalDensity.current.density
    // The bottom stops above the navigation bar, so the handles never sit in the gesture area.
    val navBottom = androidx.compose.foundation.layout.WindowInsets.navigationBars.getBottom(LocalDensity.current) / d
    val screenH = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.toFloat() - navBottom
    var h by remember { mutableIntStateOf(0) }
    val hDp = h / d
    val width = (frame?.width ?: InspectorFrame.DEFAULT_W).coerceIn(minOf(InspectorFrame.MIN_W, screenW - 24f), screenW - 24f)
    val room = (screenH - topLimit - 24f).coerceAtLeast(InspectorFrame.MIN_H)
    var auto by remember { mutableStateOf<Offset?>(null) }
    /** When the card fits neither above nor below whole, its body is capped to the roomier side. */
    var autoCap by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    val maxH = (frame?.maxH?.takeIf { it > 0f } ?: minOf(screenH * .58f, autoCap)).coerceIn(InspectorFrame.MIN_H, room)
    var above by remember { mutableStateOf(true) }
    var fx by remember { mutableFloatStateOf(.5f) }
    /** The card's bottom when it opened above its element (dp); it keeps that bottom as its content grows. */
    var anchorBottom by remember { mutableFloatStateOf(0f) }
    if (auto == null && h > 0) {
        val ax = ((target.left + target.right) / 2f - width / 2f).coerceIn(12f, screenW - 12f - width)
        // D15: the side where it fits whole, the roomier one otherwise; above never hides [avoid] (the clock) needlessly.
        above = inspectorAbove(target, hDp, topLimit, screenH, avoid, ax, width)
        val belowRoom = screenH - 12f - target.bottom - 12f
        // Below without room for all of it: the body is capped to that side (the header and margins ≈ 72 dp).
        if (!above && hDp > belowRoom) autoCap = (belowRoom - 72f).coerceAtLeast(InspectorFrame.MIN_H)
        // Above, the card never reaches down over its element: its body is capped to the room up there.
        if (above) autoCap = minOf(autoCap, target.top - 12f - topLimit - 72f).coerceAtLeast(InspectorFrame.MIN_H)
        auto = Offset(ax, if (above) target.top - 12f - minOf(hDp, autoCap + 72f) else target.bottom + 12f)
        anchorBottom = target.top - 12f
        fx = (((target.left + target.right) / 2f - ax) / width).coerceIn(0f, 1f)
    }
    val p = frame?.pos ?: auto ?: Offset(12f, topLimit)
    val x = p.x.coerceIn(12f, maxOf(12f, screenW - 12f - width))
    // Placed above its element, the card grows upwards (a fold opening) so the element stays in sight.
    val anchored = if (frame?.pos == null && above && auto != null) anchorBottom - hDp else p.y
    val y = anchored.coerceIn(topLimit, maxOf(topLimit, screenH - 12f - hDp))
    val at by androidx.compose.runtime.rememberUpdatedState(Offset(x, y))
    val size by androidx.compose.runtime.rememberUpdatedState(Offset(width, maxH))
    val shown by androidx.compose.runtime.rememberUpdatedState(hDp)
    // §5.6: scale 0.86 → 1 from the element on the Veil curve (0 ms with Reduced Motion).
    val appear = remember { androidx.compose.animation.core.Animatable(0f) }
    val ms = motionMs(160)
    androidx.compose.runtime.LaunchedEffect(Unit) { appear.animateTo(1f, androidx.compose.animation.core.tween(ms, easing = VeilMotion.ease)) }
    Box(Modifier.offset { IntOffset((x * d).roundToInt(), (y * d).roundToInt()) }
        .width(width.dp).onSizeChanged { h = it.height }.graphicsLayer {
            alpha = if (h == 0) 0f else appear.value.coerceAtMost(1f)
            val k = .86f + .14f * appear.value
            scaleX = k; scaleY = k
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(fx, if (above) 1f else 0f)
        }
        .veilSurface(editorVeil(), VeilOrigin(fx, if (above) 1f else 0f))
        .pointerInput(Unit) { detectTapGestures() }) {
        Column(Modifier.animateContentSize(androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow))) {
            if (title != null) CardHeader(title, frame) { delta -> frame?.pos = at + delta / d }
            Column(Modifier.heightIn(max = maxH.dp).verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = if (title != null) 0.dp else 12.dp, bottom = if (frame != null) 20.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // One fold open at a time per card; a new card (another target) starts with all folded.
                androidx.compose.runtime.CompositionLocalProvider(LocalFoldGroup provides remember(title) { FoldGroup() }) { content() }
            }
        }
        if (frame != null) {
            fun resize(left: Boolean, right: Boolean, down: Boolean, delta: Offset) {
                val dx = delta.x / d
                if (right) frame.width = (size.x + dx).coerceIn(InspectorFrame.MIN_W, screenW - 24f)
                if (left) {
                    val w = (size.x - dx).coerceIn(InspectorFrame.MIN_W, screenW - 24f)
                    frame.pos = Offset(at.x + size.x - w, at.y); frame.width = w
                }
                // The body is the card less its header; its cap follows the finger from what is shown now.
                if (down) frame.maxH = (minOf(size.y, shown) + delta.y / d).coerceIn(InspectorFrame.MIN_H, room)
            }
            val both = stringResource(R.string.card_size_both)
            val wide = stringResource(R.string.card_size_width)
            val tall = stringResource(R.string.card_size_height)
            ResizeHandle(Alignment.BottomStart, both) { resize(left = true, right = false, down = true, delta = it) }
            ResizeHandle(Alignment.BottomEnd, both) { resize(left = false, right = true, down = true, delta = it) }
            ResizeHandle(Alignment.CenterStart, wide) { resize(left = true, right = false, down = false, delta = it) }
            ResizeHandle(Alignment.CenterEnd, wide) { resize(left = false, right = true, down = false, delta = it) }
            ResizeHandle(Alignment.BottomCenter, tall) { resize(left = false, right = false, down = true, delta = it) }
        }
    }
}

/** What is being edited, as a grip: drag it to move the card; ⟲ puts the card back by its element. */
@Composable
private fun CardHeader(title: String, frame: InspectorFrame?, onDrag: (Offset) -> Unit) {
    val c = LocalCrystal.current
    val label = stringResource(R.string.inspect_move_card)
    val reset = stringResource(R.string.inspect_card_reset)
    Column(Modifier.fillMaxWidth().semantics { contentDescription = label }
        .pointerInput(Unit) { detectDragGestures { change, amount -> change.consume(); onDrag(amount) } }
        .padding(start = 16.dp, end = 8.dp, top = 6.dp)) {
        Box(Modifier.align(Alignment.CenterHorizontally).width(36.dp).height(4.dp).clip(CircleShape).background(c.fg3))
        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = CrystalType.body.copy(fontSize = 16.sp), color = c.fg, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            if (frame?.moved == true) Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = reset) { frame.reset() }
                .semantics(mergeDescendants = true) { contentDescription = reset },
                contentAlignment = Alignment.Center) {
                Text("⟲", Modifier.clearAndSetSemantics { }, style = CrystalType.body.copy(fontSize = 18.sp), color = c.fg2)
            }
        }
    }
}

/**
 * A grip on the card's rim inside its own margin, so it never lies over a control: drag it to size the card that
 * way. Only the end corner and end side show a mark; TalkBack gets Bigger / Smaller instead of the drag.
 */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.ResizeHandle(at: Alignment, label: String, onDrag: (Offset) -> Unit) {
    val c = LocalCrystal.current
    val side = at == Alignment.CenterStart || at == Alignment.CenterEnd
    val corner = at == Alignment.BottomStart || at == Alignment.BottomEnd
    val start = at == Alignment.BottomStart || at == Alignment.CenterStart
    val step = 40f * LocalDensity.current.density
    val bigger = stringResource(R.string.size_bigger)
    val smaller = stringResource(R.string.size_smaller)
    // Growing pulls a start-side handle leftwards and an end-side or bottom one right / down.
    fun grow(by: Float) = Offset(if (side || corner) (if (start) -by else by) else 0f, if (side) 0f else by)
    Canvas(Modifier.align(at).size(if (side) 14.dp else if (corner) 28.dp else 56.dp, if (side) 56.dp else 18.dp)
        .semantics {
            contentDescription = label
            customActions = listOf(
                androidx.compose.ui.semantics.CustomAccessibilityAction(bigger) { onDrag(grow(step)); true },
                androidx.compose.ui.semantics.CustomAccessibilityAction(smaller) { onDrag(grow(-step)); true },
            )
        }
        .pointerInput(Unit) { detectDragGestures { change, amount -> change.consume(); onDrag(amount) } }) {
        // Only the end corner and the end side are drawn, as a sign that the card sizes at all; every rim grips.
        if (at != Alignment.BottomEnd && at != Alignment.CenterEnd) return@Canvas
        val ink = c.fg2.copy(alpha = .55f)
        val w = 2.5.dp.toPx()
        val inset = 5.dp.toPx()
        when {
            side -> drawLine(ink, Offset(if (start) inset else size.width - inset, size.height / 2f - 9.dp.toPx()),
                Offset(if (start) inset else size.width - inset, size.height / 2f + 9.dp.toPx()), w, androidx.compose.ui.graphics.StrokeCap.Round)
            corner -> {
                // A quarter arc that follows the card's rounded corner.
                val r = 9.dp.toPx()
                val cx = if (start) inset + r else size.width - inset - r
                val cy = size.height - inset - r
                drawArc(ink, if (start) 90f else 0f, 90f, false, Offset(cx - r, cy - r), Size(2 * r, 2 * r),
                    style = Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
            else -> drawLine(ink, Offset(size.width / 2f - 12.dp.toPx(), size.height - inset), Offset(size.width / 2f + 12.dp.toPx(), size.height - inset),
                w, androidx.compose.ui.graphics.StrokeCap.Round)
        }
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
internal fun SliderRow(label: String, value: Float, min: Float, max: Float, step: Float, valueText: String,
                       inherit: (@Composable () -> Unit)? = null, onChange: (Float) -> Unit) {
    val c = LocalCrystal.current
    fun snap(v: Float) = (((v - min) / step).roundToInt() * step + min).coerceIn(min, max)
    // Label and value on their own line, the slider below: long labels never squeeze or overlap the track.
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = CrystalType.body.copy(fontSize = 13.sp), color = c.fg2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (inherit != null) Box(Modifier.padding(horizontal = 8.dp)) { inherit() }
        Text(valueText, Modifier.clip(CircleShape).background(c.acc.copy(alpha = .14f)).padding(horizontal = 9.dp, vertical = 2.dp),
            style = CrystalType.body.copy(fontSize = 13.sp, lineHeight = 16.sp), color = c.fg, maxLines = 1)
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        StepButton("−", stringResource(R.string.step_less, label), value > min) { onChange(snap(value - step)) }
        BeadSlider(value, min, max, step, label, valueText, Modifier.weight(1f), onChange = { v -> onChange(snap(v)) },
            track = c.fg)
        StepButton("+", stringResource(R.string.step_more, label), value < max) { onChange(snap(value + step)) }
    }
}

/**
 * B5: the inheritance label of [prop] on [item] — "Globální" while it follows the global value, "Vlastní ↺" once the
 * element has its own; a tap on Vlastní returns the global value as one undo step.
 */
@Composable
internal fun InheritChip(prop: ItemProp, state: HomeEditorState, item: HomeItem, onChange: () -> Unit) {
    val c = LocalCrystal.current
    val own = prop.isOwn(item)
    val source = prop.source(item.kind)?.let { t -> lookFoldTitle(t)?.let { stringResource(R.string.settings) + " › " + it } }
    val spoken = when {
        own -> stringResource(R.string.inherit_own_a11y)
        source != null -> stringResource(R.string.inherit_global_from_a11y, source)
        else -> stringResource(R.string.inherit_global_a11y)
    }
    val shape = RoundedCornerShape(14.dp)
    val tap = if (own) Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.inherit_reset_a11y)) {
        if (state.inherit(item.id, prop)) onChange()
    } else Modifier
    Box(Modifier.heightIn(min = 28.dp).clip(shape).border(1.dp, if (own) c.acc else c.fg3, shape).then(tap)
        .semantics { contentDescription = spoken }
        .padding(horizontal = 9.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
        Text(stringResource(if (own) R.string.inherit_own else R.string.inherit_global), Modifier.clearAndSetSemantics { },
            style = CrystalType.body.copy(fontSize = 11.sp, lineHeight = 14.sp), color = if (own) c.acc else c.fg2, maxLines = 1)
    }
}

/** [InheritChip] on its own line, at the top of a fold whose choices it covers (font, colour). */
@Composable
internal fun InheritLine(prop: ItemProp, state: HomeEditorState, item: HomeItem, onChange: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) { InheritChip(prop, state, item, onChange) }
}

@Composable
private fun StepButton(sign: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = LocalCrystal.current
    // A soft disc rather than an outline: it reads as part of the slider, not as a separate control.
    Box(Modifier.size(44.dp).padding(4.dp).clip(CircleShape).background(c.fg.copy(alpha = if (enabled) .08f else .03f))
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center) {
        Text(sign, style = CrystalType.body.copy(fontSize = 17.sp, lineHeight = 17.sp), color = if (enabled) c.fg else c.fg3)
    }
}

@Composable
internal fun <T> Chips(options: List<Pair<T, String>>, selected: T, family: (T) -> androidx.compose.ui.text.font.FontFamily? = { null },
                       onSelect: (T) -> Unit) {
    val c = LocalCrystal.current
    // Wrapped, never scrolled sideways: every option stays in sight however narrow the card is.
    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth().selectableGroup().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (value, label) ->
            val on = value == selected
            Text(label, Modifier.clip(CircleShape).then(if (on) Modifier.background(c.acc) else Modifier.border(1.dp, c.fg3, CircleShape))
                .selectable(on, role = Role.RadioButton) { onSelect(value) }.heightIn(min = 36.dp).padding(horizontal = 12.dp, vertical = 9.dp),
                style = CrystalType.body.copy(fontSize = 13.sp, lineHeight = 16.sp, fontFamily = family(value) ?: CrystalType.body.fontFamily),
                color = if (on) c.accInk else c.fg, maxLines = 1)
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
            Text(arrow, Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button) { onMove(d.first, d.second) }
                .semantics { contentDescription = text }.padding(top = 11.dp),
                style = CrystalType.body.copy(fontSize = 17.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center), color = c.fg)
        }
    }
}

/**
 * The folds of one card or panel: at most one is open, so opening another closes the last (owner, 2026-10-17).
 * Each open fold brings its own group, so folds inside it behave the same way one level down.
 */
class FoldGroup { var open by mutableStateOf<String?>(null) }

val LocalFoldGroup = androidx.compose.runtime.compositionLocalOf<FoldGroup?> { null }

/**
 * A titled section that opens in place; the card grows with a spring (§5.6). Closed it names its current choice;
 * open it sits on an accent-tinted plate with an accent title, so it is clear which part is being edited.
 */
@Composable
internal fun Fold(title: String, summary: String? = null, content: @Composable () -> Unit) {
    val c = LocalCrystal.current
    val group = LocalFoldGroup.current
    var own by remember { mutableStateOf(false) }
    val open = if (group != null) group.open == title else own
    fun toggle(v: Boolean) { if (group != null) group.open = if (v) title else null else own = v }
    val turn by androidx.compose.animation.core.animateFloatAsState(if (open) 45f else 0f, label = "fold")
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(shape)
        .then(if (open) Modifier.background(c.acc.copy(alpha = if (c.dark) .14f else .10f)).border(1.dp, c.acc.copy(alpha = .45f), shape)
            else Modifier)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).toggleable(open, role = Role.Button) { toggle(it) }.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(title.uppercase(), Modifier.weight(1f), style = CrystalType.label, color = if (open) c.acc else c.fg2, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            if (summary != null && !open) Text(summary, Modifier.padding(horizontal = 10.dp), style = CrystalType.caption, color = c.fg3, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text("+", Modifier.graphicsLayer { rotationZ = turn }, style = CrystalType.body.copy(fontSize = 18.sp), color = if (open) c.acc else c.fg2)
        }
        androidx.compose.animation.AnimatedVisibility(open, enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()) {
            Column(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                androidx.compose.runtime.CompositionLocalProvider(LocalFoldGroup provides remember { FoldGroup() }) { content() }
            }
        }
    }
}

private val FACES = listOf(TypeFace.Style to "Auto", TypeFace.Flex to "Flex", TypeFace.Inter to "Inter", TypeFace.SpaceGrotesk to "Space Grotesk",
    TypeFace.DmSerif to "DM Serif", TypeFace.JetBrainsMono to "JetBrains Mono", TypeFace.BodoniModa to "Bodoni", TypeFace.Unbounded to "Unbounded")

/** The clock's typefaces in one list: its four styles first, then the bundled faces (a face overrides the style). */
private fun clockFonts(names: Map<ClockStyle, String>): List<Pair<String, String>> =
    ClockStyle.entries.map { "s:${it.name}" to names.getValue(it) } + FACES.drop(1).map { "f:${it.first.name}" to it.second }

private fun clockFontKey(style: ClockStyle, face: TypeFace) = if (face == TypeFace.Style) "s:${style.name}" else "f:${face.name}"

private fun clockFontFamily(key: String): androidx.compose.ui.text.font.FontFamily? {
    val name = key.substring(2)
    if (key.startsWith("f:")) return faceFamily(TypeFace.valueOf(name), 450)
    return when (ClockStyle.valueOf(name)) {
        ClockStyle.Crystal -> CrystalType.clock.fontFamily
        ClockStyle.Serif -> InkFonts.serif
        ClockStyle.Grotesk -> InkFonts.sans
        ClockStyle.Mono -> InkFonts.mono
    }
}

/** Only faces with a weight axis (or a system family with weights) react to the weight slider. */
fun hasWeights(style: ClockStyle, face: TypeFace) = when (face) {
    TypeFace.Style -> style == ClockStyle.Crystal || style == ClockStyle.Grotesk
    TypeFace.DmSerif -> false
    else -> true
}

/**
 * The plate behind a clock or a date (§5.12): none, Veil (the glass chosen in Look) or one glass of its own;
 * with a plate, how far it stretches past the text (also by its handles on Home).
 */
@Composable
private fun PlateControls(state: HomeEditorState, item: HomeItem, onChange: () -> Unit) {
    Chips(listOf("" to stringResource(R.string.clock_bg_none), ClockBackground.Veil.name to stringResource(R.string.look_veil),
        ClockBackground.Crystal.name to stringResource(R.string.glass_crystal), ClockBackground.Frost.name to stringResource(R.string.glass_frost),
        ClockBackground.Liquid.name to stringResource(R.string.glass_liquid)), item.background) { v ->
        state.edit(item.id, "background") { it.copy(background = v) }; onChange()
    }
    if (item.background.isEmpty()) return
    fun edit(key: String, f: (TextLook) -> TextLook) { state.edit(item.id, key) { it.copy(look = f(it.look)) }; onChange() }
    SliderRow(stringResource(R.string.plate_wide), item.look.padX, 0f, TextLook.MAX_PAD, 2f, "${item.look.padX.roundToInt()} dp") { v -> edit("padX") { it.copy(padX = v) } }
    SliderRow(stringResource(R.string.plate_tall), item.look.padY, 0f, TextLook.MAX_PAD, 2f, "${item.look.padY.roundToInt()} dp") { v -> edit("padY") { it.copy(padY = v) } }
}

@Composable
private fun plateSummary(item: HomeItem) = stringResource(when (item.background) {
    ClockBackground.Veil.name -> R.string.look_veil
    ClockBackground.Crystal.name -> R.string.glass_crystal
    ClockBackground.Frost.name -> R.string.glass_frost
    ClockBackground.Liquid.name -> R.string.glass_liquid
    else -> R.string.clock_bg_none
})

@Composable
private fun TrackingRow(state: HomeEditorState, item: HomeItem, onChange: () -> Unit) {
    SliderRow(stringResource(R.string.type_tracking), item.look.tracking.toFloat(), TextLook.MIN_TRACKING.toFloat(), TextLook.MAX_TRACKING.toFloat(), 1f,
        "${item.look.tracking}") { v -> state.edit(item.id, "tracking") { it.copy(look = it.look.copy(tracking = v.roundToInt())) }; onChange() }
}

/** App or FLOW on Home: its own size and label; moving step by step folded. Style and size of all icons live in Look. */
@Composable
fun ItemInspector(state: HomeEditorState, item: HomeItem, iconDefault: Int, onMore: (() -> Unit)?, onRemoved: () -> Unit, onChange: () -> Unit) {
    val c = LocalCrystal.current
    // The card's header names the app; its options link sits on the right.
    if (onMore != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(stringResource(R.string.inspect_more), Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onMore)
            .padding(horizontal = 10.dp, vertical = 10.dp), style = CrystalType.body.copy(fontSize = 13.sp), color = c.acc)
    }
    val size = item.iconDp(iconDefault)
    SliderRow(stringResource(R.string.icon_this_size), size.toFloat(), ItemSizes.ICON_MIN.toFloat(), ItemSizes.ICON_MAX.toFloat(), 2f, "$size dp",
        inherit = { InheritChip(ItemProp.Size, state, item, onChange) }) { v ->
        state.edit(item.id, "size") { it.copy(sizeDp = v.roundToInt()) }; onChange()
    }
    Toggle(stringResource(R.string.inspect_label), item.label) { on -> state.edit(item.id, "label") { it.copy(label = on) }; onChange() }
    Fold(stringResource(R.string.inspect_move)) { MoveButtons { dx, dy -> state.nudge(item.id, dx, dy); onChange() } }
    Action(stringResource(R.string.inspect_remove), c.fg2) { if (state.remove(item.id)) onRemoved(); onChange() }
}

/** The clock: size and date on top; font, time format, colour and background each in one fold. */
@Composable
fun ClockInspector(state: HomeEditorState, item: HomeItem, fallback: ClockStyle, onRemoved: () -> Unit, onChange: () -> Unit) {
    val style = item.clockStyle(fallback)
    val look = item.look
    fun edit(key: String, f: (TextLook) -> TextLook) { state.edit(item.id, key) { it.copy(look = f(it.look)) }; onChange() }
    val size = if (item.sizeDp == 0) ItemSizes.CLOCK else item.sizeDp.coerceIn(ItemSizes.CLOCK_MIN, ItemSizes.CLOCK_MAX)
    SliderRow(stringResource(R.string.inspect_size), size.toFloat(), ItemSizes.CLOCK_MIN.toFloat(), ItemSizes.CLOCK_MAX.toFloat(), 2f, "$size",
        inherit = { InheritChip(ItemProp.Size, state, item, onChange) }) { v ->
        state.edit(item.id, "size") { it.copy(sizeDp = v.roundToInt()) }; onChange()
    }
    Toggle(stringResource(R.string.inspect_date), state.layout.items.any { it.kind == ItemKind.Date }) { on -> state.setDate(on); onChange() }
    val fonts = clockFonts(mapOf(ClockStyle.Crystal to stringResource(R.string.clock_crystal), ClockStyle.Serif to stringResource(R.string.clock_serif),
        ClockStyle.Grotesk to stringResource(R.string.clock_grotesk), ClockStyle.Mono to stringResource(R.string.clock_mono)))
    val key = clockFontKey(style, look.face)
    Fold(stringResource(R.string.type_font), fonts.first { it.first == key }.second) {
        InheritLine(ItemProp.Font, state, item, onChange)
        Chips(fonts, key, family = ::clockFontFamily) { v ->
            val name = v.substring(2)
            state.edit(item.id, "font") {
                if (v.startsWith("s:")) it.copy(style = name, look = it.look.copy(face = TypeFace.Style))
                else it.copy(look = it.look.copy(face = TypeFace.valueOf(name)))
            }
            onChange()
        }
        if (hasWeights(style, look.face)) {
            val weight = if (item.weight == 0) defaultWeight(style) else item.weight
            SliderRow(stringResource(R.string.inspect_weight), weight.toFloat(), 100f, 700f, 50f, "$weight",
                inherit = { InheritChip(ItemProp.Weight, state, item, onChange) }) { v ->
                state.edit(item.id, "weight") { it.copy(weight = v.roundToInt()) }; onChange()
            }
        }
        TrackingRow(state, item, onChange)
    }
    Fold(stringResource(R.string.clock_shape), if (look.hours12) "12 h" else "24 h") {
        Chips(listOf(false to "24 h", true to "12 h"), look.hours12) { v -> edit("hours12") { it.copy(hours12 = v) } }
        Chips(listOf(ClockLayout.Row to stringResource(R.string.clock_row), ClockLayout.Stack to stringResource(R.string.clock_stack),
            ClockLayout.Slant to stringResource(R.string.clock_slant)), look.layout) { v -> edit("layout") { it.copy(layout = v) } }
        // The separator is drawn only between hours and minutes on one line.
        if (look.layout == ClockLayout.Row) Chips(listOf(ClockSeparator.Colon to ":", ClockSeparator.Dot to "·", ClockSeparator.Bullet to "•",
            ClockSeparator.Bar to "|", ClockSeparator.Space to "␣", ClockSeparator.None to "—"), look.separator) { v -> edit("separator") { it.copy(separator = v) } }
        // Alignment lines the time up with the date under it; without that date there is nothing to line up with.
        if (state.layout.items.any { it.kind == ItemKind.Date && it.anchor.type == AnchorType.Bound }) {
            Kick(stringResource(R.string.clock_align))
            Chips(listOf(ItemAlign.Auto to stringResource(R.string.tint_auto), ItemAlign.Start to stringResource(R.string.align_start),
                ItemAlign.Center to stringResource(R.string.align_center), ItemAlign.End to stringResource(R.string.align_end)), look.align) { v ->
                edit("align") { it.copy(align = v) }
            }
        }
    }
    Fold(stringResource(R.string.fold_colour), "${(item.alpha * 100).roundToInt()} %") { TintAndOpacity(state, item, onChange) }
    Fold(stringResource(R.string.plate_title), plateSummary(item)) { PlateControls(state, item, onChange) }
    Action(stringResource(R.string.clock_hide), LocalCrystal.current.fg2) { if (state.remove(item.id)) onRemoved(); onChange() }
}

/** Colour (auto, the palette's accent for this place, light, dark) and opacity; the accent itself is chosen in Look › Colours. */
@Composable
private fun TintAndOpacity(state: HomeEditorState, item: HomeItem, onChange: () -> Unit) {
    InheritLine(ItemProp.Tint, state, item, onChange)
    Chips(listOf("" to stringResource(R.string.tint_auto), ItemTint.Accent.name to stringResource(R.string.tint_accent),
        ItemTint.Light.name to stringResource(R.string.tint_light), ItemTint.Dark.name to stringResource(R.string.tint_dark)), item.tint) { v ->
        state.edit(item.id, "tint") { it.copy(tint = v) }; onChange()
    }
    val pct = (item.alpha * 100).roundToInt()
    SliderRow(stringResource(R.string.inspect_opacity), pct.toFloat(), 30f, 100f, 5f, "$pct %") { v ->
        state.edit(item.id, "alpha") { it.copy(alpha = v / 100f) }; onChange()
    }
}

/** The date on its own (§5.5): format and size on top; place, font, colour and background folded. */
@Composable
fun DateInspector(state: HomeEditorState, item: HomeItem, shownFy: Float?, onRemoved: () -> Unit, onChange: () -> Unit) {
    Chips(listOf(DateFormat.Long to stringResource(R.string.date_long), DateFormat.Short to stringResource(R.string.date_short),
        DateFormat.Weekday to stringResource(R.string.date_weekday), DateFormat.Custom to stringResource(R.string.date_custom)), item.dateFormat()) { v ->
        state.edit(item.id, "style") { it.copy(style = if (v == DateFormat.Long) "" else v.name) }; onChange()
    }
    val look = item.look
    fun edit(key: String, f: (TextLook) -> TextLook) { state.edit(item.id, key) { it.copy(look = f(it.look)) }; onChange() }
    if (item.dateFormat() == DateFormat.Custom) {
        val short = stringResource(R.string.date_part_short)
        val long = stringResource(R.string.date_part_long)
        Chips(listOf(DatePart.None to "—", DatePart.Short to short, DatePart.Long to long), look.weekday) { v -> edit("weekday") { it.copy(weekday = v) } }
        Chips(listOf(DatePart.None to "—", DatePart.Number to "10.", DatePart.Short to short, DatePart.Long to long), look.month) { v ->
            edit("month") { it.copy(month = v) }
        }
        Toggle(stringResource(R.string.date_day), look.day) { v -> edit("day") { it.copy(day = v) } }
        Toggle(stringResource(R.string.date_year), look.year) { v -> edit("year") { it.copy(year = v) } }
    }
    val size = item.dateSp()
    SliderRow(stringResource(R.string.inspect_size), size.toFloat(), ItemSizes.DATE_MIN.toFloat(), ItemSizes.DATE_MAX.toFloat(), 1f, "$size",
        inherit = { InheritChip(ItemProp.Size, state, item, onChange) }) { v ->
        state.edit(item.id, "size") { it.copy(sizeDp = v.roundToInt()) }; onChange()
    }
    val bound = item.anchor.type == AnchorType.Bound
    Fold(stringResource(R.string.fold_place), stringResource(if (bound) R.string.date_under_clock else R.string.date_free)) {
        Toggle(stringResource(R.string.date_pinned), bound) { on -> state.pinDate(on, shownFy); onChange() }
        if (bound) {
            SliderRow("↔", look.dx, -TextLook.MAX_OFFSET, TextLook.MAX_OFFSET, 2f, "${look.dx.roundToInt()} dp") { v -> edit("dx") { it.copy(dx = v) } }
            SliderRow("↕", look.dy, -TextLook.MAX_OFFSET, TextLook.MAX_OFFSET, 2f, "${look.dy.roundToInt()} dp") { v -> edit("dy") { it.copy(dy = v) } }
        }
    }
    Fold(stringResource(R.string.type_font), FACES.first { it.first == look.face }.second) {
        InheritLine(ItemProp.Font, state, item, onChange)
        Chips(FACES, look.face, family = { f -> faceFamily(f, 450) }) { v -> edit("face") { it.copy(face = v) } }
        TrackingRow(state, item, onChange)
        Chips(listOf(TextCase.Auto to stringResource(R.string.tint_auto), TextCase.Upper to "ABC", TextCase.Lower to "abc"), look.case) { v ->
            edit("case") { it.copy(case = v) }
        }
        Toggle(stringResource(R.string.date_vertical), look.vertical) { v -> edit("vertical") { it.copy(vertical = v) } }
    }
    Fold(stringResource(R.string.fold_colour), "${(item.alpha * 100).roundToInt()} %") { TintAndOpacity(state, item, onChange) }
    // Under the clock the date shares the clock's plate; only a free date has one of its own.
    if (!bound) Fold(stringResource(R.string.plate_title), plateSummary(item)) { PlateControls(state, item, onChange) }
    Action(stringResource(R.string.date_hide), LocalCrystal.current.fg2) { if (state.remove(item.id)) onRemoved(); onChange() }
}

fun defaultWeight(style: ClockStyle) = if (style == ClockStyle.Crystal) 220 else 300

/** LENS: size and style on top; the app row, its spacing and LENS's position folded. The ghost rings show the adapted ARC. */
@Composable
fun LensInspector(state: HomeEditorState, item: HomeItem, dock: DockSpec, onChange: () -> Unit) {
    val c = LocalCrystal.current
    val size = item.lensDp()
    SliderRow(stringResource(R.string.inspect_size), size.toFloat(), ItemSizes.LENS_MIN.toFloat(), ItemSizes.LENS_MAX.toFloat(), 2f, "$size dp",
        inherit = { InheritChip(ItemProp.Size, state, item, onChange) }) { v ->
        state.edit(item.id, "size") { it.copy(sizeDp = v.roundToInt()) }; onChange()
    }
    val style = LensStyle.entries.firstOrNull { it.name == item.style } ?: LensStyle.Ring
    Chips(listOf(LensStyle.Ring to stringResource(R.string.lens_ring), LensStyle.Filled to stringResource(R.string.lens_filled)), style) { v ->
        state.edit(item.id, "style") { it.copy(style = if (v == LensStyle.Ring) "" else v.name) }; onChange()
    }
    val edges = listOf(DockEdge.Bottom to stringResource(R.string.dock_bottom), DockEdge.Top to stringResource(R.string.dock_top),
        DockEdge.Left to stringResource(R.string.dock_left), DockEdge.Right to stringResource(R.string.dock_right))
    // The row's edge and LENS's place in it (§5.5, D13): on a side the row runs down the edge and ARC opens inwards.
    Fold(stringResource(R.string.dock_edge), edges.first { it.first == dock.edge }.second) {
        Chips(edges, dock.edge) { v -> if (state.setDock(dock.copy(edge = v))) onChange() }
        Chips(listOf(LensAt.Start to stringResource(R.string.dock_lens_start), LensAt.Middle to stringResource(R.string.dock_lens_middle),
            LensAt.End to stringResource(R.string.dock_lens_end)), dock.lensAt) { v ->
            if (state.setDock(dock.copy(lensAt = v))) onChange()
        }
    }
    // How far apart the four slots stand (§5.5.3): set by hand, or spread evenly over the edge.
    Fold(stringResource(R.string.dock_spacing), if (dock.spread) stringResource(R.string.dock_spread_short) else "${dock.gap} · ${dock.pitch} dp") {
        Toggle(stringResource(R.string.dock_spread), dock.spread) { v -> if (state.setDock(dock.copy(spread = v))) onChange() }
        if (!dock.spread) {
            SliderRow(stringResource(R.string.dock_gap), dock.gap.toFloat(), DockSpec.MIN_GAP.toFloat(), DockSpec.MAX_GAP.toFloat(), 2f, "${dock.gap} dp") { v ->
                if (state.setDock(dock.copy(gap = v.roundToInt()), "gap")) onChange()
            }
            SliderRow(stringResource(R.string.dock_pitch), dock.pitch.toFloat(), DockSpec.MIN_PITCH.toFloat(), DockSpec.MAX_PITCH.toFloat(), 2f, "${dock.pitch} dp") { v ->
                if (state.setDock(dock.copy(pitch = v.roundToInt()), "pitch")) onChange()
            }
        }
    }
    // The drag's accessible twin (§5.5): the same band, in 4 dp steps that always include the centre.
    val room = (state.lensRoom() / 4f).toInt() * 4f
    val dx = state.lensDx().coerceIn(-room, room)
    val at = dx.roundToInt()
    val where = if (at == 0) stringResource(R.string.lens_centre) else stringResource(R.string.lens_offset, at)
    if (room > 0f) Fold(stringResource(R.string.lens_position), where) {
        SliderRow(stringResource(R.string.lens_position), dx, -room, room, 4f, where) { v ->
            state.edit(item.id, "dx") { it.copy(anchor = it.anchor.copy(dx = v)) }; onChange()
        }
    }
    Text(stringResource(R.string.lens_hint), Modifier.padding(vertical = 6.dp), style = CrystalType.body.copy(fontSize = 12.sp), color = c.fg2)
}

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

/** A list of apps or FLOWs to add to Home: the shared [ActionPicker] with just those two sections. */
@Composable
fun AddPicker(title: String, apps: List<AppEntry>, flows: List<FlowAlias>, onApp: (AppEntry) -> Unit, onFlow: (FlowAlias) -> Unit, onDismiss: () -> Unit) {
    ActionPicker(title, apps, flows, onPick = { action ->
        when (action) {
            is Action.App -> apps.firstOrNull { it.key == action.component }?.let(onApp)
            is Action.Flow -> flows.firstOrNull { it.id == action.id }?.let(onFlow)
            else -> Unit
        }
    }, onDismiss = onDismiss)
}

internal const val PICKER_FILTER_FROM = 8

/** The picker's filter row: diacritics-insensitive, the same ranking as Search; never runs anything. */
@Composable
internal fun PickerFilter(value: String, onChange: (String) -> Unit) {
    val c = LocalCrystal.current
    val label = stringResource(R.string.flow_find_app)
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).heightIn(min = 48.dp).clip(CircleShape)
        .background(c.fg.copy(alpha = 0.06f)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        HomeGlyph(Glyph.Search, null, tint = c.fg2, size = 18.dp)
        BasicTextField(value, onChange, Modifier.weight(1f).padding(start = 10.dp).semantics { contentDescription = label }, singleLine = true,
            cursorBrush = SolidColor(c.acc), textStyle = CrystalType.body.copy(fontSize = 15.sp, color = c.fg),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(label, style = CrystalType.body.copy(fontSize = 15.sp), color = c.fg3)
                inner()
            })
    }
}

/** A preset to choose in the editor bar: thumbnail + name. */
data class PresetChoice(val key: String, val name: String, val spec: LayoutSpec)

/**
 * Preset strip under the bar (§5.5): thumbnails; tapping one previews it in place, Apply keeps it, Cancel returns.
 * Reset goes back to the current preset (with a question, asked by the caller).
 */
@Composable
fun PresetStrip(choices: List<PresetChoice>, previewing: String?, geometry: HomeGeometry,
    onPick: (PresetChoice) -> Unit,
    onApply: () -> Unit, onCancel: () -> Unit, onReset: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalCrystal.current
    // The body of the editor card (InspectorCard), like the Look panel.
    Column(modifier.fillMaxWidth().padding(bottom = 4.dp)) {
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
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.preset_cancel), Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onCancel)
                .padding(horizontal = 12.dp, vertical = 10.dp), style = CrystalType.body.copy(fontSize = 13.sp), color = c.fg)
            if (previewing != null) Text(stringResource(R.string.preset_apply), Modifier.padding(start = 6.dp).clip(CircleShape).background(c.acc)
                .clickable(role = Role.Button, onClick = onApply).padding(horizontal = 14.dp, vertical = 10.dp),
                style = CrystalType.body.copy(fontSize = 13.sp), color = c.accInk)
        }
    }
}

/**
 * A miniature of a layout: the clock as bars sized by its own size and arrangement (on a plate when it has one),
 * the date, icons as dots, named apps as lines and LENS (ring or filled) on its own edge, so presets differ at a glance.
 */
@Composable
fun LayoutThumb(spec: LayoutSpec, screen: HomeGeometry, selected: Boolean) {
    val c = LocalCrystal.current
    val geometry = remember(spec, screen) { screen.forLayout(spec) }
    Canvas(Modifier.size(56.dp, 100.dp).clip(RoundedCornerShape(10.dp)).background(c.fg.copy(alpha = .06f))
        .border(if (selected) 1.5.dp else 1.dp, if (selected) c.acc else c.fg3, RoundedCornerShape(10.dp))) {
        val k = size.width / geometry.width
        val ky = size.height / geometry.height
        val m = 6.dp.toPx()
        val edge = spec.dock.edge
        val lensItem = spec.items.firstOrNull { it.kind == ItemKind.Lens }
        val lens = when (edge) {
            DockEdge.Right -> Offset(size.width - m - 2.dp.toPx(), size.height * .62f)
            DockEdge.Left -> Offset(m + 2.dp.toPx(), size.height * .62f)
            DockEdge.Top -> Offset(geometry.lensX * k, m + 10.dp.toPx())
            DockEdge.Bottom -> Offset(geometry.lensX * k, size.height - 14.dp.toPx())
        }
        val clock = spec.items.firstOrNull { it.kind == ItemKind.Clock }
        val date = spec.items.firstOrNull { it.kind == ItemKind.Date }
        val dateBar = Size(size.width * .32f, 2.dp.toPx())
        var under = Offset.Zero
        if (clock != null) {
            val lines = if (clock.look.layout == ClockLayout.Row) 1 else 2
            val f = ((if (clock.sizeDp == 0) ItemSizes.CLOCK else clock.sizeDp) / ItemSizes.CLOCK.toFloat()).coerceIn(.5f, 1.8f)
            val barH = 5.dp.toPx() * f
            val w = (size.width - 2 * m) * (if (lines == 2) .42f else .62f) * f.coerceAtMost(1.3f)
            val x = m + (size.width - 2 * m - w) * clock.anchor.fx
            val y = 12.dp.toPx() + (size.height * .5f) * clock.anchor.fy
            val blockH = lines * barH + (lines - 1) * 2.dp.toPx()
            if (clock.background.isNotEmpty()) drawRoundRect(c.fg.copy(alpha = .16f), Offset(x - 3.dp.toPx(), y - 3.dp.toPx()),
                Size(w + 6.dp.toPx(), blockH + 9.dp.toPx()), CornerRadius(4.dp.toPx()))
            repeat(lines) { i ->
                val shift = if (clock.look.layout == ClockLayout.Slant) i * w * .25f else 0f
                drawRoundRect(c.fg2, Offset(x + shift, y + i * (barH + 2.dp.toPx())), Size(if (lines == 2) w * .75f else w, barH), CornerRadius(barH / 2f))
            }
            under = Offset(x, y + blockH + 3.dp.toPx())
        }
        if (date != null) {
            val at = if (date.anchor.type == AnchorType.Bound) under
                else Offset(m + (size.width - 2 * m - dateBar.width) * date.anchor.fx, 8.dp.toPx() + size.height * .7f * date.anchor.fy)
            drawRoundRect(c.fg3, at, dateBar, CornerRadius(1.dp.toPx()))
        }
        if (lensItem?.style == LensStyle.Filled.name) drawCircle(c.fg, 4.dp.toPx(), lens) else drawCircle(c.fg, 4.dp.toPx(), lens, style = Stroke(1.dp.toPx()))
        val step = 9.dp.toPx()
        spec.items.forEach { item ->
            when {
                item.kind == ItemKind.Clock || item.kind == ItemKind.Date || item.kind == ItemKind.Lens -> Unit
                item.anchor.type == AnchorType.Stack -> drawRoundRect(c.fg2, Offset(m, size.height * .42f + item.anchor.row * 6.dp.toPx()),
                    Size(size.width * .45f, 3.dp.toPx()), CornerRadius(1.5.dp.toPx()))
                item.anchor.type == AnchorType.LensRow -> {
                    val slot = item.anchor.slot
                    // Index along the row: LENS in the middle, before or after the four slots.
                    val along = when (spec.dock.lensAt) {
                        LensAt.Middle -> slot.toFloat()
                        LensAt.Start -> (Anchor.LENS_SLOTS.indexOf(slot) + 1).toFloat()
                        LensAt.End -> (Anchor.LENS_SLOTS.indexOf(slot) - 4).toFloat()
                    }
                    val p = when {
                        edge.vertical -> Offset(lens.x, lens.y + along * step)
                        spec.dock.spread -> Offset(m + (size.width - 2 * m) * (Anchor.LENS_SLOTS.indexOf(slot) + .5f) / 4f, lens.y - 10.dp.toPx())
                        else -> Offset(lens.x + along * step, lens.y)
                    }
                    drawCircle(c.fg, 2.6.dp.toPx(), p)
                }
                else -> geometry.position(item, spec)?.let { drawCircle(c.fg, 2.6.dp.toPx(), Offset(it.x * k, it.y * ky)) }
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

/** The faint wide ARC above LENS while its inspector is open: the arc and slots ARC would really open with LENS here. */
@Composable
fun GhostArc(lens: Pt, bow: ArcBow, modifier: Modifier = Modifier, edge: DockEdge = DockEdge.Bottom) {
    val c = LocalCrystal.current
    Canvas(modifier) {
        val o = Offset(lens.x * density, lens.y * density)
        val ink = c.fg.copy(alpha = .22f)
        if (bow.angles.isEmpty()) return@Canvas
        val r = bow.curve * density
        val from = bow.angles.first()
        val to = bow.angles.last()
        val c0 = Dock.toScreen(edge, 0f, bow.drop).let { Offset(o.x + it.x * density, o.y + it.y * density) }
        drawArc(ink, -90f + Dock.degrees(edge) + from - 3f, to - from + 6f, false, Offset(c0.x - r, c0.y - r), Size(2 * r, 2 * r), style = Stroke(1.dp.toPx()))
        (0 until bow.count).forEach { i ->
            val (lx, ly) = bow.position(i)
            val p = Dock.toScreen(edge, lx, ly)
            drawCircle(ink, 3.dp.toPx(), Offset(o.x + p.x * density, o.y + p.y * density))
        }
    }
}

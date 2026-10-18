@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.intelia.launcher.ui

import dev.intelia.launcher.data.HapticSurface
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.intelia.launcher.R
import dev.intelia.launcher.data.DockEdge
import dev.intelia.launcher.action.Action
import dev.intelia.launcher.action.Gesture
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.ShortcutEntry
import dev.intelia.launcher.flow.FlowAlias
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** LENS centre sits this far above the navigation inset (4.3); Home layouts align to the same centre. */
val LensLift = 52.dp

/** LENS centre minus the screen centre: the stored [dx] (§5.5), kept at least [LENS_EDGE] from either edge of [width]. */
fun lensShift(dx: Float, width: Dp): Dp {
    val room = (width.value / 2 - LENS_EDGE).coerceAtLeast(0f)
    return dx.coerceIn(-room, room).dp
}

/** Closest the LENS centre gets to a screen edge, dp (x 60–300 on a 360 dp screen). */
const val LENS_EDGE = 60f

/**
 * Centres the content at ([x], [y]) of the parent. The node itself fills the parent: content placed
 * outside a node's own bounds would draw but never receive touches.
 */
internal fun Modifier.centerAt(x: Dp, y: Dp) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
    val height = if (constraints.hasBoundedHeight) constraints.maxHeight else placeable.height
    layout(width, height) {
        placeable.place(x.roundToPx() - placeable.width / 2, y.roundToPx() - placeable.height / 2)
    }
}

/**
 * LENS and its ARC. Tap = search, swipe up = apps, hold = ARC: one wide arc of slots opens just beyond LENS's edge
 * (M2). Slide onto a slot and release to run it; slide past an end of the arc or onto a dot to switch the set;
 * slide back onto LENS (or below it) to cancel. The composable fills the screen so the arc can grow over Home;
 * only LENS takes touches.
 */
/** Longest pause between taps that still counts towards the set a following hold opens (tap + hold). */
private const val TAP_GAP_MS = 280L

@Composable
fun LensArc(
    content: ArcContent,
    /** LENS centre in dp and the edge its row sits on; ARC opens away from that edge (§5.5). */
    lens: Pt,
    edge: DockEdge,
    flowSteps: (FlowAlias) -> List<String?>,
    onSearch: () -> Unit,
    onDrawer: () -> Unit,
    /** A released slot, as an Action Engine action: the caller runs it and reports why when it cannot. */
    onAction: (Action) -> Unit,
    onOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    lensRing: Dp = CrystalDims.lensRing,
    lensFilled: Boolean = false,
    editActions: List<CustomAccessibilityAction> = emptyList(),
    /** In the Home Editor: a click (TalkBack double tap, Enter) selects LENS instead of opening Search; no ARC. */
    onEditSelect: (() -> Unit)? = null,
    /** M4: a bound sideways flick; true when it ran, false lets the touch end as nothing. */
    onFlick: (Gesture) -> Boolean = { false },
    /** LENS's touch box, dp: [Dock.lensTouch] narrows it beside the dock's icons (S4.4). */
    touch: LensTouch = LensTouch.Default,
    /** In a preview ([LocalPreviewMode]): drawn open on the first set, no slot picked; nothing reacts or runs. */
    shown: Boolean = false,
) {
    val preview = LocalPreviewMode.current
    val homeInk = LocalHomeInk.current
    val selectLabel = stringResource(R.string.a11y_select)
    val ink = LocalInk.current
    val accent = ink.accent
    val haptics = rememberInkHaptics(HapticSurface.Arc)
    val density = LocalDensity.current
    val focus = LocalFocusManager.current
    var open by remember { mutableStateOf(shown && preview) }
    /** Opened by TalkBack or a keyboard: slots and set dots become ordinary buttons, nothing follows a finger. */
    var assisted by remember { mutableStateOf(false) }
    var track by remember { mutableStateOf(BowTrack()) }
    val latestContent by rememberUpdatedState(content)
    val openChanged by rememberUpdatedState(onOpenChange)
    val flick by rememberUpdatedState(onFlick)
    val sets = content.sets.size.coerceAtLeast(1)
    val set = track.set.coerceIn(0, sets - 1)

    // A plain hold opens on the first set (taps before it pick another), so the hand always knows where it lands.
    fun close() { open = false; assisted = false; track = BowTrack() }
    fun commit(setIndex: Int, slot: Int?) {
        val chosen = slot?.let { latestContent.sets.getOrNull(setIndex)?.slots?.getOrNull(it) }
        close()
        chosen?.let { haptics.confirm(); onAction(it.action) }
    }

    // A preview never tells Home ARC is open, never closes on stop and never takes Back.
    if (!preview) {
        LaunchedEffect(open) { openChanged(open) }
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) { close() }
        BackHandler(enabled = open) { close() }
    }

    val reveal by animateFloatAsState(if (open) 1f else 0f, tween(motionMs(130)), label = "arc-reveal")
    val searchLabel = stringResource(R.string.search)
    val lensDescription = stringResource(R.string.lens_description)
    val arcAction = stringResource(R.string.arc_action)
    val arcOpen = stringResource(R.string.arc_open)
    val missing = stringResource(R.string.flow_target_missing)
    val setNames = content.sets.mapIndexed { i, v -> setLabel(v, i) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
        val topInset = with(density) { WindowInsets.statusBarsIgnoringVisibility.getTop(this).toDp() }
        val cx = lens.x.dp
        val cy = lens.y.dp
        val look = content.look
        val bow = remember(look, sets, lens, edge, maxWidth, maxHeight) { Dock.bow(look, sets, edge, lens, maxWidth.value, maxHeight.value) }
        fun screen(x: Float, y: Float) = Dock.toScreen(edge, x, y)

        if (reveal > 0f) {
            // Veil only while ARC is open: it is a transient layer, never part of resting Home.
            val veil = if (homeInk.luminance() > .5f) Color.Black.copy(alpha = .58f) else ink.paper.copy(alpha = .78f)
            val veilRadius = (maxOf(bow.reach, bow.width) + 140f).dp
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = reveal }.drawBehind {
                drawRect(Brush.radialGradient(0f to veil, .6f to veil.copy(alpha = veil.alpha * .7f), 1f to Color.Transparent,
                    center = Offset(cx.toPx(), cy.toPx()), radius = veilRadius.toPx()))
            }.then(if (assisted) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
                onClickLabel = stringResource(R.string.close)) { close() } else Modifier))

            val view = content.sets.getOrNull(set)
            val pick = track.pick

            // The arc, and the thread to the chosen slot.
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = reveal }) {
                val center = Offset(cx.toPx(), cy.toPx())
                val r = bow.curve.dp.toPx()
                if (bow.angles.isNotEmpty()) {
                    val start = (bow.angles.first() - 4f) - 90f + Dock.degrees(edge)
                    val sweep = (bow.angles.last() - bow.angles.first()) + 8f
                    val c0 = screen(0f, bow.drop).let { Offset(center.x + it.x.dp.toPx(), center.y + it.y.dp.toPx()) }
                    drawArc(homeInk.copy(alpha = if (pick != null) .4f else .22f), start, sweep, false,
                        topLeft = Offset(c0.x - r, c0.y - r), size = Size(2 * r, 2 * r),
                        style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx()))))
                }
                pick?.let { p ->
                    val (lx, ly) = bow.position(p)
                    val e = screen(lx, ly)
                    val end = Offset(center.x + e.x.dp.toPx(), center.y + e.y.dp.toPx())
                    val length = hypot(end.x - center.x, end.y - center.y)
                    val ux = (end.x - center.x) / length
                    val uy = (end.y - center.y) / length
                    drawLine(accent, Offset(center.x + ux * 20.dp.toPx(), center.y + uy * 20.dp.toPx()),
                        Offset(end.x - ux * 28.dp.toPx(), end.y - uy * 28.dp.toPx()), 1.5.dp.toPx(), StrokeCap.Round)
                }
            }

            // Slots, in priority order (0 = middle of the arc).
            view?.slots?.forEachIndexed { index, slot ->
                if (slot == null) return@forEachIndexed
                val (lx, ly) = bow.position(index)
                val p = screen(lx, ly)
                val chosen = pick == index
                val scale by animateFloatAsState(if (chosen) 1.18f else 1f, tween(motionMs(80)), label = "arc-slot")
                val label = slotLabel(slot)
                Box(Modifier.centerAt(cx + p.x.dp, cy + p.y.dp).graphicsLayer {
                    alpha = reveal
                    scaleX = scale; scaleY = scale
                    // Slots rise out of LENS, away from its edge.
                    val rise = screen(0f, (1f - reveal) * 18f)
                    translationX = rise.x.dp.toPx(); translationY = rise.y.dp.toPx()
                }.then(if (assisted) Modifier.clip(CircleShape).clickable(role = Role.Button) { commit(set, index) }
                    .semantics { contentDescription = label } else Modifier.semantics { hideFromAccessibility() })
                    .size(52.dp), contentAlignment = Alignment.Center) {
                    ActionIcon(slot.action, label, 40.dp, if (chosen) accent else homeInk, app = slot.app, shortcut = slot.shortcut)
                    if (chosen) Canvas(Modifier.size(52.dp)) { drawCircle(accent, size.minDimension / 2f - 1.dp.toPx(), style = Stroke(1.5.dp.toPx())) }
                }
            }

            // Set dots above the middle of the arc: the shown set is filled.
            if (sets > 1) repeat(sets) { k ->
                val (dx, dy) = if (assisted) bow.dot(k, ArcBow.DOT_GAP_ASSISTED) else bow.dot(k)
                val p = screen(dx, dy)
                val name = setNames.getOrElse(k) { "" }
                Box(Modifier.centerAt(cx + p.x.dp, cy + p.y.dp).graphicsLayer { alpha = reveal }
                    .then(if (assisted) Modifier.clip(CircleShape).clickable(role = Role.Tab) { track = BowTrack(k) }
                        .semantics { contentDescription = name; selected = k == set } else Modifier.semantics { hideFromAccessibility() })
                    .size(if (assisted) 48.dp else 28.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(10.dp)) {
                        if (k == set) drawCircle(accent, size.minDimension / 2f)
                        else drawCircle(homeInk.copy(alpha = .6f), size.minDimension / 2f - .75.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                    }
                }
            }

            // Set arrows beside LENS: sliding onto one shows the previous / next set; each names the set it leads to.
            if (sets > 1) listOf(-1, 1).forEach { side ->
                val (ax, ay) = bow.arrow(side) ?: return@forEach
                val p = screen(ax, ay)
                val target = (set + side).mod(sets)
                val name = setNames.getOrElse(target) { "" }
                Column(Modifier.centerAt(cx + p.x.dp, cy + p.y.dp).graphicsLayer { alpha = reveal }
                    .then(if (assisted) Modifier.clip(CircleShape).clickable(role = Role.Tab) { track = BowTrack(target) }
                        .semantics { contentDescription = name } else Modifier.semantics { hideFromAccessibility() })
                    .size(width = 56.dp, height = 48.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Canvas(Modifier.size(12.dp)) {
                        val w = size.width
                        val h = size.height
                        val tip = if (side < 0) w * .25f else w * .75f
                        val back = if (side < 0) w * .7f else w * .3f
                        val stroke = 1.6.dp.toPx()
                        drawLine(homeInk.copy(alpha = .65f), Offset(back, h * .15f), Offset(tip, h * .5f), stroke, StrokeCap.Round)
                        drawLine(homeInk.copy(alpha = .65f), Offset(tip, h * .5f), Offset(back, h * .85f), stroke, StrokeCap.Round)
                    }
                    Text(name, style = InkText.small.copy(fontSize = 10.sp, shadow = homeShadow(homeInk)), color = homeInk.copy(alpha = .65f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // What releasing will do, said small just beyond the arc; a FLOW is always previewed step by step.
            val chosenSlot = pick?.let { view?.slots?.getOrNull(it) }
            val beyond = screen(0f, -(bow.lift + if (sets > 1) ArcBow.DOTS + 40f else 60f))
            // Kept on screen: with LENS on a side edge the label would otherwise hang past it.
            val lx = (lens.x + beyond.x).coerceIn(110f, maxWidth.value - 110f)
            val ly = (lens.y + beyond.y).coerceIn(topInset.value + 40f, maxHeight.value - bottomInset.value - 40f)
            Column(Modifier.centerAt(lx.dp, ly.dp).widthIn(max = 220.dp).graphicsLayer { alpha = reveal },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val kicker = when {
                    chosenSlot != null -> stringResource(if (chosenSlot.app != null || chosenSlot.shortcut != null) R.string.arc_release
                        else R.string.arc_release_run)
                    content.empty -> stringResource(R.string.arc_empty_title)
                    sets > 1 -> setNames.getOrElse(set) { "" }
                    else -> if (assisted) arcOpen else stringResource(R.string.arc_cancel_hint)
                }
                Kicker(kicker, color = homeInk.copy(alpha = .75f))
                if (chosenSlot != null) {
                    Text(slotLabel(chosenSlot), style = InkText.item.copy(fontSize = 19.sp, shadow = homeShadow(homeInk),
                        fontStyle = if (chosenSlot.flow != null) FontStyle.Italic else FontStyle.Normal), color = homeInk,
                        textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    chosenSlot.flow?.let { BeadThread(flowSteps(it), missing, color = homeInk.copy(alpha = .8f), bead = accent) }
                } else if (content.empty) Text(stringResource(R.string.arc_empty), style = InkText.small, color = homeInk.copy(alpha = .75f),
                    textAlign = TextAlign.Center)
            }
        }

        // LENS itself.
        Box(
            Modifier.centerAt(cx + ((touch.right - touch.left) / 2f).dp, cy + ((touch.bottom - touch.top) / 2f).dp).size(touch.width.dp, touch.height.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = "$lensDescription (LENS)"
                    if (open) stateDescription = arcOpen
                    if (editActions.isNotEmpty()) customActions = editActions
                    if (onEditSelect != null) onClick(label = selectLabel) { onEditSelect(); true }
                    else {
                        onClick(label = searchLabel) { close(); onSearch(); true }
                        onLongClick(label = arcAction) { open = true; assisted = true; track = BowTrack(); true }
                    }
                }
                .then(if (preview) Modifier else Modifier.onKeyEvent { event ->
                    // Up opens ARC on key down: otherwise focus search moves away from LENS before the key comes up.
                    if (event.key == Key.DirectionUp && onEditSelect == null) {
                        if (event.type == KeyEventType.KeyDown && !open) { open = true; assisted = true; track = BowTrack() }
                        return@onKeyEvent true
                    }
                    if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter, Key.Spacebar -> { if (onEditSelect != null) onEditSelect() else { close(); onSearch() }; true }
                        Key.Escape -> { close(); true }
                        else -> false
                    }
                }
                .focusable()
                .pointerInput(bow, edge, look.tapSets, sets, touch) {
                    awaitEachGesture {
                        var down = awaitFirstDown(requireUnconsumed = true)
                        // LENS owns this touch: Home's own tap, hold (Studio) and swipe must not see it.
                        down.consume()
                        focus.clearFocus()
                        // Taps before the hold pick the set (tap + hold = set 2, two taps + hold = set 3). A tap waits
                        // for a next one only while that can still matter; otherwise Search opens at once.
                        val counting = look.tapSets && sets > 1 && onEditSelect == null
                        var taps = 0
                        var hold: PointerInputChange?
                        while (true) {
                            hold = awaitLongPressOrCancellation(down.id)
                            if (hold != null) break
                            // Tap or flick: decide on release.
                            var event = currentEvent
                            var tapped = false
                            while (event.changes.size == 1) {
                                val change = event.changes.first()
                                if (change.id != down.id || change.isConsumed) break
                                val delta = change.position - down.position
                                val swipe = with(density) { lensSwipeSearch(delta.x.toDp().value, delta.y.toDp().value) }
                                if (!change.pressed) {
                                    if (swipe) { onDrawer(); return@awaitEachGesture }
                                    val side = with(density) { lensSideFlick(delta.x.toDp().value, delta.y.toDp().value) }
                                    if (side != null && onEditSelect == null && flick(side)) return@awaitEachGesture
                                    tapped = delta.getDistance() < viewConfiguration.touchSlop
                                    break
                                }
                                if (swipe) change.consume()
                                event = awaitPointerEvent()
                            }
                            if (!tapped) return@awaitEachGesture
                            taps++
                            val next = if (counting && taps < sets) withTimeoutOrNull(TAP_GAP_MS) { awaitFirstDown(requireUnconsumed = true) } else null
                            if (next == null) { close(); onSearch(); return@awaitEachGesture }
                            next.consume()
                            down = next
                        }
                        if (hold?.id != down.id || currentEvent.changes.size != 1) return@awaitEachGesture
                        open = true; assisted = false; track = BowTrack(set = taps.coerceAtMost(sets - 1))
                        haptics.confirm()
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.find { it.id == down.id }
                                if (change == null || event.changes.size != 1 || change.isConsumed) { close(); break }
                                val local = Dock.toLocal(edge, with(density) { change.position.x.toDp().value } - touch.left,
                                    with(density) { change.position.y.toDp().value } - touch.top)
                                val x = local.x
                                val y = local.y
                                val before = track
                                val next = bow.track(before, x, y, latestContent::filled)
                                if (look.haptics && ((next.pick != null && next.pick != before.pick) || next.set != before.set)) haptics.tick()
                                track = next
                                change.consume()
                                if (!change.pressed) { commit(next.set, next.pick); break }
                            }
                        } finally {
                            if (open && !assisted) close()
                        }
                    }
                }),
            contentAlignment = Alignment.Center,
        ) {
            LensMark(homeInk, Modifier.offset(((touch.left - touch.right) / 2f).dp, ((touch.top - touch.bottom) / 2f).dp),
                active = open, ring = lensRing, filled = lensFilled)
        }
    }
}

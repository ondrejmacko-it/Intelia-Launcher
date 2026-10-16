package dev.intelia.launcher.ui

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
import androidx.compose.foundation.layout.statusBars
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
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.ShortcutEntry
import dev.intelia.launcher.flow.FlowAlias
import kotlin.math.hypot

/** LENS centre sits this far above the navigation inset (4.3); Home layouts align to the same centre. */
val LensLift = 52.dp

/** LENS centre minus the screen centre: the stored [dx] (§5.5), kept at least [LENS_EDGE] from either edge of [width]. */
fun lensShift(dx: Float, width: Dp): Dp {
    val room = (width.value / 2 - LENS_EDGE).coerceAtLeast(0f)
    return dx.coerceIn(-room, room).dp
}

/** Closest the LENS centre gets to a screen edge, dp (x 60–300 on a 360 dp screen). */
const val LENS_EDGE = 60f

/** Everything ARC can hold, ring by ring, in fixed slots. */
class ArcContent(
    val apps: List<AppEntry?>,
    val flows: List<FlowAlias?>,
    val shortcuts: List<ShortcutEntry?>,
    val rings: Int,
) {
    val slots: List<List<Boolean>> = listOf(apps.map { it != null }, flows.map { it != null }, shortcuts.map { it != null })
        .take(rings.coerceIn(1, 3))
    val empty get() = slots.all { ring -> ring.none { it } }
}

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
 * LENS and its ARC. Tap = search, swipe up = apps, hold = ARC: slide outwards through the rings,
 * release on an item to open it, slide back onto LENS (or below it) to cancel.
 * The composable fills the screen so the rings can grow over Home; only LENS takes touches.
 */
@Composable
fun LensArc(
    content: ArcContent,
    lensDx: Float,
    flowSteps: (FlowAlias) -> List<String?>,
    onSearch: () -> Unit,
    onDrawer: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onRun: (FlowAlias) -> Unit,
    onShortcut: (ShortcutEntry) -> Unit,
    onOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    lensRing: Dp = CrystalDims.lensRing,
    lensFilled: Boolean = false,
    editActions: List<CustomAccessibilityAction> = emptyList(),
) {
    val homeInk = LocalHomeInk.current
    val ink = LocalInk.current
    val accent = ink.accent
    val haptics = rememberInkHaptics()
    val density = LocalDensity.current
    val focus = LocalFocusManager.current
    var open by remember { mutableStateOf(false) }
    /** Opened by TalkBack or a keyboard: items become ordinary buttons, nothing follows a finger. */
    var assisted by remember { mutableStateOf(false) }
    var pick by remember { mutableStateOf<ArcPick?>(null) }
    var hoverRing by remember { mutableStateOf(0) }
    val latestContent by rememberUpdatedState(content)
    val openChanged by rememberUpdatedState(onOpenChange)

    fun close() { open = false; assisted = false; pick = null; hoverRing = 0 }
    fun commit(target: ArcPick?) {
        val c = latestContent
        close()
        when (target?.ring) {
            0 -> c.apps.getOrNull(target.index)?.let { haptics.confirm(); onLaunch(it) }
            1 -> c.flows.getOrNull(target.index)?.let { haptics.confirm(); onRun(it) }
            2 -> c.shortcuts.getOrNull(target.index)?.let { haptics.confirm(); onShortcut(it) }
        }
    }

    LaunchedEffect(open) { openChanged(open) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { close() }
    BackHandler(enabled = open) { close() }

    val reveal by animateFloatAsState(if (open) 1f else 0f, tween(motionMs(180)), label = "arc-reveal")
    val searchLabel = stringResource(R.string.search)
    val lensDescription = stringResource(R.string.lens_description)
    val arcAction = stringResource(R.string.arc_action)
    val arcOpen = stringResource(R.string.arc_open)
    val kinds = listOf(stringResource(R.string.arc_ring_apps), stringResource(R.string.flow), stringResource(R.string.arc_ring_shortcuts))
    val missing = stringResource(R.string.flow_target_missing)

    BoxWithConstraints(modifier.fillMaxSize()) {
        val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
        val topInset = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
        val cx = maxWidth / 2 + lensShift(lensDx, maxWidth)
        val cy = maxHeight - bottomInset - LensLift
        val rings = remember(content.slots, cx, maxWidth) { ArcRings(content.slots, left = cx.value, right = (maxWidth - cx).value) }

        if (reveal > 0f) {
            // Veil only while ARC is open: it is a transient layer, never part of resting Home.
            val veil = if (homeInk.luminance() > .5f) Color.Black.copy(alpha = .62f) else ink.paper.copy(alpha = .82f)
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = reveal }.drawBehind {
                val center = Offset(cx.toPx(), cy.toPx())
                drawRect(Brush.radialGradient(0f to veil, .62f to veil.copy(alpha = veil.alpha * .7f), 1f to Color.Transparent,
                    center = center, radius = 520.dp.toPx()))
            }.then(if (assisted) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
                onClickLabel = stringResource(R.string.close)) { close() } else Modifier))

            val reached = if (assisted) rings.slots.lastIndex else hoverRing.coerceAtLeast(0)
            fun ringAlpha(ring: Int) = when {
                ring <= reached -> 1f
                ring == reached + 1 -> .35f
                else -> 0f
            }

            // Orbits, the thread to the chosen slot, and ring names.
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = reveal }) {
                val center = Offset(cx.toPx(), cy.toPx())
                rings.slots.indices.forEach { ring ->
                    val a = ringAlpha(ring)
                    if (a == 0f) return@forEach
                    val r = rings.radius(ring).dp.toPx()
                    val angles = rings.angles[ring]
                    val start = (angles.first() - 9f) - 90f
                    val sweep = (angles.last() - angles.first()) + 18f
                    drawArc(homeInk.copy(alpha = (if (pick?.ring == ring) .42f else .2f) * a), start, sweep, false,
                        topLeft = Offset(center.x - r, center.y - r), size = Size(2 * r, 2 * r),
                        style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx()))))
                }
                pick?.let { p ->
                    val (x, y) = rings.position(p.ring, p.index)
                    val end = Offset(center.x + x.dp.toPx(), center.y + y.dp.toPx())
                    val length = hypot(end.x - center.x, end.y - center.y)
                    val ux = (end.x - center.x) / length
                    val uy = (end.y - center.y) / length
                    val from = Offset(center.x + ux * 20.dp.toPx(), center.y + uy * 20.dp.toPx())
                    val to = Offset(end.x - ux * 28.dp.toPx(), end.y - uy * 28.dp.toPx())
                    drawLine(accent, from, to, 1.5.dp.toPx(), StrokeCap.Round)
                    drawCircle(accent, 3.dp.toPx(), Offset((from.x + to.x) / 2f, (from.y + to.y) / 2f))
                }
            }
            rings.slots.indices.forEach { ring ->
                val a = ringAlpha(ring)
                if (a > 0f) Kicker(kinds[ring], Modifier.centerAt(cx, cy - rings.radius(ring).dp - 44.dp).graphicsLayer { alpha = a * reveal },
                    color = homeInk.copy(alpha = .7f))
            }

            // Slots.
            rings.slots.forEachIndexed { ring, ringSlots ->
                val a = ringAlpha(ring)
                ringSlots.forEachIndexed { index, filled ->
                    if (!filled) return@forEachIndexed
                    val (x, y) = rings.position(ring, index)
                    val chosen = pick == ArcPick(ring, index)
                    val scale by animateFloatAsState(if (chosen) 1.22f else 1f, tween(motionMs(110)), label = "arc-slot")
                    val itemModifier = Modifier.centerAt(cx + x.dp, cy + y.dp).graphicsLayer {
                        alpha = (if (a == 0f) 0f else a) * reveal
                        scaleX = scale; scaleY = scale
                        translationY = (1f - reveal) * 18.dp.toPx()
                    }
                    val clickModifier = if (assisted) Modifier.clip(CircleShape).clickable(role = Role.Button) { commit(ArcPick(ring, index)) }
                    else Modifier.semantics { hideFromAccessibility() }
                    when (ring) {
                        0 -> content.apps[index]?.let { app ->
                            Box(itemModifier.then(clickModifier).size(52.dp).semantics { contentDescription = app.label },
                                contentAlignment = Alignment.Center) {
                                AppIcon(app, Modifier.size(42.dp), tint = if (chosen) accent else homeInk)
                                if (chosen) Canvas(Modifier.size(52.dp)) { drawCircle(accent, size.minDimension / 2f - 1.dp.toPx(), style = Stroke(1.5.dp.toPx())) }
                            }
                        }
                        1 -> content.flows[index]?.let { alias ->
                            val angle = rings.angles[ring][index]
                            Text(alias.name, itemModifier.then(clickModifier).graphicsLayer { rotationZ = angle * .55f }
                                .widthIn(max = 92.dp).padding(8.dp),
                                style = InkText.item.copy(fontStyle = FontStyle.Italic, fontSize = 20.sp, shadow = homeShadow(homeInk)),
                                color = if (chosen) accent else homeInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        2 -> content.shortcuts[index]?.let { shortcut ->
                            Box(itemModifier.then(clickModifier).size(48.dp).semantics { contentDescription = shortcut.label },
                                contentAlignment = Alignment.Center) {
                                ShortcutIcon(shortcut, Modifier.size(36.dp), tint = if (chosen) accent else homeInk)
                                if (chosen) Canvas(Modifier.size(48.dp)) { drawCircle(accent, size.minDimension / 2f - 1.dp.toPx(), style = Stroke(1.5.dp.toPx())) }
                            }
                        }
                    }
                }
            }

            // What releasing will do, said once and large.
            val chosen = pick
            Column(Modifier.fillMaxWidth().padding(top = topInset + 72.dp, start = 28.dp, end = 28.dp).graphicsLayer { alpha = reveal },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val title: String
                val kicker: String
                var steps: List<String?>? = null
                when (chosen?.ring) {
                    0 -> { title = content.apps[chosen.index]?.label.orEmpty(); kicker = kinds[0] + " · " + stringResource(R.string.arc_release) }
                    1 -> {
                        val alias = content.flows[chosen.index]
                        title = alias?.name.orEmpty(); kicker = kinds[1] + " · " + stringResource(R.string.arc_release)
                        steps = alias?.let(flowSteps)
                    }
                    2 -> { title = content.shortcuts[chosen.index]?.label.orEmpty(); kicker = kinds[2] + " · " + stringResource(R.string.arc_release) }
                    else -> {
                        title = if (content.empty) stringResource(R.string.arc_empty_title) else "ARC"
                        kicker = if (assisted) arcOpen else stringResource(R.string.arc_further)
                    }
                }
                Kicker(kicker, color = if (chosen?.ring == 1) accent else homeInk.copy(alpha = .75f))
                Text(title, style = InkText.title.copy(fontSize = 40.sp, lineHeight = 44.sp, shadow = homeShadow(homeInk),
                    fontStyle = if (chosen?.ring == 1) FontStyle.Italic else FontStyle.Normal), color = homeInk,
                    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                when {
                    steps != null -> BeadThread(steps, missing, color = homeInk.copy(alpha = .8f), bead = accent)
                    chosen == null -> Text(stringResource(if (content.empty) R.string.arc_empty else R.string.arc_cancel_hint),
                        style = InkText.small, color = homeInk.copy(alpha = .75f), textAlign = TextAlign.Center)
                }
            }
        }

        // LENS itself.
        Box(
            Modifier.centerAt(cx, cy).size(CrystalDims.lensTouchW, CrystalDims.lensTouch)
                .semantics {
                    role = Role.Button
                    contentDescription = "$lensDescription (LENS)"
                    if (open) stateDescription = arcOpen
                    if (editActions.isNotEmpty()) customActions = editActions
                    onClick(label = searchLabel) { close(); onSearch(); true }
                    onLongClick(label = arcAction) { open = true; assisted = true; true }
                }
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter, Key.Spacebar -> { close(); onSearch(); true }
                        Key.DirectionUp -> { open = true; assisted = true; true }
                        Key.Escape -> { close(); true }
                        else -> false
                    }
                }
                .focusable()
                .pointerInput(content.slots, cx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        // LENS owns this touch: Home's own tap, hold (Studio) and swipe must not see it.
                        down.consume()
                        focus.clearFocus()
                        val hold = awaitLongPressOrCancellation(down.id)
                        if (hold == null) {
                            // Tap or flick: decide on release.
                            var event = currentEvent
                            while (event.changes.size == 1) {
                                val change = event.changes.first()
                                if (change.id != down.id || change.isConsumed) break
                                val delta = change.position - down.position
                                val swipe = with(density) { lensSwipeSearch(delta.x.toDp().value, delta.y.toDp().value) }
                                if (!change.pressed) {
                                    if (swipe) onDrawer() else if (delta.getDistance() < viewConfiguration.touchSlop) { close(); onSearch() }
                                    break
                                }
                                if (swipe) change.consume()
                                event = awaitPointerEvent()
                            }
                            return@awaitEachGesture
                        }
                        if (hold.id != down.id || currentEvent.changes.size != 1) return@awaitEachGesture
                        open = true; assisted = false; pick = null; hoverRing = 0
                        haptics.confirm()
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.find { it.id == down.id }
                                if (change == null || event.changes.size != 1 || change.isConsumed) { close(); break }
                                val x = with(density) { (change.position.x - size.width / 2f).toDp().value }
                                val y = with(density) { (change.position.y - size.height / 2f).toDp().value }
                                hoverRing = rings.ringAt(x, y) ?: if (hypot(x, y) < ArcRings.DEAD) 0 else hoverRing
                                val next = rings.select(x, y, pick)
                                if (next != null && next != pick) haptics.tick()
                                pick = next
                                change.consume()
                                if (!change.pressed) { commit(pick); break }
                            }
                        } finally {
                            if (open && !assisted) close()
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            LensMark(homeInk, active = open, ring = lensRing, filled = lensFilled)
        }
    }
}

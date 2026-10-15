package dev.intelia.launcher.ui

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.intelia.launcher.R
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.Reach
import kotlin.math.min

/**
 * LENS: the single lower Home control. Tap or upward swipe opens search;
 * hold opens ARC, slide to an app and release to launch, slide back to cancel.
 */
@Composable
fun Lens(
    apps: List<AppEntry?>,
    reach: Reach,
    reducedMotion: Boolean,
    onSearch: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var session by remember { mutableStateOf<ArcGesture?>(null) }
    var lensFocused by remember { mutableStateOf(false) }
    var focusedArc by remember { mutableStateOf<Int?>(null) }
    var keyboardArc by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val focus = LocalFocusManager.current
    val density = LocalDensity.current
    val ink = MaterialTheme.colorScheme.onSurface
    val searchLabel = stringResource(R.string.search)
    val lensDescription = stringResource(R.string.lens_description)
    val arcAction = stringResource(R.string.arc_action)
    val arcOpen = stringResource(R.string.arc_open)

    fun close() { session?.cancel(); session = null; open = false; selected = null }
    fun showSearch() { close(); onSearch() }
    fun showArc(keyboard: Boolean) { close(); open = true; keyboardArc = keyboard }

    LaunchedEffect(open, keyboardArc) {
        if (open && keyboardArc) { withFrameNanos { }; focus.moveFocus(FocusDirection.Up); keyboardArc = false }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { close() }
    val scale by animateFloatAsState(if (open) 1.08f else 1f, tween(if (reducedMotion) 0 else 100), label = "lens-touch")
    val shift = when (reach) { Reach.Left -> (-24).dp; Reach.Right -> 24.dp; Reach.Center -> 0.dp }

    BoxWithConstraints(modifier.fillMaxWidth().height(200.dp).offset(x = shift).onPreviewKeyEvent { event ->
        if (event.key == Key.Escape && open) {
            if (event.type == KeyEventType.KeyUp) { close(); focus.clearFocus() }
            true
        } else false
    }) {
        val span = min(maxWidth.value - if (reach == Reach.Center) 52f else 100f, 300f)
        val centers = arcCenters(apps.size, span).mapIndexed { index, center -> center.takeIf { apps[index] != null } }
        val transition = updateTransition(open, label = "arc-presence")
        val reveal by transition.animateFloat(transitionSpec = { tween(if (reducedMotion) 0 else 140) }, label = "arc-reveal") { if (it) 1f else 0f }
        if (transition.currentState || transition.targetState) {
            if (apps.none { it != null }) Text(stringResource(R.string.arc_empty),
                Modifier.align(Alignment.TopCenter).padding(top = 38.dp, start = 24.dp, end = 24.dp), fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            apps.forEachIndexed { index, app ->
                val center = centers[index]
                if (app != null && center != null) Column(
                    Modifier.align(Alignment.BottomCenter)
                        .offset(x = center.first.dp, y = center.second.dp - 16.dp)
                        .graphicsLayer {
                            alpha = reveal
                            translationY = (1f - reveal) * 12.dp.toPx()
                            scaleX = if ((selected ?: focusedArc) == index) 1.12f else 1f
                            scaleY = scaleX
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if ((selected ?: focusedArc) == index) Text(app.label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = (if (apps.size <= 1) min(span, 160f) else span / (apps.size - 1)).coerceAtLeast(48f).dp)
                            .padding(bottom = 6.dp).clearAndSetSemantics { })
                    IconButton(
                        onClick = { close(); onLaunch(app) }, enabled = open,
                        modifier = Modifier.size(48.dp)
                            .onFocusChanged { if (it.isFocused) focusedArc = index else if (focusedArc == index) focusedArc = null }
                            .semantics { contentDescription = app.label; if (!open) hideFromAccessibility() },
                    ) { AppIcon(app, Modifier.size(44.dp)) }
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).size(64.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .drawBehind {
                    if (lensFocused) drawCircle(ink.copy(alpha = .6f), radius = size.minDimension / 2f - 2.dp.toPx(), style = Stroke(1.dp.toPx()))
                }
                .semantics {
                    role = Role.Button
                    contentDescription = "$lensDescription (LENS)"
                    if (open) stateDescription = arcOpen
                    onClick(label = searchLabel) { showSearch(); true }
                    onLongClick(label = arcAction) { showArc(false); true }
                }
                .onFocusChanged { lensFocused = it.isFocused }
                .onKeyEvent { event ->
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter, Key.Spacebar -> { if (event.type == KeyEventType.KeyUp) showSearch(); true }
                        Key.DirectionUp -> { if (event.type == KeyEventType.KeyUp) showArc(true); true }
                        else -> false
                    }
                }
                .focusable()
                .pointerInput(apps, maxWidth, reach) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        focus.clearFocus()
                        val hold = awaitLongPressOrCancellation(down.id)
                        if (hold == null) {
                            var event = currentEvent
                            while (event.changes.size == 1) {
                                val change = event.changes.first()
                                if (change.id != down.id || change.isConsumed) break
                                val delta = change.position - down.position
                                val swipe = with(density) { lensSwipeSearch(delta.x.toDp().value, delta.y.toDp().value) }
                                if (!change.pressed) {
                                    if (change.changedToUpIgnoreConsumed() && (delta.getDistance() < viewConfiguration.touchSlop || swipe)) showSearch()
                                    break
                                }
                                if (swipe) change.consume()
                                event = awaitPointerEvent()
                            }
                            return@awaitEachGesture
                        }
                        if (hold.id != down.id || currentEvent.changes.size != 1) return@awaitEachGesture
                        val gesture = ArcGesture(centers)
                        session = gesture; open = true; selected = null
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.find { it.id == down.id }
                                if (change == null || event.changes.size != 1 || change.isConsumed) { gesture.cancel(); break }
                                val x = with(density) { (change.position.x - size.width / 2f).toDp().value }
                                val y = with(density) { (change.position.y - size.height / 2f).toDp().value }
                                val previous = selected
                                selected = gesture.move(x, y)
                                if (selected != null && selected != previous) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                change.consume()
                                if (!change.pressed) {
                                    gesture.finish()?.let { apps.getOrNull(it)?.let(onLaunch) }
                                    break
                                }
                            }
                        } finally {
                            gesture.cancel(); session = null; open = false; selected = null
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) { HomeGlyph(Glyph.Search, null) }
    }
}

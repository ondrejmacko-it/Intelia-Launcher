package dev.intelia.launcher.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.intelia.launcher.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The Veil Focus sheet of a Studio page (S1): one sheet with a handle, resting at [SheetStop.Preview],
 * [SheetStop.Balanced] or [SheetStop.Settings]. Only the handle moves it (M0): the list scrolls and flings inside the
 * sheet and never changes its height or closes it. Dragging the handle well below the Preview stop calls [onDismiss]
 * ([SheetStops.release]). Without a [preview] the sheet fills the room and keeps only the dismiss drag.
 *
 * [preview] is drawn at the size it has at the Preview stop ([full] = its room in px) and scaled down evenly as the
 * sheet rises, so it never deforms and is not laid out again on every frame.
 */
@Composable
fun StudioSheet(stop: SheetStop, onStop: (SheetStop) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier,
    preview: (@Composable (full: Float) -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current.density
        val room = constraints.maxHeight.toFloat()
        val stops = remember(room, density, preview != null) {
            if (preview == null) SheetStops(0f, 0f) else SheetStops.of(room, density)
        }
        val shown = if (preview == null) SheetStop.Settings else stop
        var top by remember { mutableFloatStateOf(stops.top(shown)) }
        val scope = rememberCoroutineScope()
        val ms = motionMs(VeilMotion.PAGE_MS + 60)
        val job = remember { arrayOfNulls<Job>(1) }
        fun moveTo(target: Float) {
            job[0]?.cancel()
            job[0] = scope.launch {
                if (ms == 0) top = target else animate(top, target, animationSpec = tween(ms)) { v, _ -> top = v }
            }
        }
        fun settle(target: SheetStop) {
            onStop(target)
            moveTo(stops.top(target))
        }
        // A new room (rotation, a different page's stop) or a stop chosen elsewhere: rest there.
        LaunchedEffect(stops, shown) { moveTo(stops.top(shown)) }
        // Closing takes a clear pull: below the Preview stop, and on a page without a preview at least 40 % of the room.
        val dismissAt = maxOf(stops.preview + DISMISS_DP * density, room * DISMISS_ROOM)
        val drag = rememberDraggableState { d ->
            job[0]?.cancel()
            top = (top + d).coerceIn(0f, dismissAt + DISMISS_DP * density)
        }
        val ink = LocalInk.current
        val more = stringResource(R.string.studio_sheet_more_settings)
        val less = stringResource(R.string.studio_sheet_more_preview)
        val handleLabel = stringResource(R.string.studio_sheet_handle)
        val close = stringResource(R.string.close)
        if (preview != null && top > 1f) {
            Box(Modifier.fillMaxWidth().height(with(LocalDensity.current) { top.toDp() }).clipToBounds()) {
                val full = stops.preview
                val k = (top / full.coerceAtLeast(1f)).coerceIn(0f, 1f)
                Box(Modifier.layout { m, c ->
                    val p = m.measure(c.copy(minHeight = 0, maxHeight = full.roundToInt().coerceAtLeast(0)))
                    layout(c.maxWidth, c.maxHeight) {
                        p.placeWithLayer((c.maxWidth - p.width) / 2, 0) {
                            scaleX = k; scaleY = k; alpha = (k * 2.5f - .25f).coerceIn(0f, 1f)
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(.5f, 0f)
                        }
                    }
                }) { preview(full) }
            }
        }
        Column(Modifier.fillMaxWidth().layout { m, c ->
                val y = top.roundToInt().coerceIn(0, c.maxHeight)
                val p = m.measure(c.copy(minHeight = c.maxHeight - y, maxHeight = c.maxHeight - y))
                layout(c.maxWidth, c.maxHeight) { p.place(0, y) }
            }
            .veilSurface(veil(VeilLevel.Focus), VeilOrigin.Top, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))) {
            // The handle strip along the sheet's top edge, dragged or used through TalkBack's actions.
            Box(Modifier.fillMaxWidth().height(32.dp)
                .draggable(drag, Orientation.Vertical, onDragStopped = { v ->
                    when (val r = stops.release(top, v, FLING_DP * density, dismissAt)) {
                        SheetRelease.Dismiss -> onDismiss()
                        is SheetRelease.Settle -> settle(if (preview == null) SheetStop.Settings else r.stop)
                    }
                })
                .semantics {
                    contentDescription = handleLabel
                    customActions = buildList {
                        if (preview != null && shown != SheetStop.Settings) add(CustomAccessibilityAction(more) {
                            val t = if (shown == SheetStop.Preview) SheetStop.Balanced else SheetStop.Settings
                            onStop(t); true
                        })
                        if (preview != null && shown != SheetStop.Preview) add(CustomAccessibilityAction(less) {
                            val t = if (shown == SheetStop.Settings) SheetStop.Balanced else SheetStop.Preview
                            onStop(t); true
                        })
                        add(CustomAccessibilityAction(close) { onDismiss(); true })
                    }
                }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(36.dp, 4.dp).background(ink.muted.copy(alpha = .6f), CircleShape))
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                .navigationBarsPadding().padding(bottom = 24.dp), content = content)
        }
    }
}

/** The sheet's shared resting stop and where closing it leads, provided by Studio to each of its pages. */
class StudioSheetState(val stop: SheetStop, val onStop: (SheetStop) -> Unit, val onClose: () -> Unit)

val LocalStudioSheet = compositionLocalOf { StudioSheetState(SheetStop.Balanced, {}, {}) }

/** A release faster than this (dp/s) moves the sheet to the next stop its way. */
private const val FLING_DP = 900f

/** How far below the Preview stop the handle has to be dragged to close the page. */
private const val DISMISS_DP = 96f

/** The least share of the room the handle has to be pulled down to close a page. */
private const val DISMISS_ROOM = .4f

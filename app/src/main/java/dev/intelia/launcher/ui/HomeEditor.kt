@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.intelia.launcher.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.intelia.launcher.R
import dev.intelia.launcher.data.AnchorType
import dev.intelia.launcher.data.LayoutSpec
import dev.intelia.launcher.data.Placement

/**
 * Home Editor top bar (D15): cancel, undo and redo, what is edited, and Hotovo, 48 dp Veil Light controls in the
 * margin freed by the editor's 86 % Home ([EditView]), so it never covers a clock at the very top.
 */
@Composable
fun EditorBar(
    /** Plain values: [HomeEditorState] is plain Kotlin, so the caller refreshes after undo/redo. */
    canUndo: Boolean,
    canRedo: Boolean,
    /** The element being edited, named in the middle of the bar; null = nothing selected. */
    editing: String?,
    onCancel: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = LocalInk.current
    val c = LocalCrystal.current
    val veil = veil(VeilLevel.Light)
    Row(modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility).padding(horizontal = 12.dp).padding(top = 4.dp)
        .height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.veilSurface(veil, VeilOrigin.Bottom, CircleShape)) { BarIcon(Glyph.Close, stringResource(R.string.edit_cancel), true, onCancel) }
        Row(Modifier.padding(start = 6.dp).veilSurface(veil, VeilOrigin.Bottom, CircleShape), verticalAlignment = Alignment.CenterVertically) {
            BarIcon(Glyph.Undo, stringResource(R.string.edit_undo), canUndo, onUndo)
            BarIcon(Glyph.Redo, stringResource(R.string.edit_redo), canRedo, onRedo)
        }
        // What is edited now, in the free middle of the bar.
        Box(Modifier.weight(1f).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
            androidx.compose.animation.AnimatedVisibility(editing != null, enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut()) {
                Text(editing.orEmpty(), Modifier.veilSurface(veil, VeilOrigin.Top, CircleShape).padding(horizontal = 12.dp, vertical = 8.dp),
                    style = CrystalType.body.copy(fontSize = 13.sp), color = c.fg, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
        Row(Modifier.height(44.dp).clip(CircleShape).background(ink.accent).clickable(role = Role.Button, onClick = onDone)
            .padding(start = 12.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            HomeGlyph(Glyph.Check, null, tint = c.accInk, size = 18.dp)
            Text(stringResource(R.string.edit_finish), Modifier.padding(start = 6.dp), style = CrystalType.body.copy(fontSize = 14.sp), color = c.accInk)
        }
    }
}

/** Height of [EditorBar] below the status bar, dp. */
const val BAR_HEIGHT = 52f

/** Height of [EditorTray] and its distance from the bottom edge, dp. */
const val TRAY_HEIGHT = 56f
const val TRAY_BOTTOM = 34f

/**
 * The editor's tool tray (D15) in the bottom margin: Přidat, Předlohy, the placement (Mřížka / Volně / Hybrid,
 * a tap cycles it) and Nastavení, which opens Settings with Vzhled first; one Veil Light capsule.
 */
@Composable
fun EditorTray(placement: String, presetsOpen: Boolean, onAdd: () -> Unit, onPresets: () -> Unit,
               onPlacement: () -> Unit, onSettings: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 16.dp).padding(bottom = TRAY_BOTTOM.dp).fillMaxWidth().height(TRAY_HEIGHT.dp)
        .veilSurface(veil(VeilLevel.Light), VeilOrigin.Bottom, RoundedCornerShape(28.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        TrayButton(Glyph.Plus, stringResource(R.string.edit_add), null, onAdd)
        TrayButton(Glyph.Layers, stringResource(R.string.presets), presetsOpen, onPresets)
        TrayButton(Glyph.Grid, placement, null, onPlacement, spoken = stringResource(R.string.fold_place) + ": " + placement,
            clickLabel = stringResource(R.string.tray_cycle))
        TrayButton(Glyph.Settings, stringResource(R.string.settings), null, onSettings)
    }
}

@Composable
private fun RowScope.TrayButton(glyph: Glyph, label: String, open: Boolean?, onClick: () -> Unit,
                                 spoken: String? = null, clickLabel: String? = null) {
    // [open] null = a plain button (TalkBack must not call it a switch); otherwise the panel it toggles.
    val c = LocalCrystal.current
    Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(22.dp))
        .then(if (open == true) Modifier.background(c.acc.copy(alpha = .16f)) else Modifier)
        .then(if (open == null) Modifier.clickable(onClickLabel = clickLabel, role = Role.Button, onClick = onClick)
            else Modifier.toggleable(open, role = Role.Button) { onClick() })
        .then(if (spoken != null) Modifier.semantics(mergeDescendants = true) { contentDescription = spoken } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        HomeGlyph(glyph, null, tint = if (open == true) c.acc else c.fg, size = 20.dp)
        Text(label, style = CrystalType.body.copy(fontSize = 11.5.sp, lineHeight = 13.sp), color = c.fg, maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

@Composable
fun placementLabel(layout: LayoutSpec): String {
    val name = stringResource(when (layout.placement) {
        Placement.Grid -> R.string.placement_grid
        Placement.Free -> R.string.placement_free
        Placement.Hybrid -> R.string.placement_hybrid
    })
    return if (layout.placement == Placement.Free) name else "$name ${layout.grid.cols}×${layout.grid.rows}"
}

/**
 * A faint size dot at an element's lower end corner ([x], [y] in dp; the clock, an icon, LENS): dragging right or
 * down grows it by whole dp. TalkBack gets Bigger / Smaller.
 */
@Composable
fun SizeHandle(x: Float, y: Float, label: String, onResize: (Float) -> Unit) {
    val c = LocalCrystal.current
    val density = LocalDensity.current
    val bigger = stringResource(R.string.size_bigger)
    val smaller = stringResource(R.string.size_smaller)
    // The drag outlives recompositions: always call the latest lambda, which sees the current size.
    val resize by androidx.compose.runtime.rememberUpdatedState(onResize)
    Box(Modifier.offset { IntOffset(((x - 22f) * density.density).roundToInt(), ((y - 22f) * density.density).roundToInt()) }
        .size(44.dp).semantics {
            contentDescription = label
            customActions = listOf(androidx.compose.ui.semantics.CustomAccessibilityAction(bigger) { onResize(4f); true },
                androidx.compose.ui.semantics.CustomAccessibilityAction(smaller) { onResize(-4f); true })
        }
        .pointerInput(Unit) {
            var acc = 0f
            detectDragGestures(onDragStart = { acc = 0f }) { change, amount ->
                change.consume()
                acc += (amount.x + amount.y) / 2f / density.density * .5f
                val step = acc.toInt()
                if (step != 0) { acc -= step; resize(step.toFloat()) }
            }
        }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(c.fg.copy(alpha = .6f)).border(.5.dp, (if (c.dark) Color.Black else Color.White).copy(alpha = .35f), CircleShape))
    }
}

/**
 * Two faint grips on a clock or date plate, sitting on its rim: the right edge stretches it sideways, the bottom
 * edge down (dp). A short bar, not a button, so it never competes with the editor card; TalkBack gets Bigger / Smaller.
 */
@Composable
fun PlateHandles(zone: Zone, onStretch: (Float, Float) -> Unit) {
    val c = LocalCrystal.current
    val density = LocalDensity.current
    val bigger = stringResource(R.string.size_bigger)
    val smaller = stringResource(R.string.size_smaller)
    @Composable
    fun handle(x: Float, y: Float, label: String, sideways: Boolean) {
        fun by(v: Float) = if (sideways) onStretch(v, 0f) else onStretch(0f, v)
        Box(Modifier.offset { IntOffset(((x - 18f) * density.density).roundToInt(), ((y - 18f) * density.density).roundToInt()) }
            .size(36.dp).semantics {
                contentDescription = label
                customActions = listOf(androidx.compose.ui.semantics.CustomAccessibilityAction(bigger) { by(8f); true },
                    androidx.compose.ui.semantics.CustomAccessibilityAction(smaller) { by(-8f); true })
            }
            .pointerInput(sideways) {
                detectDragGestures { change, amount ->
                    change.consume()
                    by((if (sideways) amount.x else amount.y) / density.density)
                }
            }, contentAlignment = Alignment.Center) {
            Box(Modifier.size(if (sideways) 4.dp else 18.dp, if (sideways) 18.dp else 4.dp).clip(CircleShape)
                .background(c.fg.copy(alpha = .55f)).border(.5.dp, (if (c.dark) Color.Black else Color.White).copy(alpha = .35f), CircleShape))
        }
    }
    handle(zone.right - 7f, (zone.top + zone.bottom) / 2f, stringResource(R.string.plate_wider), true)
    handle((zone.left + zone.right) / 2f, zone.bottom - 7f, stringResource(R.string.plate_taller), false)
}

/**
 * Two-finger pinch in the editor: [target] picks the element at the first two fingers' centre (null = ignore),
 * the size follows the finger distance from where it started. Consumes the pointers, so a running drag cancels.
 */
suspend fun androidx.compose.ui.input.pointer.PointerInputScope.pinchToResize(
    target: (Offset) -> dev.intelia.launcher.data.HomeItem?,
    sizeOf: (dev.intelia.launcher.data.HomeItem) -> Int,
    onStart: () -> Unit,
    onSize: (dev.intelia.launcher.data.HomeItem, Int) -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var item: dev.intelia.launcher.data.HomeItem? = null
        var startDistance = 0f
        var startSize = 0
        while (true) {
            val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
            val down = event.changes.filter { it.pressed }
            if (down.isEmpty()) break
            if (down.size < 2) { if (item != null) break else continue }
            val a = down[0].position
            val b = down[1].position
            val distance = (a - b).getDistance()
            if (item == null) {
                item = target((a + b) / 2f) ?: break
                startDistance = distance.coerceAtLeast(1f)
                startSize = sizeOf(item)
                onStart()
            }
            event.changes.forEach { it.consume() }
            val next = (startSize * distance / startDistance).roundToInt()
            item.let { onSize(it, next) }
        }
    }
}

@Composable
private fun BarIcon(glyph: Glyph, label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = LocalCrystal.current
    Box(Modifier.size(44.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        HomeGlyph(glyph, label, tint = if (enabled) c.fg else c.fg3, size = 20.dp)
    }
}

/**
 * Drag feedback and edit marks, drawn under the elements: grid dots while dragging (Grid, Hybrid), alignment guides,
 * the accent target ring, a dashed ring where a displaced icon goes, and faint corner ticks around every icon.
 */
@Composable
fun EditorMarks(state: HomeEditorState, layout: LayoutSpec, modifier: Modifier = Modifier) {
    val c = LocalCrystal.current
    val g = state.geometry
    val drag = state.drag
    Canvas(modifier.fillMaxSize()) {
        val d = density
        fun px(p: Pt) = Offset(p.x * d, p.y * d)
        val tick = 5.dp.toPx()
        val r = 22.dp.toPx()
        val blocked = state.blocked()
        val w = 1.dp.toPx()
        // Corner ticks: the element stays where it is and only gains marks.
        layout.items.forEach { item ->
            if (item.id == drag?.id) return@forEach
            val p = g.position(item, layout) ?: return@forEach
            val o = px(p)
            for (sx in listOf(-1f, 1f)) for (sy in listOf(-1f, 1f)) {
                val corner = Offset(o.x + sx * r, o.y + sy * r)
                drawLine(c.fg2, corner, Offset(corner.x - sx * tick, corner.y), w)
                drawLine(c.fg2, corner, Offset(corner.x, corner.y - sy * tick), w)
            }
        }
        if (drag == null) return@Canvas
        if (layout.placement != Placement.Free) {
            val grid = layout.grid
            for (col in 0 until grid.cols) for (row in 0 until grid.rows) {
                val cell = g.cellCentre(col, row, grid.cols, grid.rows)
                // No dots on the clock or date: nothing can land there.
                if (blocked.none { cell in it }) drawCircle(c.fg3, 1.6.dp.toPx(), px(cell))
            }
        }
        // Faint LENS zone: nothing lands here.
        drawCircle(c.fg3, 44.dp.toPx(), px(Pt(g.lensX, g.lensY)), style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx()))))
        drag.guides.forEach { guide ->
            if (guide.vertical) drawLine(c.acc.copy(alpha = .7f), Offset(guide.at * d, 0f), Offset(guide.at * d, size.height), w)
            else drawLine(c.acc.copy(alpha = .7f), Offset(0f, guide.at * d), Offset(size.width, guide.at * d), w)
        }
        val item = state.item(drag.id) ?: return@Canvas
        if (item.kind != dev.intelia.launcher.data.ItemKind.Clock) {
            val target = g.position(item.copy(anchor = drag.target), layout)
            if (target != null) drawCircle(if (drag.valid) c.acc else Color(0xFFE5484D), r, px(target), style = Stroke(1.5.dp.toPx()))
        }
        drag.displaced?.let { (id, anchor) ->
            val moved = state.item(id) ?: return@let
            val to = g.position(moved.copy(anchor = anchor), layout) ?: return@let
            drawCircle(c.fg2, r, px(to), style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()))))
        }
    }
}

/** True for anchors the editor lays out by geometry (icons); the clock and the legacy list are handled apart. */
fun AnchorType.isPlaced() = this == AnchorType.Cell || this == AnchorType.Free || this == AnchorType.LensRow

/** Back with unsaved edits (§5.4): Pokračovat / Zahodit / Uložit, on a Veil Dense card. */
@Composable
fun LeaveEditorDialog(onKeep: () -> Unit, onDiscard: () -> Unit, onSave: () -> Unit) {
    val c = LocalCrystal.current
    Dialog(onDismissRequest = onKeep) {
        Column(Modifier.widthIn(max = 320.dp).veilSurface(veil(VeilLevel.Dense), VeilOrigin.Top)
            .padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.edit_unsaved), style = CrystalType.date.copy(fontSize = 17.sp, letterSpacing = 0.sp), color = c.fg,
                modifier = Modifier.padding(bottom = 8.dp))
            DialogRow(stringResource(R.string.edit_keep), c.fg, onKeep)
            DialogRow(stringResource(R.string.edit_discard), c.fg2, onDiscard)
            DialogRow(stringResource(R.string.edit_save), c.acc, onSave)
        }
    }
}

@Composable
private fun DialogRow(label: String, color: Color, onClick: () -> Unit) {
    Text(label, Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick)
        .padding(vertical = 13.dp), style = CrystalType.date.copy(fontSize = 15.sp, letterSpacing = 0.sp), color = color)
}

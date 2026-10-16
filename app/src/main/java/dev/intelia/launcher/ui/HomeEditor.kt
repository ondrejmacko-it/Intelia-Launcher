package dev.intelia.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * Home Editor chrome (§5.5 as approved): left = editing history and placement, right = Studio and ✓.
 * Veil Light pills below the status bar, clear of the camera and of the clock (which starts ≥ 72 dp lower).
 */
@Composable
fun EditorBar(
    state: HomeEditorState,
    presetsOpen: Boolean,
    onPresets: () -> Unit,
    onStudio: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = LocalInk.current
    val c = LocalCrystal.current
    val veil = veilTokens(VeilLevel.Light, c.dark, c.acc)
    Row(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.presets),
            Modifier.veilSurface(veil, VeilOrigin.Bottom, CircleShape).heightIn(min = 40.dp)
                .then(if (presetsOpen) Modifier.border(1.dp, c.acc, CircleShape) else Modifier)
                .toggleable(presetsOpen, role = Role.Button) { onPresets() }
                .padding(horizontal = 14.dp, vertical = 11.dp),
            style = CrystalType.date.copy(fontSize = 13.sp, letterSpacing = .2.sp), color = c.fg)
        Row(Modifier.padding(start = 8.dp).veilSurface(veil, VeilOrigin.Bottom, CircleShape), verticalAlignment = Alignment.CenterVertically) {
            BarIcon(Glyph.Undo, stringResource(R.string.edit_undo), state.canUndo, state::undo)
            BarIcon(Glyph.Redo, stringResource(R.string.edit_redo), state.canRedo, state::redo)
        }
        Box(Modifier.weight(1f))
        Box(Modifier.veilSurface(veil, VeilOrigin.Bottom, CircleShape)) {
            BarIcon(Glyph.Studio, stringResource(R.string.studio), true, onStudio)
        }
        Box(Modifier.padding(start = 8.dp).size(40.dp).clip(CircleShape).background(ink.accent)
            .clickable(role = Role.Button, onClick = onDone), contentAlignment = Alignment.Center) {
            HomeGlyph(Glyph.Check, stringResource(R.string.edit_done), tint = c.accInk, size = 20.dp)
        }
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

/** The clock's size handle at its lower end corner; dragging right or down grows it. */
@Composable
fun ClockHandle(zone: Zone, onResize: (Float) -> Unit) {
    val label = stringResource(R.string.clock_resize)
    val c = LocalCrystal.current
    val density = LocalDensity.current
    Box(Modifier.offset { IntOffset(((zone.right - 22f) * density.density).roundToInt(), ((zone.bottom - 22f) * density.density).roundToInt()) }
        .size(44.dp).semantics { contentDescription = label }
        .pointerInput(Unit) {
            var acc = 0f
            detectDragGestures(onDragStart = { acc = 0f }) { change, amount ->
                change.consume()
                acc += (amount.x + amount.y) / 2f / density.density * .5f
                val step = acc.toInt()
                if (step != 0) { acc -= step; onResize(step.toFloat()) }
            }
        }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(c.acc).border(2.dp, c.accInk, CircleShape))
    }
}

@Composable
private fun BarIcon(glyph: Glyph, label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = LocalCrystal.current
    Box(Modifier.size(40.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
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
        Column(Modifier.widthIn(max = 320.dp).veilSurface(veilTokens(VeilLevel.Dense, c.dark, c.acc), VeilOrigin.Top)
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

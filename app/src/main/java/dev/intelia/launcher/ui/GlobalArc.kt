package dev.intelia.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.intelia.launcher.R
import dev.intelia.launcher.action.ArcLook
import dev.intelia.launcher.action.GlobalArcLook
import dev.intelia.launcher.data.DockEdge
import dev.intelia.launcher.flow.FlowAlias
import java.util.Locale
import kotlin.math.hypot

/** Widest the release label under Global ARC gets, dp. */
private const val GLOBAL_LABEL_MAX = 300f

/**
 * Global ARC geometry (M3.3): the ARC V2 bow opened downwards from the camera, as if LENS sat on the top edge.
 * One set only: above the camera there is no room for set dots or arrows. Pure; dp in screen coordinates.
 */
object GlobalArcGeometry {
    fun bow(look: ArcLook, camera: Pt, width: Float, height: Float): ArcBow = Dock.bow(look, 1, DockEdge.Top, camera, width, height)

    /** The filled slot under the finger at screen ([x], [y]), keeping [previous] while near it; near the camera none. */
    fun pick(bow: ArcBow, camera: Pt, x: Float, y: Float, filled: List<Boolean>, previous: Int?): Int? =
        Dock.toLocal(DockEdge.Top, x - camera.x, y - camera.y).let { bow.select(it.x, it.y, filled, previous) }

    /** Centre of priority slot [slot] on screen. */
    fun slot(bow: ArcBow, camera: Pt, slot: Int): Pt = bow.position(slot).let { (lx, ly) ->
        Dock.toScreen(DockEdge.Top, lx, ly).let { Pt(camera.x + it.x, camera.y + it.y) }
    }
}

/**
 * Draws Global ARC under the camera at [camera] (screen dp): the veil, the dashed bow, the thread to [pick] and the
 * slots of [set]. It never takes touches while a finger drives it; the PULSE gesture that opened it keeps tracking.
 * With [onSlot] (TalkBack, no finger to follow) slots become buttons and a tap outside calls [onDismiss].
 * [overApps]: drawn above another app (the PULSE service), so the whole screen dims and the ink is light, whatever
 * that app shows, by [dim] (0–1).
 */
@Composable
fun GlobalArc(
    set: ArcSetView?,
    look: ArcLook,
    camera: Pt,
    open: Boolean,
    pick: Int?,
    flowSteps: (FlowAlias) -> List<String?>,
    onSlot: ((Int) -> Unit)? = null,
    onDismiss: () -> Unit = {},
    overApps: Boolean = false,
    dim: Float = GlobalArcLook.DIM_M / 100f,
) {
    val reveal by animateFloatAsState(if (open) 1f else 0f, tween(motionMs(130)), label = "global-arc")
    if (reveal <= 0f) return
    val homeInk = if (overApps) Color.White else LocalHomeInk.current
    val ink = LocalInk.current
    val accent = ink.accent
    val missing = stringResource(R.string.flow_target_missing)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val bow = remember(look, camera, maxWidth, maxHeight) { GlobalArcGeometry.bow(look, camera, maxWidth.value, maxHeight.value) }
        val veil = if (homeInk.luminance() > .5f) Color.Black.copy(alpha = .58f) else ink.paper.copy(alpha = .78f)
        val veilRadius = (maxOf(bow.reach, bow.width) + 140f).dp
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = reveal }.drawBehind {
            if (overApps) drawRect(Color.Black.copy(alpha = dim))
            drawRect(Brush.radialGradient(0f to veil, .6f to veil.copy(alpha = veil.alpha * .7f), 1f to Color.Transparent,
                center = Offset(camera.x.dp.toPx(), camera.y.dp.toPx()), radius = veilRadius.toPx()))
        }.then(if (onSlot != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
            onClickLabel = stringResource(R.string.close), onClick = onDismiss) else Modifier))

        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = reveal }) {
            val center = Offset(camera.x.dp.toPx(), camera.y.dp.toPx())
            val r = bow.curve.dp.toPx()
            if (bow.angles.isNotEmpty()) {
                val start = (bow.angles.first() - 4f) - 90f + Dock.degrees(DockEdge.Top)
                val sweep = (bow.angles.last() - bow.angles.first()) + 8f
                // The bow's circle centre sits [ArcBow.drop] behind LENS in ARC's local frame: above the camera here.
                val c0 = Dock.toScreen(DockEdge.Top, 0f, bow.drop).let { Offset(center.x + it.x.dp.toPx(), center.y + it.y.dp.toPx()) }
                drawArc(homeInk.copy(alpha = if (pick != null) .4f else .22f), start, sweep, false,
                    topLeft = Offset(c0.x - r, c0.y - r), size = Size(2 * r, 2 * r),
                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx()))))
            }
            pick?.let { p ->
                val s = GlobalArcGeometry.slot(bow, camera, p)
                val end = Offset(s.x.dp.toPx(), s.y.dp.toPx())
                val length = hypot(end.x - center.x, end.y - center.y)
                val ux = (end.x - center.x) / length
                val uy = (end.y - center.y) / length
                drawLine(accent, Offset(center.x + ux * 20.dp.toPx(), center.y + uy * 20.dp.toPx()),
                    Offset(end.x - ux * 28.dp.toPx(), end.y - uy * 28.dp.toPx()), 1.5.dp.toPx(), StrokeCap.Round)
            }
        }

        set?.slots?.forEachIndexed { index, slot ->
            if (slot == null) return@forEachIndexed
            val p = GlobalArcGeometry.slot(bow, camera, index)
            val chosen = pick == index
            val scale by animateFloatAsState(if (chosen) 1.18f else 1f, tween(motionMs(80)), label = "global-arc-slot")
            val label = slotLabel(slot)
            Box(Modifier.centerAt(p.x.dp, p.y.dp).graphicsLayer {
                alpha = reveal
                scaleX = scale; scaleY = scale
                // Slots drop out of the camera.
                translationY = -((1f - reveal) * 18f).dp.toPx()
            }.then(if (onSlot != null) Modifier.clip(CircleShape).clickable(role = Role.Button) { onSlot(index) }
                .semantics { contentDescription = label } else Modifier.semantics { hideFromAccessibility() })
                .size(52.dp), contentAlignment = Alignment.Center) {
                ActionIcon(slot.action, label, 40.dp, if (chosen) accent else homeInk, app = slot.app, shortcut = slot.shortcut)
                if (chosen) Canvas(Modifier.size(52.dp)) { drawCircle(accent, size.minDimension / 2f - 1.dp.toPx(), style = Stroke(1.5.dp.toPx())) }
            }
        }

        // What releasing will do, said just below the arc; a FLOW is always previewed step by step.
        val chosenSlot = pick?.let { set?.slots?.getOrNull(it) }
        val empty = set == null || set.slots.all { it == null }
        val half = minOf(GLOBAL_LABEL_MAX / 2f, maxWidth.value / 2f - 16f)
        val lx = camera.x.coerceIn(half + 16f, maxWidth.value - half - 16f)
        val ly = camera.y + bow.lift + 64f
        Column(Modifier.centerAt(lx.dp, ly.dp).widthIn(max = (2 * half).dp).graphicsLayer { alpha = reveal },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val kicker = when {
                chosenSlot != null -> stringResource(if (chosenSlot.app != null || chosenSlot.shortcut != null) R.string.arc_release
                    else R.string.arc_release_run)
                empty -> stringResource(R.string.arc_empty_title)
                onSlot != null -> stringResource(R.string.arc_open)
                else -> stringResource(R.string.global_arc_cancel_hint)
            }
            // Two lines, centred: a hint must never be cut off (it was „Návrat ke kameře gesto zruš…“).
            Text(kicker.uppercase(Locale.getDefault()), style = InkText.label, color = homeInk.copy(alpha = .75f),
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (chosenSlot != null) {
                Text(slotLabel(chosenSlot), style = InkText.item.copy(fontSize = 19.sp, shadow = homeShadow(homeInk),
                    fontStyle = if (chosenSlot.flow != null) FontStyle.Italic else FontStyle.Normal), color = homeInk,
                    textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                chosenSlot.flow?.let { BeadThread(flowSteps(it), missing, color = homeInk.copy(alpha = .8f), bead = accent) }
            } else if (empty) Text(stringResource(R.string.arc_empty), style = InkText.small, color = homeInk.copy(alpha = .75f),
                textAlign = TextAlign.Center)
        }
    }
}

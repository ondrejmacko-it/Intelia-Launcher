package dev.intelia.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.intelia.launcher.R

// Crystal design system (D10, docs/DESIGN_SYSTEM.md 4.1–4.4). Phase B1: tokens and components only;
// nothing on Home, ARC, PULSE or Studio uses them yet, so the app looks and behaves as before.

/** Fixed colours of 4.1; accents are chosen per user through [accentColor]. */
object CrystalColors {
    val FgDark = Color(0xFFF1F3F5)
    val FgLight = Color(0xFF15181C)
    val MatDark = Color(0xFF111317).copy(alpha = .90f)
    val MatLight = Color(0xFFF6F7F8).copy(alpha = .92f)
    val FrostDark = Color(0xFFA9C4E0)
    val FrostLight = Color(0xFF3F6E9C)
    val SilverDark = Color(0xFFC9CED4)
    val SilverLight = Color(0xFF59616A)
    val Capsule = Color.Black
}

@Immutable
data class CrystalPalette(
    val dark: Boolean,
    val fg: Color,
    val fg2: Color,
    val fg3: Color,
    val mat: Color,
    val matEdge: Color,
    val line: Color,
    val acc: Color,
    val accInk: Color,
    val capsule: Color = CrystalColors.Capsule,
    /** The arc hairline sits on the wallpaper, so it stays light in both themes. */
    val arc: Color = Color.White.copy(alpha = .22f),
)

fun crystalPalette(dark: Boolean, acc: Color): CrystalPalette {
    val fg = if (dark) CrystalColors.FgDark else CrystalColors.FgLight
    return CrystalPalette(
        dark = dark,
        fg = fg,
        fg2 = fg.copy(alpha = .62f),
        fg3 = fg.copy(alpha = .38f),
        mat = if (dark) CrystalColors.MatDark else CrystalColors.MatLight,
        matEdge = Color.White.copy(alpha = if (dark) .11f else .70f),
        line = (if (dark) Color.White else Color.Black).copy(alpha = .08f),
        acc = acc,
        accInk = if (acc.luminanceApprox() > .45f) Color(0xFF0D1620) else Color.White,
    )
}

/** Rec. 709 luminance of the sRGB values; good enough to pick ink on the accent. */
private fun Color.luminanceApprox() = .2126f * red + .7152f * green + .0722f * blue

/** 4.3: 4 dp grid, gutters, icon sizes, radii. */
object CrystalDims {
    val gutter = 16.dp
    val studioGutter = 20.dp
    val touch = 48.dp
    val iconHome = 40.dp
    val iconRow = 36.dp
    val iconPreview = 28.dp
    val lensRing = 44.dp
    val lensStroke = 1.5.dp
    val lensTouch = 100.dp
    val radiusSheet = 22.dp
    val radiusControl = 12.dp
    val radiusField = 24.dp
    val arcRadiusLens = 118.dp
    val arcRadiusGlobal = 104.dp
    val arcStroke = 1.dp
    val slot = 44.dp
    val capsuleHeight = 30.dp
    val capsuleMin = 30.dp
    val capsuleMax = 220.dp
    val studioTile = 36.dp
}

/** Roboto Flex at one weight/width pair; each pair is its own family because axes are fixed per [Font]. */
private fun flex(weight: Int, width: Float) = FontFamily(
    Font(
        R.font.roboto_flex,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.width(width)),
    ),
)

/** 4.2: one family, precision from the weight and width axes. */
object CrystalType {
    val clock = TextStyle(fontFamily = flex(220, 118f), fontSize = 84.sp, lineHeight = 84.sp, fontFeatureSettings = "tnum")
    val date = TextStyle(fontFamily = flex(450, 100f), fontSize = 12.sp, letterSpacing = 2.2.sp)
    val title = TextStyle(fontFamily = flex(500, 112f), fontSize = 28.sp, lineHeight = 34.sp)
    val arcName = TextStyle(fontFamily = flex(550, 110f), fontSize = 17.sp, lineHeight = 22.sp)
    val body = TextStyle(fontFamily = flex(400, 100f), fontSize = 16.sp, lineHeight = 22.sp)
    val bodyStrong = TextStyle(fontFamily = flex(500, 100f), fontSize = 16.sp, lineHeight = 22.sp)
    val bodySmall = TextStyle(fontFamily = flex(400, 100f), fontSize = 15.sp, lineHeight = 20.sp)
    val caption = TextStyle(fontFamily = flex(400, 100f), fontSize = 13.sp, lineHeight = 18.sp)
    /** Section labels: the only caps text besides the date. */
    val label = TextStyle(fontFamily = flex(600, 100f), fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.2.sp)
}

val LocalCrystal = staticCompositionLocalOf { crystalPalette(dark = true, acc = CrystalColors.FrostDark) }

/** Provides the Crystal palette for the settings' theme and accent; used inside [InteliaTheme] from phase B2. */
@Composable
fun CrystalTheme(dark: Boolean, acc: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCrystal provides crystalPalette(dark, acc), content = content)
}

/**
 * The one surface (4.4): fill, a 1 dp light top edge and, for floating cards, a soft shadow. No blur.
 */
fun Modifier.crystalMat(
    palette: CrystalPalette,
    shape: Shape = RoundedCornerShape(CrystalDims.radiusSheet),
    floating: Boolean = false,
): Modifier {
    val lifted = if (floating) shadow(18.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = .35f), spotColor = Color.Black.copy(alpha = .35f)) else this
    return lifted.clip(shape).background(palette.mat).drawWithContent {
        drawContent()
        drawRect(palette.matEdge, size = Size(size.width, 1.dp.toPx()))
    }
}

@Composable
fun CrystalSurface(modifier: Modifier = Modifier, floating: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.crystalMat(LocalCrystal.current, floating = floating), content = content)
}

/** What a slot on the arc shows (4.4). */
sealed interface ArcSlot {
    val label: String

    /** An app or shortcut; [missing] when the target is gone (35 % and a "!" badge). */
    data class Icon(override val label: String, val missing: Boolean = false, val icon: @Composable () -> Unit) : ArcSlot

    /** FLOW slots show their name instead of an icon. */
    data class FlowName(override val label: String) : ArcSlot

    data object Empty : ArcSlot {
        override val label = ""
    }
}

enum class ArcEnd { Left, Right }

/**
 * The Crystal arc: one faded hairline, five fixed slots and a set handle at each end. Rendering only;
 * the hold-slide-release gesture arrives in phase B3. The box's bottom centre is the arc centre (LENS).
 */
@Composable
fun CrystalArc(
    slots: List<ArcSlot>,
    modifier: Modifier = Modifier,
    radius: Dp = CrystalDims.arcRadiusLens,
    selected: Int? = null,
    hotEnd: ArcEnd? = null,
    setName: String? = null,
    setIndex: Int = 0,
    setCount: Int = 1,
    mirrored: Boolean = false,
) {
    val c = LocalCrystal.current
    val slotSize = CrystalDims.slot
    val half = radius + slotSize
    val angles = CrystalArcGeometry.slotAngles(slots.size, mirrored)
    Box(modifier.size(half * 2, radius + slotSize + 64.dp)) {
        Canvas(Modifier.size(half * 2, radius + slotSize + 64.dp)) {
            val r = radius.toPx()
            val cx = size.width / 2
            val cy = size.height
            val fade = Brush.horizontalGradient(
                0f to Color.Transparent, .16f to c.arc, .84f to c.arc, 1f to Color.Transparent,
                startX = cx - r, endX = cx + r,
            )
            drawArc(
                brush = fade,
                startAngle = 180f + CrystalArcGeometry.START_DEG,
                sweepAngle = CrystalArcGeometry.END_DEG - CrystalArcGeometry.START_DEG,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                style = Stroke(CrystalDims.arcStroke.toPx()),
            )
            selected?.let { index ->
                val (dx, dy) = CrystalArcGeometry.point(angles.getOrNull(index) ?: return@let, radius.value - CrystalArcGeometry.DOT_INSET)
                drawCircle(c.acc, 2.5.dp.toPx(), Offset(cx + dx.dp.toPx(), cy + dy.dp.toPx()))
            }
        }
        slots.forEachIndexed { index, slot ->
            val on = index == selected
            val r = radius.value + if (on) CrystalArcGeometry.SELECTED_LIFT else 0f
            val (dx, dy) = CrystalArcGeometry.point(angles[index], r)
            Box(
                Modifier.align(Alignment.BottomCenter).offset(dx.dp, dy.dp + slotSize / 2).size(slotSize).scale(if (on) 1.2f else 1f),
                contentAlignment = Alignment.Center,
            ) { ArcSlotContent(slot, c) }
        }
        listOf(ArcEnd.Left to CrystalArcGeometry.HANDLE_LEFT_DEG, ArcEnd.Right to CrystalArcGeometry.HANDLE_RIGHT_DEG).forEach { (end, deg) ->
            val hot = end == hotEnd
            val (dx, dy) = CrystalArcGeometry.point(deg, radius.value)
            Text(
                if (end == ArcEnd.Left) "‹" else "›",
                Modifier.align(Alignment.BottomCenter).offset(dx.dp, dy.dp).scale(if (hot) 1.25f else 1f),
                style = CrystalType.caption.copy(fontSize = 13.sp), color = if (hot) c.acc else c.fg3,
            )
        }
        Column(Modifier.align(Alignment.TopCenter).width(radius * 2), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                selected?.let { slots.getOrNull(it)?.label }.orEmpty(),
                style = CrystalType.arcName, color = c.fg, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            )
            if (setCount > 1) SetIndicator(setName, setIndex, setCount, c)
        }
    }
}

@Composable
private fun ArcSlotContent(slot: ArcSlot, c: CrystalPalette) {
    when (slot) {
        ArcSlot.Empty -> Box(Modifier.size(6.dp).clip(CircleShape).background(c.fg3))
        is ArcSlot.FlowName -> Text(slot.label, style = CrystalType.caption, color = c.fg, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        is ArcSlot.Icon -> Box(Modifier.size(CrystalDims.iconHome), contentAlignment = Alignment.Center) {
            Box(Modifier.alpha(if (slot.missing) .35f else 1f)) { slot.icon() }
            if (slot.missing) Box(
                Modifier.align(Alignment.TopEnd).size(14.dp).clip(CircleShape).background(c.mat),
                contentAlignment = Alignment.Center,
            ) { Text("!", style = CrystalType.label.copy(fontSize = 10.sp, letterSpacing = 0.sp), color = c.fg) }
        }
    }
}

@Composable
private fun SetIndicator(name: String?, index: Int, count: Int, c: CrystalPalette) {
    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (name != null) Text(name, Modifier.padding(end = 4.dp), style = CrystalType.caption, color = c.fg2, maxLines = 1)
        repeat(count) { Box(Modifier.size(4.dp).clip(CircleShape).background(if (it == index) c.fg else c.fg3)) }
    }
}

/** Studio row (4.4): outline glyph tile, title, one-line summary, chevron. */
@Composable
fun StudioRow(title: String, summary: String?, onClick: () -> Unit, modifier: Modifier = Modifier, glyph: @Composable () -> Unit) {
    val c = LocalCrystal.current
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = CrystalDims.studioGutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(CrystalDims.studioTile).border(1.dp, c.line.copy(alpha = .16f), RoundedCornerShape(CrystalDims.radiusControl)),
            contentAlignment = Alignment.Center,
        ) { glyph() }
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 8.dp)) {
            Text(title, style = CrystalType.bodyStrong, color = c.fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // Large font: the summary may wrap to a second line (4.8).
            if (summary != null) Text(summary, style = CrystalType.caption, color = c.fg2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text("›", style = CrystalType.body, color = c.fg3)
    }
}

/**
 * PULSE live capsule (4.4), shape only: black like the cutout, 30 dp high, width = content within 30–220 dp.
 * Placement on the camera and the grow animation come with PULSE 2.0.
 */
@Composable
fun LiveCapsule(width: Dp, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier.size(width.coerceIn(CrystalDims.capsuleMin, CrystalDims.capsuleMax), CrystalDims.capsuleHeight)
            .clip(CircleShape).background(LocalCrystal.current.capsule).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        content = content,
    )
}

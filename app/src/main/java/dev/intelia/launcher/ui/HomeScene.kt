package dev.intelia.launcher.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.intelia.launcher.data.AnchorType
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.DockEdge
import dev.intelia.launcher.data.HomeItem
import dev.intelia.launcher.data.IconLook
import dev.intelia.launcher.data.ItemAlign
import dev.intelia.launcher.data.ItemKind
import dev.intelia.launcher.data.ItemTint
import dev.intelia.launcher.data.LabelStyle
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.LayoutSpec
import dev.intelia.launcher.data.LensStyle
import dev.intelia.launcher.data.PaletteTarget
import dev.intelia.launcher.data.iconLook
import dev.intelia.launcher.data.labelShown
import dev.intelia.launcher.data.labelStyle
import dev.intelia.launcher.data.lensDx
import java.time.LocalDateTime

/** Where the clock and free text start below the status bar, dp: right under it (the editor bar floats over). */
internal const val TEXT_TOP = 0f
/** How close the clock and free text may come to the screen's side edges, dp. */
internal const val TEXT_EDGE = 12f

/**
 * Places a clock or an unpinned date so that its centre sits at [fx] across the editor's area, the very point
 * the editor drags, so the text follows the finger one to one; it is kept whole [margin] dp inside the screen
 * (fx 0 and 1 still mean flush left and right, 0.5 centred).
 */
internal fun Modifier.atFx(fx: Float, areaLeft: Float, areaW: Float, margin: Float) = layout { m, c ->
    val edge = margin.dp.roundToPx()
    val w = c.maxWidth
    val p = m.measure(c.copy(minWidth = 0, maxWidth = (w - 2 * edge).coerceAtLeast(0)))
    val centre = (areaLeft + fx * areaW).dp.roundToPx()
    layout(w, p.height) { p.place((centre - p.width / 2).coerceIn(edge, maxOf(edge, w - edge - p.width)), 0) }
}

/** Where free text (fy = 0) starts below the status bar; a LENS row on the top edge pushes it down by its band. */
internal fun homeTextTop(geometry: HomeGeometry, statusTop: Float, compact: Boolean): Float =
    (if (compact) 24f else TEXT_TOP) + if (geometry.dock.edge == DockEdge.Top) geometry.areaTop - statusTop - 24f else 0f

/**
 * S2, one renderer: the clock with its bound date, and an unpinned date, as Home places them. Home passes its zone
 * callbacks; Studio's miniature draws the very same composition.
 */
@Composable
internal fun HomeText(layout: LayoutSpec, geometry: HomeGeometry, textTop: Float, settings: LauncherSettings, now: LocalDateTime,
    homeInk: Color, places: PlaceColors, wallPlaces: PlaceColors, compact: Boolean, alpha: () -> Float, dateOnPlate: Boolean = false,
    onClock: (LayoutCoordinates) -> Unit = {}, onTime: (LayoutCoordinates) -> Unit = {}, onDate: (LayoutCoordinates) -> Unit = {}) {
    val clock = layout.items.firstOrNull { it.kind == ItemKind.Clock }
    val date = layout.items.firstOrNull { it.kind == ItemKind.Date }
    val freeDate = date?.takeIf { it.anchor.type == AnchorType.Free }
    val listLayout = layout.items.any { it.anchor.type == AnchorType.Stack }
    if (clock != null || (date != null && freeDate == null)) {
        val a = clock?.anchor ?: dev.intelia.launcher.data.Anchor.free(if (listLayout) 0f else .5f, 0f)
        val bias = (a.fx * 2f - 1f).coerceIn(-1f, 1f)
        // §5.12 Veil background: one Light Veil plate behind the time and its bound date; text takes the theme's ink.
        val plate = !clock?.background.isNullOrEmpty()
        val clockInk = if (plate) LocalCrystal.current.fg else homeInk
        // On a plate the theme's accent variant is right; straight on the wallpaper, the wallpaper's.
        val clockPlaces = if (plate) places else wallPlaces
        val accent = clockPlaces[PaletteTarget.Clock]
        val textAccent = clockPlaces[PaletteTarget.Text]
        // The plate keeps 8 dp from the edges; bare text may come [TEXT_EDGE] dp close.
        Column(
            Modifier.fillMaxWidth().padding(top = (geometry.statusTop + textTop + geometry.areaH * a.fy).dp)
                .atFx(a.fx, geometry.areaLeft, geometry.areaW, if (plate) 8f else TEXT_EDGE).graphicsLayer { this.alpha = alpha() }
                .onGloballyPositioned(onClock)
                .then(if (plate) Modifier.veilSurface(plateTokens(clock.background), VeilOrigin.Top, RoundedCornerShape(26.dp))
                    .padding(horizontal = (20f + clock.look.padX).dp).padding(top = (14f + clock.look.padY).dp, bottom = (16f + clock.look.padY).dp)
                    else Modifier)
                .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
            horizontalAlignment = when (clock?.look?.align ?: ItemAlign.Auto) {
                ItemAlign.Start -> Alignment.Start
                ItemAlign.Center -> Alignment.CenterHorizontally
                ItemAlign.End -> Alignment.End
                ItemAlign.Auto -> if (bias < 0f) Alignment.Start else if (bias > 0f) Alignment.End else Alignment.CenterHorizontally
            },
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (clock != null) SplitClock(now.toLocalTime(), clock.clockStyle(settings.clockStyle), clock.tintColor(accent) ?: clockInk, accent,
                (if (clock.sizeDp == 0) (if (compact) 64 else ItemSizes.CLOCK) else clock.sizeDp).sp,
                Modifier.graphicsLayer { this.alpha = clock.alpha }.onGloballyPositioned(onTime), shadow = !plate, weight = clock.weight, look = clock.look)
            if (date != null && freeDate == null) DateText(date, now, clockInk, textAccent, onPlate = plate,
                modifier = Modifier.offset(date.look.dx.dp, date.look.dy.dp).onGloballyPositioned(onDate))
        }
    }
    if (freeDate != null) {
        // An unpinned date: placed like the clock (§5.5), by its own free point.
        // Moved over the clock's plate, it reads in the plate's ink instead of the wallpaper's.
        DateText(freeDate, now, if (dateOnPlate) LocalCrystal.current.fg else homeInk, (if (dateOnPlate) places else wallPlaces)[PaletteTarget.Text],
            onPlate = dateOnPlate, modifier = Modifier.fillMaxWidth()
            .padding(top = (geometry.statusTop + textTop + geometry.areaH * freeDate.anchor.fy).dp)
            .atFx(freeDate.anchor.fx, geometry.areaLeft, geometry.areaW, TEXT_EDGE).graphicsLayer { this.alpha = alpha() }
            .onGloballyPositioned(onDate))
    }
}

/** The icon of an app or FLOW on Home in its own style, shape, colour and opacity ([IconLook]). */
@Composable
internal fun HomeIconFace(app: AppEntry?, look: IconLook, homeInk: Color, accent: Color) {
    val tint = tintOf(look.tint, homeInk, accent)
    CompositionLocalProvider(LocalIconStyle provides look.style, LocalIconShape provides look.shape) {
        Box(Modifier.graphicsLayer { alpha = look.alpha }) {
            if (app != null) AppIcon(app, Modifier.size(look.sizeDp.dp), tint = tint)
            else HomeGlyph(Glyph.Flow, null, tint = tint, size = look.sizeDp.dp * .7f)
        }
    }
}

private fun tintOf(tint: String, homeInk: Color, accent: Color): Color = when (tint) {
    ItemTint.Accent.name -> accent
    ItemTint.Light.name -> Color(0xFFF4F5F7)
    ItemTint.Dark.name -> Color(0xFF14161A)
    else -> homeInk
}

/** Height of one label line, dp, as the system and Intelia's text size draw it (non-linear above 100 % on Android 14). */
internal fun Density.labelLine(style: LabelStyle): Float = (style.sizeSp * LINE_HEIGHT).sp.toDp().value

/** The name boxes of [layout]'s labelled icons on this screen ([labelBoxes]); a side row has no room for names. */
internal fun homeLabels(layout: LayoutSpec, geometry: HomeGeometry, settings: LauncherSettings, density: Density): Map<String, LabelBox> {
    val icons = layout.items.filter { (it.kind == ItemKind.App || it.kind == ItemKind.Flow) && it.anchor.type.isPlaced() }.mapNotNull { item ->
        val at = geometry.position(item, layout) ?: return@mapNotNull null
        val style = item.labelStyle(settings.labels)
        LabelIcon(item.id, at, item.iconLook(settings).sizeDp.toFloat(),
            labelled = item.labelShown(settings.labels) && !(geometry.dock.edge.vertical && item.anchor.type == AnchorType.LensRow),
            gap = style.gap.toFloat(), lineH = density.labelLine(style), lines = style.lines)
    }
    return labelBoxes(icons, Pt(geometry.lensX, geometry.lensY).takeIf { !geometry.dock.edge.vertical }, geometry.width,
        geometry.statusTop, geometry.height - geometry.bottomInset)
}

/** An icon's name in its [LabelStyle], in the box [labelBoxes] gave it. */
@Composable
internal fun HomeLabel(name: String, box: LabelBox, style: LabelStyle, homeInk: Color, accent: Color, modifier: Modifier = Modifier) {
    val family = faceFamily(style.face, style.weight) ?: flex(style.weight, 100f)
    val size = style.sizeSp
    Text(name, modifier.labelAt(box).width(box.width.dp),
        style = CrystalType.body.copy(fontFamily = family, fontWeight = FontWeight(style.weight), fontSize = size.sp,
            lineHeight = (size * LINE_HEIGHT).sp, shadow = homeShadow(homeInk),
            textAlign = when (style.align) { ItemAlign.Start -> TextAlign.Start; ItemAlign.End -> TextAlign.End; else -> TextAlign.Center }),
        color = tintOf(style.tint, homeInk, accent), maxLines = box.lines, overflow = TextOverflow.Ellipsis)
}

/** Fills the parent and puts the content centred on [LabelBox.x], below [LabelBox.y] or, [LabelBox.above], ending at it. */
private fun Modifier.labelAt(box: LabelBox) = layout { m, c ->
    val p = m.measure(c.copy(minWidth = 0, minHeight = 0))
    layout(if (c.hasBoundedWidth) c.maxWidth else p.width, if (c.hasBoundedHeight) c.maxHeight else p.height) {
        val y = box.y.dp.roundToPx()
        p.place(box.x.dp.roundToPx() - p.width / 2, if (box.above) y - p.height else y)
    }
}

/**
 * Studio's miniature of Home (S2): the screen as Home lays it out — the same geometry, insets, LENS row, clock and date,
 * icons with their names — drawn at the real size of this window and scaled down as one picture, so it shows what
 * Home will show, a long name or a big icon included. Nothing in it reacts to touch.
 */
@Composable
internal fun HomeMiniature(settings: LauncherSettings, layout: LayoutSpec, now: LocalDateTime, entry: (String) -> AppEntry?, scale: Float,
    modifier: Modifier = Modifier,
    /** Drawn instead of the LENS mark, at the real size, given LENS's centre and edge: a preview's open ARC or Search. */
    lensOverlay: (@Composable BoxScope.(lens: Pt, edge: DockEdge) -> Unit)? = null) {
    val density = LocalDensity.current
    val homeInk = LocalHomeInk.current
    val places = LocalPlaces.current
    val wallPlaces = LocalWallPlaces.current
    val statusTop = with(density) { WindowInsets.statusBarsIgnoringVisibility.getTop(this).toDp().value }
    val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp().value }
    BoxWithConstraints(modifier) {
        val w = maxWidth.value / scale
        val h = maxHeight.value / scale
        Box(Modifier.fillMaxSize().scaled(scale, w, h)) {
            val lensSpot = Dock.centre(layout.dock, layout.lensDx(), w, h, statusTop, bottomInset)
            val rowIcon = rowIcon(layout, settings)
            val geometry = HomeGeometry(w, h, statusTop, lensSpot.x, lensSpot.y, layout.dock, bottomInset, rowIcon)
            val compact = h < 560f
            HomeText(layout, geometry, homeTextTop(geometry, statusTop, compact), settings, now, homeInk, places, wallPlaces, compact, { 1f })
            val names = homeLabels(layout, geometry, settings, density)
            val accent = wallPlaces[PaletteTarget.Icons]
            layout.items.forEach { item ->
                if ((item.kind != ItemKind.App && item.kind != ItemKind.Flow) || !item.anchor.type.isPlaced()) return@forEach
                val app = if (item.kind == ItemKind.App) entry(item.target) else null
                val flow = if (item.kind == ItemKind.Flow) settings.aliases.firstOrNull { it.id == item.target } else null
                if (app == null && flow == null) return@forEach
                val p = geometry.position(item, layout) ?: return@forEach
                names[item.id]?.let { HomeLabel(app?.label ?: flow?.name.orEmpty(), it, item.labelStyle(settings.labels), homeInk, accent) }
                Box(Modifier.centerAt(p.x.dp, p.y.dp)) { HomeIconFace(app, item.iconLook(settings), homeInk, accent) }
            }
            // The legacy text list (preset "Vlastní"), as Home sets it above LENS.
            val stack = layout.items.filter { it.kind == ItemKind.App && it.anchor.type == AnchorType.Stack }.sortedBy { it.anchor.row }
            if (stack.isNotEmpty()) Column(Modifier.align(Alignment.BottomStart)
                .padding(start = 28.dp, end = 36.dp, bottom = (bottomInset.dp + LensLift + 70.dp))) {
                stack.forEach { item ->
                    Text(entry(item.target)?.label ?: return@forEach, style = InkText.item.copy(fontSize = 30.sp, lineHeight = 34.sp,
                        shadow = homeShadow(homeInk)), color = homeInk, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 7.dp))
                }
            }
            val lens = layout.items.firstOrNull { it.kind == ItemKind.Lens }
            if (lensOverlay != null) lensOverlay(lensSpot, layout.dock.edge)
            else Box(Modifier.centerAt(lensSpot.x.dp, lensSpot.y.dp)) {
                LensMark(homeInk, ring = lens?.lensDp()?.dp ?: CrystalDims.lensRing, filled = lens?.style == LensStyle.Filled.name)
            }
        }
    }
}

/** The largest icon on [layout]'s LENS row, dp: the row's slots make room for it ([HomeGeometry.rowIcon]). */
internal fun rowIcon(layout: LayoutSpec, settings: LauncherSettings): Float =
    layout.items.filter { it.anchor.type == AnchorType.LensRow }.maxOfOrNull { it.iconLook(settings).sizeDp.toFloat() } ?: 0f

/** Lays the content out at [w] × [h] dp and draws it scaled by [k] from the top start, filling the parent. */
internal fun Modifier.scaled(k: Float, w: Float, h: Float) = layout { m, c ->
    val p = m.measure(Constraints.fixed(w.dp.roundToPx(), h.dp.roundToPx()))
    layout(c.maxWidth, c.maxHeight) {
        p.placeWithLayer(0, 0) { scaleX = k; scaleY = k; transformOrigin = TransformOrigin(0f, 0f) }
    }
}

private const val LINE_HEIGHT = 1.2f

package dev.intelia.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.intelia.launcher.R
import dev.intelia.launcher.data.DockEdge
import dev.intelia.launcher.data.ItemKind
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.LensStyle
import dev.intelia.launcher.data.PaletteTarget
import dev.intelia.launcher.data.lensDx
import dev.intelia.launcher.flow.Flow
import dev.intelia.launcher.search.AppSearch
import dev.intelia.launcher.search.SearchPicks

/**
 * The guide's interactive sandbox (ONBOARDING_V2 §3): while set, a system effect the real surfaces would do directly
 * (torch, media keys, opening a player) is only handed here as the [Action] it stands for, and nothing happens.
 */
internal val LocalSandbox = androidx.compose.runtime.staticCompositionLocalOf<((dev.intelia.launcher.action.Action) -> Unit)?> { null }

/**
 * M1: what a Studio page shows above its sheet. Each kind is drawn by the renderer the real surface uses, fed with the
 * owner's settings, inside a [PreviewCover], so a preview looks exactly like the phone and never launches, plays or
 * runs anything.
 */
enum class PreviewKind {
    /** The sheet takes the whole room. */
    None,
    /** The live Home miniature (clock, wallpaper, dock with LENS, favourites). */
    Home,
    /** LENS ARC open on the owner's first set, no slot picked. */
    Arc,
    /** Global ARC open under the camera on the set PULSE's hold opens, no slot picked (ARC page, Global ARC tab). */
    GlobalArc,
    /** The LENS row: dock icons around LENS. */
    Lens,
    /** The Search card for a sample query, marked as a sample; no focus, no keyboard. */
    Search,
    /** PULSE at the camera in the state the PULSE page picks (S3.1). */
    Pulse,
}

/** Which preview each Studio page gets, and which part of the screen it shows. Pure, so it is tested on the JVM. */
object PreviewPlan {
    /** [section] null = Studio's main page; [globalArc] = the ARC page shows its Global ARC tab. */
    fun kind(section: StudioSection?, globalArc: Boolean = false): PreviewKind = when (section) {
        null, StudioSection.Look, StudioSection.Home, StudioSection.Favorites -> PreviewKind.Home
        StudioSection.Arc -> if (globalArc) PreviewKind.GlobalArc else PreviewKind.Arc
        StudioSection.Pulse -> PreviewKind.Pulse
        // Gestures says what LENS's tap, swipe and hold do; the row it sits on is what they act on.
        StudioSection.Gestures -> PreviewKind.Lens
        // FLOWs are found and run from Search by their exact name.
        StudioSection.Actions -> PreviewKind.Search
        else -> PreviewKind.None
    }

    /**
     * The top of the strip a preview shows, dp from the screen's top: Home shows the whole screen, LENS the dock row,
     * ARC the arc above LENS, Search the card above the field. With LENS on a side or top edge ARC and LENS show the
     * whole screen. [searchTop] is the measured top of the Search card (null until it was laid out once, then the strip
     * guesses [SEARCH_STRIP]). Never less than [MIN_STRIP] tall.
     */
    fun stripTop(kind: PreviewKind, screenH: Float, lensY: Float, arcReach: Float, bottomDock: Boolean, searchTop: Float? = null): Float = when {
        kind == PreviewKind.Lens && bottomDock -> lensY - LENS_STRIP
        kind == PreviewKind.Arc && bottomDock -> lensY - arcReach - ARC_HEAD
        kind == PreviewKind.Search -> (searchTop ?: (screenH - SEARCH_STRIP)) - SEARCH_HEAD
        else -> 0f
    }.coerceIn(0f, (screenH - MIN_STRIP).coerceAtLeast(0f))

    /** The sample Search query: the owner's first FLOW name (it shows how Enter runs it), else a start of an app name. */
    fun sampleQuery(flows: List<String>, apps: List<String>): String =
        flows.firstOrNull { it.isNotBlank() }?.trim() ?: apps.map { it.trim() }.firstOrNull { it.length >= 2 }?.take(3).orEmpty()

    /**
     * dp above LENS the dock strip starts, room above the arc for its set dots and caption, the Search strip's guess
     * before the card was measured, room above the measured card (the „Ukázka“ badge sits there).
     */
    const val LENS_STRIP = 130f
    const val ARC_HEAD = 90f
    const val SEARCH_STRIP = 420f
    const val SEARCH_HEAD = 48f
    const val MIN_STRIP = 120f
}

/** True inside a preview: a renderer must not start effects with side effects (focus, keyboard, haptics, actions). */
val LocalPreviewMode = compositionLocalOf { false }

/**
 * Wraps a renderer as a preview: every touch is consumed before the renderer sees it, TalkBack hears one [label]
 * instead of the renderer's content, and [LocalPreviewMode] is set.
 */
@Composable
fun PreviewCover(label: String, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.clearAndSetSemantics { contentDescription = label }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }) {
        CompositionLocalProvider(LocalPreviewMode provides true) { content() }
    }
}

/**
 * The preview above a Studio sheet: [kind]'s renderer at the real screen size ([screenW] × [screenH]), scaled evenly
 * so its strip fits [room] (the sheet's Preview stop) and the width less a margin.
 */
@Composable
internal fun StudioPreview(kind: PreviewKind, settings: LauncherSettings, model: HomeViewModel, room: Dp, screenW: Dp, screenH: Dp,
    pulseLook: PulseLook) {
    if (kind == PreviewKind.None) return
    if (kind == PreviewKind.Pulse || kind == PreviewKind.GlobalArc) {
        // Global ARC lives under the camera, so it is PULSE's stage in its ARC state.
        PulseStage(model, settings, if (kind == PreviewKind.GlobalArc) PulseLook.Arc else pulseLook, room, screenW, Modifier.padding(top = 4.dp))
        return
    }
    val homeInk = LocalHomeInk.current
    val density = LocalDensity.current
    val now by rememberNow()
    val arc by model.arc.collectAsStateWithLifecycle()
    val home = settings.home.activeLayout()
    val statusTop = with(density) { WindowInsets.statusBarsIgnoringVisibility.getTop(this).toDp().value }
    val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp().value }
    val lens = Dock.centre(home.dock, home.lensDx(), screenW.value, screenH.value, statusTop, bottomInset)
    val reach = Dock.bow(arc.look, arc.sets.size.coerceAtLeast(1), home.dock.edge, lens, screenW.value, screenH.value).reach
    // The Search card's real top on the screen, dp, once it was laid out (its height follows the results).
    var searchTop by remember { mutableStateOf<Float?>(null) }
    val top = PreviewPlan.stripTop(kind, screenH.value, lens.y, reach, home.dock.edge == DockEdge.Bottom, searchTop)
    val stripH = screenH - top.dp
    val scale = minOf(room / stripH, (screenW - 16.dp) / screenW).coerceAtLeast(.05f)
    val shape = RoundedCornerShape(16.dp)
    val label = stringResource(when (kind) {
        PreviewKind.Arc -> R.string.arc_preview
        PreviewKind.Lens -> R.string.lens_preview
        PreviewKind.Search -> R.string.search_preview
        else -> R.string.home_preview
    })
    // TalkBack hears one label, not the live time and date or the sample results as if they were content.
    PreviewCover(label, Modifier.padding(top = 4.dp).size(screenW * scale, stripH * scale).clip(shape)
        .border(1.dp, homeInk.copy(alpha = .35f), shape)) {
        Box(Modifier.shiftUp((top * scale).dp, screenW * scale, screenH * scale)) {
            when (kind) {
                // The clock and date as Home draws them (font, size, colour, plate), not the old global clock style.
                PreviewKind.Home -> HomePreview(settings, emptyList(), now, homeInk, LocalWallPlaces.current[PaletteTarget.Clock],
                    Modifier.fillMaxSize(), scale = scale, onWallpaper = true, home = home, entry = model::entry)
                PreviewKind.Arc -> {
                    val item = home.items.firstOrNull { it.kind == ItemKind.Lens }
                    HomeMiniature(settings, home, now, model::entry, scale, Modifier.fillMaxSize()) { spot, edge ->
                        AccentScope(PaletteTarget.Lens) {
                            LensArc(arc, spot, edge, { a -> a.targets.map { model.entry(it)?.label } }, onSearch = {}, onDrawer = {},
                                onAction = {}, onOpenChange = {}, lensRing = item?.lensDp()?.dp ?: CrystalDims.lensRing,
                                lensFilled = item?.style == LensStyle.Filled.name, shown = true)
                        }
                    }
                }
                PreviewKind.Search -> {
                    // Home under Search as the Focus depth draws it: blurred 18 dp and at 97 % (D15).
                    HomeMiniature(settings, home, now, model::entry, scale, Modifier.fillMaxSize().blur((18f * scale).dp)
                        .graphicsLayer { scaleX = .97f; scaleY = .97f })
                    Box(Modifier.fillMaxSize().scaled(scale, screenW.value, screenH.value)) { SearchSample(model, settings, screenW, bottomInset) { searchTop = it } }
                }
                else -> HomeMiniature(settings, home, now, model::entry, scale, Modifier.fillMaxSize())
            }
        }
        if (kind == PreviewKind.Search) SampleBadge(Modifier.align(Alignment.TopEnd).padding(8.dp))
    }
}

/** Search as LENS opens it, over Home's scrim, for [PreviewPlan.sampleQuery]; nothing is focused or typed into. */
@Composable
private fun BoxScope.SearchSample(model: HomeViewModel, settings: LauncherSettings, screenW: Dp, bottomInset: Float, onTop: (Float) -> Unit) {
    val density = LocalDensity.current
    val apps by model.visibleApps.collectAsStateWithLifecycle()
    val favorites by model.favoriteApps.collectAsStateWithLifecycle()
    val query = remember(settings.aliases, apps) { PreviewPlan.sampleQuery(settings.aliases.map { it.name }, apps.map { it.label }) }
    val exact = remember(settings.aliases, query) { Flow.exact(settings.aliases, query) }
    val rows = remember(query, apps, settings.aliases, favorites) {
        SearchPicks.of(query, AppSearch.rank(apps, query) { it.searchKey }, Flow.suggest(settings.aliases, query), exact, favorites)
    }
    Box(Modifier.fillMaxSize().background(veilScrim()))
    // Laid out at the real screen size (the scale is a layer), so the position is in screen dp.
    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = (bottomInset.dp + LensLift - 28.dp))
        .padding(horizontal = 12.dp).onGloballyPositioned { c -> onTop(with(density) { c.positionInParent().y.toDp().value }) }) {
        ResultsCard(query, rows, model::entry, { 1f }, {}, {})
        Spacer(Modifier.height(8.dp))
        SearchField(query, remember { FocusRequester() }, { 1f }, lensShift(settings.home.lensDx(), screenW), exactMatch = exact != null,
            onChange = {}, onSubmit = {})
    }
}

/** "Ukázka": marks sample data (a query, a song, an alarm) so a preview never passes it off as the owner's. */
@Composable
internal fun SampleBadge(modifier: Modifier = Modifier) {
    val ink = LocalInk.current
    Text(stringResource(R.string.pulse_preview_sample), modifier.clip(RoundedCornerShape(10.dp)).background(ink.accent.copy(alpha = .22f))
        .padding(horizontal = 8.dp, vertical = 2.dp), style = InkText.label, color = ink.ink)
}

/** Lays the content out at [w] × [h] and places it [up] higher, so the parent shows the strip below that line. */
private fun Modifier.shiftUp(up: Dp, w: Dp, h: Dp) = layout { m, c ->
    val p = m.measure(Constraints.fixed(w.roundToPx(), h.roundToPx()))
    layout(c.maxWidth, c.maxHeight) { p.place(0, -up.roundToPx()) }
}

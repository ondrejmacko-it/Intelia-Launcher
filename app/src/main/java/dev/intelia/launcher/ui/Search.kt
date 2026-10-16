package dev.intelia.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.intelia.launcher.R
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.flow.Flow
import dev.intelia.launcher.flow.FlowAlias
import dev.intelia.launcher.search.AppSearch
import dev.intelia.launcher.search.SearchPick
import dev.intelia.launcher.search.SearchPicks
import kotlinx.coroutines.launch
import kotlin.math.max

private const val RAIL = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ"

private fun indexLetter(label: String): Char =
    AppSearch.fold(label).firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' } ?: '#'

/** Home under Search and the drawer: dimmed, never replaced (§5.6). */
private val Scrim = Color.Black.copy(alpha = .42f)

/** The paper-ink kit (rows, rail) re-coloured for text on a Veil surface of the current theme. */
@Composable
private fun veilInk(): InkPalette {
    val c = LocalCrystal.current
    val ink = LocalInk.current
    return remember(c, ink) { ink.copy(paper = Color.Transparent, ink = c.fg, muted = c.fg2, faint = c.fg3, line = c.fg3.copy(alpha = .4f)) }
}

/**
 * LENS → Search (§5.6, prototype `openSearch`): Home stays visible behind a scrim, the LENS ring opens into a Veil
 * pill that rides the keyboard, and up to six results sit in a Veil Dense card above it, nearest first. The first
 * row is what Enter does; typing alone never runs anything. Back (or closing the keyboard) plays it in reverse.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchOverlay(
    model: HomeViewModel,
    settings: LauncherSettings,
    fromLens: Boolean,
    resolve: (String) -> AppEntry?,
    onBack: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onRun: (FlowAlias) -> Unit,
    onOptions: (AppEntry) -> Unit,
) {
    val query by model.query.collectAsStateWithLifecycle()
    val results by model.results.collectAsStateWithLifecycle()
    val flows by model.flowMatches.collectAsStateWithLifecycle()
    val favorites by model.favoriteApps.collectAsStateWithLifecycle()
    val exact = remember(settings.aliases, query) { Flow.exact(settings.aliases, query) }
    val rows = remember(query, results, flows, exact, favorites) { SearchPicks.of(query, results, flows, exact, favorites) }
    val field = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val morph = remember { Animatable(if (fromLens) 0f else 1f) }
    val grow = motionMs(170)
    val growDelay = motionMs(70)
    val shrink = motionMs(160)
    var leaving by remember { mutableStateOf(false) }
    /** The options sheet hides the keyboard on purpose; that must not close Search. */
    var held by remember { mutableStateOf(false) }
    // The card is invisible until the morph is 60 % through; composing it two frames later (after the focus and
    // IME setup, before the keyboard starts to move) splits the first frame in two.
    var cardReady by remember { mutableStateOf(!fromLens || grow == 0) }
    LaunchedEffect(Unit) { repeat(2) { withFrameNanos { } }; cardReady = true }
    LaunchedEffect(Unit) {
        field.requestFocus(); keyboard?.show()
        if (fromLens) morph.animateTo(1f, tween(grow, delayMillis = growDelay, easing = CubicBezierEasing(.05f, .7f, .1f, 1f)))
    }
    fun leave() {
        if (leaving) return
        leaving = true
        scope.launch { keyboard?.hide(); focus.clearFocus(); morph.animateTo(0f, tween(shrink)); onBack() }
    }
    BackHandler { leave() }
    // The first Back goes to the IME; when it closes the keyboard, Search follows in the same gesture.
    val imeVisible = WindowInsets.isImeVisible
    var imeSeen by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible) { if (imeVisible) { imeSeen = true; held = false } else if (imeSeen && !held) leave() }

    fun act(pick: SearchPick<AppEntry>) {
        when (pick) {
            is SearchPick.Run -> onRun(pick.alias)
            is SearchPick.App -> onLaunch(pick.app)
            // Completing the name is safe: it only turns the row into the exact match.
            is SearchPick.Complete -> model.query.value = pick.alias.name
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = morph.value }.background(Scrim)
            .pointerInput(Unit) { detectTapGestures { leave() } })
        Box(Modifier.fillMaxSize()) {
            val nav = WindowInsets.navigationBars
            val ime = WindowInsets.ime
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .offset {
                    // Centred on LENS while the keyboard is down, then riding it (same frame, no relayout).
                    val rest = nav.getBottom(this) + (LensLift - 28.dp).roundToPx()
                    IntOffset(0, -max(rest, ime.getBottom(this) + 10.dp.roundToPx()))
                }
                .padding(horizontal = 12.dp)) {
                if (cardReady) ResultsCard(query, rows, resolve, morph::value, ::act, onOptions = { held = true; onOptions(it) })
                Spacer(Modifier.height(8.dp))
                SearchField(query, field, morph::value, lensShift(settings.reach), exactMatch = exact != null,
                    onChange = { model.query.value = it }, onSubmit = { SearchPicks.enter(query, rows)?.let(::act) })
            }
        }
    }
}

/** The Veil pill: drawn from the LENS ring (morph 0) to the full field (morph 1); layout never changes. */
@Composable
private fun SearchField(query: String, focus: FocusRequester, morph: () -> Float, lensDx: Dp, exactMatch: Boolean,
                        onChange: (String) -> Unit, onSubmit: () -> Unit) {
    val c = LocalCrystal.current
    val tokens = veil(VeilLevel.Dense)
    val homeInk = LocalHomeInk.current
    val description = stringResource(R.string.lens_description)
    Row(Modifier.fillMaxWidth().height(56.dp)
        .drawWithContent {
            val m = morph()
            val r = CrystalDims.lensRing.toPx() / 2f
            val x = size.width / 2f + lensDx.toPx() // the field is centred like LENS
            val h = size.height
            val left = x - r + (0f - (x - r)) * m
            val right = x + r + (size.width - (x + r)) * m
            val top = h / 2f - r + (0f - (h / 2f - r)) * m
            val bottom = h / 2f + r + (h - (h / 2f + r)) * m
            val radius = r + (h / 2f - r) * m
            val corner = CornerRadius(radius)
            val at = Offset(left, top)
            val box = Size(right - left, bottom - top)
            drawRoundRect(Brush.verticalGradient(listOf(tokens.top, tokens.bottom), top, bottom), at, box, corner, alpha = m)
            clipRect(left, top, right, bottom) { this@drawWithContent.drawContent() }
            drawRoundRect(lerp(homeInk.copy(alpha = .88f), tokens.edgeHot, m), at, box, corner, style = Stroke(if (m < 1f) 1.5.dp.toPx() else 1.dp.toPx()))
        }
        .padding(start = 18.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).graphicsLayer { alpha = ((morph() - .5f) * 2f).coerceIn(0f, 1f) }, verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(16.dp)) { drawCircle(c.fg2, size.minDimension / 2f - .75.dp.toPx(), style = Stroke(1.5.dp.toPx())) }
            BasicTextField(query, onChange, Modifier.weight(1f).padding(start = 12.dp).focusRequester(focus).semantics { contentDescription = description },
                singleLine = true, textStyle = CrystalType.body.copy(fontSize = 17.sp, color = c.fg), cursorBrush = SolidColor(c.acc),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go), keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text(stringResource(R.string.search), style = CrystalType.body.copy(fontSize = 17.sp), color = c.fg3)
                        inner()
                    }
                })
            if (exactMatch) Text(stringResource(R.string.enter_runs).uppercase(), Modifier.padding(horizontal = 6.dp), style = CrystalType.label, color = c.acc)
            if (query.isNotEmpty()) IconAction(Glyph.Close, stringResource(R.string.clear), { onChange("") }, tint = c.fg2)
        }
    }
}

/** Veil Dense card of at most [SearchPicks.MAX] rows, first row (Enter) nearest the field and lit in the accent. */
@Composable
private fun ResultsCard(
    query: String,
    rows: List<SearchPick<AppEntry>>,
    resolve: (String) -> AppEntry?,
    morph: () -> Float,
    act: (SearchPick<AppEntry>) -> Unit,
    onOptions: (AppEntry) -> Unit,
) {
    val c = LocalCrystal.current
    val blank = query.isBlank()
    if (blank && rows.isEmpty()) return
    val missing = stringResource(R.string.flow_target_missing)
    Column(Modifier.fillMaxWidth()
        .graphicsLayer {
            val a = ((morph() - .6f) / .4f).coerceIn(0f, 1f)
            alpha = a; translationY = (1f - a) * 8.dp.toPx()
        }
        .veilSurface(veil(VeilLevel.Dense), VeilOrigin.Bottom).padding(6.dp)) {
        if (blank) Text(stringResource(R.string.mine).uppercase(), Modifier.padding(start = 10.dp, top = 8.dp, bottom = 4.dp),
            style = CrystalType.label, color = c.fg2)
        if (rows.isEmpty()) Text(stringResource(R.string.no_results, query.trim()), Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
            style = CrystalType.body, color = c.fg2)
        rows.asReversed().forEachIndexed { reversed, pick ->
            val first = !blank && reversed == rows.lastIndex
            when (pick) {
                is SearchPick.App -> AppPick(pick.app, first, { act(pick) }, { onOptions(pick.app) })
                is SearchPick.Run -> FlowCard(pick.alias, resolve, missing, onRun = { act(pick) })
                is SearchPick.Complete -> FlowPick(pick.alias, first, resolve, missing) { act(pick) }
            }
        }
    }
}

private val PickShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppPick(app: AppEntry, first: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val c = LocalCrystal.current
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(PickShape)
        .then(if (first) Modifier.background(c.acc.copy(alpha = .16f)) else Modifier)
        .combinedClickable(role = Role.Button, onLongClickLabel = stringResource(R.string.app_options), onClick = onClick, onLongClick = onLongClick)
        .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(app, Modifier.size(32.dp), tint = c.fg)
        Text(app.label, Modifier.padding(start = 12.dp).weight(1f), style = CrystalType.body.copy(fontSize = 15.sp),
            color = if (app.suspended) c.fg3 else c.fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The exact FLOW: name, the numbered chain of its apps, Run. Always shown before it can run (D5). */
@Composable
private fun FlowCard(alias: FlowAlias, resolve: (String) -> AppEntry?, missing: String, onRun: () -> Unit) {
    val c = LocalCrystal.current
    Row(Modifier.fillMaxWidth().clip(PickShape).background(c.acc.copy(alpha = .14f)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 10.dp)) {
            Text(alias.name, style = CrystalType.arcName, color = c.fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            FlowChain(alias, resolve, missing, Modifier.padding(top = 8.dp), icon = 22.dp)
        }
        Capsule(stringResource(R.string.flow_run), onRun)
    }
}

/** A FLOW whose name starts with the query; a tap completes the name (the row then becomes [FlowCard]). */
@Composable
private fun FlowPick(alias: FlowAlias, first: Boolean, resolve: (String) -> AppEntry?, missing: String, onClick: () -> Unit) {
    val c = LocalCrystal.current
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(PickShape)
        .then(if (first) Modifier.background(c.acc.copy(alpha = .16f)) else Modifier)
        .clickable(role = Role.Button, onClickLabel = alias.name, onClick = onClick)
        .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        HomeGlyph(Glyph.Flow, null, tint = c.acc, size = 20.dp)
        Text(alias.name, Modifier.padding(start = 12.dp).weight(1f), style = CrystalType.body.copy(fontSize = 15.sp), color = c.fg,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        FlowChain(alias, resolve, missing, icon = 18.dp)
    }
}

@Composable
private fun FlowChain(alias: FlowAlias, resolve: (String) -> AppEntry?, missing: String, modifier: Modifier = Modifier, icon: Dp) {
    val c = LocalCrystal.current
    val labels = alias.targets.map { resolve(it)?.label ?: missing }.joinToString(", ")
    Row(modifier.semantics { contentDescription = labels }, verticalAlignment = Alignment.CenterVertically) {
        alias.targets.forEachIndexed { i, key ->
            if (i > 0) Box(Modifier.padding(horizontal = 5.dp).width(12.dp).height(1.dp).background(c.fg3))
            Text("${i + 1}", Modifier.padding(end = 4.dp), style = CrystalType.label.copy(letterSpacing = 0.sp, fontSize = 10.sp), color = c.fg2)
            val app = resolve(key)
            if (app != null) AppIcon(app, Modifier.size(icon), tint = c.fg)
            else Canvas(Modifier.size(icon)) { drawCircle(c.fg3, size.minDimension / 2f - .5.dp.toPx(), style = Stroke(1.dp.toPx())) }
        }
    }
}

/**
 * Swipe up: the A–Z drawer as a Veil Focus sheet over dimmed Home (owner, 2026-10-16). Its field is a door to
 * Search, so the drawer is never composed behind LENS → Search.
 */
@Composable
fun DrawerOverlay(
    model: HomeViewModel,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onOptions: (AppEntry) -> Unit,
    onFlows: () -> Unit,
    onStudio: () -> Unit,
) {
    val c = LocalCrystal.current
    val apps by model.visibleApps.collectAsStateWithLifecycle()
    val favorites by model.favoriteApps.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val shown = remember { Animatable(0f) }
    val inMs = motionMs(200)
    val outMs = motionMs(150)
    LaunchedEffect(Unit) { shown.animateTo(1f, tween(inMs, easing = CubicBezierEasing(.2f, .9f, .25f, 1f))) }
    var leaving by remember { mutableStateOf(false) }
    fun leave() {
        if (leaving) return
        leaving = true
        scope.launch { shown.animateTo(0f, tween(outMs)); onBack() }
    }
    BackHandler { leave() }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = shown.value }.background(Scrim)
            .pointerInput(Unit) { detectTapGestures { leave() } })
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(8.dp)
            .graphicsLayer { alpha = shown.value; translationY = (1f - shown.value) * 32.dp.toPx() }
            .veilSurface(veil(VeilLevel.Focus), VeilOrigin.Bottom)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
            CompositionLocalProvider(LocalInk provides veilInk()) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 6.dp, top = 6.dp).heightIn(min = 56.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.apps), style = CrystalType.title, color = c.fg)
                    Spacer(Modifier.weight(1f))
                    Text(apps.size.toString(), Modifier.padding(end = 6.dp), style = CrystalType.label, color = c.fg3)
                    IconAction(Glyph.Flow, stringResource(R.string.flow), onFlows, tint = c.fg)
                    IconAction(Glyph.Studio, stringResource(R.string.studio), onStudio, tint = c.fg)
                }
                Box(Modifier.weight(1f).fillMaxWidth()) { Drawer(apps, favorites, onLaunch, onOptions) }
                // The field opens Search in place: same pill, same spot at the thumb.
                val solid = veil(VeilLevel.Focus).bottom.copy(alpha = 1f)
                Row(Modifier.fillMaxWidth().padding(10.dp).height(52.dp).clip(CircleShape)
                    .background(solid).background(c.fg.copy(alpha = .06f))
                    .clickable(role = Role.Button, onClick = onSearch).padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(16.dp)) { drawCircle(c.fg2, size.minDimension / 2f - .75.dp.toPx(), style = Stroke(1.5.dp.toPx())) }
                    Text(stringResource(R.string.search), Modifier.padding(start = 12.dp), style = CrystalType.body.copy(fontSize = 17.sp), color = c.fg3)
                }
            }
        }
    }
}

@Composable
internal fun IconAction(glyph: Glyph, label: String, onClick: () -> Unit, tint: Color = LocalInk.current.ink) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        HomeGlyph(glyph, label, tint = tint)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppRow(app: AppEntry, onClick: () -> Unit, onLongClick: () -> Unit, letter: String? = null) {
    val ink = LocalInk.current
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).combinedClickable(role = Role.Button,
        onLongClickLabel = stringResource(R.string.app_options), onClick = onClick, onLongClick = onLongClick)
        .padding(start = 24.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (letter != null) Text(letter, Modifier.width(26.dp).semantics { hideFromAccessibility() },
            style = CrystalType.label.copy(letterSpacing = 0.sp), color = ink.faint)
        AppIcon(app, Modifier.size(32.dp))
        Text(app.label, Modifier.padding(start = 16.dp).weight(1f), style = CrystalType.body.copy(fontSize = 17.sp),
            color = if (app.suspended) ink.faint else ink.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Drawer(apps: List<AppEntry>, favorites: List<AppEntry>, onLaunch: (AppEntry) -> Unit, onOptions: (AppEntry) -> Unit) {
    val ink = LocalInk.current
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val letters = remember(apps) { apps.map { indexLetter(it.label) } }
    val first = remember(letters) { HashMap<Char, Int>().apply { letters.forEachIndexed { i, c -> putIfAbsent(c, i) } } }
    val haptics = rememberInkHaptics()
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp)) {
            Text(stringResource(R.string.mine).uppercase(), Modifier.padding(start = 24.dp, bottom = 10.dp), style = CrystalType.label, color = ink.muted)
            if (favorites.isEmpty()) Text(stringResource(R.string.mine_empty), Modifier.padding(horizontal = 24.dp),
                style = CrystalType.caption, color = ink.muted)
            else Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                favorites.forEach { app ->
                    Column(Modifier.width(72.dp).combinedClickable(role = Role.Button,
                        onLongClickLabel = stringResource(R.string.app_options), onClick = { onLaunch(app) }, onLongClick = { onOptions(app) })
                        .padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        AppIcon(app, Modifier.size(44.dp))
                        Text(app.label, Modifier.padding(top = 6.dp), style = CrystalType.caption.copy(fontSize = 11.sp, lineHeight = 14.sp),
                            color = ink.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Hairline()
        BoxWithConstraints(Modifier.weight(1f)) {
        // The rail needs room for 27 legible letters; above a raised keyboard it steps aside.
        val rail = maxHeight >= 380.dp
        Row(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.weight(1f), state = state, contentPadding = PaddingValues(top = 6.dp, bottom = 12.dp)) {
                itemsIndexed(apps, key = { _, app -> app.key }) { index, app ->
                    val letter = letters[index]
                    AppRow(app, onClick = { onLaunch(app) }, onLongClick = { onOptions(app) },
                        letter = if (index == 0 || letters[index - 1] != letter) letter.toString() else "")
                }
            }
            if (rail) AlphabetRail(first.keys, onLetter = { c ->
                val target = RAIL.substring(RAIL.indexOf(c)).firstNotNullOfOrNull { first[it] } ?: return@AlphabetRail
                haptics.tick()
                scope.launch { state.scrollToItem(target) }
            })
        }
        }
    }
}

/** Letters down the right edge; present ones in ink. Touch or drag to jump. */
@Composable
private fun AlphabetRail(present: Set<Char>, onLetter: (Char) -> Unit) {
    val ink = LocalInk.current
    var active by remember { mutableStateOf<Char?>(null) }
    fun pick(y: Float, height: Int) {
        val c = RAIL[(y / height * RAIL.length).toInt().coerceIn(0, RAIL.lastIndex)]
        if (c != active) { active = c; onLetter(c) }
    }
    Column(Modifier.width(32.dp).fillMaxHeight().padding(vertical = 8.dp).semantics { hideFromAccessibility() }
        .pointerInput(Unit) { detectTapGestures(onPress = { pick(it.y, size.height); tryAwaitRelease(); active = null }) }
        .pointerInput(Unit) {
            detectVerticalDragGestures(onDragStart = { pick(it.y, size.height) }, onDragEnd = { active = null },
                onDragCancel = { active = null }) { change, _ -> pick(change.position.y, size.height) }
        },
        verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
        RAIL.forEach { c ->
            val on = c == active
            Text(c.toString(), style = CrystalType.label.copy(fontSize = if (on) 13.sp else 10.sp, letterSpacing = 0.sp,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium),
                color = when { on -> ink.accent; c in present -> ink.ink; else -> ink.line })
        }
    }
}

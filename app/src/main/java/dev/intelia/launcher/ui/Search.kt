package dev.intelia.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.intelia.launcher.R
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.flow.Flow
import dev.intelia.launcher.flow.FlowAlias
import dev.intelia.launcher.search.AppSearch
import kotlinx.coroutines.launch

private const val RAIL = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ"

private fun indexLetter(label: String): Char =
    AppSearch.fold(label).firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' } ?: '#'

/**
 * Search and the drawer are one page: the field sits at the thumb, an empty query shows "Mine"
 * and A–Z, typing turns the list into results that grow upwards from the field. Enter runs an
 * exact FLOW match or opens the first app; typing alone never runs anything (D5).
 */
@Composable
fun SearchPage(
    model: HomeViewModel,
    settings: LauncherSettings,
    requestKeyboard: Boolean,
    flowSteps: (FlowAlias) -> List<String?>,
    onBack: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onRun: (FlowAlias) -> Unit,
    onOptions: (AppEntry) -> Unit,
    onFlows: () -> Unit,
    onStudio: () -> Unit,
) {
    val ink = LocalInk.current
    val query by model.query.collectAsStateWithLifecycle()
    val apps by model.visibleApps.collectAsStateWithLifecycle()
    val results by model.results.collectAsStateWithLifecycle()
    val flows by model.flowMatches.collectAsStateWithLifecycle()
    val favorites by model.favoriteApps.collectAsStateWithLifecycle()
    val field = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val drawer = query.isBlank()
    val exact = remember(settings.aliases, query) { Flow.exact(settings.aliases, query) }

    // §5.6: from LENS the ring becomes the field; focus and the IME come in the first frame, the field rides the IME.
    val morph = remember { Animatable(if (requestKeyboard) 0f else 1f) }
    val grow = motionMs(170)
    val growDelay = motionMs(70)
    val shrink = motionMs(160)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        if (requestKeyboard) {
            field.requestFocus(); keyboard?.show()
            morph.animateTo(1f, tween(grow, delayMillis = growDelay, easing = CubicBezierEasing(.05f, .7f, .1f, 1f)))
        }
    }
    fun leave() {
        if (!requestKeyboard) { onBack(); return }
        scope.launch { keyboard?.hide(); morph.animateTo(0f, tween(shrink)); onBack() }
    }
    BackHandler { leave() }

    Column(Modifier.fillMaxSize().background(ink.paper).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 4.dp).heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically) {
            BackButton(stringResource(R.string.home), ::leave)
            if (drawer) Text(stringResource(R.string.apps), Modifier.padding(start = 4.dp), style = InkText.title.copy(fontSize = 30.sp))
            Spacer(Modifier.weight(1f))
            Kicker(if (drawer) apps.size.toString() else stringResource(R.string.results_count, results.size + flows.size),
                Modifier.padding(end = 6.dp), color = ink.faint)
            IconAction(Glyph.Flow, stringResource(R.string.flow), onFlows)
            IconAction(Glyph.Studio, stringResource(R.string.studio), onStudio)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (drawer) Drawer(apps, favorites, onLaunch, onOptions)
            else Results(query, results, flows, exact, flowSteps, onLaunch, onRun, onOptions, onComplete = { model.query.value = it })
        }
        SearchField(query, field, morph.value, lensShift(settings.reach), exactMatch = exact != null, onChange = { model.query.value = it }, onSubmit = {
            when {
                exact != null -> onRun(exact)
                results.isNotEmpty() -> onLaunch(results.first())
            }
        })
    }
}

@Composable
internal fun IconAction(glyph: Glyph, label: String, onClick: () -> Unit, tint: androidx.compose.ui.graphics.Color = LocalInk.current.ink) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        HomeGlyph(glyph, label, tint = tint)
    }
}

/** An underline field: the open LENS ring, the query, and what Enter will do. */
@Composable
private fun SearchField(query: String, focus: FocusRequester, morph: Float, lensDx: Dp, exactMatch: Boolean, onChange: (String) -> Unit, onSubmit: () -> Unit) {
    val ink = LocalInk.current
    val description = stringResource(R.string.lens_description)
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp).height(56.dp)
        .drawWithContent {
            if (morph >= 1f) { drawContent(); return@drawWithContent }
            // The LENS ring opening into the field: a rounded rect from the ring's place to the full row.
            val r = CrystalDims.lensRing.toPx() / 2f
            val cx = size.width / 2f + lensDx.toPx()
            val cy = size.height / 2f - (1f - morph) * 14.dp.toPx()
            val left = cx - r + (0f - (cx - r)) * morph
            val right = cx + r + (size.width - (cx + r)) * morph
            val top = cy - r + (0f - (cy - r)) * morph
            val bottom = cy + r + (size.height - (cy + r)) * morph
            clipRect(left, top, right, bottom) { this@drawWithContent.drawContent() }
            drawRoundRect(ink.ink.copy(alpha = .88f * (1f - morph)), Offset(left, top), Size(right - left, bottom - top),
                CornerRadius(r * (1f - morph)), style = Stroke(1.5.dp.toPx()))
        }
        .drawBehind {
            val y = size.height - 1.dp.toPx()
            drawLine(ink.ink, Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx())
        }.padding(start = 12.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(16.dp)) { drawCircle(ink.ink, size.minDimension / 2f - .75.dp.toPx(), style = Stroke(1.5.dp.toPx())) }
        BasicTextField(query, onChange, Modifier.weight(1f).padding(start = 12.dp).focusRequester(focus).semantics { contentDescription = description },
            singleLine = true, textStyle = InkText.body.copy(fontSize = 18.sp, color = ink.ink), cursorBrush = SolidColor(ink.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go), keyboardActions = KeyboardActions(onGo = { onSubmit() }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text(stringResource(R.string.search), style = InkText.body.copy(fontSize = 18.sp), color = ink.faint)
                    inner()
                }
            })
        if (exactMatch) Kicker(stringResource(R.string.enter_runs), Modifier.padding(horizontal = 6.dp), color = ink.accent)
        if (query.isNotEmpty()) IconAction(Glyph.Close, stringResource(R.string.clear), { onChange("") }, tint = ink.muted)
    }
}

@Composable
private fun Results(
    query: String,
    results: List<AppEntry>,
    flows: List<FlowAlias>,
    exact: FlowAlias?,
    flowSteps: (FlowAlias) -> List<String?>,
    onLaunch: (AppEntry) -> Unit,
    onRun: (FlowAlias) -> Unit,
    onOptions: (AppEntry) -> Unit,
    onComplete: (String) -> Unit,
) {
    val ink = LocalInk.current
    val missing = stringResource(R.string.flow_target_missing)
    val others = flows.filter { it != exact }
    // Reverse layout: the first item sits right above the field, nearest the thumb.
    LazyColumn(Modifier.fillMaxSize(), reverseLayout = true, contentPadding = PaddingValues(vertical = 8.dp)) {
        if (exact != null) item(key = "exact") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 10.dp, bottom = 12.dp)) {
                Hairline()
                Kicker(stringResource(R.string.flow_exact), Modifier.padding(top = 16.dp, bottom = 6.dp), color = ink.accent)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(exact.name, style = InkText.title.copy(fontSize = 40.sp, lineHeight = 44.sp, fontStyle = FontStyle.Italic),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        BeadThread(flowSteps(exact), missing, Modifier.padding(top = 6.dp))
                    }
                    Capsule(stringResource(R.string.flow_run), { onRun(exact) })
                }
            }
        }
        if (results.isEmpty() && others.isEmpty() && exact == null) item(key = "none") {
            Text(stringResource(R.string.no_results, query.trim()), Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                style = InkText.body, color = ink.muted)
        }
        items(results, key = { it.key }) { app -> AppRow(app, onClick = { onLaunch(app) }, onLongClick = { onOptions(app) }) }
        if (others.isNotEmpty()) {
            items(others, key = { "flow:" + it.id }) { alias ->
                // Completing the name is safe: it only turns the row into the exact match above.
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClickLabel = alias.name) { onComplete(alias.name) }
                    .padding(horizontal = 24.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(alias.name, style = InkText.item.copy(fontStyle = FontStyle.Italic), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        BeadThread(flowSteps(alias), missing)
                    }
                    Quiet(stringResource(R.string.flow_run), { onRun(alias) })
                }
            }
            item(key = "flows-kicker") { Kicker(stringResource(R.string.flow), Modifier.padding(start = 24.dp, top = 12.dp, bottom = 2.dp), color = ink.accent) }
        }
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
            style = InkText.label.copy(letterSpacing = 0.sp), color = ink.faint)
        AppIcon(app, Modifier.size(32.dp))
        Text(app.label, Modifier.padding(start = 16.dp).weight(1f), style = InkText.item.copy(fontSize = 20.sp),
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
            Kicker(stringResource(R.string.mine), Modifier.padding(start = 24.dp, bottom = 10.dp), color = ink.accent)
            if (favorites.isEmpty()) Text(stringResource(R.string.mine_empty), Modifier.padding(horizontal = 24.dp),
                style = InkText.small, color = ink.muted)
            else Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                favorites.forEach { app ->
                    Column(Modifier.width(72.dp).combinedClickable(role = Role.Button,
                        onLongClickLabel = stringResource(R.string.app_options), onClick = { onLaunch(app) }, onLongClick = { onOptions(app) })
                        .padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        AppIcon(app, Modifier.size(44.dp))
                        Text(app.label, Modifier.padding(top = 6.dp), style = InkText.small.copy(fontSize = 11.sp, lineHeight = 14.sp),
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
            Text(c.toString(), style = InkText.label.copy(fontSize = if (on) 13.sp else 10.sp, letterSpacing = 0.sp,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium),
                color = when { on -> ink.accent; c in present -> ink.ink; else -> ink.line })
        }
    }
}

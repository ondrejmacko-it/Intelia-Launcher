package dev.intelia.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import dev.intelia.launcher.data.Anchor
import dev.intelia.launcher.data.AnchorType
import dev.intelia.launcher.data.ItemKind
import dev.intelia.launcher.data.LayoutSpec
import dev.intelia.launcher.data.HomeLayouts
import dev.intelia.launcher.data.HomeTemplates
import dev.intelia.launcher.data.LayoutBase
import dev.intelia.launcher.data.LensStyle
import dev.intelia.launcher.data.Placement
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import kotlin.math.hypot
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.intelia.launcher.R
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.ShortcutEntry
import dev.intelia.launcher.flow.FlowAlias
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Page { Home, Search, Flows, FlowEdit, Studio }

@Composable
fun HomeRoot(model: HomeViewModel, homeSignal: Int) {
    val settings by model.settings.collectAsStateWithLifecycle()
    InteliaTheme(settings) {
        if (settings.onboarded) HomeContent(model, settings, homeSignal)
        else Onboarding(model, settings)
    }
}

/** Current local time, updated on the minute while Home is started; no per-second work. */
@Composable
fun rememberNow(): State<LocalDateTime> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return produceState(LocalDateTime.now(), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                value = LocalDateTime.now()
                delay(60_000L - System.currentTimeMillis() % 60_000L)
            }
        }
    }
}

fun dateLine(now: LocalDateTime): String =
    now.format(DateTimeFormatter.ofPattern("EEEE d. MMMM", Locale.getDefault())).uppercase(Locale.getDefault())

@Composable
private fun HomeContent(model: HomeViewModel, settings: LauncherSettings, homeSignal: Int) {
    val context = LocalContext.current
    val favorites by model.favoriteApps.collectAsStateWithLifecycle()
    val arcApps by model.arcApps.collectAsStateWithLifecycle()
    val arcFlows by model.arcFlows.collectAsStateWithLifecycle()
    val arcShortcuts by model.arcShortcuts.collectAsStateWithLifecycle()
    val visible by model.visibleApps.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(Page.Home) }
    var searchKeyboard by rememberSaveable { mutableStateOf(true) }
    /** FLOW being edited: an id, or "" for a new command. */
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var editBack by rememberSaveable { mutableStateOf(Page.Flows) }
    var options by remember { mutableStateOf<AppEntry?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val ink = LocalInk.current
    val homeInk = LocalHomeInk.current

    fun message(id: Int) {
        scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(context.getString(id)) }
    }
    fun goHome() {
        page = Page.Home; model.query.value = ""; options = null; editing = null; focus.clearFocus(); keyboard?.hide()
    }
    fun openSearch(withKeyboard: Boolean) { searchKeyboard = withKeyboard; model.query.value = ""; page = Page.Search }
    fun launch(entry: AppEntry) { if (model.launch(entry)) goHome() else message(R.string.launch_failed) }
    fun launch(shortcut: ShortcutEntry) { if (model.launch(shortcut)) goHome() else message(R.string.launch_failed) }
    fun run(alias: FlowAlias) {
        when (model.runFlow(alias)) {
            FlowRun.Launched -> goHome()
            FlowRun.Unavailable -> message(R.string.flow_unavailable)
            FlowRun.Failed -> message(R.string.launch_failed)
        }
    }
    fun edit(alias: FlowAlias?, from: Page) { editBack = from; editing = alias?.id ?: ""; focus.clearFocus(); keyboard?.hide(); page = Page.FlowEdit }
    val steps: (FlowAlias) -> List<String?> = { alias -> alias.targets.map { model.entry(it)?.label } }

    LaunchedEffect(homeSignal) { if (homeSignal > 0) goHome() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { model.refreshShortcuts() }
    // Search is a paper surface; never leave it behind for Recents or the next return Home.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { if (page == Page.Search) goHome() }
    // A launcher never finishes on back: back always steps toward Home.
    BackHandler {
        when (page) {
            Page.FlowEdit -> { editing = null; page = editBack }
            Page.Home -> Unit
            else -> goHome()
        }
    }

    // Studio sets its own: its top is the wallpaper.
    if (page != Page.Studio) SystemBarIcons(darkIcons = if (page == Page.Home) homeInk.luminance() < .5f else !ink.dark)
    val enter = motionMs(180)
    val exit = motionMs(140)
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(page, transitionSpec = { fadeIn(tween(enter)) togetherWith fadeOut(tween(exit)) },
            label = "page") { target ->
            when (target) {
                Page.Home -> Home(
                    settings = settings,
                    apps = remember(favorites, visible) { (visible + favorites).associateBy { it.key } },
                    arc = remember(arcApps, arcFlows, arcShortcuts, settings.arcRings) {
                        ArcContent(arcApps, arcFlows, arcShortcuts, settings.arcRings)
                    },
                    flowSteps = steps,
                    onSearch = { openSearch(true) },
                    onDrawer = { openSearch(false) },
                    onStudio = { page = Page.Studio },
                    onLaunch = ::launch,
                    onOptions = { options = it },
                    onRun = ::run,
                    onShortcut = ::launch,
                    onMessage = ::message,
                    onSaveLayout = { edited ->
                        model.updateSettings { it.copy(home = it.home.save(edited)) }
                        message(R.string.edit_saved)
                    },
                )
                Page.Search -> SearchPage(
                    model = model,
                    settings = settings,
                    requestKeyboard = searchKeyboard,
                    flowSteps = steps,
                    onBack = ::goHome,
                    onLaunch = ::launch,
                    onRun = ::run,
                    onOptions = { focus.clearFocus(); keyboard?.hide(); options = it },
                    onFlows = { focus.clearFocus(); keyboard?.hide(); page = Page.Flows },
                    onStudio = { focus.clearFocus(); keyboard?.hide(); page = Page.Studio },
                )
                Page.Flows -> FlowList(
                    aliases = settings.aliases,
                    arcFlows = settings.arcFlows,
                    flowSteps = steps,
                    onBack = ::goHome,
                    onCreate = { edit(null, Page.Flows) },
                    onEdit = { edit(it, Page.Flows) },
                    onRun = ::run,
                )
                Page.FlowEdit -> {
                    val id = editing
                    val initial = settings.aliases.firstOrNull { it.id == id }
                    FlowEditor(
                        initial = initial,
                        inArc = initial != null && initial.id in settings.arcFlows,
                        arcFull = settings.arcFlows.size >= LauncherSettings.ARC_FLOW_SLOTS,
                        apps = visible,
                        resolve = model::entry,
                        onSave = { alias, inArc ->
                            model.saveAlias(alias, inArc).also { if (it == null) { editing = null; page = editBack } }
                        },
                        onDelete = { deleted -> model.deleteAlias(deleted); editing = null; page = editBack },
                        onBack = { editing = null; page = editBack },
                    )
                }
                Page.Studio -> Studio(model, settings, onBack = ::goHome, onFlows = { page = Page.Flows })
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 132.dp)) { data ->
            InkSnackbar(data.visuals.message)
        }
    }

    options?.let { entry ->
        AppOptionsSheet(
            app = entry,
            inArc = entry.key in settings.arc,
            arcFull = settings.arc.size >= LauncherSettings.ARC_SLOTS,
            favorite = entry.key in settings.favorites,
            favoritesFull = settings.favorites.size >= LauncherSettings.MAX_FAVORITES,
            onArc = { model.toggleArc(entry); options = null },
            onFavorite = { model.toggleFavorite(entry); options = null },
            onHide = { model.hide(entry); options = null },
            onDetails = { options = null; if (!model.openDetails(entry)) message(R.string.launch_failed) },
            onDismiss = { options = null },
        )
    }
}

/**
 * Home draws nothing behind itself: no background, scrim or gradient, so the system wallpaper is
 * all there is when the window is hidden for Recents. Type legibility comes from [homeShadow]
 * and the wallpaper-derived ink. The arrangement comes from the active layout (§5.4); long-press
 * on Home or an icon opens the Home Editor in place (§5.5), and only then is a 20 % scrim drawn.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Home(
    settings: LauncherSettings,
    apps: Map<String, AppEntry>,
    arc: ArcContent,
    flowSteps: (FlowAlias) -> List<String?>,
    onSearch: () -> Unit,
    onDrawer: () -> Unit,
    onStudio: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onOptions: (AppEntry) -> Unit,
    onRun: (FlowAlias) -> Unit,
    onShortcut: (ShortcutEntry) -> Unit,
    onMessage: (Int) -> Unit,
    onSaveLayout: (LayoutSpec) -> Unit,
) {
    val homeInk = LocalHomeInk.current
    val accent = LocalInk.current.accent
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val now by rememberNow()
    var arcOpen by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<HomeEditorState?>(null) }
    /** Bumped after every editor call; [HomeEditorState] is plain Kotlin. */
    var tick by remember { mutableIntStateOf(0) }
    /** Long-press on empty Home: the editor opens in the next composition, where the geometry is known. */
    var openRequested by remember { mutableStateOf(false) }
    var leaveThen by remember { mutableStateOf<(() -> Unit)?>(null) }
    var clockZone by remember { mutableStateOf<Zone?>(null) }
    /** Editor: the item whose inspector is open, the empty spot of the Add menu, its picker, the preset strip. */
    var selected by remember { mutableStateOf<String?>(null) }
    var addPoint by remember { mutableStateOf<Pt?>(null) }
    var picker by remember { mutableStateOf<ItemKind?>(null) }
    var presetsOpen by remember { mutableStateOf(false) }
    var previewKey by remember { mutableStateOf<String?>(null) }
    var presetApplied by remember { mutableStateOf(false) }
    var ask by remember { mutableStateOf<EditorAsk?>(null) }
    var afterSave by remember { mutableStateOf<(() -> Unit)?>(null) }
    val flows = remember(settings.aliases) { settings.aliases.associateBy { it.id } }
    // ARC speaks over the top of Home, so the clock and favourites step fully aside.
    val quiet by animateFloatAsState(if (arcOpen) 0f else 1f, tween(motionMs(180)), label = "home-quiet")
    val swipe = with(density) { 64.dp.toPx() }
    val editLabel = stringResource(R.string.edit_home)
    val moveLabel = stringResource(R.string.a11y_move)
    val removeLabel = stringResource(R.string.inspect_remove)

    fun refresh() { tick++ }
    fun close() {
        editor = null; leaveThen = null; selected = null; addPoint = null; picker = null
        presetsOpen = false; previewKey = null; presetApplied = false; ask = null; afterSave = null; refresh()
    }
    fun commitSave() { editor?.let { it.cancelPreview(); onSaveLayout(it.layout); it.markSaved() }; close() }
    /** ✓: the user's own layout is never replaced by a preset without a question. */
    fun save(then: () -> Unit = {}) {
        val ed = editor ?: return
        ed.cancelPreview(); previewKey = null; leaveThen = null
        val ownExists = settings.home.layouts.any { it.id == HomeLayouts.USER_ID }
        if (ed.dirty && ownExists && (settings.home.active != HomeLayouts.USER_ID || presetApplied)) { afterSave = then; ask = EditorAsk.OverwriteOwn }
        else { commitSave(); then() }
    }
    fun applyPreset() {
        val e = editor ?: return
        e.confirmPreview()
        if (previewKey != OWN_KEY) presetApplied = true
        previewKey = null; presetsOpen = false
    }
    fun add(e: HomeEditorState, kind: ItemKind, target: String) {
        val at = addPoint ?: return
        if (!e.addAt(kind, target, at)) onMessage(R.string.add_full)
        picker = null; addPoint = null; refresh()
    }
    fun leave(then: () -> Unit) { if (editor?.dirty == true) leaveThen = then else { close(); then() } }
    BackHandler(enabled = editor != null) { leave {} }

    // The chain stays the same in edit mode: swapping modifiers mid-gesture would cancel an icon's hold-and-drag.
    BoxWithConstraints(Modifier.fillMaxSize()
        .pointerInput(Unit) {
            var drag = 0f
            detectVerticalDragGestures(onDragStart = { drag = 0f }, onVerticalDrag = { _, amount -> drag += amount },
                onDragEnd = { if (drag < -swipe && editor == null) onDrawer() })
        }.combinedClickable(interactionSource = null, indication = null, onLongClickLabel = editLabel, onClick = {},
            onLongClick = { if (editor == null) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); openRequested = true } })) {
        val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
        val statusTop = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
        val cx = maxWidth / 2 + lensShift(settings.reach)
        val cy = maxHeight - bottomInset - LensLift
        val compact = maxHeight < 560.dp
        val geometry = HomeGeometry(maxWidth.value, maxHeight.value, statusTop.value, cx.value, cy.value)
        check(tick >= 0) // read so that editor changes recompose this scope
        val saved = settings.home.activeLayout()
        fun open(): HomeEditorState = editor ?: HomeEditorState(saved, geometry, blocked = { listOfNotNull(clockZone) })
            .also { editor = it; refresh() }
        if (openRequested) { openRequested = false; open() }
        val ed = editor
        ed?.geometry = geometry
        val base = ed?.layout ?: saved
        // While dragging, the clock and a displaced icon preview their targets; the dragged icon follows the finger.
        val drag = ed?.drag
        val layout = if (drag == null) base else base.copy(items = base.items.map {
            if (it.id == drag.id && it.kind == ItemKind.Clock) it.copy(anchor = drag.target)
            else if (it.id == drag.displaced?.first) it.copy(anchor = drag.displaced.second) else it
        })
        val clock = layout.items.firstOrNull { it.kind == ItemKind.Clock }
        val date = layout.items.firstOrNull { it.kind == ItemKind.Date }
        val listLayout = layout.items.any { it.anchor.type == AnchorType.Stack }
        val lensItem = layout.items.firstOrNull { it.kind == ItemKind.Lens }
        val lensPt = Pt(geometry.lensX, geometry.lensY)
        val barBottom = statusTop.value + 56f

        if (ed != null) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .20f)))
            EditorMarks(ed, layout)
        }

        if (clock != null || date != null) {
            val a = clock?.anchor ?: Anchor.free(if (listLayout) 0f else .5f, 0f)
            val bias = (a.fx * 2f - 1f).coerceIn(-1f, 1f)
            val topPad = (if (compact) 24.dp else if (listLayout) 72.dp else 92.dp) + (geometry.areaH * a.fy).dp
            Column(
                Modifier.fillMaxWidth().statusBarsPadding().padding(top = topPad).padding(horizontal = 28.dp)
                    .wrapContentWidth(BiasAlignment.Horizontal(bias)).graphicsLayer { alpha = quiet }
                    .onGloballyPositioned { c ->
                        val r = c.boundsInRoot()
                        val d = density.density
                        clockZone = Zone(r.left / d - 8f, r.top / d - 8f, r.right / d + 8f, r.bottom / d + 8f)
                    },
                horizontalAlignment = if (bias < 0f) Alignment.Start else if (bias > 0f) Alignment.End else Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (clock != null) SplitClock(now.toLocalTime(), clock.clockStyle(settings.clockStyle), clock.tintColor(accent) ?: homeInk, accent,
                    (if (clock.sizeDp == 0) (if (compact) 64 else ItemSizes.CLOCK) else clock.sizeDp).sp,
                    Modifier.graphicsLayer { alpha = clock.alpha }, weight = clock.weight)
                // 4.2: 12 sp caps, +2.2 tracking, fg2 (62 %).
                if (date != null) Text(dateLine(now), style = CrystalType.date.copy(shadow = homeShadow(homeInk)),
                    color = homeInk.copy(alpha = .62f), maxLines = 1)
            }
        }

        // Icons placed by geometry: LENS row, grid cells, free positions; apps and FLOWs.
        layout.items.forEach { item ->
            if ((item.kind != ItemKind.App && item.kind != ItemKind.Flow) || !item.anchor.type.isPlaced()) return@forEach
            val app = if (item.kind == ItemKind.App) apps[item.target] else null
            val flow = if (item.kind == ItemKind.Flow) flows[item.target] else null
            if (app == null && flow == null) return@forEach
            val name = app?.label ?: flow?.name.orEmpty()
            // The node stays put while it is dragged (its gesture needs a still frame); a ghost follows the finger.
            val dragging = drag?.id == item.id
            val p = geometry.position(item, layout) ?: return@forEach
            val iconDp = item.iconDp().dp
            key(item.id) {
                if (item.label) Text(name, Modifier.centerAt(p.x.dp, p.y.dp + iconDp / 2 + 12.dp).widthIn(max = 76.dp)
                    .graphicsLayer { alpha = if (dragging) 0f else quiet }.clearAndSetSemantics {},
                    style = CrystalType.body.copy(fontSize = 12.sp, lineHeight = 14.sp, shadow = homeShadow(homeInk)),
                    color = homeInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.centerAt(p.x.dp, p.y.dp).size(maxOf(CrystalDims.touch, iconDp + 8.dp)).graphicsLayer { alpha = if (dragging) 0f else quiet }
                    .clip(CircleShape)
                    .pointerInput(item.id) {
                        // Hold an icon: the editor opens and the icon is already in the hand (§5.5).
                        var moved = Offset.Zero
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                moved = Offset.Zero
                                open().startDrag(item.id)
                                refresh()
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                moved += amount
                                editor?.dragBy(Pt(moved.x / density.density, moved.y / density.density))
                                refresh()
                            },
                            onDragEnd = { if (editor?.drop() == true) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); refresh() },
                            onDragCancel = { editor?.cancelDrag(); refresh() },
                        )
                    }
                    .clickable(role = Role.Button) {
                        when {
                            editor != null -> { selected = item.id; refresh() }
                            app != null -> onLaunch(app)
                            flow != null -> onRun(flow)
                        }
                    }
                    .semantics {
                        contentDescription = name
                        // TalkBack (§4.8): move, edit and remove without the hold-and-drag gesture.
                        if (editor == null) customActions = listOf(
                            CustomAccessibilityAction(moveLabel) { open(); selected = item.id; refresh(); true },
                            CustomAccessibilityAction(editLabel) { open(); selected = item.id; refresh(); true },
                            CustomAccessibilityAction(removeLabel) { open().remove(item.id); refresh(); true },
                        )
                    }, contentAlignment = Alignment.Center) {
                    // No backing (4.3): system or icon-pack icons as they are; monochrome tinted fg.
                    if (app != null) AppIcon(app, Modifier.size(iconDp), tint = homeInk)
                    else HomeGlyph(Glyph.Flow, null, tint = homeInk, size = iconDp * .7f)
                }
            }
        }

        drag?.let { d -> ed.item(d.id)?.takeIf { it.kind == ItemKind.App || it.kind == ItemKind.Flow }?.let { item ->
            val app = apps[item.target]
            if (app == null && flows[item.target] == null) return@let
            Box(Modifier.centerAt(d.point.x.dp, d.point.y.dp).size(maxOf(CrystalDims.touch, item.iconDp().dp + 8.dp))
                .graphicsLayer { scaleX = 1.08f; scaleY = 1.08f }, contentAlignment = Alignment.Center) {
                if (app != null) AppIcon(app, Modifier.size(item.iconDp().dp), tint = homeInk)
                else HomeGlyph(Glyph.Flow, null, tint = homeInk, size = item.iconDp().dp * .7f)
            }
        } }

        // The legacy text list (preset "Vlastní").
        val stack = layout.items.filter { it.kind == ItemKind.App && it.anchor.type == AnchorType.Stack }.sortedBy { it.anchor.row }
        if (stack.isNotEmpty()) Column(
            Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 28.dp, bottom = bottomInset + LensLift + 64.dp)
                .graphicsLayer { alpha = quiet },
        ) {
            stack.forEach { item ->
                val app = apps[item.target] ?: return@forEach
                Text(app.label, Modifier.widthIn(max = 320.dp).heightIn(min = 48.dp)
                    .combinedClickable(role = Role.Button, onLongClickLabel = stringResource(R.string.app_options),
                        onClick = { onLaunch(app) }, onLongClick = { onOptions(app) })
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                    style = InkText.item.copy(fontSize = 30.sp, lineHeight = 34.sp, shadow = homeShadow(homeInk)),
                    color = homeInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        LensArc(arc, settings.reach, flowSteps, onSearch = onSearch, onDrawer = onDrawer, onLaunch = onLaunch, onRun = onRun,
            onShortcut = onShortcut, onOpenChange = { arcOpen = it }, lensRing = lensItem?.lensDp()?.dp ?: CrystalDims.lensRing,
            lensFilled = lensItem?.style == LensStyle.Filled.name)
        if (settings.pulse && ed == null) Pulse(settings)

        if (ed != null) {
            // Editor layer above LENS: a hold on LENS never opens ARC here; drags move elements.
            Box(Modifier.fillMaxSize()
                .pointerInput(ed) {
                    detectTapGestures(onTap = { o ->
                        val p = Pt(o.x / density.density, o.y / density.density)
                        val onLens = hypot(p.x - lensPt.x, p.y - lensPt.y) <= 34f
                        val hit = hitItem(ed, p, clockZone) ?: lensItem?.id?.takeIf { onLens }
                        when {
                            hit != null -> { selected = hit.takeIf { it != selected }; addPoint = null }
                            selected != null || addPoint != null -> { selected = null; addPoint = null }
                            presetsOpen -> { ed.cancelPreview(); presetsOpen = false; previewKey = null }
                            p.y > barBottom && hypot(p.x - lensPt.x, p.y - lensPt.y) > 50f -> addPoint = p
                        }
                        refresh()
                    })
                }
                .pointerInput(ed) {
                    var moved = Offset.Zero
                    detectDragGestures(
                        onDragStart = { o ->
                            moved = Offset.Zero
                            hitItem(ed, Pt(o.x / density.density, o.y / density.density), clockZone)?.let { ed.startDrag(it) }
                            refresh()
                        },
                        onDrag = { change, amount ->
                            if (ed.drag != null) {
                                change.consume()
                                moved += amount
                                ed.dragBy(Pt(moved.x / density.density, moved.y / density.density))
                                refresh()
                            }
                        },
                        onDragEnd = { if (ed.drop()) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); refresh() },
                        onDragCancel = { ed.cancelDrag(); refresh() },
                    )
                })
            EditorBar(ed, presetsOpen, onPresets = {
                    if (presetsOpen) { ed.cancelPreview(); previewKey = null }
                    presetsOpen = !presetsOpen; selected = null; addPoint = null; refresh()
                }, onStudio = { leave(onStudio) }, onDone = { save() },
                modifier = Modifier.align(Alignment.TopCenter))

            if (presetsOpen) {
                val start = ed.previewOf ?: ed.layout
                val favs = settings.favorites
                val choices = listOf(LayoutBase.Pure to R.string.layout_pure, LayoutBase.Essentials to R.string.layout_essentials,
                    LayoutBase.Balanced to R.string.preset_balanced).map { (b, name) ->
                    PresetChoice(b.name, stringResource(name), HomeTemplates.preview(b, favs, start))
                } + listOfNotNull(settings.home.layouts.firstOrNull { it.id == HomeLayouts.USER_ID }
                    ?.let { PresetChoice(OWN_KEY, stringResource(R.string.preset_own), it) })
                PresetStrip(choices, previewKey, geometry, placementLabel(ed.layout),
                    onPlacement = { ed.setPlacement(Placement.entries[(ed.layout.placement.ordinal + 1) % Placement.entries.size]); refresh() },
                    onPick = { ed.preview(it.spec); previewKey = it.key; refresh() },
                    onApply = { if (ed.canUndo) ask = EditorAsk.Replace else { applyPreset(); refresh() } },
                    onCancel = { ed.cancelPreview(); presetsOpen = false; previewKey = null; refresh() },
                    onReset = { ask = EditorAsk.Reset },
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = barBottom.dp))
            }

            val sel = selected?.let(ed::item)
            if (sel != null && drag == null) {
                val zone = when (sel.kind) {
                    ItemKind.Clock -> clockZone
                    ItemKind.Lens -> (sel.lensDp() / 2f + 6f).let { r -> Zone(lensPt.x - r, lensPt.y - r, lensPt.x + r, lensPt.y + r) }
                    else -> ed.pointOf(sel)?.let { c ->
                        val r = sel.iconDp() / 2f + 6f
                        Zone(c.x - r, c.y - r, c.x + r, c.y + r + if (sel.label) 20f else 0f)
                    }
                }
                if (sel.kind == ItemKind.Lens) GhostArc(lensPt, Modifier.fillMaxSize())
                if (sel.kind == ItemKind.Clock && zone != null) ClockHandle(zone) { dx ->
                    val now = if (sel.sizeDp == 0) ItemSizes.CLOCK else sel.sizeDp
                    ed.edit(sel.id, "size") { it.copy(sizeDp = (now + dx.roundToInt()).coerceIn(ItemSizes.CLOCK_MIN, ItemSizes.CLOCK_MAX)) }
                    refresh()
                }
                if (zone != null) key(sel.id) { InspectorCard(zone, maxWidth.value, barBottom) {
                    when (sel.kind) {
                        ItemKind.Clock -> ClockInspector(ed, sel, settings.clockStyle, ::refresh)
                        ItemKind.Lens -> LensInspector(ed, sel, ::refresh)
                        else -> {
                            val app = apps[sel.target]
                            ItemInspector(ed, sel, app?.label ?: flows[sel.target]?.name.orEmpty(),
                                onMore = app?.let { a -> { onOptions(a) } }, onRemoved = { selected = null }, onChange = ::refresh)
                        }
                    }
                } }
            }
            addPoint?.let { at ->
                AddMenu(at, maxWidth.value, barBottom, onApp = { picker = ItemKind.App }, onFlow = { picker = ItemKind.Flow })
            }
        }
    }

    val ed = editor
    if (ed != null) picker?.let { kind ->
        val onHome = ed.layout.items.mapTo(HashSet()) { it.target }
        AddPicker(stringResource(if (kind == ItemKind.App) R.string.pick_app else R.string.pick_flow),
            if (kind == ItemKind.App) apps.values.filter { it.key !in onHome }.sortedBy { it.label.lowercase() } else emptyList(),
            if (kind == ItemKind.Flow) settings.aliases.filter { it.id !in onHome } else emptyList(),
            onApp = { a -> add(ed, ItemKind.App, a.key) }, onFlow = { f -> add(ed, ItemKind.Flow, f.id) },
            onDismiss = { picker = null })
    }
    ask?.let { which ->
        ConfirmDialog(stringResource(when (which) {
            EditorAsk.Replace -> R.string.confirm_replace_edits
            EditorAsk.Reset -> R.string.confirm_reset
            EditorAsk.OverwriteOwn -> R.string.confirm_overwrite_own
        }), stringResource(if (which == EditorAsk.Reset) R.string.confirm_reset_action else R.string.confirm_replace),
            onConfirm = {
                ask = null
                when (which) {
                    EditorAsk.Replace -> applyPreset()
                    EditorAsk.Reset -> editor?.let { e ->
                        e.cancelPreview(); previewKey = null
                        val base = e.layout.base
                        e.replace(if (base == LayoutBase.None) HomeTemplates.linked(settings) else HomeTemplates.preview(base, settings.favorites, e.layout))
                    }
                    EditorAsk.OverwriteOwn -> { val then = afterSave; commitSave(); then?.invoke() }
                }
                refresh()
            },
            onDismiss = { ask = null; afterSave = null })
    }

    leaveThen?.let { then ->
        LeaveEditorDialog(
            onKeep = { leaveThen = null },
            onDiscard = { close(); then() },
            onSave = { save(then) },
        )
    }
}

/** The element under [p]: the nearest icon within 30 dp, else the clock block. */
private fun hitItem(state: HomeEditorState, p: Pt, clockZone: Zone? = null): String? {
    val near = state.layout.items.filter { (it.kind == ItemKind.App || it.kind == ItemKind.Flow) && it.anchor.type.isPlaced() }
        .mapNotNull { item -> state.pointOf(item)?.let { item.id to hypot(it.x - p.x, it.y - p.y) } }
        .filter { it.second <= 30f }.minByOrNull { it.second }?.first
    if (near != null) return near
    val clock = state.layout.items.firstOrNull { it.kind == ItemKind.Clock } ?: return null
    return clock.id.takeIf { clockZone != null && p in clockZone }
}

@Composable
private fun InkSnackbar(text: String) {
    val ink = LocalInk.current
    Box(Modifier.padding(horizontal = 24.dp).clip(CircleShape).background(ink.night).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(text, style = InkText.small.copy(fontSize = 14.sp), color = ink.onNight)
    }
}

private enum class EditorAsk { Replace, Reset, OverwriteOwn }

private const val OWN_KEY = "own"

@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.intelia.launcher.ui

import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import dev.intelia.launcher.action.Action
import dev.intelia.launcher.action.ActionResult
import dev.intelia.launcher.action.Reason
import dev.intelia.launcher.action.Gesture
import dev.intelia.launcher.action.Trigger
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import dev.intelia.launcher.data.Anchor
import dev.intelia.launcher.data.AnchorType
import dev.intelia.launcher.data.TextLook
import dev.intelia.launcher.data.TypeFace
import dev.intelia.launcher.data.ItemKind
import dev.intelia.launcher.data.DateFormat
import dev.intelia.launcher.data.HomeItem
import dev.intelia.launcher.data.iconLook
import dev.intelia.launcher.data.labelShown
import dev.intelia.launcher.data.labelStyle
import dev.intelia.launcher.data.LayoutSpec
import dev.intelia.launcher.data.HomeLayouts
import dev.intelia.launcher.data.HomeTemplates
import dev.intelia.launcher.data.LayoutBase
import dev.intelia.launcher.data.LensStyle
import dev.intelia.launcher.data.lensDx
import dev.intelia.launcher.data.DockEdge
import dev.intelia.launcher.data.Placement
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import kotlin.math.abs
import kotlin.math.hypot
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import dev.intelia.launcher.data.IconStyle
import dev.intelia.launcher.intelia
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.intelia.launcher.R
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.ItemAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.offset
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.PaletteTarget
import dev.intelia.launcher.data.ShortcutEntry
import dev.intelia.launcher.flow.FlowAlias
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** [depth]: 0 = Home and its layers; a step between deeper pages slides (Veil 3.0 motion), anything else fades. */
private enum class Page(val depth: Int) { Home(0), Search(0), Drawer(0), Flows(2), FlowEdit(3), Studio(1) }

@Composable
fun HomeRoot(model: HomeViewModel, homeSignal: Int) {
    val settings by model.settings.collectAsStateWithLifecycle()
    val guide by model.guide.collectAsStateWithLifecycle()
    InteliaTheme(settings) {
        val draft = guide
        when {
            draft != null -> OnboardingHost(model, draft)
            settings.onboarded -> HomeContent(model, settings, homeSignal)
            else -> GuideStart(model)
        }
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
    val arc by model.arc.collectAsStateWithLifecycle()
    val visible by model.visibleApps.collectAsStateWithLifecycle()
    val icons = LocalContext.current.intelia.icons
    // The drawer's icons are rendered while Home rests, so opening it draws them in its first frames.
    LaunchedEffect(visible, settings.iconStyle) { icons.warm(visible, settings.iconStyle != IconStyle.System) }
    var page by rememberSaveable { mutableStateOf(Page.Home) }
    /** Search came from LENS (the ring opens into the field) or from the drawer's field (already open). */
    var searchFromLens by rememberSaveable { mutableStateOf(true) }
    /** FLOW being edited: an id, or "" for a new command. */
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var editBack by rememberSaveable { mutableStateOf(Page.Flows) }
    /** Příkazy opened from Studio return there (to the same row); from the drawer they return Home. */
    var flowsBack by rememberSaveable { mutableStateOf(Page.Home) }
    /** Keeps a page's scroll and open section while a deeper page is shown; dropped on the way Home. */
    val pages = rememberSaveableStateHolder()
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
    /** Studio → "Upravit plochu" opens the editor on Home; PULSE → "Poloha čočky" opens Studio's calibration. */
    var editOnHome by remember { mutableStateOf(false) }
    var studioStart by remember { mutableStateOf(StudioStart.Main) }
    fun goHome() {
        pages.removeState(Page.Studio.name); pages.removeState(Page.Flows.name)
        page = Page.Home; model.query.value = ""; options = null; editing = null; focus.clearFocus(); keyboard?.hide()
    }
    fun openSearch(fromLens: Boolean) { searchFromLens = fromLens; model.query.value = ""; page = Page.Search }
    fun openDrawer() { model.query.value = ""; page = Page.Drawer }
    fun launch(entry: AppEntry) { if (model.launch(entry)) goHome() else message(R.string.launch_failed) }
    fun launch(shortcut: ShortcutEntry) { if (model.launch(shortcut)) goHome() else message(R.string.launch_failed) }
    fun run(alias: FlowAlias) {
        when (model.runFlow(alias)) {
            FlowRun.Launched -> goHome()
            FlowRun.Unavailable -> message(R.string.flow_unavailable)
            FlowRun.Failed -> message(R.string.launch_failed)
        }
    }
    fun editHome() { studioStart = StudioStart.Main; editOnHome = true; pages.removeState(Page.Studio.name); page = Page.Home }
    val actionHost = rememberActionHost(model, onDrawer = ::openDrawer, onEditHome = ::editHome) { openSearch(fromLens = true) }
    /** The Action Engine entry for every trigger: run, go Home when something opened, otherwise say why. */
    fun perform(action: Action) {
        when (val result = action.run(actionHost)) {
            // Search, the drawer and the editor are Home's own screens: they stay open.
            ActionResult.Opened -> if (action != Action.Search && action != Action.Drawer && action != Action.EditHome) goHome()
            ActionResult.Done -> Unit
            is ActionResult.Unavailable -> message(when (result.reason) {
                Reason.AppMissing -> R.string.action_app_missing
                Reason.FlowMissing -> R.string.action_flow_missing
                Reason.NotDefaultHome -> R.string.action_not_default_home
                Reason.ShortcutMissing -> R.string.action_shortcut_missing
                Reason.NeedsPulseService -> R.string.action_needs_pulse
                Reason.Unsupported -> R.string.action_unsupported
                Reason.Invalid, Reason.CardMissing, Reason.Failed -> R.string.launch_failed
            })
        }
    }
    fun edit(alias: FlowAlias?, from: Page) { editBack = from; editing = alias?.id ?: ""; focus.clearFocus(); keyboard?.hide(); page = Page.FlowEdit }
    val steps: (FlowAlias) -> List<String?> = { alias -> alias.targets.map { model.entry(it)?.label } }

    // PULSE everywhere (M3): while Home is in front the service hands bound gestures here, so FLOW, Search
    // and apps behave exactly as from ARC.
    LifecycleResumeEffect(Unit) {
        PulseAccessibilityService.home = { perform(it) }
        onPauseOrDispose { PulseAccessibilityService.home = null }
    }
    LaunchedEffect(homeSignal) { if (homeSignal > 0) goHome() }
    // The guide's "Prozkoumat Intelii" lands in Studio once Home is back.
    val explore by model.explore.collectAsStateWithLifecycle()
    LaunchedEffect(explore) { if (explore) { model.explore.value = false; studioStart = StudioStart.Main; page = Page.Studio } }
    // M3.3: Search or a card picked on Global ARC in another app; Home runs it once it is in front.
    val request by PulseAccessibilityService.homeRequest.collectAsStateWithLifecycle()
    LaunchedEffect(request) { request?.let { PulseAccessibilityService.consumed(); perform(it) } }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { model.refreshShortcuts() }
    // Search is a paper surface; never leave it behind for Recents or the next return Home.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { if (page == Page.Search || page == Page.Drawer) goHome() }
    // A launcher never finishes on back: back always steps toward Home.
    BackHandler {
        when (page) {
            Page.FlowEdit -> { editing = null; page = editBack }
            Page.Flows -> if (flowsBack == Page.Studio) page = Page.Studio else goHome()
            Page.Home -> Unit
            else -> goHome()
        }
    }

    // Studio sets its own: its top is the wallpaper.
    if (page != Page.Studio) SystemBarIcons(darkIcons = when (page) {
        Page.Home -> homeInk.luminance() < .5f
        Page.Search, Page.Drawer -> !ink.dark // over the depth scrim, which follows the theme
        else -> !ink.dark
    })
    // Search and the drawer are layers over Home: Home stays composed and visible under them.
    val surface = if (page == Page.Search || page == Page.Drawer) Page.Home else page
    // D15 depth: one backdrop under the open surface. The wallpaper behind the window blurs only where the system offers it.
    val depth = remember { VeilDepth() }
    val crossBlur = rememberCrossWindowBlur()
    val backdropBlur = rememberBackdropBlur()
    val focusOpen = page == Page.Search || page == Page.Drawer
    if (crossBlur) WindowBackdropBlur(focusOpen, with(LocalDensity.current) { 26.dp.roundToPx() })
    val overlayVeil = LocalVeil.current.let { if (crossBlur) it.copy(blurred = true) else it }
    CompositionLocalProvider(LocalVeilDepth provides depth) {
    val enter = motionMs(140)
    val exit = motionMs(100)
    val pageMs = motionMs(VeilMotion.PAGE_MS)
    Box(Modifier.fillMaxSize()) {
        WallpaperLayer(dev.intelia.launcher.data.Wallpapers.byId(settings.wallpaper), settings.wallDim)
        AnimatedContent(surface, transitionSpec = {
            val slide = VeilMotion.slide(initialState.depth, targetState.depth)
            if (slide != 0) VeilMotion.page(slide, pageMs) else fadeIn(tween(enter)) togetherWith fadeOut(tween(exit))
        }, label = "page") { target -> pages.SaveableStateProvider(target.name) {
            when (target) {
                Page.Home -> Box(Modifier.fillMaxSize()) {
                // S4.4: under Search or the drawer Home stays drawn but leaves the accessibility tree, so TalkBack's focus
                // order is the overlay's alone; it comes back as the overlay closes.
                Box(Modifier.fillMaxSize().veilBackdrop(depth, backdropBlur)
                    .then(if (focusOpen) Modifier.clearAndSetSemantics { } else Modifier)) { Home(
                    settings = settings,
                    apps = remember(favorites, visible) { (visible + favorites).associateBy { it.key } },
                    arc = arc,
                    flowSteps = steps,
                    onSearch = { openSearch(true) },
                    onDrawer = ::openDrawer,
                    onPulseLens = { studioStart = StudioStart.Pulse; page = Page.Studio },
                    onSettings = { studioStart = StudioStart.Editor; page = Page.Studio },
                    startEditing = editOnHome,
                    onEditingStarted = { editOnHome = false },
                    set = model::updateSettings,
                    onLaunch = ::launch,
                    onOptions = { options = it },
                    onRun = ::run,
                    onAction = ::perform,
                    onMessage = ::message,
                    onSaveLayout = { edited ->
                        model.updateSettings { it.copy(home = it.home.save(edited)) }
                        message(R.string.edit_saved)
                    },
                ) }
                if (page == Page.Search) AccentScope(PaletteTarget.Search, overlayVeil) { SearchOverlay(
                    model = model,
                    settings = settings,
                    fromLens = searchFromLens,
                    resolve = model::entry,
                    onBack = ::goHome,
                    onLaunch = ::launch,
                    onRun = ::run,
                    onOptions = { focus.clearFocus(); keyboard?.hide(); options = it },
                ) }
                // The drawer is composed once Home has rested, then kept, so a swipe up only animates it.
                var drawerReady by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(700); drawerReady = true }
                if (drawerReady || page == Page.Drawer) AccentScope(PaletteTarget.Search, overlayVeil) { DrawerOverlay(
                    model = model,
                    open = page == Page.Drawer,
                    onBack = ::goHome,
                    onSearch = { openSearch(false) },
                    onLaunch = ::launch,
                    onOptions = { options = it },
                    onFlows = { flowsBack = Page.Home; page = Page.Flows },
                    onStudio = { page = Page.Studio },
                ) }
                }
                Page.Search, Page.Drawer -> Unit
                Page.Flows -> FlowList(
                    aliases = settings.aliases,
                    arcFlows = remember(settings) { settings.controls.arcSets.flatMap { it.slots }.filterIsInstance<Action.Flow>().map { it.id } },
                    flowSteps = steps,
                    onBack = { if (flowsBack == Page.Studio) page = Page.Studio else goHome() },
                    onCreate = { edit(null, Page.Flows) },
                    onEdit = { edit(it, Page.Flows) },
                    onRun = ::run,
                )
                Page.FlowEdit -> {
                    val id = editing
                    val initial = settings.aliases.firstOrNull { it.id == id }
                    FlowEditor(
                        initial = initial,
                        inArc = initial != null && settings.controls.inArc(Action.Flow(initial.id)),
                        arcFull = settings.controls.arcFull(),
                        apps = visible,
                        resolve = model::entry,
                        onSave = { alias, inArc ->
                            model.saveAlias(alias, inArc).also { if (it == null) { editing = null; page = editBack } }
                        },
                        onDelete = { deleted -> model.deleteAlias(deleted); editing = null; page = editBack },
                        onBack = { editing = null; page = editBack },
                    )
                }
                Page.Studio -> AccentScope(PaletteTarget.Studio) {
                    Studio(model, settings, onBack = { studioStart = StudioStart.Main; goHome() }, onFlows = { flowsBack = Page.Studio; page = Page.Flows },
                        onEditHome = ::editHome,
                        start = studioStart)
                }
            }
        } }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 132.dp)) { data ->
            InkSnackbar(data.visuals.message)
        }
    }

    options?.let { entry ->
        AppOptionsSheet(
            app = entry,
            inArc = settings.controls.inArc(Action.App(entry.key)),
            arcFull = settings.controls.arcFull(),
            favorite = entry.key in settings.favorites,
            favoritesFull = settings.favorites.size >= LauncherSettings.MAX_FAVORITES,
            onArc = { model.toggleArc(entry); options = null },
            onFavorite = { model.toggleFavorite(entry); options = null },
            onHide = { model.hide(entry); options = null },
            onDetails = { options = null; if (!model.openDetails(entry)) message(R.string.launch_failed) },
            onDismiss = { options = null },
            // Over Search or the drawer the sheet covers rows of text: Focus density keeps them from showing through.
            level = if (page == Page.Search || page == Page.Drawer) VeilLevel.Focus else VeilLevel.Dense,
        )
    }
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
internal fun Home(
    settings: LauncherSettings,
    apps: Map<String, AppEntry>,
    arc: ArcContent,
    flowSteps: (FlowAlias) -> List<String?>,
    onSearch: () -> Unit,
    onDrawer: () -> Unit,
    onPulseLens: () -> Unit,
    onSettings: () -> Unit,
    startEditing: Boolean,
    onEditingStarted: () -> Unit,
    set: SettingsEdit,
    onLaunch: (AppEntry) -> Unit,
    onOptions: (AppEntry) -> Unit,
    onRun: (FlowAlias) -> Unit,
    onAction: (Action) -> Unit,
    onMessage: (Int) -> Unit,
    onSaveLayout: (LayoutSpec) -> Unit,
    /** The guide's stage hides its panel while the Home Editor is open. */
    onEditorChange: (Boolean) -> Unit = {},
    /** The guide's coach: PULSE was tapped (false) or its Global ARC released on a slot (true). */
    onPulseUsed: (Boolean) -> Unit = {},
) {
    val homeInk = LocalHomeInk.current
    val places = LocalPlaces.current
    val wallPlaces = LocalWallPlaces.current
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
    /** The time and the date apart, for hit testing; [clockZone] is the whole block (never covered). */
    var timeZone by remember { mutableStateOf<Zone?>(null) }
    var dateZone by remember { mutableStateOf<Zone?>(null) }
    /** The editor's scaled Home layer, which zones are measured in (not state: read only in callbacks). */
    val homeCoords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    if (startEditing) { onEditingStarted(); openRequested = true }
    /** Editor: the item whose inspector is open, the empty spot of the Add menu, its picker, the preset strip. */
    var selected by remember { mutableStateOf<String?>(null) }
    var addPoint by remember { mutableStateOf<Pt?>(null) }
    /** Where the owner moved and how they sized the editor card; saved with the layout. */
    val inspectorFrame = remember { InspectorFrame(settings.editorCard) }
    var picker by remember { mutableStateOf<ItemKind?>(null) }
    var presetsOpen by remember { mutableStateOf(false) }
    var previewKey by remember { mutableStateOf<String?>(null) }
    var presetApplied by remember { mutableStateOf(false) }
    var ask by remember { mutableStateOf<EditorAsk?>(null) }
    var afterSave by remember { mutableStateOf<(() -> Unit)?>(null) }
    val flows = remember(settings.aliases) { settings.aliases.associateBy { it.id } }
    // ARC speaks over the top of Home, so the clock and favourites step fully aside.
    val quiet by animateFloatAsState(if (arcOpen) 0f else 1f, tween(motionMs(130)), label = "home-quiet")
    val swipe = with(density) { 64.dp.toPx() }
    val editLabel = stringResource(R.string.edit_home)
    val moveLabel = stringResource(R.string.a11y_move)
    val lensLeft = stringResource(R.string.a11y_lens_left)
    val lensRight = stringResource(R.string.a11y_lens_right)
    val lensCentre = stringResource(R.string.a11y_lens_centre)
    val removeLabel = stringResource(R.string.inspect_remove)

    fun refresh() { tick++ }
    fun close() {
        editor = null; leaveThen = null; selected = null; addPoint = null; picker = null
        presetsOpen = false; previewKey = null; presetApplied = false; ask = null; afterSave = null; refresh()
    }
    fun commitSave() {
        val card = inspectorFrame.toCard()
        if (card != settings.editorCard) set { it.copy(editorCard = card) }
        editor?.let { it.cancelPreview(); onSaveLayout(it.layout); it.markSaved() }; close()
    }
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
    val editorOpen = editor != null
    LaunchedEffect(editorOpen) { onEditorChange(editorOpen) }

    // M4: free Home gestures run only when bound; an unbound double tap never delays anything.
    val homeDouble = settings.controls.bound(Trigger.Home(Gesture.DoubleTap))
    val homeDown by rememberUpdatedState(settings.controls.bound(Trigger.Home(Gesture.SwipeDown)))
    val homeLeft by rememberUpdatedState(settings.controls.bound(Trigger.Home(Gesture.SwipeLeft)))
    val homeRight by rememberUpdatedState(settings.controls.bound(Trigger.Home(Gesture.SwipeRight)))
    // Veil 3.0: swipe up and hold carry the drawer and the editor by default, but can be rebound.
    val homeUp by rememberUpdatedState(settings.controls.effective(Trigger.Home(Gesture.SwipeUp)))
    val homeHold = settings.controls.effective(Trigger.Home(Gesture.Hold))
    fun runHome(a: Action) { when (a) { Action.Drawer -> onDrawer(); Action.EditHome -> openRequested = true; else -> onAction(a) } }
    val homeLabels = listOf(stringResource(R.string.gesture_home_double), stringResource(R.string.gesture_home_down),
        stringResource(R.string.gesture_home_left), stringResource(R.string.gesture_home_right), stringResource(R.string.gesture_home_up))
    val homeHoldLabel = stringResource(R.string.gesture_home_hold)
    // A rebound swipe up is offered to TalkBack too; the drawer stays one LENS swipe away.
    val homeActions = listOf(homeDouble, homeDown, homeLeft, homeRight, homeUp?.takeIf { it != Action.Drawer }).mapIndexedNotNull { i, bound ->
        bound?.let { a -> CustomAccessibilityAction(homeLabels[i]) { onAction(a); true } }
    }
    val lensLabels = listOf(stringResource(R.string.gesture_lens_left), stringResource(R.string.gesture_lens_right))
    val lensFlicks = listOf(Gesture.SwipeLeft, Gesture.SwipeRight).mapIndexedNotNull { i, g ->
        settings.controls.bound(Trigger.Lens(g))?.let { a -> CustomAccessibilityAction(lensLabels[i]) { onAction(a); true } }
    }
    // The chain stays the same in edit mode: swapping modifiers mid-gesture would cancel an icon's hold-and-drag.
    BoxWithConstraints(Modifier.fillMaxSize()
        .pointerInput(Unit) {
            var drag = 0f
            detectVerticalDragGestures(onDragStart = { drag = 0f }, onVerticalDrag = { _, amount -> drag += amount },
                onDragEnd = {
                    if (editor != null) return@detectVerticalDragGestures
                    if (drag < -swipe) homeUp?.let(::runHome) else if (drag > swipe) homeDown?.let(onAction)
                })
        }
        .pointerInput(Unit) {
            // M5: sideways swipes do nothing unless bound; Home has no pager, so nothing else listens sideways.
            // A horizontal detector locks onto sideways moves only, so vertical drags stay the drawer and swipe down.
            var dx = 0f
            detectHorizontalDragGestures(onDragStart = { dx = 0f }, onHorizontalDrag = { _, d -> dx += d },
                onDragEnd = {
                    if (editor != null) return@detectHorizontalDragGestures
                    when (homeSideSwipe(dx / density.density, 0f)) {
                        Gesture.SwipeLeft -> homeLeft?.let(onAction)
                        Gesture.SwipeRight -> homeRight?.let(onAction)
                        else -> Unit
                    }
                })
        }.combinedClickable(interactionSource = null, indication = null,
            onLongClickLabel = if (homeHold == Action.EditHome) editLabel else homeHoldLabel, onClick = {},
            onDoubleClick = if (homeDouble != null && editor == null) ({ onAction(homeDouble) }) else null,
            onLongClick = { if (editor == null) homeHold?.let { haptic.performHapticFeedback(HapticFeedbackType.LongPress); runHome(it) } })
        .semantics { if (homeActions.isNotEmpty() && editor == null) customActions = homeActions }) {
        val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
        val statusTop = with(density) { WindowInsets.statusBarsIgnoringVisibility.getTop(this).toDp() }
        val saved = settings.home.activeLayout()
        // The editor's live LENS (drag, undo) wins over the saved layout's.
        val dock = (editor?.layout ?: saved).dock
        val lensSpot = Dock.centre(dock, editor?.lensDx() ?: saved.lensDx(), maxWidth.value, maxHeight.value, statusTop.value, bottomInset.value)
        val compact = maxHeight < 560.dp
        val geometry = HomeGeometry(maxWidth.value, maxHeight.value, statusTop.value, lensSpot.x, lensSpot.y, dock, bottomInset.value,
            rowIcon(editor?.layout ?: saved, settings))
        check(tick >= 0) // read so that editor changes recompose this scope
        fun open(): HomeEditorState = editor ?: HomeEditorState(saved, geometry,
            blocked = { listOfNotNull(clockZone, dateZone.takeIf { editor?.layout?.items?.any { it.kind == ItemKind.Date && it.anchor.type == AnchorType.Free } == true }) })
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
        val textTop = homeTextTop(geometry, statusTop.value, compact).dp
        val freeDate = date?.takeIf { it.anchor.type == AnchorType.Free }
        val iconDefault = settings.iconSize
        // Zones are kept in Home coordinates, inside the editor's scaled layer ([EditView]).
        fun zoneOf(c: androidx.compose.ui.layout.LayoutCoordinates, pad: Float): Zone {
            val r = homeCoords[0]?.takeIf { it.isAttached }?.localBoundingBoxOf(c, clipBounds = false) ?: c.boundsInRoot()
            val d = density.density
            return Zone(r.left / d - pad, r.top / d - pad, r.right / d + pad, r.bottom / d + pad)
        }
        val listLayout = layout.items.any { it.anchor.type == AnchorType.Stack }
        val lensItem = layout.items.firstOrNull { it.kind == ItemKind.Lens }
        val lensPt = Pt(geometry.lensX, geometry.lensY)
        val barBottom = BAR_HEIGHT
        // D15: the editor shows the same Home framed between the bar and the tray; cards are placed from the settled scale, not the animated one.
        val view = EditView(maxWidth.value, maxHeight.value, ed != null)
        val homeScale by animateFloatAsState(view.scale, tween(motionMs(320), easing = VeilMotion.ease), label = "edit-scale")
        val homeLayer = Modifier.graphicsLayer { scaleX = homeScale; scaleY = homeScale; transformOrigin = TransformOrigin(.5f, view.originY) }

        val hole = rememberLens(settings.pulseLens)
        val camera = Pt(hole.x / density.density, hole.y / density.density)
        if (ed != null) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .20f)))
        val pulseService by PulseAccessibilityService.running.collectAsStateWithLifecycle()
        val quick = remember(settings.controls, apps, settings.aliases, pulseService) {
            quickEntries(settings.controls, apps.values.toList(), settings.aliases, pulseService)
        }
        // PULSE stays at the camera, outside the editor's scaled Home.
        if (settings.pulse && ed == null) Pulse(settings, onGesture = { g ->
            if (g == Gesture.Tap) onPulseUsed(false)
            settings.controls.bound(Trigger.Pulse(g))?.let { onAction(it); true } ?: false
        }, arc = arc, onArcAction = { onPulseUsed(true); onAction(it) }, flowSteps = flowSteps, quick = quick)
        if (ed != null) {
            if (settings.pulse) Pulse(settings, preview = true)
            CameraMark(camera, selected == PULSE_ID)
            // Home (and its tap layer) is scaled to 86 % around the middle, so the camera sits above it: PULSE gets
            // its own target here, in screen coordinates.
            val pulseLabel = stringResource(R.string.pulse)
            Box(Modifier.offset(x = (camera.x - CAMERA_HALF_W).dp, y = 0.dp).size((CAMERA_HALF_W * 2).dp, (camera.y + CAMERA_BELOW).dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button,
                    onClickLabel = pulseLabel) {
                    if (presetsOpen) { ed.cancelPreview(); presetsOpen = false; previewKey = null }
                    selected = PULSE_ID.takeIf { selected != PULSE_ID }; addPoint = null
                    refresh()
                })
            // The status bar would take every tap near the camera, so the editor hides it (swipe brings it back)
            // and PULSE everywhere's lens lets taps through: tapping the capsule opens its inspector.
            val rootView = androidx.compose.ui.platform.LocalView.current
            DisposableEffect(rootView) {
                val window = (rootView.context as? android.app.Activity)?.window
                val bars = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, rootView) }
                bars?.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                bars?.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                PulseAccessibilityService.setEditing(true)
                onDispose {
                    bars?.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                    PulseAccessibilityService.setEditing(false)
                }
            }
        }

        Box(Modifier.fillMaxSize().then(homeLayer).onPlaced { homeCoords[0] = it }) {
        if (ed != null) EditorMarks(ed, layout, iconDefault)

        if (clock == null) timeZone = null
        if (date == null) dateZone = null
        // Moved over the clock's plate, an unpinned date reads in the plate's ink instead of the wallpaper's.
        val dateOnPlate = freeDate != null && !clock?.background.isNullOrEmpty() && clockZone?.let { dateZone?.centredIn(it, 8f) } == true
        if (clock == null && (date == null || freeDate != null)) clockZone = null
        HomeText(layout, geometry, textTop.value, settings, now, homeInk, places, wallPlaces, compact, { quiet }, dateOnPlate,
            onClock = { c -> clockZone = zoneOf(c, 8f) }, onTime = { c -> timeZone = zoneOf(c, 4f) }, onDate = { c -> dateZone = zoneOf(c, 6f) })

        // Icons placed by geometry: LENS row, grid cells, free positions; apps and FLOWs.
        // S2: names laid out once for Home and Studio's miniature ([homeLabels]); a side row has no room for them.
        val names = homeLabels(layout, geometry, settings, density)
        val iconAccent = wallPlaces[PaletteTarget.Icons]
        layout.items.forEach { item ->
            if ((item.kind != ItemKind.App && item.kind != ItemKind.Flow) || !item.anchor.type.isPlaced()) return@forEach
            val app = if (item.kind == ItemKind.App) apps[item.target] else null
            val flow = if (item.kind == ItemKind.Flow) flows[item.target] else null
            if (app == null && flow == null) return@forEach
            val name = app?.label ?: flow?.name.orEmpty()
            // The node stays put while it is dragged (its gesture needs a still frame); a ghost follows the finger.
            val dragging = drag?.id == item.id
            val p = geometry.position(item, layout) ?: return@forEach
            val look = item.iconLook(settings)
            val iconDp = look.sizeDp.dp
            key(item.id) {
                names[item.id]?.let { box -> HomeLabel(name, box, item.labelStyle(settings.labels), homeInk, iconAccent,
                    Modifier.graphicsLayer { alpha = if (dragging) 0f else quiet }.clearAndSetSemantics {}) }
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
                    // No backing (4.3): system or icon-pack icons as they are; monochrome tinted fg. Own style, colour, opacity (S2).
                    HomeIconFace(app, look, homeInk, iconAccent)
                }
            }
        }

        drag?.let { d -> ed.item(d.id)?.takeIf { it.kind == ItemKind.App || it.kind == ItemKind.Flow }?.let { item ->
            val app = apps[item.target]
            if (app == null && flows[item.target] == null) return@let
            Box(Modifier.centerAt(d.point.x.dp, d.point.y.dp).size(maxOf(CrystalDims.touch, item.iconDp(iconDefault).dp + 8.dp))
                .graphicsLayer { scaleX = 1.08f; scaleY = 1.08f }, contentAlignment = Alignment.Center) {
                if (app != null) AppIcon(app, Modifier.size(item.iconDp(iconDefault).dp), tint = homeInk)
                else HomeGlyph(Glyph.Flow, null, tint = homeInk, size = item.iconDp(iconDefault).dp * .7f)
            }
        } }

        // The legacy text list (preset "Vlastní").
        val stack = layout.items.filter { it.kind == ItemKind.App && it.anchor.type == AnchorType.Stack }.sortedBy { it.anchor.row }
        // The list takes the clock's own face (Terminál: monospaced) at a smaller size; the legacy list keeps its serif.
        val listFace = clock?.look?.face?.takeIf { it != TypeFace.Style }?.let { faceFamily(it, 400) }
        val listStyle = if (listFace == null) InkText.item.copy(fontSize = 30.sp, lineHeight = 34.sp)
            else InkText.item.copy(fontFamily = listFace, fontSize = 22.sp, lineHeight = 28.sp)
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
                    style = listStyle.copy(shadow = homeShadow(homeInk)),
                    color = homeInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // S4.4: LENS gives way to the icons around it, so each keeps a whole 48 dp target at any display size.
        val lensTouch = remember(layout, geometry, settings, apps, flows) {
            val targets = layout.items.mapNotNull { item ->
                if ((item.kind != ItemKind.App && item.kind != ItemKind.Flow) || !item.anchor.type.isPlaced()) return@mapNotNull null
                if (apps[item.target] == null && flows[item.target] == null) return@mapNotNull null
                val p = geometry.position(item, layout) ?: return@mapNotNull null
                p to maxOf(CrystalDims.touch.value, item.iconLook(settings).sizeDp + 8f)
            }
            Dock.lensTouch(lensSpot, dock.edge.vertical, targets)
        }
        AccentScope(PaletteTarget.Lens) { LensArc(arc, lensSpot, dock.edge, flowSteps, onSearch = onSearch, onDrawer = onDrawer, touch = lensTouch,
            onAction = onAction, onOpenChange = { arcOpen = it }, lensRing = lensItem?.lensDp()?.dp ?: CrystalDims.lensRing,
            lensFilled = lensItem?.style == LensStyle.Filled.name,
            // TalkBack moves LENS in the editor without a drag (§5.5), each as one undo step.
            // Outside the editor TalkBack gets the bound LENS flicks instead (M4).
            editActions = if (ed == null) lensFlicks else listOf(
                CustomAccessibilityAction(lensLeft) { ed.setLensDx(ed.layout.lensDx() - LENS_A11Y_STEP).also { refresh() } },
                CustomAccessibilityAction(lensRight) { ed.setLensDx(ed.layout.lensDx() + LENS_A11Y_STEP).also { refresh() } },
                CustomAccessibilityAction(lensCentre) { ed.setLensDx(0f).also { refresh() } },
            ),
            onEditSelect = if (ed == null) null else { { selected = ed.lensId(); refresh() } },
            onFlick = { g -> settings.controls.bound(Trigger.Lens(g))?.let { onAction(it); true } ?: false }) }
        if (ed != null) {
            // Editor layer above LENS: a hold on LENS never opens ARC here; drags move elements.
            Box(Modifier.fillMaxSize()
                .pointerInput(ed) {
                    detectTapGestures(onTap = { o ->
                        val p = Pt(o.x / density.density, o.y / density.density)
                        val onLens = hypot(p.x - ed.lensAt().x, p.y - ed.lensAt().y) <= 34f
                        // The whole capsule around the camera, not just the lens: PULSE is edited by tapping it (§5.5).
                        val onCamera = view.toScreen(p).let { q -> abs(q.x - camera.x) <= 96f && q.y <= camera.y + 40f }
                        val hit = hitItem(ed, p, timeZone, dateZone) ?: ed.lensId()?.takeIf { onLens } ?: PULSE_ID.takeIf { onCamera }
                        when {
                            hit != null -> {
                                // A preset only previewed is dropped first, so edits never land on a layout ✓ would discard.
                                if (presetsOpen) { ed.cancelPreview(); presetsOpen = false; previewKey = null }
                                selected = hit.takeIf { it != selected }; addPoint = null
                            }
                            selected != null || addPoint != null -> { selected = null; addPoint = null }
                            presetsOpen -> { ed.cancelPreview(); presetsOpen = false; previewKey = null }
                            view.toScreen(p).y > barBottom && hypot(p.x - ed.lensAt().x, p.y - ed.lensAt().y) > 50f -> addPoint = p
                        }
                        refresh()
                    })
                }
                .pointerInput(ed) {
                    // Pinch (§5.5 extension): two fingers resize the element under them, or the selected one.
                    pinchToResize(
                        target = { centre ->
                            val p = Pt(centre.x / density.density, centre.y / density.density)
                            (hitItem(ed, p, timeZone, dateZone, reach = 56f) ?: ed.lensId()?.takeIf { hypot(p.x - ed.lensAt().x, p.y - ed.lensAt().y) <= 60f }
                                ?: selected)?.let(ed::item)?.takeIf { it.kind != ItemKind.Widget }
                        },
                        sizeOf = { it.currentSize(iconDefault) },
                        onStart = { ed.cancelDrag(); refresh() },
                        onSize = { item, size ->
                            ed.edit(item.id, "size") { it.copy(sizeDp = size.coerceIn(item.sizeRange())) }
                            selected = item.id; refresh()
                        },
                    )
                }
                .pointerInput(ed) {
                    var moved = Offset.Zero
                    var first = true
                    detectDragGestures(
                        onDragStart = { o ->
                            moved = Offset.Zero
                            first = true
                            val p = Pt(o.x / density.density, o.y / density.density)
                            val hit = hitItem(ed, p, timeZone, dateZone)
                            // LENS moves sideways within its band (§5.5); its own hold never opens ARC here.
                            if (hit == null && hypot(p.x - ed.lensAt().x, p.y - ed.lensAt().y) <= 42f) { ed.startLensDrag(); selected = ed.lensId() }
                            else hit?.let { ed.startDrag(it) }
                            refresh()
                        },
                        onDrag = { change, amount ->
                            // The first step arrives without the touch slop; add it back so LENS and icons stay under the finger.
                            val step = if (first) HomeEditorState.withSlop(Pt(amount.x, amount.y), viewConfiguration.touchSlop)
                                .let { Offset(it.x, it.y) } else amount
                            first = false
                            if (ed.lensDrag != null) {
                                change.consume()
                                moved += step
                                if (ed.dragLensBy((if (dock.edge.vertical) moved.y else moved.x) / density.density)) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                refresh()
                            } else if (ed.drag != null) {
                                change.consume()
                                moved += step
                                ed.dragBy(Pt(moved.x / density.density, moved.y / density.density))
                                refresh()
                            }
                        },
                        onDragEnd = {
                            if (ed.lensDrag != null) ed.dropLens()
                            else if (ed.drop()) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            refresh()
                        },
                        onDragCancel = { ed.cancelDrag(); ed.cancelLensDrag(); refresh() },
                    )
                })
        }
        }
        if (ed != null) {
            // What is edited now, named in the bar (and in the card's header).
            val editingTitle = when (val id = selected) {
                null -> null
                PULSE_ID -> stringResource(R.string.pulse)
                else -> ed.item(id)?.let { item ->
                    when (item.kind) {
                        ItemKind.Clock -> stringResource(R.string.place_clock)
                        ItemKind.Date -> stringResource(R.string.inspect_date)
                        ItemKind.Lens -> stringResource(R.string.place_lens)
                        ItemKind.App -> apps[item.target]?.label
                        ItemKind.Flow -> flows[item.target]?.name
                        else -> null
                    }
                }
            }
            EditorFrame(view.toScreen(Zone(0f, 0f, maxWidth.value, maxHeight.value)))
            EditorBar(ed.canUndo, ed.canRedo, editing = editingTitle, onCancel = { leave {} }, onUndo = { ed.undo(); refresh() },
                onRedo = { ed.redo(); refresh() }, onDone = { save() }, modifier = Modifier.align(Alignment.TopCenter))
            // The tray steps aside while a card is open: cards may reach down to the navigation bar.
            val cardOpen = presetsOpen || addPoint != null || (selected != null && drag == null && ed.lensDrag == null)
            androidx.compose.animation.AnimatedVisibility(!cardOpen && drag == null && ed.lensDrag == null, Modifier.align(Alignment.BottomCenter),
                enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut()) {
                EditorTray(placementLabel(ed.layout), presetsOpen,
                    onAdd = { selected = null; addPoint = Pt(geometry.lensX, geometry.areaTop + geometry.areaH / 2f); refresh() },
                    onPresets = {
                        if (presetsOpen) { ed.cancelPreview(); previewKey = null }
                        presetsOpen = !presetsOpen; selected = null; addPoint = null; refresh()
                    },
                    onPlacement = { ed.setPlacement(Placement.entries[(ed.layout.placement.ordinal + 1) % Placement.entries.size]); refresh() },
                    // Every setting lives in Settings only: unsaved edits are asked about first, as for PULSE calibration.
                    onSettings = { leave(onSettings) })
            }

            /** The LENS row as a target: the Presets card opens just above it. */
            val lensRowZone = view.toScreen(geometry.rowZone)
            if (presetsOpen) {
                val start = ed.previewOf ?: ed.layout
                val favs = settings.favorites
                val choices = HomeTemplates.PRESETS.map { b ->
                    PresetChoice(b.name, stringResource(presetName(b)), HomeTemplates.preview(b, favs, start))
                } + listOfNotNull(settings.home.layouts.firstOrNull { it.id == HomeLayouts.USER_ID }
                    ?.let { PresetChoice(OWN_KEY, stringResource(R.string.preset_own), it) })
                // The same editor card as an element's: placed above the LENS row, so the clock and the top of the
                // previewed layout stay visible, or where the owner moved it.
                key("presets") { InspectorCard(lensRowZone, maxWidth.value, barBottom + EDIT_LABEL, null,
                    stringResource(R.string.presets), inspectorFrame) {
                    PresetStrip(choices, previewKey, geometry,
                        onPick = { ed.preview(it.spec); previewKey = it.key; refresh() },
                        onApply = { if (ed.canUndo) ask = EditorAsk.Replace else { applyPreset(); refresh() } },
                        onCancel = { ed.cancelPreview(); presetsOpen = false; previewKey = null; refresh() },
                        onReset = { ask = EditorAsk.Reset })
                } }
            }

            // PULSE has one home, Studio › PULSE: a tap on the camera in the editor goes there (unsaved edits are asked about first).
            if (selected == PULSE_ID && drag == null) LaunchedEffect(Unit) { selected = null; leave(onPulseLens) }
            val sel = selected?.let(ed::item)
            val bow = remember(arc.look, arc.sets.size, lensPt, dock.edge, maxWidth, maxHeight) {
                Dock.bow(arc.look, arc.sets.size.coerceAtLeast(1), dock.edge, lensPt, maxWidth.value, maxHeight.value)
            }
            if (sel?.kind == ItemKind.Lens || ed.lensDrag != null) GhostArc(lensPt, bow, Modifier.fillMaxSize().then(homeLayer), dock.edge)
            if (sel != null && drag == null && ed.lensDrag == null) {
                val zone = when (sel.kind) {
                    ItemKind.Clock -> timeZone ?: clockZone
                    ItemKind.Date -> dateZone
                    // The card keeps clear of the ghost ARC: its zone reaches up to the outermost ring.
                    ItemKind.Lens -> (sel.lensDp() / 2f + 6f).let { r ->
                        val tip = Dock.toScreen(dock.edge, 0f, -maxOf(r, bow.reach + 8f))
                        Zone(lensPt.x + minOf(-r, tip.x), lensPt.y + minOf(-r, tip.y), lensPt.x + maxOf(r, tip.x), lensPt.y + maxOf(r, tip.y))
                    }
                    else -> ed.pointOf(sel)?.let { c ->
                        val r = sel.iconDp(iconDefault) / 2f + 6f
                        Zone(c.x - r, c.y - r, c.x + r, c.y + r + if (sel.labelShown(settings.labels)) 20f else 0f)
                    }
                }
                
                // A faint size dot at the lower end corner of the clock, or just off an icon or LENS, so their own drag (move) stays free.
                val corner = when (sel.kind) {
                    ItemKind.Clock -> zone?.let { Pt(it.right - 6f, it.bottom - 6f) }
                    ItemKind.Date -> null
                    ItemKind.Lens -> (sel.lensDp() / 2f).let { r -> Dock.toScreen(dock.edge, r + 16f, r * .6f).let { Pt(lensPt.x + it.x, lensPt.y + it.y) } }
                    else -> ed.pointOf(sel)?.let { p -> (sel.iconDp(iconDefault) / 2f).let { r -> Pt(p.x + r + 10f, p.y + r) } }
                }
                // Handles live in Home coordinates, in a layer scaled like Home.
                if (corner != null) Box(Modifier.fillMaxSize().then(homeLayer)) { SizeHandle(corner.x, corner.y, stringResource(when (sel.kind) {
                    ItemKind.Clock -> R.string.clock_resize
                    ItemKind.Lens -> R.string.lens_resize
                    else -> R.string.icon_resize
                })) { dx ->
                    ed.edit(sel.id, "size") { it.copy(sizeDp = (it.currentSize(iconDefault) + dx.roundToInt()).coerceIn(it.sizeRange())) }
                    refresh()
                } }
                // A plate stretches by its own handles: right edge sideways, bottom edge down.
                val plateZone = when {
                    sel.background.isEmpty() -> null
                    sel.kind == ItemKind.Clock -> clockZone
                    sel.kind == ItemKind.Date && sel.anchor.type != AnchorType.Bound -> dateZone
                    else -> null
                }
                if (plateZone != null) Box(Modifier.fillMaxSize().then(homeLayer)) { PlateHandles(plateZone) { dx, dy ->
                    ed.edit(sel.id, "pad") { it.copy(look = it.look.copy(
                        padX = (it.look.padX + dx / 2f).coerceIn(0f, TextLook.MAX_PAD), padY = (it.look.padY + dy / 2f).coerceIn(0f, TextLook.MAX_PAD))) }
                    refresh()
                } }
                if (zone != null) key(sel.id) { InspectorCard(view.toScreen(zone), maxWidth.value, barBottom + EDIT_LABEL,
                    // Above the ghost ARC the LENS card may cover the clock: the arcs matter more while LENS is edited.
                    clockZone?.takeIf { !sel.kind.isText() && sel.kind != ItemKind.Lens }?.let(view::toScreen), editingTitle, inspectorFrame) {
                    when (sel.kind) {
                        ItemKind.Clock -> ClockInspector(ed, sel, settings.clockStyle, onRemoved = { selected = null }, onChange = ::refresh)
                        ItemKind.Date -> DateInspector(ed, sel, zone.let { z ->
                            ((z.top + 6f - statusTop.value - textTop.value) / geometry.areaH).takeIf { it.isFinite() } }, onRemoved = { selected = null }, onChange = ::refresh)
                        ItemKind.Lens -> LensInspector(ed, sel, dock, onChange = ::refresh)
                        else -> {
                            val app = apps[sel.target]
                            ItemInspector(ed, sel, settings,
                                onMore = app?.let { a -> { onOptions(a) } }, onRemoved = { selected = null }, onChange = ::refresh)
                        }
                    }
                } }
            }
            addPoint?.let { at ->
                fun addText(kind: ItemKind) { ed.addText(kind, at); addPoint = null; refresh() }
                AddMenu(view.toScreen(at), maxWidth.value, barBottom, clockZone?.let(view::toScreen), onApp = { picker = ItemKind.App }, onFlow = { picker = ItemKind.Flow },
                    onClock = { addText(ItemKind.Clock) }.takeIf { clock == null }, onDate = { addText(ItemKind.Date) }.takeIf { date == null })
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

/** The element under [p]: the nearest icon within [reach] dp, else the date, else the time. */
private fun hitItem(state: HomeEditorState, p: Pt, timeZone: Zone?, dateZone: Zone?, reach: Float = 30f): String? {
    val near = state.layout.items.filter { (it.kind == ItemKind.App || it.kind == ItemKind.Flow) && it.anchor.type.isPlaced() }
        .mapNotNull { item -> state.pointOf(item)?.let { item.id to hypot(it.x - p.x, it.y - p.y) } }
        .filter { it.second <= reach }.minByOrNull { it.second }?.first
    if (near != null) return near
    state.layout.items.firstOrNull { it.kind == ItemKind.Date }?.let { if (dateZone != null && p in dateZone) return it.id }
    val clock = state.layout.items.firstOrNull { it.kind == ItemKind.Clock } ?: return null
    return clock.id.takeIf { timeZone != null && p in timeZone }
}

/** Id of the PULSE pseudo-element in the editor (it is a setting, not a layout item). */
private const val PULSE_ID = "#pulse"
private const val CAMERA_HALF_W = 96f
private const val CAMERA_BELOW = 24f
/** How far one TalkBack "move LENS" action shifts it (dp). */
private const val LENS_A11Y_STEP = 24f

/** Room under the editor bar for the name of what is edited, dp. */
private const val EDIT_LABEL = 8f
/** Opacity of the date on a plate: full, as the dark Veil over a white wallpaper has no margin below it (VeilTest). */
internal const val PLATE_TEXT_ALPHA = 1f

/** The date line in its own format, size, colour and opacity; 4.2 default: 12 sp caps, +2.2 tracking, fg2 (62 %). */
@Composable
internal fun DateText(item: HomeItem, now: LocalDateTime, homeInk: Color, accent: Color, modifier: Modifier, onPlate: Boolean = false) {
    val size = item.dateSp()
    val look = item.look
    val text = dateText(now, item.dateFormat(), look)
    val ownPlate = item.anchor.type != AnchorType.Bound && item.background.isNotEmpty()
    // On its own plate or on the clock's: the plate's ink, no wallpaper shadow, the plate's text opacity.
    val plate = ownPlate || onPlate
    val ink = if (ownPlate) LocalCrystal.current.fg else homeInk
    val family = faceFamily(look.face, if (item.weight == 0) 450 else item.weight) ?: CrystalType.date.fontFamily
    // One line never wraps: a wide face (Mono, Unbounded) would push the last word onto a hidden second line.
    // Too wide for the room, the line steps down in size until it fits whole.
    var fit by remember(text, size, look, family) { androidx.compose.runtime.mutableFloatStateOf(1f) }
    val s = size * fit
    val style = CrystalType.date.copy(fontFamily = family, fontSize = s.sp,
        letterSpacing = (2.2f * s / ItemSizes.DATE + look.tracking / 100f * s).sp, shadow = if (plate) null else homeShadow(homeInk),
        // Vertical: one letter per line, centred, a little air between lines.
        lineHeight = if (look.vertical) (size * 1.2f).sp else TextUnit.Unspecified,
        textAlign = if (look.vertical) TextAlign.Center else TextAlign.Unspecified)
    Text(if (look.vertical) text.toList().joinToString("\n") else text,
        modifier.graphicsLayer { alpha = item.alpha }
            .then(if (ownPlate) Modifier.veilSurface(plateTokens(item.background), VeilOrigin.Top, RoundedCornerShape(18.dp))
                .padding(horizontal = (16f + look.padX).dp, vertical = (10f + look.padY).dp) else Modifier),
        style = style, color = item.tintColor(accent) ?: ink.copy(alpha = if (plate) PLATE_TEXT_ALPHA else .62f), maxLines = if (look.vertical) 40 else 1,
        softWrap = look.vertical, onTextLayout = { if (!look.vertical && it.didOverflowWidth && fit > .5f) fit *= .92f })
}

/** A faint dashed ring around the camera in the editor: tap it to edit PULSE. */
@Composable
private fun CameraMark(at: Pt, selected: Boolean) {
    val c = LocalCrystal.current
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        drawCircle(if (selected) c.acc else c.fg2, 22.dp.toPx(), Offset(at.x * density, at.y * density),
            style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))))
    }
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

/** The name of a built-in preset in the editor's strip. */
internal fun presetName(base: LayoutBase) = when (base) {
    LayoutBase.Pure, LayoutBase.None -> R.string.layout_pure
    LayoutBase.Essentials -> R.string.layout_essentials
    LayoutBase.Balanced -> R.string.preset_balanced
    LayoutBase.Editorial -> R.string.preset_editorial
    LayoutBase.Terminal -> R.string.preset_terminal
    LayoutBase.Poster -> R.string.preset_poster
    LayoutBase.Plate -> R.string.preset_plate
    LayoutBase.Side -> R.string.preset_side
}

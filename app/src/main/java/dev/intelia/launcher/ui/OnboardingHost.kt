package dev.intelia.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.intelia.launcher.R
import dev.intelia.launcher.action.Action
import dev.intelia.launcher.action.Gesture
import dev.intelia.launcher.action.Trigger
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.ItemKind
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.PaletteTarget
import dev.intelia.launcher.data.TypeFace
import dev.intelia.launcher.data.Wallpapers
import dev.intelia.launcher.flow.FlowAlias
import dev.intelia.launcher.onboarding.ControlTab
import dev.intelia.launcher.onboarding.GuideStep
import dev.intelia.launcher.onboarding.HomeChoice
import dev.intelia.launcher.onboarding.OnboardingDraft
import dev.intelia.launcher.onboarding.PulsePick
import dev.intelia.launcher.onboarding.Tried
import dev.intelia.launcher.pulse.MediaWatch
import dev.intelia.launcher.search.AppSearch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** A first run before the draft exists: the draft waits (briefly) for the app list, so the first Home has its apps. */
@Composable
internal fun GuideStart(model: HomeViewModel) {
    LaunchedEffect(Unit) {
        withTimeoutOrNull(2_500) { model.visibleApps.first { it.isNotEmpty() } }
        model.beginGuide(rerun = false)
    }
    Box(Modifier.fillMaxSize())
}

private enum class GuideOverlay { Search, Drawer }
private enum class LookTab { Theme, Color, Clock, Icons, Material }

/**
 * Onboarding V2 on the live Home (ONBOARDING_V2 §3, owner revision): the real [Home] drawn from the draft, in a
 * sandbox where nothing launches, runs or changes the system, and one compact panel per step. Steps that choose a
 * look show the whole Home above the panel (the Home Editor's zoom-out); the Controls step gives the full screen to the
 * Home, where LENS, ARC, PULSE and FLOW are tried for real. Done zooms the Home back to full size and commits the draft.
 */
@Composable
internal fun OnboardingHost(model: HomeViewModel, draft: OnboardingDraft) {
    val context = LocalContext.current
    val density = LocalDensity.current
    fun update(transform: (OnboardingDraft) -> OnboardingDraft) = model.updateGuide(transform)
    var compare by rememberSaveable { mutableStateOf(false) }
    var hidden by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var editRequest by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf<GuideOverlay?>(null) }
    var drawerReady by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    var leaving by remember { mutableStateOf<(() -> Unit)?>(null) }

    val visible by model.visibleApps.collectAsStateWithLifecycle()
    val shortcuts by model.shortcuts.collectAsStateWithLifecycle()
    // "Porovnat" shows the start only while held; the stored settings and the draft never change by it.
    val base = if (compare) draft.start else draft.settings
    // Trying PULSE needs a Global ARC: without one bound, a marked demo on hold, only on the stage of this step.
    val shown = remember(base, draft.step) { if (draft.step == GuideStep.Controls) base.withPulseArcDemo() else base }
    val pulseArcDemo = shown !== base
    // FLOW is shown with the user's own first one; without any, a clearly marked demo made of apps on this phone.
    val demo = remember(visible) { demoFlow(context.getString(R.string.guide_flow_demo_name), visible) }
    val flow = draft.settings.aliases.firstOrNull() ?: demo
    val searchSettings = remember(shown, flow) { if (shown.aliases.any { it.id == flow?.id } || flow == null) shown else shown.copy(aliases = shown.aliases + flow) }
    val ownArc = remember(shown.controls, visible, shown.aliases, shortcuts) { ArcContent.of(shown.controls, visible, shown.aliases, shortcuts) }
    // An empty ARC (a new install) is shown with the Home's own apps as a marked demo; it is never stored.
    val arcDemo = ownArc.sets.all { set -> set.slots.all { it == null } }
    val arc = remember(ownArc, arcDemo, shown.favorites, visible) {
        if (!arcDemo) ownArc else {
            val keys = (shown.favorites + model.essentialKeys() + visible.map { it.key }).distinct().take(LauncherSettings.ARC_SLOTS)
            val controls = keys.fold(shown.controls) { c, k -> c.toggleInArc(Action.App(k), true) }
            ArcContent.of(controls, visible, shown.aliases, shortcuts)
        }
    }
    val apps = remember(visible) { visible.associateBy { it.key } }
    val steps: (FlowAlias) -> List<String?> = { alias -> alias.targets.map { model.entry(it)?.label } }

    LaunchedEffect(note) { if (note != null) { delay(2_600); note = null } }
    LaunchedEffect(draft.step) { compare = false; hidden = false }
    val leaveMs = motionMs(320)
    LaunchedEffect(leaving) { leaving?.let { delay(leaveMs.toLong()); it() } }
    fun leave(then: () -> Unit) { overlay = null; hidden = false; leaving = then }
    fun closeOverlay() { overlay = null; model.query.value = "" }
    fun would(name: String?) { note = context.getString(R.string.guide_would_open, name ?: context.getString(R.string.act_unknown)) }
    fun ran(alias: FlowAlias) { note = context.getString(R.string.guide_would_run, alias.name) }
    /** Every action from the sandboxed Home: Search and the drawer open as the real layers, the rest is only named. */
    fun act(action: Action) {
        when (action) {
            Action.Search -> { update { it.tried(Tried.Tap) }; overlay = GuideOverlay.Search }
            Action.Drawer -> { update { it.tried(Tried.Swipe) }; drawerReady = true; overlay = GuideOverlay.Drawer }
            is Action.App -> would(model.entry(action.component)?.label)
            is Action.Flow -> searchSettings.aliases.firstOrNull { it.id == action.id }?.let(::ran) ?: would(null)
            else -> would(context.getString(systemActionLabelRes(action)))
        }
    }
    fun arcAct(action: Action) { update { it.tried(Tried.Hold).tried(Tried.ArcPick) }; act(action) }

    // One stable sandbox for the stage (a static local), always calling the current [act].
    val currentAct = rememberUpdatedState<(Action) -> Unit>(::act)
    val sandbox = remember { { a: Action -> currentAct.value(a) } }
    var skipMenu by remember { mutableStateOf(false) }
    LaunchedEffect(draft.step) { skipMenu = false }
    BackHandler(enabled = !editing && leaving == null) {
        when {
            overlay != null -> closeOverlay()
            skipMenu -> skipMenu = false
            hidden -> hidden = false
            !draft.first -> update { it.back() }
        }
    }

    val full = leaving != null || hidden || editing || draft.step == GuideStep.Welcome || draft.step == GuideStep.Controls
    val interactive = leaving == null && (hidden || editing || draft.step == GuideStep.Controls)
    val panelOnTop = draft.step == GuideStep.Controls && draft.tab != ControlTab.Pulse
    val statusTop = with(density) { WindowInsets.statusBarsIgnoringVisibility.getTop(this).toDp() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenH = maxHeight
        var panelH by remember { mutableStateOf(0.dp) }
        var wallInk by remember { mutableStateOf(Color.White) }
        val stageTop = statusTop + 64.dp
        // The soft keyboard (app search in Plocha) takes room from the panel, never hides the field.
        val ime = with(density) { WindowInsets.ime.getBottom(this).toDp() }
        val room = screenH - stageTop - panelH - 20.dp
        val mini = (room / screenH).coerceIn(.2f, .8f)
        val scale by animateFloatAsState(if (full) 1f else mini, tween(motionMs(320), easing = VeilMotion.ease), label = "guide-scale")
        val shift by animateDpAsState(if (full) 0.dp else stageTop, tween(motionMs(320), easing = VeilMotion.ease), label = "guide-shift")
        val scrim by animateFloatAsState(if (full) 0f else .32f, tween(motionMs(320)), label = "guide-scrim")
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrim)))

        // The stage: the real Home of the shown settings, laid out at the screen's size and scaled as a layer.
        InteliaTheme(shown) {
            val homeInk = LocalHomeInk.current
            wallInk = homeInk
            // The status bar always sits on the wallpaper (or the scrim over it), never on the panel.
            SystemBarIcons(darkIcons = full && homeInk.luminance() < .5f)
            val corner = (28f / scale.coerceAtLeast(.1f)).dp
            val stageLabel = stringResource(R.string.guide_stage)
            Box(Modifier.fillMaxSize()
                .graphicsLayer {
                    scaleX = scale; scaleY = scale; transformOrigin = TransformOrigin(.5f, 0f); translationY = shift.toPx()
                    clip = scale < .999f; shape = RoundedCornerShape(corner)
                }
                .then(if (scale < .999f) Modifier.border((1.5f / scale).dp, homeInk.copy(alpha = .35f), RoundedCornerShape(corner)) else Modifier)
                .then(if (interactive) Modifier else Modifier.blockTouches().clearAndSetSemantics { contentDescription = stageLabel })) {
                WallpaperLayer(Wallpapers.byId(shown.wallpaper), shown.wallDim)
                // Torch, media keys and players are only named here; the production host is never reached.
                CompositionLocalProvider(LocalSandbox provides sandbox) { Home(
                    settings = shown,
                    apps = apps,
                    arc = arc,
                    flowSteps = steps,
                    onSearch = { act(Action.Search) },
                    onDrawer = { act(Action.Drawer) },
                    onPulseLens = {},
                    onSettings = {},
                    startEditing = editRequest,
                    onEditingStarted = { editRequest = false },
                    set = { t -> update { it.edit(t) } },
                    onLaunch = { would(it.label) },
                    onOptions = {},
                    onRun = ::ran,
                    onAction = ::arcAct,
                    onMessage = {},
                    onSaveLayout = { edited -> update { it.withEditedLayout(edited) } },
                    onEditorChange = { editing = it },
                    onPulseUsed = { arcUsed -> update { it.tried(if (arcUsed) Tried.PulseArc else Tried.PulseTap) } },
                ) }
                if (overlay == GuideOverlay.Search) AccentScope(PaletteTarget.Search) {
                    SearchOverlay(model, searchSettings, fromLens = true, resolve = model::entry, onBack = ::closeOverlay,
                        onLaunch = { closeOverlay(); would(it.label) }, onRun = { closeOverlay(); ran(it) }, onOptions = {})
                }
                if (drawerReady) AccentScope(PaletteTarget.Search) {
                    DrawerOverlay(model, open = overlay == GuideOverlay.Drawer, onBack = ::closeOverlay,
                        onSearch = { overlay = GuideOverlay.Search }, onLaunch = { closeOverlay(); would(it.label) }, onOptions = {},
                        onFlows = {}, onStudio = {})
                }
            }
        }

        // The guide's own chrome follows the draft, so a theme or colour picked in Look shows here too.
        InteliaTheme(draft.settings) {
            val ink = LocalInk.current
            val chrome = leaving == null && !editing && overlay == null
            // Porovnat: small, on the preview's corner; held (or toggled by TalkBack) it shows the start.
            if (chrome && !full && draft.settings != draft.start && draft.step in setOf(GuideStep.Home, GuideStep.Look, GuideStep.Done))
                ComparePill(compare, { compare = it }, Modifier.align(Alignment.TopEnd)
                    .padding(top = stageTop + 10.dp, end = maxWidth * (1f - scale) / 2 + 10.dp))
            if (chrome && !hidden && draft.step != GuideStep.Welcome) TopBar(draft, onBack = { update { it.back() } },
                skipMenu = skipMenu, onSkipMenu = { skipMenu = it }, onSkip = { update { it.skip() } }, onSkipAll = { update { it.skipAll() } },
                Modifier.align(Alignment.TopCenter), if (full) wallInk else Color.White)
            if (chrome && hidden) Capsule(stringResource(R.string.guide_show_panel), { hidden = false },
                Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 52.dp), filled = false)
            if (interactive && hidden && overlay == null && !editing) SampleBadge(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 60.dp, end = 16.dp))
            note?.let { text ->
                // Away from the panel: under it when it sits on top, under the top bar otherwise.
                Text(text, Modifier.align(if (panelOnTop || hidden) Alignment.Center else Alignment.TopCenter)
                    .then(if (panelOnTop || hidden) Modifier.padding(top = 160.dp) else Modifier.statusBarsPadding().padding(top = 60.dp)).padding(horizontal = 32.dp).clip(CircleShape)
                    .veilSurface(veil(VeilLevel.Focus), shape = CircleShape).padding(horizontal = 18.dp, vertical = 10.dp)
                    .semantics { contentDescription = text }, style = InkText.small.copy(fontSize = 14.sp), color = ink.ink, textAlign = TextAlign.Center)
            }
            AnimatedVisibility(chrome && !hidden, Modifier.align(if (panelOnTop) Alignment.TopCenter else Alignment.BottomCenter),
                enter = fadeIn(tween(motionMs(200))) + slideInVertically(tween(motionMs(260))) { if (panelOnTop) -it / 3 else it / 3 },
                exit = fadeOut(tween(motionMs(160))) + slideOutVertically(tween(motionMs(200))) { if (panelOnTop) -it / 3 else it / 3 }) {
                Box(Modifier.fillMaxWidth().then(if (panelOnTop) Modifier.statusBarsPadding().padding(top = 56.dp) else Modifier.navigationBarsPadding().imePadding())
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .onSizeChanged { panelH = with(density) { it.height.toDp() } + 20.dp }) {
                    GuidePanel(model, draft, maxH = (screenH * (when { panelOnTop -> .36f; full -> .6f; else -> .5f })).coerceAtMost(screenH - ime - statusTop - 72.dp),
                        onHide = { hidden = true }, onEdit = { editRequest = true }, onShowSearch = {
                            flow?.let { model.query.value = it.name }; update { it.tried(Tried.FlowShown) }; overlay = GuideOverlay.Search
                        }, onShowDrawer = { act(Action.Drawer) }, onShowLensSearch = { act(Action.Search) }, flow = flow, flowIsDemo = flow === demo, arcDemo = arcDemo, pulseArcDemo = pulseArcDemo,
                        leave = ::leave)
                }
            }
        }
    }
}

/** Swallows every touch before the Home sees it: the look steps show the Home, they do not run it. */
private fun Modifier.blockTouches() = pointerInput(Unit) {
    awaitPointerEventScope { while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() } }
}

/** A demo FLOW from apps on this phone (maps and music when they exist); never stored, always marked "Ukázka". */
private fun demoFlow(name: String, apps: List<AppEntry>): FlowAlias? {
    fun find(vararg words: String) = apps.firstOrNull { a -> words.any { a.component.packageName.contains(it, ignoreCase = true) } }
    val picks = listOfNotNull(find("maps", "navigation", "waze"), find("music", "spotify", "deezer", "tidal")).distinct()
        .ifEmpty { apps.take(2) }
    return if (picks.isEmpty()) null else FlowAlias("guide-demo", name, picks.map { it.key })
}

/** Hold PULSE to open Global ARC: a marked demo binding for the Controls stage, only when the user has none. */
private fun LauncherSettings.withPulseArcDemo(): LauncherSettings {
    if (Gesture.entries.any { controls.bound(Trigger.Pulse(it)) is Action.Arc }) return this
    if (controls.bound(Trigger.Pulse(Gesture.Hold)) != null) return this
    return copy(controlConfig = controls.bind(Trigger.Pulse(Gesture.Hold), Action.Arc()))
}

/**
 * Back, the progress (Kea-style track filled to the step) and Skip. Skip asks once which one is meant: this step, or
 * the rest of the guide (which lands on Done, so nothing is saved unseen). Done has nothing left to skip.
 */
@Composable
private fun TopBar(draft: OnboardingDraft, onBack: () -> Unit, skipMenu: Boolean, onSkipMenu: (Boolean) -> Unit, onSkip: () -> Unit,
    onSkipAll: () -> Unit, modifier: Modifier, onWall: Color) {
    val ink = LocalInk.current.copy(ink = onWall)
    val progress by animateFloatAsState((draft.index + 1f) / draft.steps.size, tween(motionMs(260)), label = "guide-progress")
    val label = stringResource(R.string.guide_progress, draft.index + 1, draft.steps.size)
    Column(modifier.fillMaxWidth().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconAction(Glyph.Back, stringResource(R.string.guide_back), onBack, tint = onWall)
            Box(Modifier.weight(1f).padding(horizontal = 8.dp).height(5.dp).clip(CircleShape).background(ink.ink.copy(alpha = .16f))
                .semantics { contentDescription = label; progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) }) {
                Box(Modifier.fillMaxWidth(progress).height(5.dp).clip(CircleShape).background(ink.accent))
            }
            if (!draft.last) Quiet(stringResource(R.string.guide_skip), { onSkipMenu(!skipMenu) }, Modifier.padding(end = 8.dp), color = ink.ink)
            else Spacer(Modifier.width(56.dp))
        }
        // Two plain choices under Skip, not a dialog; Back or a choice closes them.
        if (skipMenu && !draft.last) InteliaSkipMenu(onSkip = { onSkipMenu(false); onSkip() }, onSkipAll = { onSkipMenu(false); onSkipAll() },
            Modifier.align(Alignment.End).padding(end = 12.dp))
    }
}

@Composable
private fun InteliaSkipMenu(onSkip: () -> Unit, onSkipAll: () -> Unit, modifier: Modifier) {
    val ink = LocalInk.current
    val shape = RoundedCornerShape(20.dp)
    Column(modifier.width(220.dp).clip(shape).background(ink.paper.copy(alpha = .92f)).veilSurface(veil(VeilLevel.Focus), shape = shape)
        .padding(vertical = 6.dp)) {
        listOf(stringResource(R.string.guide_skip_step) to onSkip, stringResource(R.string.guide_skip_all) to onSkipAll).forEach { (text, go) ->
            Text(text, Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = go).padding(horizontal = 18.dp, vertical = 13.dp),
                style = InkText.body.copy(fontSize = 15.sp), color = ink.ink)
        }
    }
}

/**
 * "Porovnat": a small pill on the preview. Held, the stage shows where the guide started; released, the edits are back.
 * TalkBack has no hold, so a double tap toggles it there. It never changes the draft or the stored settings.
 */
@Composable
private fun ComparePill(comparing: Boolean, onCompare: (Boolean) -> Unit, modifier: Modifier) {
    val ink = LocalInk.current
    val text = stringResource(if (comparing) R.string.guide_compare_default else R.string.guide_compare)
    val hint = stringResource(R.string.guide_compare_hint)
    Text(text, modifier.clip(CircleShape).background(if (comparing) ink.accent else ink.paper.copy(alpha = .86f))
        .pointerInput(Unit) { detectTapGestures(onPress = { onCompare(true); tryAwaitRelease(); onCompare(false) }) }
        .semantics { contentDescription = "$text. $hint"; onClick { onCompare(!comparing); true } }
        .heightIn(min = 36.dp).padding(horizontal = 14.dp, vertical = 8.dp),
        style = InkText.small.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = if (comparing) ink.onAccent else ink.ink)
}

/**
 * One step's panel: title, a short line, the step's current choice (scrolling, with a cue when more lies below) and one
 * pinned primary action that never covers the content: the scroll area ends above it.
 */
@Composable
private fun GuidePanel(model: HomeViewModel, draft: OnboardingDraft, maxH: Dp, onHide: () -> Unit,
    onEdit: () -> Unit, onShowSearch: () -> Unit, onShowDrawer: () -> Unit, onShowLensSearch: () -> Unit, flow: FlowAlias?, flowIsDemo: Boolean,
    arcDemo: Boolean, pulseArcDemo: Boolean, leave: (() -> Unit) -> Unit) {
    val ink = LocalInk.current
    val scope = rememberCoroutineScope()
    fun update(transform: (OnboardingDraft) -> OnboardingDraft) = model.updateGuide(transform)
    val title = stringResource(when (draft.step) {
        GuideStep.Welcome -> R.string.guide_welcome_title
        GuideStep.Home -> R.string.guide_home_title
        GuideStep.Look -> R.string.guide_look_title
        GuideStep.Controls -> R.string.guide_controls_title
        GuideStep.Access -> R.string.guide_access_title
        GuideStep.Done -> R.string.guide_done_title
    })
    val line = when (draft.step) {
        GuideStep.Welcome -> stringResource(R.string.guide_welcome_body)
        GuideStep.Access -> stringResource(R.string.guide_access_body)
        else -> null
    }
    // Controls is a small coach over the full Home; the look steps leave room for the whole Home above the panel.
    val coach = draft.step == GuideStep.Controls
    val titleSize = when (draft.step) { GuideStep.Welcome -> 28.sp; GuideStep.Controls -> 19.sp; else -> 22.sp }
    // Legible over any wallpaper and over the Home's own clock: Focus glass on an almost solid paper base.
    val panelShape = RoundedCornerShape(if (coach) 24.dp else 28.dp)
    Column(Modifier.fillMaxWidth().heightIn(max = maxH).clip(panelShape).background(ink.paper.copy(alpha = .9f))
        .veilSurface(veil(VeilLevel.Focus), shape = panelShape)
        .semantics { paneTitle = title }.padding(horizontal = 18.dp, vertical = if (coach) 10.dp else 16.dp)) {
        if (draft.step == GuideStep.Welcome) Kicker(stringResource(R.string.guide_welcome_kicker).uppercase(), color = ink.accent)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f).padding(top = 2.dp), style = InkText.title.copy(fontSize = titleSize,
                lineHeight = titleSize * 1.18f, fontWeight = FontWeight.SemiBold), color = ink.ink)
            if (coach) SampleBadge(Modifier.padding(end = 4.dp))
            IconAction(Glyph.Close, stringResource(R.string.guide_hide_panel), onHide, tint = ink.muted)
        }
        line?.let { Text(it, Modifier.padding(top = 4.dp), style = InkText.body.copy(fontSize = 15.sp), color = ink.muted) }
        if (draft.step != GuideStep.Welcome) {
            val scroll = rememberScrollState()
            Box(Modifier.weight(1f, fill = false).padding(top = if (coach) 6.dp else 12.dp)) {
                Column(Modifier.fillMaxWidth().verticalScroll(scroll)) {
                    when (draft.step) {
                        GuideStep.Home -> HomeStep(model, draft, onEdit)
                        GuideStep.Look -> LookStep(model, draft)
                        GuideStep.Controls -> ControlsStep(model, draft, onShowSearch, onShowDrawer, onShowLensSearch, flow, flowIsDemo, arcDemo, pulseArcDemo)
                        GuideStep.Access -> AccessStep(model, draft)
                        GuideStep.Done -> DoneStep(model, draft)
                        GuideStep.Welcome -> Unit
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (scroll.canScrollForward) MoreBelow({ scope.launch { scroll.animateScrollTo(scroll.value + scroll.viewportSize * 2 / 3) } },
                    Modifier.align(Alignment.BottomCenter))
            }
        }
        Spacer(Modifier.height(if (coach) 8.dp else 12.dp))
        val primary = stringResource(when (draft.step) {
            GuideStep.Welcome -> R.string.guide_customize
            GuideStep.Done -> R.string.guide_go_home
            else -> R.string.guide_next
        })
        Capsule(primary, {
            when (draft.step) {
                GuideStep.Done -> leave { model.finishGuide() }
                else -> update { it.next() }
            }
        }, Modifier.fillMaxWidth().heightIn(min = if (coach) 48.dp else 54.dp))
        when (draft.step) {
            GuideStep.Welcome -> Quiet(stringResource(if (draft.rerun) R.string.guide_skip_all else R.string.guide_use_defaults),
                { leave { model.skipGuide() } }, Modifier.align(Alignment.CenterHorizontally), color = ink.ink)
            GuideStep.Done -> Quiet(stringResource(R.string.guide_explore), { leave { model.finishGuide(); model.explore.value = true } },
                Modifier.align(Alignment.CenterHorizontally), color = ink.ink)
            else -> Unit
        }
    }
}

/** The cue that more lies below the visible part of a panel: a fade into the paper and a small "more" pill. */
@Composable
private fun MoreBelow(onClick: () -> Unit, modifier: Modifier) {
    val ink = LocalInk.current
    Box(modifier.fillMaxWidth().height(44.dp).background(Brush.verticalGradient(listOf(Color.Transparent, ink.paper.copy(alpha = .96f)))),
        contentAlignment = Alignment.BottomCenter) {
        Text(stringResource(R.string.guide_more_below), Modifier.clip(CircleShape).background(ink.ink.copy(alpha = .1f))
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp, vertical = 6.dp),
            style = InkText.small.copy(fontSize = 13.sp), color = ink.ink)
    }
}

/** Categories in one line that scrolls sideways; the chosen one is kept in sight. Never wraps into two rows. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> GuideTabs(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val ink = LocalInk.current
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (value, label) ->
            val on = value == selected
            val near = remember { BringIntoViewRequester() }
            LaunchedEffect(on) { if (on) near.bringIntoView() }
            Box(Modifier.bringIntoViewRequester(near).heightIn(min = 44.dp).clip(CircleShape)
                .background(if (on) ink.accent.copy(alpha = if (ink.dark) .30f else .22f) else ink.ink.copy(alpha = .06f))
                .then(if (on) Modifier.border(1.dp, ink.accent.copy(alpha = .65f), CircleShape) else Modifier)
                .selectable(on, role = Role.Tab) { onSelect(value) }.padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center) {
                Text(label, style = InkText.body.copy(fontSize = 14.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (on) ink.ink else ink.muted, maxLines = 1, softWrap = false)
            }
        }
    }
}

/**
 * A Kea-like choice card: rows with a ring that fills, title and one line; a radio group for TalkBack. The chosen row's
 * extra controls ([below]) scroll into view when it is picked, so they are never left under the pinned button.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> ChoiceCard(options: List<Triple<T, String, String>>, selected: T?, onSelect: (T) -> Unit, below: @Composable (T) -> Unit = {}) {
    val ink = LocalInk.current
    Column(Modifier.fillMaxWidth().veilPane(RoundedCornerShape(24.dp), ink).selectableGroup()) {
        options.forEachIndexed { i, (value, name, detail) ->
            val on = value == selected
            if (i > 0) Hairline(Modifier.padding(horizontal = 16.dp))
            Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).selectable(on, role = Role.RadioButton) { onSelect(value) }
                .padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CheckRing(on)
                Column(Modifier.padding(start = 14.dp)) {
                    Text(name, style = InkText.body.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold), color = ink.ink)
                    Text(detail, style = InkText.small.copy(fontSize = 14.sp), color = ink.muted)
                }
            }
            if (on) {
                val near = remember { BringIntoViewRequester() }
                LaunchedEffect(Unit) { delay(80); near.bringIntoView() }
                Box(Modifier.bringIntoViewRequester(near)) { below(value) }
            }
        }
    }
}

/** The ring of a row: empty, or filled in the accent with a check (Kea's done state in Intelia's ring language). */
@Composable
private fun CheckRing(on: Boolean, modifier: Modifier = Modifier) {
    val ink = LocalInk.current
    Box(modifier.size(28.dp).clip(CircleShape).then(if (on) Modifier.background(ink.accent) else Modifier.border(1.5.dp, ink.faint, CircleShape)),
        contentAlignment = Alignment.Center) {
        if (on) HomeGlyph(Glyph.Check, null, tint = ink.onAccent, size = 16.dp)
    }
}

/** A Kea status chip: what is true right now, with a check when it is done. */
@Composable
private fun StateChip(text: String, done: Boolean, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val ink = LocalInk.current
    Row(modifier.clip(CircleShape).background(if (done) ink.accent.copy(alpha = .2f) else ink.ink.copy(alpha = .08f))
        .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
        .semantics(mergeDescendants = true) {}.heightIn(min = 40.dp).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (done) HomeGlyph(Glyph.Check, null, tint = ink.accent, size = 16.dp)
        else Box(Modifier.size(7.dp).clip(CircleShape).background(ink.muted))
        Text(text, Modifier.padding(start = 8.dp), style = InkText.small.copy(fontSize = 14.sp), color = if (done) ink.ink else ink.muted)
    }
}

@Composable
private fun HomeStep(model: HomeViewModel, draft: OnboardingDraft, onEdit: () -> Unit) {
    // Before a pick the card names what the stage really shows, so choice and preview never disagree.
    val choice = draft.home ?: if (draft.settings.home.activeLayout().items.none { it.kind == ItemKind.App }) HomeChoice.Empty else HomeChoice.Basic
    var picking by rememberSaveable { mutableStateOf(false) }
    ChoiceCard(listOf(
        Triple(HomeChoice.Empty, stringResource(R.string.guide_home_empty), stringResource(R.string.guide_home_empty_detail)),
        Triple(HomeChoice.Basic, stringResource(R.string.guide_home_basic), stringResource(R.string.guide_home_basic_detail)),
        Triple(HomeChoice.Custom, stringResource(R.string.guide_home_custom), stringResource(R.string.guide_home_custom_detail)),
    ), choice, { c -> picking = false; model.updateGuide { it.withHome(c, model.essentialKeys()) } }) { c ->
        Column(Modifier.fillMaxWidth().padding(start = 58.dp, end = 16.dp, bottom = 12.dp)) {
            when (c) {
                HomeChoice.Basic -> LensApps(model, draft, picking) { picking = it }
                HomeChoice.Custom -> Capsule(stringResource(R.string.guide_edit_layout), onEdit, Modifier.heightIn(min = 48.dp), filled = false)
                HomeChoice.Empty -> Unit
            }
        }
    }
}

/** The four apps beside LENS, and a search to change them (diacritics-insensitive, the launcher's own ranking). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LensApps(model: HomeViewModel, draft: OnboardingDraft, picking: Boolean, onPicking: (Boolean) -> Unit) {
    val ink = LocalInk.current
    val visible by model.visibleApps.collectAsStateWithLifecycle()
    val picked = draft.settings.home.activeLayout().items.filter { it.kind == ItemKind.App }.map { it.target }
    fun toggle(key: String) {
        val next = if (key in picked) picked - key else (picked + key).takeLast(OnboardingDraft.LENS_APPS)
        model.updateGuide { it.withLensApps(next, model.essentialKeys()) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            picked.forEach { key -> model.entry(key)?.let { AppIcon(it, Modifier.size(32.dp).semantics { contentDescription = it.label }) } }
        }
        Quiet(stringResource(if (picking) R.string.guide_apps_done else R.string.guide_change_apps), { onPicking(!picking) })
    }
    if (!picking) return
    var query by rememberSaveable { mutableStateOf("") }
    val hint = stringResource(R.string.guide_apps_search)
    BasicTextField(query, { query = it.take(40) }, Modifier.padding(top = 8.dp).fillMaxWidth().clip(CircleShape).background(ink.ink.copy(alpha = .07f))
        .padding(horizontal = 16.dp, vertical = 12.dp).semantics { contentDescription = hint }, singleLine = true,
        textStyle = InkText.body.copy(color = ink.ink), cursorBrush = SolidColor(ink.accent),
        decorationBox = { field -> Box { if (query.isEmpty()) Text(hint, style = InkText.body, color = ink.muted); field() } })
    Text(stringResource(R.string.guide_apps_limit), Modifier.padding(top = 6.dp), style = InkText.small, color = ink.muted)
    val ranked = remember(query, visible) { AppSearch.rank(visible, query) { it.searchKey }.take(30) }
    ranked.forEach { app ->
        val on = app.key in picked
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).selectable(on, role = Role.Checkbox) { toggle(app.key) }, verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app, Modifier.size(32.dp))
            Text(app.label, Modifier.weight(1f).padding(start = 12.dp), style = InkText.body, color = ink.ink, maxLines = 1)
            CheckRing(on, Modifier.size(24.dp))
        }
    }
}

@Composable
private fun LookStep(model: HomeViewModel, draft: OnboardingDraft) {
    val set: SettingsEdit = { t -> model.updateGuide { it.edit(t) } }
    var tab by rememberSaveable { mutableStateOf(LookTab.Theme) }
    GuideTabs(listOf(LookTab.Theme to stringResource(R.string.guide_tab_theme), LookTab.Color to stringResource(R.string.guide_tab_color),
        LookTab.Clock to stringResource(R.string.guide_tab_clock), LookTab.Icons to stringResource(R.string.guide_tab_icons),
        LookTab.Material to stringResource(R.string.guide_tab_material)), tab, { tab = it })
    Spacer(Modifier.height(10.dp))
    val s = draft.settings
    when (tab) {
        LookTab.Theme -> ThemeTiles(s.theme, s) { v -> set { it.copy(theme = v) } }
        LookTab.Color -> PaletteEditor(s, set)
        LookTab.Clock -> Chips(FACES.filter { it.first != TypeFace.Style }, draft.clockFace() ?: TypeFace.Unbounded, family = { faceFamily(it, 500) }) { f ->
            model.updateGuide { it.withClockFace(f) }
        }
        LookTab.Icons -> IconStyleChips(s, set)
        LookTab.Material -> MaterialChips(s, set)
    }
    GroupNote(stringResource(R.string.guide_more_in_studio))
}

/** The coach over the full Home: one line of what to try, chips that tick (and are the accessible way to try it). */
@Composable
private fun ControlsStep(model: HomeViewModel, draft: OnboardingDraft, onShowSearch: () -> Unit, onShowDrawer: () -> Unit, onShowLensSearch: () -> Unit,
    flow: FlowAlias?, flowIsDemo: Boolean, arcDemo: Boolean, pulseArcDemo: Boolean) {
    val ink = LocalInk.current
    GuideTabs(ControlTab.entries.map { it to it.name.uppercase() }, draft.tab, { t -> model.updateGuide { it.copy(tab = t) } })
    Spacer(Modifier.height(8.dp))
    @Composable
    fun hint(id: Int) = Text(stringResource(id), style = InkText.body.copy(fontSize = 15.sp), color = ink.ink)
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun chips(content: @Composable () -> Unit) = FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    when (draft.tab) {
        ControlTab.Lens -> {
            hint(R.string.guide_lens_hint)
            chips {
                StateChip(stringResource(R.string.guide_tried_tap), Tried.Tap in draft.tried, onClick = onShowLensSearch)
                StateChip(stringResource(R.string.guide_tried_swipe), Tried.Swipe in draft.tried, onClick = onShowDrawer)
            }
        }
        ControlTab.Arc -> {
            hint(R.string.guide_arc_hint)
            if (arcDemo) Text(stringResource(R.string.guide_arc_demo), Modifier.padding(top = 4.dp), style = InkText.small, color = ink.muted)
            chips { StateChip(stringResource(R.string.guide_tried_arc), Tried.ArcPick in draft.tried) }
        }
        ControlTab.Pulse -> PulseControls(model, draft, pulseArcDemo)
        ControlTab.Flow -> {
            hint(R.string.guide_flow_hint)
            if (flow != null) Column(Modifier.padding(top = 8.dp).fillMaxWidth().veilPane(RoundedCornerShape(20.dp), ink).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(flow.name, Modifier.weight(1f), style = InkText.item, color = ink.ink)
                    if (flowIsDemo) SampleBadge() else Kicker(stringResource(R.string.guide_flow_yours).uppercase())
                }
                BeadThread(flow.targets.map { model.entry(it)?.label }, stringResource(R.string.arc_slot_unavailable), Modifier.padding(top = 6.dp))
            }
            if (flow != null) Capsule(stringResource(R.string.guide_flow_show), onShowSearch, Modifier.padding(top = 8.dp).heightIn(min = 44.dp), filled = false)
        }
    }
}

/**
 * PULSE on the real Home at the camera: tap for Quick View, hold for Global ARC (a marked demo binding when the user has
 * none). Where PULSE works is one line here and opens into the three choices only on "Změnit".
 */
@Composable
private fun PulseControls(model: HomeViewModel, draft: OnboardingDraft, pulseArcDemo: Boolean) {
    val ink = LocalInk.current
    val service by PulseAccessibilityService.running.collectAsStateWithLifecycle()
    var choosing by rememberSaveable { mutableStateOf(false) }
    if (draft.pulse == PulsePick.Off) Text(stringResource(R.string.guide_pulse_off_hint), style = InkText.body.copy(fontSize = 15.sp), color = ink.ink)
    else {
        Text(stringResource(R.string.guide_pulse_hint), style = InkText.body.copy(fontSize = 15.sp), color = ink.ink)
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StateChip(stringResource(R.string.guide_tried_pulse), Tried.PulseTap in draft.tried)
            if (pulseArcDemo || Tried.PulseArc in draft.tried) StateChip(stringResource(R.string.guide_tried_pulse_arc), Tried.PulseArc in draft.tried)
        }
    }
    val mode = stringResource(when (draft.pulse) {
        PulsePick.Everywhere -> R.string.pulse_everywhere_short
        PulsePick.HomeOnly -> R.string.pulse_home_only
        PulsePick.Off -> R.string.pulse_everywhere_off
    })
    Row(Modifier.padding(top = 10.dp).fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.guide_pulse_where), Modifier.weight(1f), style = InkText.small.copy(fontSize = 14.sp), color = ink.muted)
        Text(mode, Modifier.padding(end = 8.dp), style = InkText.body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = ink.ink)
        Quiet(stringResource(if (choosing) R.string.guide_apps_done else R.string.guide_change), { choosing = !choosing })
    }
    if (choosing || draft.pulse == PulsePick.Off) ChoiceCard(listOf(
        Triple(PulsePick.Everywhere, stringResource(R.string.pulse_everywhere_short), stringResource(R.string.guide_pulse_everywhere_detail)),
        Triple(PulsePick.HomeOnly, stringResource(R.string.pulse_home_only), stringResource(R.string.guide_pulse_home_detail)),
        Triple(PulsePick.Off, stringResource(R.string.pulse_everywhere_off), stringResource(R.string.guide_pulse_off_detail)),
    ), draft.pulse, { p -> model.updateGuide { it.withPulse(p) } })
    if (draft.pulse == PulsePick.Everywhere && !service) GroupNote(stringResource(R.string.guide_pulse_needs))
}

@Composable
private fun AccessStep(model: HomeViewModel, draft: OnboardingDraft) {
    val context = LocalContext.current
    val role = rememberHomeRole()
    val service by PulseAccessibilityService.running.collectAsStateWithLifecycle()
    // Read again on every return from Android Settings; never taken from a stored preference.
    var media by remember { mutableStateOf(MediaWatch.allowed(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { media = MediaWatch.allowed(context) }
    val pulseRow = draft.pulse == PulsePick.Everywhere
    val musicRow = draft.pulse != PulsePick.Off
    val music = media && draft.wantsMusic
    val ink = LocalInk.current
    Column(Modifier.fillMaxWidth().veilPane(RoundedCornerShape(24.dp), ink)) {
        PermissionRow(stringResource(R.string.guide_perm_home), stringResource(R.string.guide_perm_home_detail),
            stringResource(R.string.guide_perm_home_more), role.held, stringResource(R.string.guide_perm_set), role.request)
        if (pulseRow) {
            Hairline(Modifier.padding(horizontal = 16.dp))
            PermissionRow(stringResource(R.string.guide_perm_pulse), stringResource(R.string.guide_perm_pulse_detail),
                stringResource(R.string.guide_perm_pulse_more), service, stringResource(R.string.guide_perm_set)) { PulseAccessibilityService.openSettings(context) }
        }
        if (musicRow) {
            Hairline(Modifier.padding(horizontal = 16.dp))
            PermissionRow(stringResource(R.string.guide_perm_music), stringResource(R.string.guide_perm_music_detail),
                stringResource(R.string.guide_perm_music_more), music, stringResource(R.string.guide_perm_set)) {
                model.updateGuide { it.copy(wantsMusic = true) }
                if (!media) MediaWatch.openSettings(context)
            }
        }
    }
}

/**
 * Kea's permission row: state ring and the real state, what it gives (one line), one action, and the longer
 * explanation only on "Podrobnosti". The state is read from the system, never stored.
 */
@Composable
private fun PermissionRow(title: String, detail: String, more: String, done: Boolean, action: String, onAction: () -> Unit) {
    val ink = LocalInk.current
    var open by rememberSaveable(title) { mutableStateOf(false) }
    val state = stringResource(if (done) R.string.guide_perm_on else R.string.guide_perm_off)
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.Top) {
        CheckRing(done, Modifier.padding(top = 2.dp))
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Column(Modifier.semantics(mergeDescendants = true) { stateDescription = state }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), style = InkText.body.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold), color = ink.ink)
                    Text(state, style = InkText.small.copy(fontSize = 13.sp), color = if (done) ink.accent else ink.muted)
                }
                Text(detail, style = InkText.small.copy(fontSize = 14.sp), color = ink.muted)
            }
            if (open) Text(more, Modifier.padding(top = 6.dp), style = InkText.small.copy(fontSize = 13.sp), color = ink.muted)
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!done) Text(action, Modifier.clip(CircleShape).background(ink.accent.copy(alpha = .2f))
                    .clickable(role = Role.Button, onClick = onAction).heightIn(min = 40.dp).padding(horizontal = 16.dp, vertical = 10.dp),
                    style = InkText.small.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = ink.accent)
                Quiet(stringResource(if (open) R.string.guide_perm_less else R.string.guide_perm_more), { open = !open },
                    Modifier.padding(start = if (done) 0.dp else 8.dp), color = ink.muted)
            }
        }
    }
}

/** Done: plocha, motiv and PULSE (with what it really does now), each back to its step; the Home above is what stays. */
@Composable
private fun DoneStep(model: HomeViewModel, draft: OnboardingDraft) {
    val ink = LocalInk.current
    val s = draft.settings
    val home = stringResource(when (draft.home ?: if (s.home.activeLayout().items.none { it.kind == ItemKind.App }) HomeChoice.Empty else HomeChoice.Basic) {
        HomeChoice.Empty -> R.string.guide_home_empty
        HomeChoice.Basic -> R.string.guide_home_basic
        HomeChoice.Custom -> R.string.guide_home_custom
    })
    val service by PulseAccessibilityService.running.collectAsStateWithLifecycle()
    // What PULSE will really do after Done, as Studio reports it: Everywhere waits for its service.
    val pulse = stringResource(when (draft.pulse) {
        PulsePick.Everywhere -> if (service) R.string.pulse_everywhere_short else R.string.guide_pulse_pending
        PulsePick.HomeOnly -> R.string.pulse_home_only
        PulsePick.Off -> R.string.pulse_everywhere_off
    })
    Column(Modifier.fillMaxWidth().veilPane(RoundedCornerShape(24.dp), ink)) {
        listOf(
            Triple(stringResource(R.string.home_section), home, GuideStep.Home),
            Triple(stringResource(R.string.guide_tab_theme), stringResource(themeName(s.theme)), GuideStep.Look),
            Triple("PULSE", pulse, GuideStep.Controls),
        ).forEachIndexed { i, (name, value, step) ->
            if (i > 0) Hairline(Modifier.padding(horizontal = 16.dp))
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button) {
                model.updateGuide { it.goTo(step).let { d -> if (step == GuideStep.Controls) d.copy(tab = ControlTab.Pulse) else d } }
            }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(name, Modifier.weight(1f), style = InkText.body, color = ink.muted)
                Text(value, style = InkText.body.copy(fontWeight = FontWeight.SemiBold), color = ink.ink)
            }
        }
    }
    Text(stringResource(R.string.guide_done_body), Modifier.padding(top = 10.dp), style = InkText.small.copy(fontSize = 14.sp), color = ink.muted)
}

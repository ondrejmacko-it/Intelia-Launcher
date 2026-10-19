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
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val shown = if (compare) draft.start else draft.settings
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

    BackHandler(enabled = !editing && leaving == null) {
        when {
            overlay != null -> closeOverlay()
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
        val stageTop = statusTop + 64.dp
        val room = screenH - stageTop - panelH - 20.dp
        val mini = (room / screenH).coerceIn(.2f, .8f)
        val scale by animateFloatAsState(if (full) 1f else mini, tween(motionMs(320), easing = VeilMotion.ease), label = "guide-scale")
        val shift by animateDpAsState(if (full) 0.dp else stageTop, tween(motionMs(320), easing = VeilMotion.ease), label = "guide-shift")
        val scrim by animateFloatAsState(if (full) 0f else .32f, tween(motionMs(320)), label = "guide-scrim")
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrim)))

        // The stage: the real Home of the shown settings, laid out at the screen's size and scaled as a layer.
        InteliaTheme(shown) {
            val homeInk = LocalHomeInk.current
            if (full) SystemBarIcons(darkIcons = homeInk.luminance() < .5f)
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
                Home(
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
                )
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
            if (!full) SystemBarIcons(darkIcons = !ink.dark)
            val chrome = leaving == null && !editing && overlay == null
            if (chrome && !hidden && draft.step != GuideStep.Welcome) TopBar(draft, onBack = { update { it.back() } },
                onSkip = { update { it.skip() } }, Modifier.align(Alignment.TopCenter))
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
                Box(Modifier.fillMaxWidth().then(if (panelOnTop) Modifier.statusBarsPadding().padding(top = 56.dp) else Modifier.navigationBarsPadding())
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .onSizeChanged { panelH = with(density) { it.height.toDp() } + 20.dp }) {
                    GuidePanel(model, draft, maxH = screenH * (when { panelOnTop -> .42f; full -> .6f; else -> .5f }), compare = compare, onCompare = { compare = it },
                        onHide = { hidden = true }, onEdit = { editRequest = true }, onShowSearch = {
                            flow?.let { model.query.value = it.name }; update { it.tried(Tried.FlowShown) }; overlay = GuideOverlay.Search
                        }, onShowDrawer = { act(Action.Drawer) }, onShowLensSearch = { act(Action.Search) }, flow = flow, flowIsDemo = flow === demo, arcDemo = arcDemo,
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

/** Back, the progress (Kea-style track filled to the step) and Skip; Done has nothing left to skip. */
@Composable
private fun TopBar(draft: OnboardingDraft, onBack: () -> Unit, onSkip: () -> Unit, modifier: Modifier) {
    val ink = LocalInk.current
    val progress by animateFloatAsState((draft.index + 1f) / draft.steps.size, tween(motionMs(260)), label = "guide-progress")
    val label = stringResource(R.string.guide_progress, draft.index + 1, draft.steps.size)
    Row(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        BackButton(stringResource(R.string.guide_back), onBack)
        Box(Modifier.weight(1f).padding(horizontal = 8.dp).height(5.dp).clip(CircleShape).background(ink.ink.copy(alpha = .16f))
            .semantics { contentDescription = label; progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) }) {
            Box(Modifier.fillMaxWidth(progress).height(5.dp).clip(CircleShape).background(ink.accent))
        }
        if (!draft.last) Quiet(stringResource(R.string.guide_skip), onSkip, Modifier.padding(end = 8.dp), color = ink.ink)
        else Spacer(Modifier.width(56.dp))
    }
}

/** One step's panel: title, a short line, the step's few choices (scrolling), one pinned primary action. */
@Composable
private fun GuidePanel(model: HomeViewModel, draft: OnboardingDraft, maxH: Dp, compare: Boolean, onCompare: (Boolean) -> Unit, onHide: () -> Unit,
    onEdit: () -> Unit, onShowSearch: () -> Unit, onShowDrawer: () -> Unit, onShowLensSearch: () -> Unit, flow: FlowAlias?, flowIsDemo: Boolean,
    arcDemo: Boolean, leave: (() -> Unit) -> Unit) {
    val ink = LocalInk.current
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
        GuideStep.Look -> stringResource(R.string.guide_look_body)
        GuideStep.Access -> stringResource(R.string.guide_access_body)
        GuideStep.Done -> stringResource(R.string.guide_done_body)
        else -> null
    }
    // Controls coaches over the full Home; the look steps leave room for the whole Home above the panel.
    val compact = draft.step != GuideStep.Welcome
    // Legible over any wallpaper and over the Home's own clock: Focus glass on a paper base.
    val panelShape = RoundedCornerShape(30.dp)
    Column(Modifier.fillMaxWidth().heightIn(max = maxH).clip(panelShape).background(ink.paper.copy(alpha = .8f))
        .veilSurface(veil(VeilLevel.Focus), shape = panelShape)
        .semantics { paneTitle = title }.padding(horizontal = 20.dp, vertical = if (compact) 14.dp else 20.dp)) {
        if (draft.step == GuideStep.Welcome) Kicker(stringResource(R.string.guide_welcome_kicker).uppercase(), color = ink.accent)
        else if (draft.settings != draft.start && draft.step in setOf(GuideStep.Home, GuideStep.Look, GuideStep.Done)) Compare(compare, onCompare)
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = InkText.title.copy(fontSize = if (compact) 24.sp else 30.sp,
                lineHeight = if (compact) 28.sp else 34.sp, fontWeight = FontWeight.SemiBold), color = ink.ink)
            if (draft.step == GuideStep.Controls) SampleBadge(Modifier.padding(end = 4.dp))
            IconAction(Glyph.Close, stringResource(R.string.guide_hide_panel), onHide, tint = ink.muted)
        }
        line?.let { Text(it, Modifier.padding(top = 6.dp), style = InkText.body.copy(fontSize = 15.sp), color = ink.muted) }
        Column(Modifier.weight(1f, fill = false).padding(top = 14.dp).verticalScroll(rememberScrollState())) {
            when (draft.step) {
                GuideStep.Welcome -> Unit
                GuideStep.Home -> HomeStep(model, draft, onEdit)
                GuideStep.Look -> LookStep(model, draft)
                GuideStep.Controls -> ControlsStep(model, draft, onShowSearch, onShowDrawer, onShowLensSearch, flow, flowIsDemo, arcDemo)
                GuideStep.Access -> AccessStep(model, draft)
                GuideStep.Done -> DoneStep(model, draft)
            }
        }
        Spacer(Modifier.height(12.dp))
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
        }, Modifier.fillMaxWidth().heightIn(min = 56.dp))
        when (draft.step) {
            GuideStep.Welcome -> Quiet(stringResource(if (draft.rerun) R.string.guide_skip else R.string.guide_use_defaults),
                { leave { model.skipGuide() } }, Modifier.align(Alignment.CenterHorizontally), color = ink.ink)
            GuideStep.Done -> Quiet(stringResource(R.string.guide_explore), { leave { model.finishGuide(); model.explore.value = true } },
                Modifier.align(Alignment.CenterHorizontally), color = ink.ink)
            else -> Unit
        }
    }
}

/** "Výchozí / Moje úpravy": one big stage that switches at once, never two small pictures side by side. */
@Composable
private fun Compare(mine: Boolean, onCompare: (Boolean) -> Unit) {
    VeilChoice(listOf(true to stringResource(R.string.guide_compare_default), false to stringResource(R.string.guide_compare_mine)), mine, onCompare)
}

/** A Kea-like choice card: rows with a ring that fills, title and one line; a radio group for TalkBack. */
@Composable
private fun <T> ChoiceCard(options: List<Triple<T, String, String>>, selected: T?, onSelect: (T) -> Unit, below: @Composable (T) -> Unit = {}) {
    val ink = LocalInk.current
    Column(Modifier.fillMaxWidth().veilPane(RoundedCornerShape(24.dp), ink).selectableGroup()) {
        options.forEachIndexed { i, (value, name, detail) ->
            val on = value == selected
            if (i > 0) Hairline(Modifier.padding(horizontal = 16.dp))
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).selectable(on, role = Role.RadioButton) { onSelect(value) }
                .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                CheckRing(on)
                Column(Modifier.padding(start = 14.dp)) {
                    Text(name, style = InkText.body.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold), color = ink.ink)
                    Text(detail, style = InkText.small.copy(fontSize = 14.sp), color = ink.muted)
                }
            }
            if (on) below(value)
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
        .semantics(mergeDescendants = true) {}.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (done) HomeGlyph(Glyph.Check, null, tint = ink.accent, size = 16.dp)
        else Box(Modifier.size(7.dp).clip(CircleShape).background(ink.muted))
        Text(text, Modifier.padding(start = 8.dp), style = InkText.small.copy(fontSize = 14.sp), color = if (done) ink.ink else ink.muted)
    }
}

@Composable
private fun HomeStep(model: HomeViewModel, draft: OnboardingDraft, onEdit: () -> Unit) {
    val choice = draft.home ?: if (draft.settings.home.activeLayout().items.none { it.kind == ItemKind.App }) HomeChoice.Empty else HomeChoice.Basic
    var picking by rememberSaveable { mutableStateOf(false) }
    ChoiceCard(listOf(
        Triple(HomeChoice.Empty, stringResource(R.string.guide_home_empty), stringResource(R.string.guide_home_empty_detail)),
        Triple(HomeChoice.Basic, stringResource(R.string.guide_home_basic), stringResource(R.string.guide_home_basic_detail)),
        Triple(HomeChoice.Custom, stringResource(R.string.guide_home_custom), stringResource(R.string.guide_home_custom_detail)),
    ), choice, { c -> picking = false; model.updateGuide { it.withHome(c, model.essentialKeys()) } }) { c ->
        Column(Modifier.fillMaxWidth().padding(start = 58.dp, end = 16.dp, bottom = 14.dp)) {
            when (c) {
                HomeChoice.Basic -> LensApps(model, draft, picking) { picking = it }
                HomeChoice.Custom -> Capsule(stringResource(R.string.guide_edit_layout), onEdit, filled = false)
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
        Text(stringResource(R.string.guide_lens_apps), Modifier.weight(1f), style = InkText.small, color = ink.muted)
        Quiet(stringResource(if (picking) R.string.guide_apps_done else R.string.guide_change_apps), { onPicking(!picking) })
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        picked.forEach { key -> model.entry(key)?.let { AppIcon(it, Modifier.size(36.dp).semantics { contentDescription = it.label }) } }
    }
    if (!picking) return
    var query by rememberSaveable { mutableStateOf("") }
    val hint = stringResource(R.string.guide_apps_search)
    BasicTextField(query, { query = it.take(40) }, Modifier.padding(top = 10.dp).fillMaxWidth().clip(CircleShape).background(ink.ink.copy(alpha = .07f))
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
    VeilChoice(listOf(LookTab.Theme to stringResource(R.string.guide_tab_theme), LookTab.Color to stringResource(R.string.guide_tab_color),
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

@Composable
private fun ControlsStep(model: HomeViewModel, draft: OnboardingDraft, onShowSearch: () -> Unit, onShowDrawer: () -> Unit, onShowLensSearch: () -> Unit,
    flow: FlowAlias?, flowIsDemo: Boolean, arcDemo: Boolean) {
    val ink = LocalInk.current
    VeilChoice(ControlTab.entries.map { it to it.name.uppercase() }, draft.tab, { t -> model.updateGuide { it.copy(tab = t) } })
    Spacer(Modifier.height(10.dp))
    @Composable
    fun hint(id: Int) = Text(stringResource(id), style = InkText.body.copy(fontSize = 15.sp), color = ink.ink)
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun chips(content: @Composable () -> Unit) = FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    when (draft.tab) {
        ControlTab.Lens -> {
            hint(R.string.guide_lens_hint)
            // The chips are also the accessible way to show each gesture's result.
            chips {
                StateChip(stringResource(R.string.guide_tried_tap), Tried.Tap in draft.tried, onClick = onShowLensSearch)
                StateChip(stringResource(R.string.guide_tried_swipe), Tried.Swipe in draft.tried, onClick = onShowDrawer)
            }
        }
        ControlTab.Arc -> {
            hint(R.string.guide_arc_hint)
            if (arcDemo) Text(stringResource(R.string.guide_arc_demo), Modifier.padding(top = 6.dp), style = InkText.small, color = ink.muted)
            chips { StateChip(stringResource(R.string.guide_tried_arc), Tried.ArcPick in draft.tried) }
        }
        ControlTab.Pulse -> PulseControls(model, draft)
        ControlTab.Flow -> {
            hint(R.string.guide_flow_hint)
            if (flow != null) Column(Modifier.padding(top = 10.dp).fillMaxWidth().veilPane(RoundedCornerShape(20.dp), ink).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(flow.name, Modifier.weight(1f), style = InkText.item, color = ink.ink)
                    if (flowIsDemo) SampleBadge() else Kicker(stringResource(R.string.guide_flow_yours).uppercase())
                }
                BeadThread(flow.targets.map { model.entry(it)?.label }, stringResource(R.string.arc_slot_unavailable), Modifier.padding(top = 6.dp))
            }
            Text(stringResource(R.string.guide_flow_where), Modifier.padding(top = 8.dp), style = InkText.small, color = ink.muted)
            if (flow != null) Capsule(stringResource(R.string.guide_flow_show), onShowSearch, Modifier.padding(top = 10.dp), filled = false)
        }
    }
}

/** Where PULSE works, and its states drawn by the PULSE page's own stage (sample music marked as such). */
@Composable
private fun PulseControls(model: HomeViewModel, draft: OnboardingDraft) {
    val ink = LocalInk.current
    val service by PulseAccessibilityService.running.collectAsStateWithLifecycle()
    Text(stringResource(R.string.guide_pulse_hint), style = InkText.body.copy(fontSize = 15.sp), color = ink.ink)
    Spacer(Modifier.height(10.dp))
    ChoiceCard(listOf(
        Triple(PulsePick.Everywhere, stringResource(R.string.pulse_everywhere_short), stringResource(R.string.guide_pulse_everywhere_detail)),
        Triple(PulsePick.HomeOnly, stringResource(R.string.pulse_home_only), stringResource(R.string.guide_pulse_home_detail)),
        Triple(PulsePick.Off, stringResource(R.string.pulse_everywhere_off), stringResource(R.string.guide_pulse_off_detail)),
    ), draft.pulse, { p -> model.updateGuide { it.withPulse(p) } })
    if (draft.pulse == PulsePick.Everywhere && !service) GroupNote(stringResource(R.string.guide_pulse_needs))
    if (draft.pulse == PulsePick.Off) return
    var look by rememberSaveable { mutableStateOf(PulseLook.Rest) }
    Text(stringResource(R.string.guide_pulse_states), Modifier.padding(top = 12.dp, bottom = 6.dp), style = InkText.small, color = ink.muted)
    VeilChoice(listOf(PulseLook.Rest to stringResource(R.string.pulse_preview_rest), PulseLook.Music to stringResource(R.string.pulse_preview_music),
        PulseLook.Expanded to stringResource(R.string.pulse_preview_expanded), PulseLook.Arc to stringResource(R.string.global_arc)), look, { look = it })
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
        // Music is a sample here even without notification access; Studio's stage marks it "Ukázka".
        PulseStage(model, draft.settings.copy(pulseMusic = draft.settings.pulseMusic || look == PulseLook.Music), look, 170.dp, maxWidth)
    }
}

@Composable
private fun AccessStep(model: HomeViewModel, draft: OnboardingDraft) {
    val context = LocalContext.current
    val role = rememberHomeRole()
    val service by PulseAccessibilityService.running.collectAsStateWithLifecycle()
    var media by remember { mutableStateOf(MediaWatch.allowed(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { media = MediaWatch.allowed(context) }
    val pulseRow = draft.pulse == PulsePick.Everywhere
    val musicRow = draft.pulse != PulsePick.Off
    val music = media && draft.wantsMusic
    val ready = role.held && (!pulseRow || service) && (!musicRow || music)
    StateChip(stringResource(if (ready) R.string.guide_access_ready else R.string.guide_access_waiting), ready)
    Spacer(Modifier.height(12.dp))
    val ink = LocalInk.current
    Column(Modifier.fillMaxWidth().veilPane(RoundedCornerShape(24.dp), ink)) {
        PermissionRow(stringResource(R.string.guide_perm_home), stringResource(R.string.guide_perm_home_detail), role.held,
            stringResource(R.string.guide_perm_set), role.request)
        if (pulseRow) {
            Hairline(Modifier.padding(horizontal = 16.dp))
            PermissionRow(stringResource(R.string.guide_perm_pulse), stringResource(R.string.guide_perm_pulse_detail), service,
                stringResource(R.string.guide_perm_open)) { PulseAccessibilityService.openSettings(context) }
        }
        if (musicRow) {
            Hairline(Modifier.padding(horizontal = 16.dp))
            PermissionRow(stringResource(R.string.guide_perm_music), stringResource(R.string.guide_perm_music_detail), music,
                stringResource(if (media) R.string.guide_perm_set else R.string.guide_perm_open)) {
                model.updateGuide { it.copy(wantsMusic = true) }
                if (!media) MediaWatch.openSettings(context)
            }
        }
    }
}

/** Kea's permission row: state ring, what it gives (one line), and one action; the state is read, never stored. */
@Composable
private fun PermissionRow(title: String, detail: String, done: Boolean, action: String, onAction: () -> Unit) {
    val ink = LocalInk.current
    val state = stringResource(if (done) R.string.guide_perm_on else R.string.guide_perm_off)
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).semantics(mergeDescendants = true) { stateDescription = state }
        .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        CheckRing(done)
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 8.dp)) {
            Text(title, style = InkText.body.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold), color = ink.ink)
            Text(detail, style = InkText.small.copy(fontSize = 13.sp), color = ink.muted)
        }
        if (!done) Text(action, Modifier.clip(CircleShape).background(ink.accent.copy(alpha = .2f)).clickable(role = Role.Button, onClick = onAction)
            .padding(horizontal = 14.dp, vertical = 10.dp), style = InkText.small.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = ink.accent)
    }
}

/** Done: what the user chose, each row back to its step; the Home above is exactly what Go to Home keeps. */
@Composable
private fun DoneStep(model: HomeViewModel, draft: OnboardingDraft) {
    val ink = LocalInk.current
    val s = draft.settings
    val home = stringResource(when (draft.home ?: if (s.home.activeLayout().items.none { it.kind == ItemKind.App }) HomeChoice.Empty else HomeChoice.Basic) {
        HomeChoice.Empty -> R.string.guide_home_empty
        HomeChoice.Basic -> R.string.guide_home_basic
        HomeChoice.Custom -> R.string.guide_home_custom
    })
    val pulse = stringResource(when (draft.pulse) {
        PulsePick.Everywhere -> R.string.pulse_everywhere_short
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
}

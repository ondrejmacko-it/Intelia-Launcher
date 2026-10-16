package dev.intelia.launcher.ui

import androidx.activity.compose.BackHandler
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
import dev.intelia.launcher.data.HomeLayout
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
                    favorites = favorites,
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
 * and the wallpaper-derived ink.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Home(
    settings: LauncherSettings,
    favorites: List<AppEntry>,
    arc: ArcContent,
    flowSteps: (FlowAlias) -> List<String?>,
    onSearch: () -> Unit,
    onDrawer: () -> Unit,
    onStudio: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onOptions: (AppEntry) -> Unit,
    onRun: (FlowAlias) -> Unit,
    onShortcut: (ShortcutEntry) -> Unit,
) {
    val homeInk = LocalHomeInk.current
    val accent = LocalInk.current.accent
    val density = LocalDensity.current
    val now by rememberNow()
    var arcOpen by remember { mutableStateOf(false) }
    // ARC speaks over the top of Home, so the clock and favourites step fully aside.
    val quiet by animateFloatAsState(if (arcOpen) 0f else 1f, tween(motionMs(180)), label = "home-quiet")
    val swipe = with(density) { 64.dp.toPx() }
    val studioLabel = stringResource(R.string.studio)

    BoxWithConstraints(Modifier.fillMaxSize()
        .pointerInput(Unit) {
            var drag = 0f
            detectVerticalDragGestures(onDragStart = { drag = 0f }, onVerticalDrag = { _, amount -> drag += amount },
                onDragEnd = { if (drag < -swipe) onDrawer() })
        }
        .combinedClickable(interactionSource = null, indication = null, onLongClickLabel = studioLabel,
            onClick = {}, onLongClick = onStudio)) {
        val custom = settings.layout == HomeLayout.Custom
        val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
        val cx = maxWidth / 2 + lensShift(settings.reach)
        val cy = maxHeight - bottomInset - LensLift
        val compact = maxHeight < 560.dp

        if (settings.showClock || settings.showDate) Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = if (compact) 24.dp else if (custom) 72.dp else 92.dp)
                .padding(horizontal = 28.dp).graphicsLayer { alpha = quiet },
            horizontalAlignment = if (custom) Alignment.Start else Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (settings.showClock) SplitClock(now.toLocalTime(), settings.clockStyle, homeInk, accent, if (compact) 64.sp else 84.sp)
            // 4.2: 12 sp caps, +2.2 tracking, fg2 (62 %).
            if (settings.showDate) Text(dateLine(now), style = CrystalType.date.copy(shadow = homeShadow(homeInk)),
                color = homeInk.copy(alpha = .62f), maxLines = 1)
        }

        when (settings.layout) {
            HomeLayout.Pure -> Unit
            HomeLayout.Essentials -> favorites.take(4).let { around ->
                // 4.3: two pairs at x = 72/118 and 242/288 dp of a 360 dp screen, i.e. ±62 and ±108 dp
                // from LENS, on its baseline; the wider centre gap keeps it from reading as a dock.
                val offsets = when (around.size) {
                    1 -> listOf((-62).dp)
                    2 -> listOf((-62).dp, 62.dp)
                    3 -> listOf((-108).dp, (-62).dp, 62.dp)
                    else -> listOf((-108).dp, (-62).dp, 62.dp, 108.dp)
                }
                around.zip(offsets)
            }.forEach { (app, offset) ->
                val x = (cx + offset).coerceIn(24.dp, maxWidth - 24.dp)
                Box(Modifier.centerAt(x, cy).size(CrystalDims.touch).clip(CircleShape).graphicsLayer { alpha = quiet }
                    .combinedClickable(role = Role.Button, onLongClickLabel = stringResource(R.string.app_options),
                        onClick = { onLaunch(app) }, onLongClick = { onOptions(app) })
                    .semantics { contentDescription = app.label }, contentAlignment = Alignment.Center) {
                    // No backing (4.3): system or icon-pack icons as they are; monochrome tinted fg.
                    AppIcon(app, Modifier.size(CrystalDims.iconHome), tint = homeInk)
                }
            }
            HomeLayout.Custom -> Column(
                Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 28.dp, bottom = bottomInset + LensLift + 64.dp)
                    .graphicsLayer { alpha = quiet },
            ) {
                favorites.forEach { app ->
                    Text(app.label, Modifier.widthIn(max = 320.dp).heightIn(min = 48.dp)
                        .combinedClickable(role = Role.Button, onLongClickLabel = stringResource(R.string.app_options),
                            onClick = { onLaunch(app) }, onLongClick = { onOptions(app) })
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                        style = InkText.item.copy(fontSize = 30.sp, lineHeight = 34.sp, shadow = homeShadow(homeInk)),
                        color = homeInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        LensArc(arc, settings.reach, flowSteps, onSearch = onSearch, onDrawer = onDrawer, onLaunch = onLaunch, onRun = onRun,
            onShortcut = onShortcut, onOpenChange = { arcOpen = it })
        if (settings.pulse) Pulse(settings)
    }
}

@Composable
private fun InkSnackbar(text: String) {
    val ink = LocalInk.current
    Box(Modifier.padding(horizontal = 24.dp).clip(CircleShape).background(ink.night).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(text, style = InkText.small.copy(fontSize = 14.sp), color = ink.onNight)
    }
}

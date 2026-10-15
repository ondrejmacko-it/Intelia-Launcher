package dev.intelia.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.intelia.launcher.R
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.flow.Flow
import dev.intelia.launcher.flow.FlowAlias
import dev.intelia.launcher.intelia
import dev.intelia.launcher.search.AppSearch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Page { Home, Search, Flow }

/** Editing target for the FLOW sheet: null alias means a new command. */
private data class FlowEdit(val alias: FlowAlias?)

@Composable
fun HomeRoot(model: HomeViewModel, homeSignal: Int) {
    val settings by model.settings.collectAsStateWithLifecycle()
    InteliaTheme(settings) {
        // Home draws on the wallpaper without a Surface, so text needs an explicit content colour.
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) { HomeContent(model, homeSignal) }
    }
}

@Composable
private fun HomeContent(model: HomeViewModel, homeSignal: Int) {
    val context = LocalContext.current
    val settings by model.settings.collectAsStateWithLifecycle()
    val query by model.query.collectAsStateWithLifecycle()
    val visible by model.visibleApps.collectAsStateWithLifecycle()
    val results by model.results.collectAsStateWithLifecycle()
    val flows by model.flowMatches.collectAsStateWithLifecycle()
    val arc by model.arcApps.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(Page.Home) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var options by remember { mutableStateOf<AppEntry?>(null) }
    var flowEdit by remember { mutableStateOf<FlowEdit?>(null) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val searchFocus = remember { FocusRequester() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    val dark = isDark(settings)
    val shown = if (query.isNotBlank()) Page.Search else page

    fun message(id: Int) { scope.launch { snackbar.showSnackbar(context.getString(id)) } }
    fun goHome() { page = Page.Home; model.query.value = ""; focus.clearFocus(); keyboard?.hide() }
    fun open(next: Page) { page = next; if (next != Page.Search) { model.query.value = ""; focus.clearFocus(); keyboard?.hide() } }
    fun launch(entry: AppEntry) {
        if (model.launch(entry)) goHome() else message(R.string.launch_failed)
    }
    fun run(alias: FlowAlias) {
        when (model.runFlow(alias)) {
            FlowRun.Launched -> goHome()
            FlowRun.Unavailable -> message(R.string.flow_unavailable)
            FlowRun.Failed -> message(R.string.launch_failed)
        }
    }

    LaunchedEffect(homeSignal) {
        if (homeSignal > 0) { settingsOpen = false; options = null; flowEdit = null; goHome() }
    }
    LaunchedEffect(page) {
        if (page == Page.Search) { searchFocus.requestFocus(); keyboard?.show() }
    }
    // A launcher never finishes on back; back always steps toward Home.
    BackHandler { goHome() }

    val backdrop = scheme.background.copy(alpha = when {
        shown != Page.Home -> .92f
        !dark -> .55f
        else -> .40f
    })
    Box(Modifier.fillMaxSize().background(backdrop)) {
        if (shown == Page.Home) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to scheme.background.copy(alpha = .28f), .30f to scheme.background.copy(alpha = .20f),
            .55f to Color.Transparent, .72f to Color.Transparent, 1f to scheme.background.copy(alpha = .38f))))
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = 20.dp)) {
            if (shown != Page.Home) Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = ::goHome) { HomeGlyph(Glyph.Back, stringResource(R.string.home)) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { open(Page.Flow) }) { HomeGlyph(Glyph.Flow, stringResource(R.string.flow)) }
                IconButton(onClick = { settingsOpen = true; focus.clearFocus(); keyboard?.hide() }) {
                    HomeGlyph(Glyph.Settings, stringResource(R.string.settings))
                }
            }
            Box(Modifier.weight(1f)) {
                Crossfade(shown, animationSpec = tween(if (settings.reduceMotion) 0 else 160), label = "morph") { target ->
                    when (target) {
                        Page.Home -> Box(Modifier.fillMaxSize()) {
                            ClockHome(settings.showClock, settings.showDate, onSwipeUp = { open(Page.Search) },
                                onLongPress = { settingsOpen = true })
                            Lens(arc, settings.reach, settings.reduceMotion, onSearch = { open(Page.Search) }, onLaunch = ::launch,
                                modifier = Modifier.align(Alignment.BottomCenter))
                        }
                        Page.Search -> SearchPage(query, visible, results, flows, model::entry, ::launch, ::run,
                            onHold = { options = it; focus.clearFocus(); keyboard?.hide() })
                        Page.Flow -> FlowLibrary(settings.aliases, model::entry, onCreate = { flowEdit = FlowEdit(null) },
                            onEdit = { flowEdit = FlowEdit(it) }, onRun = ::run)
                    }
                }
            }
            if (shown != Page.Home) SearchField(query, searchFocus,
                onChange = { model.query.value = it; if (page != Page.Search && it.isNotBlank()) page = Page.Search },
                onSubmit = {
                    val exact = Flow.exact(settings.aliases, query)
                    when {
                        exact != null && exact.targets.size == 1 -> run(exact)
                        exact != null -> Unit // Multi-step commands always need the explicit Run button.
                        results.isNotEmpty() -> launch(results.first())
                    }
                },
                onApps = { open(Page.Search) }, onFlow = { open(Page.Flow) })
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(bottom = 72.dp))
    }

    if (settingsOpen) SettingsSheet(model, onDismiss = { settingsOpen = false })
    options?.let { entry ->
        AppOptionsSheet(entry, inArc = entry.key in settings.arc, arcFull = settings.arc.size >= 5,
            onArc = { model.toggleArc(entry); options = null },
            onHide = { model.hide(entry); options = null },
            onDetails = { options = null; if (!model.openDetails(entry)) message(R.string.launch_failed) },
            onDismiss = { options = null })
    }
    flowEdit?.let { edit ->
        FlowEditorSheet(edit.alias, visible, onSave = model::saveAlias,
            onDelete = { id -> model.deleteAlias(id); flowEdit = null }, onDismiss = { flowEdit = null })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClockHome(showClock: Boolean, showDate: Boolean, onSwipeUp: () -> Unit, onLongPress: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val now by produceState(LocalTime.now() to LocalDate.now(), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                value = LocalTime.now() to LocalDate.now()
                delay(60_000L - System.currentTimeMillis() % 60_000L)
            }
        }
    }
    var drag by remember { mutableStateOf(0f) }
    BoxWithConstraints(Modifier.fillMaxSize()
        .pointerInput(Unit) {
            detectVerticalDragGestures(onDragStart = { drag = 0f }, onVerticalDrag = { _, amount -> drag += amount },
                onDragEnd = { if (drag < -100f) onSwipeUp() })
        }
        .combinedClickable(interactionSource = null, indication = null, onLongClickLabel = stringResource(R.string.settings),
            onClick = {}, onLongClick = onLongPress)) {
        val compact = maxHeight < 280.dp
        Column(Modifier.fillMaxSize().padding(top = if (compact) 18.dp else 94.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (showClock) ClockFace(now.first, compact)
            if (showDate) Text(now.second.format(DateTimeFormatter.ofPattern("EEEE d. MMMM", Locale.getDefault())),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun SearchField(query: String, focus: FocusRequester, onChange: (String) -> Unit, onSubmit: () -> Unit,
    onApps: () -> Unit, onFlow: () -> Unit) {
    val context = LocalContext.current
    Surface(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 14.dp), shape = RoundedCornerShape(14.dp),
        color = Color.Transparent, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(query, onChange, singleLine = true,
                modifier = Modifier.weight(1f).focusRequester(focus).semantics { contentDescription = context.getString(R.string.lens_description) },
                placeholder = { Text(stringResource(R.string.search), fontSize = 14.sp) },
                leadingIcon = { HomeGlyph(Glyph.Search, null) },
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onSubmit() }))
            if (query.isNotEmpty()) IconButton(onClick = { onChange("") }) {
                Text("×", Modifier.semantics { contentDescription = context.getString(R.string.clear) }, fontSize = 24.sp)
            } else {
                IconButton(onClick = onApps) { HomeGlyph(Glyph.Apps, stringResource(R.string.apps)) }
                IconButton(onClick = onFlow) { HomeGlyph(Glyph.Flow, stringResource(R.string.flow)) }
            }
        }
    }
}

@Composable
private fun SearchPage(query: String, apps: List<AppEntry>, results: List<AppEntry>, flows: List<FlowAlias>,
    resolve: (String) -> AppEntry?, onLaunch: (AppEntry) -> Unit, onRun: (FlowAlias) -> Unit, onHold: (AppEntry) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        flows.take(3).forEach { alias ->
            FlowPreview(alias, resolve, onRun = { onRun(alias) })
            Spacer(Modifier.height(10.dp))
        }
        if (query.isBlank()) AppIndex(apps, onLaunch, onHold)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (results.isEmpty() && flows.isEmpty()) item {
                Text(stringResource(R.string.no_results), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 24.dp))
            }
            items(results, key = { it.key }) { app -> AppRow(app, onClick = { onLaunch(app) }, onLongClick = { onHold(app) }) }
        }
    }
}

@Composable
private fun FlowPreview(alias: FlowAlias, resolve: (String) -> AppEntry?, onRun: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(22.dp), color = scheme.surfaceContainer, border = BorderStroke(1.dp, scheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("FLOW", fontSize = 10.sp, letterSpacing = 2.sp, color = scheme.primary)
                Text(alias.name, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleMedium)
                FlowSequence(alias, resolve, Modifier.padding(top = 8.dp))
            }
            FilledTonalButton(onClick = onRun, shape = RoundedCornerShape(16.dp)) { Text(stringResource(R.string.flow_run)) }
        }
    }
}

@Composable
fun FlowSequence(alias: FlowAlias, resolve: (String) -> AppEntry?, modifier: Modifier = Modifier) {
    val unavailable = stringResource(R.string.flow_target_missing)
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        alias.targets.forEachIndexed { index, key ->
            if (index > 0) Text("→", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            val entry = resolve(key)
            if (entry != null) AppIcon(entry, Modifier.size(22.dp))
            else Text(unavailable, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun FlowLibrary(aliases: List<FlowAlias>, resolve: (String) -> AppEntry?, onCreate: () -> Unit,
    onEdit: (FlowAlias) -> Unit, onRun: (FlowAlias) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize()) {
        Text(stringResource(R.string.flow_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.flow_hint), Modifier.padding(top = 10.dp, bottom = 22.dp), color = scheme.onSurfaceVariant, fontSize = 14.sp)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (aliases.isEmpty()) item { Text(stringResource(R.string.flow_empty), color = scheme.onSurfaceVariant) }
            items(aliases, key = { it.id }) { alias ->
                Surface(onClick = { onEdit(alias) }, shape = RoundedCornerShape(22.dp), color = scheme.surfaceContainer) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(alias.name, style = MaterialTheme.typography.titleMedium)
                            FlowSequence(alias, resolve, Modifier.padding(top = 8.dp))
                        }
                        FilledTonalButton(onClick = { onRun(alias) }, shape = RoundedCornerShape(16.dp)) { Text(stringResource(R.string.flow_run)) }
                    }
                }
            }
        }
        FilledTonalButton(onClick = onCreate, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), shape = RoundedCornerShape(20.dp)) {
            Text(stringResource(R.string.flow_create), Modifier.padding(vertical = 6.dp))
        }
    }
}

private fun indexLetter(label: String): String =
    AppSearch.fold(label).firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() }?.toString() ?: "#"

/** Open typographic lanes: letter column, icon, label. No usage tracking. */
@Composable
private fun AppIndex(apps: List<AppEntry>, onLaunch: (AppEntry) -> Unit, onHold: (AppEntry) -> Unit) {
    val state = rememberLazyListState()
    LazyColumn(state = state, contentPadding = PaddingValues(bottom = 16.dp)) {
        itemsIndexed(apps, key = { _, app -> app.key }) { index, app ->
            val letter = indexLetter(app.label)
            val first = index == 0 || indexLetter(apps[index - 1].label) != letter
            AppRow(app, onClick = { onLaunch(app) }, onLongClick = { onHold(app) }, letter = if (first) letter else "")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(app: AppEntry, onClick: () -> Unit, onLongClick: () -> Unit, letter: String? = null) {
    Row(Modifier.fillMaxWidth().combinedClickable(role = Role.Button, onLongClickLabel = stringResource(R.string.app_options),
        onClick = onClick, onLongClick = onLongClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (letter != null) Text(letter, Modifier.width(34.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppIcon(app, Modifier.size(36.dp))
        Text(app.label, Modifier.padding(start = 16.dp).weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp,
            color = if (app.suspended) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun AppIcon(app: AppEntry, modifier: Modifier = Modifier) {
    val icons = LocalContext.current.intelia.icons
    val revision by icons.revision.collectAsStateWithLifecycle()
    val bitmap by produceState<ImageBitmap?>(icons.peek(app.key), app.key, revision) { value = icons.load(app) }
    Box(modifier, contentAlignment = Alignment.Center) {
        bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}

package dev.intelia.launcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.HomeLayout
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.ShortcutEntry
import dev.intelia.launcher.flow.Flow
import dev.intelia.launcher.flow.FlowAlias
import dev.intelia.launcher.flow.FlowError
import dev.intelia.launcher.intelia
import dev.intelia.launcher.search.AppSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class FlowRun { Launched, Unavailable, Failed }

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application.intelia
    val settings: StateFlow<LauncherSettings> = app.settings.state
    val query = MutableStateFlow("")

    val allApps: StateFlow<List<AppEntry>> = app.apps.apps.map { it.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val visibleApps: StateFlow<List<AppEntry>> = combine(allApps, settings) { apps, s -> apps.filter { it.key !in s.hidden } }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val results: StateFlow<List<AppEntry>> = combine(visibleApps, query) { apps, q -> AppSearch.rank(apps, q) { it.searchKey } }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val flowMatches: StateFlow<List<FlowAlias>> = combine(settings, query) { s, q -> Flow.suggest(s.aliases, q) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Fixed slots; a removed or hidden app leaves its slot empty instead of shifting the others. */
    val arcApps: StateFlow<List<AppEntry?>> = combine(visibleApps, settings) { apps, s ->
        val byKey = apps.associateBy { it.key }
        List(LauncherSettings.ARC_SLOTS) { i -> s.arc.getOrNull(i)?.let(byKey::get) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, List(LauncherSettings.ARC_SLOTS) { null })

    val arcFlows: StateFlow<List<FlowAlias?>> = settings.map { s ->
        val byId = s.aliases.associateBy { it.id }
        List(LauncherSettings.ARC_FLOW_SLOTS) { i -> s.arcFlows.getOrNull(i)?.let(byId::get) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, List(LauncherSettings.ARC_FLOW_SLOTS) { null })

    private val _shortcuts = MutableStateFlow<List<ShortcutEntry>>(emptyList())
    /** Shortcuts the system exposes to us; empty unless Intelia is the default home app. */
    val shortcuts: StateFlow<List<ShortcutEntry>> = _shortcuts.asStateFlow()

    val arcShortcuts: StateFlow<List<ShortcutEntry?>> = combine(shortcuts, settings) { list, s ->
        val byKey = list.associateBy { it.key }
        List(LauncherSettings.ARC_SHORTCUT_SLOTS) { i -> s.arcShortcuts.getOrNull(i)?.let(byKey::get) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, List(LauncherSettings.ARC_SHORTCUT_SLOTS) { null })

    val favoriteApps: StateFlow<List<AppEntry>> = combine(visibleApps, settings) { apps, s ->
        val byKey = apps.associateBy { it.key }
        s.favorites.mapNotNull(byKey::get)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hiddenApps: StateFlow<List<AppEntry>> = combine(allApps, settings) { apps, s -> apps.filter { it.key in s.hidden } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val byKey get() = allApps.value.associateBy { it.key }

    fun entry(key: String): AppEntry? = byKey[key]

    fun canUseShortcuts(): Boolean = app.apps.canUseShortcuts()

    /** Called when Home starts; the shortcut list changes rarely and is read off the main thread. */
    fun refreshShortcuts() {
        viewModelScope.launch { _shortcuts.value = withContext(Dispatchers.IO) { app.apps.shortcuts() } }
    }

    fun launch(entry: AppEntry): Boolean = !entry.suspended && app.apps.launch(entry)

    fun launch(shortcut: ShortcutEntry): Boolean = app.apps.startShortcut(shortcut)

    fun openDetails(entry: AppEntry): Boolean = app.apps.openDetails(entry)

    private var running = false

    /** Resolves the whole sequence first and dispatches it in one UI turn; no timers or background work. */
    fun runFlow(alias: FlowAlias): FlowRun {
        if (running) return FlowRun.Failed
        running = true
        try {
            val current = byKey
            val targets = Flow.resolve(alias) { key -> current[key]?.takeIf { !it.suspended } } ?: return FlowRun.Unavailable
            return if (targets.all(app.apps::launch)) FlowRun.Launched else FlowRun.Failed
        } finally {
            running = false
        }
    }

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) = app.settings.update(transform)

    /** Adds to the first empty slot so the other slots keep their positions. */
    private fun <T> placeInSlot(list: List<T>, item: T, capacity: Int): List<T> = if (item in list || list.size >= capacity) list else list + item

    fun toggleArc(entry: AppEntry) = updateSettings { s ->
        if (entry.key in s.arc) s.copy(arc = s.arc - entry.key) else s.copy(arc = placeInSlot(s.arc, entry.key, LauncherSettings.ARC_SLOTS))
    }

    fun toggleArcFlow(id: String, on: Boolean) = updateSettings { s ->
        s.copy(arcFlows = if (on) placeInSlot(s.arcFlows, id, LauncherSettings.ARC_FLOW_SLOTS) else s.arcFlows - id)
    }

    fun toggleArcShortcut(key: String) = updateSettings { s ->
        s.copy(arcShortcuts = if (key in s.arcShortcuts) s.arcShortcuts - key
        else placeInSlot(s.arcShortcuts, key, LauncherSettings.ARC_SHORTCUT_SLOTS))
    }

    fun toggleFavorite(entry: AppEntry) = updateSettings { s ->
        s.copy(favorites = if (entry.key in s.favorites) s.favorites - entry.key
        else placeInSlot(s.favorites, entry.key, LauncherSettings.MAX_FAVORITES))
    }

    fun moveFavorite(key: String, delta: Int) = updateSettings { s ->
        val i = s.favorites.indexOf(key)
        val j = i + delta
        if (i < 0 || j !in s.favorites.indices) s
        else s.copy(favorites = s.favorites.toMutableList().apply { add(j, removeAt(i)) })
    }

    fun hide(entry: AppEntry) = updateSettings { it.copy(hidden = it.hidden + entry.key) }
    fun unhide(entry: AppEntry) = updateSettings { it.copy(hidden = it.hidden - entry.key) }

    /** Finishes onboarding; Essentials starts with the default dialer, messages, camera and browser. */
    fun finishOnboarding(layout: HomeLayout) {
        val apps = visibleApps.value
        val essentials = app.apps.essentialPackages().mapNotNull { pkg -> apps.firstOrNull { it.mainProfile && it.component.packageName == pkg }?.key }
        updateSettings { s ->
            s.copy(onboarded = true, layout = layout, favorites = s.favorites.ifEmpty { essentials })
        }
    }

    fun saveAlias(alias: FlowAlias, inArc: Boolean): FlowError? {
        val current = settings.value.aliases
        Flow.validate(alias, current)?.let { return it }
        updateSettings { s ->
            val exists = s.aliases.any { it.id == alias.id }
            val aliases = if (exists) s.aliases.map { if (it.id == alias.id) alias else it } else s.aliases + alias
            val arcFlows = if (inArc) placeInSlot(s.arcFlows, alias.id, LauncherSettings.ARC_FLOW_SLOTS) else s.arcFlows - alias.id
            s.copy(aliases = aliases, arcFlows = arcFlows)
        }
        return null
    }

    fun deleteAlias(id: String) = updateSettings { s -> s.copy(aliases = s.aliases.filterNot { it.id == id }, arcFlows = s.arcFlows - id) }
}

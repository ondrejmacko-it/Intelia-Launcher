package dev.intelia.launcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.flow.Flow
import dev.intelia.launcher.flow.FlowAlias
import dev.intelia.launcher.flow.FlowError
import dev.intelia.launcher.intelia
import dev.intelia.launcher.search.AppSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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
        s.arc.map { byKey[it] }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hiddenApps: StateFlow<List<AppEntry>> = combine(allApps, settings) { apps, s -> apps.filter { it.key in s.hidden } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val byKey get() = allApps.value.associateBy { it.key }

    fun entry(key: String): AppEntry? = byKey[key]

    fun launch(entry: AppEntry): Boolean = !entry.suspended && app.apps.launch(entry)

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

    fun toggleArc(entry: AppEntry) = updateSettings { s ->
        if (entry.key in s.arc) s.copy(arc = s.arc - entry.key)
        else if (s.arc.size < LauncherSettings.ARC_SLOTS) s.copy(arc = s.arc + entry.key) else s
    }

    fun hide(entry: AppEntry) = updateSettings { it.copy(hidden = it.hidden + entry.key) }
    fun unhide(entry: AppEntry) = updateSettings { it.copy(hidden = it.hidden - entry.key) }

    fun saveAlias(alias: FlowAlias): FlowError? {
        val current = settings.value.aliases
        Flow.validate(alias, current)?.let { return it }
        updateSettings { s ->
            val exists = s.aliases.any { it.id == alias.id }
            s.copy(aliases = if (exists) s.aliases.map { if (it.id == alias.id) alias else it } else s.aliases + alias)
        }
        return null
    }

    fun deleteAlias(id: String) = updateSettings { s -> s.copy(aliases = s.aliases.filterNot { it.id == id }) }
}

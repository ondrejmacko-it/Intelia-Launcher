package dev.intelia.launcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.intelia.launcher.action.Action
import dev.intelia.launcher.action.ArcLook
import dev.intelia.launcher.action.ArcSet
import dev.intelia.launcher.action.Controls
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.ShortcutEntry
import dev.intelia.launcher.flow.Flow
import dev.intelia.launcher.flow.FlowAlias
import dev.intelia.launcher.flow.FlowError
import dev.intelia.launcher.intelia
import dev.intelia.launcher.onboarding.ControlTab
import dev.intelia.launcher.onboarding.DraftCodec
import dev.intelia.launcher.onboarding.GuideStep
import dev.intelia.launcher.onboarding.OnboardingDraft
import dev.intelia.launcher.pulse.MediaWatch
import dev.intelia.launcher.search.AppSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    private val _shortcuts = MutableStateFlow<List<ShortcutEntry>>(emptyList())
    /** Shortcuts the system exposes to us; empty unless Intelia is the default home app. */
    val shortcuts: StateFlow<List<ShortcutEntry>> = _shortcuts.asStateFlow()

    /**
     * ARC from `settings.controls` (M2): every set, each slot resolved on this device. A removed or hidden app, a
     * deleted FLOW or a shortcut the system no longer offers leaves its slot empty instead of shifting the others.
     */
    val arc: StateFlow<ArcContent> = combine(visibleApps, shortcuts, settings) { apps, list, s ->
        ArcContent.of(s.controls, apps, s.aliases, list)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, ArcContent(emptyList(), ArcLook()))

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

    /**
     * Every ARC edit goes through the Control System: the first one writes `controlConfig` (migrated from the legacy
     * lists, which stay untouched), later ones edit it.
     */
    fun editControls(transform: (Controls) -> Controls) = updateSettings { s -> s.copy(controlConfig = transform(s.controls)) }

    fun toggleArc(entry: AppEntry) = editControls { c -> Action.App(entry.key).let { c.toggleInArc(it, !c.inArc(it)) } }

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

    /**
     * The guide's draft (ONBOARDING_V2 §7): its own file in `noBackupFilesDir`, apart from the settings document, so a
     * permission round-trip or process death resumes it and a backup never carries it. Null = no guide open.
     */
    private val guideFile = File(application.noBackupFilesDir, "onboarding-draft.json")
    private val _guide = MutableStateFlow(DraftCodec.decode(guideFile.takeIf { it.isFile }?.let { runCatching { it.readText() }.getOrNull() }))
    val guide: StateFlow<OnboardingDraft?> = _guide.asStateFlow()

    init {
        // The last draft wins; a write replaces the file in one rename, so a kill mid-write keeps the previous one.
        viewModelScope.launch(Dispatchers.IO) {
            _guide.drop(1).collectLatest { draft ->
                runCatching {
                    if (draft == null) guideFile.delete()
                    else File(guideFile.path + ".tmp").apply { writeText(DraftCodec.encode(draft)) }.renameTo(guideFile)
                }
            }
        }
    }

    /** The phone's dialer, messages, camera and browser, as app keys: what a new Home starts with around LENS. */
    fun essentialKeys(): List<String> {
        val apps = visibleApps.value
        return app.apps.essentialPackages().mapNotNull { pkg -> apps.firstOrNull { it.mainProfile && it.component.packageName == pkg }?.key }
    }

    /** First run ([rerun] false) starts from the signature Home; Studio's rerun from the current settings. */
    fun beginGuide(rerun: Boolean) {
        _guide.value = if (rerun) OnboardingDraft.rerun(settings.value) else OnboardingDraft.firstRun(settings.value, essentialKeys())
    }

    /** Discovery Hub › Vyzkoušet: the guide's sandbox on [tab], from the current settings (a rerun keeps them as they are). */
    fun tryInGuide(tab: ControlTab) {
        beginGuide(rerun = true)
        updateGuide { it.goTo(GuideStep.Controls).copy(tab = tab) }
    }

    /** Set by the guide's "Prozkoumat Intelii": Home opens Studio once, after the commit. */
    val explore = MutableStateFlow(false)

    fun updateGuide(transform: (OnboardingDraft) -> OnboardingDraft) { _guide.value = _guide.value?.let(transform) }

    /** Done: the draft becomes the stored settings in one sanitized update, then the draft is gone. */
    fun finishGuide() {
        val draft = _guide.value ?: return
        val music = MediaWatch.allowed(getApplication())
        updateSettings { draft.commit(music) }
        _guide.value = null
    }

    /** "Použít výchozí" / skip all: a first run keeps the default Home; a rerun changes nothing. */
    fun skipGuide() {
        val draft = _guide.value ?: return
        if (!draft.rerun) updateSettings { draft.start.copy(onboarded = true) }
        _guide.value = null
    }

    fun saveAlias(alias: FlowAlias, inArc: Boolean): FlowError? {
        val current = settings.value.aliases
        Flow.validate(alias, current)?.let { return it }
        updateSettings { s ->
            val exists = s.aliases.any { it.id == alias.id }
            val aliases = if (exists) s.aliases.map { if (it.id == alias.id) alias else it } else s.aliases + alias
            s.copy(aliases = aliases, controlConfig = s.controls.toggleInArc(Action.Flow(alias.id), inArc))
        }
        return null
    }

    fun deleteAlias(id: String) = updateSettings { s ->
        s.copy(aliases = s.aliases.filterNot { it.id == id }, controlConfig = s.controls.toggleInArc(Action.Flow(id), on = false))
    }
}

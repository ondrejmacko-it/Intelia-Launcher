package dev.intelia.launcher.ui

import dev.intelia.launcher.search.AppSearch

/**
 * The pages of Settings (Studio, Veil 3.0): five areas on the main page and the pages inside them. [parent] is where
 * Back goes; null = the main page.
 */
enum class StudioSection(val parent: StudioSection? = null) {
    Look, Home, Controls, Actions, System,
    Favorites(Home), Pulse(Controls), Arc(Controls), Gestures(Controls), Motion(Controls), Backup(System);

    /** 1 for an area, 2 for a page inside one; drives the slide direction. */
    val depth: Int get() = if (parent == null) 1 else 2
}

/**
 * Every row a Settings search can lead to; the page opens, scrolls to the row and lights it briefly. A target names
 * one row, and each setting has exactly one row, so a search never offers the same thing twice.
 */
enum class StudioTarget(val section: StudioSection) {
    Theme(StudioSection.Look), Colors(StudioSection.Look), Material(StudioSection.Look), Wallpaper(StudioSection.Look),
    TextSize(StudioSection.Look), Icons(StudioSection.Look),
    EditHome(StudioSection.Home), Grid(StudioSection.Home), Placement(StudioSection.Home), Favorites(StudioSection.Favorites),
    Pulse(StudioSection.Pulse), PulseLook(StudioSection.Pulse), PulseEvents(StudioSection.Pulse), PulseMusic(StudioSection.Pulse),
    PulseLens(StudioSection.Pulse),
    Arc(StudioSection.Arc), GlobalArc(StudioSection.Arc), Shortcuts(StudioSection.Arc),
    Gestures(StudioSection.Gestures), Keys(StudioSection.Gestures),
    Motion(StudioSection.Motion), Haptics(StudioSection.Motion),
    Flows(StudioSection.Actions),
    DefaultHome(StudioSection.System), Permissions(StudioSection.System), Hidden(StudioSection.System), Guide(StudioSection.System), Backup(StudioSection.Backup),
    About(StudioSection.System);

    /** The row's anchor on its page. */
    val anchor: String get() = name
}

/** One searchable entry: what it is called, where it lives ("Vzhled › Tapeta") and words people may type instead. */
data class StudioEntry(val target: StudioTarget, val label: String, val path: String, val keywords: String)

object StudioIndex {
    /**
     * Entries for [query], best first, diacritics-insensitive like app search; a hit on the name ranks above a hit on
     * the path or keywords. Nothing opens while typing: the caller only lists them.
     */
    fun search(entries: List<StudioEntry>, query: String): List<StudioEntry> {
        val q = AppSearch.fold(query)
        if (q.isEmpty()) return emptyList()
        return entries.mapNotNull { e ->
            val name = AppSearch.score(AppSearch.fold(e.label), q)
            val rest = AppSearch.score(AppSearch.fold(e.path + " " + e.keywords), q)
            (name ?: rest?.plus(10))?.let { it to e }
        }.sortedBy { it.first }.map { it.second }
    }
}

package dev.intelia.launcher.ui

import dev.intelia.launcher.search.AppSearch

/**
 * The pages of Settings (Studio), in the order of its main page. [Home] has no page of its own: it opens the editor.
 */
enum class StudioSection { Look, Icons, Home, Favorites, Arc, Flow, Pulse, Gestures, Motion, Backup, System, About }

/**
 * Every place a Settings search can lead to: a page, or a part of one opened there. Each setting lives in exactly
 * one place, so a search never offers the same thing twice.
 */
enum class StudioTarget(val section: StudioSection) {
    Material(StudioSection.Look), Wallpaper(StudioSection.Look), Theme(StudioSection.Look), Colors(StudioSection.Look),
    Icons(StudioSection.Icons),
    EditHome(StudioSection.Home), Grid(StudioSection.Home),
    Favorites(StudioSection.Favorites),
    Arc(StudioSection.Arc), Shortcuts(StudioSection.Arc),
    Flows(StudioSection.Flow),
    Pulse(StudioSection.Pulse),
    Gestures(StudioSection.Gestures),
    Motion(StudioSection.Motion),
    Backup(StudioSection.Backup),
    Hidden(StudioSection.System), DefaultHome(StudioSection.System),
    About(StudioSection.About),
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

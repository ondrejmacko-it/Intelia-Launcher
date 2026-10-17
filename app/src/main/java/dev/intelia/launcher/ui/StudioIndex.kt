package dev.intelia.launcher.ui

import dev.intelia.launcher.search.AppSearch

/** Studio's five sections (Veil 3.0 B4). [Home] has no page of its own: it opens the editor on Home. */
enum class StudioSection { Look, Home, Controls, Flow, System }

/**
 * Every place a Studio search can lead to: a part of a section page, opened there. The same keys name the folds on
 * the pages, so the index and the pages cannot drift apart.
 */
enum class StudioTarget(val section: StudioSection) {
    Icons(StudioSection.Look), Material(StudioSection.Look), Wallpaper(StudioSection.Look), Theme(StudioSection.Look),
    Colors(StudioSection.Look),
    EditHome(StudioSection.Home),
    Arc(StudioSection.Controls), Pulse(StudioSection.Controls), Motion(StudioSection.Controls),
    Flows(StudioSection.Flow), Shortcuts(StudioSection.Flow),
    Hidden(StudioSection.System), DefaultHome(StudioSection.System), About(StudioSection.System),
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

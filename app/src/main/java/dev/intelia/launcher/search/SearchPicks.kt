package dev.intelia.launcher.search

import dev.intelia.launcher.flow.FlowAlias

/** One row of the Search card (DESIGN_SYSTEM §5.6): the first one is what Enter does. */
sealed interface SearchPick<out A> {
    /** The query is exactly this FLOW's name: Enter (or Run) runs it. */
    data class Run(val alias: FlowAlias) : SearchPick<Nothing>
    /** A FLOW whose name starts with the query: Enter only completes the name, it never runs. */
    data class Complete(val alias: FlowAlias) : SearchPick<Nothing>
    data class App<A>(val app: A) : SearchPick<A>
}

object SearchPicks {
    const val MAX = 6

    /**
     * Rows for the Search card, nearest the field first. A blank query lists [favorites] and nothing is
     * highlighted (Enter does nothing); otherwise the exact FLOW, then apps, then other FLOW names.
     */
    fun <A> of(query: String, apps: List<A>, flows: List<FlowAlias>, exact: FlowAlias?, favorites: List<A>, max: Int = MAX): List<SearchPick<A>> {
        if (query.isBlank()) return favorites.take(max).map { SearchPick.App(it) }
        val rows = ArrayList<SearchPick<A>>(max)
        if (exact != null) rows += SearchPick.Run(exact)
        apps.forEach { if (rows.size < max) rows += SearchPick.App(it) }
        flows.forEach { if (rows.size < max && it != exact) rows += SearchPick.Complete(it) }
        return rows
    }

    /** What Enter acts on: the first row, but never on a blank query. */
    fun <A> enter(query: String, rows: List<SearchPick<A>>): SearchPick<A>? = if (query.isBlank()) null else rows.firstOrNull()
}

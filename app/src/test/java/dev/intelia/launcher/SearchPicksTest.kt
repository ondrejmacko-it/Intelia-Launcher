package dev.intelia.launcher

import dev.intelia.launcher.flow.FlowAlias
import dev.intelia.launcher.search.SearchPick
import dev.intelia.launcher.search.SearchPicks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchPicksTest {
    private val ranní = FlowAlias("a", "ranní", listOf("x"))
    private val ranníBěh = FlowAlias("b", "ranní běh", listOf("y"))

    @Test fun blankQueryListsFavouritesAndEnterDoesNothing() {
        val rows = SearchPicks.of("  ", listOf("Mapy"), listOf(ranní), null, favorites = (1..9).map { "f$it" })
        assertEquals(SearchPicks.MAX, rows.size)
        assertEquals(SearchPick.App("f1"), rows.first())
        assertNull(SearchPicks.enter("  ", rows))
    }

    @Test fun exactFlowComesFirstAndIsNotRepeated() {
        val rows = SearchPicks.of("ranní", listOf("Rádio"), listOf(ranní, ranníBěh), ranní, favorites = emptyList())
        assertEquals(listOf(SearchPick.Run(ranní), SearchPick.App("Rádio"), SearchPick.Complete(ranníBěh)), rows)
        assertEquals(SearchPick.Run(ranní), SearchPicks.enter("ranní", rows))
    }

    @Test fun capAtSixWithAppsBeforeFlowNames() {
        val rows = SearchPicks.of("r", (1..8).map { "a$it" }, listOf(ranní), null, favorites = emptyList())
        assertEquals(6, rows.size)
        assertEquals(true, rows.all { it is SearchPick.App })
    }

    @Test fun onlyAFlowNameCompletesNeverRuns() {
        val rows = SearchPicks.of("ran", emptyList<String>(), listOf(ranní), null, favorites = emptyList())
        assertEquals(SearchPick.Complete(ranní), SearchPicks.enter("ran", rows))
    }
}

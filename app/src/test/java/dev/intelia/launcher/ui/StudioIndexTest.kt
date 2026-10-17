package dev.intelia.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioIndexTest {
    private val entries = listOf(
        StudioEntry(StudioTarget.Icons, "Ikony", "Vzhled › Ikony", "tvar styl velikost balíček"),
        StudioEntry(StudioTarget.Wallpaper, "Tapeta", "Vzhled › Tapeta", "ztmavení pozadí systémová"),
        StudioEntry(StudioTarget.Motion, "Pohyb a dotek", "Ovládání", "animace omezit pohyb haptika"),
        StudioEntry(StudioTarget.Arc, "ARC", "Ovládání", "kruhy prstence podržení"),
    )

    @Test fun diacriticsDoNotMatter() {
        assertEquals(StudioTarget.Wallpaper, StudioIndex.search(entries, "ztmaveni").single().target)
        assertEquals(StudioTarget.Icons, StudioIndex.search(entries, "BALICEK").single().target)
    }

    @Test fun aNameHitRanksAboveAKeywordHit() {
        // "pohyb" is the name of Motion and a keyword too; nothing else matches.
        assertEquals(listOf(StudioTarget.Motion), StudioIndex.search(entries, "pohyb").map { it.target })
        val vzhled = StudioIndex.search(entries, "ikony").map { it.target }
        assertEquals(StudioTarget.Icons, vzhled.first())
    }

    @Test fun thePathFindsAWholeSection() {
        assertEquals(setOf(StudioTarget.Motion, StudioTarget.Arc), StudioIndex.search(entries, "ovladani").map { it.target }.toSet())
    }

    @Test fun emptyOrUnknownFindsNothing() {
        assertTrue(StudioIndex.search(entries, "  ").isEmpty())
        assertTrue(StudioIndex.search(entries, "xyz").isEmpty())
    }

    @Test fun everyTargetBelongsToASection() {
        StudioSection.entries.filter { it != StudioSection.Home }.forEach { s ->
            assertTrue("$s has no target", StudioTarget.entries.any { it.section == s })
        }
    }
}

package dev.intelia.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewPlanTest {
    @Test fun pagesThatChangeHomeShowHome() {
        listOf(null, StudioSection.Look, StudioSection.Home, StudioSection.Favorites).forEach {
            assertEquals(it.toString(), PreviewKind.Home, PreviewPlan.kind(it))
        }
    }

    @Test fun everyOtherPageGivesTheSheetTheRoom() {
        val home = setOf(StudioSection.Look, StudioSection.Home, StudioSection.Favorites)
        StudioSection.entries.filterNot { it in home }.forEach {
            assertEquals(it.name, PreviewKind.None, PreviewPlan.kind(it))
        }
    }
}

package dev.intelia.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewPlanTest {
    @Test fun pagesThatChangeHomeShowHome() {
        listOf(null, StudioSection.Look, StudioSection.Home, StudioSection.Favorites).forEach {
            assertEquals(it.toString(), PreviewKind.Home, PreviewPlan.kind(it))
        }
    }

    @Test fun controlPagesShowWhatTheyChange() {
        assertEquals(PreviewKind.Arc, PreviewPlan.kind(StudioSection.Arc))
        assertEquals(PreviewKind.Pulse, PreviewPlan.kind(StudioSection.Pulse))
        assertEquals(PreviewKind.Lens, PreviewPlan.kind(StudioSection.Gestures))
        assertEquals(PreviewKind.Search, PreviewPlan.kind(StudioSection.Actions))
    }

    @Test fun everyOtherPageGivesTheSheetTheRoom() {
        val shown = setOf(StudioSection.Look, StudioSection.Home, StudioSection.Favorites, StudioSection.Arc, StudioSection.Pulse,
            StudioSection.Gestures, StudioSection.Actions)
        StudioSection.entries.filterNot { it in shown }.forEach {
            assertEquals(it.name, PreviewKind.None, PreviewPlan.kind(it))
        }
    }

    @Test fun stripsShowThePartOfTheScreenTheyAreAbout() {
        val h = 900f
        val lens = 820f
        assertEquals(0f, PreviewPlan.stripTop(PreviewKind.Home, h, lens, 200f, true))
        assertEquals(lens - PreviewPlan.LENS_STRIP, PreviewPlan.stripTop(PreviewKind.Lens, h, lens, 200f, true))
        assertEquals(lens - 200f - PreviewPlan.ARC_HEAD, PreviewPlan.stripTop(PreviewKind.Arc, h, lens, 200f, true))
        assertEquals(h - PreviewPlan.SEARCH_STRIP, PreviewPlan.stripTop(PreviewKind.Search, h, lens, 200f, true))
    }

    @Test fun aSideDockOrATinyScreenShowsMoreNeverLess() {
        // LENS on a side edge: the arc opens sideways, so the whole screen is shown.
        assertEquals(0f, PreviewPlan.stripTop(PreviewKind.Arc, 900f, 450f, 200f, false))
        assertEquals(0f, PreviewPlan.stripTop(PreviewKind.Lens, 900f, 450f, 200f, false))
        // A huge arc never pushes the top above the screen; a strip is never shorter than MIN_STRIP.
        assertEquals(0f, PreviewPlan.stripTop(PreviewKind.Arc, 900f, 820f, 2000f, true))
        assertEquals(900f - PreviewPlan.MIN_STRIP, PreviewPlan.stripTop(PreviewKind.Lens, 900f, 2000f, 0f, true))
        assertEquals(0f, PreviewPlan.stripTop(PreviewKind.Search, 300f, 280f, 0f, true))
    }

    @Test fun sampleQueryPrefersAFlowNameThenAnAppStart() {
        assertEquals("pocty", PreviewPlan.sampleQuery(listOf(" ", "pocty "), listOf("Kalkulačka")))
        assertEquals("Kal", PreviewPlan.sampleQuery(emptyList(), listOf("X", "Kalkulačka")))
        assertEquals("Fo", PreviewPlan.sampleQuery(emptyList(), listOf("Fo")))
        assertEquals("", PreviewPlan.sampleQuery(emptyList(), emptyList()))
    }
}

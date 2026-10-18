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
        assertEquals(PreviewKind.GlobalArc, PreviewPlan.kind(StudioSection.Arc, globalArc = true))
        // The tab only matters on the ARC page.
        assertEquals(PreviewKind.Pulse, PreviewPlan.kind(StudioSection.Pulse, globalArc = true))
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
        assertEquals(h - PreviewPlan.SEARCH_STRIP - PreviewPlan.SEARCH_HEAD, PreviewPlan.stripTop(PreviewKind.Search, h, lens, 200f, true))
    }

    @Test fun searchStripFollowsTheMeasuredCard() {
        // A short card (no FLOW, few results) shows a short strip; a tall one more, never above the screen.
        assertEquals(600f - PreviewPlan.SEARCH_HEAD, PreviewPlan.stripTop(PreviewKind.Search, 900f, 820f, 0f, true, searchTop = 600f))
        assertEquals(300f - PreviewPlan.SEARCH_HEAD, PreviewPlan.stripTop(PreviewKind.Search, 900f, 820f, 0f, true, searchTop = 300f))
        assertEquals(0f, PreviewPlan.stripTop(PreviewKind.Search, 900f, 820f, 0f, true, searchTop = 5f))
        assertEquals(900f - PreviewPlan.MIN_STRIP, PreviewPlan.stripTop(PreviewKind.Search, 900f, 820f, 0f, true, searchTop = 880f))
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

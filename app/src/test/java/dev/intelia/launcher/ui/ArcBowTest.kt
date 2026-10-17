package dev.intelia.launcher.ui

import kotlin.math.abs
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArcBowTest {
    private val full = List(8) { true }
    private val bow = ArcBow(5, 120f, 120f, left = 205f, right = 205f, sets = 3)

    @Test fun firstSlotSitsStraightAboveLensAndTheArcIsFlat() {
        val (x, y) = bow.position(0)
        assertEquals(0f, x, .01f)
        assertEquals(-bow.lift, y, .01f)
        assertTrue(bow.lift < 100f)
        assertTrue(bow.position(1).first < 0f && bow.position(2).first > 0f)
        // Not a half circle: the ends come down, but stay above the LENS row and well beside it.
        val (ex, ey) = bow.position(4)
        assertTrue(ey > y && ey < 0f && abs(ex) > 100f)
        assertTrue(bow.curve > 2 * bow.lift)
    }

    @Test fun eightSlotsUseTheWholeWidthAt48dpOrMore() {
        val b = ArcBow(8, 120f, 120f, left = 205f, right = 205f)
        assertTrue(b.width > 150f && b.width <= 205f - 29f)
        listOf(b, ArcBow(8, 104f, 90f, 205f, 205f), ArcBow(8, 140f, 150f, 60f, 350f), ArcBow(5, 104f, 90f, 60f, 300f)).forEach { a ->
            val ps = (0 until a.count).map { a.position(it) }.sortedBy { it.first }
            ps.zipWithNext().forEach { (p, q) -> assertTrue("chord", hypot(p.first - q.first, p.second - q.second) >= 48f) }
        }
    }

    @Test fun edgeKeepsSlotsOnScreen() {
        val b = ArcBow(8, 120f, 120f, left = 60f, right = 350f)
        (0 until 8).forEach { assertTrue(b.position(it).first >= -60f + 29f && b.position(it).first <= 350f - 29f) }
    }

    @Test fun selectsNearestFilledAndKeepsPrevious() {
        val (x, y) = bow.position(2)
        assertEquals(2, bow.select(x, y, full))
        assertNull(bow.select(x, y, listOf(true, true, false, true, true)).takeIf { it == 2 })
        assertEquals(2, bow.select(x - 8f, y, full, previous = 2))
        // Straight up from LENS lands on the middle slot.
        assertEquals(0, bow.select(0f, -70f, full))
    }

    @Test fun backOnLensBelowOrBesideItPicksNothing() {
        assertNull(bow.select(0f, -20f, full))
        assertNull(bow.select(0f, 40f, full))
        assertNull(bow.select(70f, -10f, full))
        assertEquals(BowTrack(1, null, armed = true), bow.track(BowTrack(1, 3), 0f, -10f) { full })
    }

    @Test fun anArrowSwitchesSetOncePerVisit() {
        val (rx, ry) = bow.arrow(1)!!
        val (lx, ly) = bow.arrow(-1)!!
        val switched = bow.track(BowTrack(0, 2), rx, ry) { full }
        assertEquals(BowTrack(1, null, armed = false), switched)
        // Staying on the arrow does not keep cycling.
        assertEquals(1, bow.track(switched, rx + 4f, ry) { full }.set)
        // Leaving it re-arms; the left arrow goes back, wrapping around.
        val rearmed = bow.track(switched, 0f, -10f) { full }
        assertTrue(rearmed.armed)
        assertEquals(0, bow.track(rearmed, lx, ly) { full }.set)
        assertEquals(2, bow.track(BowTrack(0), lx, ly) { full }.set)
    }

    @Test fun dotsPickTheirSet() {
        val (dx, dy) = bow.dot(2)
        assertEquals(BowTrack(2, null, armed = false), bow.track(BowTrack(0, 1), dx, dy) { full })
        assertNull(ArcBow(5, 120f, 120f, 205f, 205f, sets = 1).dotAt(dx, dy))
    }

    @Test fun oneSetHasNoArrowsAndATightSideHasNone() {
        val single = ArcBow(5, 120f, 120f, 205f, 205f, sets = 1)
        assertNull(single.arrow(1))
        assertEquals(0, single.track(BowTrack(0), ArcBow.ARROW_X, ArcBow.ARROW_Y) { full }.set)
        val side = ArcBow(5, 120f, 120f, left = 40f, right = 350f, sets = 2)
        assertNull(side.arrow(-1))
        assertTrue(side.arrow(1) != null)
    }
}

package dev.intelia.launcher.ui

import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArcBowTest {
    private val full = List(5) { true }
    private val bow = ArcBow(5, 120f, 120f, left = 180f, right = 180f, sets = 3)

    @Test fun firstSlotSitsStraightAboveLens() {
        val (x, y) = bow.position(0)
        assertEquals(0f, x, .01f)
        assertEquals(-120f, y, .01f)
        assertTrue(bow.position(1).first < 0f && bow.position(2).first > 0f)
    }

    @Test fun slotsStayAtLeast48dpApartEvenWhenCrowded() {
        listOf(ArcBow(7, 104f, 90f, 180f, 180f), ArcBow(7, 120f, 120f, 60f, 300f), ArcBow(5, 104f, 90f, 60f, 300f)).forEach { b ->
            (0 until b.count - 1).forEach { i ->
                val a = b.angles[i] * Math.PI / 180
                val c = b.angles[i + 1] * Math.PI / 180
                val d = hypot(b.radius * (Math.sin(a) - Math.sin(c)), b.radius * (Math.cos(a) - Math.cos(c)))
                assertTrue("chord $d", d >= 48.0)
            }
        }
    }

    @Test fun edgeKeepsSlotsOnScreen() {
        val b = ArcBow(5, 120f, 120f, left = 60f, right = 300f)
        (0 until 5).forEach { assertTrue(b.position(it).first >= -60f + 33f) }
    }

    @Test fun selectsNearestFilledAndKeepsPrevious() {
        val (x, y) = bow.position(2)
        assertEquals(2, bow.select(x, y, full))
        assertNull(bow.select(x, y, listOf(true, true, false, true, true)).takeIf { it == 2 })
        assertEquals(2, bow.select(x - 8f, y, full, previous = 2))
    }

    @Test fun backOnLensOrBelowPicksNothing() {
        assertNull(bow.select(0f, -20f, full))
        assertNull(bow.select(0f, 40f, full))
        assertEquals(BowTrack(1, null, armed = true), bow.track(BowTrack(1, 3), 0f, -10f) { full })
    }

    @Test fun slidingPastAnEndSwitchesSetOncePerVisit() {
        val past = bow.position(4).let { (x, _) -> x } // rightmost slot (priority 4 is on the right of the first)
        val rightEnd = bow.angles.last()
        val a = (rightEnd + 30f) * Math.PI.toFloat() / 180f
        val px = 120f * kotlin.math.sin(a)
        val py = -120f * kotlin.math.cos(a)
        assertTrue(past > 0f)
        val switched = bow.track(BowTrack(0, 2), px, py) { full }
        assertEquals(BowTrack(1, null, armed = false), switched)
        // Staying past the end does not keep cycling.
        assertEquals(1, bow.track(switched, px, py) { full }.set)
        // Back on the arc re-arms; past the left end goes back, wrapping around.
        val (sx, sy) = bow.position(0)
        val rearmed = bow.track(switched, sx, sy) { full }
        assertTrue(rearmed.armed)
        val l = (bow.angles.first() - 30f) * Math.PI.toFloat() / 180f
        assertEquals(0, bow.track(rearmed, 120f * kotlin.math.sin(l), -120f * kotlin.math.cos(l)) { full }.set)
        assertEquals(2, bow.track(BowTrack(0), 120f * kotlin.math.sin(l), -120f * kotlin.math.cos(l)) { full }.set)
    }

    @Test fun dotsPickTheirSet() {
        val (dx, dy) = bow.dot(2)
        assertEquals(BowTrack(2, null, armed = false), bow.track(BowTrack(0, 1), dx, dy) { full })
        assertNull(ArcBow(5, 120f, 120f, 180f, 180f, sets = 1).dotAt(dx, dy))
    }

    @Test fun oneSetNeverSwitches() {
        val single = ArcBow(5, 120f, 120f, 180f, 180f, sets = 1)
        val l = (single.angles.first() - 30f) * Math.PI.toFloat() / 180f
        assertEquals(0, single.track(BowTrack(0), 120f * kotlin.math.sin(l), -120f * kotlin.math.cos(l)) { full }.set)
    }
}

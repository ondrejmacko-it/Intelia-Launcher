package dev.intelia.launcher.ui

import dev.intelia.launcher.action.Gesture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LensFlickTest {
    @Test fun aClearSidewaysFlickIsLeftOrRight() {
        assertEquals(Gesture.SwipeLeft, lensSideFlick(-80f, 10f))
        assertEquals(Gesture.SwipeRight, lensSideFlick(80f, -20f))
    }

    @Test fun shortOrSlantedMovesAreNoFlick() {
        assertNull(lensSideFlick(40f, 0f))
        assertNull(lensSideFlick(80f, -60f))
        assertNull(lensSideFlick(0f, 0f))
    }

    /** An upward flick (drawer) is never also a side flick, so a bound flick cannot steal the drawer. */
    @Test fun noMoveIsBothDrawerAndFlick() {
        for (x in -200..200 step 4) for (y in -200..200 step 4) {
            val fx = x.toFloat()
            val fy = y.toFloat()
            assertFalse("$x,$y", lensSwipeSearch(fx, fy) && lensSideFlick(fx, fy) != null)
        }
    }
}

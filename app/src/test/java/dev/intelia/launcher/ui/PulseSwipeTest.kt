package dev.intelia.launcher.ui

import dev.intelia.launcher.action.Gesture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PulseSwipeTest {
    @Test fun clearSwipesAreRecognised() {
        assertEquals(Gesture.SwipeDown, pulseSwipe(5f, 60f))
        assertEquals(Gesture.SwipeLeft, pulseSwipe(-60f, 8f))
        assertEquals(Gesture.SwipeRight, pulseSwipe(60f, 20f))
    }

    @Test fun shortOrDiagonalMovesAreNoSwipe() {
        assertNull(pulseSwipe(20f, 20f))
        assertNull(pulseSwipe(50f, 50f))
        assertNull(pulseSwipe(0f, -80f))
    }

    @Test fun homeSideSwipeNeverTakesTheDrawerFlick() {
        for (x in -200..200 step 8) for (y in -400..-48 step 8) {
            val g = homeSideSwipe(x.toFloat(), y.toFloat())
            if (g != null) assert(kotlin.math.abs(y) <= kotlin.math.abs(x) * .6f) { "$x,$y" }
        }
        assertEquals(Gesture.SwipeLeft, homeSideSwipe(-120f, 10f))
        assertNull(homeSideSwipe(-120f, -100f))
    }
}

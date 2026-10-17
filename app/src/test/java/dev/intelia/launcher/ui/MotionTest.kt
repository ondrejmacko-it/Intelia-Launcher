package dev.intelia.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {
    @Test fun onlyStepsBetweenDeeperPagesSlide() {
        assertEquals(1, VeilMotion.slide(1, 2))   // Studio → Příkazy
        assertEquals(1, VeilMotion.slide(2, 3))   // Příkazy → editor
        assertEquals(-1, VeilMotion.slide(3, 2))
        assertEquals(-1, VeilMotion.slide(2, 1))  // back to the same Studio row
        assertEquals(0, VeilMotion.slide(0, 1))   // Home ↔ Studio fades
        assertEquals(0, VeilMotion.slide(2, 0))
        assertEquals(0, VeilMotion.slide(2, 2))
    }

    @Test fun easeSettlesWithASlightOvershoot() {
        val ease = VeilMotion.ease
        assertEquals(0f, ease.transform(0f), 1e-4f)
        assertEquals(1f, ease.transform(1f), 1e-4f)
        val peak = (0..100).maxOf { ease.transform(it / 100f) }
        assertTrue("overshoot $peak", peak > 1f && peak <= 1.02f)
        assertTrue("quick start", ease.transform(.3f) > .7f)
    }
}

package dev.intelia.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PulseMenuTest {
    @Test fun actionsDependOnApiAndFlash() {
        assertEquals(PulseAction.entries.toList(), pulseActions(36, torch = true))
        assertFalse(PulseAction.Torch in pulseActions(36, torch = false))
        val old = pulseActions(27, torch = true)
        assertFalse(PulseAction.Screenshot in old)
        assertFalse(PulseAction.Lock in old)
        assertTrue(PulseAction.Notifications in old && PulseAction.QuickSettings in old)
    }

    @Test fun panelsOnlyFromApi29() {
        assertEquals(listOf(PulseLink.Bluetooth, PulseLink.Sound, PulseLink.Display), pulseLinks(28))
        assertEquals(PulseLink.entries.toList(), pulseLinks(29))
    }

    @Test fun lensTargetIsCentredSquareOnPhone3() {
        // Phone (3): lens 539.4 / 68.25 px, r 21 px, pad 10 dp = 26.25 px, min 32 dp = 84 px.
        val t = lensTarget(539.4f, 68.25f, 21f, 26.25f, 84f)
        assertEquals(94, t.width)
        assertEquals(t.width, t.height)
        assertEquals(492, t.left)
        assertEquals(21, t.top)
    }

    @Test fun lensTargetKeepsMinimumAndScreenTop() {
        val t = lensTarget(100f, 10f, 4f, 2f, 84f)
        assertEquals(84, t.width)
        assertEquals(0, t.top)
    }

    @Test fun menuIsCentredAndClamped() {
        assertEquals(66f to 948f, menuLeftAndWidth(1080f, 540f, 32f, 948f))
        val (left, width) = menuLeftAndWidth(1080f, 540f, 32f, 600f)
        assertEquals(240f, left, 0.01f)
        assertEquals(600f, width, 0.01f)
        // Lens near the left edge: the menu stays on screen.
        assertEquals(32f, menuLeftAndWidth(1080f, 80f, 32f, 600f).first, 0.01f)
        assertEquals(448f, menuLeftAndWidth(1080f, 1060f, 32f, 600f).first, 0.01f)
    }

    @Test fun menuStartsBelowStatusBarAndLens() {
        // Lens ring (68 + 33 + 11) is lower than a 110 px status bar here.
        assertEquals(136f, menuTop(110f, 68f, 33f, 11f, 24f), 0.01f)
        // A lens hanging lower than the status bar pushes the sheet down.
        assertEquals(154f, menuTop(80f, 80f, 40f, 10f, 24f), 0.01f)
    }

    @Test fun ringerOnlyTogglesSoundAndVibrate() {
        assertEquals(RINGER_VIBRATE, nextRinger(RINGER_NORMAL))
        assertEquals(RINGER_NORMAL, nextRinger(RINGER_VIBRATE))
        assertEquals(RINGER_NORMAL, nextRinger(RINGER_SILENT))
    }

    @Test fun volumeFractionIsSafe() {
        assertEquals(.5f, volumeFraction(5, 10), 0.001f)
        assertEquals(0f, volumeFraction(3, 0), 0.001f)
        assertEquals(1f, volumeFraction(12, 10), 0.001f)
    }
}

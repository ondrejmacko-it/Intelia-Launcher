package dev.intelia.launcher.pulse

import dev.intelia.launcher.action.Gesture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PulseGesturesTest {
    private fun rec(vararg bound: Gesture) = PulseGestures({ it in bound })

    @Test fun unboundCapsuleTapsAtOnce() {
        val r = rec()
        assertNull(r.down(0f, 0f).timer)
        assertEquals(PulseGestures.Out(Gesture.Tap), r.up(0f, 0f))
        assertFalse(r.busy)
    }

    @Test fun tapWaitsOnlyWhileADoubleCanFollow() {
        val r = rec(Gesture.DoubleTap)
        r.down(0f, 0f)
        val up = r.up(0f, 0f)
        assertNull(up.run)
        assertEquals(300L, up.timer)
        assertEquals(Gesture.Tap, r.timeout().run)
    }

    @Test fun doubleTapAndDoubleTapHold() {
        val r = rec(Gesture.DoubleTap, Gesture.DoubleTapHold)
        r.down(0f, 0f); r.up(0f, 0f)
        assertEquals(400L, r.down(1f, 1f).timer)
        assertEquals(Gesture.DoubleTap, r.up(1f, 1f).run)

        r.down(0f, 0f); r.up(0f, 0f); r.down(0f, 0f)
        assertEquals(Gesture.DoubleTapHold, r.timeout().run)
        assertTrue(r.holding)
        assertNull(r.up(0f, 0f).run)
    }

    @Test fun onlyDoubleTapHoldBoundKeepsTwoTapsAsOneTap() {
        val r = rec(Gesture.DoubleTapHold)
        r.down(0f, 0f); r.up(0f, 0f); r.down(0f, 0f)
        assertEquals(Gesture.Tap, r.up(0f, 0f).run)
    }

    @Test fun holdOnlyWhenBoundAndNeverAfterADrag() {
        assertNull(rec().down(0f, 0f).timer)
        val r = rec(Gesture.Hold, Gesture.SwipeDown)
        assertEquals(400L, r.down(0f, 0f).timer)
        // Within the slop the timer stays, past it the hold is dropped.
        assertEquals(PulseGestures.KEEP, r.move(3f, 3f).timer)
        assertNull(r.move(0f, 20f).timer)
        assertNull(r.timeout().run)
        assertEquals(Gesture.SwipeDown, r.up(0f, 60f).run)
    }

    @Test fun holdThenReleaseRunsNothingMore() {
        val r = rec(Gesture.Hold)
        r.down(0f, 0f)
        assertEquals(Gesture.Hold, r.timeout().run)
        r.move(0f, 80f)
        assertNull(r.up(0f, 80f).run)
    }

    @Test fun unboundOrDiagonalSwipeRunsNothing() {
        val r = rec(Gesture.SwipeLeft)
        r.down(0f, 0f); r.move(0f, 50f)
        assertNull(r.up(0f, 60f).run)
        r.down(0f, 0f); r.move(40f, 40f)
        assertNull(r.up(50f, 50f).run)
        r.down(0f, 0f); r.move(-40f, 0f)
        assertEquals(Gesture.SwipeLeft, r.up(-60f, 4f).run)
    }

    @Test fun secondFingerAndCancelRunNothing() {
        val r = rec(Gesture.Hold)
        r.down(0f, 0f)
        r.down(10f, 0f) // second finger
        assertNull(r.timeout().run)
        assertNull(r.up(0f, 0f).run)
        assertFalse(r.busy)
        r.down(0f, 0f); r.cancel()
        assertNull(r.up(0f, 0f).run)
        // A cancel between the taps drops the pending tap.
        val d = rec(Gesture.DoubleTap)
        d.down(0f, 0f); d.up(0f, 0f); d.cancel()
        assertNull(d.timeout().run)
    }

    @Test fun aCancelWithoutLiftNeverSwallowsTheNextTouch() {
        val r = rec(Gesture.Hold)
        r.down(0f, 0f); r.timeout(); r.cancel() // the system took the touch, no up follows
        assertEquals(400L, r.down(0f, 0f).timer)
        assertEquals(Gesture.Hold, r.timeout().run)
    }

    @Test fun swipeDirections() {
        val r = rec()
        assertEquals(Gesture.SwipeUp, r.swipe(4f, -50f))
        assertEquals(Gesture.SwipeRight, r.swipe(50f, 10f))
        assertNull(r.swipe(20f, 20f))
    }

    @Test fun stages() {
        assertEquals(PulseStage.Hidden, pulseStage(event = false, expanded = false, arc = false))
        assertEquals(PulseStage.Compact, pulseStage(event = true, expanded = false, arc = false))
        assertEquals(PulseStage.Expanded, pulseStage(event = true, expanded = true, arc = false))
        assertEquals(PulseStage.Arc, pulseStage(event = false, expanded = true, arc = true))
    }
}

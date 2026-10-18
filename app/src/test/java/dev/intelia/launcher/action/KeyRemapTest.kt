package dev.intelia.launcher.action

import dev.intelia.launcher.action.KeyRemap.Effect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyRemapTest {
    private val up = 24
    private val down = 25
    private fun remap(vararg g: Gesture) = KeyRemap { code, gesture -> code == up && gesture in g }

    @Test fun unboundKeysAreNeverTaken() {
        val r = remap(Gesture.DoubleTap)
        assertFalse(r.takes(down))
        assertEquals(emptyList<Effect>(), r.down(down, 0))
        assertEquals(emptyList<Effect>(), r.up(down))
    }

    @Test fun singlePressWithOnlyDoubleBoundStillChangesVolume() {
        val r = remap(Gesture.DoubleTap)
        assertTrue(r.takes(up))
        r.down(up, 0)
        assertEquals(listOf(Effect.CancelHold, Effect.StartTap(up)), r.up(up))
        assertEquals(listOf(Effect.Replay(up)), r.tapTimeout(up))
    }

    @Test fun doublePressRunsDouble() {
        val r = remap(Gesture.DoubleTap)
        r.down(up, 0); r.up(up)
        r.down(up, 0)
        assertEquals(listOf(Effect.CancelHold, Effect.CancelTap, Effect.Run(up, Gesture.DoubleTap)), r.up(up))
        assertEquals(emptyList<Effect>(), r.tapTimeout(up))
    }

    @Test fun tapRunsAtOnceWithoutDouble() {
        val r = remap(Gesture.Tap)
        r.down(up, 0)
        assertEquals(listOf(Effect.CancelHold, Effect.Run(up, Gesture.Tap)), r.up(up))
    }

    @Test fun holdRunsOnceAndSwallowsTheRest() {
        val r = remap(Gesture.Hold)
        assertEquals(listOf(Effect.StartHold(up)), r.down(up, 0))
        assertEquals(listOf(Effect.Run(up, Gesture.Hold)), r.hold(up))
        assertEquals(emptyList<Effect>(), r.down(up, 1))
        assertEquals(emptyList<Effect>(), r.up(up))
        // A short press of a hold-only key keeps its own job.
        r.down(up, 0)
        assertEquals(listOf(Effect.CancelHold, Effect.Replay(up)), r.up(up))
    }

    @Test fun heldKeyWithoutHoldBindingKeepsRepeating() {
        val r = remap(Gesture.DoubleTap)
        r.down(up, 0)
        assertEquals(listOf(Effect.Replay(up)), r.down(up, 1))
    }

    @Test fun resetLeavesNothingPending() {
        val r = remap(Gesture.DoubleTap)
        r.down(up, 0); r.up(up)
        r.reset()
        assertEquals(emptyList<Effect>(), r.tapTimeout(up))
    }
}

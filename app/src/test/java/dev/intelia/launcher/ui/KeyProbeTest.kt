package dev.intelia.launcher.ui

import dev.intelia.launcher.action.Gesture
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyProbeTest {
    @Test fun pressesCountAndRepeatsMarkAHold() {
        var seen = KeyProbe.record(emptyList(), 24, 0)
        seen = KeyProbe.record(seen, 24, 0)
        assertEquals(listOf(KeyHit(24, presses = 2)), seen)
        seen = KeyProbe.record(seen, 24, 3)
        assertEquals(listOf(Gesture.Tap, Gesture.Hold), KeyProbe.gestures(seen.single()))
    }

    @Test fun newestFirstAndBounded() {
        val seen = (1..10).fold(emptyList<KeyHit>()) { s, c -> KeyProbe.record(s, c, 0) }
        assertEquals(KeyProbe.MAX_SHOWN, seen.size)
        assertEquals(10, seen.first().code)
        assertEquals(listOf(3, 10, 9), KeyProbe.record(seen.dropLast(1) + KeyHit(3), 3, 0).take(3).map { it.code })
    }
}

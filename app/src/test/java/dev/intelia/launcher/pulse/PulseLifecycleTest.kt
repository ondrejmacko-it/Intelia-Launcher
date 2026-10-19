package dev.intelia.launcher.pulse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PulseLifecycleTest {
    private val unlocked = PulseFacts(connected = true, everywhere = true, interactive = true, keyguardLocked = false)

    @Test fun lensOnlyWhenConnectedEverywhereAndUnlocked() {
        assertTrue(PulseLifecycle.wantsLens(unlocked))
        assertFalse(PulseLifecycle.wantsLens(unlocked.copy(connected = false)))
        assertFalse(PulseLifecycle.wantsLens(unlocked.copy(everywhere = false)))
        assertFalse(PulseLifecycle.wantsLens(unlocked.copy(interactive = false)))
        assertFalse(PulseLifecycle.wantsLens(unlocked.copy(keyguardLocked = true)))
    }

    /** The 2026-10-19 bug: after unlock the decision must follow the keyguard, whatever broadcasts arrived. */
    @Test fun unlockIsDecidedFromCurrentFactsNotFromEvents() {
        val lockScreen = unlocked.copy(keyguardLocked = true)
        assertEquals(PulseHealth.Locked, PulseLifecycle.health(lockScreen))
        // No USER_PRESENT ever: re-reading the facts after unlock alone brings the lens back.
        assertTrue(PulseLifecycle.wantsLens(lockScreen.copy(keyguardLocked = false)))
    }

    @Test fun healthStates() {
        assertEquals(PulseHealth.Disconnected, PulseLifecycle.health(unlocked.copy(connected = false, everywhere = false)))
        assertEquals(PulseHealth.Off, PulseLifecycle.health(unlocked.copy(everywhere = false)))
        assertEquals(PulseHealth.Locked, PulseLifecycle.health(unlocked.copy(interactive = false)))
        assertEquals(PulseHealth.Attaching, PulseLifecycle.health(unlocked))
        assertEquals(PulseHealth.Active, PulseLifecycle.health(unlocked.copy(lensAttached = true)))
        assertEquals(PulseHealth.Error, PulseLifecycle.health(unlocked.copy(lensFailed = true)))
        // An attached lens wins over a stale failure flag.
        assertEquals(PulseHealth.Active, PulseLifecycle.health(unlocked.copy(lensAttached = true, lensFailed = true)))
    }

    @Test fun backoffIsBoundedAndResets() {
        val b = Backoff(PulseLifecycle.ATTACH_RETRY_MS)
        assertEquals(250L, b.next())
        assertEquals(1_000L, b.next())
        assertEquals(4_000L, b.next())
        assertNull(b.next())
        assertNull(b.next())
        assertEquals(3, b.attempts)
        b.reset()
        assertEquals(250L, b.next())
    }

    @Test fun unlockChecksStopWithinAMinute() {
        val total = PulseLifecycle.UNLOCK_CHECKS_MS.sum()
        assertTrue(total in 30_000..60_000)
        assertTrue(PulseLifecycle.UNLOCK_CHECKS_MS.toList() == PulseLifecycle.UNLOCK_CHECKS_MS.sorted())
    }
}

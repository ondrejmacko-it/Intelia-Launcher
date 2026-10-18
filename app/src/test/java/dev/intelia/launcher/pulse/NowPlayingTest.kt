package dev.intelia.launcher.pulse

import dev.intelia.launcher.pulse.NowPlaying.Companion.ACTION_PAUSE
import dev.intelia.launcher.pulse.NowPlaying.Companion.ACTION_PLAY
import dev.intelia.launcher.pulse.NowPlaying.Companion.ACTION_SEEK_TO
import dev.intelia.launcher.pulse.NowPlaying.Companion.ACTION_SKIP_TO_NEXT
import dev.intelia.launcher.pulse.NowPlaying.Companion.STATE_PAUSED
import dev.intelia.launcher.pulse.NowPlaying.Companion.STATE_PLAYING
import dev.intelia.launcher.pulse.NowPlaying.Companion.STATE_STOPPED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingTest {
    private fun np(pkg: String = "a", state: Int = STATE_PLAYING, actions: Long = 0, duration: Long = 200_000, pos: Long = 10_000) =
        NowPlaying(pkg, "t", "a", state, actions, duration, pos, updatedAt = 1_000, speed = 1f)

    @Test fun controlsOnlyForSupportedActions() {
        val p = np(actions = ACTION_SKIP_TO_NEXT or ACTION_PAUSE)
        assertTrue(p.canNext); assertFalse(p.canPrev); assertTrue(p.canToggle); assertFalse(p.canSeek)
        // Playing with only PLAY: no pause button.
        assertFalse(np(actions = ACTION_PLAY).canToggle)
        assertTrue(np(state = STATE_PAUSED, actions = ACTION_PLAY).canToggle)
    }

    @Test fun seekNeedsActionAndDuration() {
        assertTrue(np(actions = ACTION_SEEK_TO).canSeek)
        assertFalse(np(actions = ACTION_SEEK_TO, duration = 0).canSeek)
    }

    @Test fun positionMovesOnlyWhilePlayingAndStaysInTrack() {
        assertEquals(15_000, np().positionAt(6_000))
        assertEquals(10_000, np(state = STATE_PAUSED).positionAt(6_000))
        assertEquals(200_000, np().positionAt(10_000_000))
        assertEquals(10_000, np().positionAt(0)) // a clock behind the report never moves it back
    }

    @Test fun pickPrefersPlayingThenPausedNeverStopped() {
        val paused = np("p", STATE_PAUSED)
        val playing = np("q")
        assertEquals("q", NowPlaying.pick(listOf(paused, playing))?.pkg)
        assertEquals("p", NowPlaying.pick(listOf(np("s", STATE_STOPPED), paused))?.pkg)
        assertNull(NowPlaying.pick(listOf(np("s", STATE_STOPPED))))
        assertNull(NowPlaying.pick(emptyList()))
    }

    @Test fun clockFormats() {
        assertEquals("0:00", clock(-5))
        assertEquals("3:07", clock(187_000))
        assertEquals("1:02:05", clock(3_725_000))
    }
}

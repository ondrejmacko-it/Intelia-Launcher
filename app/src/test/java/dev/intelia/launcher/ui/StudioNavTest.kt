package dev.intelia.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioNavTest {
    @Test fun backReturnsThroughTheWayYouCame() {
        // Editor → PULSE → Gesta → Zpět → PULSE → Zpět → editor (empty stack).
        var s = listOf("Pulse")
        s = StudioStack.push(s, "Gestures")
        assertEquals(listOf("Pulse", "Gestures"), s)
        s = StudioStack.pop(s)
        assertEquals(listOf("Pulse"), s)
        assertTrue(StudioStack.pop(s).isEmpty())
    }

    @Test fun quickAccessPulseGoesBackToMainNotControls() {
        val s = StudioStack.push(listOf("Main"), "Pulse")
        assertEquals(listOf("Main"), StudioStack.pop(s))
    }

    @Test fun aPageAlreadyOnTheStackIsReturnedTo() {
        val s = listOf("Main", "Controls", "Gestures", "Arc")
        assertEquals(listOf("Main", "Controls", "Gestures"), StudioStack.push(s, "Gestures"))
    }

    @Test fun savedStackDropsUnknownNames() {
        val saved = StudioStack.save(listOf("Main", "Gone", "Pulse"))
        assertEquals(listOf("Main", "Pulse"), StudioStack.restore(saved) { it != "Gone" })
        assertTrue(StudioStack.restore("") { true }.isEmpty())
    }

    @Test fun stopsAreOrderedAndLeaveRoomForRows() {
        val d = 2.625f
        for (room in listOf(1400f, 1800f, 2100f, 900f, 400f)) {
            val s = SheetStops.of(room, d)
            assertTrue("$room", s.settings <= s.balanced && s.balanced <= s.preview && s.preview <= room)
            if (room >= 1400f) assertTrue("sheet keeps 176 dp at Preview", room - s.preview >= 176f * d - .5f)
        }
    }

    @Test fun releaseSettlesNearestOrFlingsToNext() {
        val s = SheetStops(preview = 1500f, balanced = 800f)
        assertEquals(SheetStop.Balanced, s.settle(900f, 0f, 2000f))
        assertEquals(SheetStop.Settings, s.settle(300f, 0f, 2000f))
        assertEquals(SheetStop.Preview, s.settle(900f, 3000f, 2000f))
        assertEquals(SheetStop.Settings, s.settle(790f, -3000f, 2000f))
        // A fling past the last stop stays there.
        assertEquals(SheetStop.Preview, s.settle(1500f, 3000f, 2000f))
    }

    @Test fun onlyAClearHandleDragClosesThePage() {
        val s = SheetStops(preview = 1500f, balanced = 800f)
        val dismissAt = 1800f
        // Past the line, released still or moving down: close.
        assertEquals(SheetRelease.Dismiss, s.release(1900f, 0f, 2000f, dismissAt))
        assertEquals(SheetRelease.Dismiss, s.release(1900f, 5000f, 2000f, dismissAt))
        // Past the line but flung back up: stays open, back on the Preview stop.
        assertEquals(SheetRelease.Settle(SheetStop.Preview), s.release(1900f, -3000f, 2000f, dismissAt))
        // A fast downward fling from Settings only moves one stop, never closes.
        assertEquals(SheetRelease.Settle(SheetStop.Balanced), s.release(40f, 9000f, 2000f, dismissAt))
        // Short of the line: the nearest stop.
        assertEquals(SheetRelease.Settle(SheetStop.Preview), s.release(1700f, 0f, 2000f, dismissAt))
    }
}

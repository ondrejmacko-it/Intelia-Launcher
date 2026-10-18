package dev.intelia.launcher.ui

import dev.intelia.launcher.action.ArcLook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalArcGeometryTest {
    private val camera = Pt(205f, 26f)
    private val bow = GlobalArcGeometry.bow(ArcLook(), camera, 410f, 910f)
    private val full = List(ArcLook().slots) { true }

    @Test fun slotsHangBelowTheCameraAndStayOnScreen() {
        val first = GlobalArcGeometry.slot(bow, camera, 0)
        assertEquals(camera.x, first.x, .01f)
        assertEquals(camera.y + bow.lift, first.y, .01f)
        repeat(full.size) { i ->
            val p = GlobalArcGeometry.slot(bow, camera, i)
            assertTrue("slot $i below the camera", p.y > camera.y + 40f)
            assertTrue("slot $i on screen", p.x in 24f..386f)
        }
    }

    @Test fun theSlotUnderTheFingerIsPickedAndTheCameraCancels() {
        repeat(full.size) { i ->
            val p = GlobalArcGeometry.slot(bow, camera, i)
            assertEquals(i, GlobalArcGeometry.pick(bow, camera, p.x, p.y, full, null))
        }
        assertNull(GlobalArcGeometry.pick(bow, camera, camera.x, camera.y + 10f, full, null))
        val p = GlobalArcGeometry.slot(bow, camera, 1)
        assertNull(GlobalArcGeometry.pick(bow, camera, p.x, p.y, full.mapIndexed { i, f -> f && i != 1 }, null))
    }
}

package dev.intelia.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CrystalArcGeometryTest {
    @Test fun fiveSlotsSitAtFixedAnglesAndMirror() {
        assertEquals(listOf(150f, 120f, 90f, 60f, 30f), CrystalArcGeometry.slotAngles())
        assertEquals(listOf(30f, 60f, 90f, 120f, 150f), CrystalArcGeometry.slotAngles(mirrored = true))
        assertEquals(listOf(105f, 75f), CrystalArcGeometry.slotAngles(2))
        assertEquals(5, CrystalArcGeometry.slotAngles(9).size)
    }

    @Test fun pointsUseScreenCoordinates() {
        val (x, y) = CrystalArcGeometry.point(90f, 118f)
        assertEquals(0f, x, .001f)
        assertEquals(-118f, y, .001f)
        assertEquals(118f, CrystalArcGeometry.point(0f, 118f).first, .001f)
    }
}

package dev.intelia.launcher.ui

import dev.intelia.launcher.data.Anchor
import dev.intelia.launcher.data.DockEdge
import dev.intelia.launcher.data.DockSpec
import dev.intelia.launcher.data.LayoutSpec
import dev.intelia.launcher.data.LensAt
import dev.intelia.launcher.data.SettingsCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DockTest {
    private val w = 360f
    private val h = 800f
    private val top = 30f
    private val bottom = 20f

    private fun geometry(dock: DockSpec, dx: Float = 0f): HomeGeometry {
        val c = Dock.centre(dock, dx, w, h, top, bottom)
        return HomeGeometry(w, h, top, c.x, c.y, dock, bottom)
    }

    @Test fun bottomRowIsUnchanged() {
        val g = geometry(DockSpec())
        assertEquals(Pt(180f, h - bottom - Dock.INSET), Pt(g.lensX, g.lensY))
        assertEquals(listOf(72f, 118f, 242f, 288f), Anchor.LENS_SLOTS.sorted().map { g.lensSlot(it).x })
        assertEquals(g.lensY - HomeGeometry.LENS_BAND, g.areaBottom)
    }

    @Test fun sideRowRunsDownTheEdgeAndKeepsTheContentClear() {
        val g = geometry(DockSpec(DockEdge.Left))
        assertEquals(Dock.INSET, g.lensX)
        val ys = Anchor.LENS_SLOTS.sorted().map { g.lensSlot(it) }
        assertTrue(ys.all { it.x == g.lensX })
        assertEquals(listOf(g.lensY - 108f, g.lensY - 62f, g.lensY + 62f, g.lensY + 108f), ys.map { it.y })
        assertTrue(g.areaLeft > g.lensX)
        assertEquals(1, g.lensSlotAt(Pt(g.lensX + 10f, g.lensY + 60f)))
        assertNull(g.lensSlotAt(Pt(g.lensX + 60f, g.lensY + 60f)))
        val r = geometry(DockSpec(DockEdge.Right))
        assertEquals(w - Dock.INSET, r.lensX)
        assertTrue(r.areaRight < r.lensX)
    }

    @Test fun topRowPushesTheAreaDown() {
        val g = geometry(DockSpec(DockEdge.Top))
        assertEquals(top + Dock.TOP_INSET, g.lensY)
        assertEquals(g.lensY + HomeGeometry.LENS_BAND, g.areaTop)
        assertEquals(h - bottom - HomeGeometry.FOOT, g.areaBottom)
    }

    @Test fun lensFirstOrLastPutsAllFourSlotsOnOneSideInOrder() {
        val first = geometry(DockSpec(lensAt = LensAt.Start))
        val xs = Anchor.LENS_SLOTS.sorted().map { first.lensSlot(it).x }
        assertEquals(listOf(62f, 108f, 154f, 200f).map { first.lensX + it }, xs)
        assertTrue(xs.last() <= w - 24f)
        val last = geometry(DockSpec(lensAt = LensAt.End))
        val ls = Anchor.LENS_SLOTS.sorted().map { last.lensSlot(it).x }
        assertEquals(listOf(200f, 154f, 108f, 62f).map { last.lensX - it }, ls)
        assertTrue(ls.first() >= 24f)
        // The row as a whole stays centred.
        assertEquals(w / 2f, (first.lensX + xs.last()) / 2f, .01f)
    }

    @Test fun arcTurnsToFaceTheCentreAndInputMapsBack() {
        DockEdge.entries.forEach { e ->
            val up = Dock.toScreen(e, 0f, -100f)
            val c = geometry(DockSpec(e))
            // The ring's top points towards the screen centre.
            val towards = (w / 2f - c.lensX) * up.x + (h / 2f - c.lensY) * up.y
            assertTrue("$e", towards > 0f)
            val p = Dock.toLocal(e, Dock.toScreen(e, 13f, -41f).x, Dock.toScreen(e, 13f, -41f).y)
            assertTrue(abs(p.x - 13f) < 1e-4f && abs(p.y + 41f) < 1e-4f)
        }
    }

    @Test fun oldDocumentsHaveTheBottomDockAndUnknownNamesFallBack() {
        assertEquals(DockSpec(), LayoutSpec("x").dock)
        val s = SettingsCodec.decode("{\"schema\":2,\"home\":{\"active\":\"mine\",\"layouts\":[{\"id\":\"mine\",\"dock\":{\"edge\":\"Diagonal\",\"lensAt\":\"End\"}}]}}")
        assertEquals(DockSpec(DockEdge.Bottom, LensAt.End), s.home.activeLayout().dock)
    }
}

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

    @Test fun widerRowKeepsItsStepsAndAWideOneShrinksToFit() {
        val g = geometry(DockSpec(gap = 80, pitch = 70))
        assertEquals(listOf(30f, 100f, 260f, 330f), Anchor.LENS_SLOTS.sorted().map { g.lensSlot(it).x })
        // 2 × (140 + 140) = 560 dp on a 312 dp band: both steps scale by the same factor, order and symmetry stay.
        val tight = geometry(DockSpec(gap = 140, pitch = 140))
        val xs = Anchor.LENS_SLOTS.sorted().map { tight.lensSlot(it).x }
        assertEquals(xs, xs.sorted())
        assertTrue(xs.first() >= 24f && xs.last() <= w - 24f)
        assertEquals(w / 2f, (xs.first() + xs.last()) / 2f, .01f)
        // LENS first: the row is centred by its own length.
        val first = geometry(DockSpec(lensAt = LensAt.Start, gap = 64, pitch = 50))
        assertEquals(w / 2f, (first.lensX + first.lensSlot(Anchor.LENS_SLOTS.max()).x) / 2f, .01f)
    }

    @Test fun spreadFillsTheEdge() {
        val g = geometry(DockSpec(spread = true))
        val xs = Anchor.LENS_SLOTS.sorted().map { g.lensSlot(it).x }
        // 156 dp of room per side: LENS and the slots 78 dp apart, the outer ones at the 24 dp limit.
        assertEquals(listOf(24f, 102f, 258f, 336f), xs)
        val first = geometry(DockSpec(lensAt = LensAt.Start, spread = true))
        assertEquals(LENS_EDGE, first.lensX, .01f)
        val fs = Anchor.LENS_SLOTS.sorted().map { first.lensSlot(it).x }
        assertTrue(fs.zipWithNext().all { (a, b) -> b - a > 46f } && fs.last() <= w - 24f)
    }

    @Test fun spacingIsClampedOnRestore() {
        assertEquals(DockSpec(gap = DockSpec.MIN_GAP, pitch = DockSpec.MAX_PITCH), DockSpec(gap = -5, pitch = 9999).sanitized())
        val s = SettingsCodec.decode("{\"schema\":2,\"home\":{\"active\":\"mine\",\"layouts\":[{\"id\":\"mine\",\"dock\":{\"gap\":1,\"pitch\":500,\"spread\":true}}]}}")
        assertEquals(DockSpec(gap = DockSpec.MIN_GAP, pitch = DockSpec.MAX_PITCH, spread = true), s.home.activeLayout().dock)
    }
}

package dev.intelia.launcher.ui

import dev.intelia.launcher.data.Anchor
import dev.intelia.launcher.data.GridSpec
import dev.intelia.launcher.data.HomeItem
import dev.intelia.launcher.data.ItemKind
import dev.intelia.launcher.data.LayoutSpec
import dev.intelia.launcher.data.Placement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeEditorStateTest {
    // Nothing Phone (3): 411 × 911 dp, LENS 82 dp above the bottom.
    private val geo = HomeGeometry(411f, 911f, statusTop = 40f, lensX = 205.5f, lensY = 829f)
    private fun layout(placement: Placement = Placement.Grid, vararg items: HomeItem) =
        LayoutSpec("l", placement = placement, grid = GridSpec(4, 6), items = listOf(HomeItem("lens", ItemKind.Lens, Anchor.Lens)) + items)
    private fun app(id: String, a: Anchor) = HomeItem(id, target = "$id/X", anchor = a)
    private fun HomeEditorState.move(id: String, to: Pt): Boolean {
        assertTrue(startDrag(id))
        dragBy(to - pointOf(item(id)!!)!!)
        return drop()
    }

    @Test fun gridSnapsToTheNearestCell() {
        val e = HomeEditorState(layout(Placement.Grid, app("a", Anchor.cell(0, 0))), geo)
        val target = geo.cellCentre(2, 3, 4, 6)
        assertTrue(e.move("a", target + Pt(9f, -7f)))
        assertEquals(Anchor.cell(2, 3), e.item("a")!!.anchor)
        assertTrue(e.dirty)
    }

    @Test fun occupiedCellPushesTheOccupantToTheNearestFreeCell() {
        val e = HomeEditorState(layout(Placement.Grid, app("a", Anchor.cell(0, 0)), app("b", Anchor.cell(2, 2)), app("c", Anchor.cell(3, 2))), geo)
        e.startDrag("a")
        val p = e.dragBy(geo.cellCentre(2, 2, 4, 6) - e.pointOf(e.item("a")!!)!!)!!
        assertEquals("b" to Anchor.cell(1, 2), p.displaced)
        e.drop()
        assertEquals(Anchor.cell(2, 2), e.item("a")!!.anchor)
        assertEquals(Anchor.cell(1, 2), e.item("b")!!.anchor)
    }

    @Test fun lensRowSwapsAndLensZoneIsNeverCovered() {
        val e = HomeEditorState(layout(Placement.Grid, app("a", Anchor.lensRow(-1)), app("b", Anchor.cell(1, 1))), geo)
        assertTrue(e.move("b", geo.lensSlot(-1) + Pt(10f, 5f)))
        assertEquals(Anchor.lensRow(-1), e.item("b")!!.anchor)
        assertEquals(Anchor.cell(1, 1), e.item("a")!!.anchor)
        // Onto LENS itself: rejected, nothing changes.
        assertFalse(e.move("a", Pt(geo.lensX, geo.lensY - 30f)))
        assertEquals(Anchor.cell(1, 1), e.item("a")!!.anchor)
    }

    @Test fun blockedAreasRejectTheDrop() {
        val clock = Zone(60f, 100f, 350f, 260f)
        val e = HomeEditorState(layout(Placement.Free, app("a", Anchor.free(.5f, .9f))), geo, blocked = { listOf(clock) })
        assertFalse(e.move("a", Pt(200f, 180f)))
        assertFalse(e.dirty)
    }

    @Test fun freePlacementSnapsToGuides() {
        val e = HomeEditorState(layout(Placement.Free, app("a", Anchor.free(.2f, .8f)), app("b", Anchor.free(.8f, .5f))), geo)
        val bPt = e.pointOf(e.item("b")!!)!!
        e.startDrag("a")
        val p = e.dragBy(Pt(geo.width / 2 + 5f, bPt.y - 6f) - e.pointOf(e.item("a")!!)!!)!!
        assertEquals(setOf(Guide(true, geo.width / 2), Guide(false, bPt.y)), p.guides.toSet())
        e.drop()
        assertEquals(Pt(geo.width / 2, bPt.y).x, e.pointOf(e.item("a")!!)!!.x, .01f)
    }

    @Test fun hybridSnapsOnlyNearACell() {
        val e = HomeEditorState(layout(Placement.Hybrid, app("a", Anchor.cell(0, 0))), geo)
        e.move("a", geo.cellCentre(1, 1, 4, 6) + Pt(6f, 6f))
        assertEquals(Anchor.cell(1, 1), e.item("a")!!.anchor)
        e.move("a", geo.cellCentre(1, 1, 4, 6) + Pt(40f, 35f))
        assertEquals(dev.intelia.launcher.data.AnchorType.Free, e.item("a")!!.anchor.type)
    }

    @Test fun undoRedoIsBoundedToFifty() {
        val e = HomeEditorState(layout(Placement.Grid, app("a", Anchor.cell(0, 0))), geo)
        repeat(60) { i -> e.move("a", geo.cellCentre(i % 2 + 1, 0, 4, 6)) }
        var undos = 0
        while (e.canUndo) { e.undo(); undos++ }
        assertEquals(HomeEditorState.MAX_UNDO, undos)
        assertTrue(e.canRedo)
        e.redo()
        assertTrue(e.canUndo)
        // A new edit clears redo.
        e.setPlacement(Placement.Free)
        assertFalse(e.canRedo)
    }

    @Test fun cancelAndSavedBaseline() {
        val e = HomeEditorState(layout(Placement.Grid, app("a", Anchor.cell(0, 0))), geo)
        e.startDrag("a"); e.dragBy(Pt(100f, 100f)); e.cancelDrag()
        assertNull(e.drag)
        assertFalse(e.dirty)
        e.move("a", geo.cellCentre(3, 3, 4, 6))
        e.markSaved()
        assertFalse(e.dirty)
        assertFalse(e.startDrag("lens"))
    }

    @Test fun clockMovesFreelyButStaysOutOfTheLensBand() {
        val e = HomeEditorState(layout(Placement.Grid, HomeItem("clock", ItemKind.Clock, Anchor.free(.5f, 0f))), geo)
        e.startDrag("clock"); e.dragBy(Pt(0f, 2000f)); e.drop()
        assertEquals(HomeEditorState.CLOCK_MAX_FY, e.item("clock")!!.anchor.fy)
    }
}

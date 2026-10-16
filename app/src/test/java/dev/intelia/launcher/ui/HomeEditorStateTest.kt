package dev.intelia.launcher.ui

import dev.intelia.launcher.data.Anchor
import dev.intelia.launcher.data.AnchorType
import dev.intelia.launcher.data.GridSpec
import dev.intelia.launcher.data.HomeItem
import dev.intelia.launcher.data.ItemKind
import dev.intelia.launcher.data.LayoutSpec
import dev.intelia.launcher.data.Placement
import dev.intelia.launcher.data.lensDx
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

    @Test fun sliderEditsCollapseIntoOneUndoStep() {
        val e = HomeEditorState(layout(Placement.Grid, app("a", Anchor.cell(0, 0))), geo)
        for (v in 33..60) e.edit("a", "size") { it.copy(sizeDp = v) }
        assertEquals(60, e.item("a")!!.sizeDp)
        e.edit("a", "label") { it.copy(label = true) }
        e.undo()
        assertEquals(60, e.item("a")!!.sizeDp)
        assertFalse(e.item("a")!!.label)
        e.undo()
        assertEquals(0, e.item("a")!!.sizeDp)
        assertFalse(e.canUndo)
    }

    @Test fun removeTakesOnlyTheItemAndNeverLens() {
        val e = HomeEditorState(layout(Placement.Grid, app("a", Anchor.cell(0, 0))), geo)
        assertFalse(e.remove("lens"))
        assertTrue(e.remove("a"))
        assertNull(e.item("a"))
        e.undo()
        assertEquals(Anchor.cell(0, 0), e.item("a")!!.anchor)
    }

    @Test fun addAtSnapsAndFallsBackWithoutPushingAside() {
        val e = HomeEditorState(layout(Placement.Grid, app("app:b", Anchor.cell(2, 2))), geo)
        assertTrue(e.addAt(ItemKind.App, "x/X", geo.cellCentre(1, 1, 4, 6)))
        assertEquals(Anchor.cell(1, 1), e.item("app:x/X")!!.anchor)
        assertFalse(e.addAt(ItemKind.App, "x/X", geo.cellCentre(0, 0, 4, 6)))
        // On a taken cell the occupant stays and the new FLOW goes to the nearest free cell.
        assertTrue(e.addAt(ItemKind.Flow, "f1", geo.cellCentre(2, 2, 4, 6)))
        assertEquals(Anchor.cell(2, 2), e.item("app:b")!!.anchor)
        val f = e.item("flow:f1")!!
        assertEquals(ItemKind.Flow, f.kind)
        assertEquals(1, Math.abs(f.anchor.col - 2) + Math.abs(f.anchor.row - 2))
        // An empty LENS-row slot takes it directly.
        assertTrue(e.addAt(ItemKind.App, "y/Y", geo.lensSlot(1)))
        assertEquals(Anchor.lensRow(1), e.item("app:y/Y")!!.anchor)
    }

    @Test fun previewIsOutsideHistoryUntilConfirmed() {
        val start = layout(Placement.Grid, app("a", Anchor.cell(0, 0)))
        val e = HomeEditorState(start, geo)
        val pure = dev.intelia.launcher.data.HomeTemplates.preview(dev.intelia.launcher.data.LayoutBase.Pure, listOf("a/X"), start)
        e.preview(pure)
        assertEquals(pure, e.layout)
        assertFalse(e.canUndo)
        e.cancelPreview()
        assertEquals(start, e.layout)
        e.preview(pure)
        e.confirmPreview()
        assertEquals(pure, e.layout)
        assertNull(e.previewOf)
        e.undo()
        assertEquals(start, e.layout)
    }

    @Test fun dateToggle() {
        val e = HomeEditorState(layout(Placement.Grid), geo)
        e.setDate(true)
        assertTrue(e.layout.items.any { it.kind == ItemKind.Date })
        e.setDate(false)
        assertFalse(e.layout.items.any { it.kind == ItemKind.Date })
    }

    @Test fun nudgeMovesOneCellWithDropRules() {
        val e = HomeEditorState(layout(Placement.Grid, app("a", Anchor.cell(0, 0)), app("b", Anchor.cell(2, 0))), geo)
        assertTrue(e.nudge("a", 1, 0))
        assertEquals(Anchor.cell(1, 0), e.item("a")!!.anchor)
        assertTrue(e.nudge("a", 0, 1))
        assertEquals(Anchor.cell(1, 1), e.item("a")!!.anchor)
        e.undo()
        assertEquals(Anchor.cell(1, 0), e.item("a")!!.anchor)
        assertFalse(e.nudge("lens", 1, 0))
    }

    @Test fun nudgeOnLensRowGoesToTheNextSlotOrUp() {
        val e = HomeEditorState(layout(Placement.Grid, app("a", Anchor.lensRow(-2)), app("b", Anchor.lensRow(-1))), geo)
        assertTrue(e.nudge("a", 1, 0))
        assertEquals(Anchor.lensRow(-1), e.item("a")!!.anchor)
        assertEquals(Anchor.lensRow(-2), e.item("b")!!.anchor)
        assertTrue(e.nudge("b", 0, -1))
        assertEquals(AnchorType.Cell, e.item("b")!!.anchor.type)
        assertEquals(5, e.item("b")!!.anchor.row)
    }

    private fun withText(vararg items: HomeItem) = layout(Placement.Grid,
        HomeItem("clock", ItemKind.Clock, Anchor.free(.3f, .1f)), HomeItem("date", ItemKind.Date, Anchor.Bound), *items)

    @Test fun dateUnpinsBelowTheClockMovesOnItsOwnAndPinsBack() {
        val e = HomeEditorState(withText(), geo)
        e.pinDate(false)
        val free = e.item("date")!!.anchor
        assertEquals(AnchorType.Free, free.type)
        assertEquals(.3f, free.fx, .001f)
        assertEquals(.1f + HomeEditorState.DATE_BELOW, free.fy, .001f)
        assertTrue(e.canDrag(e.item("date")!!))
        assertTrue(e.move("date", geo.freePoint(.7f, .4f)))
        assertEquals(.7f, e.item("date")!!.anchor.fx, .01f)
        assertEquals(.3f, e.item("clock")!!.anchor.fx, .001f)
        e.pinDate(true)
        assertEquals(Anchor.Bound, e.item("date")!!.anchor)
        assertFalse(e.canDrag(e.item("date")!!))
    }

    @Test fun clockAndDateCanBeHiddenAndAddedBackOnce() {
        val e = HomeEditorState(withText(), geo)
        assertTrue(e.remove("clock"))
        assertTrue(e.remove("date"))
        assertNull(e.item("clock"))
        assertFalse(e.remove("lens"))
        assertTrue(e.addText(ItemKind.Clock, geo.freePoint(.5f, .2f)))
        assertFalse(e.addText(ItemKind.Clock, geo.freePoint(.5f, .2f)))
        assertTrue(e.addText(ItemKind.Date, geo.freePoint(.5f, .9f)))
        // Text never lands in the LENS band.
        assertTrue(e.item("date")!!.anchor.fy <= HomeEditorState.CLOCK_MAX_FY)
        e.undo(); e.undo()
        assertNull(e.item("clock"))
    }

    @Test fun labelsAndIconSizesChangeAsOneStep() {
        val e = HomeEditorState(withText(app("a", Anchor.cell(0, 0)).copy(sizeDp = 56), app("b", Anchor.cell(1, 0))), geo)
        e.setLabels(true)
        assertTrue(e.layout.items.filter { it.kind == ItemKind.App }.all { it.label })
        assertFalse(e.item("clock")!!.label)
        e.resetIconSizes()
        assertEquals(0, e.item("a")!!.sizeDp)
        e.undo(); e.undo()
        assertEquals(56, e.item("a")!!.sizeDp)
        assertFalse(e.item("b")!!.label)
    }

    @Test fun lensDragsSidewaysSnapsToTheCentreAndUndoesInOneStep() {
        val e = HomeEditorState(layout(), geo)
        e.startLensDrag()
        assertFalse(e.dragLensBy(30f))
        assertEquals(30f, e.lensDx())
        assertTrue("snaps back onto the centre", e.dragLensBy(6f))
        assertEquals(0f, e.lensDx())
        e.dragLensBy(80f); e.dragLensBy(500f)
        assertEquals("capped at 120 dp from the centre", 120f, e.lensDx())
        assertTrue(e.dropLens())
        assertEquals(120f, e.layout.lensDx())
        assertTrue(e.canUndo)
        e.undo()
        assertEquals(0f, e.lensDx())
        assertFalse(e.canUndo)
    }

    @Test fun lensStaysClearOfTheEdgesOnANarrowScreen() {
        val e = HomeEditorState(layout(), HomeGeometry(320f, 700f, statusTop = 24f, lensX = 160f, lensY = 640f))
        e.startLensDrag(); e.dragLensBy(-400f)
        assertEquals(-(160f - LENS_EDGE), e.lensDx())
        e.cancelLensDrag()
        assertFalse(e.canUndo)
        assertTrue(e.setLensDx(-500f))
        assertEquals(-100f, e.layout.lensDx())
    }

    @Test fun lensRowSlotsFoldToTheOtherSideNearAnEdge() {
        val g = HomeGeometry(360f, 800f, statusTop = 24f, lensX = 300f, lensY = 740f)
        val xs = Anchor.LENS_SLOTS.map { g.lensSlot(it).x }
        assertEquals(listOf(192f, 238f, 146f, 100f), xs) // -2, -1, 1, 2: everything to the left of LENS
        assertEquals(4, xs.distinct().size)
        assertTrue(xs.all { it in 24f..336f })
        // Centred LENS: unchanged ±62 / ±108.
        assertEquals(listOf(97.5f, 143.5f, 267.5f, 313.5f), Anchor.LENS_SLOTS.map { geo.lensSlot(it).x })
    }

    @Test fun aPresetKeepsLensWhereItIs() {
        val e = HomeEditorState(layout(), geo)
        e.setLensDx(-80f)
        e.preview(layout(Placement.Free))
        assertEquals(-80f, e.lensDx())
        e.confirmPreview()
        assertEquals(-80f, e.layout.lensDx())
    }
}

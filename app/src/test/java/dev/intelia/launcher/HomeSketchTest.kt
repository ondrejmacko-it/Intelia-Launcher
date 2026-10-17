package dev.intelia.launcher

import dev.intelia.launcher.data.Anchor
import dev.intelia.launcher.data.DockSpec
import dev.intelia.launcher.data.HomeItem
import dev.intelia.launcher.data.HomeSketch
import dev.intelia.launcher.data.HomeTemplates
import dev.intelia.launcher.data.ItemKind
import dev.intelia.launcher.data.LayoutBase
import dev.intelia.launcher.data.LayoutSpec
import dev.intelia.launcher.data.LensAt
import dev.intelia.launcher.data.sketch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSketchTest {
    private val lens = HomeItem("lens", ItemKind.Lens, Anchor.Lens)
    private fun app(key: String, slot: Int) = HomeItem("app:$key", target = key, anchor = Anchor.lensRow(slot))
    private fun layout(vararg items: HomeItem, lensAt: LensAt = LensAt.Middle) =
        LayoutSpec("mine", items = listOf(lens) + items, dock = DockSpec(lensAt = lensAt))

    @Test fun rowFollowsTheLayoutsSlotsNotTheFavourites() {
        val row = layout(app("c", 1), app("a", -2), app("d", 2), app("b", -1)).sketch().row
        assertEquals(listOf("a", "b", null, "c", "d"), row.map { it?.target?.ifEmpty { null } })
        assertEquals(HomeSketch.LENS_SLOT, row[2])
    }

    @Test fun emptySlotsStayAsGapsAndAnEmptyRowIsLensAlone() {
        assertEquals(listOf(null, "b", "lens", null, null),
            layout(app("b", -1)).sketch().row.map { it?.target?.ifEmpty { it.id } })
        assertEquals(listOf(HomeSketch.LENS_SLOT), layout().sketch().row)
    }

    @Test fun lensAtStartOrEndLeadsOrClosesTheRow() {
        val apps = arrayOf(app("a", -2), app("b", -1), app("c", 1), app("d", 2))
        assertEquals(HomeSketch.LENS_SLOT, layout(*apps, lensAt = LensAt.Start).sketch().row.first())
        assertEquals(HomeSketch.LENS_SLOT, layout(*apps, lensAt = LensAt.End).sketch().row.last())
    }

    @Test fun freeDateAboveTheClockIsDrawnAbove() {
        val clock = HomeItem("clock", ItemKind.Clock, Anchor.free(.5f, .044f))
        assertTrue(layout(clock, HomeItem("date", ItemKind.Date, Anchor.free(.3f, .031f))).sketch().dateAbove)
        assertFalse(layout(clock, HomeItem("date", ItemKind.Date, Anchor.free(.3f, .2f))).sketch().dateAbove)
        assertFalse(layout(clock, HomeItem("date", ItemKind.Date, Anchor.Bound)).sketch().dateAbove)
    }

    @Test fun legacyListKeepsItsOrder() {
        val items = HomeTemplates.build(LayoutBase.None, listOf("x", "y", "z"), showClock = true, showDate = true, legacyList = true)
        val sketch = LayoutSpec("preset", items = items.reversed()).sketch()
        assertEquals(listOf("x", "y", "z"), sketch.list)
        assertEquals(listOf(HomeSketch.LENS_SLOT), sketch.row)
    }
}

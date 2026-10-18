package dev.intelia.launcher.ui

import dev.intelia.launcher.data.LabelOverride
import dev.intelia.launcher.data.IconOverride
import dev.intelia.launcher.data.Anchor
import dev.intelia.launcher.data.GridSpec
import dev.intelia.launcher.data.HomeItem
import dev.intelia.launcher.data.ItemKind
import dev.intelia.launcher.data.LayoutSpec
import dev.intelia.launcher.data.Placement
import dev.intelia.launcher.data.TextLook
import dev.intelia.launcher.data.TypeFace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InheritanceTest {
    private val geo = HomeGeometry(411f, 911f, statusTop = 40f, lensX = 205.5f, lensY = 829f)
    private val clock = HomeItem("clock", ItemKind.Clock, Anchor(), sizeDp = 110, style = "Mono", weight = 500, tint = "Accent",
        alpha = .7f, look = TextLook(face = TypeFace.Inter, tracking = 6))
    private val app = HomeItem("a", target = "a/X", anchor = Anchor.cell(0, 0), sizeDp = 52, label = true)
    private fun editor() = HomeEditorState(LayoutSpec("l", placement = Placement.Grid, grid = GridSpec(4, 6),
        items = listOf(HomeItem("lens", ItemKind.Lens, Anchor.Lens), clock, app)), geo)

    @Test fun defaultsFollowTheGlobalValue() {
        val plain = HomeItem("c", ItemKind.Clock)
        ItemProp.entries.forEach { assertFalse(it.name, it.isOwn(plain)) }
        val iconProps = setOf(ItemProp.Label, ItemProp.LabelLook, ItemProp.Icon)
        ItemProp.entries.filter { it !in iconProps }.forEach { assertTrue(it.name, it.isOwn(clock)) }
        // S2: an icon's label switch, label look and icon look; returning one leaves the others.
        val own = app.copy(labelLook = LabelOverride(lines = 2), icon = IconOverride(alpha = .5f))
        iconProps.forEach { assertTrue(it.name, it.isOwn(own)) }
        assertEquals(own.copy(icon = null), ItemProp.Icon.inherit(own))
        assertEquals(own.copy(label = null), ItemProp.Label.inherit(own))
        assertEquals(StudioTarget.Icons, ItemProp.LabelLook.source(ItemKind.App))
    }

    @Test fun inheritDropsOnlyThatProperty() {
        val sized = ItemProp.Size.inherit(clock)
        assertEquals(0, sized.sizeDp)
        assertEquals(clock.copy(sizeDp = 0), sized)
        val font = ItemProp.Font.inherit(clock)
        assertEquals("", font.style)
        assertEquals(TypeFace.Style, font.look.face)
        assertEquals(6, font.look.tracking)
        assertEquals(500, font.weight)
        assertEquals(0, ItemProp.Weight.inherit(clock).weight)
        assertEquals("", ItemProp.Tint.inherit(clock).tint)
        assertEquals(.7f, ItemProp.Tint.inherit(clock).alpha)
    }

    @Test fun aDateKeepsItsFormatWhenItsFaceIsInherited() {
        val date = HomeItem("d", ItemKind.Date, style = "Short", look = TextLook(face = TypeFace.DmSerif))
        assertTrue(ItemProp.Font.isOwn(date))
        val back = ItemProp.Font.inherit(date)
        assertEquals("Short", back.style)
        assertFalse(ItemProp.Font.isOwn(back))
        assertFalse(ItemProp.Font.isOwn(date.copy(look = TextLook())))
    }

    @Test fun iconSizeComesFromLookIconsOtherSizesFromTheKind() {
        assertEquals(StudioTarget.Icons, ItemProp.Size.source(ItemKind.App))
        assertEquals(StudioTarget.Icons, ItemProp.Size.source(ItemKind.Flow))
        assertNull(ItemProp.Size.source(ItemKind.Clock))
        assertNull(ItemProp.Tint.source(ItemKind.Clock))
    }

    @Test fun returnToGlobalIsItsOwnUndoStep() {
        val e = editor()
        e.edit("a", "size") { it.copy(sizeDp = 60) }
        e.edit("a", "size") { it.copy(sizeDp = 64) }
        assertTrue(e.inherit("a", ItemProp.Size))
        assertEquals(0, e.item("a")!!.sizeDp)
        assertEquals(true, e.item("a")!!.label)
        e.undo()
        assertEquals(64, e.item("a")!!.sizeDp)
        e.undo()
        assertEquals(52, e.item("a")!!.sizeDp)
    }

    @Test fun inheritingWhatIsAlreadyGlobalChangesNothing() {
        val e = editor()
        assertTrue(e.inherit("clock", ItemProp.Weight))
        assertFalse(e.inherit("clock", ItemProp.Weight))
        assertFalse(e.inherit("missing", ItemProp.Size))
        e.undo()
        assertEquals(500, e.item("clock")!!.weight)
    }
}

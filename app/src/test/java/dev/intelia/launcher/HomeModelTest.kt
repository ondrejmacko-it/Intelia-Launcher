package dev.intelia.launcher

import dev.intelia.launcher.data.Anchor
import dev.intelia.launcher.data.AnchorType
import dev.intelia.launcher.data.GridSpec
import dev.intelia.launcher.data.HomeItem
import dev.intelia.launcher.data.HomeLayout
import dev.intelia.launcher.data.HomeLayouts
import dev.intelia.launcher.data.ItemKind
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.LayoutSpec
import dev.intelia.launcher.data.SettingsCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeModelTest {
    private fun apps(l: LayoutSpec) = l.items.filter { it.kind == ItemKind.App }

    @Test fun schema1MigratesToALinkedLayoutWithoutLosingAnything() {
        val v1 = """{"schema":1,"onboarded":true,"layout":"Essentials","favorites":["a/A","b/B","c/C","d/D","e/E"],
            "showClock":true,"showDate":false,"clockStyle":"Crystal","arc":["x/X"]}"""
        val s = SettingsCodec.decode(v1)
        assertEquals(LauncherSettings.SCHEMA, s.schema)
        // Legacy fields are kept as they were.
        assertEquals(listOf("a/A", "b/B", "c/C", "d/D", "e/E"), s.favorites)
        assertEquals(HomeLayout.Essentials, s.layout)
        assertEquals(listOf("x/X"), s.arc)
        val active = s.home.activeLayout()
        assertTrue(active.linked)
        assertEquals("Moje plocha", active.name)
        // The first four sit in the LENS row exactly as phase B2 drew them.
        assertEquals(listOf(-2, -1, 1, 2), apps(active).map { it.anchor.slot })
        assertEquals(listOf("a/A", "b/B", "c/C", "d/D"), apps(active).map { it.target })
        assertTrue(active.items.any { it.kind == ItemKind.Clock })
        assertFalse(active.items.any { it.kind == ItemKind.Date })
        assertEquals(1, active.items.count { it.kind == ItemKind.Lens })
    }

    @Test fun legacyPlacementOfFewFavouritesAndOtherPresets() {
        fun linked(layout: HomeLayout, favs: List<String>) =
            LauncherSettings(layout = layout, favorites = favs, schema = 1).sanitized().home.activeLayout()
        assertEquals(listOf(-1), apps(linked(HomeLayout.Essentials, listOf("a/A"))).map { it.anchor.slot })
        assertEquals(listOf(-2, -1, 1), apps(linked(HomeLayout.Essentials, listOf("a/A", "b/B", "c/C"))).map { it.anchor.slot })
        assertTrue(apps(linked(HomeLayout.Pure, listOf("a/A"))).isEmpty())
        val custom = linked(HomeLayout.Custom, listOf("a/A", "b/B"))
        assertEquals(listOf(AnchorType.Stack, AnchorType.Stack), apps(custom).map { it.anchor.type })
        assertEquals(0f, custom.items.first { it.kind == ItemKind.Clock }.anchor.fx)
    }

    @Test fun emptyAndCorruptedDocuments() {
        val fresh = SettingsCodec.decode(null)
        assertEquals(HomeLayouts.LINKED_ID, fresh.home.active)
        assertEquals(1, fresh.home.layouts.size)
        assertEquals(LauncherSettings.fresh(), SettingsCodec.decode("{not json"))
        // Damaged home values degrade item by item; the rest of the document survives.
        val bad = SettingsCodec.decode("""{"schema":2,"accent":"Sage","home":{"active":"ghost","layouts":[
            {"id":"u1","name":"\u0000Mine","grid":{"cols":99,"rows":1},"items":[
              {"id":"a","kind":"App","target":"a/A","anchor":{"type":"Cell","col":50,"row":-3}},
              {"id":"a","kind":"App","target":"b/B"},
              {"id":"n","kind":"App","target":""},
              {"id":"f","kind":"App","target":"c/C","anchor":{"type":"Free","fx":-4,"fy":7}},
              {"id":"s","kind":"App","target":"d/D","anchor":{"type":"LensRow","slot":9}},
              {"id":"w","kind":"Neon","target":"e/E","anchor":{"type":"Warp"}},
              {"id":"c1","kind":"Clock"},{"id":"c2","kind":"Clock"}]},
            {"id":"preset","linked":false,"items":[]},
            {"id":"u1","items":[]}]}}""")
        assertEquals("Sage", bad.accent.name)
        assertEquals(HomeLayouts.LINKED_ID, bad.home.active)
        val u1 = bad.home.layouts.single { it.id == "u1" }
        assertEquals(GridSpec(6, 4), u1.grid)
        assertEquals("Mine", u1.name)
        assertEquals(Anchor.cell(5, 0), u1.items.first { it.id == "a" }.anchor)
        assertEquals(0f, u1.items.first { it.id == "f" }.anchor.fx)
        assertEquals(1f, u1.items.first { it.id == "f" }.anchor.fy)
        assertFalse(u1.items.any { it.id == "n" || it.id == "s" })
        assertEquals(1, u1.items.count { it.kind == ItemKind.Clock })
        assertEquals(1, u1.items.count { it.kind == ItemKind.Lens })
        assertEquals(2, bad.home.layouts.size)
    }

    @Test fun roundTripAndIdempotence() {
        val user = LayoutSpec("mine", "Moje", items = listOf(HomeItem("a", target = "a/A", anchor = Anchor.cell(1, 2))))
        val s = LauncherSettings(favorites = listOf("a/A"), home = HomeLayouts("mine", listOf(user)), schema = 2).sanitized()
        assertEquals(s, SettingsCodec.decode(SettingsCodec.encode(s)))
        assertEquals(s, s.sanitized())
        assertEquals("mine", s.home.active)
    }

    @Test fun presetChangesNeverTouchAUserLayout() {
        val user = LayoutSpec("mine", "Moje", items = listOf(HomeItem("a", target = "a/A", anchor = Anchor.free(.2f, .3f))))
        val s = LauncherSettings(home = HomeLayouts("mine", listOf(user)), schema = 2).sanitized()
        val after = s.copy(layout = HomeLayout.Pure, favorites = listOf("z/Z")).sanitized()
        assertEquals(s.home.layouts.single { it.id == "mine" }, after.home.layouts.single { it.id == "mine" })
        assertEquals(dev.intelia.launcher.data.LayoutBase.Pure, after.home.layouts.first { it.linked }.base)
    }

    @Test fun cellsReclampWhenTheGridShrinks() {
        val user = LayoutSpec("mine", grid = GridSpec(6, 8), items = listOf(HomeItem("a", target = "a/A", anchor = Anchor.cell(5, 7))))
        val s = LauncherSettings(home = HomeLayouts("mine", listOf(user.copy(grid = GridSpec(3, 4)))), schema = 2).sanitized()
        assertEquals(Anchor.cell(2, 3), s.home.activeLayout().items.first { it.id == "a" }.anchor)
    }

    @Test fun savingAnEditedPresetCreatesAnOwnCopyAndKeepsThePreset() {
        val s = LauncherSettings(favorites = listOf("a/A"), schema = 2).sanitized()
        val edited = s.home.activeLayout().let { l -> l.copy(items = l.items.map { if (it.target == "a/A") it.copy(anchor = Anchor.cell(1, 1)) else it }) }
        val saved = s.copy(home = s.home.save(edited)).sanitized()
        assertEquals(HomeLayouts.USER_ID, saved.home.active)
        assertEquals(Anchor.cell(1, 1), saved.home.activeLayout().items.first { it.target == "a/A" }.anchor)
        assertTrue(saved.home.layouts.first().linked)
        assertEquals(Anchor.lensRow(-1), saved.home.layouts.first().items.first { it.target == "a/A" }.anchor)
        // Saving the own copy again replaces it in place.
        val again = saved.home.save(saved.home.activeLayout().copy(placement = dev.intelia.launcher.data.Placement.Free))
        assertEquals(2, again.layouts.size)
    }
}

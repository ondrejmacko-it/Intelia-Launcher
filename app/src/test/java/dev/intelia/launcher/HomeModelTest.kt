package dev.intelia.launcher

import dev.intelia.launcher.data.Anchor
import dev.intelia.launcher.data.AnchorType
import dev.intelia.launcher.data.GridSpec
import dev.intelia.launcher.data.HomeItem
import dev.intelia.launcher.data.IconShape
import dev.intelia.launcher.data.HomeLayout
import dev.intelia.launcher.data.HomeLayouts
import dev.intelia.launcher.data.ItemKind
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.LayoutSpec
import dev.intelia.launcher.data.SettingsCodec
import dev.intelia.launcher.data.Reach
import dev.intelia.launcher.data.lensDx
import dev.intelia.launcher.data.ClockLayout
import dev.intelia.launcher.data.ClockStyle
import dev.intelia.launcher.data.HomeTemplates
import dev.intelia.launcher.data.LayoutBase
import dev.intelia.launcher.data.ClockSeparator
import dev.intelia.launcher.data.DateFormat
import dev.intelia.launcher.data.DatePart
import dev.intelia.launcher.data.TextCase
import dev.intelia.launcher.data.TextLook
import dev.intelia.launcher.data.TypeFace
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

    @Test fun itemLookIsClampedAndUnknownValuesFallBack() {
        val bad = LayoutSpec("mine", items = listOf(
            HomeItem("clock", ItemKind.Clock, Anchor.free(.5f, 0f), sizeDp = 999, style = "Comic", weight = 5000, tint = "Neon", alpha = Float.NaN),
            HomeItem("lens", ItemKind.Lens, Anchor.Lens, style = "Filled", weight = 300, alpha = .1f),
            HomeItem("app:a/X", target = "a/X", anchor = Anchor.cell(0, 0), style = "Crystal", tint = "Accent"),
        ))
        val s = LauncherSettings(home = HomeLayouts("mine", listOf(bad))).sanitized()
        val l = s.home.activeLayout()
        val clock = l.items.first { it.kind == ItemKind.Clock }
        assertEquals(160, clock.sizeDp); assertEquals("", clock.style); assertEquals(900, clock.weight)
        assertEquals("", clock.tint); assertEquals(1f, clock.alpha)
        val lens = l.items.first { it.kind == ItemKind.Lens }
        assertEquals("Filled", lens.style); assertEquals(0, lens.weight); assertEquals(.3f, lens.alpha)
        val app = l.items.first { it.kind == ItemKind.App }
        assertEquals("", app.style); assertEquals("Accent", app.tint)
    }

    @Test fun lookSettingsAreClampedAndDateKeepsItsOwnSizeRange() {
        val s = LauncherSettings(veilShift = 99, iconSize = 7, home = HomeLayouts(HomeLayouts.USER_ID, listOf(LayoutSpec(HomeLayouts.USER_ID, items = listOf(
            HomeItem("date", ItemKind.Date, Anchor.free(.2f, .3f), sizeDp = 14, style = "Short"),
            HomeItem("clock", ItemKind.Clock, Anchor.free(.5f, .1f), style = "Short")))))).sanitized()
        assertEquals(LauncherSettings.MAX_VEIL_SHIFT, s.veilShift)
        assertEquals(LauncherSettings.MIN_ICON, s.iconSize)
        val items = s.home.layouts.first { it.id == HomeLayouts.USER_ID }.items
        val date = items.first { it.kind == ItemKind.Date }
        assertEquals(14, date.sizeDp)
        assertEquals("Short", date.style)
        assertEquals(AnchorType.Free, date.anchor.type)
        assertEquals("", items.first { it.kind == ItemKind.Clock }.style)
    }

    @Test fun lensPositionMigratesFromReachAndIsBounded() {
        // Before the continuous position: no dx anywhere, Reach decides, for the linked preset and a user layout alike.
        val old = """{"schema":2,"reach":"Left","home":{"active":"mine","layouts":[{"id":"mine","name":"M","base":"None",
            "items":[{"id":"lens","kind":"Lens","anchor":{"type":"Lens"}}]}]}}"""
        val s = SettingsCodec.decode(old)
        assertEquals(-56f, s.home.lensDx())
        assertEquals(-56f, s.home.layouts.first { it.linked }.lensDx())
        // Once stored, the layout's own value wins over Reach; it survives a round trip.
        val moved = s.copy(reach = Reach.Right, home = s.home.copy(layouts = s.home.layouts.map { l ->
            l.copy(items = l.items.map { if (it.kind == ItemKind.Lens) it.copy(anchor = it.anchor.copy(dx = 97f)) else it })
        })).sanitized()
        assertEquals(97f, moved.home.lensDx())
        assertEquals(97f, SettingsCodec.decode(SettingsCodec.encode(moved)).home.lensDx())
        assertEquals(56f, moved.home.layouts.first { it.linked }.lensDx())
        // Out of range or not a number.
        fun with(dx: Float) = moved.copy(home = moved.home.copy(layouts = moved.home.layouts.map { l ->
            l.copy(items = l.items.map { if (it.kind == ItemKind.Lens) it.copy(anchor = it.anchor.copy(dx = dx)) else it })
        })).sanitized().home.lensDx()
        assertEquals(LauncherSettings.MAX_LENS_DX, with(5000f))
        assertEquals(-LauncherSettings.MAX_LENS_DX, with(-5000f))
        assertEquals(0f, with(Float.NaN))
    }

    @Test
    fun clockBackgroundIsKeptOnlyOnTheClock() {
        val items = listOf(
            HomeItem("c", ItemKind.Clock, Anchor.free(.5f, .1f), background = "Veil"),
            HomeItem("a", ItemKind.App, Anchor.cell(0, 0), target = "p/a", background = "Veil"),
        )
        val s = LauncherSettings(home = HomeLayouts("u", listOf(LayoutSpec("u", items = items))), schema = 2).sanitized()
        val kept = s.home.activeLayout().items
        assertEquals("Veil", kept.first { it.id == "c" }.background)
        assertEquals("", kept.first { it.id == "a" }.background)
        val odd = LauncherSettings(home = HomeLayouts("u", listOf(LayoutSpec("u", items = listOf(items[0].copy(background = "Glass"))))), schema = 2)
        assertEquals("", odd.sanitized().home.activeLayout().items.first { it.id == "c" }.background)
    }

    @Test
    fun textLookIsClampedAndKeptOnlyOnClockAndDate() {
        val wild = TextLook(face = TypeFace.Inter, tracking = 99, dx = 500f, dy = Float.NaN, layout = ClockLayout.Stack, padX = 999f, padY = -5f)
        val items = listOf(
            HomeItem("c", ItemKind.Clock, Anchor.free(.5f, .1f), look = wild, background = "Liquid"),
            HomeItem("d", ItemKind.Date, Anchor.free(.5f, .3f), background = "Glitter"),
            HomeItem("a", ItemKind.App, Anchor.cell(0, 0), target = "p/a", look = wild),
        )
        val kept = LauncherSettings(home = HomeLayouts("u", listOf(LayoutSpec("u", items = items))), schema = 2).sanitized().home.activeLayout().items
        val c = kept.first { it.id == "c" }.look
        assertEquals(TextLook.MAX_TRACKING, c.tracking)
        assertEquals(TextLook.MAX_OFFSET, c.dx)
        assertEquals(0f, c.dy)
        assertEquals(ClockLayout.Stack, c.layout)
        assertEquals(TextLook.MAX_PAD, c.padX)
        assertEquals(0f, c.padY)
        assertEquals("Liquid", kept.first { it.id == "c" }.background)
        assertEquals("", kept.first { it.id == "d" }.background)
        assertEquals(TextLook(), kept.first { it.id == "a" }.look)
    }

    @Test
    fun unknownLookNamesDecodeToDefaultsAndOldDocumentsHaveNone() {
        val doc = SettingsCodec.encode(LauncherSettings(home = HomeLayouts("u", listOf(LayoutSpec("u",
            items = listOf(HomeItem("c", ItemKind.Clock, Anchor.free(.5f, .1f), look = TextLook(face = TypeFace.Unbounded, separator = ClockSeparator.Bar)))))), schema = 2))
        val odd = doc.replace("\"Unbounded\"", "\"Comic\"")
        val look = SettingsCodec.decode(odd).home.activeLayout().items.first { it.id == "c" }.look
        assertEquals(TypeFace.Style, look.face)
        assertEquals(ClockSeparator.Bar, look.separator)
        val old = SettingsCodec.decode(doc.replace(Regex(",?\"look\":\\{[^}]*\\}"), "")).home.activeLayout().items.first { it.id == "c" }.look
        assertEquals(TextLook(), old)
    }

    @Test
    fun clockDigitsAndOwnDateFormat() {
        assertEquals("09" to "05", dev.intelia.launcher.ui.clockDigits(java.time.LocalTime.of(9, 5), false))
        assertEquals("9" to "05", dev.intelia.launcher.ui.clockDigits(java.time.LocalTime.of(21, 5), true))
        assertEquals("12" to "00", dev.intelia.launcher.ui.clockDigits(java.time.LocalTime.of(0, 0), true))
        val now = java.time.LocalDateTime.of(2026, 10, 17, 11, 0)
        val cs = java.util.Locale.forLanguageTag("cs")
        val own = TextLook(weekday = DatePart.None, month = DatePart.Number, year = true)
        assertEquals("17. 10. 2026", dev.intelia.launcher.ui.dateText(now, DateFormat.Custom, own, cs))
        assertEquals("SOBOTA 17. ŘÍJNA", dev.intelia.launcher.ui.dateText(now, DateFormat.Long, TextLook(), cs))
        assertEquals("sobota", dev.intelia.launcher.ui.dateText(now, DateFormat.Weekday, TextLook(case = TextCase.Lower), cs))
    }

    @Test fun iconShapeDefaultsToCircleAndUnknownNamesFallBack() {
        assertEquals(IconShape.Circle, SettingsCodec.decode("{\"schema\":2}").iconShape)
        assertEquals(IconShape.Circle, SettingsCodec.decode("{\"schema\":2,\"iconShape\":\"Blob\"}").iconShape)
        val drop = LauncherSettings.fresh().copy(iconShape = IconShape.Drop)
        assertEquals(IconShape.Drop, SettingsCodec.decode(SettingsCodec.encode(drop)).iconShape)
    }

    /** D15: a new install gets the "Čisté" clock straight on the wallpaper and no icon names. */
    @Test fun freshInstallHasThePureClockAndNoNames() {
        val fresh = LauncherSettings.fresh()
        assertEquals(ClockStyle.Crystal, fresh.clockStyle)
        val clock = fresh.home.activeLayout().items.single { it.kind == ItemKind.Clock }
        assertEquals(ClockLayout.Row, clock.look.layout)
        assertEquals("", clock.background)
        for (base in listOf(LayoutBase.Essentials, LayoutBase.Balanced)) {
            val items = HomeTemplates.build(base, (1..8).map { "p$it/A" }, showClock = true, showDate = true)
            assertTrue(items.none { it.label })
        }
    }
}


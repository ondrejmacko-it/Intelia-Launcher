package dev.intelia.launcher

import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.PulseLens
import dev.intelia.launcher.data.SettingsCodec
import dev.intelia.launcher.data.ThemeMode
import dev.intelia.launcher.flow.Flow
import dev.intelia.launcher.flow.FlowAlias
import dev.intelia.launcher.flow.FlowError
import dev.intelia.launcher.search.AppSearch
import dev.intelia.launcher.ui.ArcPick
import dev.intelia.launcher.ui.ArcRings
import dev.intelia.launcher.ui.lensSwipeSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowTest {
    private val train = FlowAlias("1", "Vlak", listOf("cz.cd/Main", "com.spotify/Main"))

    @Test fun normalizesCaseWidthAndSpaces() {
        assertEquals("vlak domů", Flow.normalize("  ＶＬＡＫ   Domů "))
    }

    @Test fun exactMatchOnlyAndPrefixSuggestions() {
        assertEquals(train, Flow.exact(listOf(train), " vlak "))
        assertNull(Flow.exact(listOf(train), "vla"))
        assertEquals(listOf(train), Flow.suggest(listOf(train), "vl"))
        assertTrue(Flow.suggest(listOf(train), " ").isEmpty())
    }

    @Test fun validation() {
        assertNull(Flow.validate(train))
        assertEquals(FlowError.Empty, Flow.validate(train.copy(name = "  ")))
        assertEquals(FlowError.ControlCharacter, Flow.validate(train.copy(name = "a\u0007")))
        assertEquals(FlowError.TooLong, Flow.validate(train.copy(name = "x".repeat(65))))
        assertEquals(FlowError.Duplicate, Flow.validate(train.copy(id = "2"), listOf(train.copy(name = "VLAK"))))
        assertNull(Flow.validate(train, listOf(train)))
        assertEquals(FlowError.NoTargets, Flow.validate(train.copy(targets = emptyList())))
        assertEquals(FlowError.TooManyTargets, Flow.validate(train.copy(targets = List(5) { "p/$it" })))
        assertEquals(FlowError.InvalidTarget, Flow.validate(train.copy(targets = listOf("a/b", "a/b"))))
    }

    @Test fun resolveIsAllOrNothing() {
        assertEquals(listOf("cz.cd/Main", "com.spotify/Main"), Flow.resolve(train) { it })
        assertNull(Flow.resolve(train) { key -> key.takeIf { it.startsWith("cz") } })
    }
}

class AppSearchTest {
    private val apps = listOf("Google Maps", "Mapy.cz", "Gmail", "Kalkulačka", "Čtečka", "Messages")

    private fun rank(query: String) = AppSearch.rank(apps, query) { AppSearch.fold(it) }

    @Test fun prefixBeatsWordStartBeatsContains() {
        assertEquals(listOf("Mapy.cz", "Google Maps"), rank("map"))
    }

    @Test fun ignoresDiacritics() {
        assertEquals(listOf("Kalkulačka"), rank("kalkulack"))
        assertEquals(listOf("Čtečka"), rank("cte"))
    }

    @Test fun initialsAndMultiWord() {
        assertEquals(listOf("Gmail", "Google Maps"), rank("gm"))
        assertEquals(listOf("Google Maps"), rank("goo ma"))
    }

    @Test fun blankQueryHasNoResults() {
        assertTrue(rank("  ").isEmpty())
    }
}

class SettingsCodecTest {
    @Test fun roundTrip() {
        val settings = LauncherSettings(theme = ThemeMode.Dark, arc = listOf("a/b"), hidden = setOf("c/d"),
            aliases = listOf(FlowAlias("1", "vlak", listOf("a/b"))))
        assertEquals(settings, SettingsCodec.decode(SettingsCodec.encode(settings)))
    }

    @Test fun corruptOrUnknownInputFallsBackSafely() {
        assertEquals(LauncherSettings(), SettingsCodec.decode("{not json"))
        assertEquals(ThemeMode.System, SettingsCodec.decode("""{"theme":"Neon","future":1}""").theme)
    }

    @Test fun sanitizesBoundsAndDuplicates() {
        val decoded = SettingsCodec.decode("""{"arc":["a","a","b","c","d","e","f",""],
            "aliases":[{"id":"1","name":"x","targets":["a"]},{"id":"2","name":"X","targets":["b"]}]}""")
        assertEquals(listOf("a", "b", "c", "d", "e"), decoded.arc)
        assertEquals(1, decoded.aliases.size)
    }

    @Test fun designFieldsAreBoundedAndLegacyMotionMigrates() {
        val decoded = SettingsCodec.decode("""{"reduceMotion":true,"arcRings":9,"arcFlows":["1","ghost","1"],
            "favorites":["a","a","b"],"aliases":[{"id":"1","name":"vlak","targets":["a"]}]}""")
        assertEquals(3, decoded.arcRings)
        assertEquals(listOf("1"), decoded.arcFlows)
        assertEquals(listOf("a", "b"), decoded.favorites)
        assertEquals(dev.intelia.launcher.data.Motion.Off, decoded.motion)
        assertFalse(decoded.reduceMotion)
    }

    @Test fun pulseLensIsClampedAndNonFiniteFallsBackToAuto() {
        val decoded = SettingsCodec.decode("""{"pulseLens":{"x":-5,"y":900,"r":0.5}}""")
        assertEquals(PulseLens(0f, LauncherSettings.MAX_LENS_Y, LauncherSettings.MIN_LENS_R), decoded.pulseLens)
        val big = LauncherSettings(pulseLens = PulseLens(5000f, 24f, 99f)).sanitized()
        assertEquals(PulseLens(LauncherSettings.MAX_LENS_X, 24f, LauncherSettings.MAX_LENS_R), big.pulseLens)
        val kept = PulseLens(205.5f, 26f, 8f)
        assertEquals(kept, LauncherSettings(pulseLens = kept).sanitized().pulseLens)
        assertNull(LauncherSettings(pulseLens = PulseLens(Float.NaN, 24f, 10f)).sanitized().pulseLens)
        assertNull(LauncherSettings(pulseLens = PulseLens(200f, 24f, Float.POSITIVE_INFINITY)).sanitized().pulseLens)
        assertNull(SettingsCodec.decode("{}").pulseLens)
    }

    @Test fun pulseAlarmHoursStayWithinADay() {
        assertEquals(12, SettingsCodec.decode("{}").pulseAlarmHours)
        assertEquals(1, SettingsCodec.decode("""{"pulseAlarmHours":0}""").pulseAlarmHours)
        assertEquals(1, LauncherSettings(pulseAlarmHours = -3).sanitized().pulseAlarmHours)
        assertEquals(24, SettingsCodec.decode("""{"pulseAlarmHours":999}""").pulseAlarmHours)
        assertEquals(6, SettingsCodec.decode("""{"pulseAlarmHours":6}""").pulseAlarmHours)
    }

    @Test fun foundationSettingsStillDecode() {
        val decoded = SettingsCodec.decode("""{"theme":"Dark","accent":"Sage","arc":["a"]}""")
        assertEquals(ThemeMode.Dark, decoded.theme)
        assertEquals(listOf("a"), decoded.arc)
        assertFalse(decoded.onboarded)
    }
}

class ArcRingsTest {
    private val full = listOf(List(5) { true }, List(7) { true }, List(7) { true })
    private val rings = ArcRings(full, left = 205f, right = 205f)

    private fun at(ring: Int, index: Int) = rings.position(ring, index)

    @Test fun distanceChoosesRingAngleChoosesSlot() {
        val (x0, y0) = at(0, 2)
        assertEquals(ArcPick(0, 2), rings.select(x0, y0))
        val (x1, y1) = at(1, 6)
        assertEquals(ArcPick(1, 6), rings.select(x1, y1))
        val (x2, y2) = at(2, 0)
        assertEquals(ArcPick(2, 0), rings.select(x2, y2))
    }

    @Test fun backOnLensOrBelowCancels() {
        assertNull(rings.select(0f, -20f))
        assertNull(rings.select(40f, 60f))
        assertNull(rings.select(0f, -400f))
    }

    @Test fun emptySlotsStayEmptyAndDoNotShift() {
        val sparse = ArcRings(listOf(listOf(true, false, true, false, false)), 205f, 205f)
        val (x, y) = sparse.position(0, 1)
        assertNull(sparse.select(x, y))
        assertEquals(sparse.position(0, 2), ArcRings(listOf(List(5) { true }), 205f, 205f).position(0, 2))
    }

    @Test fun selectionHoldsNearPreviousSlot() {
        val previous = ArcPick(1, 3)
        val (x, y) = at(1, 3)
        val drift = x + 30f
        assertEquals(previous, rings.select(drift, y, previous))
    }

    @Test fun slotsStayOnScreenWhenLensIsOffCentre() {
        val shifted = ArcRings(full, left = 120f, right = 290f)
        for (ring in full.indices) for (i in full[ring].indices) {
            val (x, _) = shifted.position(ring, i)
            assertTrue("ring $ring slot $i at $x", x >= -120f + 30f && x <= 290f - 30f)
        }
    }

    @Test fun swipeUpDetection() {
        assertTrue(lensSwipeSearch(0f, -60f))
        assertFalse(lensSwipeSearch(0f, -20f))
        assertFalse(lensSwipeSearch(200f, -60f))
    }
}

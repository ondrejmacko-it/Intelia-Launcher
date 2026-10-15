package dev.intelia.launcher

import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.SettingsCodec
import dev.intelia.launcher.data.ThemeMode
import dev.intelia.launcher.flow.Flow
import dev.intelia.launcher.flow.FlowAlias
import dev.intelia.launcher.flow.FlowError
import dev.intelia.launcher.search.AppSearch
import dev.intelia.launcher.ui.ArcGesture
import dev.intelia.launcher.ui.arcCenters
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
}

class ArcGestureTest {
    private val centers = arcCenters(5, 300f)

    @Test fun selectsOnlyAfterArming() {
        val gesture = ArcGesture(centers)
        assertNull(gesture.move(centers[2].first, -10f))
        assertEquals(2, gesture.move(centers[2].first, centers[2].second))
        assertEquals(2, gesture.finish())
        assertNull(gesture.finish())
    }

    @Test fun returningToLensCancelsSelection() {
        val gesture = ArcGesture(centers)
        gesture.move(centers[0].first, centers[0].second)
        assertNull(gesture.move(0f, 0f))
        assertNull(gesture.finish())
    }

    @Test fun fastCrossingSelects() {
        val gesture = ArcGesture(centers)
        gesture.move(centers[1].first, -40f)
        assertEquals(1, gesture.move(centers[1].first, centers[1].second - 35f))
    }

    @Test fun secondPointerCancels() {
        val gesture = ArcGesture(centers)
        gesture.move(centers[2].first, centers[2].second)
        assertNull(gesture.move(centers[2].first, centers[2].second, pointers = 2))
        assertNull(gesture.finish())
    }

    @Test fun swipeUpDetection() {
        assertTrue(lensSwipeSearch(0f, -60f))
        assertFalse(lensSwipeSearch(120f, -60f))
        assertFalse(lensSwipeSearch(0f, -20f))
    }
}

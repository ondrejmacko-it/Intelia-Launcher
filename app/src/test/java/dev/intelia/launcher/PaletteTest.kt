package dev.intelia.launcher

import dev.intelia.launcher.data.Accent
import dev.intelia.launcher.data.AccentColors
import dev.intelia.launcher.data.IconStyle
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.Palette
import dev.intelia.launcher.data.PaletteTarget
import dev.intelia.launcher.data.SettingsCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteTest {
    @Test
    fun everyAccentKeepsTextContrastOnVeil() {
        Accent.entries.forEach { a ->
            val dark = AccentColors.contrast(AccentColors.argb(a, true), AccentColors.VEIL_DARK)
            val light = AccentColors.contrast(AccentColors.argb(a, false), AccentColors.VEIL_LIGHT)
            assertTrue("$a dark $dark", dark >= 4.5)
            assertTrue("$a light $light", light >= 4.5)
        }
    }

    @Test
    fun sixteenHuesPlusDynamicAndOldNamesKept() {
        assertEquals(17, Accent.entries.size)
        assertEquals(listOf("Dynamic", "Mono", "Sage", "Slate", "Terracotta", "Frost", "Silver"), Accent.entries.take(7).map { it.name })
    }

    @Test
    fun unifiedPaletteUsesTheMainAccentEverywhere() {
        val s = LauncherSettings(accent = Accent.Sky, palette = Palette(unified = true, clock = Accent.Rose))
        PaletteTarget.entries.forEach { assertEquals(Accent.Sky, s.accentFor(it)) }
        val split = s.copy(palette = s.palette.copy(unified = false))
        assertEquals(Accent.Rose, split.accentFor(PaletteTarget.Clock))
        assertEquals(Accent.Sky, split.accentFor(PaletteTarget.Lens))
    }

    @Test
    fun olderDocumentsDecodeUnifiedAndUnknownColoursFallBack() {
        val old = SettingsCodec.decode("""{"accent":"Sage","schema":2}""")
        assertEquals(Palette(), old.palette)
        val odd = SettingsCodec.decode("""{"accent":"Sage","palette":{"unified":false,"clock":"Neon","lens":"Teal"},"schema":2}""")
        assertEquals(null, odd.palette.clock)
        assertEquals(Accent.Teal, odd.accentFor(PaletteTarget.Lens))
        assertEquals(Accent.Sage, odd.accentFor(PaletteTarget.Clock))
        assertEquals(odd, SettingsCodec.decode(SettingsCodec.encode(odd)))
    }

    @Test
    fun tintedIconsAreAppendedAndDecode() {
        assertEquals(listOf("System", "Ink", "Veil", "Tinted"), IconStyle.entries.map { it.name })
        assertEquals(IconStyle.Tinted, SettingsCodec.decode("""{"iconStyle":"Tinted","schema":2}""").iconStyle)
    }
}

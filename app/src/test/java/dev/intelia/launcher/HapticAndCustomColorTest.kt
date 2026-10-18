package dev.intelia.launcher

import dev.intelia.launcher.data.AccentColors
import dev.intelia.launcher.data.CustomColors
import dev.intelia.launcher.data.Haptic
import dev.intelia.launcher.data.HapticPrefs
import dev.intelia.launcher.data.HapticSurface
import dev.intelia.launcher.data.Haptics
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.SettingsCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticAndCustomColorTest {
    @Test fun anExistingInstallKeepsItsHapticFeel() {
        // No strength stored: the old choice decides, so an update changes nothing.
        assertEquals(Haptic.Step.Light.level, LauncherSettings(haptics = Haptics.Soft).hapticLevel())
        assertEquals(Haptic.Step.Strong.level, LauncherSettings(haptics = Haptics.Strong).hapticLevel())
        // …and the stop shown is the one it had (Jemná stays Jemná, not Střední).
        Haptics.entries.forEach { assertEquals(it, Haptic.toLegacy(LauncherSettings(haptics = it).hapticLevel())) }
        assertEquals(Haptic.Step.Light, Haptic.step(LauncherSettings(haptics = Haptics.Soft).hapticLevel()))
        assertEquals(0, LauncherSettings(haptics = Haptics.Off).hapticLevel())
        // An old document without the new fields decodes to the same.
        val old = SettingsCodec.decode("""{"haptics":"Strong","accent":"Sage"}""")
        assertEquals(Haptic.Step.Strong.level, old.hapticLevel())
        assertNull(old.custom.accent)
    }

    @Test fun settingAStrengthAlsoWritesTheOldChoice() {
        val s = LauncherSettings().withHapticLevel(70)
        assertEquals(70, s.hapticLevel())
        assertEquals(Haptics.Strong, s.haptics)
        assertEquals(Haptics.Off, LauncherSettings().withHapticLevel(-5).haptics)
        assertEquals(0, LauncherSettings().withHapticLevel(-5).hapticLevel())
        assertEquals(100, LauncherSettings().withHapticLevel(400).hapticLevel())
    }

    @Test fun surfacesFallBackToTheGlobalLevelAndAreClamped() {
        val p = HapticPrefs(strength = 40, per = mapOf(HapticSurface.Arc to 90, HapticSurface.Pulse to 300)).sanitized()
        assertEquals(90, Haptic.level(p, Haptics.Soft, HapticSurface.Arc))
        assertEquals(100, Haptic.level(p, Haptics.Soft, HapticSurface.Pulse))
        assertEquals(40, Haptic.level(p, Haptics.Soft, HapticSurface.Keys))
        assertEquals(40, Haptic.level(p, Haptics.Soft))
    }

    @Test fun planUsesAmplitudeWhereItCanElseFourSystemStops() {
        assertEquals(Haptic.Plan.None, Haptic.plan(0, Haptic.Kind.Tick, amplitudeControl = true))
        // The system's touch feedback switch wins on both paths.
        assertEquals(Haptic.Plan.None, Haptic.plan(80, Haptic.Kind.Tick, amplitudeControl = true, systemOn = false))
        assertEquals(Haptic.Plan.None, Haptic.plan(80, Haptic.Kind.Tick, amplitudeControl = false, systemOn = false))
        val weak = Haptic.plan(1, Haptic.Kind.Tick, true) as Haptic.Plan.Amplitude
        val strong = Haptic.plan(100, Haptic.Kind.Tick, true) as Haptic.Plan.Amplitude
        assertTrue(weak.amplitude >= Haptic.MIN_AMPLITUDE && strong.amplitude == 255)
        val confirm = Haptic.plan(50, Haptic.Kind.Confirm, true) as Haptic.Plan.Amplitude
        val tick = Haptic.plan(50, Haptic.Kind.Tick, true) as Haptic.Plan.Amplitude
        assertTrue(confirm.amplitude > tick.amplitude && confirm.ms > tick.ms)
        // Without amplitude control every level lands on one of the stops, and each stop's level maps back to it.
        Haptic.Step.entries.forEach { assertEquals(it, Haptic.step(it.level)) }
        assertEquals(Haptic.Plan.System(Haptic.Step.Light, Haptic.Kind.Tick), Haptic.plan(20, Haptic.Kind.Tick, false))
        assertEquals(Haptic.Plan.System(Haptic.Step.Strong, Haptic.Kind.Confirm), Haptic.plan(90, Haptic.Kind.Confirm, false))
    }

    @Test fun customAccentIsOptionalOpaqueAndRoundTrips() {
        assertEquals(0xFF123456.toInt(), CustomColors(0x00123456).sanitized().accent)
        assertEquals(0xFFA1B2C3.toInt(), CustomColors.parseHex(" #a1b2c3"))
        assertNull(CustomColors.parseHex("#12345"))
        assertNull(CustomColors.parseHex("#12345G"))
        assertEquals("#A1B2C3", CustomColors.hex(0xFFA1B2C3.toInt()))
        val s = LauncherSettings(custom = CustomColors(0xFF00AA55.toInt())).withHapticLevel(60)
        val back = SettingsCodec.decode(SettingsCodec.encode(s))
        assertEquals(0xFF00AA55.toInt(), back.custom.accent)
        assertEquals(60, back.hapticLevel())
    }

    @Test fun everyDerivedCustomAccentKeepsContrastInBothThemes() {
        // A grid over hue, saturation and lightness, including black, white and pure primaries.
        val bases = buildList {
            for (h in 0 until 360 step 15) for (s in listOf(0f, .3f, .7f, 1f)) for (l in listOf(0f, .2f, .5f, .8f, 1f))
                add(AccentColors.fromHsl(h.toFloat(), s, l))
        }
        bases.forEach { base ->
            val dark = AccentColors.derive(base, true)
            val light = AccentColors.derive(base, false)
            assertTrue("dark ${CustomColors.hex(base)}", AccentColors.contrast(dark, AccentColors.VEIL_DARK) >= 4.5)
            assertTrue("ink on dark ${CustomColors.hex(base)}", AccentColors.contrast(dark, AccentColors.INK_LIGHT) >= 4.5)
            assertTrue("light ${CustomColors.hex(base)}", AccentColors.contrast(light, AccentColors.VEIL_LIGHT) >= 4.5)
            assertTrue("white on light ${CustomColors.hex(base)}", AccentColors.contrast(light, AccentColors.WHITE) >= 4.5)
        }
    }

    @Test fun deriveKeepsTheHueOfAColourThatAlreadyPasses() {
        val deep = 0xFF3F6E9C.toInt() // Frost, light theme
        assertEquals(deep, AccentColors.derive(deep, false))
        val (h0, _, _) = AccentColors.hsl(0xFF2E7D32.toInt())
        val (h1, _, _) = AccentColors.hsl(AccentColors.derive(0xFF2E7D32.toInt(), true))
        assertTrue(kotlin.math.abs(h0 - h1) < 3f)
    }
}

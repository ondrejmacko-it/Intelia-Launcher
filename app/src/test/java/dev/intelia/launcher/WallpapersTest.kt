package dev.intelia.launcher

import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.Wallpapers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpapersTest {
    private val darkInk = 0x15181C // CrystalColors.FgLight: type on a light ground
    private val lightInk = 0xF1F3F5 // CrystalColors.FgDark

    private fun contrast(a: Int, b: Int): Float {
        val (lo, hi) = listOf(Wallpapers.luminance(a), Wallpapers.luminance(b)).sorted()
        return (hi + .05f) / (lo + .05f)
    }

    @Test fun homeTypeIsLegibleOnEveryWallpaperAtEveryDim() {
        for (p in Wallpapers.all) for (dim in 0..Wallpapers.maxDim(p)) {
            val ink = if (Wallpapers.wantsDarkText(p, dim)) darkInk else lightInk
            // The ground carries body text (date, names); the lights only pass under large type with its shadow.
            assertTrue("${p.id} $dim base", contrast(ink, Wallpapers.dimmed(p.base, dim)) >= 4.5f)
            for (l in p.lights) assertTrue("${p.id} $dim light", contrast(ink, Wallpapers.dimmed(l.rgb, dim)) >= 3f)
        }
    }

    @Test fun thereAreTenPlusDistinctLightAndDarkOnes() {
        assertTrue(Wallpapers.all.size >= 10)
        assertEquals(Wallpapers.all.size, Wallpapers.all.map { it.id }.toSet().size)
        assertTrue(Wallpapers.all.count { Wallpapers.wantsDarkText(it, 0) } >= 4)
        assertTrue(Wallpapers.all.count { !Wallpapers.wantsDarkText(it, 0) } >= 4)
    }

    @Test fun unknownWallpaperFallsBackToTheSystemOneAndDimIsBounded() {
        val s = LauncherSettings(wallpaper = "nope", wallDim = 99).sanitized()
        assertEquals("", s.wallpaper)
        assertEquals(Wallpapers.MAX_DIM, s.wallDim)
        assertEquals("ocean", LauncherSettings(wallpaper = "ocean", wallDim = -3).sanitized().wallpaper)
        assertEquals(Wallpapers.LIGHT_MAX_DIM, LauncherSettings(wallpaper = "paper", wallDim = 50).sanitized().wallDim)
        assertEquals(0, LauncherSettings(wallDim = -3).sanitized().wallDim)
        // Absent in older documents: the system wallpaper, undimmed.
        assertEquals("", LauncherSettings().wallpaper)
    }
}

package dev.intelia.launcher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import dev.intelia.launcher.data.Glass
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VeilTest {
    private val acc = Color(0xFFA9C4E0)

    @Test fun levelsStayInTheApprovedDensityBands() {
        for (dark in listOf(true, false)) {
            val light = veilTokens(VeilLevel.Light, dark, acc)
            val dense = veilTokens(VeilLevel.Dense, dark, acc)
            val focus = veilTokens(VeilLevel.Focus, dark, acc)
            assertTrue(dense.top.alpha >= .75f && dense.bottom.alpha <= .855f)
            assertTrue(light.top.alpha >= .6f)
            assertTrue(focus.top.alpha >= .87f && focus.bottom.alpha <= .955f)
            assertTrue(light.bottom.alpha < dense.top.alpha && dense.bottom.alpha < focus.top.alpha)
        }
    }

    @Test fun edgeIsSoftNotNeon() {
        for (dark in listOf(true, false)) {
            val t = veilTokens(VeilLevel.Dense, dark, acc)
            assertTrue(t.edgeHot.alpha <= .6f && t.edgeCold.alpha <= .16f && t.glow.alpha <= .08f)
        }
    }

    @Test fun textStaysLegibleOnTheLightestVeilOverTheWorstWallpaper() {
        for (dark in listOf(true, false)) {
            val t = veilTokens(VeilLevel.Light, dark, acc)
            val wall = if (dark) Color.White else Color.Black
            val fg = if (dark) CrystalColors.FgDark else CrystalColors.FgLight
            for (fill in listOf(t.top, t.bottom)) {
                val ground = fill.compositeOver(wall)
                val (hi, lo) = listOf(fg.luminance(), ground.luminance()).sorted().let { it[1] to it[0] }
                assertTrue("dark=$dark ${(hi + .05f) / (lo + .05f)}", (hi + .05f) / (lo + .05f) >= 4.5f)
            }
        }
    }

    @Test fun edgelessGlassesKeepTextLegibleWithTheirLightIncluded() {
        for (glass in Glass.entries.filter { it != Glass.Veil }) for (dark in listOf(true, false)) for (shift in listOf(0, 5)) {
            val t = veilTokens(VeilLevel.Light, dark, acc, VeilPrefs(shift = shift, glass = glass))
            assertFalse(t.edge)
            val wall = if (dark) Color.White else Color.Black
            val fg = if (dark) CrystalColors.FgDark else CrystalColors.FgLight
            // Worst point of each fill: the sheen at the top, the bloom and the grain's white specks at the bottom.
            val top = t.sheen.compositeOver(t.glow.compositeOver(t.top.compositeOver(wall)))
            val speck = Color.White.copy(alpha = t.grain)
            val bottom = speck.compositeOver(t.bloom.compositeOver(t.bottom.compositeOver(wall)))
            for (ground in listOf(top, bottom)) {
                val (hi, lo) = listOf(fg.luminance(), ground.luminance()).sorted().let { it[1] to it[0] }
                assertTrue("$glass dark=$dark shift=$shift ${(hi + .05f) / (lo + .05f)}", (hi + .05f) / (lo + .05f) >= 4.5f)
            }
        }
    }

    @Test fun veilKeepsItsEdge() {
        assertTrue(veilTokens(VeilLevel.Dense, true, acc).edge)
    }
}

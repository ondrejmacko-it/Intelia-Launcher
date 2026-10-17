package dev.intelia.launcher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import dev.intelia.launcher.data.Glass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VeilTest {
    private val acc = Color(0xFFA9C4E0)

    @Test fun densitiesUseTheApprovedOpacities() {
        assertEquals(listOf(.64f, .80f, .74f), VeilLevel.entries.map { veilAlpha(it) })
        assertEquals(listOf(.48f, .66f, .60f), VeilLevel.entries.map { veilAlpha(it, blurred = true) })
        for (level in VeilLevel.entries) {
            val plain = veilTokens(level, true, acc)
            val blurred = veilTokens(level, true, acc, VeilPrefs(blurred = true))
            // The accent tint adds a few percent on top of the body opacity.
            assertTrue(plain.top.alpha in veilAlpha(level)..veilAlpha(level) + .05f)
            assertTrue(blurred.top.alpha < plain.top.alpha)
        }
    }

    @Test fun presetsAreOneMaterial() {
        for (dark in listOf(true, false)) for (level in VeilLevel.entries) {
            val veil = veilTokens(level, dark, acc)
            // Same body for every preset; Frost is a little milkier, and a dark body grows with the preset's light.
            for (glass in listOf(Glass.Crystal, Glass.Liquid)) {
                val t = veilTokens(level, dark, acc, VeilPrefs(glass = glass)).top
                if (dark) assertTrue(t.alpha > veil.top.alpha) else assertEquals(veil.top, t)
            }
            assertTrue(veilTokens(level, dark, acc, VeilPrefs(glass = Glass.Frost)).top.alpha > veil.top.alpha)
            // Every preset, Matte included, keeps the one crystal edge: light at the top, accent at the bottom.
            for (prefs in Glass.entries.map { VeilPrefs(glass = it) } + VeilPrefs(solid = true)) {
                val t = veilTokens(level, dark, acc, prefs)
                assertTrue(t.edgeTop.alpha > t.edgeMid.alpha && t.edgeBottom.alpha > 0f)
            }
        }
    }

    @Test fun edgeIsSoftNotNeon() {
        for (dark in listOf(true, false)) for (glass in Glass.entries) {
            val t = veilTokens(VeilLevel.Dense, dark, acc, VeilPrefs(glass = glass))
            // Alpha is stored in 8 bits: a little headroom over the design values .9 / .24 / .05.
            assertTrue(t.edgeTop.alpha <= .91f && t.edgeBottom.alpha <= .25f && t.glow.alpha <= .06f)
        }
    }

    @Test fun textStaysLegibleOverTheWorstWallpaperWithThePresetLightIncluded() {
        for (level in listOf(VeilLevel.Light, VeilLevel.Focus)) for (glass in Glass.entries) for (dark in listOf(true, false)) for (shift in listOf(0, 5)) {
            assertLegible(veilTokens(level, dark, acc, VeilPrefs(shift = shift, glass = glass)), dark, "$level $glass dark=$dark shift=$shift")
        }
    }

    @Test fun plateGlassIsBolderAndStillLegible() {
        for (glass in Glass.entries.filter { it != Glass.Veil }) for (dark in listOf(true, false)) for (shift in listOf(0, 5)) {
            val panel = veilTokens(VeilLevel.Light, dark, acc, VeilPrefs(shift = shift, glass = glass))
            val t = plateGlass(glass, dark, acc, shift)
            assertTrue(t.top.alpha > panel.top.alpha && t.edgeTop.alpha >= panel.edgeTop.alpha && t.streak.alpha >= panel.streak.alpha)
            assertLegible(t, dark, "$glass dark=$dark shift=$shift")
        }
    }

    @Test fun depthScrimIsTintedBaseAtFiftyTwoPercent() {
        for (dark in listOf(true, false)) {
            val s = veilScrimColor(dark, acc)
            assertEquals(.08f + .52f * .92f, s.alpha, .005f)
            assertEquals(dark, s.copy(alpha = 1f).luminance() < .5f)
        }
    }

    @Test fun backdropBlursOnceAtTheStrongerDepth() {
        assertEquals(0, backdropBlurPx(0f, 0f, 3f))
        assertEquals(15, backdropBlurPx(0f, 1f, 3f))
        assertEquals(54, backdropBlurPx(1f, 1f, 3f)) // a sheet over Search does not add a second blur
        assertEquals(27, backdropBlurPx(.5f, 0f, 3f))
    }

    /** ≥ 4.5:1 at the worst point of each fill: the sheen and facets at the top, the grain's white specks and the inner glow anywhere. */
    private fun assertLegible(t: VeilTokens, dark: Boolean, what: String) {
        val wall = if (dark) Color.White else Color.Black
        val fg = if (dark) CrystalColors.FgDark else CrystalColors.FgLight
        val body = t.glow.compositeOver(t.top.compositeOver(wall))
        val lit = t.innerGlow.compositeOver(t.streak.compositeOver(t.sheen.compositeOver(body)))
        val speck = Color.White.copy(alpha = t.grain).compositeOver(lit)
        for (ground in listOf(body, lit, speck)) {
            val (hi, lo) = listOf(fg.luminance(), ground.luminance()).sorted().let { it[1] to it[0] }
            assertTrue("$what ${(hi + .05f) / (lo + .05f)}", (hi + .05f) / (lo + .05f) >= 4.5f)
        }
    }
}

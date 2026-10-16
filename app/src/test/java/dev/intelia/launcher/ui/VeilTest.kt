package dev.intelia.launcher.ui

import androidx.compose.ui.graphics.Color
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
            assertTrue(focus.top.alpha >= .88f && focus.bottom.alpha <= .955f)
            assertTrue(light.bottom.alpha < dense.top.alpha && dense.bottom.alpha < focus.top.alpha)
        }
    }

    @Test fun edgeIsSoftNotNeon() {
        for (dark in listOf(true, false)) {
            val t = veilTokens(VeilLevel.Dense, dark, acc)
            assertTrue(t.edgeHot.alpha <= .6f && t.edgeCold.alpha <= .1f && t.glow.alpha <= .08f)
        }
    }
}

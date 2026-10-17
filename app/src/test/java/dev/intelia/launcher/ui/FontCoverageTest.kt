package dev.intelia.launcher.ui

import java.awt.Font
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every bundled clock and date face draws Czech: a missing glyph would fall back to another font mid-word. */
class FontCoverageTest {
    private val czech = "ěščřžýáíéůúťďňó ĚŠČŘŽÝÁÍÉŮÚŤĎŇÓ 0123456789:.–"

    @Test fun everyBundledFontHasCzechGlyphs() {
        val dir = listOf(File("src/main/res/font"), File("app/src/main/res/font")).first { it.isDirectory }
        val fonts = dir.listFiles { f -> f.extension == "ttf" }.orEmpty()
        assertEquals(7, fonts.size)
        for (file in fonts) {
            val font = Font.createFont(Font.TRUETYPE_FONT, file)
            val missing = czech.filterIndexed { _, ch -> !font.canDisplay(ch) }
            assertTrue("${file.name} lacks \"$missing\"", missing.isEmpty())
        }
    }
}

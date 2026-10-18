package dev.intelia.launcher.ui

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.intelia.launcher.R

// Crystal design system (D10, docs/DESIGN_SYSTEM.md 4.1–4.4). Since phase B2 Home uses the palette,
// type and LENS sizes; ARC, PULSE and Studio still draw with Ink until their phases (B3–B5).

/** Fixed colours of 4.1; accents are chosen per user through [accentColor]. */
object CrystalColors {
    val FgDark = Color(0xFFF1F3F5)
    val FgLight = Color(0xFF15181C)
    val MatDark = Color(0xFF111317).copy(alpha = .90f)
    val MatLight = Color(0xFFF6F7F8).copy(alpha = .92f)
    val FrostDark = Color(0xFFA9C4E0)
    val FrostLight = Color(0xFF3F6E9C)
    val SilverDark = Color(0xFFC9CED4)
    val SilverLight = Color(0xFF59616A)
    val Capsule = Color.Black
}

@Immutable
data class CrystalPalette(
    val dark: Boolean,
    val fg: Color,
    val fg2: Color,
    val fg3: Color,
    val mat: Color,
    val matEdge: Color,
    val line: Color,
    val acc: Color,
    val accInk: Color,
    val capsule: Color = CrystalColors.Capsule,
    /** The arc hairline sits on the wallpaper, so it stays light in both themes. */
    val arc: Color = Color.White.copy(alpha = .22f),
)

fun crystalPalette(dark: Boolean, acc: Color): CrystalPalette {
    val fg = if (dark) CrystalColors.FgDark else CrystalColors.FgLight
    return CrystalPalette(
        dark = dark,
        fg = fg,
        fg2 = fg.copy(alpha = .62f),
        fg3 = fg.copy(alpha = .38f),
        mat = if (dark) CrystalColors.MatDark else CrystalColors.MatLight,
        matEdge = Color.White.copy(alpha = if (dark) .11f else .70f),
        line = (if (dark) Color.White else Color.Black).copy(alpha = .08f),
        acc = acc,
        accInk = if (acc.luminanceApprox() > .45f) Color(0xFF0D1620) else Color.White,
    )
}

/** Rec. 709 luminance of the sRGB values; good enough to pick ink on the accent. */
private fun Color.luminanceApprox() = .2126f * red + .7152f * green + .0722f * blue

/** 4.3: 4 dp grid, gutters, icon sizes, radii. */
object CrystalDims {
    val gutter = 16.dp
    val studioGutter = 20.dp
    val touch = 48.dp
    val iconHome = 40.dp
    val iconRow = 36.dp
    val iconPreview = 28.dp
    val lensRing = 44.dp
    val lensStroke = 1.5.dp
    /** 84 dp wide so the LENS-row icons at ±62 dp (48 dp touch) keep their inner edge; 100 dp tall for the thumb. */
    val lensTouchW = 84.dp
    val lensTouch = 100.dp
    val radiusSheet = 22.dp
    val radiusControl = 12.dp
    val radiusField = 24.dp
    val arcRadiusLens = 118.dp
    val arcRadiusGlobal = 104.dp
    val arcStroke = 1.dp
    val slot = 44.dp
    val capsuleHeight = 30.dp
    val capsuleMin = 30.dp
    val capsuleMax = 220.dp
    val studioTile = 36.dp
}

/** Roboto Flex at one weight/width pair; each pair is its own family because axes are fixed per [Font]. */
internal fun flex(weight: Int, width: Float) = FontFamily(
    Font(
        R.font.roboto_flex,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.width(width)),
    ),
)

/** 4.2: one family, precision from the weight and width axes. */
object CrystalType {
    val clock = TextStyle(fontFamily = flex(220, 118f), fontSize = 84.sp, lineHeight = 84.sp, fontFeatureSettings = "tnum")
    val date = TextStyle(fontFamily = flex(450, 100f), fontSize = 12.sp, letterSpacing = 2.2.sp)
    val title = TextStyle(fontFamily = flex(500, 112f), fontSize = 28.sp, lineHeight = 34.sp)
    val arcName = TextStyle(fontFamily = flex(550, 110f), fontSize = 17.sp, lineHeight = 22.sp)
    val body = TextStyle(fontFamily = flex(400, 100f), fontSize = 16.sp, lineHeight = 22.sp)
    val bodyStrong = TextStyle(fontFamily = flex(500, 100f), fontSize = 16.sp, lineHeight = 22.sp)
    val bodySmall = TextStyle(fontFamily = flex(400, 100f), fontSize = 15.sp, lineHeight = 20.sp)
    val caption = TextStyle(fontFamily = flex(400, 100f), fontSize = 13.sp, lineHeight = 18.sp)
    /** Section labels: the only caps text besides the date. */
    val label = TextStyle(fontFamily = flex(600, 100f), fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.2.sp)
}

val LocalCrystal = staticCompositionLocalOf { crystalPalette(dark = true, acc = CrystalColors.FrostDark) }

/** Provides the Crystal palette for the settings' theme and accent; used inside [InteliaTheme] from phase B2. */
@Composable
fun CrystalTheme(dark: Boolean, acc: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCrystal provides crystalPalette(dark, acc), content = content)
}

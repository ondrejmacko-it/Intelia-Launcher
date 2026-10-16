package dev.intelia.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class Glyph { Search, Apps, Flow, Back, Studio, Close, Bell, Bolt, Torch, Plus }

/** Original monoline glyphs on a 24-unit grid; no icon font or bitmap assets. */
@Composable
fun HomeGlyph(glyph: Glyph, label: String?, modifier: Modifier = Modifier, tint: Color = LocalInk.current.ink, size: Dp = 22.dp) {
    Canvas(modifier.size(size).semantics { if (label != null) contentDescription = label }) {
        val unit = this.size.width / 24f
        val w = 1.6f * unit
        fun point(x: Float, y: Float) = Offset(x * unit, y * unit)
        fun line(x: Float, y: Float, endX: Float, endY: Float) = drawLine(tint, point(x, y), point(endX, endY), w, StrokeCap.Round)
        fun ring(x: Float, y: Float, r: Float) = drawCircle(tint, r * unit, point(x, y), style = Stroke(w))
        when (glyph) {
            Glyph.Search -> { ring(10f, 10f, 6.5f); line(15f, 15f, 21f, 21f) }
            Glyph.Apps -> for (x in listOf(4f, 15f)) for (y in listOf(4f, 15f)) {
                drawRoundRect(tint, point(x, y), Size(5f * unit, 5f * unit), CornerRadius(2.5f * unit), style = Stroke(w))
            }
            // Two beads on a thread: the FLOW sign.
            Glyph.Flow -> { line(2f, 12f, 22f, 12f); drawCircle(tint, 3.2f * unit, point(8f, 12f)); ring(16f, 12f, 3.2f) }
            Glyph.Back -> { line(20f, 12f, 4f, 12f); line(4f, 12f, 10f, 6f); line(4f, 12f, 10f, 18f) }
            // Studio: a ring with its tide dash.
            Glyph.Studio -> { ring(12f, 10f, 6f); line(9f, 20f, 15f, 20f) }
            Glyph.Close -> { line(6f, 6f, 18f, 18f); line(18f, 6f, 6f, 18f) }
            Glyph.Plus -> { line(12f, 5f, 12f, 19f); line(5f, 12f, 19f, 12f) }
            Glyph.Bell -> {
                val path = Path().apply {
                    moveTo(6f * unit, 17f * unit); lineTo(6f * unit, 11f * unit)
                    cubicTo(6f * unit, 7f * unit, 9f * unit, 5f * unit, 12f * unit, 5f * unit)
                    cubicTo(15f * unit, 5f * unit, 18f * unit, 7f * unit, 18f * unit, 11f * unit)
                    lineTo(18f * unit, 17f * unit); close()
                }
                drawPath(path, tint, style = Stroke(w, join = StrokeJoin.Round))
                line(10f, 20f, 14f, 20f)
            }
            Glyph.Bolt -> {
                val path = Path().apply {
                    moveTo(13f * unit, 3f * unit); lineTo(6f * unit, 13f * unit); lineTo(12f * unit, 13f * unit)
                    lineTo(11f * unit, 21f * unit); lineTo(18f * unit, 11f * unit); lineTo(12f * unit, 11f * unit); close()
                }
                drawPath(path, tint, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            Glyph.Torch -> {
                line(8f, 4f, 16f, 4f); line(8f, 4f, 9f, 9f); line(16f, 4f, 15f, 9f)
                drawRoundRect(tint, point(9f, 9f), Size(6f * unit, 11f * unit), CornerRadius(1.5f * unit), style = Stroke(w))
                line(12f, 12f, 12f, 15f)
            }
        }
    }
}

/** Paired unequal strokes: the Intelia mark. */
@Composable
fun InteliaMark(modifier: Modifier = Modifier, ink: Color = LocalInk.current.ink, accent: Color = LocalInk.current.accent) {
    Canvas(modifier) {
        val w = size.width * .17f
        drawLine(accent, Offset(size.width * .29f, size.height * .15f), Offset(size.width * .29f, size.height * .85f), w, StrokeCap.Round)
        drawLine(ink, Offset(size.width * .69f, size.height * .15f), Offset(size.width * .69f, size.height * .65f), w, StrokeCap.Round)
        drawLine(ink, Offset(size.width * .69f, size.height * .65f), Offset(size.width * .55f, size.height * .85f), w, StrokeCap.Round)
    }
}

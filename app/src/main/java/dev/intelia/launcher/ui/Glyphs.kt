package dev.intelia.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class Glyph { Search, Apps, Flow, Back, Settings }

/** Original monoline glyphs on a 24-unit grid; no icon font or bitmap assets. */
@Composable
fun HomeGlyph(glyph: Glyph, label: String?, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    val background = MaterialTheme.colorScheme.surface
    Canvas(modifier.size(22.dp).semantics { if (label != null) contentDescription = label }) {
        val unit = size.width / 24f
        fun point(x: Float, y: Float) = Offset(x * unit, y * unit)
        fun line(x: Float, y: Float, endX: Float, endY: Float) =
            drawLine(ink, point(x, y), point(endX, endY), 1.7f * unit, StrokeCap.Round)
        when (glyph) {
            Glyph.Search -> {
                drawCircle(ink, 6.5f * unit, point(10f, 10f), style = Stroke(1.7f * unit))
                line(15f, 15f, 21f, 21f)
            }
            Glyph.Apps -> for (x in listOf(4f, 15f)) for (y in listOf(4f, 15f)) {
                drawRoundRect(ink, point(x, y), Size(5f * unit, 5f * unit), CornerRadius(unit))
            }
            Glyph.Flow -> {
                val path = Path().apply {
                    moveTo(13f * unit, 3f * unit); lineTo(6f * unit, 13f * unit); lineTo(12f * unit, 13f * unit)
                    lineTo(11f * unit, 21f * unit); lineTo(18f * unit, 11f * unit); lineTo(12f * unit, 11f * unit); close()
                }
                drawPath(path, ink, style = Stroke(1.6f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            Glyph.Back -> { line(20f, 12f, 4f, 12f); line(4f, 12f, 11f, 5f); line(4f, 12f, 11f, 19f) }
            Glyph.Settings -> {
                for (y in listOf(5f, 12f, 19f)) line(3f, y, 21f, y)
                for ((x, y) in listOf(8f to 5f, 16f to 12f, 8f to 19f)) {
                    drawCircle(background, 3f * unit, point(x, y))
                    drawCircle(ink, 2.5f * unit, point(x, y), style = Stroke(1.7f * unit))
                }
            }
        }
    }
}

/** Paired unequal strokes: the Intelia mark, shared with the clock separator. */
@Composable
fun InteliaMark(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        val w = size.width * .17f
        drawLine(accent, Offset(size.width * .29f, size.height * .15f), Offset(size.width * .29f, size.height * .85f), w, StrokeCap.Round)
        drawLine(ink, Offset(size.width * .69f, size.height * .15f), Offset(size.width * .69f, size.height * .65f), w, StrokeCap.Round)
        drawLine(ink, Offset(size.width * .69f, size.height * .65f), Offset(size.width * .55f, size.height * .85f), w, StrokeCap.Round)
    }
}

private val clockFormat = DateTimeFormatter.ofPattern("HH:mm")

/** Monoline digits drawn as paths; no bundled font and no per-second work. */
@Composable
fun ClockFace(now: LocalTime, compact: Boolean, modifier: Modifier = Modifier) {
    val value = now.format(clockFormat)
    val ink = MaterialTheme.colorScheme.onSurface
    Canvas(modifier.widthIn(max = if (compact) 200.dp else 268.dp).fillMaxWidth(.82f).aspectRatio(3.15f)
        .semantics { contentDescription = value }) {
        val unit = size.width / 232f
        val top = (size.height - 64f * unit) / 2f
        value.filter(Char::isDigit).forEachIndexed { index, digit ->
            val x = (index * 51f + if (index >= 2) 28f else 0f) * unit
            val glyph = digitPath(digit)
            glyph.transform(Matrix().apply { translate(x, top); scale(unit, unit) })
            drawPath(glyph, ink, style = Stroke(2.4f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        drawLine(ink.copy(alpha = .8f), Offset(106f * unit, top + 19f * unit), Offset(106f * unit, top + 27f * unit), 2.4f * unit, StrokeCap.Round)
        drawLine(ink.copy(alpha = .55f), Offset(113f * unit, top + 38f * unit), Offset(113f * unit, top + 45f * unit), 2.4f * unit, StrokeCap.Round)
    }
}

/** 36 × 64 local coordinates; all glyphs share one baseline. */
private fun digitPath(digit: Char) = Path().apply {
    when (digit) {
        '0' -> { moveTo(18f, 1f); cubicTo(-5f, 1f, -5f, 63f, 18f, 63f); cubicTo(41f, 63f, 41f, 1f, 18f, 1f) }
        '1' -> { moveTo(5f, 14f); lineTo(20f, 1f); lineTo(20f, 63f) }
        '2' -> { moveTo(1f, 14f); cubicTo(3f, -6f, 36f, -3f, 35f, 15f); cubicTo(35f, 29f, 10f, 40f, 1f, 63f); lineTo(36f, 63f) }
        '3' -> { moveTo(1f, 3f); lineTo(34f, 3f); lineTo(16f, 29f); cubicTo(46f, 24f, 43f, 64f, 17f, 63f); cubicTo(7f, 63f, 3f, 60f, 1f, 57f) }
        '4' -> { moveTo(27f, 63f); lineTo(27f, 1f); lineTo(1f, 43f); lineTo(37f, 43f) }
        '5' -> { moveTo(34f, 2f); lineTo(5f, 2f); lineTo(2f, 29f); cubicTo(43f, 17f, 46f, 64f, 17f, 63f); cubicTo(8f, 63f, 3f, 60f, 1f, 57f) }
        '6' -> { moveTo(31f, 2f); cubicTo(9f, -3f, -4f, 25f, 2f, 50f); cubicTo(9f, 77f, 43f, 59f, 34f, 39f); cubicTo(28f, 25f, 9f, 28f, 1f, 38f) }
        '7' -> { moveTo(0f, 2f); lineTo(36f, 2f); lineTo(9f, 63f) }
        '8' -> { moveTo(18f, 31f); cubicTo(-15f, 20f, 2f, -8f, 26f, 3f); cubicTo(46f, 15f, 26f, 28f, 18f, 31f); cubicTo(-15f, 43f, 1f, 75f, 28f, 60f); cubicTo(46f, 47f, 30f, 35f, 18f, 31f) }
        '9' -> { moveTo(5f, 61f); cubicTo(28f, 67f, 42f, 38f, 34f, 13f); cubicTo(27f, -12f, -6f, 4f, 2f, 25f); cubicTo(8f, 40f, 27f, 36f, 35f, 26f) }
    }
}

package dev.intelia.launcher.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import dev.intelia.launcher.R
import dev.intelia.launcher.data.ClockSeparator
import dev.intelia.launcher.data.DateFormat
import dev.intelia.launcher.data.DatePart
import dev.intelia.launcher.data.TextCase
import dev.intelia.launcher.data.TextLook
import dev.intelia.launcher.data.TypeFace
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Hours and minutes as drawn: 24 h with a leading zero, or 12 h without one (no AM/PM, §5.12). */
fun clockDigits(now: LocalTime, hours12: Boolean): Pair<String, String> =
    (if (hours12) "${(now.hour + 11) % 12 + 1}" else "%02d".format(now.hour)) to "%02d".format(now.minute)

fun separatorText(separator: ClockSeparator): String = when (separator) {
    ClockSeparator.Colon -> ":"
    ClockSeparator.Dot -> "·"
    ClockSeparator.Bullet -> "•"
    ClockSeparator.Bar -> "|"
    ClockSeparator.Space -> " "
    ClockSeparator.None -> ""
}

/** The date line in [format]; [DateFormat.Custom] joins the parts chosen in [look], then [TextLook.case] applies. */
fun dateText(now: LocalDateTime, format: DateFormat, look: TextLook = TextLook(), locale: Locale = Locale.getDefault()): String {
    fun f(p: String) = now.format(DateTimeFormatter.ofPattern(p, locale))
    val text = when (format) {
        DateFormat.Long -> f("EEEE d. MMMM")
        DateFormat.Short -> f("d. M.")
        DateFormat.Weekday -> f("EEEE")
        DateFormat.Custom -> listOfNotNull(
            when (look.weekday) { DatePart.None -> null; DatePart.Short, DatePart.Number -> f("EEE"); DatePart.Long -> f("EEEE") },
            when {
                look.month == DatePart.Number -> f(if (look.day) "d. M." else "M.")
                look.day -> f("d.")
                else -> null
            },
            // With the day, the month takes the genitive ("17. října"); alone, the nominative ("říjen").
            when (look.month) { DatePart.None, DatePart.Number -> null; DatePart.Short -> f("LLL"); DatePart.Long -> f(if (look.day) "MMMM" else "LLLL") },
            if (look.year) f("yyyy") else null,
        ).joinToString(" ").ifEmpty { f("d. M.") }
    }
    return when (look.case) {
        TextCase.Upper -> text.uppercase(locale)
        TextCase.Lower -> text.lowercase(locale)
        TextCase.Auto -> if (format == DateFormat.Long || format == DateFormat.Weekday) text.uppercase(locale) else text
    }
}

/** Font family for [face] at [weight]; null = the caller's own ([TypeFace.Style]). Variable faces get the weight axis. */
fun faceFamily(face: TypeFace, weight: Int): FontFamily? {
    val w = weight.coerceIn(100, 900)
    fun variable(res: Int) = FontFamily(Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w))))
    return when (face) {
        TypeFace.Style -> null
        TypeFace.Flex -> flex(w, 118f)
        TypeFace.Inter -> variable(R.font.inter)
        TypeFace.SpaceGrotesk -> variable(R.font.space_grotesk)
        TypeFace.DmSerif -> FontFamily(Font(R.font.dm_serif_display))
        TypeFace.JetBrainsMono -> variable(R.font.jetbrains_mono)
        TypeFace.BodoniModa -> variable(R.font.bodoni_moda)
        TypeFace.Unbounded -> variable(R.font.unbounded)
    }
}

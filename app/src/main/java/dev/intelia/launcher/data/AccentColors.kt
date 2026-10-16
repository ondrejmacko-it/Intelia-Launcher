package dev.intelia.launcher.data

/**
 * The accent hues as ARGB (§5.12): a pale variant for the dark theme and a deep one for the light theme, each
 * at least 4.5:1 against the Veil base of its theme ([VEIL_DARK], [VEIL_LIGHT]); checked by a JVM test.
 * [Accent.Dynamic] comes from the wallpaper at run time and falls back to Terracotta here.
 */
object AccentColors {
    const val VEIL_DARK = 0xFF15171B.toInt()
    const val VEIL_LIGHT = 0xFFF5F6F7.toInt()
    const val INK_DARK = 0xFFF1ECE4.toInt()
    const val INK_LIGHT = 0xFF1C1814.toInt()

    fun argb(accent: Accent, dark: Boolean): Int {
        val (pale, deep) = when (accent) {
            Accent.Dynamic, Accent.Terracotta -> 0xFFE39A78 to 0xFFA84B29
            Accent.Mono -> INK_DARK.toLong() to INK_LIGHT.toLong()
            Accent.Sage -> 0xFFA9C9B6 to 0xFF3F6B57
            Accent.Slate -> 0xFFAFC3E6 to 0xFF3C5A86
            Accent.Frost -> 0xFFA9C4E0 to 0xFF3F6E9C
            Accent.Silver -> 0xFFC9CED4 to 0xFF59616A
            Accent.Rose -> 0xFFF4A6B8 to 0xFFB23A5A
            Accent.Coral -> 0xFFF7A58C to 0xFFB4472E
            Accent.Amber -> 0xFFF2C46D to 0xFF8A5A00
            Accent.Lime -> 0xFFC3DE7C to 0xFF4F6B12
            Accent.Mint -> 0xFF8FE0BE to 0xFF1F7355
            Accent.Teal -> 0xFF7FD3D3 to 0xFF1A6E72
            Accent.Sky -> 0xFF8CCBF2 to 0xFF1F6A9E
            Accent.Indigo -> 0xFFAEB4F5 to 0xFF4A4FB0
            Accent.Violet -> 0xFFC6A8F2 to 0xFF6E46AE
            Accent.Orchid -> 0xFFE7A6E0 to 0xFF94398C
        }
        return (if (dark) pale else deep).toInt()
    }

    /** WCAG relative luminance of an opaque ARGB colour. */
    fun luminance(argb: Int): Double {
        fun channel(shift: Int): Double {
            val c = (argb shr shift and 0xFF) / 255.0
            return if (c <= .03928) c / 12.92 else Math.pow((c + .055) / 1.055, 2.4)
        }
        return .2126 * channel(16) + .7152 * channel(8) + .0722 * channel(0)
    }

    fun contrast(a: Int, b: Int): Double {
        val (hi, lo) = luminance(a).let { la -> luminance(b).let { lb -> maxOf(la, lb) to minOf(la, lb) } }
        return (hi + .05) / (lo + .05)
    }
}

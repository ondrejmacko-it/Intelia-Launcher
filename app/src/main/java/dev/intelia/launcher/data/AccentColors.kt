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

    /**
     * D25: the colour drawn for an owner-made [base] in one theme. Hue and saturation are kept; lightness moves (up in
     * the dark theme, down in the light one) until the accent has [MIN_CONTRAST] against that theme's Veil base and
     * the text on it ([INK_LIGHT] on pale accents, white on deep ones, as `inkPalette` picks at luminance .3) has it too.
     */
    fun derive(base: Int, dark: Boolean): Int {
        val (h, s, l0) = hsl(base or OPAQUE)
        fun ok(c: Int): Boolean = if (dark) contrast(c, VEIL_DARK) >= MIN_CONTRAST && luminance(c) > PALE_LUMINANCE &&
            contrast(c, INK_LIGHT) >= MIN_CONTRAST
        else contrast(c, VEIL_LIGHT) >= MIN_CONTRAST && contrast(c, WHITE) >= MIN_CONTRAST && luminance(c) <= PALE_LUMINANCE
        var l = l0
        repeat(101) {
            val c = fromHsl(h, s, l)
            if (ok(c)) return c
            l = (if (dark) l + .01f else l - .01f).coerceIn(0f, 1f)
        }
        // Saturated hues that never pass (rare): fall back to the theme's ink-like extreme of the same hue.
        return fromHsl(h, s * .5f, if (dark) .85f else .25f).takeIf(::ok) ?: if (dark) INK_DARK else INK_LIGHT
    }

    /** Hue 0–360, saturation and lightness 0–1 of an ARGB colour. */
    fun hsl(argb: Int): Triple<Float, Float, Float> {
        val r = (argb shr 16 and 0xFF) / 255f
        val g = (argb shr 8 and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val l = (max + min) / 2f
        if (max == min) return Triple(0f, 0f, l)
        val d = max - min
        val s = if (l > .5f) d / (2f - max - min) else d / (max + min)
        val h = when (max) {
            r -> (g - b) / d + (if (g < b) 6f else 0f)
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        } * 60f
        return Triple(h, s, l)
    }

    /** Opaque ARGB from hue 0–360, saturation and lightness 0–1. */
    fun fromHsl(h: Float, s: Float, l: Float): Int {
        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val hp = ((h % 360f + 360f) % 360f) / 60f
        val x = c * (1f - kotlin.math.abs(hp % 2f - 1f))
        val (r, g, b) = when (hp.toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val m = l - c / 2f
        fun ch(v: Float) = ((v + m) * 255f + .5f).toInt().coerceIn(0, 255)
        return OPAQUE or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }

    const val OPAQUE = 0xFF000000.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
    const val MIN_CONTRAST = 4.5
    /** Above this luminance `inkPalette` puts dark ink on the accent, at or below it white. */
    const val PALE_LUMINANCE = .3

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

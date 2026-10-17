package dev.intelia.launcher.data

import kotlin.math.pow

/** One soft light in a wallpaper: an opaque RGB colour fading out from its centre (fractions of the screen) over [r] × its longer side. */
data class WallLight(val rgb: Int, val x: Float, val y: Float, val r: Float)

/**
 * One of Intelia's own wallpapers (D13 ④): a ground and two or three soft lights, drawn by the launcher. No
 * permission, no image asset, nothing animated; the system wallpaper stays untouched.
 */
data class WallpaperPreset(val id: String, val base: Int, val lights: List<WallLight>)

object Wallpapers {
    /** Most a wallpaper may be darkened, %. */
    const val MAX_DIM = 60

    /**
     * Most a light preset may be darkened, %: further on its ground turns mid grey, where neither dark nor light
     * type reaches 4.5:1 (WallpapersTest). A darker look is a dark preset.
     */
    const val LIGHT_MAX_DIM = 35

    /** On the system wallpaper only its hint is known: from this dim on, a light wallpaper takes light type. */
    const val SYSTEM_LIGHT_TEXT_DIM = 30

    /** Above this relative luminance a ground takes dark type (where light and dark type have equal contrast). */
    private const val DARK_TEXT_ABOVE = .18f

    val all: List<WallpaperPreset> = listOf(
        WallpaperPreset("night", 0x0E1116, listOf(WallLight(0x1E2A44, .2f, .15f, .7f), WallLight(0x2A1E3A, .85f, .8f, .6f))),
        WallpaperPreset("ocean", 0x0A1A24, listOf(WallLight(0x0F3A4A, .3f, .3f, .7f), WallLight(0x123048, .8f, .85f, .6f))),
        WallpaperPreset("forest", 0x0D1A14, listOf(WallLight(0x1C3A2A, .25f, .2f, .7f), WallLight(0x2A3A1E, .8f, .75f, .55f))),
        WallpaperPreset("aurora", 0x0B1220, listOf(WallLight(0x14453F, .3f, .2f, .6f), WallLight(0x2B2560, .75f, .55f, .6f),
            WallLight(0x10303A, .4f, .9f, .5f))),
        WallpaperPreset("dusk", 0x1A1424, listOf(WallLight(0x4A2A48, .8f, .15f, .6f), WallLight(0x2A2050, .2f, .7f, .7f))),
        WallpaperPreset("ember", 0x1A0F0D, listOf(WallLight(0x4A2318, .75f, .25f, .6f), WallLight(0x3A1A24, .2f, .85f, .6f))),
        WallpaperPreset("graphite", 0x16181B, listOf(WallLight(0x2A2D33, .5f, .1f, .8f), WallLight(0x202226, .5f, .95f, .6f))),
        WallpaperPreset("mist", 0xDDE2E8, listOf(WallLight(0xF2F4F7, .3f, .2f, .6f), WallLight(0xC8D2DE, .8f, .85f, .6f))),
        WallpaperPreset("paper", 0xECEAE6, listOf(WallLight(0xF7F6F3, .5f, .2f, .7f), WallLight(0xDCD8D2, .5f, .95f, .6f))),
        WallpaperPreset("sand", 0xE8DFD2, listOf(WallLight(0xF5EEE3, .25f, .25f, .6f), WallLight(0xD9C9B4, .8f, .8f, .6f))),
        WallpaperPreset("dawn", 0xEED9D3, listOf(WallLight(0xF6E6D8, .3f, .2f, .6f), WallLight(0xD8D2EC, .8f, .8f, .65f))),
        WallpaperPreset("ice", 0xD8E8EE, listOf(WallLight(0xEEF6F8, .2f, .3f, .6f), WallLight(0xBFD8E4, .85f, .75f, .6f))),
        WallpaperPreset("sage", 0xD9E2D6, listOf(WallLight(0xEEF2EA, .3f, .25f, .6f), WallLight(0xC3D2BE, .75f, .8f, .6f))),
    )

    fun byId(id: String): WallpaperPreset? = all.firstOrNull { it.id == id }

    /** The dim limit of [preset]; the system wallpaper (null) takes the general one. */
    fun maxDim(preset: WallpaperPreset?): Int =
        if (preset != null && luminance(preset.base) > DARK_TEXT_ABOVE) LIGHT_MAX_DIM else MAX_DIM

    /** Home type on [preset] darkened by [dim] %: dark only while its ground stays light. */
    fun wantsDarkText(preset: WallpaperPreset, dim: Int): Boolean = luminance(dimmed(preset.base, dim)) > DARK_TEXT_ABOVE

    /** [rgb] under [dim] % of black. */
    fun dimmed(rgb: Int, dim: Int): Int {
        val k = 1f - dim.coerceIn(0, 100) / 100f
        fun ch(shift: Int) = (((rgb shr shift) and 0xFF) * k).toInt().coerceIn(0, 255)
        return (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    /** WCAG relative luminance of an RGB colour. */
    fun luminance(rgb: Int): Float {
        fun ch(shift: Int): Float {
            val c = ((rgb shr shift) and 0xFF) / 255f
            return if (c <= .03928f) c / 12.92f else ((c + .055f) / 1.055f).pow(2.4f)
        }
        return .2126f * ch(16) + .7152f * ch(8) + .0722f * ch(0)
    }
}

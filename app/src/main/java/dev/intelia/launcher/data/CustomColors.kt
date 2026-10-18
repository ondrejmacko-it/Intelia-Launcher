package dev.intelia.launcher.data

import kotlinx.serialization.Serializable

/**
 * Owner-made colours (D25). Step 1: one main [accent] (opaque ARGB base, null = use the preset accent). It overrides
 * [LauncherSettings.accent] while set and leaves it untouched, so going back to a preset restores the old look.
 * Later steps add per-place and component colours and whole themes here, never to the preset enum.
 */
@Serializable
data class CustomColors(val accent: Int? = null) {
    fun sanitized(): CustomColors = CustomColors(accent?.let { it or AccentColors.OPAQUE })

    companion object {
        /** "#RRGGBB" (or "RRGGBB", any case) to an opaque ARGB; anything else is null. */
        fun parseHex(text: String): Int? {
            val t = text.trim().removePrefix("#")
            if (t.length != 6 || t.any { it.digitToIntOrNull(16) == null }) return null
            return AccentColors.OPAQUE or t.toInt(16)
        }

        fun hex(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)
    }
}

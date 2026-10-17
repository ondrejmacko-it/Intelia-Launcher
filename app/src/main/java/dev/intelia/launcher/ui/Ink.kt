package dev.intelia.launcher.ui

import android.app.Activity
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import dev.intelia.launcher.data.Accent
import dev.intelia.launcher.data.AccentColors
import dev.intelia.launcher.data.Haptics
import dev.intelia.launcher.data.IconShape
import dev.intelia.launcher.data.IconStyle
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.Motion
import dev.intelia.launcher.data.PaletteTarget
import dev.intelia.launcher.data.ThemeMode
import dev.intelia.launcher.data.Wallpapers

/**
 * Ink: paper, ink and one accent. No cards and no elevation; structure comes from hairlines,
 * type and the two foci of Home (PULSE at the camera, LENS at the thumb).
 */
@Immutable
data class InkPalette(
    val paper: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val line: Color,
    val accent: Color,
    val onAccent: Color,
    val danger: Color,
    val night: Color,
    val onNight: Color,
    val dark: Boolean,
)

private val PaperLight = Color(0xFFEFEBE4)
private val InkLight = Color(0xFF1C1814)
private val PaperDark = Color(0xFF141210)
private val InkDark = Color(0xFFF1ECE4)

fun inkPalette(dark: Boolean, accent: Color): InkPalette {
    val ink = if (dark) InkDark else InkLight
    return InkPalette(
        paper = if (dark) PaperDark else PaperLight,
        ink = ink,
        muted = ink.copy(alpha = if (dark) .72f else .68f),
        faint = ink.copy(alpha = .46f),
        line = ink.copy(alpha = .14f),
        accent = accent,
        // Ink on light accents (all dark-theme accents), white on deep ones; both keep 4.5:1.
        onAccent = if (accent.luminance() > .3f) InkLight else Color.White,
        danger = if (dark) Color(0xFFE58C78) else Color(0xFFA23B2A),
        night = Color(0xFF0E0D0C),
        onNight = InkDark,
        dark = dark,
    )
}

/** Type families in one place; bundled faces replace the system fallbacks here. */
object InkFonts {
    val serif: FontFamily = FontFamily.Serif
    val sans: FontFamily = FontFamily.SansSerif
    val mono: FontFamily = FontFamily.Monospace
}

object InkText {
    val title = TextStyle(fontFamily = InkFonts.serif, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 36.sp)
    val item = TextStyle(fontFamily = InkFonts.serif, fontSize = 21.sp, lineHeight = 26.sp)
    val body = TextStyle(fontFamily = InkFonts.sans, fontSize = 16.sp, lineHeight = 22.sp)
    val small = TextStyle(fontFamily = InkFonts.sans, fontSize = 13.sp, lineHeight = 18.sp)
    /** Monospaced capitals for metadata; never below 11sp so it survives contrast and scaling. */
    val label = TextStyle(fontFamily = InkFonts.mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 2.sp)
}

val LocalInk = staticCompositionLocalOf { inkPalette(false, Color(0xFFA84B29)) }
/** Ink for anything drawn straight on the wallpaper (Home, PULSE, ARC). */
val LocalHomeInk = staticCompositionLocalOf { Color.White }
val LocalMotion = staticCompositionLocalOf { Motion.Calm }
val LocalIconStyle = staticCompositionLocalOf { IconStyle.System }
val LocalIconShape = staticCompositionLocalOf { IconShape.Circle }
val LocalInkHaptics = staticCompositionLocalOf { Haptics.Soft }

/** Duration scaled by the Motion setting; 0 disables the animation. */
@Composable
fun motionMs(calm: Int = 220): Int = when (LocalMotion.current) {
    Motion.Calm -> calm
    Motion.Brisk -> calm * 3 / 5
    Motion.Off -> 0
}

class InkHaptics(private val haptic: HapticFeedback, private val mode: Haptics) {
    /** Crossing onto a new target. */
    fun tick() {
        if (mode != Haptics.Off) {
            haptic.performHapticFeedback(if (mode == Haptics.Strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
        }
    }

    /** Arming a gesture or committing an action. */
    fun confirm() {
        if (mode != Haptics.Off) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

@Composable
fun rememberInkHaptics(): InkHaptics {
    val haptic = LocalHapticFeedback.current
    val mode = LocalInkHaptics.current
    return remember(haptic, mode) { InkHaptics(haptic, mode) }
}

/**
 * Text on the wallpaper gets at most a 0–1 dp shadow (4.1); legibility comes from [LocalHomeInk]
 * switching to dark ink on light wallpapers, not from a halo or scrim. Radius in px (≈1 dp at 3×).
 */
fun homeShadow(ink: Color) = Shadow(
    color = if (ink.luminance() > .5f) Color.Black.copy(alpha = .35f) else Color.White.copy(alpha = .25f),
    blurRadius = 3f,
)

@Composable
fun isDark(settings: LauncherSettings): Boolean = when (settings.theme) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Dark -> true
    ThemeMode.Light -> false
}

@Composable
internal fun accentColor(accent: Accent, dark: Boolean): Color =
    if (accent == Accent.Dynamic && Build.VERSION.SDK_INT >= 31) {
        val context = LocalContext.current
        if (dark) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary
    } else Color(AccentColors.argb(accent, dark))

/** Resolved colours of the palette's places (§5.12); all equal to the main accent while it is unified. */
@Immutable
class PlaceColors(private val colors: Map<PaletteTarget, Color>, val main: Color) {
    operator fun get(target: PaletteTarget): Color = colors[target] ?: main
}

val LocalPlaces = staticCompositionLocalOf { PlaceColors(emptyMap(), Color(0xFFA84B29)) }

/**
 * The palette resolved for text drawn straight on the wallpaper (clock and date without a plate): the accent's
 * variant follows the wallpaper like [LocalHomeInk], not the theme, so a Light theme over a dark wallpaper does
 * not paint a dark clock on it.
 */
val LocalWallPlaces = staticCompositionLocalOf { PlaceColors(emptyMap(), Color(0xFFA84B29)) }

/**
 * Re-provides the theme (Ink, Crystal and Material primary) with [target]'s colour, so everything inside —
 * texts, switches, sliders — follows that place of the palette. A no-op while it equals the current accent.
 */
@Composable
fun AccentScope(target: PaletteTarget, content: @Composable () -> Unit) {
    val acc = LocalPlaces.current[target]
    val ink = LocalInk.current
    if (acc == ink.accent) return content()
    val scoped = inkPalette(ink.dark, acc)
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = acc, onPrimary = scoped.onAccent)) {
        CompositionLocalProvider(LocalInk provides scoped, LocalCrystal provides crystalPalette(ink.dark, acc), content = content)
    }
}

/** [AccentScope] with the Veil choices of an overlay (e.g. the lighter blurred column while the system blurs the wallpaper). */
@Composable
fun AccentScope(target: PaletteTarget, veil: VeilPrefs, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalVeil provides veil) { AccentScope(target, content) }
}

/** True when the system wallpaper wants dark text; follows wallpaper changes while composed. */
@Composable
fun rememberWallpaperWantsDarkText(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { WallpaperManager.getInstance(context) }
    var dark by remember { mutableStateOf(wantsDarkText(manager.safeColors())) }
    DisposableEffect(manager) {
        val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
            if (which and WallpaperManager.FLAG_SYSTEM != 0) dark = wantsDarkText(colors)
        }
        manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        onDispose { manager.removeOnColorsChangedListener(listener) }
    }
    return dark
}

private fun WallpaperManager.safeColors(): WallpaperColors? = try {
    getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
} catch (_: SecurityException) {
    null
}

private fun wantsDarkText(colors: WallpaperColors?): Boolean = when {
    colors == null -> false
    Build.VERSION.SDK_INT >= 31 -> colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
    else -> Color(colors.primaryColor.toArgb()).luminance() > .55f
}

/** Status and navigation bar icons: dark icons on a light ground. */
@Composable
fun SystemBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = darkIcons
            isAppearanceLightNavigationBars = darkIcons
        }
    }
}

@Composable
fun InteliaTheme(settings: LauncherSettings, content: @Composable () -> Unit) {
    val dark = isDark(settings)
    val ink = inkPalette(dark, accentColor(settings.accent, dark))
    val places = PlaceColors(PaletteTarget.entries.associateWith { accentColor(settings.accentFor(it), dark) }, ink.accent)
    // Home type follows the wallpaper, not the theme: Crystal fg light on light wallpapers (4.1).
    // Intelia's own wallpaper is known exactly; on the system one only its hint, and a strong dim makes it dark.
    val systemLight = rememberWallpaperWantsDarkText()
    val wallLight = Wallpapers.byId(settings.wallpaper)?.let { Wallpapers.wantsDarkText(it, settings.wallDim) }
        ?: (systemLight && settings.wallDim < Wallpapers.SYSTEM_LIGHT_TEXT_DIM)
    val homeInk = if (wallLight) CrystalColors.FgLight else CrystalColors.FgDark
    val wallPlaces = if (wallLight == !dark) places
        else PlaceColors(PaletteTarget.entries.associateWith { accentColor(settings.accentFor(it), !wallLight) }, accentColor(settings.accent, !wallLight))
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = ink.accent, onPrimary = ink.onAccent, background = ink.paper, surface = ink.paper,
        onBackground = ink.ink, onSurface = ink.ink, onSurfaceVariant = ink.muted, outline = ink.faint,
        outlineVariant = ink.line, surfaceContainer = ink.paper, surfaceContainerLow = ink.paper,
        surfaceContainerHigh = ink.paper, surfaceContainerHighest = ink.paper, error = ink.danger,
        inverseSurface = ink.night, inverseOnSurface = ink.onNight,
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalInk provides ink,
            LocalPlaces provides places,
            LocalHomeInk provides homeInk,
            LocalWallPlaces provides wallPlaces,
            LocalMotion provides settings.motion,
            LocalIconStyle provides settings.iconStyle,
            LocalIconShape provides settings.iconShape,
            LocalVeil provides VeilPrefs(settings.veilSolid, settings.veilShift, settings.veilEdge, settings.glass),
            LocalInkHaptics provides settings.haptics,
            LocalContentColor provides ink.ink,
            LocalTextStyle provides InkText.body.copy(color = ink.ink),
        ) { CrystalTheme(dark, ink.accent, content) }
    }
}

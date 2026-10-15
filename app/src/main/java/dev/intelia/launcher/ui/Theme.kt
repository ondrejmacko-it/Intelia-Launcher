package dev.intelia.launcher.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.intelia.launcher.data.Accent
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.ThemeMode

private val NeutralDark = darkColorScheme(
    primary = Color(0xFFE8E8E8), onPrimary = Color(0xFF222222), background = Color(0xFF121212),
    surface = Color(0xFF1C1C1C), surfaceContainer = Color(0xFF252525), surfaceContainerHigh = Color(0xFF303030),
    surfaceContainerLow = Color(0xFF1F1F1F), onSurface = Color(0xFFF5F5F5), onSurfaceVariant = Color(0xFFC9C9C9),
    outlineVariant = Color(0xFF404040), outline = Color(0xFF707070),
    secondaryContainer = Color(0xFF333333), onSecondaryContainer = Color(0xFFEEEEEE),
)

private val NeutralLight = lightColorScheme(
    primary = Color(0xFF292929), onPrimary = Color.White, background = Color(0xFFF5F5F5),
    surface = Color.White, surfaceContainer = Color(0xFFEEEEEE), surfaceContainerHigh = Color(0xFFE6E6E6),
    surfaceContainerLow = Color(0xFFF7F7F7), onSurface = Color(0xFF161616), onSurfaceVariant = Color(0xFF4B4B4B),
    outlineVariant = Color(0xFFD3D3D3), outline = Color(0xFF999999),
    secondaryContainer = Color(0xFFE0E0E0), onSecondaryContainer = Color(0xFF292929),
)

@Composable
fun isDark(settings: LauncherSettings): Boolean = when (settings.theme) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Dark -> true
    ThemeMode.Light -> false
}

@Composable
fun InteliaTheme(settings: LauncherSettings, content: @Composable () -> Unit) {
    val dark = isDark(settings)
    val context = LocalContext.current
    val neutral = if (dark) NeutralDark else NeutralLight
    val scheme: ColorScheme = when (settings.accent) {
        Accent.Dynamic -> if (Build.VERSION.SDK_INT >= 31) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else neutral
        Accent.Sage -> neutral.copy(primary = if (dark) Color(0xFFBBD1C2) else Color(0xFF355B46))
        Accent.Slate -> neutral.copy(primary = if (dark) Color(0xFFBCCDEB) else Color(0xFF36557E))
        Accent.Mono -> neutral
    }
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

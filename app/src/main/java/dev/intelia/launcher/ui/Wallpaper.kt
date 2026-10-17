package dev.intelia.launcher.ui

import android.app.Activity
import android.app.ActivityManager
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import dev.intelia.launcher.data.WallpaperPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * What lies behind Home: one of Intelia's own wallpapers ([preset], drawn once and cached, nothing animated) or
 * the system wallpaper (null), and [dim] % of black over either. While Intelia draws its own, the window stops
 * asking the system for the wallpaper underneath, so it is not composited for nothing.
 */
@Composable
fun WallpaperLayer(preset: WallpaperPreset?, dim: Int, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val own = preset != null
    if (!view.isInEditMode) DisposableEffect(own) {
        val window = (view.context as? Activity)?.window
        if (own) window?.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        else window?.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        onDispose { window?.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER) }
    }
    Box(modifier.fillMaxSize()) {
        if (preset != null) CachedWallpaper(preset, Modifier.fillMaxSize())
        if (dim > 0) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim / 100f)))
    }
}

/**
 * Home's own wallpaper as one picture, rendered once per preset and size off the main thread ([WallpaperExport.render],
 * the same drawing). The live lights and grain cover the screen several times over; redrawn in every frame of an
 * opening drawer or Search they cost the GPU more than a 120 Hz frame allows (Phone (3)). A low-RAM phone renders it
 * at half size and lets the GPU scale it; the soft lights do not show the difference. Until the picture is ready the
 * live drawing stands in.
 */
@Composable
private fun CachedWallpaper(preset: WallpaperPreset, modifier: Modifier) {
    val context = LocalContext.current
    val half = remember(context) { context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val picture by produceState<ImageBitmap?>(null, preset, size, half) {
        value = null
        if (size.width <= 0 || size.height <= 0) return@produceState
        val k = if (half) 2 else 1
        value = withContext(Dispatchers.Default) {
            WallpaperExport.render(preset, 0, (size.width / k).coerceAtLeast(1), (size.height / k).coerceAtLeast(1)).asImageBitmap()
        }
    }
    Box(modifier.onSizeChanged { size = it }) {
        val p = picture
        if (p == null) PresetWallpaper(preset, Modifier.fillMaxSize())
        else Image(p, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
    }
}

/** [preset] drawn into any box: Home's background and the small swatches in the Look panel. */
@Composable
fun PresetWallpaper(preset: WallpaperPreset, modifier: Modifier) {
    val grain = remember { grainTile() }
    Canvas(modifier.drawWithCache {
        val long = maxOf(size.width, size.height)
        val lights = preset.lights.map { l ->
            val c = Color(0xFF000000 or l.rgb.toLong())
            Triple(Brush.radialGradient(listOf(c, c.copy(alpha = .55f), c.copy(alpha = 0f)), Offset(l.x * size.width, l.y * size.height),
                l.r * long), Offset(l.x * size.width, l.y * size.height), l.r * long)
        }
        // A faint grain keeps the long gradients from banding on an 8-bit panel.
        val noise = ShaderBrush(ImageShader(grain, TileMode.Repeated, TileMode.Repeated))
        onDrawBehind {
            drawRect(Color(0xFF000000 or preset.base.toLong()))
            lights.forEach { (brush, centre, r) -> drawCircle(brush, r, centre) }
            drawRect(noise)
        }
    }) {}
}

private fun grainTile(): ImageBitmap = grainBitmap().asImageBitmap()

/** 64 × 64 px of ±1.5 % white / black specks, the same every time (Home and the exported system wallpaper). */
internal fun grainBitmap(): android.graphics.Bitmap {
    val n = 64
    val rnd = Random(7)
    val px = IntArray(n * n) {
        val v = rnd.nextInt(-4, 5)
        if (v >= 0) (v shl 24) or 0xFFFFFF else (-v shl 24)
    }
    return android.graphics.Bitmap.createBitmap(px, n, n, android.graphics.Bitmap.Config.ARGB_8888)
}

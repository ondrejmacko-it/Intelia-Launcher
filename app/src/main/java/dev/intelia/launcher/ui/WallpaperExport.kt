package dev.intelia.launcher.ui

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import dev.intelia.launcher.data.WallpaperPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Saves one of Intelia's own wallpapers as the system wallpaper (D13 ④, `SET_WALLPAPER`). Only on the owner's explicit
 * confirmation in Vzhled › Tapeta; it replaces the current system wallpaper and, when chosen, the lock screen's.
 * The picture is the one Home draws: the same lights, grain and dim, at the screen's portrait size.
 */
object WallpaperExport {
    enum class Target { Home, HomeAndLock }

    /** Longest side of the exported picture; larger screens are drawn at this size and scaled by the system. */
    const val MAX_SIDE = 4096

    /** Portrait size for a screen of [w] × [h] px whatever its rotation, each side 1..[MAX_SIDE]. */
    fun size(w: Int, h: Int): Pair<Int, Int> {
        val short = minOf(w, h).coerceAtLeast(1)
        val long = maxOf(w, h).coerceAtLeast(1)
        val k = minOf(1f, MAX_SIDE.toFloat() / long)
        return (short * k).toInt().coerceIn(1, MAX_SIDE) to (long * k).toInt().coerceIn(1, MAX_SIDE)
    }

    fun flags(target: Target): Int = when (target) {
        Target.Home -> WallpaperManager.FLAG_SYSTEM
        Target.HomeAndLock -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
    }

    /** [preset] with [dim] % black over it, drawn as `PresetWallpaper` draws it. */
    fun render(preset: WallpaperPreset, dim: Int, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(0xFF000000.toInt() or preset.base)
        val long = maxOf(w, h).toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        preset.lights.forEach { l ->
            val c = 0xFF000000.toInt() or l.rgb
            val x = l.x * w
            val y = l.y * h
            val r = l.r * long
            paint.shader = RadialGradient(x, y, r, intArrayOf(c, (c and 0xFFFFFF) or (140 shl 24), c and 0xFFFFFF), null, Shader.TileMode.CLAMP)
            canvas.drawCircle(x, y, r, paint)
        }
        paint.shader = BitmapShader(grainBitmap(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        if (dim > 0) canvas.drawColor((dim.coerceIn(0, 100) * 255 / 100) shl 24)
        return bmp
    }

    /** Draws and saves off the main thread; false when the system refuses (policy, storage) or drawing fails. */
    suspend fun save(context: Context, preset: WallpaperPreset, dim: Int, target: Target): Boolean = withContext(Dispatchers.Default) {
        runCatching {
            val (sw, sh) = if (android.os.Build.VERSION.SDK_INT >= 30) {
                context.getSystemService(android.view.WindowManager::class.java).maximumWindowMetrics.bounds.let { it.width() to it.height() }
            } else context.resources.displayMetrics.let { it.widthPixels to it.heightPixels }
            val (w, h) = size(sw, sh)
            val manager = WallpaperManager.getInstance(context)
            if (!manager.isWallpaperSupported || !manager.isSetWallpaperAllowed) return@runCatching false
            val bmp = render(preset, dim, w, h)
            try { manager.setBitmap(bmp, null, true, flags(target)) != 0 } finally { bmp.recycle() }
        }.getOrDefault(false)
    }
}

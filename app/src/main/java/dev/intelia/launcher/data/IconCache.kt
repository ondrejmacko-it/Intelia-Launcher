package dev.intelia.launcher.data

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * A rendered icon. [glyph] is true for a single-colour themed glyph that the UI tints with ink;
 * otherwise the bitmap is the full-colour icon.
 */
class RenderedIcon(val bitmap: ImageBitmap, val glyph: Boolean)

/** One rendered size per icon and style, bounded by bytes; invalidated per changed package. */
class IconCache(context: Context, private val apps: AppRepository) {
    private val densityDpi = context.resources.displayMetrics.densityDpi
    private val sizePx = (ICON_DP * context.resources.displayMetrics.density).roundToInt()
    private val cache = object : LruCache<String, RenderedIcon>(MAX_BYTES) {
        override fun sizeOf(key: String, value: RenderedIcon) = value.bitmap.width * value.bitmap.height * 4
    }
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            apps.changedPackages.collect { packageName ->
                cache.snapshot().keys.filter { it.startsWith("$packageName/") }.forEach(cache::remove)
                _revision.value++
            }
        }
    }

    private fun cacheKey(key: String, ink: Boolean) = if (ink) "$key#ink" else key

    fun peek(key: String, ink: Boolean = false): RenderedIcon? = cache.get(cacheKey(key, ink))

    suspend fun load(entry: AppEntry, ink: Boolean = false): RenderedIcon? = load(entry.key, ink) { apps.icon(entry, densityDpi) }

    /** Renders [entries] missing from the cache in the background, so the drawer's rows find them on first draw. */
    suspend fun warm(entries: List<AppEntry>, ink: Boolean) {
        for (entry in entries) if (cache.get(cacheKey(entry.key, ink)) == null) load(entry, ink)
    }

    suspend fun loadShortcut(key: String, ink: Boolean = false): RenderedIcon? = load(key, ink) { apps.shortcutIcon(key, densityDpi) }

    private suspend fun load(baseKey: String, ink: Boolean, source: () -> Drawable?): RenderedIcon? {
        val key = cacheKey(baseKey, ink)
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val icon = source() ?: return@withContext null
                render(icon, ink)?.also { cache.put(key, it) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun render(icon: Drawable, ink: Boolean): RenderedIcon? {
        if (ink && Build.VERSION.SDK_INT >= 33 && icon is AdaptiveIconDrawable) {
            icon.monochrome?.let { mono ->
                // The themed glyph alone, tinted at draw time. Wrapping it in an AdaptiveIconDrawable rendered an
                // opaque disc on Android 16 (phone, 2026-10-16), so the layer is drawn directly: an adaptive layer is
                // 108 dp for 72 dp shown, hence a quarter of the size beyond each edge.
                val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
                val inset = sizePx / 4
                mono.setBounds(-inset, -inset, sizePx + inset, sizePx + inset)
                mono.draw(android.graphics.Canvas(bitmap))
                return RenderedIcon(bitmap.asImageBitmap(), glyph = true)
            }
        }
        return RenderedIcon(icon.toBitmap(sizePx, sizePx).asImageBitmap(), glyph = false)
    }

    private companion object {
        const val ICON_DP = 56
        const val MAX_BYTES = 16 * 1024 * 1024
    }
}

package dev.intelia.launcher.data

import android.app.ActivityManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.concurrent.Executors
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

    /** An eighth of the app's heap class, at most 16 MB: a low-RAM phone keeps fewer icons rather than evicting others. */
    private val maxBytes = context.getSystemService(ActivityManager::class.java).let { am ->
        val heapMb = if (am.isLowRamDevice) am.memoryClass / 2 else am.memoryClass
        (heapMb * 1024 * 1024 / 8).coerceIn(MIN_BYTES, MAX_BYTES)
    }
    private val cache = object : LruCache<String, RenderedIcon>(maxBytes) {
        override fun sizeOf(key: String, value: RenderedIcon) = value.bitmap.width * value.bitmap.height * 4
    }
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            apps.changedPackages.collect { packageName ->
                // After a reboot most reported packages have no icon here yet; only a real eviction redraws icons.
                val stale = cache.snapshot().keys.filter { it.startsWith("$packageName/") }
                stale.forEach(cache::remove)
                if (stale.isNotEmpty()) _revision.value++
            }
        }
    }

    private fun cacheKey(key: String, ink: Boolean) = if (ink) "$key#ink" else key

    fun peek(key: String, ink: Boolean = false): RenderedIcon? = cache.get(cacheKey(key, ink))

    suspend fun load(entry: AppEntry, ink: Boolean = false): RenderedIcon? = load(entry.key, ink) { apps.icon(entry, densityDpi) }

    /**
     * Renders [entries] missing from the cache so the drawer's rows find them on first draw. Runs one icon at a
     * time on a background-priority thread and stops while the cache is full, so it never competes with the
     * screen or with a phone that is still starting up.
     */
    suspend fun warm(entries: List<AppEntry>, ink: Boolean) = withContext(warmer) {
        for (entry in entries) {
            if (cache.size() >= maxBytes * 7 / 8) break
            val key = cacheKey(entry.key, ink)
            if (cache.get(key) != null) continue
            try {
                apps.icon(entry, densityDpi)?.let { render(it, ink) }?.let { cache.put(key, it) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
            yield()
        }
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
        const val MIN_BYTES = 4 * 1024 * 1024
        const val MAX_BYTES = 16 * 1024 * 1024

        /** One thread at background priority, shared by every warm-up. */
        val warmer = Executors.newSingleThreadExecutor { task ->
            Thread({ Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND); task.run() }, "IconWarm")
        }.asCoroutineDispatcher()
    }
}

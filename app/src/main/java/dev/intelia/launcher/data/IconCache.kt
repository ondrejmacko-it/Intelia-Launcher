package dev.intelia.launcher.data

import android.content.Context
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

/** One rendered size per icon, bounded by bytes; invalidated per changed package. */
class IconCache(context: Context, private val apps: AppRepository) {
    private val densityDpi = context.resources.displayMetrics.densityDpi
    private val sizePx = (ICON_DP * context.resources.displayMetrics.density).roundToInt()
    private val cache = object : LruCache<String, ImageBitmap>(MAX_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
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

    fun peek(key: String): ImageBitmap? = cache.get(key)

    suspend fun load(entry: AppEntry): ImageBitmap? {
        cache.get(entry.key)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                apps.icon(entry, densityDpi)?.toBitmap(sizePx, sizePx)?.asImageBitmap()?.also { cache.put(entry.key, it) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }
    }

    private companion object {
        const val ICON_DP = 56
        const val MAX_BYTES = 8 * 1024 * 1024
    }
}

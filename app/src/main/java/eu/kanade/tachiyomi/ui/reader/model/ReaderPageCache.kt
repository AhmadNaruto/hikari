package eu.kanade.tachiyomi.ui.reader.model

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.LruCache
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import okio.Buffer
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.NativeImageDecoder
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlin.math.roundToInt

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class ReaderPageCache(
    private val application: Application,
    private val preferences: ReaderPreferences,
) : ComponentCallbacks2 {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        application.registerComponentCallbacks(this)

        scope.launch {
            preferences.readerPageCache.changes()
                .collect { enabled ->
                    if (!enabled) {
                        clear()
                    }
                }
        }
    }

    private const val MIN_FREE_HEAP_BYTES = 64L * 1024 * 1024

    private val maxMemory = Runtime.getRuntime().maxMemory()
    private val cacheSize = (maxMemory / 8).toInt()

    private class BitmapPool(private val maxSize: Int = 8) {
        private val pool = LinkedList<Bitmap>()

        fun get(width: Int, height: Int, config: Bitmap.Config): Bitmap? {
            synchronized(this) {
                val iterator = pool.iterator()
                while (iterator.hasNext()) {
                    val bitmap = iterator.next()
                    if (!bitmap.isRecycled && bitmap.width == width && bitmap.height == height && bitmap.config == config) {
                        iterator.remove()
                        return bitmap
                    }
                }
            }
            return null
        }

        fun offer(bitmap: Bitmap) {
            synchronized(this) {
                if (!bitmap.isRecycled && pool.size < maxSize) {
                    pool.add(bitmap)
                } else if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }

        fun flush() {
            synchronized(this) {
                for (bitmap in pool) {
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
                pool.clear()
            }
        }
    }

    private val bitmapPool = BitmapPool()
    private val jobs = ConcurrentHashMap<String, Job>()

    private data class CachedBitmap(val bitmap: Bitmap, val byteCount: Int)

    private val cache = object : LruCache<String, CachedBitmap>(cacheSize) {
        override fun sizeOf(key: String, value: CachedBitmap): Int {
            return value.byteCount
        }

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: CachedBitmap, newValue: CachedBitmap?) {
            jobs.remove(key)?.cancel()
            if (newValue == null && !oldValue.bitmap.isRecycled) {
                bitmapPool.offer(oldValue.bitmap)
            }
        }
    }

    fun get(page: ReaderPage): Bitmap? {
        if (!preferences.readerPageCache.get()) return null
        val key = getKey(page) ?: return null
        return cache.get(key)?.bitmap
    }

    fun preload(page: ReaderPage) {
        if (!preferences.readerPageCache.get()) return
        val streamFn = page.stream ?: return
        val key = getKey(page) ?: return
        if (cache.get(key) != null) return

        val freeHeap = Runtime.getRuntime().maxMemory() - (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
        if (freeHeap < MIN_FREE_HEAP_BYTES) return

        jobs.remove(key)?.cancel()

        val job = scope.launch {
            var bitmap: Bitmap? = null
            try {
                val bytes = streamFn().use { it.readBytes() }
                val buffer = Buffer().write(bytes)
                val dimenOptions = ImageUtil.extractImageOptions(buffer)
                val srcWidth = dimenOptions.outWidth
                val srcHeight = dimenOptions.outHeight

                if (srcWidth <= 0 || srcHeight <= 0) return@launch

                val isExtremelyTall = srcHeight > srcWidth * 3
                val exceedsTextureLimit = srcHeight > ImageUtil.hardwareBitmapThreshold ||
                    srcWidth > ImageUtil.hardwareBitmapThreshold
                if (isExtremelyTall || exceedsTextureLimit) return@launch

                val displayMetrics = application.resources.displayMetrics
                val reqWidth = displayMetrics.widthPixels
                val reqHeight = displayMetrics.heightPixels

                val widthPercent = reqWidth.toDouble() / srcWidth
                val heightPercent = reqHeight.toDouble() / srcHeight
                val multiplier = min(widthPercent, heightPercent)

                val finalMultiplier = if (multiplier > 1.0 && !preferences.readerUpscaling.get()) {
                    1.0
                } else {
                    multiplier
                }

                val dstWidth = (srcWidth * finalMultiplier).roundToInt().coerceAtLeast(1)
                val dstHeight = (srcHeight * finalMultiplier).roundToInt().coerceAtLeast(1)

                val isUpscaling = finalMultiplier > 1.0 && preferences.readerUpscaling.get()

                var filters = 0
                if (isUpscaling) {
                    filters = filters or NativeImageDecoder.FILTER_UPSCALING
                }
                if (preferences.readerSharpening.get()) {
                    filters = filters or NativeImageDecoder.FILTER_SHARPEN
                }
                if (preferences.readerDenoising.get()) {
                    filters = filters or NativeImageDecoder.FILTER_DENOISE
                }

                if (!isActive) return@launch

                bitmap = bitmapPool.get(dstWidth, dstHeight, Bitmap.Config.ARGB_8888)
                    ?: Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888)

                val success = NativeImageDecoder.decode(
                    bitmap = bitmap,
                    data = bytes,
                    filters = filters,
                    sharpeningStrength = preferences.readerSharpeningStrength.get() / 10.0f,
                    denoisingStrength = preferences.readerDenoisingStrength.get() / 10.0f,
                )
                if (success && isActive) {
                    cache.put(key, CachedBitmap(bitmap, bitmap.allocationByteCount))
                    bitmap = null
                } else {
                    bitmapPool.offer(bitmap)
                    bitmap = null
                }
            } catch (_: Throwable) {
                if (bitmap != null) {
                    bitmapPool.offer(bitmap)
                }
            }
        }
        jobs[key] = job
        job.invokeOnCompletion {
            jobs.remove(key, job)
        }
    }

    fun clear() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        cache.trimToSize(-1)
        bitmapPool.flush()
    }

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
        ) {
            clear()
        }
    }

    override fun onLowMemory() {
        clear()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
    }

    private fun getKey(page: ReaderPage): String? {
        val chapter = page.chapterOrNull ?: return null
        return "${chapter.chapter.id}_${page.index}"
    }
}

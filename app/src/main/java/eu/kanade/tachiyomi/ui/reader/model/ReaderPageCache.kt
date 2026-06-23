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
import okio.Buffer
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.NativeImageDecoder
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.min
import kotlin.math.roundToInt

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
object ReaderPageCache : ComponentCallbacks2 {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        val application = Injekt.get<Application>()
        application.registerComponentCallbacks(this)

        val preferences = Injekt.get<ReaderPreferences>()
        scope.launch {
            preferences.readerPageCache.changes()
                .collect { enabled ->
                    if (!enabled) {
                        clear()
                    }
                }
        }
    }

    private val maxMemory = Runtime.getRuntime().maxMemory()
    private val cacheSize = (maxMemory / 8).toInt()

    private val cache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.allocationByteCount
        }
    }

    fun get(page: ReaderPage): Bitmap? {
        val preferences = Injekt.get<ReaderPreferences>()
        if (!preferences.readerPageCache.get()) return null
        val key = getKey(page) ?: return null
        return cache.get(key)
    }

    fun preload(page: ReaderPage) {
        val preferences = Injekt.get<ReaderPreferences>()
        if (!preferences.readerPageCache.get()) return
        val streamFn = page.stream ?: return
        val key = getKey(page) ?: return
        if (cache.get(key) != null) return

        scope.launch {
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

                val preferences = Injekt.get<ReaderPreferences>()
                val displayMetrics = Injekt.get<Application>().resources.displayMetrics
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

                val bitmap = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888)
                val success = NativeImageDecoder.decode(
                    bitmap = bitmap,
                    data = bytes,
                    filters = filters,
                    sharpeningStrength = preferences.readerSharpeningStrength.get() / 10.0f,
                    denoisingStrength = preferences.readerDenoisingStrength.get() / 10.0f,
                )
                if (success) {
                    cache.put(key, bitmap)
                } else {
                    bitmap.recycle()
                }
            } catch (_: Throwable) {
            }
        }
    }

    fun clear() {
        cache.evictAll()
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

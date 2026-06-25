package eu.kanade.tachiyomi.data.coil

import android.graphics.Bitmap
import android.os.Build
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.allowRgb565
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import okio.BufferedSource
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.NativeImageDecoder
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A Coil [Decoder] that leverages the custom native C++ [NativeImageDecoder] pipeline.
 * Features:
 * - High-performance decoding via AImageDecoder (API 30+)
 * - Hardware-accelerated upscaling via EASU (FSR 1.0)
 * - Reduced JVM heap pressure
 */
class HikariImageDecoder(
    private val resources: ImageSource,
    private val options: Options,
    private val preferences: ReaderPreferences,
) : Decoder {

    override suspend fun decode(): DecodeResult? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null

        val bytes = resources.source().readByteArray()

        val dimenOptions = ImageUtil.extractImageOptions(okio.Buffer().write(bytes))
        val srcWidth = dimenOptions.outWidth
        val srcHeight = dimenOptions.outHeight

        val reqWidth = options.size.widthPx(options.scale) { srcWidth }
        val reqHeight = options.size.heightPx(options.scale) { srcHeight }

        val widthPercent = reqWidth.toDouble() / srcWidth
        val heightPercent = reqHeight.toDouble() / srcHeight
        val multiplier = if (options.scale == coil3.size.Scale.FILL) {
            max(widthPercent, heightPercent)
        } else {
            min(widthPercent, heightPercent)
        }

        val dstWidth = (srcWidth * multiplier).roundToInt().coerceAtLeast(1)
        val dstHeight = (srcHeight * multiplier).roundToInt().coerceAtLeast(1)

        val sampleSize = DecodeUtils.calculateInSampleSize(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            dstWidth = dstWidth,
            dstHeight = dstHeight,
            scale = options.scale,
        )

        val isUpscaling = dstWidth > srcWidth / sampleSize || dstHeight > srcHeight / sampleSize
        var filters = 0
        if (isUpscaling && preferences.readerUpscaling.get()) {
            when (preferences.readerUpscalerType.get()) {
                1 -> filters = filters or NativeImageDecoder.FILTER_AVIR
                2 -> filters = filters or NativeImageDecoder.FILTER_LANCIR
                else -> filters = filters or NativeImageDecoder.FILTER_UPSCALING
            }
        }
        if (preferences.readerSharpening.get()) {
            filters = filters or NativeImageDecoder.FILTER_SHARPEN
        }
        if (preferences.readerDenoising.get()) {
            filters = filters or NativeImageDecoder.FILTER_DENOISE
        }

        val useRgb565 = options.allowRgb565 && filters == 0
        val bitmap = Bitmap.createBitmap(
            dstWidth,
            dstHeight,
            if (useRgb565) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888,
        )

        val success = NativeImageDecoder.decode(
            bitmap,
            bytes,
            filters,
            preferences.readerSharpeningStrength.get() / 10.0f,
            preferences.readerDenoisingStrength.get() / 10.0f,
        )

        val finalBitmap = if (success) {
            bitmap
        } else {
            bitmap.recycle()
            val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = if (options.allowRgb565 && filters == 0) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
            }
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
        } ?: return null

        return DecodeResult(
            image = finalBitmap.asImage(),
            isSampled = sampleSize > 1,
        )
    }

    class Factory : Decoder.Factory {
        override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null

            val source = result.source.source()
            if (!isSupported(source)) return null

            if (isLargeOrTall(source)) return null

            val preferences = Injekt.get<ReaderPreferences>()
            return HikariImageDecoder(result.source, options, preferences)
        }

        private fun isLargeOrTall(source: BufferedSource): Boolean {
            val options = ImageUtil.extractImageOptions(source.peek())
            val srcWidth = options.outWidth
            val srcHeight = options.outHeight

            val isExtremelyTall = srcHeight > srcWidth * 3
            val exceedsTextureLimit = srcHeight > ImageUtil.hardwareBitmapThreshold ||
                srcWidth > ImageUtil.hardwareBitmapThreshold

            return isExtremelyTall || exceedsTextureLimit
        }

        private fun isSupported(source: BufferedSource): Boolean {
            val type = source.peek().inputStream().use {
                ImageUtil.findImageType(it)
            }
            return when (type) {
                ImageUtil.ImageType.JPEG, ImageUtil.ImageType.PNG, ImageUtil.ImageType.WEBP -> true
                else -> false
            }
        }
    }
}

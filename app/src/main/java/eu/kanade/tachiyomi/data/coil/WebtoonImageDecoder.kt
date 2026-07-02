package eu.kanade.tachiyomi.data.coil

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.graphics.createBitmap
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.allowRgb565
import okio.BufferedSource
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.NativeImageDecoder
import tachiyomi.core.common.util.system.logcat

/**
 * A Coil [Decoder] specifically tuned for very tall webtoon-style images.
 * It uses aggressive inSampleSize to prevent OOM and reduce memory pressure
 * for images that exceed typical texture limits.
 */
class WebtoonImageDecoder(private val source: BufferedSource, private val options: Options) : Decoder {

    override suspend fun decode(): DecodeResult? {
        val bytes = source.readByteArray()
        val dimenOptions = ImageUtil.extractImageOptions(okio.Buffer().write(bytes))

        val srcWidth = dimenOptions.outWidth
        val srcHeight = dimenOptions.outHeight

        if (srcWidth <= 0 || srcHeight <= 0) return null

        val isExtremelyTall = srcHeight > srcWidth * 3

        logcat { "Tuning Webtoon image: ${srcWidth}x$srcHeight" }

        var inSampleSize = 1
        val maxDimension = maxOf(srcWidth, srcHeight)
        val targetLimit = ImageUtil.hardwareBitmapThreshold.coerceAtLeast(2048)

        while (maxDimension / inSampleSize > targetLimit) {
            inSampleSize *= 2
        }

        if (options.allowRgb565 && isExtremelyTall && inSampleSize == 1) {
            inSampleSize = 2
        }

        val dstWidth = srcWidth / inSampleSize
        val dstHeight = srcHeight / inSampleSize

        val bitmap = createBitmap(
            dstWidth,
            dstHeight,
            if (options.allowRgb565) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888,
        )

        var success = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && NativeImageDecoder.isAvailable) {
            success = NativeImageDecoder.decode(bitmap, bytes)
        }

        if (!success) {
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                this.inPreferredConfig = if (options.allowRgb565) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
            }
            val b = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            if (b != null) {
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawBitmap(b, 0f, 0f, null)
                b.recycle()
                success = true
            }
        }

        if (!success) {
            bitmap.recycle()
            throw IllegalStateException("Failed to decode extremely tall image.")
        }

        return DecodeResult(
            image = bitmap.asImage(),
            isSampled = inSampleSize > 1,
        )
    }

    class Factory : Decoder.Factory {
        override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder? {
            // Only apply this to standard raster formats, AVIF/JXL handled by TachiyomiImageDecoder
            val source = result.source.source()
            return if (isWebtoonApplicable(source)) {
                WebtoonImageDecoder(source, options)
            } else {
                null
            }
        }

        private fun isWebtoonApplicable(source: BufferedSource): Boolean {
            val type = source.peek().inputStream().use {
                ImageUtil.findImageType(it)
            }
            val isSupportedType = when (type) {
                ImageUtil.ImageType.JPEG, ImageUtil.ImageType.PNG, ImageUtil.ImageType.WEBP -> true
                else -> false
            }
            if (!isSupportedType) return false

            val options = ImageUtil.extractImageOptions(source.peek())
            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return false

            val isExtremelyTall = srcHeight > srcWidth * 3
            val exceedsTextureLimit =
                srcHeight > ImageUtil.hardwareBitmapThreshold || srcWidth > ImageUtil.hardwareBitmapThreshold

            return isExtremelyTall || exceedsTextureLimit
        }
    }
}

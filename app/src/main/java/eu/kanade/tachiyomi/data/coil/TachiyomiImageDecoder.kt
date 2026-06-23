package eu.kanade.tachiyomi.data.coil

import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.allowRgb565
import coil3.request.bitmapConfig
import okio.BufferedSource
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.decoder.ImageDecoder

/**
 * A [Decoder] that uses built-in [ImageDecoder] to decode images that is not supported by the system.
 */
class TachiyomiImageDecoder(private val resources: ImageSource, private val options: Options) : Decoder {

    override suspend fun decode(): DecodeResult? {
        val bytes = try {
            resources.source().use { it.readByteArray() }
        } catch (e: Exception) {
            return null
        }

        val decoder = ImageDecoder.newInstance(java.io.ByteArrayInputStream(bytes), options.cropBorders, displayProfile)

        var sampleSize = 1
        val bitmap = if (decoder != null && decoder.width > 0 && decoder.height > 0) {
            val srcWidth = decoder.width
            val srcHeight = decoder.height

            val dstWidth = options.size.widthPx(options.scale) { srcWidth }
            val dstHeight = options.size.heightPx(options.scale) { srcHeight }

            sampleSize = DecodeUtils.calculateInSampleSize(
                srcWidth = srcWidth,
                srcHeight = srcHeight,
                dstWidth = dstWidth,
                dstHeight = dstHeight,
                scale = options.scale,
            )

            val decoded = decoder.decode(sampleSize = sampleSize)
            decoder.recycle()
            decoded
        } else {
            null
        }

        val finalBitmap = if (bitmap != null) {
            bitmap
        } else {
            val bitmapOptions = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bitmapOptions)

            val srcWidth = bitmapOptions.outWidth
            val srcHeight = bitmapOptions.outHeight

            val dstWidth = options.size.widthPx(options.scale) { srcWidth }
            val dstHeight = options.size.heightPx(options.scale) { srcHeight }

            sampleSize = DecodeUtils.calculateInSampleSize(
                srcWidth = srcWidth,
                srcHeight = srcHeight,
                dstWidth = dstWidth,
                dstHeight = dstHeight,
                scale = options.scale,
            )

            val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = if (options.allowRgb565) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
            }
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
        } ?: return null

        val targetConfig = when {
            options.bitmapConfig == Bitmap.Config.HARDWARE && ImageUtil.canUseHardwareBitmap(finalBitmap) -> Bitmap.Config.HARDWARE
            options.allowRgb565 -> Bitmap.Config.RGB_565
            else -> null
        }

        var resultBitmap = finalBitmap
        if (targetConfig != null && targetConfig != finalBitmap.config) {
            val newBitmap = finalBitmap.copy(targetConfig, false)
            if (newBitmap != null) {
                finalBitmap.recycle()
                resultBitmap = newBitmap
            }
        }

        return DecodeResult(
            image = resultBitmap.asImage(),
            isSampled = sampleSize > 1,
        )
    }

    class Factory : Decoder.Factory {

        override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder? {
            return if (options.customDecoder || isApplicable(result.source.source())) {
                TachiyomiImageDecoder(result.source, options)
            } else {
                null
            }
        }

        private fun isApplicable(source: BufferedSource): Boolean {
            val type = source.peek().inputStream().use {
                ImageUtil.findImageType(it)
            }
            return when (type) {
                ImageUtil.ImageType.AVIF, ImageUtil.ImageType.JXL, ImageUtil.ImageType.HEIF -> true
                else -> false
            }
        }

        override fun equals(other: Any?) = other is Factory

        override fun hashCode() = javaClass.hashCode()
    }

    companion object {
        var displayProfile: ByteArray? = null
    }
}

package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder
import com.davemorrissey.labs.subscaleview.provider.InputProvider
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import tachiyomi.core.common.util.system.NativeImageDecoder
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class FilteringImageRegionDecoder(
    private val wrapped: ImageRegionDecoder,
) : ImageRegionDecoder {

    private val preferences: ReaderPreferences by lazy { Injekt.get() }

    override fun init(context: Context, provider: InputProvider): Point =
        wrapped.init(context, provider)

    override fun decodeRegion(sRect: Rect, sampleSize: Int): Bitmap {
        val bitmap = wrapped.decodeRegion(sRect, sampleSize)

        var filters = 0
        if (preferences.readerUpscaling.get()) {
            when (preferences.readerUpscalerType.get()) {
                1 -> filters = filters or NativeImageDecoder.FILTER_AVIR
                2 -> filters = filters or NativeImageDecoder.FILTER_LANCIR
                else -> filters = filters or NativeImageDecoder.FILTER_UPSCALING
            }
        }
        if (preferences.readerSharpening.get()) filters = filters or NativeImageDecoder.FILTER_SHARPEN
        if (preferences.readerDenoising.get()) filters = filters or NativeImageDecoder.FILTER_DENOISE

        if (filters != 0) {
            NativeImageDecoder.process(
                bitmap,
                filters,
                preferences.readerSharpeningStrength.get() / 10.0f,
                preferences.readerDenoisingStrength.get() / 10.0f,
            )
        }

        return bitmap
    }

    override fun isReady(): Boolean = wrapped.isReady()

    override fun recycle() = wrapped.recycle()
}

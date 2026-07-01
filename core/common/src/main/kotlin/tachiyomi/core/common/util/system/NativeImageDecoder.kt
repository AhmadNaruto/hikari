package tachiyomi.core.common.util.system

import android.graphics.Bitmap
import android.hardware.HardwareBuffer
import android.os.Build

/**
 * High-performance image decoding utility using the Android NDK and C++.
 * This class provides zero-copy decoding into Bitmaps and direct HardwareBuffer allocation.
 */
object NativeImageDecoder {

    /** Bitmask for sharpening filter */
    const val FILTER_SHARPEN = 1 shl 0

    /** Bitmask for denoising filter */
    const val FILTER_DENOISE = 1 shl 1

    /** Bitmask for AI-based upscaling */
    const val FILTER_UPSCALING = 1 shl 2

    /** Bitmask for AVIR high quality resizing */
    const val FILTER_AVIR = 1 shl 3

    /** Bitmask for LANCIR fast high quality resizing */
    const val FILTER_LANCIR = 1 shl 4

    init {
        try {
            System.loadLibrary("z")
            System.loadLibrary("intl")
            System.loadLibrary("glib-2.0")
            System.loadLibrary("gmodule-2.0")
            System.loadLibrary("gobject-2.0")
            System.loadLibrary("gthread-2.0")
            System.loadLibrary("gio-2.0")
            System.loadLibrary("girepository-2.0")
            System.loadLibrary("vips")
        } catch (_: Exception) {
        }
        try {
            System.loadLibrary("hikari-image")
        } catch (_: Exception) {
        }
    }

    /**
     * Decodes pixel data directly into an existing [Bitmap]'s memory buffer.
     * This avoids large JVM heap allocations and extra copies between the NDK and JVM.
     *
     * @param bitmap The target bitmap (must be RGBA_8888).
     * @param data The raw encoded image data (JPEG, WebP, etc.).
     * @param filters Bitmask of [FILTER] values to apply during decoding.
     * @return True if decoding was successful.
     */
    fun decode(
        bitmap: Bitmap,
        data: ByteArray,
        filters: Int = 0,
        sharpeningStrength: Float = 1.0f,
        denoisingStrength: Float = 1.0f,
    ): Boolean {
        if (bitmap.config != Bitmap.Config.ARGB_8888 && bitmap.config != Bitmap.Config.RGB_565) {
            return false
        }
        if (bitmap.config == Bitmap.Config.RGB_565 && filters != 0) {
            return false
        }
        return nativeDecode(bitmap, data, data.size, filters, sharpeningStrength, denoisingStrength)
    }

    /**
     * Decodes a specific region of an image into an existing [Bitmap] tile.
     *
     * @param bitmap The target bitmap tile.
     * @param left Left coordinate of the region.
     * @param top Top coordinate of the region.
     * @param right Right coordinate of the region.
     * @param bottom Bottom coordinate of the region.
     * @param sampleSize The sample size for downscaling.
     * @return True if decoding was successful.
     */
    fun decodeRegion(
        bitmap: Bitmap,
        data: ByteArray,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        sampleSize: Int,
        filters: Int = 0,
        sharpeningStrength: Float = 1.0f,
        denoisingStrength: Float = 1.0f,
    ): Boolean {
        if (bitmap.config != Bitmap.Config.ARGB_8888 && bitmap.config != Bitmap.Config.RGB_565) {
            return false
        }
        if (bitmap.config == Bitmap.Config.RGB_565 && filters != 0) {
            return false
        }
        return nativeDecodeRegion(bitmap, data, data.size, left, top, right, bottom, sampleSize, filters, sharpeningStrength, denoisingStrength)
    }

    /**
     * Applies native post-processing filters to an existing [Bitmap].
     *
     * @param bitmap The target bitmap.
     * @param filters Bitmask of [FILTER] values to apply.
     * @return True if processing was successful.
     */
    fun process(
        bitmap: Bitmap,
        filters: Int,
        sharpeningStrength: Float = 1.0f,
        denoisingStrength: Float = 1.0f,
    ): Boolean {
        if (bitmap.config != Bitmap.Config.ARGB_8888) return false
        return nativeProcess(bitmap, filters, sharpeningStrength, denoisingStrength)
    }

    /**
     * Advanced: Allocates a [HardwareBuffer] directly in native memory.
     * This allows the GPU to sample the image without copying it from JVM memory.
     *
     * @param width The image width.
     * @param height The image height.
     * @return A [HardwareBuffer] ready for GPU use, or null if allocation fails.
     */
    fun allocateHardwareBuffer(width: Int, height: Int): HardwareBuffer? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nativeDecodeToHardwareBuffer(width, height)
        } else {
            null
        }
    }

    private external fun nativeDecode(
        bitmap: Bitmap,
        data: ByteArray,
        length: Int,
        filters: Int,
        sharpeningStrength: Float,
        denoisingStrength: Float,
    ): Boolean

    private external fun nativeDecodeRegion(
        bitmap: Bitmap,
        data: ByteArray,
        length: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        sampleSize: Int,
        filters: Int,
        sharpeningStrength: Float,
        denoisingStrength: Float,
    ): Boolean

    private external fun nativeProcess(
        bitmap: Bitmap,
        filters: Int,
        sharpeningStrength: Float,
        denoisingStrength: Float,
    ): Boolean

    private external fun nativeDecodeToHardwareBuffer(width: Int, height: Int): HardwareBuffer?
}

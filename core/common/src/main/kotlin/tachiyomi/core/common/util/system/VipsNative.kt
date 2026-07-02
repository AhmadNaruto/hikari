package tachiyomi.core.common.util.system

import logcat.LogPriority

object VipsNative {
    
    private var initialized = false
    private var libraryLoadError: Throwable? = null
    private val libraryLoaded: Boolean = try {
        System.loadLibrary("c++_shared")
        System.loadLibrary("z")
        System.loadLibrary("intl")
        System.loadLibrary("glib-2.0")
        System.loadLibrary("gmodule-2.0")
        System.loadLibrary("gobject-2.0")
        System.loadLibrary("gthread-2.0")
        System.loadLibrary("gio-2.0")
        System.loadLibrary("girepository-2.0")
        System.loadLibrary("vips")
        System.loadLibrary("hikari-image")
        true
    } catch (e: Throwable) {
        libraryLoadError = e
        false
    }

    fun safeInit(): Boolean {
        if (!libraryLoaded) {
            logcat(LogPriority.ERROR, libraryLoadError) { "Failed to load libvips native dependencies" }
            return false
        }
        if (initialized) return true
        return try {
            initialized = init()
            initialized
        } catch (e: Throwable) {
            false
        }
    }

    fun isInitialized(): Boolean = initialized

    private external fun init(): Boolean
    
    external fun getVersion(): String
    
    external fun compressJpeg(input: ByteArray, quality: Int): ByteArray?
    
    external fun compressWebp(input: ByteArray, quality: Int): ByteArray?
    
    external fun compressPng(input: ByteArray, compression: Int): ByteArray?
    
    fun shutdown() {
        if (!initialized) return
        runCatching { nativeShutdown() }
        initialized = false
    }

    private external fun nativeShutdown()
}

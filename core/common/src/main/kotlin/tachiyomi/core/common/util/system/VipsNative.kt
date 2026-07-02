package tachiyomi.core.common.util.system

object VipsNative {
    
    private var initialized = false
    private val libraryLoaded: Boolean = runCatching {
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
    }.isSuccess

    fun safeInit(): Boolean {
        if (!libraryLoaded) return false
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
    
    external fun shutdown()
}

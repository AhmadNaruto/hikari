package tachiyomi.core.common.util.system

object VipsNative {
    
    private var initialized = false

    fun safeInit(): Boolean {
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

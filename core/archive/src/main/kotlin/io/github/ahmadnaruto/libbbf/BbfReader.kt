package io.github.ahmadnaruto.libbbf

import java.io.Closeable
import java.nio.ByteBuffer

class BbfReader(val filePath: String) : Closeable {
    private var nativeReaderPtr: Long = 0

    init {
        nativeReaderPtr = openReader(filePath)
        if (nativeReaderPtr == 0L) {
            throw IllegalArgumentException("Failed to open BBF file: $filePath")
        }
    }

    override fun close() {
        if (nativeReaderPtr != 0L) {
            closeReader(nativeReaderPtr)
            nativeReaderPtr = 0L
        }
    }

    val pageCount: Int
        get() {
            checkClosed()
            return getPageCount(nativeReaderPtr)
        }

    val assetCount: Int
        get() {
            checkClosed()
            return getAssetCount(nativeReaderPtr)
        }

    val sectionCount: Int
        get() {
            checkClosed()
            return getSectionCount(nativeReaderPtr)
        }

    val metaCount: Int
        get() {
            checkClosed()
            return getMetaCount(nativeReaderPtr)
        }

    fun getPageAssetIndex(pageIndex: Int): Int {
        checkClosed()
        require(pageIndex in 0 until pageCount) { "Page index out of bounds: $pageIndex" }
        return getPageAssetIndex(nativeReaderPtr, pageIndex)
    }

    fun getAssetSize(assetIndex: Int): Long {
        checkClosed()
        require(assetIndex in 0 until assetCount) { "Asset index out of bounds: $assetIndex" }
        return getAssetSize(nativeReaderPtr, assetIndex)
    }

    fun getAssetType(assetIndex: Int): BbfMediaType {
        checkClosed()
        require(assetIndex in 0 until assetCount) { "Asset index out of bounds: $assetIndex" }
        return BbfMediaType.fromValue(getAssetType(nativeReaderPtr, assetIndex))
    }

    /**
     * Returns a direct ByteBuffer referencing the asset payload directly inside the memory map.
     * Note: Accessing this ByteBuffer after calling close() on this BbfReader will cause a JVM crash.
     */
    fun getAssetByteBuffer(assetIndex: Int): ByteBuffer? {
        checkClosed()
        require(assetIndex in 0 until assetCount) { "Asset index out of bounds: $assetIndex" }
        return getAssetByteBuffer(nativeReaderPtr, assetIndex)
    }

    /**
     * Returns a copy of the asset data in a ByteArray. This copy is safe to use even after close() is called.
     */
    fun getAssetBytes(assetIndex: Int): ByteArray? {
        checkClosed()
        require(assetIndex in 0 until assetCount) { "Asset index out of bounds: $assetIndex" }
        return getAssetBytes(nativeReaderPtr, assetIndex)
    }

    /**
     * Returns the asset content as a UTF-8 String. This copy is safe to use even after close() is called.
     */
    fun getAssetString(assetIndex: Int): String? {
        checkClosed()
        require(assetIndex in 0 until assetCount) { "Asset index out of bounds: $assetIndex" }
        return getAssetString(nativeReaderPtr, assetIndex)
    }

    /**
     * Finds the asset index associated with the given logical path. Returns -1 if not found.
     */
    fun findAssetByPath(path: String): Long {
        checkClosed()
        return findAssetByPath(nativeReaderPtr, path)
    }

    /**
     * Returns the logical path of the asset, or null if it has no logical path metadata.
     */
    fun getAssetPath(assetIndex: Int): String? {
        checkClosed()
        require(assetIndex in 0 until assetCount) { "Asset index out of bounds: $assetIndex" }
        return getAssetPath(nativeReaderPtr, assetIndex)
    }

    fun getMetadata(index: Int): MetadataEntry? {
        checkClosed()
        require(index in 0 until metaCount) { "Metadata index out of bounds: $index" }
        return getMetadata(nativeReaderPtr, index)
    }

    fun getSection(index: Int): SectionEntry? {
        checkClosed()
        require(index in 0 until sectionCount) { "Section index out of bounds: $index" }
        return getSection(nativeReaderPtr, index)
    }

    private fun checkClosed() {
        if (nativeReaderPtr == 0L) {
            throw IllegalStateException("BbfReader has already been closed.")
        }
    }

    companion object {
        init {
            System.loadLibrary("bbfjni")
        }

        @JvmStatic private external fun openReader(filePath: String): Long
        @JvmStatic private external fun closeReader(readerPtr: Long)
        @JvmStatic private external fun getPageCount(readerPtr: Long): Int
        @JvmStatic private external fun getAssetCount(readerPtr: Long): Int
        @JvmStatic private external fun getSectionCount(readerPtr: Long): Int
        @JvmStatic private external fun getMetaCount(readerPtr: Long): Int
        @JvmStatic private external fun getPageAssetIndex(readerPtr: Long, pageIndex: Int): Int
        @JvmStatic private external fun getAssetSize(readerPtr: Long, assetIndex: Int): Long
        @JvmStatic private external fun getAssetByteBuffer(readerPtr: Long, assetIndex: Int): ByteBuffer?
        @JvmStatic private external fun getAssetBytes(readerPtr: Long, assetIndex: Int): ByteArray?
        @JvmStatic private external fun getAssetString(readerPtr: Long, assetIndex: Int): String?
        @JvmStatic private external fun findAssetByPath(readerPtr: Long, path: String): Long
        @JvmStatic private external fun getAssetPath(readerPtr: Long, assetIndex: Int): String?
        @JvmStatic private external fun getAssetType(readerPtr: Long, assetIndex: Int): Int
        @JvmStatic private external fun getMetadata(readerPtr: Long, index: Int): MetadataEntry?
        @JvmStatic private external fun getSection(readerPtr: Long, index: Int): SectionEntry?
    }
}

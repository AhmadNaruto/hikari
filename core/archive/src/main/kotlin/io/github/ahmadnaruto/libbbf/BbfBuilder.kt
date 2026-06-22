package io.github.ahmadnaruto.libbbf

import java.io.Closeable

class BbfBuilder(
    val outputFile: String,
    val alignmentExponent: Int = 12, // 2^12 = 4096 bytes
    val reamSizeExponent: Int = 16,  // 2^16 = 65536 bytes
    val flags: Int = 2               // BBF_VARIABLE_REAM_SIZE_FLAG (from BBF::BBF_VARIABLE_REAM_SIZE_FLAG)
) : Closeable {
    private var nativeBuilderPtr: Long = 0

    init {
        nativeBuilderPtr = openBuilder(outputFile, alignmentExponent, reamSizeExponent, flags)
        if (nativeBuilderPtr == 0L) {
            throw IllegalArgumentException("Failed to create BBF builder for: $outputFile")
        }
    }

    override fun close() {
        if (nativeBuilderPtr != 0L) {
            closeBuilder(nativeBuilderPtr)
            nativeBuilderPtr = 0L
        }
    }

    fun addPage(filePath: String, pageFlags: Int = 0, assetFlags: Int = 0): Boolean {
        checkClosed()
        return addPage(nativeBuilderPtr, filePath, pageFlags, assetFlags)
    }

    fun addAsset(filePath: String, assetFlags: Int = 0): Long {
        checkClosed()
        return addAsset(nativeBuilderPtr, filePath, assetFlags)
    }

    fun addAssetWithPath(filePath: String, logicalPath: String?, assetFlags: Int = 0): Long {
        checkClosed()
        return addAssetWithPath(nativeBuilderPtr, filePath, logicalPath, assetFlags)
    }

    fun addAssetFromMemory(data: ByteArray, extensionOrName: String?, assetFlags: Int = 0): Long {
        checkClosed()
        return addAssetFromMemory(nativeBuilderPtr, data, extensionOrName, assetFlags)
    }

    fun addAssetFromMemoryWithPath(data: ByteArray, extensionOrName: String?, logicalPath: String?, assetFlags: Int = 0): Long {
        checkClosed()
        return addAssetFromMemoryWithPath(nativeBuilderPtr, data, extensionOrName, logicalPath, assetFlags)
    }

    fun addPageByAssetIndex(assetIndex: Long, pageFlags: Int = 0): Boolean {
        checkClosed()
        return addPageByAssetIndex(nativeBuilderPtr, assetIndex, pageFlags)
    }

    fun addPageWithPath(filePath: String, logicalPath: String?, pageFlags: Int = 0, assetFlags: Int = 0): Boolean {
        checkClosed()
        return addPageWithPath(nativeBuilderPtr, filePath, logicalPath, pageFlags, assetFlags)
    }

    fun addMeta(key: String, value: String, parent: String? = null): Boolean {
        checkClosed()
        return addMeta(nativeBuilderPtr, key, value, parent)
    }

    fun addSection(sectionName: String, startIndex: Long, parentName: String? = null): Boolean {
        checkClosed()
        return addSection(nativeBuilderPtr, sectionName, startIndex, parentName)
    }

    fun finalizeBuilder(): Boolean {
        checkClosed()
        return finalizeBuilder(nativeBuilderPtr)
    }

    private fun checkClosed() {
        if (nativeBuilderPtr == 0L) {
            throw IllegalStateException("BbfBuilder has already been closed.")
        }
    }

    companion object {
        init {
            System.loadLibrary("bbfjni")
        }

        @JvmStatic private external fun openBuilder(outputFile: String, alignment: Int, reamSize: Int, flags: Int): Long
        @JvmStatic private external fun closeBuilder(builderPtr: Long)
        @JvmStatic private external fun addPage(builderPtr: Long, filePath: String, pageFlags: Int, assetFlags: Int): Boolean
        @JvmStatic private external fun addAsset(builderPtr: Long, filePath: String, assetFlags: Int): Long
        @JvmStatic private external fun addAssetWithPath(builderPtr: Long, filePath: String, logicalPath: String?, assetFlags: Int): Long
        @JvmStatic private external fun addAssetFromMemory(builderPtr: Long, data: ByteArray, extensionOrName: String?, assetFlags: Int): Long
        @JvmStatic private external fun addAssetFromMemoryWithPath(builderPtr: Long, data: ByteArray, extensionOrName: String?, logicalPath: String?, assetFlags: Int): Long
        @JvmStatic private external fun addPageByAssetIndex(builderPtr: Long, assetIndex: Long, pageFlags: Int): Boolean
        @JvmStatic private external fun addPageWithPath(builderPtr: Long, filePath: String, logicalPath: String?, pageFlags: Int, assetFlags: Int): Boolean
        @JvmStatic private external fun addMeta(builderPtr: Long, key: String, value: String, parent: String?): Boolean
        @JvmStatic private external fun addSection(builderPtr: Long, sectionName: String, startIndex: Long, parentName: String?): Boolean
        @JvmStatic private external fun finalizeBuilder(builderPtr: Long): Boolean
        
        @JvmStatic external fun petrifyFile(inputPath: String, outputPath: String): Boolean
    }
}

package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import io.github.ahmadnaruto.libbbf.BbfReader

/**
 * Loader used to load a chapter from a BBF (Bound Book Format) file.
 */
internal class BbfPageLoader(private val reader: BbfReader) : PageLoader() {
    override var isLocal: Boolean = true

    override suspend fun getPages(): List<ReaderPage> {
        val pageCount = reader.pageCount
        val pages = ArrayList<ReaderPage>(pageCount)
        for (i in 0 until pageCount) {
            val assetIndex = reader.getPageAssetIndex(i)
            pages.add(
                ReaderPage(i).apply {
                    stream = {
                        // Use getAssetBytes() instead of getAssetInputStream() to copy data into
                        // JVM heap eagerly. getAssetInputStream() returns a zero-copy InputStream
                        // backed directly by the mmap region — accessing it after reader.close()
                        // (which calls munmap) would crash the JVM. The copied ByteArray is safe
                        // to read even after the BbfReader is closed.
                        reader.getAssetBytes(assetIndex)?.inputStream()
                            ?: throw Exception("Failed to get asset stream")
                    }
                    status = Page.State.Ready
                }
            )
        }
        return pages
    }

    override suspend fun loadPage(page: ReaderPage) {
        check(!isRecycled)
    }

    override fun recycle() {
        super.recycle()
        reader.close()
    }
}

package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import io.github.ahmadnaruto.libbbf.BbfReader

/**
 * Loader used to load a chapter from a BBF file.
 */
internal class BbfPageLoader(private val reader: BbfReader) : PageLoader() {
    override var isLocal: Boolean = true

    override suspend fun getPages(): List<ReaderPage> {
        val pageCount = reader.pageCount
        return (0 until pageCount).map { pageIndex ->
            ReaderPage(pageIndex).apply {
                stream = {
                    val assetIndex = reader.getPageAssetIndex(pageIndex)
                    val bytes = reader.getAssetBytes(assetIndex) ?: throw Exception("Failed to read BBF page: $pageIndex")
                    bytes.inputStream()
                }
                status = Page.State.Ready
            }
        }
    }

    override suspend fun loadPage(page: ReaderPage) {
        check(!isRecycled)
    }

    override fun recycle() {
        super.recycle()
        reader.close()
    }
}

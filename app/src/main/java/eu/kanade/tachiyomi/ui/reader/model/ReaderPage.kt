package eu.kanade.tachiyomi.ui.reader.model

import eu.kanade.tachiyomi.source.model.Page
import java.io.InputStream

open class ReaderPage(
    index: Int,
    url: String = "",
    imageUrl: String? = null,
    var stream: (() -> InputStream)? = null,
) : Page(index, url, imageUrl, null) {

    private var _chapter: ReaderChapter? = null
    open var chapter: ReaderChapter
        get() = _chapter ?: throw UninitializedPropertyAccessException("chapter has not been initialized")
        set(value) {
            _chapter = value
            if (status == State.Ready) {
                ReaderPageCache.preload(this)
            }
        }

    open val chapterOrNull: ReaderChapter?
        get() = _chapter

    override var status: State
        get() = super.status
        set(value) {
            val old = super.status
            super.status = value
            if (value == State.Ready && old != State.Ready) {
                if (_chapter != null) {
                    ReaderPageCache.preload(this)
                }
            }
        }
}

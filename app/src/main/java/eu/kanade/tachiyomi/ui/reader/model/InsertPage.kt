package eu.kanade.tachiyomi.ui.reader.model

class InsertPage(val parent: ReaderPage) : ReaderPage(parent.index, parent.url, parent.imageUrl) {

    override var chapter: ReaderChapter
        get() = parent.chapter
        set(value) {
            parent.chapter = value
        }

    override val chapterOrNull: ReaderChapter?
        get() = parent.chapterOrNull

    init {
        status = State.Ready
        stream = parent.stream
    }
}

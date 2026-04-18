package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import eu.kanade.tachiyomi.util.lang.normalizeTitle
import eu.kanade.tachiyomi.util.lang.extractDeduplicationIds
import hikari.domain.manga.model.toDomainManga
import tachiyomi.domain.manga.model.Manga

/**
 * A sealed interface representing updates from the Global Search Multiplexer.
 */
sealed interface GlobalSearchUpdate {
    val source: CatalogueSource

    /**
     * Successfully received results from a source.
     */
    data class Success(
        override val source: CatalogueSource,
        val result: List<Manga>,
        val normalizedResults: Map<String, Manga>,
    ) : GlobalSearchUpdate

    /**
     * Encountered an error while searching a source.
     */
    data class Error(override val source: CatalogueSource, val throwable: Throwable) : GlobalSearchUpdate
}

/**
 * Multiplexes search requests across multiple [CatalogueSource]s using Kotlin Flows.
 *
 * This engine allows for streaming results as they arrive and provides centralized
 * concurrency management.
 */
class GlobalSearchMultiplexer(
    private val sources: List<CatalogueSource>,
    private val query: String,
    private val networkToLocalManga: suspend (List<Manga>) -> List<Manga>,
) {
    /**
     * Executes the search across all sources and returns a stream of [GlobalSearchUpdate]s.
     *
     * @param concurrency The maximum number of sources to hit in parallel.
     */
    fun search(concurrency: Int = 5): Flow<GlobalSearchUpdate> {
        return sources.asFlow()
            .flatMapMerge(concurrency) { source ->
                flow {
                    try {
                        val page = source.getSearchManga(1, query, source.getFilterList())

                        val titles = page.mangas
                            .map { it.toDomainManga(source.id) }
                            .distinctBy { it.url }
                            .let { networkToLocalManga(it) }

                        val normalized = mutableMapOf<String, Manga>()
                        titles.forEach { manga ->
                            val ids = manga.description?.extractDeduplicationIds() ?: emptyList()
                            if (ids.isNotEmpty()) {
                                ids.forEach { id -> normalized[id] = manga }
                            } else {
                                normalized[manga.title.normalizeTitle()] = manga
                            }
                        }

                        emit(GlobalSearchUpdate.Success(source, titles, normalized))
                    } catch (e: Exception) {
                        emit(GlobalSearchUpdate.Error(source, e))
                    }
                }
            }
            .flowOn(Dispatchers.IO)
    }
}

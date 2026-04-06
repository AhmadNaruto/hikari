package eu.kanade.presentation.more.settings.screen.browse

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.manga.interactor.GetHiddenManga
import tachiyomi.domain.source.repository.SourceRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class HiddenMangaSourcesScreenModel(
    private val getHiddenManga: GetHiddenManga = Injekt.get(),
    private val sourceRepository: SourceRepository = Injekt.get(),
) : StateScreenModel<HiddenMangaSourcesScreenModel.State>(State.Loading) {

    init {
        screenModelScope.launchIO {
            kotlinx.coroutines.flow.combine(
                getHiddenManga.subscribeSourceIds(),
                sourceRepository.getSources(),
            ) { sourceIdsMap, allSources ->
                val sources = allSources.filter { sourceIdsMap.containsKey(it.id) }
                    .sortedBy { it.name }

                State.Success(
                    sources = sources.toImmutableList(),
                    countMap = sourceIdsMap,
                )
            }.collectLatest { newState ->
                mutableState.update { newState }
            }
        }
    }

    sealed interface State {
        data object Loading : State
        data class Success(
            val sources: ImmutableList<tachiyomi.domain.source.model.Source>,
            val countMap: Map<Long, Long>,
        ) : State
    }
}

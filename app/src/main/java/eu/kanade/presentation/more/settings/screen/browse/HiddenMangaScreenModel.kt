package eu.kanade.presentation.more.settings.screen.browse

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.manga.interactor.UpdateManga
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.domain.manga.interactor.GetHiddenManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.toMangaUpdate
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class HiddenMangaScreenModel(
    private val sourceId: Long,
    private val getHiddenManga: GetHiddenManga = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val updateManga: UpdateManga = Injekt.get(),
) : StateScreenModel<HiddenMangaScreenModel.State>(State()) {

    var source: Source? by mutableStateOf(null)
        private set

    init {
        val s = sourceManager.getOrStub(sourceId)
        source = Source(
            id = s.id,
            lang = s.lang,
            name = s.name,
            supportsLatest = false,
            isStub = false,
        )

        screenModelScope.launchIO {
            getHiddenManga.subscribe(sourceId).collectLatest { mangaList ->
                mutableState.update { it.copy(mangaList = mangaList.toImmutableList()) }
            }
        }
    }

    fun toggleSelection(manga: Manga) {
        mutableState.update { state ->
            val newSelected = state.selected.toMutableList().apply {
                if (contains(manga)) remove(manga) else add(manga)
            }
            state.copy(selected = newSelected)
        }
    }

    fun selectAll(selection: Boolean) {
        mutableState.update { state ->
            val newSelected = if (selection) state.mangaList else emptyList()
            state.copy(selected = newSelected)
        }
    }

    fun invertSelection() {
        mutableState.update { state ->
            val newSelected = state.mangaList.filter { !state.selected.contains(it) }
            state.copy(selected = newSelected)
        }
    }

    fun unhideSelected() {
        screenModelScope.launchIO {
            val selectedManga = state.value.selected
            val updates = selectedManga.map { it.toMangaUpdate().copy(hidden = false) }
            updateManga.awaitAll(updates)
            selectAll(false)
        }
    }

    fun unhide(manga: Manga) {
        screenModelScope.launchIO {
            updateManga.await(manga.toMangaUpdate().copy(hidden = false))
        }
    }

    data class State(
        val mangaList: ImmutableList<Manga> = kotlinx.collections.immutable.persistentListOf(),
        val selected: List<Manga> = emptyList(),
    ) {
        val selectionMode: Boolean = selected.isNotEmpty()
    }
}

private fun CoroutineScope.launchIO(block: suspend CoroutineScope.() -> Unit) {
    launch(Dispatchers.IO, block = block)
}

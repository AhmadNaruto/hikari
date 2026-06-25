package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.tachiyomi.ui.library.LibraryItem
import kotlinx.coroutines.launch
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.presentation.core.components.material.PullRefresh

@Composable
fun LibraryContent(
    categories: ImmutableList<Category>,
    selection: PersistentSet<Long>,
    contentPadding: PaddingValues,
    currentPage: Int,
    isRefreshing: Boolean,
    showPageTabs: Boolean,
    onChangeCurrentPage: (Int) -> Unit,
    onClickManga: (Long, String) -> Unit,
    onContinueReadingClicked: ((LibraryManga) -> Unit)?,
    onToggleSelection: (Category, LibraryManga) -> Unit,
    onToggleRangeSelection: (Category, LibraryManga) -> Unit,
    onRefresh: () -> Unit,
    getItemCountForCategory: (Category) -> Int?,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
    getItemsForCategory: (Category) -> ImmutableList<LibraryItem>,
    continueReadingManga: HistoryWithRelations? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(
            top = contentPadding.calculateTopPadding(),
            start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
            end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
        ),
    ) {
        val pagerState = rememberPagerState(currentPage) { categories.size }

        val scope = rememberCoroutineScope()

        if (showPageTabs && categories.isNotEmpty() && (categories.size > 1 || !categories.first().isSystemCategory)) {
            LaunchedEffect(categories) {
                if (categories.size <= pagerState.currentPage) {
                    pagerState.scrollToPage(categories.size - 1)
                }
            }
            LibraryTabs(
                categories = categories,
                pagerState = pagerState,
                getItemCountForCategory = getItemCountForCategory,
                onTabItemClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(it)
                    }
                },
            )
        }

        LaunchedEffect(currentPage) {
            if (!pagerState.isScrollInProgress && pagerState.currentPage != currentPage &&
                currentPage < categories.size
            ) {
                pagerState.scrollToPage(currentPage)
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            beyondViewportPageCount = 1,
        ) { page ->
            PullRefresh(
                refreshing = isRefreshing,
                enabled = selection.isEmpty(),
                onRefresh = onRefresh,
            ) {
                val category = categories.getOrNull(page)
                val items = if (category != null) getItemsForCategory(category) else persistentListOf()
                val columns by getColumnsForOrientation(false)

                LibraryDashboard(
                    items = items,
                    columns = columns,
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                    onMangaClick = { item, tag ->
                        if (selection.isNotEmpty()) {
                            onToggleSelection(category!!, item.libraryManga)
                        } else {
                            onClickManga(item.libraryManga.manga.id, tag)
                        }
                    },
                    onMangaLongClick = { item ->
                        onToggleRangeSelection(category!!, item.libraryManga)
                    },
                    onContinueClick = { item ->
                        onContinueReadingClicked?.invoke(item.libraryManga)
                    },
                    continueReadingManga = continueReadingManga,
                )
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            onChangeCurrentPage(pagerState.currentPage)
        }
    }
}

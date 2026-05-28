package eu.kanade.presentation.more.settings.screen.browse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.browse.components.InLibraryBadge
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.components.RadioMenuItem
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.presentation.library.components.CommonMangaItemDefaults
import eu.kanade.presentation.library.components.MangaComfortableGridItem
import eu.kanade.presentation.library.components.MangaCompactGridItem
import eu.kanade.presentation.library.components.MangaListItem
import eu.kanade.presentation.util.Screen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.util.plus
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class HiddenMangaScreen(
    private val sourceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { HiddenMangaScreenModel(sourceId) }
        val state by screenModel.state.collectAsState()

        val sourcePreferences = remember { Injekt.get<SourcePreferences>() }
        val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
        val displayMode by sourcePreferences.sourceDisplayMode.collectAsState()
        val portraitColumns by libraryPreferences.portraitColumns.collectAsState()
        val landscapeColumns by libraryPreferences.landscapeColumns.collectAsState()
        val orientation = LocalConfiguration.current.orientation
        val columns = remember(portraitColumns, landscapeColumns, orientation) {
            val isLandscape = orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val cols = if (isLandscape) landscapeColumns else portraitColumns
            if (cols == 0) GridCells.Adaptive(128.dp) else GridCells.Fixed(cols)
        }

        BackHandler(enabled = state.selectionMode || state.searchQuery != null) {
            when {
                state.selectionMode -> screenModel.selectAll(false)
                state.searchQuery != null -> screenModel.search(null)
            }
        }

        Scaffold(
            topBar = { scrollBehavior ->
                SearchToolbar(
                    searchQuery = state.searchQuery,
                    onChangeSearchQuery = screenModel::search,
                    titleContent = {
                        AppBarTitle(
                            title = screenModel.source?.visualName ?: stringResource(MR.strings.label_hidden_manga),
                        )
                    },
                    navigateUp = navigator::pop,
                    searchEnabled = !state.selectionMode,
                    actions = {
                        if (state.selectionMode) {
                            IconButton(onClick = { screenModel.selectAll(true) }) {
                                Icon(
                                    Icons.Outlined.SelectAll,
                                    contentDescription = stringResource(MR.strings.action_select_all),
                                )
                            }
                            IconButton(onClick = { screenModel.invertSelection() }) {
                                Icon(
                                    Icons.Outlined.FlipToBack,
                                    contentDescription = stringResource(MR.strings.action_select_inverse),
                                )
                            }
                        } else {
                            var selectingDisplayMode by remember { mutableStateOf(false) }
                            IconButton(onClick = { selectingDisplayMode = true }) {
                                Icon(
                                    imageVector = if (displayMode == LibraryDisplayMode.List) {
                                        Icons.AutoMirrored.Filled.ViewList
                                    } else {
                                        Icons.Filled.ViewModule
                                    },
                                    contentDescription = stringResource(MR.strings.action_display_mode),
                                )
                            }
                            DropdownMenu(
                                expanded = selectingDisplayMode,
                                onDismissRequest = { selectingDisplayMode = false },
                            ) {
                                RadioMenuItem(
                                    text = { Text(text = stringResource(MR.strings.action_display_comfortable_grid)) },
                                    isChecked = displayMode == LibraryDisplayMode.ComfortableGrid,
                                ) {
                                    selectingDisplayMode = false
                                    sourcePreferences.sourceDisplayMode.set(LibraryDisplayMode.ComfortableGrid)
                                }
                                RadioMenuItem(
                                    text = { Text(text = stringResource(MR.strings.action_display_grid)) },
                                    isChecked = displayMode == LibraryDisplayMode.CompactGrid,
                                ) {
                                    selectingDisplayMode = false
                                    sourcePreferences.sourceDisplayMode.set(LibraryDisplayMode.CompactGrid)
                                }
                                RadioMenuItem(
                                    text = { Text(text = stringResource(MR.strings.action_display_list)) },
                                    isChecked = displayMode == LibraryDisplayMode.List,
                                ) {
                                    selectingDisplayMode = false
                                    sourcePreferences.sourceDisplayMode.set(LibraryDisplayMode.List)
                                }
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                HiddenMangaBottomActionMenu(
                    visible = state.selectionMode,
                    modifier = Modifier.fillMaxWidth(),
                    onUnhideClicked = { screenModel.unhideSelected() },
                )
            },
        ) { paddingValues ->
            if (state.mangaList.isEmpty()) {
                EmptyScreen(
                    stringRes = MR.strings.no_results_found,
                    modifier = Modifier.padding(paddingValues),
                )
            } else {
                when (displayMode) {
                    LibraryDisplayMode.List -> {
                        ScrollbarLazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = paddingValues + PaddingValues(vertical = 12.dp) + PaddingValues(bottom = 80.dp),
                        ) {
                            items(
                                items = state.filteredMangaList,
                                key = { it.id },
                            ) { manga ->
                                MangaListItem(
                                    coverData = MangaCover(
                                        mangaId = manga.id,
                                        sourceId = manga.source,
                                        isMangaFavorite = manga.favorite,
                                        url = manga.thumbnailUrl,
                                        lastModified = manga.coverLastModified,
                                    ),
                                    title = manga.title,
                                    onClick = {
                                        if (state.selectionMode) {
                                            screenModel.toggleSelection(manga)
                                        } else {
                                            navigator.push(MangaScreen(manga.id))
                                        }
                                    },
                                    onLongClick = { screenModel.toggleSelection(manga) },
                                    isSelected = state.selected.contains(manga),
                                    coverAlpha = if (manga.favorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f,
                                    badge = {
                                        InLibraryBadge(enabled = manga.favorite)
                                    },
                                )
                            }
                        }
                    }

                    LibraryDisplayMode.ComfortableGrid -> {
                        LazyVerticalGrid(
                            columns = columns,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = paddingValues + PaddingValues(8.dp) + PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridVerticalSpacer),
                            horizontalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridHorizontalSpacer),
                        ) {
                            items(
                                items = state.filteredMangaList,
                                key = { it.id },
                            ) { manga ->
                                MangaComfortableGridItem(
                                    coverData = MangaCover(
                                        mangaId = manga.id,
                                        sourceId = manga.source,
                                        isMangaFavorite = manga.favorite,
                                        url = manga.thumbnailUrl,
                                        lastModified = manga.coverLastModified,
                                    ),
                                    title = manga.title,
                                    onClick = {
                                        if (state.selectionMode) {
                                            screenModel.toggleSelection(manga)
                                        } else {
                                            navigator.push(MangaScreen(manga.id))
                                        }
                                    },
                                    onLongClick = { screenModel.toggleSelection(manga) },
                                    isSelected = state.selected.contains(manga),
                                    coverAlpha = if (manga.favorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f,
                                    coverBadgeStart = {
                                        InLibraryBadge(enabled = manga.favorite)
                                    },
                                )
                            }
                        }
                    }

                    LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> {
                        LazyVerticalGrid(
                            columns = columns,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = paddingValues + PaddingValues(8.dp) + PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridVerticalSpacer),
                            horizontalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridHorizontalSpacer),
                        ) {
                            items(
                                items = state.filteredMangaList,
                                key = { it.id },
                            ) { manga ->
                                MangaCompactGridItem(
                                    coverData = MangaCover(
                                        mangaId = manga.id,
                                        sourceId = manga.source,
                                        isMangaFavorite = manga.favorite,
                                        url = manga.thumbnailUrl,
                                        lastModified = manga.coverLastModified,
                                    ),
                                    title = manga.title.takeIf { displayMode == LibraryDisplayMode.CompactGrid },
                                    onClick = {
                                        if (state.selectionMode) {
                                            screenModel.toggleSelection(manga)
                                        } else {
                                            navigator.push(MangaScreen(manga.id))
                                        }
                                    },
                                    onLongClick = { screenModel.toggleSelection(manga) },
                                    isSelected = state.selected.contains(manga),
                                    coverAlpha = if (manga.favorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f,
                                    coverBadgeStart = {
                                        InLibraryBadge(enabled = manga.favorite)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenMangaBottomActionMenu(
    visible: Boolean,
    onUnhideClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = Alignment.Bottom),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom),
    ) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.large.copy(
                bottomEnd = ZeroCornerSize,
                bottomStart = ZeroCornerSize,
            ),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            val haptic = LocalHapticFeedback.current
            val scope = rememberCoroutineScope()
            var toConfirm by remember { mutableStateOf(false) }
            var resetJob by remember { mutableStateOf<Job?>(null) }

            val onLongClickItem = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                toConfirm = true
                resetJob?.cancel()
                resetJob = scope.launch {
                    delay(1000)
                    toConfirm = false
                }
            }

            Row(
                modifier = Modifier
                    .padding(
                        WindowInsets.navigationBars
                            .only(WindowInsetsSides.Bottom)
                            .asPaddingValues(),
                    )
                    .padding(horizontal = 8.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                val animatedWeight by animateFloatAsState(
                    targetValue = if (toConfirm) 2f else 1f,
                    label = "weight",
                )
                Box(
                    modifier = Modifier
                        .size(height = 48.dp, width = 160.dp)
                        .weight(animatedWeight)
                        .combinedClickable(
                            interactionSource = null,
                            indication = ripple(bounded = false),
                            onLongClick = onLongClickItem,
                            onClick = onUnhideClicked,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = stringResource(MR.strings.action_unhide),
                        )
                        AnimatedVisibility(
                            visible = toConfirm,
                            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                        ) {
                            Text(
                                text = stringResource(MR.strings.action_unhide),
                                overflow = TextOverflow.Visible,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

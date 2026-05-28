package eu.kanade.presentation.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.SearchToolbar
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.Pill
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.active
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.library.service.LibraryPreferences

@Composable
fun LibraryToolbar(
    hasActiveFilters: Boolean,
    canSelectCategory: Boolean,
    selectedCount: Int,
    title: LibraryToolbarTitle,
    onClickTitle: () -> Unit,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
    onClickFilter: () -> Unit,
    onClickRefresh: () -> Unit,
    onClickGlobalUpdate: () -> Unit,
    onClickOpenRandomManga: () -> Unit,
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
    filterDownloaded: TriState,
    filterUnread: TriState,
    filterStarted: TriState,
    filterBookmarked: TriState,
    filterCompleted: TriState,
    onToggleFilter: ((LibraryPreferences) -> Preference<TriState>) -> Unit,
) = when {
    selectedCount > 0 -> LibrarySelectionToolbar(
        selectedCount = selectedCount,
        onClickUnselectAll = onClickUnselectAll,
        onClickSelectAll = onClickSelectAll,
        onClickInvertSelection = onClickInvertSelection,
    )

    else -> LibraryRegularToolbar(
        title = title,
        hasFilters = hasActiveFilters,
        canSelectCategory = canSelectCategory,
        searchQuery = searchQuery,
        onClickTitle = onClickTitle,
        onSearchQueryChange = onSearchQueryChange,
        onClickFilter = onClickFilter,
        onClickRefresh = onClickRefresh,
        onClickGlobalUpdate = onClickGlobalUpdate,
        onClickOpenRandomManga = onClickOpenRandomManga,
        scrollBehavior = scrollBehavior,
        filterDownloaded = filterDownloaded,
        filterUnread = filterUnread,
        filterStarted = filterStarted,
        filterBookmarked = filterBookmarked,
        filterCompleted = filterCompleted,
        onToggleFilter = onToggleFilter,
    )
}

@Composable
private fun LibraryRegularToolbar(
    title: LibraryToolbarTitle,
    hasFilters: Boolean,
    canSelectCategory: Boolean,
    searchQuery: String?,
    onClickTitle: () -> Unit,
    onSearchQueryChange: (String?) -> Unit,
    onClickFilter: () -> Unit,
    onClickRefresh: () -> Unit,
    onClickGlobalUpdate: () -> Unit,
    onClickOpenRandomManga: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
    filterDownloaded: TriState,
    filterUnread: TriState,
    filterStarted: TriState,
    filterBookmarked: TriState,
    filterCompleted: TriState,
    onToggleFilter: ((LibraryPreferences) -> Preference<TriState>) -> Unit,
) {
    val pillAlpha = if (isSystemInDarkTheme()) 0.12f else 0.08f
    SearchToolbar(
        titleContent = {
            Column {
                Row(
                    modifier = if (canSelectCategory) {
                        Modifier.clickable(onClick = onClickTitle)
                    } else {
                        Modifier
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title.text,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, false),
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (canSelectCategory) {
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (title.numberOfManga != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Pill(
                            text = "${title.numberOfManga}",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = pillAlpha),
                            fontSize = 14.sp,
                        )
                    }
                }
                Text(
                    text = stringResource(MR.strings.library_continue_reading_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        searchQuery = searchQuery,
        onChangeSearchQuery = onSearchQueryChange,
        actions = {
            val filterTint = if (hasFilters) MaterialTheme.colorScheme.active else LocalContentColor.current
            AppBarActions(
                persistentListOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_filter),
                        icon = Icons.Outlined.FilterList,
                        iconTint = filterTint,
                        onClick = onClickFilter,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_update_library),
                        onClick = onClickGlobalUpdate,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_update_category),
                        onClick = onClickRefresh,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_open_random_manga),
                        onClick = onClickOpenRandomManga,
                    ),
                ),
            )
        },
        scrollBehavior = scrollBehavior,
        subContent = {
            if (searchQuery != null) {
                LibrarySearchFilterRow(
                    filterDownloaded = filterDownloaded,
                    filterUnread = filterUnread,
                    filterStarted = filterStarted,
                    filterBookmarked = filterBookmarked,
                    filterCompleted = filterCompleted,
                    onToggleFilter = onToggleFilter,
                )
            }
        },
    )
}

@Composable
private fun LibrarySearchFilterRow(
    filterDownloaded: TriState,
    filterUnread: TriState,
    filterStarted: TriState,
    filterBookmarked: TriState,
    filterCompleted: TriState,
    onToggleFilter: ((LibraryPreferences) -> Preference<TriState>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriStateFilterChip(
            label = stringResource(MR.strings.label_downloaded),
            state = filterDownloaded,
            onClick = { onToggleFilter(LibraryPreferences::filterDownloaded) },
        )
        TriStateFilterChip(
            label = stringResource(MR.strings.action_filter_unread),
            state = filterUnread,
            onClick = { onToggleFilter(LibraryPreferences::filterUnread) },
        )
        TriStateFilterChip(
            label = stringResource(MR.strings.label_started),
            state = filterStarted,
            onClick = { onToggleFilter(LibraryPreferences::filterStarted) },
        )
        TriStateFilterChip(
            label = stringResource(MR.strings.action_filter_bookmarked),
            state = filterBookmarked,
            onClick = { onToggleFilter(LibraryPreferences::filterBookmarked) },
        )
        TriStateFilterChip(
            label = stringResource(MR.strings.completed),
            state = filterCompleted,
            onClick = { onToggleFilter(LibraryPreferences::filterCompleted) },
        )
    }
}

@Composable
private fun TriStateFilterChip(
    label: String,
    state: TriState,
    onClick: () -> Unit,
) {
    val isSelected = state != TriState.DISABLED
    val isExcluded = state == TriState.ENABLED_NOT

    val colors = if (isExcluded) {
        FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    } else {
        FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }

    val icon = @Composable {
        if (state == TriState.ENABLED_IS) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        } else if (state == TriState.ENABLED_NOT) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text = label) },
        leadingIcon = if (isSelected) icon else null,
        colors = colors,
    )
}

@Composable
private fun LibrarySelectionToolbar(
    selectedCount: Int,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
) {
    AppBar(
        titleContent = { Text(text = "$selectedCount") },
        actions = {
            AppBarActions(
                persistentListOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_all),
                        icon = Icons.Outlined.SelectAll,
                        onClick = onClickSelectAll,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_inverse),
                        icon = Icons.Outlined.FlipToBack,
                        onClick = onClickInvertSelection,
                    ),
                ),
            )
        },
        isActionMode = true,
        onCancelActionMode = onClickUnselectAll,
    )
}

@Immutable
data class LibraryToolbarTitle(
    val text: String,
    val numberOfManga: Int? = null,
)

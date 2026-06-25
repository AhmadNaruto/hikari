package eu.kanade.presentation.updates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import me.saket.swipe.SwipeableActionsBox
import me.saket.swipe.SwipeAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.collections.immutable.ImmutableList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.manga.components.ChapterDownloadIndicator
import eu.kanade.presentation.manga.components.DotSeparatorText
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.updates.components.UpdatesGroupHeader
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.HikariCardGroup
import tachiyomi.presentation.core.components.HikariGroupedListItem
import tachiyomi.presentation.core.components.HikariListItemPosition
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

internal fun LazyListScope.updatesLastUpdatedItem(
    lastUpdated: Long,
) {
    item(key = "updates-lastUpdated") {
        HikariCardGroup(modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)) {
            Box(
                modifier = Modifier.padding(MaterialTheme.padding.medium),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.padding.medium))
                    Column {
                        Text(
                            text = stringResource(MR.strings.updates_last_update_info, ""),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = relativeTimeSpanString(lastUpdated),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

internal fun LazyListScope.updatesUiItems(
    uiModels: ImmutableList<UpdatesUiModel>,
    selectionMode: Boolean,
    onUpdateSelected: (UpdatesItem, Boolean, Boolean) -> Unit,
    onClickCover: (UpdatesItem) -> Unit,
    onClickUpdate: (UpdatesItem) -> Unit,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
    onMultiBookmarkClicked: (List<UpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<UpdatesItem>, read: Boolean) -> Unit,
) {
    uiModels.forEachIndexed { index, item ->
        when (item) {
            is UpdatesUiModel.Header -> {
                item(
                    key = "updatesHeader-${item.hashCode()}",
                    contentType = "header",
                ) {
                    val count = remember(uiModels, index) {
                        var c = 0
                        for (i in index + 1 until uiModels.size) {
                            if (uiModels[i] is UpdatesUiModel.Item) c++ else break
                        }
                        c
                    }
                    UpdatesGroupHeader(
                        modifier = Modifier.animateItemFastScroll(),
                        text = relativeDateText(item.date),
                        count = count,
                    )
                }
            }

            is UpdatesUiModel.Item -> {
                val updatesItem = item.item
                item(
                    key = "updates-${updatesItem.update.mangaId}-${updatesItem.update.chapterId}",
                    contentType = "item",
                ) {
                    val position = remember(uiModels, index) {
                        val prev = uiModels.getOrNull(index - 1)
                        val next = uiModels.getOrNull(index + 1)
                        val isFirst = prev == null || prev is UpdatesUiModel.Header
                        val isLast = next == null || next is UpdatesUiModel.Header
                        when {
                            isFirst && isLast -> ItemPosition.Single
                            isFirst -> ItemPosition.First
                            isLast -> ItemPosition.Last
                            else -> ItemPosition.Middle
                        }
                    }
                    UpdatesUiItem(
                        modifier = Modifier.animateItemFastScroll(),
                        update = updatesItem.update,
                        selected = updatesItem.selected,
                        position = position,
                        readProgress = updatesItem.update.lastPageRead
                            .takeIf { !updatesItem.update.read && it > 0L }
                            ?.let {
                                stringResource(
                                    MR.strings.chapter_progress,
                                    it + 1,
                                )
                            },
                        onLongClick = {
                            onUpdateSelected(updatesItem, !updatesItem.selected, true)
                        },
                        onClick = {
                            when {
                                selectionMode -> onUpdateSelected(updatesItem, !updatesItem.selected, false)
                                else -> onClickUpdate(updatesItem)
                            }
                        },
                        onClickCover = { onClickCover(updatesItem) }.takeIf { !selectionMode },
                        onDownloadChapter = { action: ChapterDownloadAction ->
                            onDownloadChapter(listOf(updatesItem), action)
                        }.takeIf { !selectionMode },
                        onToggleBookmark = {
                            onMultiBookmarkClicked(listOf(updatesItem), !updatesItem.update.bookmark)
                        }.takeIf { !selectionMode },
                        onToggleRead = {
                            onMultiMarkAsReadClicked(listOf(updatesItem), !updatesItem.update.read)
                        }.takeIf { !selectionMode },
                        downloadStateProvider = updatesItem.downloadStateProvider,
                        downloadProgressProvider = updatesItem.downloadProgressProvider,
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdatesUiItem(
    update: UpdatesWithRelations,
    selected: Boolean,
    position: ItemPosition,
    readProgress: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickCover: (() -> Unit)?,
    onDownloadChapter: ((ChapterDownloadAction) -> Unit)?,
    onToggleBookmark: (() -> Unit)?,
    onToggleRead: (() -> Unit)?,
    downloadStateProvider: () -> Download.State,
    downloadProgressProvider: () -> Int,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val textAlpha = if (update.read) DISABLED_ALPHA else 1f

    val bookmarkAction = onToggleBookmark?.let {
        swipeAction(
            onSwipe = it,
            icon = if (update.bookmark) Icons.Outlined.BookmarkRemove else Icons.Outlined.BookmarkAdd,
            background = MaterialTheme.colorScheme.tertiaryContainer,
        )
    }
    val readAction = onToggleRead?.let {
        swipeAction(
            onSwipe = it,
            icon = if (update.read) Icons.Outlined.RemoveDone else Icons.Outlined.Done,
            background = MaterialTheme.colorScheme.primaryContainer,
        )
    }

    if (bookmarkAction != null || readAction != null) {
        SwipeableActionsBox(
            modifier = modifier.clipToBounds(),
            startActions = listOfNotNull(readAction),
            endActions = listOfNotNull(bookmarkAction),
            swipeThreshold = 56.dp,
            backgroundUntilSwipeThreshold = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            HikariGroupedListItem(
                modifier = Modifier,
                position = position.toHikariListItemPosition(),
                selected = selected,
                height = 72.dp,
                onClick = onClick,
                onLongClick = {
                    onLongClick()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = MaterialTheme.padding.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MangaCover.Square(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxHeight(),
                        data = update.coverData,
                        onClick = onClickCover,
                    )

                    Column(
                        modifier = Modifier
                            .padding(horizontal = MaterialTheme.padding.medium)
                            .weight(1f),
                    ) {
                        Text(
                            text = update.mangaTitle,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = textAlpha),
                            overflow = TextOverflow.Ellipsis,
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            var textHeight by remember { mutableIntStateOf(0) }
                            if (!update.read) {
                                Icon(
                                    imageVector = Icons.Filled.Circle,
                                    contentDescription = stringResource(MR.strings.unread),
                                    modifier = Modifier.size(8.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            if (update.bookmark) {
                                Icon(
                                    imageVector = Icons.Filled.Bookmark,
                                    contentDescription = stringResource(MR.strings.action_filter_bookmarked),
                                    modifier = Modifier
                                        .sizeIn(maxHeight = with(LocalDensity.current) { textHeight.toDp() - 2.dp }),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            Text(
                                text = update.chapterName,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalContentColor.current.copy(alpha = textAlpha),
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { textHeight = it.size.height },
                                modifier = Modifier
                                    .weight(weight = 1f, fill = false),
                            )
                            if (readProgress != null) {
                                DotSeparatorText()
                                Text(
                                    text = readProgress,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalContentColor.current.copy(alpha = textAlpha),
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    ChapterDownloadIndicator(
                        enabled = onDownloadChapter != null,
                        modifier = Modifier.padding(start = 4.dp),
                        downloadStateProvider = downloadStateProvider,
                        downloadProgressProvider = downloadProgressProvider,
                        onClick = { onDownloadChapter?.invoke(it) },
                    )
                }
            }
        }
    } else {
        HikariGroupedListItem(
            modifier = modifier,
            position = position.toHikariListItemPosition(),
            selected = selected,
            height = 72.dp,
            onClick = onClick,
            onLongClick = {
                onLongClick()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = MaterialTheme.padding.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MangaCover.Square(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxHeight(),
                    data = update.coverData,
                    onClick = onClickCover,
                )

                Column(
                    modifier = Modifier
                        .padding(horizontal = MaterialTheme.padding.medium)
                        .weight(1f),
                ) {
                    Text(
                        text = update.mangaTitle,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalContentColor.current.copy(alpha = textAlpha),
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        var textHeight by remember { mutableIntStateOf(0) }
                        if (!update.read) {
                            Icon(
                                imageVector = Icons.Filled.Circle,
                                contentDescription = stringResource(MR.strings.unread),
                                modifier = Modifier.size(8.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        if (update.bookmark) {
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = stringResource(MR.strings.action_filter_bookmarked),
                                modifier = Modifier
                                    .sizeIn(maxHeight = with(LocalDensity.current) { textHeight.toDp() - 2.dp }),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                        Text(
                            text = update.chapterName,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalContentColor.current.copy(alpha = textAlpha),
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { textHeight = it.size.height },
                            modifier = Modifier
                                .weight(weight = 1f, fill = false),
                        )
                        if (readProgress != null) {
                            DotSeparatorText()
                            Text(
                                text = readProgress,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalContentColor.current.copy(alpha = textAlpha),
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                ChapterDownloadIndicator(
                    enabled = onDownloadChapter != null,
                    modifier = Modifier.padding(start = 4.dp),
                    downloadStateProvider = downloadStateProvider,
                    downloadProgressProvider = downloadProgressProvider,
                    onClick = { onDownloadChapter?.invoke(it) },
                )
            }
        }
    }
}

private fun ItemPosition.toHikariListItemPosition(): HikariListItemPosition {
    return when (this) {
        ItemPosition.First -> HikariListItemPosition.First
        ItemPosition.Middle -> HikariListItemPosition.Middle
        ItemPosition.Last -> HikariListItemPosition.Last
        ItemPosition.Single -> HikariListItemPosition.Single
    }
}

enum class ItemPosition {
    First,
    Middle,
    Last,
    Single,
}

private fun swipeAction(
    onSwipe: () -> Unit,
    icon: ImageVector,
    background: Color,
    isUndo: Boolean = false,
): SwipeAction {
    return SwipeAction(
        icon = {
            Icon(
                modifier = androidx.compose.ui.Modifier.padding(16.dp),
                imageVector = icon,
                tint = contentColorFor(background),
                contentDescription = null,
            )
        },
        background = background,
        onSwipe = onSwipe,
        isUndo = isUndo,
    )
}

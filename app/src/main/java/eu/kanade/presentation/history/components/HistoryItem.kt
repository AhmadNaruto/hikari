package eu.kanade.presentation.history.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.contentColorFor
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import me.saket.swipe.SwipeableActionsBox
import me.saket.swipe.SwipeAction
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.util.lang.toTimestampString
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.HikariGroupedListItem
import tachiyomi.presentation.core.components.HikariListItemPosition
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun HistoryItem(
    history: HistoryWithRelations,
    position: ItemPosition,
    onClickCover: () -> Unit,
    onClickResume: () -> Unit,
    onClickDelete: () -> Unit,
    onClickFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val favoriteAction = swipeAction(
        onSwipe = onClickFavorite,
        icon = if (history.coverData.isMangaFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        background = MaterialTheme.colorScheme.primaryContainer,
    )
    val deleteAction = swipeAction(
        onSwipe = onClickDelete,
        icon = Icons.Outlined.Delete,
        background = MaterialTheme.colorScheme.errorContainer,
    )

    SwipeableActionsBox(
        modifier = modifier.clipToBounds(),
        startActions = listOf(favoriteAction),
        endActions = listOf(deleteAction),
        swipeThreshold = 56.dp,
        backgroundUntilSwipeThreshold = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        HikariGroupedListItem(
            modifier = Modifier,
            position = position.toHikariListItemPosition(),
            height = 72.dp,
            onClick = onClickResume,
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
                    data = history.coverData,
                    onClick = onClickCover,
                )
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = MaterialTheme.padding.medium),
                ) {
                    val textStyle = MaterialTheme.typography.bodyMedium
                    Text(
                        text = history.title,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = textStyle,
                    )
                    val readAt = remember { history.readAt?.toTimestampString() ?: "" }
                    Text(
                        text = if (history.chapterNumber > -1) {
                            stringResource(
                                MR.strings.recent_manga_time,
                                formatChapterNumber(history.chapterNumber),
                                readAt,
                            )
                        } else {
                            readAt
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (!history.coverData.isMangaFavorite) {
                    IconButton(onClick = onClickFavorite) {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(MR.strings.add_to_library),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                IconButton(onClick = onClickDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(MR.strings.action_delete),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

fun ItemPosition.toHikariListItemPosition(): HikariListItemPosition {
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

@PreviewLightDark
@Composable
private fun HistoryItemPreviews(
    @PreviewParameter(HistoryWithRelationsProvider::class)
    historyWithRelations: HistoryWithRelations,
) {
    TachiyomiPreviewTheme {
        Surface {
            HistoryItem(
                history = historyWithRelations,
                position = ItemPosition.Single,
                onClickCover = {},
                onClickResume = {},
                onClickDelete = {},
                onClickFavorite = {},
            )
        }
    }
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

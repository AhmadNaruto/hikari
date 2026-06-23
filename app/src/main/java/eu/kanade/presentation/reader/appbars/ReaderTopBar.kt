package eu.kanade.presentation.reader.appbars

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ReaderTopBar(
    mangaTitle: String?,
    chapterTitle: String?,
    navigateUp: () -> Unit,
    bookmarked: Boolean,
    onToggleBookmarked: () -> Unit,
    onOpenInWebView: (() -> Unit)?,
    onOpenInBrowser: (() -> Unit)?,
    onShare: (() -> Unit)?,
    chapters: List<ReaderChapter>,
    currentChapter: ReaderChapter?,
    onChapterSelected: (ReaderChapter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showChapterSheet by remember { mutableStateOf(false) }

    AppBar(
        modifier = modifier,
        backgroundColor = Color.Transparent,
        navigateUp = navigateUp,
        titleContent = {
            Column(
                modifier = Modifier
                    .clickable { showChapterSheet = true }
                    .padding(vertical = 4.dp),
            ) {
                mangaTitle?.let {
                    Text(
                        text = it,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                chapterTitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(
                            repeatDelayMillis = 2_000,
                        ),
                    )
                }
            }
        },
        actions = {
            AppBarActions(
                actions = persistentListOf<AppBar.AppBarAction>().builder()
                    .apply {
                        add(
                            AppBar.Action(
                                title = stringResource(
                                    if (bookmarked) {
                                        MR.strings.action_remove_bookmark
                                    } else {
                                        MR.strings.action_bookmark
                                    },
                                ),
                                icon = if (bookmarked) {
                                    Icons.Outlined.Bookmark
                                } else {
                                    Icons.Outlined.BookmarkBorder
                                },
                                onClick = onToggleBookmarked,
                            ),
                        )
                        onOpenInWebView?.let {
                            add(
                                AppBar.OverflowAction(
                                    title = stringResource(MR.strings.action_open_in_web_view),
                                    onClick = it,
                                ),
                            )
                        }
                        onOpenInBrowser?.let {
                            add(
                                AppBar.OverflowAction(
                                    title = stringResource(MR.strings.action_open_in_browser),
                                    onClick = it,
                                ),
                            )
                        }
                        onShare?.let {
                            add(
                                AppBar.OverflowAction(
                                    title = stringResource(MR.strings.action_share),
                                    onClick = it,
                                ),
                            )
                        }
                    }
                    .build(),
            )
        },
    )

    if (showChapterSheet && chapters.isNotEmpty()) {
        val activeChapterIndex = remember(chapters, currentChapter) {
            chapters.indexOfFirst { it.chapter.id == currentChapter?.chapter?.id }.coerceAtLeast(0)
        }
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = activeChapterIndex)

        AdaptiveSheet(
            onDismissRequest = { showChapterSheet = false },
            header = {
                Text(
                    text = stringResource(MR.strings.chapters),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 24.dp,
                            top = 16.dp,
                            end = 24.dp,
                            bottom = 16.dp,
                        ),
                )
            },
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(chapters) { chapter ->
                    val isCurrent = chapter.chapter.id == currentChapter?.chapter?.id
                    val isRead = chapter.chapter.read
                    val color = if (isCurrent) {
                        MaterialTheme.colorScheme.primary
                    } else if (isRead) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    val fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal

                    Text(
                        text = chapter.chapter.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = color,
                        fontWeight = fontWeight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showChapterSheet = false
                                onChapterSelected(chapter)
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

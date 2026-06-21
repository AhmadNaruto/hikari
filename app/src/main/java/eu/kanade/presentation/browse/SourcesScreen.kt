package eu.kanade.presentation.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import eu.kanade.presentation.components.AdaptiveSheet
import tachiyomi.presentation.core.components.HikariGroupedListItem
import tachiyomi.presentation.core.components.HikariListItemPosition
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.components.BadgeGroup
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.presentation.history.components.ItemPosition
import eu.kanade.tachiyomi.ui.browse.source.SourcesScreenModel
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel.Listing
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.source.model.Pin
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.SECONDARY_ALPHA
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.plus
import tachiyomi.source.local.isLocal

@Composable
fun SourcesScreen(
    state: SourcesScreenModel.State,
    contentPadding: PaddingValues,
    onClickItem: (Source, Listing) -> Unit,
    onClickPin: (Source) -> Unit,
    onLongClickItem: (Source) -> Unit,
) {
    when {
        state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
        state.isEmpty -> EmptyScreen(
            stringRes = MR.strings.source_empty_screen,
            modifier = Modifier.padding(contentPadding),
        )
        else -> {
            val counts = remember(state.items) {
                val map = mutableMapOf<String, Int>()
                var currentHeader: String? = null
                var currentCount = 0
                for (item in state.items) {
                    when (item) {
                        is SourceUiModel.Header -> {
                            if (currentHeader != null) {
                                map[currentHeader] = currentCount
                            }
                            currentHeader = item.language
                            currentCount = 0
                        }
                        is SourceUiModel.Item -> {
                            currentCount++
                        }
                    }
                }
                if (currentHeader != null) {
                    map[currentHeader] = currentCount
                }
                map
            }

            ScrollbarLazyColumn(
                contentPadding = contentPadding + topSmallPaddingValues,
            ) {
                itemsIndexed(
                    items = state.items,
                    contentType = { _, it ->
                        when (it) {
                            is SourceUiModel.Header -> "header"
                            is SourceUiModel.Item -> "item"
                        }
                    },
                    key = { _, it ->
                        when (it) {
                            is SourceUiModel.Header -> it.hashCode()
                            is SourceUiModel.Item -> "source-${it.source.key()}"
                        }
                    },
                ) { index, model ->
                    when (model) {
                        is SourceUiModel.Header -> {
                            val count = counts[model.language] ?: 0
                            SourceHeader(
                                modifier = Modifier.animateItem(),
                                language = model.language,
                                count = count,
                            )
                        }
                        is SourceUiModel.Item -> {
                            val prevIsHeaderOrNull = index == 0 || state.items[index - 1] is SourceUiModel.Header
                            val nextIsHeaderOrNull =
                                index == state.items.lastIndex || state.items[index + 1] is SourceUiModel.Header
                            val position = when {
                                prevIsHeaderOrNull && nextIsHeaderOrNull -> ItemPosition.Single
                                prevIsHeaderOrNull -> ItemPosition.First
                                nextIsHeaderOrNull -> ItemPosition.Last
                                else -> ItemPosition.Middle
                            }
                            SourceItem(
                                modifier = Modifier.animateItem(),
                                source = model.source,
                                position = position,
                                onClickItem = onClickItem,
                                onLongClickItem = onLongClickItem,
                                onClickPin = onClickPin,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceHeader(
    language: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = MaterialTheme.padding.medium,
                end = MaterialTheme.padding.medium,
                top = MaterialTheme.padding.small,
                bottom = MaterialTheme.padding.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        Text(
            text = LocaleHelper.getSourceDisplayName(language, context),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (count > 0) {
            BadgeGroup {
                Badge(
                    text = "$count",
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun SourceItem(
    source: Source,
    position: ItemPosition,
    onClickItem: (Source, Listing) -> Unit,
    onLongClickItem: (Source) -> Unit,
    onClickPin: (Source) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseSourceItem(
        modifier = modifier,
        source = source,
        position = position,
        onClickItem = { onClickItem(source, Listing.Popular) },
        onLongClickItem = { onLongClickItem(source) },
        action = {
            if (source.supportsLatest) {
                TextButton(onClick = { onClickItem(source, Listing.Latest) }) {
                    Text(
                        text = stringResource(MR.strings.latest),
                        style = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
            SourcePinButton(
                isPinned = Pin.Pinned in source.pin,
                onClick = { onClickPin(source) },
            )
        },
    )
}

@Composable
private fun SourcePinButton(
    isPinned: Boolean,
    onClick: () -> Unit,
) {
    val icon = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin
    val tint = if (isPinned) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onBackground.copy(
            alpha = SECONDARY_ALPHA,
        )
    }
    val description = if (isPinned) MR.strings.action_unpin else MR.strings.action_pin
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            tint = tint,
            contentDescription = stringResource(description),
        )
    }
}

@Composable
fun SourceOptionsDialog(
    source: Source,
    onClickPin: () -> Unit,
    onClickDisable: () -> Unit,
    onDismiss: () -> Unit,
) {
    AdaptiveSheet(
        onDismissRequest = onDismiss,
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = source.visualName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isLocal = source.isLocal()
            val textId = if (Pin.Pinned in source.pin) MR.strings.action_unpin else MR.strings.action_pin

            HikariGroupedListItem(
                position = if (isLocal) HikariListItemPosition.Single else HikariListItemPosition.First,
                onClick = {
                    onClickPin()
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(textId),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (!isLocal) {
                HikariGroupedListItem(
                    position = HikariListItemPosition.Last,
                    onClick = {
                        onClickDisable()
                        onDismiss()
                    }
                ) {
                    Text(
                        text = stringResource(MR.strings.action_disable),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

sealed interface SourceUiModel {
    data class Item(val source: Source) : SourceUiModel
    data class Header(val language: String) : SourceUiModel
}

package eu.kanade.presentation.more.settings.screen.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.util.system.copyToClipboard
import hikari.domain.extensionrepo.model.ExtensionRepo
import kotlinx.collections.immutable.ImmutableSet
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.HikariGroupedListItem
import tachiyomi.presentation.core.components.HikariListItemPosition
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ExtensionReposContent(
    repos: ImmutableSet<ExtensionRepo>,
    disabledRepos: ImmutableSet<String>,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    onOpenWebsite: (ExtensionRepo) -> Unit,
    onClickDelete: (String) -> Unit,
    onClickToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reposList = remember(repos) { repos.toList() }
    LazyColumn(
        state = lazyListState,
        contentPadding = paddingValues,
        modifier = modifier,
    ) {
        itemsIndexed(
            items = reposList,
            key = { _, repo -> repo.baseUrl },
        ) { index, repo ->
            val position = when {
                reposList.size == 1 -> HikariListItemPosition.Single
                index == 0 -> HikariListItemPosition.First
                index == reposList.size - 1 -> HikariListItemPosition.Last
                else -> HikariListItemPosition.Middle
            }
            ExtensionRepoListItem(
                modifier = Modifier.animateItem(),
                repo = repo,
                position = position,
                isEnabled = repo.baseUrl !in disabledRepos,
                onOpenWebsite = { onOpenWebsite(repo) },
                onDelete = { onClickDelete(repo.baseUrl) },
                onToggle = { onClickToggle(repo.baseUrl) },
            )
        }
    }
}

@Composable
private fun ExtensionRepoListItem(
    repo: ExtensionRepo,
    position: HikariListItemPosition,
    isEnabled: Boolean,
    onOpenWebsite: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    HikariGroupedListItem(
        position = position,
        modifier = modifier,
        horizontalPadding = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MaterialTheme.padding.medium,
                    top = MaterialTheme.padding.medium,
                    end = MaterialTheme.padding.medium,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.AutoMirrored.Outlined.Label, contentDescription = null)
            Text(
                text = repo.name,
                modifier = Modifier
                    .padding(start = MaterialTheme.padding.medium)
                    .weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = if (isEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onOpenWebsite) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = stringResource(MR.strings.action_open_in_browser),
                )
            }

            IconButton(
                onClick = {
                    val url = "${repo.baseUrl}/index.min.json"
                    context.copyToClipboard(url, url)
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(MR.strings.action_copy_to_clipboard),
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(MR.strings.action_delete),
                )
            }
        }
    }
}

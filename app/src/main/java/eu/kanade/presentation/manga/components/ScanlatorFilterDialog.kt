package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DisabledByDefault
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.HikariGroupedListItem
import tachiyomi.presentation.core.components.HikariListItemPosition
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ScanlatorFilterDialog(
    availableScanlators: Set<String>,
    excludedScanlators: Set<String>,
    onDismissRequest: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    val sortedAvailableScanlators = remember(availableScanlators) {
        availableScanlators.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
    }
    val mutableExcludedScanlators = remember(excludedScanlators) { excludedScanlators.toMutableStateList() }
    
    AdaptiveSheet(
        onDismissRequest = onDismissRequest,
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(MR.strings.exclude_scanlators),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            if (sortedAvailableScanlators.isEmpty()) {
                Text(
                    text = stringResource(MR.strings.no_scanlators_found),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val state = rememberLazyListState()
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                ) {
                    LazyColumn(
                        state = state,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(sortedAvailableScanlators.size) { index ->
                            val scanlator = sortedAvailableScanlators[index]
                            val isExcluded = mutableExcludedScanlators.contains(scanlator)
                            val position = when {
                                sortedAvailableScanlators.size == 1 -> HikariListItemPosition.Single
                                index == 0 -> HikariListItemPosition.First
                                index == sortedAvailableScanlators.size - 1 -> HikariListItemPosition.Last
                                else -> HikariListItemPosition.Middle
                            }
                            
                            HikariGroupedListItem(
                                position = position,
                                onClick = {
                                    if (isExcluded) {
                                        mutableExcludedScanlators.remove(scanlator)
                                    } else {
                                        mutableExcludedScanlators.add(scanlator)
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isExcluded) {
                                            Icons.Rounded.DisabledByDefault
                                        } else {
                                            Icons.Rounded.CheckBoxOutlineBlank
                                        },
                                        tint = if (isExcluded) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            LocalContentColor.current
                                        },
                                        contentDescription = null,
                                    )
                                    Text(
                                        text = scanlator,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 16.dp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    if (state.canScrollBackward) HorizontalDivider(modifier = Modifier.align(Alignment.TopCenter))
                    if (state.canScrollForward) HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
            
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (sortedAvailableScanlators.isNotEmpty()) {
                    if (mutableExcludedScanlators.isEmpty()) {
                        TextButton(onClick = { mutableExcludedScanlators.addAll(availableScanlators) }) {
                            Text(text = stringResource(MR.strings.action_select_all))
                        }
                    } else {
                        TextButton(onClick = mutableExcludedScanlators::clear) {
                            Text(text = stringResource(MR.strings.action_reset))
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
                if (sortedAvailableScanlators.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = {
                            onConfirm(mutableExcludedScanlators.toSet())
                            onDismissRequest()
                        }
                    ) {
                        Text(text = stringResource(MR.strings.action_ok))
                    }
                }
            }
        }
    }
}

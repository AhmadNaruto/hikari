package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.AdaptiveSheet
import tachiyomi.presentation.core.components.material.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import eu.kanade.presentation.more.settings.Preference
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LabeledCheckbox
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MultiSelectListPreferenceWidget(
    preference: Preference.PreferenceItem.MultiSelectListPreference,
    values: Set<String>,
    onValuesChange: (Set<String>) -> Unit,
) {
    var isDialogShown by remember { mutableStateOf(false) }

    TextPreferenceWidget(
        title = preference.title,
        subtitle = preference.subtitleProvider(values, preference.entries),
        icon = preference.icon,
        onPreferenceClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        val selected = remember {
            preference.entries.keys
                .filter { values.contains(it) }
                .toMutableStateList()
        }
        AdaptiveSheet(
            onDismissRequest = { isDialogShown = false },
            header = {
                Text(
                    text = preference.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = MaterialTheme.padding.medium,
                            top = MaterialTheme.padding.small,
                            end = MaterialTheme.padding.medium,
                            bottom = MaterialTheme.padding.small,
                        ),
                )
            },
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.padding.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
            ) {
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    preference.entries.forEach { current ->
                        item {
                            val isSelected = selected.contains(current.key)
                            LabeledCheckbox(
                                label = current.value,
                                checked = isSelected,
                                onCheckedChange = {
                                    if (it) {
                                        selected.add(current.key)
                                    } else {
                                        selected.remove(current.key)
                                    }
                                },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { isDialogShown = false },
                    ) {
                        Text(text = stringResource(MR.strings.action_cancel))
                    }
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onValuesChange(selected.toMutableSet())
                            isDialogShown = false
                        },
                    ) {
                        Text(text = stringResource(MR.strings.action_ok))
                    }
                }
            }
        }
    }
}

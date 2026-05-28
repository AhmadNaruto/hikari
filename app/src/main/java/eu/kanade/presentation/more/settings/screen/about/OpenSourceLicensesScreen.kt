package eu.kanade.presentation.more.settings.screen.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.util.htmlReadyLicenseContent
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.HikariGroupedListItem
import tachiyomi.presentation.core.components.HikariListItemPosition
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.plus

class OpenSourceLicensesScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.licenses),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            val libraries by produceLibraries(R.raw.aboutlibraries)
            val libs = libraries
            if (libs == null) {
                LoadingScreen(Modifier.padding(contentPadding))
            } else {
                val libsList = remember(libs) { libs.libraries }
                ScrollbarLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding + PaddingValues(horizontal = MaterialTheme.padding.medium, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    itemsIndexed(
                        items = libsList,
                        key = { _, lib -> lib.name },
                    ) { index, lib ->
                        val position = when {
                            libsList.size == 1 -> HikariListItemPosition.Single
                            index == 0 -> HikariListItemPosition.First
                            index == libsList.size - 1 -> HikariListItemPosition.Last
                            else -> HikariListItemPosition.Middle
                        }

                        LibraryListItem(
                            library = lib,
                            position = position,
                            onClick = {
                                navigator.push(
                                    OpenSourceLibraryLicenseScreen(
                                        name = lib.name,
                                        website = lib.website,
                                        license = lib.licenses.firstOrNull()?.htmlReadyLicenseContent.orEmpty(),
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun LibraryListItem(
        library: Library,
        position: HikariListItemPosition,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        HikariGroupedListItem(
            position = position,
            modifier = modifier,
            horizontalPadding = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(
                        horizontal = MaterialTheme.padding.medium,
                        vertical = 12.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = library.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val version = library.artifactVersion
                    if (!version.isNullOrEmpty()) {
                        Text(
                            text = version,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

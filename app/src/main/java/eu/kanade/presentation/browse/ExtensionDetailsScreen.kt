package eu.kanade.presentation.browse

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.DisplayMetrics
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import eu.kanade.presentation.components.AdaptiveSheet
import tachiyomi.presentation.core.components.material.padding
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.domain.extension.interactor.ExtensionSourceItem
import eu.kanade.presentation.browse.components.ExtensionIcon
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.WarningBanner
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsScreenModel
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.copyToClipboard
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.HikariCard
import tachiyomi.presentation.core.components.HikariCardDefaults
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.Switch
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
fun ExtensionDetailsScreen(
    navigateUp: () -> Unit,
    state: ExtensionDetailsScreenModel.State,
    onClickSourcePreferences: (sourceId: Long) -> Unit,
    onClickEnableAll: () -> Unit,
    onClickDisableAll: () -> Unit,
    onClickClearCookies: () -> Unit,
    onClickUninstall: () -> Unit,
    onClickSource: (sourceId: Long) -> Unit,
    onClickIncognito: (Boolean) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val url = remember(state.extension) {
        val regex = """https://raw.githubusercontent.com/(.+?)/(.+?)/.+""".toRegex()
        regex.find(state.extension?.repoUrl.orEmpty())
            ?.let {
                val (user, repo) = it.destructured
                "https://github.com/$user/$repo"
            }
            ?: state.extension?.repoUrl
    }

    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.label_extension_info),
                navigateUp = navigateUp,
                actions = {
                    AppBarActions(
                        actions = persistentListOf<AppBar.AppBarAction>().builder()
                            .apply {
                                if (url != null) {
                                    add(
                                        AppBar.Action(
                                            title = stringResource(MR.strings.action_open_repo),
                                            icon = Icons.AutoMirrored.Outlined.Launch,
                                            onClick = {
                                                uriHandler.openUri(url)
                                            },
                                        ),
                                    )
                                }
                                addAll(
                                    listOf(
                                        AppBar.OverflowAction(
                                            title = stringResource(MR.strings.action_enable_all),
                                            onClick = onClickEnableAll,
                                        ),
                                        AppBar.OverflowAction(
                                            title = stringResource(MR.strings.action_disable_all),
                                            onClick = onClickDisableAll,
                                        ),
                                        AppBar.OverflowAction(
                                            title = stringResource(MR.strings.pref_clear_cookies),
                                            onClick = onClickClearCookies,
                                        ),
                                    ),
                                )
                            }
                            .build(),
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        if (state.extension == null) {
            EmptyScreen(
                MR.strings.empty_screen,
                modifier = Modifier.padding(paddingValues),
            )
            return@Scaffold
        }

        ExtensionDetails(
            contentPadding = paddingValues,
            extension = state.extension,
            sources = state.sources,
            incognitoMode = state.isIncognito,
            onClickSourcePreferences = onClickSourcePreferences,
            onClickUninstall = onClickUninstall,
            onClickSource = onClickSource,
            onClickIncognito = onClickIncognito,
        )
    }
}

@Composable
private fun ExtensionDetails(
    contentPadding: PaddingValues,
    extension: Extension.Installed,
    sources: ImmutableList<ExtensionSourceItem>,
    incognitoMode: Boolean,
    onClickSourcePreferences: (sourceId: Long) -> Unit,
    onClickUninstall: () -> Unit,
    onClickSource: (sourceId: Long) -> Unit,
    onClickIncognito: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var showNsfwWarning by remember { mutableStateOf(false) }

    ScrollbarLazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
    ) {
        if (extension.isObsolete) {
            item {
                WarningBanner(MR.strings.obsolete_extension_message)
            }
        }

        item {
            DetailsHeader(
                extension = extension,
                onClickUninstall = onClickUninstall,
                onClickAppInfo = {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", extension.pkgName, null)
                        context.startActivity(this)
                    }
                    Unit
                }.takeIf { extension.isShared },
                onClickAgeRating = {
                    showNsfwWarning = true
                },
            )
        }

        item {
            IncognitoCard(
                incognitoMode = incognitoMode,
                onIncognitoChange = onClickIncognito,
            )
        }

        item {
            LanguagesCard(
                sources = sources,
                onClickSourcePreferences = onClickSourcePreferences,
                onClickSource = onClickSource,
            )
        }
    }

    if (showNsfwWarning) {
        NsfwWarningDialog(
            onClickConfirm = {
                showNsfwWarning = false
            },
        )
    }
}

@Composable
private fun DetailsHeader(
    extension: Extension,
    onClickAgeRating: () -> Unit,
    onClickUninstall: () -> Unit,
    onClickAppInfo: (() -> Unit)?,
) {
    val context = LocalContext.current
    HikariCard(
        modifier = Modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = MaterialTheme.padding.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val extDebugInfo = buildString {
                            append(
                                """
                                Extension name: ${extension.name} (lang: ${extension.lang}; package: ${extension.pkgName})
                                Extension version: ${extension.versionName} (lib: ${extension.libVersion}; version code: ${extension.versionCode})
                                NSFW: ${extension.isNsfw}
                                """.trimIndent(),
                            )

                            if (extension is Extension.Installed) {
                                append("\n\n")
                                append(
                                    """
                                    Update available: ${extension.hasUpdate}
                                    Obsolete: ${extension.isObsolete}
                                    Shared: ${extension.isShared}
                                    Repository: ${extension.repoUrl}
                                    """.trimIndent(),
                                )
                            }
                        }
                        context.copyToClipboard("Extension Debug information", extDebugInfo)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ExtensionIcon(
                    modifier = Modifier.size(96.dp),
                    extension = extension,
                    density = DisplayMetrics.DENSITY_XXXHIGH,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = extension.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(4.dp))

                val strippedPkgName = extension.pkgName.substringAfter("eu.kanade.tachiyomi.extension.")
                Text(
                    text = strippedPkgName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.secondaryItemAlpha(),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaterialTheme.padding.small),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatsColumn(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Sell,
                    value = extension.versionName,
                    label = stringResource(MR.strings.ext_info_version),
                )

                VerticalDivider(modifier = Modifier.height(32.dp))

                StatsColumn(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Language,
                    value = LocaleHelper.getSourceDisplayName(extension.lang, context),
                    label = stringResource(MR.strings.ext_info_language),
                )

                VerticalDivider(modifier = Modifier.height(32.dp))

                val ageRatingText = if (extension.isNsfw) "18+" else "Safe"
                StatsColumn(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Shield,
                    value = ageRatingText,
                    label = stringResource(MR.strings.ext_info_age_rating),
                    onClick = onClickAgeRating,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onClickUninstall,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(MR.strings.ext_uninstall))
                }

                if (onClickAppInfo != null) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onClickAppInfo,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(MR.strings.ext_app_info),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsColumn(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = modifier.then(clickableModifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.secondaryItemAlpha(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun IncognitoCard(
    incognitoMode: Boolean,
    onIncognitoChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    HikariCard(
        modifier = modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_glasses_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(MR.strings.pref_incognito_mode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(MR.strings.pref_incognito_mode_extension_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.secondaryItemAlpha(),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = incognitoMode,
                onCheckedChange = onIncognitoChange,
            )
        }
    }
}

@Composable
private fun LanguagesCard(
    sources: ImmutableList<ExtensionSourceItem>,
    onClickSourcePreferences: (sourceId: Long) -> Unit,
    onClickSource: (sourceId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val configurableSource = remember(sources) {
        sources.find { it.source is ConfigurableSource }
    }

    HikariCard(
        modifier = modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Languages",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (configurableSource != null) {
                    IconButton(
                        onClick = { onClickSourcePreferences(configurableSource.source.id) },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(MR.strings.label_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select languages to use in this extension",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.secondaryItemAlpha(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sources.forEach { source ->
                    LanguageRowItem(
                        source = source,
                        onClickSource = onClickSource,
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageRowItem(
    source: ExtensionSourceItem,
    onClickSource: (sourceId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val displayName = remember(source.source) {
        if (source.labelAsName) {
            source.source.toString()
        } else {
            LocaleHelper.getSourceDisplayName(source.source.lang, context)
        }
    }

    HikariCard(
        modifier = modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        containerColor = HikariCardDefaults.containerColor(HikariCardDefaults.nestedCardElevation),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClickSource(source.source.id) }
                .padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            Switch(
                checked = source.enabled,
                onCheckedChange = null,
            )
        }
    }
}

@Composable
private fun NsfwWarningDialog(
    onClickConfirm: () -> Unit,
) {
    AdaptiveSheet(
        onDismissRequest = onClickConfirm,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.padding.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            Text(
                text = stringResource(MR.strings.ext_nsfw_warning),
                style = MaterialTheme.typography.bodyMedium,
            )

            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onClickConfirm,
            ) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        }
    }
}

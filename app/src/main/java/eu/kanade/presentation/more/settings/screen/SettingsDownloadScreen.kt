package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastMap
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.PreferenceItem
import eu.kanade.presentation.more.settings.widget.TriStateListDialog
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.download.service.ChapterFormat
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.download.service.DownloadQueueSortingMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.HikariCardDefaults
import tachiyomi.presentation.core.components.SectionCard
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsDownloadScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_downloads

    @Composable
    override fun getPreferences(): List<Preference> {
        val getCategories = remember { Injekt.get<GetCategories>() }
        val allCategories by getCategories.subscribe().collectAsState(initial = emptyList())

        val downloadPreferences = remember { Injekt.get<DownloadPreferences>() }
        val parallelSourceLimit by downloadPreferences.parallelSourceLimit.collectAsState()
        val parallelPageLimit by downloadPreferences.parallelPageLimit.collectAsState()
        return listOf(
            getGeneralGroup(downloadPreferences),
            getConcurrencyGroup(downloadPreferences, parallelSourceLimit, parallelPageLimit),
            getDeleteChaptersGroup(
                downloadPreferences = downloadPreferences,
                categories = allCategories,
            ),
            getAutoDownloadGroup(
                downloadPreferences = downloadPreferences,
                allCategories = allCategories,
            ),
            getDownloadAheadGroup(downloadPreferences = downloadPreferences),
        )
    }

    @Composable
    private fun getGeneralGroup(downloadPreferences: DownloadPreferences): Preference.PreferenceGroup {
        val downloadImageResize by downloadPreferences.downloadImageResize.collectAsState()
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_downloads),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_category_downloads),
                ) {
                    SectionCard {
                        Column {
                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = downloadPreferences.downloadOnlyOverWifi,
                                    title = stringResource(MR.strings.connected_to_wifi),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = downloadPreferences.chapterFormat,
                                    entries = persistentMapOf(
                                        ChapterFormat.IMAGES to stringResource(MR.strings.chapter_format_images),
                                        ChapterFormat.CBZ to stringResource(MR.strings.chapter_format_cbz),
                                        ChapterFormat.BBF to stringResource(MR.strings.chapter_format_bbf),
                                    ),
                                    title = stringResource(MR.strings.pref_chapter_format),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = downloadPreferences.splitTallImages,
                                    title = stringResource(MR.strings.split_tall_images),
                                    subtitle = stringResource(MR.strings.split_tall_images_summary),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = downloadPreferences.downloadImageResize,
                                    entries = persistentMapOf(
                                        0 to "None (Original)",
                                        360 to "360px width",
                                        480 to "480px width",
                                        720 to "720px width",
                                        1080 to "1080px width",
                                        1440 to "1440px width",
                                        1600 to "1600px width",
                                    ),
                                    title = "Resize Downloaded Images",
                                    subtitle = "Resize images before saving to CBZ, BBF, etc.",
                                ),
                                highlightKey = null,
                            )

                            if (downloadImageResize > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                    color = HikariCardDefaults.dividerColor(),
                                )

                                PreferenceItem(
                                    item = Preference.PreferenceItem.ListPreference(
                                        preference = downloadPreferences.downloadImageResizeFilter,
                                        entries = persistentMapOf(
                                            0 to "LANCIR (Fast High Quality)",
                                            1 to "AVIR (High Quality)",
                                        ),
                                        title = "Download Resize Algorithm",
                                        subtitle = "LANCIR is recommended for speed during background downloads.",
                                    ),
                                    highlightKey = null,
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = downloadPreferences.downloadQueueSortingMode,
                                    entries = persistentMapOf(
                                        DownloadQueueSortingMode.FIFO to stringResource(MR.strings.download_queue_sorting_fifo),
                                        DownloadQueueSortingMode.LIFO to stringResource(MR.strings.download_queue_sorting_lifo),
                                        DownloadQueueSortingMode.CHAPTER_ASC to stringResource(MR.strings.download_queue_sorting_chapter_asc),
                                        DownloadQueueSortingMode.CHAPTER_DESC to stringResource(MR.strings.download_queue_sorting_chapter_desc),
                                    ),
                                    title = stringResource(MR.strings.pref_download_queue_sorting),
                                    subtitle = stringResource(MR.strings.pref_download_queue_sorting_summary),
                                ),
                                highlightKey = null,
                            )
                        }
                    }
                },
            ),
        )
    }

    @Composable
    private fun getConcurrencyGroup(
        downloadPreferences: DownloadPreferences,
        parallelSourceLimit: Int,
        parallelPageLimit: Int,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_download_concurrent_sources),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_download_concurrent_sources),
                ) {
                    SectionCard {
                        Column {
                            PreferenceItem(
                                item = Preference.PreferenceItem.SliderPreference(
                                    value = parallelSourceLimit,
                                    valueRange = 1..50,
                                    title = stringResource(MR.strings.pref_download_concurrent_sources),
                                    onValueChanged = { downloadPreferences.parallelSourceLimit.set(it) },
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SliderPreference(
                                    value = parallelPageLimit,
                                    valueRange = 1..50,
                                    title = stringResource(MR.strings.pref_download_concurrent_pages),
                                    subtitle = stringResource(MR.strings.pref_download_concurrent_pages_summary),
                                    onValueChanged = { downloadPreferences.parallelPageLimit.set(it) },
                                ),
                                highlightKey = null,
                            )
                        }
                    }
                },
            ),
        )
    }

    @Composable
    private fun getDeleteChaptersGroup(
        downloadPreferences: DownloadPreferences,
        categories: List<Category>,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_delete_chapters),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_category_delete_chapters),
                ) {
                    SectionCard {
                        Column {
                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = downloadPreferences.removeAfterMarkedAsRead,
                                    title = stringResource(MR.strings.pref_remove_after_marked_as_read),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = downloadPreferences.removeAfterReadSlots,
                                    entries = persistentMapOf(
                                        -1 to stringResource(MR.strings.disabled),
                                        0 to stringResource(MR.strings.last_read_chapter),
                                        1 to stringResource(MR.strings.second_to_last),
                                        2 to stringResource(MR.strings.third_to_last),
                                        3 to stringResource(MR.strings.fourth_to_last),
                                        4 to stringResource(MR.strings.fifth_to_last),
                                    ),
                                    title = stringResource(MR.strings.pref_remove_after_read),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = downloadPreferences.removeBookmarkedChapters,
                                    title = stringResource(MR.strings.pref_remove_bookmarked_chapters),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = getExcludedCategoriesPreference(
                                    downloadPreferences = downloadPreferences,
                                    categories = { categories },
                                ),
                                highlightKey = null,
                            )
                        }
                    }
                },
            ),
        )
    }

    @Composable
    private fun getExcludedCategoriesPreference(
        downloadPreferences: DownloadPreferences,
        categories: () -> List<Category>,
    ): Preference.PreferenceItem.MultiSelectListPreference {
        return Preference.PreferenceItem.MultiSelectListPreference(
            preference = downloadPreferences.removeExcludeCategories,
            entries = categories()
                .associate { it.id.toString() to it.visualName }
                .toImmutableMap(),
            title = stringResource(MR.strings.pref_remove_exclude_categories),
        )
    }

    @Composable
    private fun getAutoDownloadGroup(
        downloadPreferences: DownloadPreferences,
        allCategories: List<Category>,
    ): Preference.PreferenceGroup {
        val downloadNewChaptersPref = downloadPreferences.downloadNewChapters
        val downloadNewUnreadChaptersOnlyPref = downloadPreferences.downloadNewUnreadChaptersOnly
        val downloadNewChapterCategoriesPref = downloadPreferences.downloadNewChapterCategories
        val downloadNewChapterCategoriesExcludePref = downloadPreferences.downloadNewChapterCategoriesExclude

        val downloadNewChapters by downloadNewChaptersPref.collectAsState()

        val included by downloadNewChapterCategoriesPref.collectAsState()
        val excluded by downloadNewChapterCategoriesExcludePref.collectAsState()
        var showDialog by rememberSaveable { mutableStateOf(false) }
        if (showDialog) {
            TriStateListDialog(
                title = stringResource(MR.strings.categories),
                message = stringResource(MR.strings.pref_download_new_categories_details),
                items = allCategories,
                initialChecked = included.mapNotNull { id -> allCategories.find { it.id.toString() == id } },
                initialInversed = excluded.mapNotNull { id -> allCategories.find { it.id.toString() == id } },
                itemLabel = { it.visualName },
                onDismissRequest = { showDialog = false },
                onValueChanged = { newIncluded, newExcluded ->
                    downloadNewChapterCategoriesPref.set(newIncluded.fastMap { it.id.toString() }.toSet())
                    downloadNewChapterCategoriesExcludePref.set(newExcluded.fastMap { it.id.toString() }.toSet())
                    showDialog = false
                },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_auto_download),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_category_auto_download),
                ) {
                    SectionCard {
                        Column {
                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = downloadPreferences.downloadNewChapters,
                                    title = stringResource(MR.strings.pref_download_new),
                                ),
                                highlightKey = null,
                            )

                            if (downloadNewChapters) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                    color = HikariCardDefaults.dividerColor(),
                                )
                            }

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = downloadNewUnreadChaptersOnlyPref,
                                    title = stringResource(MR.strings.pref_download_new_unread_chapters_only),
                                    enabled = downloadNewChapters,
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.TextPreference(
                                    title = stringResource(MR.strings.categories),
                                    subtitle = getCategoriesLabel(
                                        allCategories = allCategories,
                                        included = included,
                                        excluded = excluded,
                                    ),
                                    enabled = downloadNewChapters,
                                    onClick = { showDialog = true },
                                ),
                                highlightKey = null,
                            )
                        }
                    }
                },
            ),
        )
    }

    @Composable
    private fun getDownloadAheadGroup(
        downloadPreferences: DownloadPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.download_ahead),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.download_ahead),
                ) {
                    SectionCard {
                        Column {
                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = downloadPreferences.autoDownloadWhileReading,
                                    entries = listOf(0, 2, 3, 5, 10)
                                        .associateWith {
                                            if (it == 0) {
                                                stringResource(MR.strings.disabled)
                                            } else {
                                                pluralStringResource(MR.plurals.next_unread_chapters, count = it, it)
                                            }
                                        }
                                        .toImmutableMap(),
                                    title = stringResource(MR.strings.auto_download_while_reading),
                                ),
                                highlightKey = null,
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.InfoPreference(
                                    stringResource(MR.strings.download_ahead_info),
                                ),
                                highlightKey = null,
                            )
                        }
                    }
                },
            ),
        )
    }
}

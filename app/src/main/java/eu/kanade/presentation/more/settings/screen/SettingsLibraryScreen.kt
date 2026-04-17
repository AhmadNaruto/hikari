package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastMap
import androidx.core.content.ContextCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.PreferenceItem
import eu.kanade.presentation.more.settings.widget.TriStateListDialog
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.launch
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.ResetCategoryFlags
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_HAS_UNREAD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_READ
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MARK_DUPLICATE_CHAPTER_READ_EXISTING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MARK_DUPLICATE_CHAPTER_READ_NEW
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.presentation.core.components.SectionCard
import java.text.DateFormat
import java.util.Calendar

object SettingsLibraryScreen : SearchableSettings {

    @Composable
    @ReadOnlyComposable
    override fun getTitleRes() = MR.strings.pref_category_library

    @Composable
    override fun getPreferences(): List<Preference> {
        val getCategories = remember { Injekt.get<GetCategories>() }
        val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
        val allCategories by getCategories.subscribe().collectAsState(initial = emptyList())

        return listOf(
            getCategoriesGroup(LocalNavigator.currentOrThrow, allCategories, libraryPreferences),
            getGlobalUpdateGroup(allCategories, libraryPreferences),
            getBehaviorGroup(libraryPreferences),
        )
    }

    @Composable
    private fun getCategoriesGroup(
        navigator: Navigator,
        allCategories: List<Category>,
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val scope = rememberCoroutineScope()
        val userCategoriesCount = allCategories.filterNot(Category::isSystemCategory).size

        val ids = listOf(libraryPreferences.defaultCategory.defaultValue()) +
            allCategories.fastMap { it.id.toInt() }
        val labels = listOf(stringResource(MR.strings.default_category_summary)) +
            allCategories.fastMap { it.visualName }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.categories),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.categories),
                ) {
                    SectionCard {
                        Column {
                            PreferenceItem(
                                item = Preference.PreferenceItem.TextPreference(
                                    title = stringResource(MR.strings.action_edit_categories),
                                    subtitle = pluralStringResource(
                                        MR.plurals.num_categories,
                                        count = userCategoriesCount,
                                        userCategoriesCount,
                                    ),
                                    onClick = { navigator.push(CategoryScreen()) },
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider()

                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = libraryPreferences.defaultCategory,
                                    entries = ids.zip(labels).toMap().toImmutableMap(),
                                    title = stringResource(MR.strings.default_category),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider()

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = libraryPreferences.categorizedDisplaySettings,
                                    title = stringResource(MR.strings.categorized_display_settings),
                                    onValueChanged = {
                                        if (!it) {
                                            scope.launch {
                                                Injekt.get<ResetCategoryFlags>().await()
                                            }
                                        }
                                        true
                                    },
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
    private fun getGlobalUpdateGroup(
        allCategories: List<Category>,
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current

        val autoUpdateSchedulePref = libraryPreferences.autoUpdateSchedule
        val autoUpdateSchedule by autoUpdateSchedulePref.collectAsState()

        val autoUpdateCategoriesPref = libraryPreferences.updateCategories
        val autoUpdateCategoriesExcludePref = libraryPreferences.updateCategoriesExclude

        val included by autoUpdateCategoriesPref.collectAsState()
        val excluded by autoUpdateCategoriesExcludePref.collectAsState()
        var showCategoriesDialog by rememberSaveable { mutableStateOf(false) }
        if (showCategoriesDialog) {
            TriStateListDialog(
                title = stringResource(MR.strings.categories),
                message = stringResource(MR.strings.pref_library_update_categories_details),
                items = allCategories,
                initialChecked = included.mapNotNull { id -> allCategories.find { it.id.toString() == id } },
                initialInversed = excluded.mapNotNull { id -> allCategories.find { it.id.toString() == id } },
                itemLabel = { it.visualName },
                onDismissRequest = { showCategoriesDialog = false },
                onValueChanged = { newIncluded, newExcluded ->
                    autoUpdateCategoriesPref.set(newIncluded.map { it.id.toString() }.toSet())
                    autoUpdateCategoriesExcludePref.set(newExcluded.map { it.id.toString() }.toSet())
                    showCategoriesDialog = false
                },
            )
        }

        val hoursMap = remember {
            val timeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT)
            (0..23).associate { h ->
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, 0)
                }
                h.toString() to timeFormatter.format(cal.time)
            }
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_library_update),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_category_library_update),
                ) {
                    SectionCard {
                        Column {
                            PreferenceItem(
                                item = Preference.PreferenceItem.MultiSelectListPreference(
                                    preference = autoUpdateSchedulePref,
                                    entries = hoursMap.toImmutableMap(),
                                    title = stringResource(MR.strings.pref_library_update_schedule),
                                    subtitle = if (autoUpdateSchedule.isEmpty()) {
                                        stringResource(MR.strings.update_schedule_none)
                                    } else {
                                        autoUpdateSchedule
                                            .map { it.toInt() }
                                            .sorted()
                                            .joinToString(", ") { hoursMap[it.toString()]!! }
                                    },
                                    onValueChanged = {
                                        ContextCompat.getMainExecutor(context)
                                            .execute { LibraryUpdateJob.setupTask(context) }
                                        true
                                    },
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider()

                            PreferenceItem(
                                item = Preference.PreferenceItem.MultiSelectListPreference(
                                    preference = libraryPreferences.autoUpdateDeviceRestrictions,
                                    entries = persistentMapOf(
                                        DEVICE_ONLY_ON_WIFI to stringResource(MR.strings.connected_to_wifi),
                                        DEVICE_NETWORK_NOT_METERED to stringResource(MR.strings.network_not_metered),
                                        DEVICE_CHARGING to stringResource(MR.strings.charging),
                                    ),
                                    title = stringResource(MR.strings.pref_library_update_restriction),
                                    subtitle = stringResource(MR.strings.restrictions),
                                    enabled = autoUpdateSchedule.isNotEmpty(),
                                    onValueChanged = {
                                        ContextCompat.getMainExecutor(context)
                                            .execute { LibraryUpdateJob.setupTask(context) }
                                        true
                                    },
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider()

                            PreferenceItem(
                                item = Preference.PreferenceItem.TextPreference(
                                    title = stringResource(MR.strings.categories),
                                    subtitle = getCategoriesLabel(
                                        allCategories = allCategories,
                                        included = included,
                                        excluded = excluded,
                                    ),
                                    onClick = { showCategoriesDialog = true },
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider()

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = libraryPreferences.autoUpdateMetadata,
                                    title = stringResource(MR.strings.pref_library_update_refresh_metadata),
                                    subtitle = stringResource(MR.strings.pref_library_update_refresh_metadata_summary),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider()

                            PreferenceItem(
                                item = Preference.PreferenceItem.MultiSelectListPreference(
                                    preference = libraryPreferences.autoUpdateMangaRestrictions,
                                    entries = persistentMapOf(
                                        MANGA_HAS_UNREAD to stringResource(MR.strings.pref_update_only_completely_read),
                                        MANGA_NON_READ to stringResource(MR.strings.pref_update_only_started),
                                        MANGA_NON_COMPLETED to stringResource(MR.strings.pref_update_only_non_completed),
                                    ),
                                    title = stringResource(MR.strings.pref_library_update_smart_update),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider()

                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = libraryPreferences.libraryUpdateParallelSourceUpdates,
                                    entries = persistentMapOf(
                                        1 to "1",
                                        2 to "2",
                                        3 to "3",
                                        5 to "5 (${stringResource(MR.strings.label_default)})",
                                        10 to "10",
                                        15 to "15",
                                        20 to "20",
                                    ),
                                    title = stringResource(MR.strings.pref_library_update_parallel_sources),
                                    subtitle = stringResource(MR.strings.pref_library_update_parallel_sources_summary),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider()

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = libraryPreferences.newShowUpdatesCount,
                                    title = stringResource(MR.strings.pref_library_update_show_tab_badge),
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
    private fun getBehaviorGroup(
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_behavior),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_behavior),
                ) {
                    SectionCard {
                        Column {
                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = libraryPreferences.swipeToStartAction,
                                    entries = persistentMapOf(
                                        LibraryPreferences.ChapterSwipeAction.Disabled to
                                            stringResource(MR.strings.disabled),
                                        LibraryPreferences.ChapterSwipeAction.ToggleBookmark to
                                            stringResource(MR.strings.action_bookmark),
                                        LibraryPreferences.ChapterSwipeAction.ToggleRead to
                                            stringResource(MR.strings.action_mark_as_read),
                                        LibraryPreferences.ChapterSwipeAction.Download to
                                            stringResource(MR.strings.action_download),
                                    ),
                                    title = stringResource(MR.strings.pref_chapter_swipe_start),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider()

                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = libraryPreferences.swipeToEndAction,
                                    entries = persistentMapOf(
                                        LibraryPreferences.ChapterSwipeAction.Disabled to
                                            stringResource(MR.strings.disabled),
                                        LibraryPreferences.ChapterSwipeAction.ToggleBookmark to
                                            stringResource(MR.strings.action_bookmark),
                                        LibraryPreferences.ChapterSwipeAction.ToggleRead to
                                            stringResource(MR.strings.action_mark_as_read),
                                        LibraryPreferences.ChapterSwipeAction.Download to
                                            stringResource(MR.strings.action_download),
                                    ),
                                    title = stringResource(MR.strings.pref_chapter_swipe_end),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider()

                            PreferenceItem(
                                item = Preference.PreferenceItem.MultiSelectListPreference(
                                    preference = libraryPreferences.markDuplicateReadChapterAsRead,
                                    entries = persistentMapOf(
                                        MARK_DUPLICATE_CHAPTER_READ_EXISTING to
                                            stringResource(MR.strings.pref_mark_duplicate_read_chapter_read_existing),
                                        MARK_DUPLICATE_CHAPTER_READ_NEW to
                                            stringResource(MR.strings.pref_mark_duplicate_read_chapter_read_new),
                                    ),
                                    title = stringResource(MR.strings.pref_mark_duplicate_read_chapter_read),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider()

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = libraryPreferences.hideMissingChapters,
                                    title = stringResource(MR.strings.pref_hide_missing_chapter_indicators),
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

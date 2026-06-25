package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.PreferenceItem
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.util.system.hasDisplayCutout
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.HikariCardDefaults
import tachiyomi.presentation.core.components.SectionCard
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.NumberFormat

object SettingsReaderScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_reader

    @Composable
    override fun getPreferences(): List<Preference> {
        val readerPref = remember { Injekt.get<ReaderPreferences>() }

        return listOf(
            getCoreViewerGroup(readerPreferences = readerPref),
            getDisplayGroup(readerPreferences = readerPref),
            getReadingGroup(readerPreferences = readerPref),
            getPagedGroup(readerPreferences = readerPref),
            getWebtoonGroup(readerPreferences = readerPref),
            getNavigationGroup(readerPreferences = readerPref),
            getEInkGroup(readerPreferences = readerPref),
        )
    }

    @Composable
    private fun getCoreViewerGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_viewer_type),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_viewer_type),
                ) {
                    SectionCard {
                        Column {
                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = readerPreferences.defaultReadingMode,
                                    entries = ReadingMode.entries.drop(1)
                                        .associate { it.flagValue to stringResource(it.stringRes) }
                                        .toImmutableMap(),
                                    title = stringResource(MR.strings.pref_viewer_type),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = readerPreferences.doubleTapAnimSpeed,
                                    entries = persistentMapOf(
                                        1 to stringResource(MR.strings.double_tap_anim_speed_0),
                                        500 to stringResource(MR.strings.double_tap_anim_speed_normal),
                                        250 to stringResource(MR.strings.double_tap_anim_speed_fast),
                                    ),
                                    title = stringResource(MR.strings.pref_double_tap_anim_speed),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.showReadingMode,
                                    title = stringResource(MR.strings.pref_show_reading_mode),
                                    subtitle = stringResource(MR.strings.pref_show_reading_mode_summary),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.showNavigationOverlayOnStart,
                                    title = stringResource(MR.strings.pref_show_navigation_mode),
                                    subtitle = stringResource(MR.strings.pref_show_navigation_mode_summary),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.pageTransitions,
                                    title = stringResource(MR.strings.pref_page_transitions),
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
    private fun getDisplayGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val fullscreenPref = readerPreferences.fullscreen
        val fullscreen by fullscreenPref.collectAsState()
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_display),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_category_display),
                ) {
                    SectionCard {
                        Column {
                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = readerPreferences.defaultOrientationType,
                                    entries = ReaderOrientation.entries.drop(1)
                                        .associate { it.flagValue to stringResource(it.stringRes) }
                                        .toImmutableMap(),
                                    title = stringResource(MR.strings.pref_rotation_type),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = readerPreferences.readerTheme,
                                    entries = persistentMapOf(
                                        1 to stringResource(MR.strings.black_background),
                                        2 to stringResource(MR.strings.gray_background),
                                        0 to stringResource(MR.strings.white_background),
                                        3 to stringResource(MR.strings.automatic_background),
                                    ),
                                    title = stringResource(MR.strings.pref_reader_theme),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = fullscreenPref,
                                    title = stringResource(MR.strings.pref_fullscreen),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.drawUnderCutout,
                                    title = stringResource(MR.strings.pref_cutout_short),
                                    enabled = LocalView.current.hasDisplayCutout() && fullscreen,
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.keepScreenOn,
                                    title = stringResource(MR.strings.pref_keep_screen_on),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.showPageNumber,
                                    title = stringResource(MR.strings.pref_show_page_number),
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
    private fun getEInkGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val flashPageState by readerPreferences.flashOnPageChange.collectAsState()

        val flashMillisPref = readerPreferences.flashDurationMillis
        val flashMillis by flashMillisPref.collectAsState()

        val flashIntervalPref = readerPreferences.flashPageInterval
        val flashInterval by flashIntervalPref.collectAsState()

        val flashColorPref = readerPreferences.flashColor

        return Preference.PreferenceGroup(
            title = "E-Ink",
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = "E-Ink",
                ) {
                    SectionCard {
                        Column {
                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.flashOnPageChange,
                                    title = stringResource(MR.strings.pref_flash_page),
                                    subtitle = stringResource(MR.strings.pref_flash_page_summ),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SliderPreference(
                                    value = flashMillis / ReaderPreferences.MILLI_CONVERSION,
                                    valueRange = 1..15,
                                    title = stringResource(MR.strings.pref_flash_duration),
                                    valueString = stringResource(MR.strings.pref_flash_duration_summary, flashMillis),
                                    enabled = flashPageState,
                                    onValueChanged = { flashMillisPref.set(it * ReaderPreferences.MILLI_CONVERSION) },
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SliderPreference(
                                    value = flashInterval,
                                    valueRange = 1..10,
                                    title = stringResource(MR.strings.pref_flash_page_interval),
                                    valueString = pluralStringResource(
                                        MR.plurals.pref_pages,
                                        flashInterval,
                                        flashInterval,
                                    ),
                                    enabled = flashPageState,
                                    onValueChanged = { flashIntervalPref.set(it) },
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = flashColorPref,
                                    entries = persistentMapOf(
                                        ReaderPreferences.FlashColor.BLACK to
                                            stringResource(MR.strings.pref_flash_style_black),
                                        ReaderPreferences.FlashColor.WHITE to
                                            stringResource(MR.strings.pref_flash_style_white),
                                        ReaderPreferences.FlashColor.WHITE_BLACK
                                            to stringResource(MR.strings.pref_flash_style_white_black),
                                    ),
                                    title = stringResource(MR.strings.pref_flash_with),
                                    enabled = flashPageState,
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
    private fun getReadingGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_reading),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_category_reading),
                ) {
                    SectionCard {
                        Column {
                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.skipRead,
                                    title = stringResource(MR.strings.pref_skip_read_chapters),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.skipFiltered,
                                    title = stringResource(MR.strings.pref_skip_filtered_chapters),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.skipDupe,
                                    title = stringResource(MR.strings.pref_skip_dupe_chapters),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.alwaysShowChapterTransition,
                                    title = stringResource(MR.strings.pref_always_show_chapter_transition),
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
    private fun getPagedGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val navModePref = readerPreferences.navigationModePager
        val imageScaleTypePref = readerPreferences.imageScaleType
        val dualPageSplitPref = readerPreferences.dualPageSplitPaged
        val rotateToFitPref = readerPreferences.dualPageRotateToFit

        val navMode by navModePref.collectAsState()
        val imageScaleType by imageScaleTypePref.collectAsState()
        val dualPageSplit by dualPageSplitPref.collectAsState()
        val rotateToFit by rotateToFitPref.collectAsState()
        val readerUpscaling by readerPreferences.readerUpscaling.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pager_viewer),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pager_viewer),
                ) {
                    SectionCard {
                        Column {
                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = navModePref,
                                    entries = ReaderPreferences.TapZones
                                        .mapIndexed { index, it -> index to stringResource(it) }
                                        .toMap()
                                        .toImmutableMap(),
                                    title = stringResource(MR.strings.pref_viewer_nav),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = readerPreferences.pagerNavInverted,
                                    entries = persistentListOf(
                                        ReaderPreferences.TappingInvertMode.NONE,
                                        ReaderPreferences.TappingInvertMode.HORIZONTAL,
                                        ReaderPreferences.TappingInvertMode.VERTICAL,
                                        ReaderPreferences.TappingInvertMode.BOTH,
                                    )
                                        .associateWith { stringResource(it.titleRes) }
                                        .toImmutableMap(),
                                    title = stringResource(MR.strings.pref_read_with_tapping_inverted),
                                    enabled = navMode != 5,
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = imageScaleTypePref,
                                    entries = ReaderPreferences.ImageScaleType
                                        .mapIndexed { index, it -> index + 1 to stringResource(it) }
                                        .toMap()
                                        .toImmutableMap(),
                                    title = stringResource(MR.strings.pref_image_scale_type),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = readerPreferences.zoomStart,
                                    entries = ReaderPreferences.ZoomStart
                                        .mapIndexed { index, it -> index + 1 to stringResource(it) }
                                        .toMap()
                                        .toImmutableMap(),
                                    title = stringResource(MR.strings.pref_zoom_start),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.cropBorders,
                                    title = stringResource(MR.strings.pref_crop_borders),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.readerUpscaling,
                                    title = stringResource(MR.strings.pref_reader_upscaling),
                                ),
                                highlightKey = null,
                            )

                            if (readerUpscaling) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                    color = HikariCardDefaults.dividerColor(),
                                )

                                PreferenceItem(
                                    item = Preference.PreferenceItem.ListPreference(
                                        preference = readerPreferences.readerUpscalerType,
                                        entries = mapOf(
                                            0 to "AMD FSR EASU",
                                            1 to "AVIR (High Quality)",
                                            2 to "LANCIR (Fast High Quality)"
                                        ).toImmutableMap(),
                                        title = "Upscaler Type",
                                    ),
                                    highlightKey = null,
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.landscapeZoom,
                                    title = stringResource(MR.strings.pref_landscape_zoom),
                                    enabled = imageScaleType == 1,
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.navigateToPan,
                                    title = stringResource(MR.strings.pref_navigate_pan),
                                    enabled = navMode != 5,
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = dualPageSplitPref,
                                    title = stringResource(MR.strings.pref_dual_page_split),
                                    onValueChanged = {
                                        rotateToFitPref.set(false)
                                        true
                                    },
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.dualPageInvertPaged,
                                    title = stringResource(MR.strings.pref_dual_page_invert),
                                    subtitle = stringResource(MR.strings.pref_dual_page_invert_summary),
                                    enabled = dualPageSplit,
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = rotateToFitPref,
                                    title = stringResource(MR.strings.pref_page_rotate),
                                    onValueChanged = {
                                        dualPageSplitPref.set(false)
                                        true
                                    },
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.dualPageRotateToFitInvert,
                                    title = stringResource(MR.strings.pref_page_rotate_invert),
                                    enabled = rotateToFit,
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
    private fun getWebtoonGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val numberFormat = remember { NumberFormat.getPercentInstance() }

        val navModePref = readerPreferences.navigationModeWebtoon
        val dualPageSplitPref = readerPreferences.dualPageSplitWebtoon
        val rotateToFitPref = readerPreferences.dualPageRotateToFitWebtoon
        val webtoonSidePaddingPref = readerPreferences.webtoonSidePadding

        val navMode by navModePref.collectAsState()
        val dualPageSplit by dualPageSplitPref.collectAsState()
        val rotateToFit by rotateToFitPref.collectAsState()
        val webtoonSidePadding by webtoonSidePaddingPref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.webtoon_viewer),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.webtoon_viewer),
                ) {
                    SectionCard {
                        Column {
                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = navModePref,
                                    entries = ReaderPreferences.TapZones
                                        .mapIndexed { index, it -> index to stringResource(it) }
                                        .toMap()
                                        .toImmutableMap(),
                                    title = stringResource(MR.strings.pref_viewer_nav),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = readerPreferences.webtoonNavInverted,
                                    entries = persistentListOf(
                                        ReaderPreferences.TappingInvertMode.NONE,
                                        ReaderPreferences.TappingInvertMode.HORIZONTAL,
                                        ReaderPreferences.TappingInvertMode.VERTICAL,
                                        ReaderPreferences.TappingInvertMode.BOTH,
                                    )
                                        .associateWith { stringResource(it.titleRes) }
                                        .toImmutableMap(),
                                    title = stringResource(MR.strings.pref_read_with_tapping_inverted),
                                    enabled = navMode != 5,
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SliderPreference(
                                    value = webtoonSidePadding,
                                    valueRange = ReaderPreferences.let {
                                        it.WEBTOON_PADDING_MIN..it.WEBTOON_PADDING_MAX
                                    },
                                    title = stringResource(MR.strings.pref_webtoon_side_padding),
                                    valueString = numberFormat.format(webtoonSidePadding / 100f),
                                    onValueChanged = { webtoonSidePaddingPref.set(it) },
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = readerPreferences.readerHideThreshold,
                                    entries = persistentMapOf(
                                        ReaderPreferences.ReaderHideThreshold.HIGHEST to
                                            stringResource(MR.strings.pref_highest),
                                        ReaderPreferences.ReaderHideThreshold.HIGH to
                                            stringResource(MR.strings.pref_high),
                                        ReaderPreferences.ReaderHideThreshold.LOW to stringResource(
                                            MR.strings.pref_low,
                                        ),
                                        ReaderPreferences.ReaderHideThreshold.LOWEST to
                                            stringResource(MR.strings.pref_lowest),
                                    ),
                                    title = stringResource(MR.strings.pref_hide_threshold),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.cropBordersWebtoon,
                                    title = stringResource(MR.strings.pref_crop_borders),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = dualPageSplitPref,
                                    title = stringResource(MR.strings.pref_dual_page_split),
                                    onValueChanged = {
                                        rotateToFitPref.set(false)
                                        true
                                    },
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.dualPageInvertWebtoon,
                                    title = stringResource(MR.strings.pref_dual_page_invert),
                                    subtitle = stringResource(MR.strings.pref_dual_page_invert_summary),
                                    enabled = dualPageSplit,
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = rotateToFitPref,
                                    title = stringResource(MR.strings.pref_page_rotate),
                                    onValueChanged = {
                                        dualPageSplitPref.set(false)
                                        true
                                    },
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.dualPageRotateToFitInvertWebtoon,
                                    title = stringResource(MR.strings.pref_page_rotate_invert),
                                    enabled = rotateToFit,
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.webtoonDoubleTapZoomEnabled,
                                    title = stringResource(MR.strings.pref_double_tap_zoom),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.webtoonDisableZoomOut,
                                    title = stringResource(MR.strings.pref_webtoon_disable_zoom_out),
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
    private fun getNavigationGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val readWithVolumeKeysPref = readerPreferences.readWithVolumeKeys
        val readWithVolumeKeys by readWithVolumeKeysPref.collectAsState()
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_reader_navigation),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_reader_navigation),
                ) {
                    SectionCard {
                        Column {
                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readWithVolumeKeysPref,
                                    title = stringResource(MR.strings.pref_read_with_volume_keys),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.readWithVolumeKeysInverted,
                                    title = stringResource(MR.strings.pref_read_with_volume_keys_inverted),
                                    enabled = readWithVolumeKeys,
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.readWithLongTap,
                                    title = stringResource(MR.strings.pref_read_with_long_tap),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                color = HikariCardDefaults.dividerColor(),
                            )

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = readerPreferences.folderPerManga,
                                    title = stringResource(MR.strings.pref_create_folder_per_manga),
                                    subtitle = stringResource(MR.strings.pref_create_folder_per_manga_summary),
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

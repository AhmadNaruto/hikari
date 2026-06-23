package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.HikariCardDefaults
import tachiyomi.presentation.core.components.HikariCardGroup
import tachiyomi.presentation.core.components.HikariSectionHeader
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import java.text.NumberFormat

@Composable
internal fun ColumnScope.ReadingModePage(screenModel: ReaderSettingsScreenModel) {
    HeadingItem(MR.strings.pref_category_for_this_series)
    val manga by screenModel.mangaFlow.collectAsState()

    val readingMode = remember(manga) { ReadingMode.fromPreference(manga?.readingMode?.toInt()) }
    SettingsChipRow(MR.strings.pref_category_reading_mode) {
        ReadingMode.entries.forEach {
            FilterChip(
                selected = it == readingMode,
                onClick = { screenModel.onChangeReadingMode(it) },
                label = { Text(stringResource(it.stringRes)) },
            )
        }
    }

    val orientation = remember(manga) { ReaderOrientation.fromPreference(manga?.readerOrientation?.toInt()) }
    SettingsChipRow(MR.strings.rotation_type) {
        ReaderOrientation.entries.forEach {
            FilterChip(
                selected = it == orientation,
                onClick = { screenModel.onChangeOrientation(it) },
                label = { Text(stringResource(it.stringRes)) },
            )
        }
    }

    val viewer by screenModel.viewerFlow.collectAsState()
    if (viewer is WebtoonViewer) {
        WebtoonViewerSettings(screenModel)
    } else {
        PagerViewerSettings(screenModel)
    }
}

@Composable
private fun ColumnScope.PagerViewerSettings(screenModel: ReaderSettingsScreenModel) {
    HeadingItem(MR.strings.pager_viewer)

    val navigationModePager by screenModel.preferences.navigationModePager.collectAsState()
    val pagerNavInverted by screenModel.preferences.pagerNavInverted.collectAsState()
    TapZonesItems(
        selected = navigationModePager,
        onSelect = screenModel.preferences.navigationModePager::set,
        invertMode = pagerNavInverted,
        onSelectInvertMode = screenModel.preferences.pagerNavInverted::set,
    )

    val imageScaleType by screenModel.preferences.imageScaleType.collectAsState()
    SettingsChipRow(MR.strings.pref_image_scale_type) {
        ReaderPreferences.ImageScaleType.mapIndexed { index, it ->
            FilterChip(
                selected = imageScaleType == index + 1,
                onClick = { screenModel.preferences.imageScaleType.set(index + 1) },
                label = { Text(stringResource(it)) },
            )
        }
    }

    val zoomStart by screenModel.preferences.zoomStart.collectAsState()
    SettingsChipRow(MR.strings.pref_zoom_start) {
        ReaderPreferences.ZoomStart.mapIndexed { index, it ->
            FilterChip(
                selected = zoomStart == index + 1,
                onClick = { screenModel.preferences.zoomStart.set(index + 1) },
                label = { Text(stringResource(it)) },
            )
        }
    }

    HikariSectionHeader(text = "Viewer Options")
    HikariCardGroup {
        CheckboxItem(
            label = stringResource(MR.strings.pref_crop_borders),
            pref = screenModel.preferences.cropBorders,
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
            thickness = 0.5.dp,
            color = HikariCardDefaults.dividerColor(),
        )
        CheckboxItem(
            label = stringResource(MR.strings.pref_landscape_zoom),
            pref = screenModel.preferences.landscapeZoom,
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
            thickness = 0.5.dp,
            color = HikariCardDefaults.dividerColor(),
        )
        CheckboxItem(
            label = stringResource(MR.strings.pref_navigate_pan),
            pref = screenModel.preferences.navigateToPan,
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
            thickness = 0.5.dp,
            color = HikariCardDefaults.dividerColor(),
        )
        CheckboxItem(
            label = stringResource(MR.strings.pref_dual_page_split),
            pref = screenModel.preferences.dualPageSplitPaged,
        )
        val dualPageSplitPaged by screenModel.preferences.dualPageSplitPaged.collectAsState()
        if (dualPageSplitPaged) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
                thickness = 0.5.dp,
                color = HikariCardDefaults.dividerColor(),
            )
            CheckboxItem(
                label = stringResource(MR.strings.pref_dual_page_invert),
                pref = screenModel.preferences.dualPageInvertPaged,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
            thickness = 0.5.dp,
            color = HikariCardDefaults.dividerColor(),
        )
        CheckboxItem(
            label = stringResource(MR.strings.pref_page_rotate),
            pref = screenModel.preferences.dualPageRotateToFit,
        )
        val dualPageRotateToFit by screenModel.preferences.dualPageRotateToFit.collectAsState()
        if (dualPageRotateToFit) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
                thickness = 0.5.dp,
                color = HikariCardDefaults.dividerColor(),
            )
            CheckboxItem(
                label = stringResource(MR.strings.pref_page_rotate_invert),
                pref = screenModel.preferences.dualPageRotateToFitInvert,
            )
        }
    }
}

@Composable
private fun ColumnScope.WebtoonViewerSettings(screenModel: ReaderSettingsScreenModel) {
    val numberFormat = remember { NumberFormat.getPercentInstance() }

    HeadingItem(MR.strings.webtoon_viewer)

    val navigationModeWebtoon by screenModel.preferences.navigationModeWebtoon.collectAsState()
    val webtoonNavInverted by screenModel.preferences.webtoonNavInverted.collectAsState()
    TapZonesItems(
        selected = navigationModeWebtoon,
        onSelect = screenModel.preferences.navigationModeWebtoon::set,
        invertMode = webtoonNavInverted,
        onSelectInvertMode = screenModel.preferences.webtoonNavInverted::set,
    )

    HikariSectionHeader(text = "Viewer Options")
    HikariCardGroup {
        val webtoonSidePadding by screenModel.preferences.webtoonSidePadding.collectAsState()
        SliderItem(
            value = webtoonSidePadding,
            valueRange = ReaderPreferences.let { it.WEBTOON_PADDING_MIN..it.WEBTOON_PADDING_MAX },
            label = stringResource(MR.strings.pref_webtoon_side_padding),
            valueString = numberFormat.format(webtoonSidePadding / 100f),
            onChange = {
                screenModel.preferences.webtoonSidePadding.set(it)
            },
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
            thickness = 0.5.dp,
            color = HikariCardDefaults.dividerColor(),
        )
        CheckboxItem(
            label = stringResource(MR.strings.pref_crop_borders),
            pref = screenModel.preferences.cropBordersWebtoon,
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
            thickness = 0.5.dp,
            color = HikariCardDefaults.dividerColor(),
        )
        CheckboxItem(
            label = stringResource(MR.strings.pref_dual_page_split),
            pref = screenModel.preferences.dualPageSplitWebtoon,
        )
        val dualPageSplitWebtoon by screenModel.preferences.dualPageSplitWebtoon.collectAsState()
        if (dualPageSplitWebtoon) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
                thickness = 0.5.dp,
                color = HikariCardDefaults.dividerColor(),
            )
            CheckboxItem(
                label = stringResource(MR.strings.pref_dual_page_invert),
                pref = screenModel.preferences.dualPageInvertWebtoon,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
            thickness = 0.5.dp,
            color = HikariCardDefaults.dividerColor(),
        )
        CheckboxItem(
            label = stringResource(MR.strings.pref_page_rotate),
            pref = screenModel.preferences.dualPageRotateToFitWebtoon,
        )
        val dualPageRotateToFitWebtoon by screenModel.preferences.dualPageRotateToFitWebtoon.collectAsState()
        if (dualPageRotateToFitWebtoon) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
                thickness = 0.5.dp,
                color = HikariCardDefaults.dividerColor(),
            )
            CheckboxItem(
                label = stringResource(MR.strings.pref_page_rotate_invert),
                pref = screenModel.preferences.dualPageRotateToFitInvertWebtoon,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
            thickness = 0.5.dp,
            color = HikariCardDefaults.dividerColor(),
        )
        CheckboxItem(
            label = stringResource(MR.strings.pref_double_tap_zoom),
            pref = screenModel.preferences.webtoonDoubleTapZoomEnabled,
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
            thickness = 0.5.dp,
            color = HikariCardDefaults.dividerColor(),
        )
        CheckboxItem(
            label = stringResource(MR.strings.pref_webtoon_disable_zoom_out),
            pref = screenModel.preferences.webtoonDisableZoomOut,
        )
    }
}

@Composable
private fun ColumnScope.TapZonesItems(
    selected: Int,
    onSelect: (Int) -> Unit,
    invertMode: ReaderPreferences.TappingInvertMode,
    onSelectInvertMode: (ReaderPreferences.TappingInvertMode) -> Unit,
) {
    SettingsChipRow(MR.strings.pref_viewer_nav) {
        ReaderPreferences.TapZones.mapIndexed { index, it ->
            FilterChip(
                selected = selected == index,
                onClick = { onSelect(index) },
                label = { Text(stringResource(it)) },
            )
        }
    }

    if (selected != 5) {
        SettingsChipRow(MR.strings.pref_read_with_tapping_inverted) {
            ReaderPreferences.TappingInvertMode.entries.forEach {
                FilterChip(
                    selected = it == invertMode,
                    onClick = { onSelectInvertMode(it) },
                    label = { Text(stringResource(it.titleRes)) },
                )
            }
        }
    }
}

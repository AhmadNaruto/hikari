package eu.kanade.presentation.reader.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import eu.kanade.tachiyomi.util.system.hasDisplayCutout
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HikariCardDefaults
import tachiyomi.presentation.core.components.HikariCardGroup
import tachiyomi.presentation.core.components.HikariSectionHeader
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

private val themes = listOf(
    MR.strings.black_background to 1,
    MR.strings.gray_background to 2,
    MR.strings.white_background to 0,
    MR.strings.automatic_background to 3,
)

private val flashColors = listOf(
    MR.strings.pref_flash_style_black to ReaderPreferences.FlashColor.BLACK,
    MR.strings.pref_flash_style_white to ReaderPreferences.FlashColor.WHITE,
    MR.strings.pref_flash_style_white_black to ReaderPreferences.FlashColor.WHITE_BLACK,
)

@Composable
internal fun ColumnScope.GeneralPage(screenModel: ReaderSettingsScreenModel) {
    val readerTheme by screenModel.preferences.readerTheme.collectAsState()

    val flashPageState by screenModel.preferences.flashOnPageChange.collectAsState()

    val flashMillisPref = screenModel.preferences.flashDurationMillis
    val flashMillis by flashMillisPref.collectAsState()

    val flashIntervalPref = screenModel.preferences.flashPageInterval
    val flashInterval by flashIntervalPref.collectAsState()

    val flashColorPref = screenModel.preferences.flashColor
    val flashColor by flashColorPref.collectAsState()

    val readerSharpening by screenModel.preferences.readerSharpening.collectAsState()
    val readerSharpeningStrengthPref = screenModel.preferences.readerSharpeningStrength
    val readerSharpeningStrength by readerSharpeningStrengthPref.collectAsState()

    val readerDenoising by screenModel.preferences.readerDenoising.collectAsState()
    val readerDenoisingStrengthPref = screenModel.preferences.readerDenoisingStrength
    val readerDenoisingStrength by readerDenoisingStrengthPref.collectAsState()

    SettingsChipRow(MR.strings.pref_reader_theme) {
        themes.map { (labelRes, value) ->
            FilterChip(
                selected = readerTheme == value,
                onClick = { screenModel.preferences.readerTheme.set(value) },
                label = { Text(stringResource(labelRes)) },
            )
        }
    }

    HikariSectionHeader(text = "Display")
    HikariCardGroup {
        CheckboxItem(
            label = stringResource(MR.strings.pref_show_page_number),
            pref = screenModel.preferences.showPageNumber,
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
            thickness = 0.5.dp,
            color = HikariCardDefaults.dividerColor(),
        )
        CheckboxItem(
            label = stringResource(MR.strings.pref_fullscreen),
            pref = screenModel.preferences.fullscreen,
        )
        val isFullscreen by screenModel.preferences.fullscreen.collectAsState()
        if (LocalActivity.current?.hasDisplayCutout() == true && isFullscreen) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
                thickness = 0.5.dp,
                color = HikariCardDefaults.dividerColor(),
            )
            CheckboxItem(
                label = stringResource(MR.strings.pref_cutout_short),
                pref = screenModel.preferences.drawUnderCutout,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
            thickness = 0.5.dp,
            color = HikariCardDefaults.dividerColor(),
        )
        CheckboxItem(
            label = stringResource(MR.strings.pref_keep_screen_on),
            pref = screenModel.preferences.keepScreenOn,
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
            thickness = 0.5.dp,
            color = HikariCardDefaults.dividerColor(),
        )
        CheckboxItem(
            label = stringResource(MR.strings.pref_read_with_long_tap),
            pref = screenModel.preferences.readWithLongTap,
        )
    }

    HikariSectionHeader(text = "Transitions")
    HikariCardGroup {
        CheckboxItem(
            label = stringResource(MR.strings.pref_page_transitions),
            pref = screenModel.preferences.pageTransitions,
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
            thickness = 0.5.dp,
            color = HikariCardDefaults.dividerColor(),
        )
        CheckboxItem(
            label = stringResource(MR.strings.pref_always_show_chapter_transition),
            pref = screenModel.preferences.alwaysShowChapterTransition,
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
            thickness = 0.5.dp,
            color = HikariCardDefaults.dividerColor(),
        )
        CheckboxItem(
            label = stringResource(MR.strings.pref_flash_page),
            pref = screenModel.preferences.flashOnPageChange,
        )
    }

    if (flashPageState) {
        HikariSectionHeader(text = "Flash settings")
        HikariCardGroup {
            SliderItem(
                value = flashMillis / ReaderPreferences.MILLI_CONVERSION,
                valueRange = 1..15,
                label = stringResource(MR.strings.pref_flash_duration),
                valueString = stringResource(MR.strings.pref_flash_duration_summary, flashMillis),
                onChange = { flashMillisPref.set(it * ReaderPreferences.MILLI_CONVERSION) },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
                thickness = 0.5.dp,
                color = HikariCardDefaults.dividerColor(),
            )
            SliderItem(
                value = flashInterval,
                valueRange = 1..10,
                label = stringResource(MR.strings.pref_flash_page_interval),
                valueString = pluralStringResource(MR.plurals.pref_pages, flashInterval, flashInterval),
                onChange = {
                    flashIntervalPref.set(it)
                },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
        SettingsChipRow(MR.strings.pref_flash_with) {
            flashColors.map { (labelRes, value) ->
                FilterChip(
                    selected = flashColor == value,
                    onClick = { flashColorPref.set(value) },
                    label = { Text(stringResource(labelRes)) },
                )
            }
        }
    }

    HikariSectionHeader(text = "Image Processing & Preloading")
    HikariCardGroup {
        CheckboxItem(
            label = stringResource(MR.strings.pref_reader_page_cache),
            pref = screenModel.preferences.readerPageCache,
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
            thickness = 0.5.dp,
            color = HikariCardDefaults.dividerColor(),
        )
        CheckboxItem(
            label = stringResource(MR.strings.pref_reader_upscaling),
            pref = screenModel.preferences.readerUpscaling,
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
            thickness = 0.5.dp,
            color = HikariCardDefaults.dividerColor(),
        )
        CheckboxItem(
            label = stringResource(MR.strings.pref_reader_sharpening),
            pref = screenModel.preferences.readerSharpening,
        )
        if (readerSharpening) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
                thickness = 0.5.dp,
                color = HikariCardDefaults.dividerColor(),
            )
            SliderItem(
                value = readerSharpeningStrength,
                valueRange = 0..20,
                label = stringResource(MR.strings.pref_reader_sharpening_strength),
                valueString = (readerSharpeningStrength / 10.0f).toString(),
                onChange = { readerSharpeningStrengthPref.set(it) },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
            thickness = 0.5.dp,
            color = HikariCardDefaults.dividerColor(),
        )
        CheckboxItem(
            label = stringResource(MR.strings.pref_reader_denoising),
            pref = screenModel.preferences.readerDenoising,
        )
        if (readerDenoising) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
                thickness = 0.5.dp,
                color = HikariCardDefaults.dividerColor(),
            )
            SliderItem(
                value = readerDenoisingStrength,
                valueRange = 0..10,
                label = stringResource(MR.strings.pref_reader_denoising_strength),
                valueString = "${readerDenoisingStrength * 10}%",
                onChange = { readerDenoisingStrengthPref.set(it) },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}

package eu.kanade.presentation.more.settings.screen

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import androidx.compose.material3.HorizontalDivider
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.PreferenceItem
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageScreen
import eu.kanade.presentation.more.settings.widget.AppThemeModePreferenceWidget
import eu.kanade.presentation.more.settings.widget.AppThemePreferenceWidget
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.LocalDate
import tachiyomi.presentation.core.components.SectionCard

object SettingsAppearanceScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_appearance

    @Composable
    override fun getPreferences(): List<Preference> {
        val uiPreferences = remember { Injekt.get<UiPreferences>() }

        return listOf(
            getThemeGroup(uiPreferences = uiPreferences),
            getInterfaceGroup(uiPreferences = uiPreferences),
            getDisplayGroup(uiPreferences = uiPreferences),
        )
    }

    @Composable
    private fun getThemeGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current

        val themeModePref = uiPreferences.themeMode
        val themeMode by themeModePref.collectAsState()

        val appThemePref = uiPreferences.appTheme
        val appTheme by appThemePref.collectAsState()

        val amoledPref = uiPreferences.themeDarkAmoled
        val amoled by amoledPref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_theme),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_app_theme),
                ) {
                    SectionCard {
                        Column {
                            AppThemeModePreferenceWidget(
                                value = themeMode,
                                onItemClick = {
                                    themeModePref.set(it)
                                    setAppCompatDelegateThemeMode(it)
                                },
                            )

                            AppThemePreferenceWidget(
                                value = appTheme,
                                amoled = amoled,
                                onItemClick = { appThemePref.set(it) },
                            )
                        }
                    }
                },
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_dark_theme_pure_black),
                ) {
                    SectionCard {
                        PreferenceItem(
                            item = Preference.PreferenceItem.SwitchPreference(
                                preference = amoledPref,
                                title = stringResource(MR.strings.pref_dark_theme_pure_black),
                                enabled = themeMode != ThemeMode.LIGHT,
                                onValueChanged = {
                                    (context as? Activity)?.let { ActivityCompat.recreate(it) }
                                    true
                                },
                            ),
                            highlightKey = null,
                        )
                    }
                },
            ),
        )
    }

    @Composable
    private fun getInterfaceGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val navigator = LocalNavigator.currentOrThrow

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_display),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_category_display),
                ) {
                    SectionCard {
                        Column {
                            PreferenceItem(
                                item = Preference.PreferenceItem.TextPreference(
                                    title = stringResource(MR.strings.pref_app_language),
                                    onClick = { navigator.push(AppLanguageScreen()) },
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider()

                            PreferenceItem(
                                item = Preference.PreferenceItem.ListPreference(
                                    preference = uiPreferences.tabletUiMode,
                                    entries = TabletUiMode.entries
                                        .associateWith { stringResource(it.titleRes) }
                                        .toImmutableMap(),
                                    title = stringResource(MR.strings.pref_tablet_ui_mode),
                                    onValueChanged = {
                                        Injekt.get<Activity>().toast(MR.strings.requires_app_restart)
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
    private fun getDisplayGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val now = remember { LocalDate.now() }

        val dateFormat by uiPreferences.dateFormat.collectAsState()
        val formattedNow = remember(dateFormat) {
            UiPreferences.dateFormat(dateFormat).format(now)
        }

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
                                    preference = uiPreferences.dateFormat,
                                    entries = DateFormats
                                        .associateWith {
                                            val formattedDate = UiPreferences.dateFormat(it).format(now)
                                            "${it.ifEmpty { stringResource(MR.strings.label_default) }} ($formattedDate)"
                                        }
                                        .toImmutableMap(),
                                    title = stringResource(MR.strings.pref_date_format),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider()

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = uiPreferences.relativeTime,
                                    title = stringResource(MR.strings.pref_relative_format),
                                    subtitle = stringResource(
                                        MR.strings.pref_relative_format_summary,
                                        stringResource(MR.strings.relative_time_today),
                                        formattedNow,
                                    ),
                                ),
                                highlightKey = null,
                            )

                            HorizontalDivider()

                            PreferenceItem(
                                item = Preference.PreferenceItem.SwitchPreference(
                                    preference = uiPreferences.imagesInDescription,
                                    title = stringResource(MR.strings.pref_display_images_description),
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

private val DateFormats = listOf(
    "",
    "MM/dd/yy",
    "dd/MM/yy",
    "yyyy-MM-dd",
    "dd MMM yyyy",
    "MMM dd, yyyy",
)

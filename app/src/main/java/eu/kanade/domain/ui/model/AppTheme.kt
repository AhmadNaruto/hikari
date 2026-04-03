package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

enum class AppTheme(val titleRes: StringResource?) {
    DEFAULT(MR.strings.label_default),
    MONET(MR.strings.theme_monet),
}

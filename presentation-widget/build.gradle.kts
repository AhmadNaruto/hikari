plugins {
    alias(hikarix.plugins.android.library)
    alias(hikarix.plugins.compose)

    alias(hikarix.plugins.spotless)
}

android {
    namespace = "tachiyomi.presentation.widget"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.domain)
    implementation(projects.presentationCore)
    api(projects.i18n)

    implementation(libs.androidx.glance.appWidget)
    implementation(libs.material)

    implementation(libs.kotlinx.collections.immutable)

    implementation(libs.coil.core)

    api(libs.injekt)
}

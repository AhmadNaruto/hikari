plugins {
    alias(hikarix.plugins.android.library)
    alias(hikarix.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "hikari.core.archive"
}

dependencies {
    implementation(libs.jsoup)
    implementation(libs.archive)
    implementation(libs.unifile)
}

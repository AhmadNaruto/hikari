import hikari.gradle.Config

plugins {
    alias(hikarix.plugins.android.library)
    alias(hikarix.plugins.spotless)
}

android {
    namespace = "hikari.telemetry"

    sourceSets {
        getByName("main") {
            if (Config.includeTelemetry) {
                kotlin.srcDirs("src/firebase/kotlin")
            } else {
                kotlin.srcDirs("src/noop/kotlin")
                manifest.srcFile("src/noop/AndroidManifext.xml")
            }
        }
    }
}

dependencies {
    implementation(projects.core.common)

    if (Config.includeTelemetry) {
        implementation(platform(libs.firebase.bom))
        implementation(libs.firebase.analytics)
        implementation(libs.firebase.crashlytics)
    }
}

package hikari.gradle.configurations

import com.android.build.api.dsl.ApplicationDefaultConfig
import hikari.gradle.extensions.android
import hikari.gradle.extensions.coreLibraryDesugaring
import hikari.gradle.extensions.libs
import hikari.gradle.extensions.hikarix
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

fun Project.configureAndroid() {
    android {
        defaultConfig {
            minSdk = hikarix.versions.android.sdk.min.get().toInt()
            if (this is ApplicationDefaultConfig) {
                targetSdk = hikarix.versions.android.sdk.target.get().toInt()
            }

            ndkVersion = hikarix.versions.android.ndk.get()
        }

        compileSdk = hikarix.versions.android.sdk.compile.get().toInt()

        compileOptions {
            isCoreLibraryDesugaringEnabled = true
        }
    }

    dependencies {
        coreLibraryDesugaring(libs.android.desugar)
    }
}

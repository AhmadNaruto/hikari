import hikari.gradle.configurations.configureAndroid
import hikari.gradle.configurations.configureKotlin
import hikari.gradle.extensions.alias
import hikari.gradle.extensions.configureTest
import hikari.gradle.extensions.libs
import hikari.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("UNUSED")
class PluginKotlinMultiplatform : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.android.library)
            alias(libs.plugins.kotlin.multiplatform)
        }

        configureKotlin()
        configureTest()
        configureAndroid()

        kotlin {
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            applyDefaultHierarchyTemplate()

            androidTarget()
        }
    }
}

private fun Project.kotlin(block: KotlinMultiplatformExtension.() -> Unit) {
    extensions.configure(block)
}

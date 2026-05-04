import hikari.gradle.configurations.configureAndroid
import hikari.gradle.configurations.configureKotlin
import hikari.gradle.extensions.alias
import hikari.gradle.extensions.configureTest
import hikari.gradle.extensions.libs
import hikari.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("UNUSED")
class PluginAndroidBase : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.kotlin.android)
        }

        configureKotlin()
        configureTest()
        configureAndroid()
    }
}

import hikari.gradle.extensions.alias
import hikari.gradle.extensions.libs
import hikari.gradle.extensions.hikarix
import hikari.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("UNUSED")
class PluginAndroidTest : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.android.test)
            alias(hikarix.plugins.android.base)
        }
    }
}

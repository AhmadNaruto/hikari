import hikari.gradle.extensions.alias
import hikari.gradle.extensions.hikarix
import hikari.gradle.extensions.libs
import hikari.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("UNUSED")
class PluginAndroidLibrary : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.android.library)
            alias(hikarix.plugins.android.base)
        }
    }
}

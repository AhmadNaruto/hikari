dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../libs.versions.toml"))
        }
        create("hikarix") {
            from(files("../hikari.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"

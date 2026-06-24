rootProject.name = "maeve"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        // Fabric Loom + Fabric tooling
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        // Compose Multiplatform / JetBrains
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") { name = "Compose Dev" }
        google()
    }
    // Convention plugins shared across all modules.
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        google()
    }
}

include(":shared")
include(":mod")
include(":launcher")
include(":backend")

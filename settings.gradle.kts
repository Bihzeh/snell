rootProject.name = "snell"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") { name = "Compose Dev" }
        google()
    }
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") { name = "Compose Dev" }
        google()
    }
}

include(":shared")
include(":mod")
include(":launcher")
include(":backend")

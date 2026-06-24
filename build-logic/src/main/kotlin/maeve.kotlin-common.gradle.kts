// Shared Kotlin/JVM conventions for every Maeve JVM module.
// Applied via `plugins { id("maeve.kotlin-common") }`.
//
// Repositories are intentionally NOT declared here. They are centralized in
// settings.gradle.kts (dependencyResolutionManagement). Declaring project-level
// repositories would override that list under Gradle's PREFER_PROJECT mode and
// drop Google/Fabric/Compose maven for this module.

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
}

group = providers.gradleProperty("maeve.group").getOrElse("gg.maeve")
version = providers.gradleProperty("maeve.version").getOrElse("0.0.1-SNAPSHOT")

kotlin {
    jvmToolchain(25) // Minecraft 26.x requires JDK 25
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

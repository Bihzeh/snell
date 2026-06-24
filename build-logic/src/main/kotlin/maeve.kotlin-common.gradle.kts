// Shared Kotlin/JVM conventions for every Maeve JVM module.
// Applied via `plugins { id("maeve.kotlin-common") }`.

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
}

group = providers.gradleProperty("maeve.group").getOrElse("gg.maeve")
version = providers.gradleProperty("maeve.version").getOrElse("0.0.1-SNAPSHOT")

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(25) // Minecraft 26.1 requires JDK 25
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        // Treat warnings as a signal, not a build break, during early scaffolding.
        allWarningsAsErrors.set(false)
    }
}

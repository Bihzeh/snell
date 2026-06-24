plugins {
    id("maeve.kotlin-common")
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    // Microsoft auth + Mojang downloads.
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)

    implementation(project(":shared"))
}

compose.desktop {
    application {
        mainClass = "gg.maeve.launcher.MainKt"
        nativeDistributions {
            // Windows-first; macOS/Linux added later (see ADR-0004).
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
            )
            packageName = "Maeve"
            packageVersion = (project.version as String).substringBefore("-").ifEmpty { "1.0.0" }
        }
    }
}

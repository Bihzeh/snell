plugins {
    id("snell.kotlin-common")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
    // JUnit 5 engine for the kotlin.test assertions (test-only; not shipped).
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

tasks.test { useJUnitPlatform() }

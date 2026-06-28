// Fabric mod for Minecraft 26.2.
//
// 26.x build model (verified June 2026 against the Fabric example-mod 26.2 branch
// and docs.fabricmc.net/develop/porting): Minecraft jars ship UNOBFUSCATED.
//   * Plugin id is `net.fabricmc.fabric-loom` (the no-remap Loom).
//   * NO mappings(...) dependency.
//   * Plain `implementation` (NOT modImplementation); output is `jar` (no remapJar).
//   * Compile against JDK 25.

plugins {
    id("snell.kotlin-common")
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    implementation(libs.fabric.language.kotlin)

    // Shared cosmetics protocol + version constants. include() JiJ-nests shared.jar
    // inside the mod jar so the Fabric classloader can load gg.snell.shared.* at runtime
    // (Phase 3 cosmetics reference it); implementation() gives compile-time visibility.
    include(project(":shared"))
    implementation(project(":shared"))

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

tasks.test { useJUnitPlatform() }

loom {
    mixin {
        defaultRefmapName.set("snell.refmap.json")
    }
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "minecraft" to libs.versions.minecraft.get(),
        "loader" to libs.versions.fabric.loader.get(),
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
}

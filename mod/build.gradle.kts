// Fabric mod for Minecraft 26.2.
//
// 26.x build model (verified June 2026 against the Fabric example-mod 26.2 branch
// and docs.fabricmc.net/develop/porting): Minecraft jars ship UNOBFUSCATED.
//   * Plugin id is `net.fabricmc.fabric-loom` (the no-remap Loom).
//   * NO mappings(...) dependency.
//   * Plain `implementation` (NOT modImplementation); output is `jar` (no remapJar).
//   * Compile against JDK 25.

plugins {
    id("maeve.kotlin-common")
    alias(libs.plugins.fabric.loom)
}

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    implementation(libs.fabric.language.kotlin)

    // Shared cosmetics protocol + version constants (bundled via JiJ in Phase 1).
    implementation(project(":shared"))
}

loom {
    mixin {
        defaultRefmapName.set("maeve.refmap.json")
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

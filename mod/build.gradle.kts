// Fabric mod for Minecraft 26.1.
//
// IMPORTANT — 26.1 build model (verified June 2026, docs.fabricmc.net/develop/porting):
//   * Minecraft jars now ship UNOBFUSCATED. Loom no longer remaps.
//   * Use the no-remap `net.fabricmc.fabric-loom` plugin (NOT the old `fabric-loom`).
//   * Do NOT declare a `mappings(...)` dependency (Mojang mappings are built in).
//   * Use plain `implementation`/`compileOnly` (NOT `modImplementation`/`modCompileOnly`).
//   * Compile against JDK 25.
// Confirm the exact DSL against live Loom 1.15 docs on the first real build.

plugins {
    id("maeve.kotlin-common")
    alias(libs.plugins.fabric.loom)
}

dependencies {
    minecraft(libs.minecraft)

    // No mappings(...) line: 26.1 jars are unobfuscated (Mojang mappings).

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    implementation(libs.fabric.language.kotlin)

    // Shared cosmetics protocol + version constants.
    implementation(project(":shared"))
}

loom {
    // Mixin refmap name; harmless to set even with the no-remap plugin.
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

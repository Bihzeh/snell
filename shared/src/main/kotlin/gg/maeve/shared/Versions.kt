package gg.maeve.shared

/**
 * Version constants shared across the mod, launcher, and backend so that the
 * targeted Minecraft / Fabric stack is defined in exactly one place.
 *
 * Verified live June 2026. See gradle/libs.versions.toml for the build-side
 * source of truth; this object mirrors the runtime-relevant subset.
 */
object Versions {
    const val MINECRAFT = "26.2"
    const val FABRIC_LOADER = "0.19.3"

    /** Bundled performance mods placed into the game profile by the launcher. */
    val BUNDLED_MODS: List<BundledMod> = listOf(
        BundledMod("sodium", "0.9.0", "LGPL-3.0", "https://modrinth.com/mod/sodium"),
        BundledMod("lithium", "0.25.0", "LGPL-3.0-only", "https://modrinth.com/mod/lithium"),
    )
}

data class BundledMod(
    val id: String,
    val version: String,
    val license: String,
    val source: String,
)

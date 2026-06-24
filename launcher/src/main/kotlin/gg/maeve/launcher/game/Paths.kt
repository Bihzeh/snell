package gg.maeve.launcher.game

import java.nio.file.Path
import kotlin.io.path.createDirectories

/**
 * Filesystem layout for launcher-managed game data. Windows: %APPDATA%/Maeve;
 * other OSes: ~/.maeve. A shared cache (assets/libraries/versions/jre) plus
 * per-instance game directories.
 */
class MaevePaths(val root: Path) {
    val assets: Path get() = root.resolve("assets")
    val assetObjects: Path get() = assets.resolve("objects")
    val assetIndexes: Path get() = assets.resolve("indexes")
    val libraries: Path get() = root.resolve("libraries")
    val versions: Path get() = root.resolve("versions")
    val jre: Path get() = root.resolve("jre")

    fun instance(id: String): Path = root.resolve("instances").resolve(id)
    fun mods(instanceId: String): Path = instance(instanceId).resolve("mods")
    fun natives(instanceId: String): Path = instance(instanceId).resolve("natives")
    fun clientJar(version: String): Path = versions.resolve(version).resolve("$version.jar")

    /** Resolve a relative artifact path under [libraries], rejecting traversal (e.g. "../"). */
    fun safeLibrary(relative: String): Path {
        val resolved = libraries.resolve(relative).normalize()
        require(resolved.startsWith(libraries.normalize())) { "Path traversal in library coordinate: $relative" }
        return resolved
    }

    fun ensureBase() {
        listOf(assetObjects, assetIndexes, libraries, versions, jre).forEach { it.createDirectories() }
    }

    companion object {
        fun default(): MaevePaths {
            val os = System.getProperty("os.name").lowercase()
            val base = when {
                os.contains("win") -> Path.of(System.getenv("APPDATA") ?: System.getProperty("user.home"), "Maeve")
                else -> Path.of(System.getProperty("user.home"), ".maeve")
            }
            return MaevePaths(base)
        }
    }
}

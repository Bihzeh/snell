package gg.snell.launcher.game

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

/**
 * Filesystem layout for launcher-managed game data. Windows: %APPDATA%/Snell;
 * other OSes: ~/.snell. A shared cache (assets/libraries/versions/jre) plus
 * per-instance game directories.
 */
class SnellPaths(val root: Path) {
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
        fun default(): SnellPaths {
            val home = System.getProperty("user.home")
            val os = System.getProperty("os.name").lowercase()
            val (legacy, base) = if (os.contains("win")) {
                val appData = System.getenv("APPDATA") ?: home
                Path.of(appData, "Maeve") to Path.of(appData, "Snell")
            } else {
                Path.of(home, ".maeve") to Path.of(home, ".snell")
            }
            // One-time carry-over from the pre-rename (Maeve) data dir so existing installs
            // keep their instances, auth tokens and caches after the rebrand.
            migrateLegacyData(legacy, base)
            return SnellPaths(base)
        }
    }
}

/**
 * Moves the legacy (pre-Snell "Maeve") data directory to the current location on first run.
 * No-op once [current] exists — never clobber a live install — or when there is nothing to
 * migrate. Best-effort: any IO failure is swallowed so a migration hiccup can't block launch;
 * worst case the user starts on a fresh data dir and the legacy one is left intact.
 */
internal fun migrateLegacyData(legacy: Path, current: Path) {
    if (Files.exists(current) || !Files.isDirectory(legacy)) return
    runCatching {
        current.parent?.let { Files.createDirectories(it) }
        Files.move(legacy, current)
        migrateInstanceConfigNamespace(current)
    }
}

/**
 * Per instance, the mod wrote its settings under config/maeve/; the renamed mod reads
 * config/snell/. The top-level [migrateLegacyData] move carries the old subdir over verbatim,
 * so rename it in each instance — otherwise every migrated user's HUD config (enabled modules,
 * anchors, styles) silently resets to defaults despite the file being present. Best-effort.
 */
private fun migrateInstanceConfigNamespace(root: Path) {
    val instances = root.resolve("instances")
    if (!Files.isDirectory(instances)) return
    runCatching {
        Files.newDirectoryStream(instances).use { instanceDirs ->
            for (instance in instanceDirs) {
                val old = instance.resolve("config").resolve("maeve")
                val new = instance.resolve("config").resolve("snell")
                if (Files.isDirectory(old) && !Files.exists(new)) runCatching { Files.move(old, new) }
            }
        }
    }
}

/**
 * Newest non-sources jar in a dev `mod/build/libs` directory, or null if the directory is
 * absent or empty. Picks the most recently modified jar so a stale older-version jar left
 * in the directory is never chosen.
 */
internal fun findDevModJar(dir: Path): Path? {
    if (!Files.isDirectory(dir)) return null
    return Files.list(dir).use { stream ->
        stream.filter { val n = it.fileName.toString(); n.endsWith(".jar") && !n.contains("sources") }
            .max(compareBy { Files.getLastModifiedTime(it) })
            .orElse(null)
    }
}

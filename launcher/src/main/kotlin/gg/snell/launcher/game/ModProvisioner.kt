package gg.snell.launcher.game

import kotlinx.serialization.Serializable
import java.io.InputStream
import java.net.URLEncoder
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists

@Serializable
data class ModrinthVersion(val id: String = "", val files: List<MrFile> = emptyList()) {
    @Serializable data class MrFile(
        val url: String,
        val filename: String,
        val primary: Boolean = false,
        val size: Long? = null,
        val hashes: Hashes = Hashes(),
    )
    @Serializable data class Hashes(val sha1: String? = null)
}

/**
 * Places the bundled performance/QoL mods into the instance's mods/ folder via the
 * Modrinth API (Fabric API, Fabric Language Kotlin, Sodium, Lithium) plus the Snell
 * mod itself. Downloads are verified against Modrinth's sha1 + size.
 */
class ModProvisioner(private val net: Net, private val paths: SnellPaths) {

    suspend fun provision(
        instanceId: String,
        mcVersion: String,
        localSnellMod: Path?,
        enabledMods: Set<String>? = null,
        onStatus: (String) -> Unit,
    ) {
        val modsDir = paths.mods(instanceId)
        Files.createDirectories(modsDir)
        pruneLegacySelfMods(modsDir)
        val slugs = selectBundledMods(enabledMods)
        for (slug in slugs) {
            onStatus("Mod: $slug")
            val file = latestFabricFile(slug, mcVersion)
            net.download(file.url, modsDir.resolve(file.filename), sha1 = file.hashes.sha1, size = file.size)
        }
        // Snell is the core product mod — always installed (NOT gated by enabledMods,
        // unlike the toggleable BUNDLED performance mods above). Dev builds may pass a
        // freshly built jar; shipped launchers extract the jar bundled into the
        // distribution. A missing bundle degrades to "no Snell mod" with a visible
        // status rather than blocking the launch.
        val installed = installSnellMod(
            modsDir,
            localSnellMod,
            openBundled = { bundledModStream(BUNDLED_MOD_RESOURCE) },
            onStatus = onStatus,
        )
        if (!installed) onStatus("Snell mod unavailable — not bundled in this build")
    }

    private suspend fun latestFabricFile(slug: String, mc: String): ModrinthVersion.MrFile {
        val mcEnc = URLEncoder.encode(mc, "UTF-8")
        val url = "https://api.modrinth.com/v2/project/$slug/version" +
            "?loaders=%5B%22fabric%22%5D&game_versions=%5B%22$mcEnc%22%5D"
        val versions = LauncherJson.decodeFromString<List<ModrinthVersion>>(net.text(url))
        val v = versions.firstOrNull() ?: error("Modrinth: no '$slug' build for Minecraft $mc")
        return v.files.firstOrNull { it.primary }
            ?: v.files.firstOrNull()
            ?: error("Modrinth: '$slug' version ${v.id} for $mc has no files")
    }

    private companion object {
        const val BUNDLED_MOD_RESOURCE = "bundled-mods/snell.jar"
    }
}

/**
 * Bundled mods placed into the instance's mods/ folder. fabric-api and fabric-language-kotlin
 * are REQUIRED runtime dependencies of the Snell mod, so they install unconditionally; the
 * performance mods are user-toggleable. [enabledMods] null installs every optional mod;
 * otherwise only those present in the set. Required mods are never filtered out — dropping
 * them makes Fabric abort the launch with a missing-dependency error.
 */
internal val REQUIRED_MODS = listOf("fabric-api", "fabric-language-kotlin")
internal val OPTIONAL_MODS = listOf("sodium", "lithium")

internal fun selectBundledMods(enabledMods: Set<String>?): List<String> =
    REQUIRED_MODS + (if (enabledMods == null) OPTIONAL_MODS else OPTIONAL_MODS.filter { it in enabledMods })

/**
 * Removes the pre-rename self-mod jar (maeve.jar) from [modsDir]. The product mod now ships as
 * snell.jar with a DIFFERENT Fabric mod id, so a data dir migrated from the old install still
 * holds the old jar — and Fabric would load BOTH copies at once (duplicate HUD overlay, menu
 * keybind and mixins). Best-effort; a failure to delete just leaves the harmless duplicate.
 */
internal fun pruneLegacySelfMods(modsDir: Path) {
    runCatching { Files.deleteIfExists(modsDir.resolve("maeve.jar")) }
}

/**
 * Opens a classpath resource, trying the loaders most likely to see a resource bundled
 * into the launcher distribution first: this module's own loader (the resource ships in
 * the same jar as [ModProvisioner]), then the thread context loader, then the system
 * loader. Returns the first hit, or null if no loader can see it.
 */
internal fun bundledModStream(resource: String): InputStream? {
    val loaders = listOfNotNull(
        ModProvisioner::class.java.classLoader,
        Thread.currentThread().contextClassLoader,
        ClassLoader.getSystemClassLoader(),
    )
    for (loader in loaders) loader.getResourceAsStream(resource)?.let { return it }
    return null
}

/**
 * Installs the Snell mod into [modsDir] as snell.jar. Prefers an explicit [localOverride]
 * jar (dev fast-iteration on a freshly built mod/build/libs jar); otherwise extracts the
 * jar bundled into the launcher distribution (opened by [openBundled]). Writes via a temp
 * sibling + atomic move so a kill mid-write never leaves a truncated jar. Returns true if
 * a jar was installed, false if neither source was available.
 */
internal fun installSnellMod(
    modsDir: Path,
    localOverride: Path?,
    openBundled: () -> InputStream?,
    onStatus: (String) -> Unit = {},
): Boolean {
    val target = modsDir.resolve("snell.jar")
    if (localOverride != null && localOverride.exists()) {
        atomicReplace(target) { tmp -> Files.copy(localOverride, tmp, StandardCopyOption.REPLACE_EXISTING) }
        onStatus("Mod: snell (local)")
        return true
    }
    val stream = openBundled() ?: return false
    stream.use { s -> atomicReplace(target) { tmp -> Files.copy(s, tmp, StandardCopyOption.REPLACE_EXISTING) } }
    onStatus("Mod: snell")
    return true
}

/**
 * Runs [write] against a temp file in [target]'s directory, then moves it onto [target]
 * atomically (falling back to a plain replace where ATOMIC_MOVE is unsupported).
 */
private fun atomicReplace(target: Path, write: (Path) -> Unit) {
    val tmp = Files.createTempFile(target.parent, "snell", ".tmp")
    try {
        write(tmp)
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING)
        }
    } finally {
        Files.deleteIfExists(tmp)
    }
}

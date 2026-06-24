package gg.maeve.launcher.game

import kotlinx.serialization.Serializable
import java.net.URLEncoder
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
 * Modrinth API (Fabric API, Fabric Language Kotlin, Sodium, Lithium) plus the local
 * Maeve mod jar (dev). Downloads are verified against Modrinth's sha1 + size.
 */
class ModProvisioner(private val net: Net, private val paths: MaevePaths) {

    suspend fun provision(
        instanceId: String,
        mcVersion: String,
        localMaeveMod: Path?,
        enabledMods: Set<String>? = null,
        onStatus: (String) -> Unit,
    ) {
        val modsDir = paths.mods(instanceId)
        Files.createDirectories(modsDir)
        val slugs = if (enabledMods == null) BUNDLED else BUNDLED.filter { it in enabledMods }
        for (slug in slugs) {
            onStatus("Mod: $slug")
            val file = latestFabricFile(slug, mcVersion)
            net.download(file.url, modsDir.resolve(file.filename), sha1 = file.hashes.sha1, size = file.size)
        }
        localMaeveMod?.takeIf { it.exists() }?.let {
            Files.copy(it, modsDir.resolve("maeve.jar"), StandardCopyOption.REPLACE_EXISTING)
            onStatus("Mod: maeve (local)")
        }
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
        val BUNDLED = listOf("fabric-api", "fabric-language-kotlin", "sodium", "lithium")
    }
}

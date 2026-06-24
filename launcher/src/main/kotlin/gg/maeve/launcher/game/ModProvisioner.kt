package gg.maeve.launcher.game

import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists

@Serializable
data class ModrinthVersion(val files: List<MrFile> = emptyList()) {
    @Serializable data class MrFile(val url: String, val filename: String, val primary: Boolean = false)
}

/**
 * Places the bundled performance/QoL mods into the instance's mods/ folder:
 * Fabric API + Fabric Language Kotlin + Sodium + Lithium (latest Fabric build for
 * the MC version, via the Modrinth API) plus the local Maeve mod jar (dev).
 */
class ModProvisioner(private val net: Net, private val paths: MaevePaths) {

    suspend fun provision(
        instanceId: String,
        mcVersion: String,
        localMaeveMod: Path?,
        onStatus: (String) -> Unit,
    ) {
        val modsDir = paths.mods(instanceId)
        Files.createDirectories(modsDir)
        for (slug in BUNDLED) {
            onStatus("Mod: $slug")
            val file = latestFabricFile(slug, mcVersion)
            net.download(file.url, modsDir.resolve(file.filename))
        }
        localMaeveMod?.takeIf { it.exists() }?.let {
            Files.copy(it, modsDir.resolve("maeve.jar"), StandardCopyOption.REPLACE_EXISTING)
            onStatus("Mod: maeve (local)")
        }
    }

    private suspend fun latestFabricFile(slug: String, mc: String): ModrinthVersion.MrFile {
        val url = "https://api.modrinth.com/v2/project/$slug/version" +
            "?loaders=%5B%22fabric%22%5D&game_versions=%5B%22$mc%22%5D"
        val versions = LauncherJson.decodeFromString<List<ModrinthVersion>>(net.text(url))
        val v = versions.firstOrNull() ?: error("No $slug build for Minecraft $mc")
        return v.files.firstOrNull { it.primary } ?: v.files.first()
    }

    private companion object {
        val BUNDLED = listOf("fabric-api", "fabric-language-kotlin", "sodium", "lithium")
    }
}

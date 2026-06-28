package gg.snell.launcher.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** Shared lenient JSON for all launcher metadata. */
internal val LauncherJson = Json { ignoreUnknownKeys = true; isLenient = true }

@Serializable
data class VersionManifest(val versions: List<ManifestEntry>) {
    @Serializable data class ManifestEntry(val id: String, val url: String)
    fun url(id: String): String =
        versions.firstOrNull { it.id == id }?.url ?: error("Version $id not in manifest")
}

@Serializable
data class VersionJson(
    val id: String,
    val mainClass: String,
    val downloads: Downloads,
    val libraries: List<Library> = emptyList(),
    val assetIndex: AssetIndexRef,
    val arguments: Arguments = Arguments(),
) {
    @Serializable data class Downloads(val client: Download)
    @Serializable data class Download(val url: String, val sha1: String, val size: Long)
    @Serializable data class AssetIndexRef(val id: String, val url: String, val sha1: String, val size: Long, val totalSize: Long = 0)
}

@Serializable
data class Library(
    val name: String,
    val downloads: LibDownloads? = null,
    val rules: List<Rule>? = null,
) {
    @Serializable data class LibDownloads(val artifact: Artifact? = null)
    @Serializable data class Artifact(val path: String, val url: String, val sha1: String, val size: Long)
}

@Serializable
data class Rule(
    val action: String,
    val os: Os? = null,
    val features: Map<String, Boolean>? = null,
) {
    @Serializable data class Os(val name: String? = null, val arch: String? = null, val version: String? = null)
}

/** game/jvm entries are either a bare string or {rules, value:(string|[string])}. Parsed in LaunchBuilder. */
@Serializable
data class Arguments(
    val game: List<JsonElement> = emptyList(),
    val jvm: List<JsonElement> = emptyList(),
)

@Serializable
data class AssetIndex(
    val objects: Map<String, Asset> = emptyMap(),
    @SerialName("map_to_resources") val mapToResources: Boolean = false,
    val virtual: Boolean = false,
) {
    @Serializable data class Asset(val hash: String, val size: Long)
}

/** Fetches Mojang launcher metadata. */
class MojangMeta(private val net: Net) {
    suspend fun manifest(): VersionManifest =
        LauncherJson.decodeFromString(net.text(VERSION_MANIFEST))

    suspend fun version(url: String): VersionJson =
        LauncherJson.decodeFromString(net.text(url))

    suspend fun assetIndex(ref: VersionJson.AssetIndexRef): AssetIndex =
        LauncherJson.decodeFromString(net.text(ref.url))

    companion object {
        const val VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
        const val RESOURCES = "https://resources.download.minecraft.net"
    }
}

package gg.snell.launcher.game

import kotlinx.serialization.Serializable

@Serializable
data class FabricProfile(
    val mainClass: String,
    val libraries: List<FabricLib> = emptyList(),
    val arguments: Arguments = Arguments(),
) {
    @Serializable data class FabricLib(val name: String, val url: String? = null)
}

/** Fetches the Fabric loader launch profile and resolves its library coordinates. */
class FabricMeta(private val net: Net) {

    suspend fun profile(minecraft: String, loader: String): FabricProfile =
        LauncherJson.decodeFromString(net.text(profileUrl(minecraft, loader)))

    /** Resolve a Fabric library to (localRelativePath, downloadUrl). */
    fun resolve(lib: FabricProfile.FabricLib): Pair<String, String> {
        val path = mavenPath(lib.name)
        val base = (lib.url ?: MAVEN_CENTRAL).trimEnd('/') + "/"
        return path to (base + path)
    }

    companion object {
        const val MAVEN_CENTRAL = "https://repo1.maven.org/maven2/"

        fun profileUrl(minecraft: String, loader: String): String =
            "https://meta.fabricmc.net/v2/versions/loader/$minecraft/$loader/profile/json"

        /** "group:artifact:version[:classifier]" -> "group/path/artifact/version/artifact-version[-classifier].jar" */
        fun mavenPath(name: String): String {
            val parts = name.split(":")
            require(parts.size >= 3) { "Bad maven coordinate: $name" }
            val (group, artifact, version) = parts
            val classifier = parts.getOrNull(3)?.let { "-$it" } ?: ""
            val groupPath = group.replace('.', '/')
            return "$groupPath/$artifact/$version/$artifact-$version$classifier.jar"
        }
    }
}

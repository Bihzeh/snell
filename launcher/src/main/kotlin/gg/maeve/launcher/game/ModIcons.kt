package gg.maeve.launcher.game

import kotlinx.serialization.Serializable
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readBytes

@Serializable
data class ModrinthProject(val slug: String = "", val icon_url: String? = null)

/**
 * Fetches mod logos from the Modrinth API and caches them on disk (`<data>/icons/<slug>.img`),
 * returning the encoded image bytes per slug (the UI layer decodes to a Compose bitmap). Network
 * and parse failures are swallowed — a missing icon just means the caller shows a generic glyph.
 * The disk cache makes repeat loads instant and offline-safe.
 */
object ModIcons {
    private const val UA = "Maeve/0.1 (github.com/Bihzeh/maeve)"

    internal fun projectsUrl(slugs: List<String>): String {
        val ids = "[" + slugs.joinToString(",") { "\"$it\"" } + "]"
        return "https://api.modrinth.com/v2/projects?ids=" + URLEncoder.encode(ids, "UTF-8")
    }

    suspend fun load(slugs: List<String>, root: Path, net: Net): Map<String, ByteArray> {
        val dir = root.resolve("icons")
        Files.createDirectories(dir)
        fun cache(slug: String): Path = dir.resolve("$slug.img")

        val missing = slugs.filter { !cache(it).exists() }
        if (missing.isNotEmpty()) runCatching {
            val projects = LauncherJson.decodeFromString<List<ModrinthProject>>(
                net.text(projectsUrl(missing), mapOf("User-Agent" to UA)),
            )
            for (p in projects) {
                val iconUrl = p.icon_url ?: continue
                runCatching { net.download(iconUrl, cache(p.slug)) }
            }
        }

        val out = LinkedHashMap<String, ByteArray>()
        for (slug in slugs) {
            val f = cache(slug)
            if (f.exists()) runCatching { out[slug] = f.readBytes() }
        }
        return out
    }
}

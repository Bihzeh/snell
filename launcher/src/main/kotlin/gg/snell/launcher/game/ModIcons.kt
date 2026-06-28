package gg.snell.launcher.game

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.net.URLEncoder
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.writeText

@Serializable
data class ModrinthProject(
    val slug: String = "",
    val title: String = "",
    val description: String = "",
    val categories: List<String> = emptyList(),
    val icon_url: String? = null,
)

/** Display copy for a bundled mod, sourced live from Modrinth (title, tagline, primary tag). */
@Serializable
data class ModInfo(val title: String, val description: String, val category: String)

/** Modrinth's category slugs are lowercase; the UI shows the title-cased form ("optimization" → "Optimization"). */
internal fun ModrinthProject.toInfo(): ModInfo = ModInfo(
    title = title.ifBlank { slug },
    description = description,
    category = categories.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "",
)

/**
 * Fetches each bundled mod's logo AND display copy (title, tagline, primary tag) from the
 * Modrinth `/v2/projects` endpoint, caching both on disk — the icon bytes at
 * `<data>/icons/<slug>.img`, the text at `<data>/icons/<slug>.meta.json` — so repeat loads
 * are instant and offline-safe. Network/parse failures are swallowed: a missing icon falls
 * back to the generic glyph and missing metadata falls back to the caller's hardcoded copy.
 */
object ModIcons {
    private const val UA = "Snell/0.1 (github.com/Bihzeh/snell)"

    /** Decoded icon bytes + display copy for the requested mods, each keyed by slug. */
    data class Catalog(val icons: Map<String, ByteArray>, val info: Map<String, ModInfo>)

    internal fun projectsUrl(slugs: List<String>): String {
        val ids = "[" + slugs.joinToString(",") { "\"$it\"" } + "]"
        return "https://api.modrinth.com/v2/projects?ids=" + URLEncoder.encode(ids, "UTF-8")
    }

    suspend fun load(slugs: List<String>, root: Path, net: Net): Catalog {
        val dir = root.resolve("icons")
        Files.createDirectories(dir)
        fun iconCache(slug: String): Path = dir.resolve("$slug.img")
        fun metaCache(slug: String): Path = dir.resolve("$slug.meta.json")

        // The meta file is the "already fetched" marker: every real project yields metadata,
        // whereas a project with no icon_url would never write an icon file and so would
        // re-hit the API on every load if we keyed off the icon cache instead.
        val missing = slugs.filter { !metaCache(it).exists() }
        if (missing.isNotEmpty()) runCatching {
            val projects = LauncherJson.decodeFromString<List<ModrinthProject>>(
                net.text(projectsUrl(missing), mapOf("User-Agent" to UA)),
            )
            for (p in projects) {
                // Download the icon BEFORE writing the meta marker, since the marker is what
                // drops a slug out of `missing`. Writing it only once the icon has actually
                // landed means a transient icon-download failure is retried on the next load
                // instead of permanently masking the missing logo; a project with no icon has
                // nothing to wait for. The meta itself is written atomically (temp + move) so
                // an interrupted write never leaves a truncated file that pins the mod forever.
                val iconUrl = p.icon_url
                if (iconUrl != null && !runCatching { net.download(iconUrl, iconCache(p.slug)) }.isSuccess) continue
                runCatching { writeAtomic(metaCache(p.slug), LauncherJson.encodeToString(p.toInfo())) }
            }
        }

        val icons = LinkedHashMap<String, ByteArray>()
        val info = LinkedHashMap<String, ModInfo>()
        for (slug in slugs) {
            iconCache(slug).let { if (it.exists()) runCatching { icons[slug] = it.readBytes() } }
            val mc = metaCache(slug)
            if (mc.exists()) {
                runCatching { LauncherJson.decodeFromString<ModInfo>(mc.readText()) }
                    .onSuccess { info[slug] = it }
                    // Corrupt cache (e.g. an interrupted legacy write) — delete it so the next
                    // load treats the slug as missing and re-fetches, rather than staying pinned.
                    .onFailure { runCatching { mc.deleteIfExists() } }
            }
        }
        return Catalog(icons, info)
    }
}

/** Writes [text] to [target] via a sibling temp file + atomic move, so an interrupted write never lands a partial file. */
private fun writeAtomic(target: Path, text: String) {
    val tmp = Files.createTempFile(target.parent, target.fileName.toString(), ".tmp")
    try {
        tmp.writeText(text)
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING)
        }
    } finally {
        Files.deleteIfExists(tmp)
    }
}

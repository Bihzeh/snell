package gg.maeve.launcher.update

import gg.maeve.launcher.game.LauncherJson
import gg.maeve.launcher.game.Net
import java.awt.Desktop
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

/**
 * Checks GitHub Releases for a newer launcher build and applies it. Pre-releases are
 * included (we ship those). The selection logic is pure ([selectUpdate]) and unit-tested;
 * [check] just feeds it the live releases JSON.
 */
class UpdateService(
    private val current: SemVer? = SemVer.parse(BuildInfo.version),
    private val fetchReleases: suspend () -> String = {
        Net().use { it.text(RELEASES_URL, mapOf("User-Agent" to "Maeve-Launcher", "Accept" to "application/vnd.github+json")) }
    },
) {
    suspend fun check(): UpdateInfo? {
        val releases = LauncherJson.decodeFromString<List<GhRelease>>(fetchReleases())
        return selectUpdate(releases, current)
    }

    /** Pure: newest non-draft release that has a platform installer + is newer than [current]. */
    fun selectUpdate(releases: List<GhRelease>, current: SemVer?): UpdateInfo? {
        val newest = releases.asSequence()
            .filter { !it.draft }
            .mapNotNull { r ->
                val v = SemVer.parse(r.tagName) ?: return@mapNotNull null
                val asset = INSTALLER_EXTS.firstNotNullOfOrNull { ext ->
                    r.assets.firstOrNull { it.name.endsWith(ext, ignoreCase = true) }
                } ?: return@mapNotNull null
                val sums = r.assets.firstOrNull { it.name.equals("SHA256SUMS.txt", ignoreCase = true) }?.url
                UpdateInfo(v, r.tagName, asset.url, asset.name, sums)
            }
            .maxByOrNull { it.version } ?: return null
        return if (current == null || newest.version > current) newest else null
    }

    /** Download the verified installer and run it, then exit so it can replace files. */
    suspend fun apply(info: UpdateInfo, onStatus: (String) -> Unit) {
        if (!isWindows()) { onStatus("Opening the releases page…"); openReleases(); return }
        val dir = Files.createTempDirectory("maeve-update")
        val installer = dir.resolve(info.installerName)
        onStatus("Downloading ${info.tag}…")
        Net().use { net ->
            val expected = info.sha256SumsUrl?.let { url -> sha256For(net.text(url), info.installerName) }
            net.download(info.installerUrl, installer, sha256 = expected)
        }
        onStatus("Starting installer…")
        ProcessBuilder(installerCommand(installer)).directory(dir.toFile()).start()
        exitProcess(0)
    }

    private fun installerCommand(installer: Path): List<String> =
        if (installer.toString().endsWith(".msi", ignoreCase = true)) listOf("msiexec", "/i", installer.toString())
        else listOf(installer.toString())

    private fun openReleases() = runCatching {
        if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI(RELEASES_PAGE))
    }

    companion object {
        const val REPO = "Bihzeh/maeve"
        const val RELEASES_URL = "https://api.github.com/repos/$REPO/releases?per_page=15"
        const val RELEASES_PAGE = "https://github.com/$REPO/releases"
        private val INSTALLER_EXTS = listOf(".exe", ".msi")

        fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("win")

        /** Find the hash for [fileName] in a SHA256SUMS.txt ("<hash>  <file>" per line). */
        fun sha256For(sums: String, fileName: String): String? = sums.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.endsWith(fileName) }
            ?.substringBefore(' ')
            ?.takeIf { it.matches(Regex("[0-9a-fA-F]{64}")) }
    }
}

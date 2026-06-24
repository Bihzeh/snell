package gg.maeve.launcher.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.exists
import kotlin.io.path.isExecutable
import kotlin.io.path.name

@Serializable
data class AdoptiumRelease(val binaries: List<Binary> = emptyList()) {
    @Serializable data class Binary(@SerialName("package") val pkg: Pkg)
    @Serializable data class Pkg(val name: String, val link: String)
}

/** Ensures a Temurin 25 JRE is available to run the game; returns the java path. */
class JreProvisioner(private val net: Net, private val paths: MaevePaths) {

    suspend fun ensure(platform: Platform = Platform.current()): Path {
        findJava()?.let { return it }
        Files.createDirectories(paths.jre)
        val rel = LauncherJson.decodeFromString<List<AdoptiumRelease>>(net.text(apiUrl(platform)))
        val pkg = rel.firstOrNull()?.binaries?.firstOrNull()?.pkg ?: error("No Temurin 25 JRE for $platform")
        val archive = paths.jre.resolve(pkg.name)
        net.download(pkg.link, archive)
        extract(archive, paths.jre)
        return findJava() ?: error("java not found after extracting ${pkg.name}")
    }

    private fun findJava(): Path? {
        if (!paths.jre.exists()) return null
        val exe = if (System.getProperty("os.name").lowercase().contains("win")) "java.exe" else "java"
        return Files.walk(paths.jre).use { stream ->
            stream.filter { it.name == exe && it.parent?.name == "bin" }.findFirst().orElse(null)
        }?.also { runCatching { if (!it.isExecutable()) it.toFile().setExecutable(true) } }
    }

    private fun extract(archive: Path, dest: Path) {
        if (archive.name.endsWith(".zip")) {
            ZipInputStream(Files.newInputStream(archive)).use { zip ->
                var e = zip.nextEntry
                while (e != null) {
                    val out = dest.resolve(e.name).normalize()
                    require(out.startsWith(dest)) { "Zip slip: ${e.name}" }
                    if (e.isDirectory) Files.createDirectories(out)
                    else { Files.createDirectories(out.parent); Files.copy(zip, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING) }
                    e = zip.nextEntry
                }
            }
        } else {
            // tar.gz on Linux/macOS — use the system tar.
            val p = ProcessBuilder("tar", "-xzf", archive.toString(), "-C", dest.toString())
                .inheritIO().start()
            require(p.waitFor() == 0) { "tar extraction failed for $archive" }
        }
    }

    private fun apiUrl(p: Platform): String {
        val os = when (p.os) { "osx" -> "mac"; else -> p.os }
        val arch = if (p.arch == "arm64") "aarch64" else "x64"
        return "https://api.adoptium.net/v3/assets/feature_releases/25/ga" +
            "?architecture=$arch&image_type=jre&os=$os&vendor=eclipse&page_size=1"
    }
}

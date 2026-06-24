package gg.maeve.launcher.game

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.delay
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.DigestInputStream
import java.security.MessageDigest
import kotlin.io.path.exists
import kotlin.io.path.fileSize

/**
 * HTTP helper for metadata fetches and file downloads. Streams to a temp file then
 * verifies (size + sha) before an atomic move, so an interrupted or tampered
 * download never lands as a usable file. Retries transient failures with backoff.
 */
class Net(private val client: HttpClient = defaultClient()) : AutoCloseable {

    suspend fun text(url: String, headers: Map<String, String> = emptyMap()): String =
        withRetry { client.get(url) { headers.forEach { (k, v) -> header(k, v) } }.bodyAsText() }

    /**
     * Download [url] to [dest]. Skips if a same-size file is already present (fast).
     * On a fresh download, verifies [size]/[sha256]/[sha1] when given and fails loudly.
     */
    suspend fun download(
        url: String,
        dest: Path,
        sha1: String? = null,
        sha256: String? = null,
        size: Long? = null,
    ) {
        if (dest.exists() && isFresh(dest, sha1, size)) return
        Files.createDirectories(dest.parent)
        val part = dest.resolveSibling("${dest.fileName}.part")
        withRetry {
            client.prepareGet(url).execute { resp ->
                resp.bodyAsChannel().toInputStream().use { input ->
                    Files.newOutputStream(part).use { out -> input.copyTo(out, 1 shl 16) }
                }
            }
            verify(part, sha1, sha256, size)
            Files.move(part, dest, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun isFresh(dest: Path, sha1: String?, size: Long?): Boolean = when {
        size != null -> dest.fileSize() == size           // fast skip; integrity verified at download time
        sha1 != null -> hash(dest, "SHA-1").equals(sha1, ignoreCase = true)
        else -> true
    }

    private fun verify(file: Path, sha1: String?, sha256: String?, size: Long?) {
        size?.let { require(Files.size(file) == it) { "size mismatch for $file: ${Files.size(file)} != $it" } }
        sha256?.let { require(hash(file, "SHA-256").equals(it, ignoreCase = true)) { "sha256 mismatch for $file" } }
        sha1?.let { require(hash(file, "SHA-1").equals(it, ignoreCase = true)) { "sha1 mismatch for $file" } }
    }

    private suspend fun <T> withRetry(attempts: Int = 3, block: suspend () -> T): T {
        var last: Exception? = null
        repeat(attempts) { i ->
            try {
                return block()
            } catch (e: Exception) {
                last = e
                if (i < attempts - 1) delay(500L * (i + 1))
            }
        }
        throw last ?: IllegalStateException("download failed")
    }

    override fun close() = client.close()

    companion object {
        private fun defaultClient() = HttpClient(CIO) {
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 60_000                 // per-read idle; no whole-request cap (large files)
            }
        }

        /** Streaming digest (no whole-file buffering). */
        fun hash(path: Path, algorithm: String): String {
            val md = MessageDigest.getInstance(algorithm)
            DigestInputStream(Files.newInputStream(path), md).use { s ->
                val buf = ByteArray(1 shl 16)
                while (s.read(buf) != -1) { /* digesting */ }
            }
            return md.digest().joinToString("") { "%02x".format(it) }
        }
    }
}

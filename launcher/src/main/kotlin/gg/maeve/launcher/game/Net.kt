package gg.maeve.launcher.game

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.exists
import kotlin.io.path.fileSize

/** Thin HTTP helper for metadata fetches and file downloads. */
class Net(private val client: HttpClient = HttpClient(CIO)) : AutoCloseable {

    suspend fun text(url: String): String = client.get(url).bodyAsText()

    /** Download [url] to [dest], skipping if a file with matching sha1 (or size) already exists. */
    suspend fun download(url: String, dest: Path, sha1: String? = null, size: Long? = null) {
        if (dest.exists() && upToDate(dest, sha1, size)) return
        Files.createDirectories(dest.parent)
        val bytes = client.get(url).readBytes()
        Files.write(dest, bytes)
    }

    private fun upToDate(dest: Path, sha1: String?, size: Long?): Boolean = when {
        size != null -> dest.fileSize() == size               // fast path: avoid hashing on re-runs
        sha1 != null -> sha1Of(dest).equals(sha1, ignoreCase = true)
        else -> true
    }

    override fun close() = client.close()

    companion object {
        fun sha1Of(path: Path): String {
            val md = MessageDigest.getInstance("SHA-1")
            return md.digest(Files.readAllBytes(path)).joinToString("") { "%02x".format(it) }
        }
    }
}

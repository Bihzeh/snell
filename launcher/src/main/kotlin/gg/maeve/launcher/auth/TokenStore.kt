package gg.maeve.launcher.auth

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.path.exists

/**
 * Secure, client-side storage for Microsoft refresh tokens. Tokens NEVER leave the
 * user's machine and are NEVER sent to the Maeve backend (ADR-0006).
 */
interface TokenStore {
    fun saveRefreshToken(account: String, refreshToken: String)
    fun loadRefreshToken(account: String): String?
    fun clear(account: String)
}

/**
 * File-backed store using AES-256-GCM with a locally-generated key.
 *
 * SCOPE OF PROTECTION (honest): the key sits beside the ciphertext, so this is
 * obfuscation, not strong encryption-at-rest. On POSIX systems both files are chmod
 * 600 (owner-only). On Windows POSIX perms are unavailable — a warning is emitted and
 * the files inherit the directory ACL (NOT owner-restricted). The property that
 * matters per ADR-0006 — tokens are local-only, never sent to a server — always holds.
 * Hardening to the OS credential store (Windows DPAPI / macOS Keychain / libsecret) is
 * a tracked follow-up; this interface lets that swap in without touching callers.
 */
class FileTokenStore(private val dir: Path) : TokenStore {
    private val keyFile: Path get() = dir.resolve("auth.key")
    private fun tokenFile(account: String): Path = dir.resolve("token-${sanitize(account)}.enc")

    override fun saveRefreshToken(account: String, refreshToken: String) {
        Files.createDirectories(dir)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, key(), GCMParameterSpec(128, iv))
        val out = iv + cipher.doFinal(refreshToken.toByteArray(Charsets.UTF_8))
        Files.write(tokenFile(account), Base64.getEncoder().encode(out))
        restrict(tokenFile(account))
    }

    override fun loadRefreshToken(account: String): String? {
        val file = tokenFile(account)
        if (!file.exists()) return null
        return try {
            val raw = Base64.getDecoder().decode(Files.readAllBytes(file))
            val iv = raw.copyOfRange(0, 12)
            val ct = raw.copyOfRange(12, raw.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ct), Charsets.UTF_8)
        } catch (e: AEADBadTagException) {
            // Authentication tag mismatch -> corrupt or tampered. Discard and force re-login.
            System.err.println("Maeve: WARNING — stored token failed authentication (corrupt or tampered); discarding.")
            Files.deleteIfExists(file)
            null
        } catch (e: Exception) {
            null
        }
    }

    override fun clear(account: String) {
        Files.deleteIfExists(tokenFile(account))
    }

    private fun key(): SecretKeySpec {
        Files.createDirectories(dir)
        if (!keyFile.exists()) {
            val k = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey().encoded
            try {
                Files.write(keyFile, Base64.getEncoder().encode(k), StandardOpenOption.CREATE_NEW)
                restrict(keyFile)
            } catch (e: java.nio.file.FileAlreadyExistsException) {
                // Another instance created it first; fall through and read the winner's key.
            }
        }
        return SecretKeySpec(Base64.getDecoder().decode(Files.readAllBytes(keyFile)), "AES")
    }

    private fun restrict(path: Path) {
        runCatching { Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------")) }
            .onFailure {
                System.err.println(
                    "Maeve: WARNING — could not restrict permissions on ${path.fileName} " +
                        "(non-POSIX filesystem, e.g. Windows); the token file is not OS-access-controlled. " +
                        "OS-keychain/DPAPI hardening is pending.",
                )
            }
    }

    private fun sanitize(account: String) = account.replace(Regex("[^A-Za-z0-9_.-]"), "_")
}

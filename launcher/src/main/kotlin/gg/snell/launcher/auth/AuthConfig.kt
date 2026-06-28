package gg.snell.launcher.auth

import gg.snell.launcher.game.SnellPaths
import java.nio.file.Files
import java.nio.file.Path

/**
 * Resolves the Azure application (client) ID, in priority order:
 *   1. the SNELL_AZURE_CLIENT_ID env var,
 *   2. an `azure_client_id.txt` file in the launcher data dir,
 *   3. the bundled [DEFAULT_CLIENT_ID].
 * The client ID is a PUBLIC-client identifier — it travels in every device-code request and is
 * not a secret, so it is safe to embed in the binary / public repo. (1) and (2) exist only to
 * override the default per-machine; the Minecraft-API approval is an entitlement on this same app
 * registration and never mints a new ID.
 */
object AuthConfig {
    const val ENV = "SNELL_AZURE_CLIENT_ID"
    const val SCOPE = "XboxLive.signin offline_access"

    /** Snell's registered Entra app (public client, device-code flow). Not a secret. */
    const val DEFAULT_CLIENT_ID = "3d3ac659-6014-476e-a51f-9b7376fabf32"

    fun clientId(dataDir: Path = SnellPaths.default().root): String? {
        System.getenv(ENV)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        val file = dataDir.resolve("azure_client_id.txt")
        if (Files.exists(file)) Files.readString(file).trim().ifEmpty { null }?.let { return it }
        return DEFAULT_CLIENT_ID.ifEmpty { null }
    }
}

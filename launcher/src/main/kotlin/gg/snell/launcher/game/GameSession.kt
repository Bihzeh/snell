package gg.snell.launcher.game

import java.util.UUID

/**
 * The account session passed to the game. Real values come from Microsoft auth
 * (next chunk); until then [offline] provides a placeholder so the client can be
 * launched and inspected. Offline sessions are rejected by online servers.
 */
data class GameSession(
    val uuid: String,
    val username: String,
    val accessToken: String,
    val userType: String, // "msa" for real Microsoft accounts, "legacy" for offline
) {
    companion object {
        fun offline(username: String = "Bihzeh"): GameSession {
            // Deterministic offline UUID from the name (matches the vanilla offline scheme).
            val uuid = UUID.nameUUIDFromBytes("OfflinePlayer:$username".toByteArray())
            return GameSession(uuid.toString(), username, accessToken = "0", userType = "legacy")
        }
    }
}

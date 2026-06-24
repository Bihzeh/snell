package gg.maeve.launcher.auth

/**
 * Secure, client-side storage for Microsoft refresh tokens. Tokens NEVER leave
 * the user's machine and are NEVER sent to the Maeve backend (ADR-0006).
 *
 * Windows-first: back with the Windows Credential Manager (DPAPI). macOS Keychain
 * and libsecret implementations land alongside those OS targets.
 */
interface TokenStore {
    fun saveRefreshToken(account: String, refreshToken: String)
    fun loadRefreshToken(account: String): String?
    fun clear(account: String)
}

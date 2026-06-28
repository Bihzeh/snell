package gg.snell.launcher.auth

import gg.snell.launcher.game.LauncherJson
import gg.snell.launcher.game.GameSession
import kotlinx.coroutines.delay
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** What the UI shows the user during device-code sign-in. */
data class DeviceCodePrompt(val userCode: String, val verificationUri: String, val expiresInSeconds: Int, val message: String)

/** Result of a successful sign-in: a game session + the refresh token to persist. */
data class AuthResult(val session: GameSession, val refreshToken: String?)

/**
 * Microsoft -> Xbox Live -> XSTS -> Minecraft authentication (device-code flow),
 * verified against the documented endpoints. The flow logic is pure over [MsaTransport]
 * so it is unit-testable with canned responses and an injectable [sleep].
 *
 * Tokens are returned to the caller to store CLIENT-SIDE (see TokenStore / ADR-0006);
 * nothing here contacts the Snell backend.
 */
class MsaDeviceCodeAuth(
    private val clientId: String,
    private val transport: MsaTransport,
    private val sleep: suspend (Long) -> Unit = { delay(it) },
) {
    private val json = LauncherJson

    suspend fun requestDeviceCode(): DeviceCodeResponse {
        val r = transport.postForm(DEVICE_CODE_URL, mapOf("client_id" to clientId, "scope" to AuthConfig.SCOPE))
        if (!r.ok) throw AuthException("Couldn't start sign-in (HTTP ${r.status}). Check the Azure client ID. ${shortError(r.body)}")
        return json.decodeFromString(r.body)
    }

    /** Polls the token endpoint until the user completes (or it expires). */
    suspend fun pollForToken(dc: DeviceCodeResponse): TokenResponse {
        var interval = dc.interval.coerceAtLeast(1)
        var waited = 0
        while (waited < dc.expiresIn) {
            sleep(interval * 1000L); waited += interval
            val r = transport.postForm(
                TOKEN_URL,
                mapOf("grant_type" to DEVICE_GRANT, "client_id" to clientId, "device_code" to dc.deviceCode),
            )
            val t: TokenResponse = json.decodeFromString(r.body)
            if (t.accessToken != null) return t
            when (t.error) {
                "authorization_pending" -> {}
                "slow_down" -> interval += 5
                "expired_token" -> throw AuthException("The sign-in code expired. Try again.")
                "authorization_declined" -> throw AuthException("Sign-in was declined.")
                else -> throw AuthException("Sign-in failed: ${t.error ?: "unknown"} ${(t.errorDescription ?: "").take(160)}".trim())
            }
        }
        throw AuthException("The sign-in code expired. Try again.")
    }

    suspend fun refresh(refreshToken: String): TokenResponse {
        val r = transport.postForm(
            TOKEN_URL,
            mapOf("grant_type" to "refresh_token", "client_id" to clientId, "refresh_token" to refreshToken, "scope" to AuthConfig.SCOPE),
        )
        val t: TokenResponse = json.decodeFromString(r.body)
        if (t.accessToken == null) throw AuthException("Session expired — please sign in again.")
        return t
    }

    private suspend fun xbl(msAccessToken: String): XboxResponse {
        val body = buildJsonObject {
            putJsonObject("Properties") {
                put("AuthMethod", "RPS"); put("SiteName", "user.auth.xboxlive.com"); put("RpsTicket", "d=$msAccessToken")
            }
            put("RelyingParty", "http://auth.xboxlive.com"); put("TokenType", "JWT")
        }
        val r = transport.postJson(XBL_URL, body.toString())
        if (!r.ok) throw AuthException("Xbox Live sign-in failed (HTTP ${r.status}).")
        return json.decodeFromString(r.body)
    }

    private suspend fun xsts(xblToken: String): XboxResponse {
        val body = buildJsonObject {
            putJsonObject("Properties") { put("SandboxId", "RETAIL"); putJsonArray("UserTokens") { add(xblToken) } }
            put("RelyingParty", "rp://api.minecraftservices.com/"); put("TokenType", "JWT")
        }
        val r = transport.postJson(XSTS_URL, body.toString())
        if (r.status == 401) throw AuthException(mapXstsError(json.decodeFromString<XstsError>(r.body).xErr))
        if (!r.ok) throw AuthException("Xbox authorization failed (HTTP ${r.status}).")
        return json.decodeFromString(r.body)
    }

    private suspend fun mcLogin(uhs: String, xstsToken: String): McLoginResponse {
        val body = buildJsonObject { put("identityToken", "XBL3.0 x=$uhs;$xstsToken") }
        val r = transport.postJson(MC_LOGIN_URL, body.toString())
        if (!r.ok) throw AuthException("Minecraft sign-in failed (HTTP ${r.status}).")
        return json.decodeFromString(r.body)
    }

    private suspend fun profile(mcToken: String): McProfile {
        val r = transport.getBearer(MC_PROFILE_URL, mcToken)
        if (r.status == 403) throw AuthException("This launcher's Microsoft API access isn't approved yet (Azure app pending), or the account can't use the API.")
        if (r.status == 404 || r.body.contains("\"error\"")) throw AuthException("This Microsoft account doesn't own Minecraft: Java Edition.")
        if (!r.ok) throw AuthException("Couldn't fetch the Minecraft profile (HTTP ${r.status}).")
        return json.decodeFromString(r.body)
    }

    /** Full interactive sign-in. [onPrompt] is invoked once with the user code to display. */
    suspend fun signInWithDeviceCode(onPrompt: (DeviceCodePrompt) -> Unit): AuthResult {
        val dc = requestDeviceCode()
        onPrompt(DeviceCodePrompt(dc.userCode, dc.verificationUri, dc.expiresIn, dc.message))
        return finish(pollForToken(dc))
    }

    /** Silent re-login from a stored refresh token. */
    suspend fun signInWithRefreshToken(refreshToken: String): AuthResult = finish(refresh(refreshToken))

    private suspend fun finish(token: TokenResponse): AuthResult {
        val msAccess = token.accessToken ?: throw AuthException("No access token from Microsoft.")
        val xbl = xbl(msAccess)
        val xsts = xsts(xbl.token)
        val uhs = xsts.userHash ?: xbl.userHash ?: throw AuthException("Missing Xbox user hash.")
        val mc = mcLogin(uhs, xsts.token)
        val profile = profile(mc.accessToken)
        val session = GameSession(profile.id, profile.name, mc.accessToken, userType = "msa")
        return AuthResult(session, token.refreshToken)
    }

    companion object {
        const val DEVICE_CODE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode"
        const val TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token"
        const val DEVICE_GRANT = "urn:ietf:params:oauth:grant-type:device_code"
        const val XBL_URL = "https://user.auth.xboxlive.com/user/authenticate"
        const val XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize"
        const val MC_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox"
        const val MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile"

        fun mapXstsError(xErr: Long): String = when (xErr) {
            2148916227L -> "This account is banned from Xbox Live."
            2148916233L -> "This Microsoft account has no Xbox profile. Create one at xbox.com, then sign in again."
            2148916235L -> "Xbox Live isn't available in this account's region."
            2148916236L, 2148916237L -> "This account needs adult verification on the Xbox website."
            2148916238L -> "This account is a child and must be added to a Family on xbox.com."
            else -> "Xbox authorization failed (XErr=$xErr)."
        }

        private fun shortError(body: String): String = body.take(180)
    }
}

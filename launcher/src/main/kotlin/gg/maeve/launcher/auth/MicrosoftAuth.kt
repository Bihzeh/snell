package gg.maeve.launcher.auth

/**
 * Microsoft -> Xbox Live -> XSTS -> Minecraft authentication (verified June 2026).
 * See docs/adr/ADR-0006.
 *
 * Flow (device-code variant; preferred for Phase 1 — no loopback server needed):
 *   1. POST https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode
 *        scope = "XboxLive.signin offline_access"  (consumers tenant; NO client secret)
 *      -> show user_code + verification_uri to the user; poll the token endpoint.
 *   2. POST https://login.microsoftonline.com/consumers/oauth2/v2.0/token
 *        grant_type=urn:ietf:params:oauth:grant-type:device_code -> MS access_token (+ refresh).
 *   3. POST https://user.auth.xboxlive.com/user/authenticate
 *        RpsTicket "d=<ms_access_token>"  -> XBL token + userhash (uhs).
 *   4. POST https://xsts.auth.xboxlive.com/xsts/authorize
 *        RelyingParty "rp://api.minecraftservices.com/" -> XSTS token.
 *   5. POST https://api.minecraftservices.com/authentication/login_with_xbox
 *        identityToken "XBL3.0 x=<uhs>;<xsts_token>" -> Minecraft access_token (24h).
 *   6. GET  https://api.minecraftservices.com/minecraft/profile -> uuid + name (ownership).
 *
 * GOTCHAS (verified):
 *   - Use the CONSUMERS tenant, not /common or an AAD tenant id.
 *   - A newly-registered Azure app MUST apply (form) for Minecraft API access or
 *     api.minecraftservices.com returns 403. (Phase 0 long-lead task — see plan.)
 *   - No client secret: public client + PKCE for the auth-code variant (Phase 2).
 *   - Microsoft tokens are stored CLIENT-SIDE only (see TokenStore). The backend
 *     never receives them.
 */
data class MinecraftSession(
    val uuid: String,
    val username: String,
    val accessToken: String,
)

interface MicrosoftAuth {
    /** Runs the device-code flow end to end, surfacing the user code via [onUserCode]. */
    suspend fun signInWithDeviceCode(onUserCode: (userCode: String, verificationUri: String) -> Unit): MinecraftSession
}

/** Phase 1 implements this with the Ktor client against the endpoints above. */
class KtorMicrosoftAuth(
    private val azureClientId: String,
) : MicrosoftAuth {
    override suspend fun signInWithDeviceCode(
        onUserCode: (String, String) -> Unit,
    ): MinecraftSession {
        TODO("Phase 1: implement the 6-step flow with the Ktor HttpClient.")
    }
}

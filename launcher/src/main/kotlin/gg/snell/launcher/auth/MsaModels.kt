package gg.snell.launcher.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Microsoft OAuth2 device-code response. */
@Serializable
data class DeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_uri") val verificationUri: String,
    @SerialName("expires_in") val expiresIn: Int,
    val interval: Int = 5,
    val message: String = "",
)

/** Token endpoint response — success carries tokens, failure carries [error]. */
@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Int? = null,
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)

/** XBL and XSTS share this success shape (Token + a user hash in DisplayClaims). */
@Serializable
data class XboxResponse(
    @SerialName("Token") val token: String,
    @SerialName("DisplayClaims") val displayClaims: DisplayClaims,
) {
    @Serializable data class DisplayClaims(val xui: List<Xui> = emptyList())
    @Serializable data class Xui(val uhs: String)
    val userHash: String? get() = displayClaims.xui.firstOrNull()?.uhs
}

/** XSTS 401 error body. */
@Serializable
data class XstsError(
    @SerialName("XErr") val xErr: Long = 0,
    @SerialName("Message") val message: String = "",
)

@Serializable
data class McLoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int = 86_400,
)

@Serializable
data class McProfile(val id: String, val name: String)

/** Thrown for user-actionable auth failures (shown in the UI). */
class AuthException(message: String) : Exception(message)

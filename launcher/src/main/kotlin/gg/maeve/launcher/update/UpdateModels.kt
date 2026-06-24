package gg.maeve.launcher.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GhRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val prerelease: Boolean = false,
    val draft: Boolean = false,
    val assets: List<GhAsset> = emptyList(),
)

@Serializable
data class GhAsset(
    val name: String,
    @SerialName("browser_download_url") val url: String,
    val size: Long = 0,
)

/** A newer release the launcher can install. */
data class UpdateInfo(
    val version: SemVer,
    val tag: String,
    val installerUrl: String,
    val installerName: String,
    val sha256SumsUrl: String?,
)

/** Update UI state. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data object UpToDate : UpdateState
    data class Working(val status: String) : UpdateState
    data class Error(val message: String) : UpdateState
}

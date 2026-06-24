package gg.maeve.launcher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import gg.maeve.launcher.auth.AuthConfig
import gg.maeve.launcher.auth.DeviceCodePrompt
import gg.maeve.launcher.auth.FileTokenStore
import gg.maeve.launcher.auth.KtorMsaTransport
import gg.maeve.launcher.auth.MsaDeviceCodeAuth
import gg.maeve.launcher.game.GameSession
import gg.maeve.launcher.game.Launcher
import gg.maeve.launcher.game.MaevePaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities

enum class Screen { SIGN_IN, HOME, SETTINGS, MODS }

/** UI state holder wrapping the existing auth + launch logic. All Compose-state writes
 *  are marshaled to the UI (EDT) thread. No provisioning/auth behavior changes here. */
class LauncherViewModel(private val scope: CoroutineScope) {
    private val account = "default"
    private val tokenStore = FileTokenStore(MaevePaths.default().root.resolve("auth"))

    var screen by mutableStateOf(Screen.SIGN_IN)
    var session by mutableStateOf<GameSession?>(null)

    // sign-in
    var signInBusy by mutableStateOf(false)
    var prompt by mutableStateOf<DeviceCodePrompt?>(null)
    var signInError by mutableStateOf<String?>(null)

    // play
    var playing by mutableStateOf(false)
    var playStatus by mutableStateOf("")
    var playFraction by mutableStateOf<Float?>(null)
    var playExit by mutableStateOf<String?>(null)
    val log = mutableStateListOf<String>()

    // settings + mods
    var maxMemoryMb by mutableStateOf(2048)
    val mods = mutableStateMapOf("sodium" to true, "lithium" to true)
    val dataDir: Path get() = MaevePaths.default().root

    private fun ui(block: () -> Unit) = SwingUtilities.invokeLater(block)

    /** Dev-only: shown when MAEVE_DEV=1 (or -Dmaeve.dev=true). NEVER enabled in public
     *  builds — playing without sign-in/ownership is against our server-legal stance. */
    val devMode: Boolean = System.getenv("MAEVE_DEV") == "1" ||
        System.getProperty("maeve.dev") == "true" ||
        java.nio.file.Files.exists(MaevePaths.default().root.resolve("dev.flag"))

    fun continueOffline() {
        session = GameSession.offline()
        screen = Screen.HOME
    }

    fun signIn() {
        val clientId = AuthConfig.clientId()
        if (clientId == null) { signInError = "Set MAEVE_AZURE_CLIENT_ID to your approved Azure app id."; return }
        signInBusy = true; signInError = null
        scope.launch(Dispatchers.IO) {
            runCatching {
                KtorMsaTransport().use { transport ->
                    val auth = MsaDeviceCodeAuth(clientId, transport)
                    val onPrompt: (DeviceCodePrompt) -> Unit = { p -> ui { prompt = p }; openBrowser(p.verificationUri) }
                    val stored = tokenStore.loadRefreshToken(account)
                    val result = if (stored != null) {
                        runCatching { auth.signInWithRefreshToken(stored) }.getOrElse { auth.signInWithDeviceCode(onPrompt) }
                    } else {
                        auth.signInWithDeviceCode(onPrompt)
                    }
                    result.refreshToken?.let { tokenStore.saveRefreshToken(account, it) }
                    ui { session = result.session; prompt = null; screen = Screen.HOME }
                }
            }.onFailure { e -> ui { signInError = e.message; prompt = null } }
            ui { signInBusy = false }
        }
    }

    fun play() {
        val s = session ?: return
        playing = true; playExit = null; playError = null; log.clear()
        val enabled = mods.filterValues { it }.keys.toSet()
        scope.launch(Dispatchers.IO) {
            runCatching {
                val proc = Launcher().launch(
                    session = s,
                    localMaeveMod = defaultMaeveMod(),
                    maxMemoryMb = maxMemoryMb,
                    enabledMods = enabled,
                    onStatus = { msg -> ui { playStatus = msg; playFraction = parseFraction(msg) } },
                    onLog = { line -> ui { log.add(line); if (log.size > 300) log.removeAt(0) } },
                )
                proc.waitFor()
                ui { playExit = "Game exited (code ${proc.exitValue()})"; playStatus = "" }
            }.onFailure { e -> ui { playError = e.message } }
            ui { playing = false; playFraction = null }
        }
    }

    var playError by mutableStateOf<String?>(null)

    private fun parseFraction(status: String): Float? {
        val m = Regex("""(\d+)\s*/\s*(\d+)""").find(status) ?: return null
        val (a, b) = m.destructured
        val total = b.toFloatOrNull() ?: return null
        return if (total > 0) (a.toFloatOrNull() ?: 0f) / total else null
    }

    private fun defaultMaeveMod(): Path? {
        val dir = Path.of("mod/build/libs")
        if (!Files.isDirectory(dir)) return null
        return Files.list(dir).use { st ->
            st.filter { val n = it.fileName.toString(); n.endsWith(".jar") && !n.contains("sources") }.findFirst().orElse(null)
        }
    }

    private fun openBrowser(uri: String) = runCatching {
        val u = URI(uri)
        require(u.scheme?.lowercase() in setOf("https", "http"))
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) Desktop.getDesktop().browse(u)
    }
}

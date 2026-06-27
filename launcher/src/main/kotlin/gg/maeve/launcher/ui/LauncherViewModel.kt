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
import gg.maeve.launcher.game.findDevModJar
import gg.maeve.launcher.update.BuildInfo
import gg.maeve.launcher.update.UpdateService
import gg.maeve.launcher.update.UpdateState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI
import java.nio.file.Path
import javax.swing.SwingUtilities

enum class Screen { SIGN_IN, HOME, MODS, COSMETICS, FRIENDS, SETTINGS }

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
    // launcher behavior (UI state; disk persistence is a tracked follow-up)
    var closeOnLaunch by mutableStateOf(true)
    var autoUpdate by mutableStateOf(true)
    var minimizeToTray by mutableStateOf(false)
    var showLogWindow by mutableStateOf(true)
    var logWindowOpen by mutableStateOf(false)
    val dataDir: Path get() = MaevePaths.default().root

    private fun ui(block: () -> Unit) = SwingUtilities.invokeLater(block)

    // --- self-update ---
    private val updater = UpdateService()
    val currentVersion: String get() = BuildInfo.version
    var update by mutableStateOf<UpdateState>(UpdateState.Idle)

    fun checkForUpdates() {
        update = UpdateState.Checking
        scope.launch(Dispatchers.IO) {
            runCatching { updater.check() }
                .onSuccess { info -> ui { update = info?.let { UpdateState.Available(it) } ?: UpdateState.UpToDate } }
                .onFailure { e -> ui { update = UpdateState.Error(e.message ?: "Update check failed") } }
        }
    }

    fun applyUpdate() {
        val info = (update as? UpdateState.Available)?.info ?: return
        scope.launch(Dispatchers.IO) {
            runCatching { updater.apply(info) { s -> ui { update = UpdateState.Working(s) } } }
                .onFailure { e -> ui { update = UpdateState.Error(e.message ?: "Update failed") } }
        }
    }

    /** Build-time only (BuildInfo.isDev, set by -Pmaeve.dev=true). Public releases build
     *  with dev=false, so the offline bypass is absent from the binary — no env var, file,
     *  or password can enable it. Playing without sign-in/ownership stays out of public builds. */
    val devMode: Boolean = BuildInfo.isDev

    fun continueOffline() {
        session = GameSession.offline()
        screen = Screen.HOME
    }

    fun signIn() {
        val clientId = AuthConfig.clientId()
        if (clientId == null) { signInError = "No Azure client ID configured."; return } // unreachable: AuthConfig has a baked-in default
        signInBusy = true; signInError = null
        signInJob = scope.launch(Dispatchers.IO) {
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
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e // user cancelled; UI already reset
                ui { signInError = e.message; prompt = null }
            }
            ui { signInBusy = false }
        }
    }

    private var signInJob: Job? = null

    /** Cancel a pending device-code sign-in and return to the idle sign-in state. */
    fun cancelSignIn() {
        signInJob?.cancel(); signInJob = null
        prompt = null; signInBusy = false; signInError = null
    }

    fun play() {
        val s = session ?: return
        playing = true; playExit = null; playError = null; log.clear()
        if (showLogWindow) logWindowOpen = true
        val enabled = mods.filterValues { it }.keys.toSet()
        scope.launch(Dispatchers.IO) {
            runCatching {
                val proc = Launcher().launch(
                    session = s,
                    // Identical in dev and public builds (decoupled from BuildInfo.isDev /
                    // auth state): use a freshly built mod/build/libs jar when present — only
                    // happens when run from the repo — otherwise the bundled resource that
                    // ModProvisioner extracts (bundled-mods/maeve.jar). Every installed build
                    // ships and installs the mod regardless of which workflow produced it.
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

    private fun defaultMaeveMod(): Path? = findDevModJar(Path.of("mod/build/libs"))

    private fun openBrowser(uri: String) = runCatching {
        val u = URI(uri)
        require(u.scheme?.lowercase() in setOf("https", "http"))
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) Desktop.getDesktop().browse(u)
    }
}

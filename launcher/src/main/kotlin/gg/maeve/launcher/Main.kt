package gg.maeve.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import gg.maeve.launcher.auth.AuthConfig
import gg.maeve.launcher.auth.DeviceCodePrompt
import gg.maeve.launcher.auth.FileTokenStore
import gg.maeve.launcher.auth.KtorMsaTransport
import gg.maeve.launcher.auth.MsaDeviceCodeAuth
import gg.maeve.launcher.game.GameSession
import gg.maeve.launcher.game.Launcher
import gg.maeve.launcher.game.MaevePaths
import gg.maeve.shared.Versions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities

private const val ACCOUNT = "default"

private fun defaultMaeveMod(): Path? {
    val dir = Path.of("mod/build/libs")
    if (!Files.isDirectory(dir)) return null
    return Files.list(dir).use { s ->
        s.filter { val n = it.fileName.toString(); n.endsWith(".jar") && !n.contains("sources") }.findFirst().orElse(null)
    }
}

private fun openBrowser(uri: String) = runCatching {
    val u = URI(uri)
    require(u.scheme?.lowercase() in setOf("https", "http")) { "unexpected URI scheme: ${u.scheme}" }
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(u)
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Maeve",
        state = rememberWindowState(width = 1000.dp, height = 640.dp),
    ) {
        MaterialTheme { App() }
    }
}

@Composable
private fun App() {
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<GameSession?>(null) }
    var status by remember { mutableStateOf("Not signed in") }
    var prompt by remember { mutableStateOf<DeviceCodePrompt?>(null) }
    var busy by remember { mutableStateOf(false) }
    val log = remember { mutableStateListOf<String>() }

    fun ui(block: () -> Unit) = SwingUtilities.invokeLater(block)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Maeve", style = MaterialTheme.typography.headlineMedium)
        Text("Target: Minecraft ${Versions.MINECRAFT} (Fabric ${Versions.FABRIC_LOADER})")
        Text(status, style = MaterialTheme.typography.bodyMedium)

        prompt?.let {
            Text(
                "Sign in: open ${it.verificationUri} and enter code  ${it.userCode}",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(enabled = !busy, onClick = {
                val clientId = AuthConfig.clientId()
                if (clientId == null) {
                    status = "Set MAEVE_AZURE_CLIENT_ID to your approved Azure app id, then retry."
                    return@Button
                }
                busy = true
                scope.launch(Dispatchers.IO) {
                    val tokenStore = FileTokenStore(MaevePaths.default().root.resolve("auth"))
                    runCatching {
                        KtorMsaTransport().use { transport ->
                            val auth = MsaDeviceCodeAuth(clientId, transport)
                            val onPrompt: (DeviceCodePrompt) -> Unit = { p ->
                                ui { prompt = p; status = "Waiting for sign-in…" }
                                openBrowser(p.verificationUri)
                            }
                            val stored = tokenStore.loadRefreshToken(ACCOUNT)
                            val result = if (stored != null) {
                                runCatching { auth.signInWithRefreshToken(stored) }
                                    .getOrElse { auth.signInWithDeviceCode(onPrompt) }
                            } else {
                                auth.signInWithDeviceCode(onPrompt)
                            }
                            ui {
                                session = result.session
                                prompt = null
                                status = "Signed in: ${result.session.username}"
                            }
                            result.refreshToken?.let { tokenStore.saveRefreshToken(ACCOUNT, it) }
                        }
                    }.onFailure { e -> ui { prompt = null; status = "Sign-in failed: ${e.message}" } }
                    ui { busy = false }
                }
            }) {
                Text(session?.let { "Account: ${it.username}" } ?: "Sign in with Microsoft")
            }

            Button(
                enabled = session != null && !busy,
                onClick = {
                    val s = session ?: return@Button
                    busy = true; log.clear()
                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            val proc = Launcher().launch(
                                session = s,
                                localMaeveMod = defaultMaeveMod(),
                                onStatus = { msg -> SwingUtilities.invokeLater { status = msg } },
                                onLog = { line -> SwingUtilities.invokeLater { log.add(line); if (log.size > 300) log.removeAt(0) } },
                            )
                            proc.waitFor()
                            SwingUtilities.invokeLater { status = "Game exited (code ${proc.exitValue()})" }
                        }.onFailure { e -> SwingUtilities.invokeLater { status = "Error: ${e.message}" } }
                        SwingUtilities.invokeLater { busy = false }
                    }
                },
            ) {
                Text(if (busy) "Working…" else "Launch")
            }
        }

        if (log.isNotEmpty()) {
            Text(
                log.takeLast(60).joinToString("\n"),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            )
        }
    }
}

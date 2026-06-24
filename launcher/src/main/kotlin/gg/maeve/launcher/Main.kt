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
import gg.maeve.launcher.game.GameSession
import gg.maeve.launcher.game.Launcher
import gg.maeve.shared.Versions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities

/** Dev convenience: find the locally-built Maeve mod jar to bundle when launching
 *  from the repo (gradle run). In a packaged build this returns null until the mod
 *  is bundled into the distribution. */
private fun defaultMaeveMod(): Path? {
    val dir = Path.of("mod/build/libs")
    if (!Files.isDirectory(dir)) return null
    return Files.list(dir).use { stream ->
        stream.filter {
            val n = it.fileName.toString()
            n.endsWith(".jar") && !n.contains("sources")
        }.findFirst().orElse(null)
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
    val scope = rememberCoroutineScope() // tied to composition; cancelled on window close
    var session by remember { mutableStateOf<GameSession?>(null) }
    var status by remember { mutableStateOf("Not signed in") }
    var launching by remember { mutableStateOf(false) }
    val log = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Maeve", style = MaterialTheme.typography.headlineMedium)
        Text("Target: Minecraft ${Versions.MINECRAFT} (Fabric ${Versions.FABRIC_LOADER})")
        Text(status, style = MaterialTheme.typography.bodyMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val s = GameSession.offline()
                session = s
                status = "Signed in (offline stub): ${s.username} — real Microsoft auth coming"
            }) {
                Text(session?.let { "Account: ${it.username}" } ?: "Sign in with Microsoft")
            }
            Button(
                enabled = session != null && !launching,
                onClick = {
                    val s = session ?: return@Button
                    launching = true
                    log.clear()
                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            val proc = Launcher().launch(
                                session = s,
                                localMaeveMod = defaultMaeveMod(),
                                onStatus = { s -> SwingUtilities.invokeLater { status = s } },
                                onLog = { line -> SwingUtilities.invokeLater { log.add(line); if (log.size > 300) log.removeAt(0) } },
                            )
                            proc.waitFor()
                            SwingUtilities.invokeLater { status = "Game exited (code ${proc.exitValue()})" }
                        }.onFailure { e -> SwingUtilities.invokeLater { status = "Error: ${e.message}" } }
                        SwingUtilities.invokeLater { launching = false }
                    }
                },
            ) {
                Text(if (launching) "Working…" else "Launch")
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

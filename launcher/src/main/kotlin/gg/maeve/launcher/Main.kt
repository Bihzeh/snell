package gg.maeve.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import gg.maeve.shared.Versions

/**
 * Maeve launcher entrypoint. Phase 1 scope: Microsoft device-code login, then
 * provision + launch MC 26.1 + Fabric + the Maeve mod + bundled perf mods.
 * This is a minimal shell; auth and provisioning wiring lands in Phase 1.
 */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Maeve") {
        MaterialTheme {
            var status by remember { mutableStateOf("Not signed in") }
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Maeve", style = MaterialTheme.typography.headlineMedium)
                Text("Target: Minecraft ${Versions.MINECRAFT} (Fabric ${Versions.FABRIC_LOADER})")
                Text(status)
                Button(onClick = { status = "Device-code login starts here (Phase 1)" }) {
                    Text("Sign in with Microsoft")
                }
                Button(enabled = false, onClick = {}) { Text("Launch") }
            }
        }
    }
}

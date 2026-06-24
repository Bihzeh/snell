package gg.maeve.launcher.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gg.maeve.launcher.ui.LauncherViewModel
import gg.maeve.launcher.ui.components.ButtonVariant
import gg.maeve.launcher.ui.components.MaeveButton
import gg.maeve.launcher.ui.components.MaeveCard
import gg.maeve.launcher.ui.theme.Maeve
import gg.maeve.launcher.update.UpdateState

@Composable
fun SettingsScreen(vm: LauncherViewModel) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        MaeveCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("About", style = MaterialTheme.typography.titleMedium)
                Text("Maeve ${vm.currentVersion}", color = Maeve.text2, style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MaeveButton("Check for updates", { vm.checkForUpdates() }, variant = ButtonVariant.Secondary)
                    val msg = when (val u = vm.update) {
                        is UpdateState.Checking -> "Checking…"
                        is UpdateState.UpToDate -> "Up to date"
                        is UpdateState.Available -> "Update available — ${u.info.tag}"
                        is UpdateState.Working -> u.status
                        is UpdateState.Error -> u.message
                        else -> ""
                    }
                    if (msg.isNotEmpty()) Text(msg, color = Maeve.text2, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        MaeveCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Memory", style = MaterialTheme.typography.titleMedium)
                Text("%.1f GB allocated".format(vm.maxMemoryMb / 1024.0), color = Maeve.text2, style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = vm.maxMemoryMb.toFloat(),
                    onValueChange = { vm.maxMemoryMb = (it / 512).toInt() * 512 },
                    valueRange = 1024f..8192f,
                    steps = 13,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = Maeve.elevated2),
                )
            }
        }
        MaeveCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Game directory", style = MaterialTheme.typography.titleMedium)
                Text(vm.dataDir.toString(), color = Maeve.text2, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

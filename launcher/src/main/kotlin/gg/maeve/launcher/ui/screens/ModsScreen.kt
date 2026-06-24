package gg.maeve.launcher.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gg.maeve.launcher.ui.LauncherViewModel
import gg.maeve.launcher.ui.components.ModRow
import gg.maeve.launcher.ui.theme.Maeve

@Composable
fun ModsScreen(vm: LauncherViewModel) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Performance mods", style = MaterialTheme.typography.headlineMedium)
        ModRow("Sodium", "Rendering engine replacement — large FPS gains.", vm.mods["sodium"] == true, onToggle = { vm.mods["sodium"] = it })
        ModRow("Lithium", "Server-side tick & game-logic optimizations.", vm.mods["lithium"] == true, onToggle = { vm.mods["lithium"] = it })
        Text("Fabric API and Fabric Language Kotlin are always included.", color = Maeve.text3, style = MaterialTheme.typography.bodySmall)
    }
}

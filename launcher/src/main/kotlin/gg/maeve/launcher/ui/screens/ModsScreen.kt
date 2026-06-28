package gg.maeve.launcher.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gg.maeve.launcher.ui.LauncherViewModel
import gg.maeve.launcher.ui.components.ModRow
import gg.maeve.launcher.ui.components.PillKind
import gg.maeve.launcher.ui.components.SectionLabel
import gg.maeve.launcher.ui.components.StatusPill
import gg.maeve.launcher.ui.theme.Maeve

@Composable
fun ModsScreen(vm: LauncherViewModel) {
    LaunchedEffect(Unit) { vm.loadModIcons() }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Mods & Performance")
        Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Bundled performance mods")
                Spacer(Modifier.weight(1f))
                StatusPill("Server-legal", PillKind.UpToDate, showDot = false)
            }
            Text(
                "These ship with Maeve and run on any server — pure performance, no gameplay advantage.",
                style = MaterialTheme.typography.bodySmall, color = Maeve.text2,
            )
            Spacer(Modifier.height(2.dp))
            ModRow(
                icon = "bolt", name = "Sodium", meta = "Rendering",
                description = "Rewrites Minecraft's renderer for big FPS gains and smoother frame pacing.",
                statusLabel = "Active", statusKind = PillKind.Online,
                logo = vm.modIcons["sodium"],
                enabled = vm.mods["sodium"] ?: true, onToggle = { vm.mods["sodium"] = it },
            )
            ModRow(
                icon = "speed", name = "Lithium", meta = "Server tick",
                description = "Optimizes physics, mob AI and tick scheduling — identical behavior, less CPU.",
                statusLabel = "Active", statusKind = PillKind.Online,
                logo = vm.modIcons["lithium"],
                enabled = vm.mods["lithium"] ?: true, onToggle = { vm.mods["lithium"] = it },
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "More server-legal modules (zoom, toggle-sprint, FPS readouts) are added over time.",
                style = MaterialTheme.typography.labelMedium, color = Maeve.text3,
            )
        }
    }
}

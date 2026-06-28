package gg.snell.launcher.ui.screens

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
import gg.snell.launcher.ui.LauncherViewModel
import gg.snell.launcher.ui.components.ModRow
import gg.snell.launcher.ui.components.PillKind
import gg.snell.launcher.ui.components.SectionLabel
import gg.snell.launcher.ui.components.StatusPill
import gg.snell.launcher.ui.theme.Snell

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
                "These ship with Snell and run on any server — pure performance, no gameplay advantage.",
                style = MaterialTheme.typography.bodySmall, color = Snell.text2,
            )
            Spacer(Modifier.height(2.dp))
            // meta (tag) + description come live from Modrinth once loaded; the hardcoded
            // strings are the offline/first-paint fallback before the fetch resolves.
            val sodium = vm.modInfo["sodium"]
            ModRow(
                icon = "bolt", name = "Sodium",
                meta = sodium?.category?.takeIf { it.isNotBlank() } ?: "Rendering",
                description = sodium?.description?.takeIf { it.isNotBlank() }
                    ?: "Rewrites Minecraft's renderer for big FPS gains and smoother frame pacing.",
                statusLabel = "Active", statusKind = PillKind.Online,
                logo = vm.modIcons["sodium"],
                enabled = vm.mods["sodium"] ?: true, onToggle = { vm.mods["sodium"] = it },
            )
            val lithium = vm.modInfo["lithium"]
            ModRow(
                icon = "speed", name = "Lithium",
                meta = lithium?.category?.takeIf { it.isNotBlank() } ?: "Server tick",
                description = lithium?.description?.takeIf { it.isNotBlank() }
                    ?: "Optimizes physics, mob AI and tick scheduling — identical behavior, less CPU.",
                statusLabel = "Active", statusKind = PillKind.Online,
                logo = vm.modIcons["lithium"],
                enabled = vm.mods["lithium"] ?: true, onToggle = { vm.mods["lithium"] = it },
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "More server-legal modules (zoom, toggle-sprint, FPS readouts) are added over time.",
                style = MaterialTheme.typography.labelMedium, color = Snell.text3,
            )
        }
    }
}

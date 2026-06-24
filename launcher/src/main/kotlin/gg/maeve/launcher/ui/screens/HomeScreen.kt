package gg.maeve.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gg.maeve.launcher.ui.LauncherViewModel
import gg.maeve.launcher.ui.components.ButtonVariant
import gg.maeve.launcher.ui.components.MaeveButton
import gg.maeve.launcher.ui.components.MaeveCard
import gg.maeve.launcher.ui.components.MaeveProgress
import gg.maeve.launcher.ui.components.PillKind
import gg.maeve.launcher.ui.components.PlayButton
import gg.maeve.launcher.ui.components.Spinner
import gg.maeve.launcher.ui.components.StatusPill
import gg.maeve.launcher.ui.theme.Maeve
import gg.maeve.launcher.ui.theme.MaeveFonts
import gg.maeve.shared.Versions

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Top: version + account
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Chip("Minecraft ${Versions.MINECRAFT} · Fabric")
            Spacer(Modifier.weight(1f))
            Chip(vm.session?.username ?: "—")
        }

        // Middle: featured + what's new
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MaeveCard(Modifier.weight(1.7f).fillMaxSize()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("FEATURED", color = Maeve.gold, letterSpacing = 2.sp, style = MaterialTheme.typography.labelSmall)
                    Text("Minecraft ${Versions.MINECRAFT}, tuned.", fontFamily = MaeveFonts.Marcellus, fontSize = 34.sp, color = MaterialTheme.colorScheme.onBackground)
                    Text("Sodium and Lithium bundled, an in-game HUD, and a launcher that stays out of your way.", color = Maeve.text2, style = MaterialTheme.typography.bodyMedium)
                    MaeveButton("Read the notes", { }, variant = ButtonVariant.Secondary)
                }
            }
            MaeveCard(Modifier.weight(1f).fillMaxSize()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("What's new", style = MaterialTheme.typography.titleMedium)
                    Text("Sodium 0.9 rendering", color = Maeve.text2, style = MaterialTheme.typography.bodySmall)
                    Text("Keystroke + FPS HUD", color = Maeve.text2, style = MaterialTheme.typography.bodySmall)
                    Text("Lithium tick budget", color = Maeve.text2, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.weight(1f))
                    Text("Updated recently", color = Maeve.text3, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Bottom bar
        MaeveCard(Modifier.fillMaxWidth()) {
            if (vm.playing) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Spinner(18)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(vm.playStatus.ifEmpty { "Working…" }, style = MaterialTheme.typography.bodyMedium)
                        MaeveProgress(vm.playFraction)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val exit = vm.playExit
                    val err = vm.playError
                    when {
                        err != null -> StatusPill(err.take(40), PillKind.Failed)
                        exit != null -> StatusPill(exit, PillKind.Neutral)
                        else -> StatusPill("Ready to play", PillKind.UpToDate)
                    }
                    Spacer(Modifier.weight(1f))
                    PlayButton(enabled = vm.session != null && !vm.playing, onClick = vm::play)
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String) {
    Box(Modifier.border(1.dp, Maeve.border, RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 9.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = Maeve.text2, fontWeight = FontWeight.Medium)
    }
}

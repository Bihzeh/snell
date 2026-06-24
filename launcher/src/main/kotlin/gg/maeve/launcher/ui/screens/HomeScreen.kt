package gg.maeve.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gg.maeve.launcher.ui.LauncherViewModel
import gg.maeve.launcher.ui.components.ButtonVariant
import gg.maeve.launcher.ui.components.Carousel
import gg.maeve.launcher.ui.components.MaeveButton
import gg.maeve.launcher.ui.components.MaeveCard
import gg.maeve.launcher.ui.components.MaeveProgress
import gg.maeve.launcher.ui.components.PillKind
import gg.maeve.launcher.ui.components.PlayButton
import gg.maeve.launcher.ui.components.Spinner
import gg.maeve.launcher.ui.components.StatusPill
import gg.maeve.launcher.ui.components.VersionRender
import gg.maeve.launcher.ui.components.promoSlides
import gg.maeve.launcher.ui.theme.Maeve
import gg.maeve.launcher.ui.theme.MaeveFonts
import gg.maeve.launcher.update.UpdateState
import gg.maeve.shared.Versions

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        UpdateBanner(vm)
        // Top: version + account
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Chip("Minecraft ${Versions.MINECRAFT} · Fabric")
            Spacer(Modifier.weight(1f))
            Chip(vm.session?.username ?: "—")
        }

        // Carousel — full-width, inline with the cards (future ads / promoted servers / news)
        Carousel(promoSlides())

        // Featured (with version render) + What's new
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Featured card: render panel on the left, marketing copy on the right.
            Box(
                Modifier.weight(1.7f).fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, Maeve.borderSoft, RoundedCornerShape(14.dp)),
            ) {
                Row(Modifier.fillMaxSize()) {
                    VersionRender(Versions.MINECRAFT, Modifier.weight(1f).fillMaxHeight())
                    Column(Modifier.weight(1f).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("FEATURED", color = Maeve.gold, letterSpacing = 2.sp, style = MaterialTheme.typography.labelSmall)
                        Text("Minecraft ${Versions.MINECRAFT}, tuned.", fontFamily = MaeveFonts.Marcellus, fontSize = 28.sp, color = MaterialTheme.colorScheme.onBackground)
                        Text("Sodium and Lithium bundled, an in-game HUD, and a launcher that stays out of your way.", color = Maeve.text2, style = MaterialTheme.typography.bodyMedium)
                        MaeveButton("Read the notes", { }, variant = ButtonVariant.Secondary)
                    }
                }
            }
            MaeveCard(Modifier.weight(1f).fillMaxHeight()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("What's new", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium)
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
                        Text(vm.playStatus.ifEmpty { "Working…" }, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
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
private fun UpdateBanner(vm: LauncherViewModel) {
    val u = vm.update
    when (u) {
        is UpdateState.Available -> Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Maeve.goldSubtle)
                .border(1.dp, Maeve.gold.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Update available — ${u.info.tag}", color = Maeve.gold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            MaeveButton("Update now", { vm.applyUpdate() })
        }
        is UpdateState.Working -> Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface)
                .border(1.dp, Maeve.borderSoft, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spinner(16); Text(u.status, color = Maeve.text2, style = MaterialTheme.typography.bodyMedium)
        }
        else -> {}
    }
}

@Composable
private fun Chip(text: String) {
    Box(Modifier.border(1.dp, Maeve.border, RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 9.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = Maeve.text2, fontWeight = FontWeight.Medium)
    }
}

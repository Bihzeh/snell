package gg.maeve.launcher.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import gg.maeve.launcher.ui.LauncherViewModel
import gg.maeve.launcher.ui.components.MaeveButton
import gg.maeve.launcher.ui.components.MaeveCard
import gg.maeve.launcher.ui.components.MaeveProgress
import gg.maeve.launcher.ui.components.MaeveSelectDisplay
import gg.maeve.launcher.ui.components.PillKind
import gg.maeve.launcher.ui.components.PlayButton
import gg.maeve.launcher.ui.components.SectionLabel
import gg.maeve.launcher.ui.components.Spinner
import gg.maeve.launcher.ui.components.StatusPill
import gg.maeve.launcher.ui.components.SymIcon
import gg.maeve.launcher.ui.components.VersionRender
import gg.maeve.launcher.ui.theme.Maeve
import gg.maeve.launcher.update.UpdateState
import gg.maeve.shared.Versions

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        UpdateBanner(vm)
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Hero(vm, Modifier.weight(1.9f).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                WhatsNew()
                ComingSoonSlot(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Hero(vm: LauncherViewModel, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).border(1.dp, Maeve.border, RoundedCornerShape(14.dp))) {
        // Art background (no crown overlay — the player skin is the subject here).
        VersionRender(Versions.MINECRAFT, Modifier.fillMaxSize(), showOverlay = false)
        // Status badge top-left.
        Row(Modifier.align(Alignment.TopStart).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SymIcon("verified", 16.dp, Maeve.accentHi)
            Text("Up to date · Fabric", color = Maeve.text2, style = MaterialTheme.typography.labelMedium)
        }
        // Player skin render, centered in the upper area.
        Image(
            painterResource("skin/player.png"),
            contentDescription = "Player skin",
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp).fillMaxHeight(0.62f),
            contentScale = ContentScale.Fit,
        )
        // Legibility scrim under the player card.
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(240.dp)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Maeve.bg2.copy(alpha = 0.94f)))))
        // Player card controls.
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (vm.playing) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Maeve.s1.copy(alpha = 0.88f)).border(1.dp, Maeve.border, RoundedCornerShape(12.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Spinner(16)
                        Text(vm.playStatus.ifEmpty { "Preparing…" }, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                    }
                    MaeveProgress(vm.playFraction)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(vm.session?.username ?: "Player", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    SymIcon("edit", 16.dp, Maeve.text2)
                }
                MaeveSelectDisplay("Default profile · ${Versions.MINECRAFT}", leadingIcon = "tune")
                PlayButton(enabled = vm.session != null, label = "Launch", onClick = vm::play)
                val exit = vm.playExit; val err = vm.playError
                when {
                    err != null -> StatusPill(err.take(48), PillKind.Failed)
                    exit != null -> StatusPill(exit, PillKind.Neutral)
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun WhatsNew() {
    MaeveCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("What's new")
            NewsLine("speed", "Sodium + Lithium bundled")
            NewsLine("dashboard", "FPS / coords / keystroke HUD")
            NewsLine("tune", "Fully customizable in-game HUD")
            Spacer(Modifier.height(2.dp))
            Text("Maeve ${buildVersion()}", color = Maeve.text3, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun NewsLine(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SymIcon(icon, 18.dp, Maeve.accentHi)
        Text(text, color = Maeve.text2, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ComingSoonSlot(modifier: Modifier) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Maeve.s1).border(1.dp, Maeve.border, RoundedCornerShape(14.dp)).padding(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Friends")
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SymIcon("group", 22.dp, Maeve.text3)
                Text("Parties & social — coming soon", color = Maeve.text3, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun UpdateBanner(vm: LauncherViewModel) {
    when (val u = vm.update) {
        is UpdateState.Available -> Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Maeve.ember.copy(alpha = 0.10f))
                .border(1.dp, Maeve.ember.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SymIcon("download", 18.dp, Maeve.ember)
            Text("Update available — ${u.info.tag}", color = Maeve.ember, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            MaeveButton("Update now", { vm.applyUpdate() })
        }
        is UpdateState.Working -> Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Maeve.s1)
                .border(1.dp, Maeve.border, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spinner(16); Text(u.status, color = Maeve.text2, style = MaterialTheme.typography.bodyMedium)
        }
        else -> {}
    }
}

private fun buildVersion(): String = gg.maeve.launcher.update.BuildInfo.version

package gg.snell.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gg.snell.launcher.ui.LauncherViewModel
import gg.snell.launcher.ui.components.DynamicSky
import gg.snell.launcher.ui.components.SnellButton
import gg.snell.launcher.ui.components.SnellCard
import gg.snell.launcher.ui.components.SnellProgress
import gg.snell.launcher.ui.components.NameTag
import gg.snell.launcher.ui.components.PillKind
import gg.snell.launcher.ui.components.RotatableSkin
import gg.snell.launcher.ui.components.SectionLabel
import gg.snell.launcher.ui.components.Spinner
import gg.snell.launcher.ui.components.StatusPill
import gg.snell.launcher.ui.components.SymIcon
import gg.snell.launcher.ui.theme.Snell
import gg.snell.launcher.ui.theme.SnellFonts
import gg.snell.launcher.update.UpdateState
import gg.snell.shared.Versions
import kotlin.math.roundToInt

private val LaunchAllShape = RoundedCornerShape(14.dp)
private val LaunchTopShape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
private val LaunchBotShape = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp, topStart = 0.dp, topEnd = 0.dp)

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        UpdateBanner(vm)
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Left: promoted-server ads row, then the launch card (per Claude Design).
            Column(Modifier.weight(1.9f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    AdCard("Hypixel Network", "mc.hypixel.net", "Ranked Bedwars, SkyBlock & 30+ minigames.", Modifier.weight(1f).fillMaxHeight())
                    AdCard("CubeCraft Games", "play.cubecraft.net", "Lucky Islands, EggWars & weekly events.", Modifier.weight(1f).fillMaxHeight())
                }
                LaunchCard(vm, Modifier.weight(1f).fillMaxWidth())
            }
            // Right rail.
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                WhatsNew()
                ComingSoonSlot(Modifier.weight(1f))
            }
        }
    }
}

/** The launch card: Minecraft backdrop filling the whole card + 50% darken + skin + LAUNCH. */
@Composable
private fun LaunchCard(vm: LauncherViewModel, modifier: Modifier) {
    Box(modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).border(1.dp, Snell.border, RoundedCornerShape(14.dp))) {
        // Dynamic day/night Minecraft sky (follows local time).
        DynamicSky(Modifier.fillMaxSize())
        // Subtle bottom scrim for the launch bar.
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(150.dp)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)))))
        // Skin with the Minecraft nametag directly above its head.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val skinH = maxHeight * 0.78f
            Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                NameTag(vm.session?.username ?: "Player")
                Spacer(Modifier.height(6.dp))
                RotatableSkin(frameCount = 24, modifier = Modifier.height(skinH).aspectRatio(360f / 464f))
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
            if (!vm.playing) {
                val err = vm.playError; val exit = vm.playExit
                when {
                    err != null -> { StatusPill(err.take(48), PillKind.Failed); Spacer(Modifier.height(8.dp)) }
                    exit != null -> { StatusPill(exit, PillKind.Neutral); Spacer(Modifier.height(8.dp)) }
                    else -> {}
                }
            }
            LaunchBar(vm, shape = if (vm.playing) LaunchTopShape else LaunchAllShape)
            if (vm.playing) ProgressStrip(vm)
        }
    }
}

/** Promoted-server card (frame 03): Name / IP / MOTD, a standalone card above the launch card. */
@Composable
private fun AdCard(name: String, ip: String, motd: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(Snell.s1).border(1.dp, Snell.border, RoundedCornerShape(14.dp))) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Snell.ka1, Snell.ka2))))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Transparent, Snell.accent.copy(alpha = 0.12f)))))
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SymIcon("verified", 13.dp, Snell.ember)
                Spacer(Modifier.width(5.dp))
                Text("PROMOTED SERVER", color = Snell.ember, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                JoinButton()
            }
            Spacer(Modifier.weight(1f))
            Text(name, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(ip, color = Snell.accentHi, fontFamily = SnellFonts.Mono, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(motd, color = Snell.text2, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun JoinButton() {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(Snell.accent.copy(alpha = 0.18f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { /* join — follow-up */ }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        SymIcon("bolt", 15.dp, Snell.accentHi)
        Text("Join", color = Snell.accentHi, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

/** The split launch button: LAUNCH action + a version/profile dropdown affordance. */
@Composable
private fun LaunchBar(vm: LauncherViewModel, shape: Shape = LaunchAllShape) {
    val enabled = vm.session != null && !vm.playing
    Row(
        Modifier.fillMaxWidth().height(72.dp)
            .then(if (enabled) Modifier.shadow(16.dp, shape, ambientColor = Snell.accent, spotColor = Snell.accent) else Modifier)
            .clip(shape).background(Brush.horizontalGradient(listOf(Snell.accentHi, Snell.accent))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).fillMaxHeight()
                .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.play() }
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SymIcon("play_arrow", 30.dp, Snell.onAccent)
            Column {
                Text("LAUNCH", fontFamily = SnellFonts.Display, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 1.sp, color = Snell.onAccent)
                Text("Snell Client ${Versions.MINECRAFT}", color = Snell.onAccent.copy(alpha = 0.72f), style = MaterialTheme.typography.labelMedium)
            }
        }
        Box(Modifier.width(1.dp).height(40.dp).background(Snell.onAccent.copy(alpha = 0.25f)))
        Box(
            Modifier.fillMaxHeight()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { /* profile/version dropdown — follow-up */ }
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center,
        ) { SymIcon("expand_more", 26.dp, Snell.onAccent) }
    }
}

/** Loading-status drawer attached under the launch button while launching (darker shade). */
@Composable
private fun ProgressStrip(vm: LauncherViewModel) {
    Column(
        Modifier.fillMaxWidth().clip(LaunchBotShape).background(Color(0xFF06303A)).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(vm.playStatus.ifEmpty { "Preparing\u2026" }, color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            vm.playFraction?.let { Text("${(it.coerceIn(0f, 1f) * 100).roundToInt()}%", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall) }
        }
        SnellProgress(vm.playFraction)
    }
}

@Composable
private fun WhatsNew() {
    SnellCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("What's new")
            NewsLine("speed", "Sodium + Lithium bundled")
            NewsLine("dashboard", "FPS / coords / keystroke HUD")
            NewsLine("tune", "Fully customizable in-game HUD")
            Spacer(Modifier.height(2.dp))
            Text("Snell ${buildVersion()}", color = Snell.text3, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun NewsLine(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SymIcon(icon, 18.dp, Snell.accentHi)
        Text(text, color = Snell.text2, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ComingSoonSlot(modifier: Modifier) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Snell.s1).border(1.dp, Snell.border, RoundedCornerShape(14.dp)).padding(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Friends")
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SymIcon("group", 22.dp, Snell.text3)
                Text("Parties & social — coming soon", color = Snell.text3, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun UpdateBanner(vm: LauncherViewModel) {
    when (val u = vm.update) {
        is UpdateState.Available -> Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Snell.ember.copy(alpha = 0.10f))
                .border(1.dp, Snell.ember.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SymIcon("download", 18.dp, Snell.ember)
            Text("Update available — ${u.info.tag}", color = Snell.ember, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            SnellButton("Update now", { vm.applyUpdate() })
        }
        is UpdateState.Working -> Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Snell.s1)
                .border(1.dp, Snell.border, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spinner(16); Text(u.status, color = Snell.text2, style = MaterialTheme.typography.bodyMedium)
        }
        else -> {}
    }
}

private fun buildVersion(): String = gg.snell.launcher.update.BuildInfo.version

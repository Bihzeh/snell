package gg.snell.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import gg.snell.launcher.ui.LauncherViewModel
import gg.snell.launcher.ui.components.ButtonVariant
import gg.snell.launcher.ui.components.SnellButton
import gg.snell.launcher.ui.components.SnellSelectDisplay
import gg.snell.launcher.ui.components.SnellSwitch
import gg.snell.launcher.ui.components.SectionLabel
import gg.snell.launcher.ui.components.SymIcon
import gg.snell.launcher.ui.theme.Snell
import gg.snell.launcher.update.UpdateState
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: LauncherViewModel) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Settings")
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionLabel("Game & Performance")
            Card {
                // RAM
                RowPad(divider = true) {
                    Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Memory allocation", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("RAM reserved for Minecraft", style = MaterialTheme.typography.bodySmall, color = Snell.text3)
                            }
                            Text("${vm.maxMemoryMb / 1024} GB", style = MaterialTheme.typography.headlineSmall, color = Snell.accentHi)
                        }
                        Slider(
                            value = vm.maxMemoryMb.toFloat(),
                            onValueChange = { vm.maxMemoryMb = ((it / 512f).roundToInt() * 512).coerceIn(2048, 16384) },
                            valueRange = 2048f..16384f,
                            colors = SliderDefaults.colors(thumbColor = Snell.accent, activeTrackColor = Snell.accent, inactiveTrackColor = Snell.s2),
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("2 GB", style = MaterialTheme.typography.labelSmall, color = Snell.text3)
                            Text("Recommended 6–8 GB", style = MaterialTheme.typography.labelSmall, color = Snell.text3)
                            Text("16 GB", style = MaterialTheme.typography.labelSmall, color = Snell.text3)
                        }
                    }
                }
                // Java
                RowPad(divider = true) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Java runtime", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text("Snell manages this automatically", style = MaterialTheme.typography.bodySmall, color = Snell.text3)
                        }
                        SnellSelectDisplay("Bundled · Temurin 25")
                    }
                }
                // Game dir
                RowPad(divider = false) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Game directory", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(vm.dataDir.toString(), style = MaterialTheme.typography.bodySmall, color = Snell.text3)
                        }
                        SnellButton("Change", { /* directory picker — follow-up */ }, variant = ButtonVariant.Secondary, leadingIcon = "folder_open")
                    }
                }
            }

            SectionLabel("Launcher behavior")
            Card {
                ToggleRow("Close launcher when the game starts", vm.closeOnLaunch) { vm.closeOnLaunch = it }
                ToggleRow("Keep Snell up to date automatically", vm.autoUpdate) { vm.autoUpdate = it }
                ToggleRow("Minimize to system tray", vm.minimizeToTray) { vm.minimizeToTray = it }
                ToggleRow("Show live game logs on launch", vm.showLogWindow) { vm.showLogWindow = it }
            }

            SectionLabel("About")
            Card {
                RowPad(divider = false) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Snell ${vm.currentVersion}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(updateText(vm.update), style = MaterialTheme.typography.bodySmall, color = Snell.text3)
                        }
                        SnellButton("Check for updates", { vm.checkForUpdates() }, variant = ButtonVariant.Secondary, leadingIcon = "refresh")
                    }
                }
            }

            // Advanced (collapsed)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Snell.s1)
                    .border(1.dp, Snell.border, RoundedCornerShape(14.dp)).padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SymIcon("code", 20.dp, Snell.text2)
                Column(Modifier.weight(1f)) {
                    Text("Advanced", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("JVM arguments, environment, experimental flags", style = MaterialTheme.typography.bodySmall, color = Snell.text3)
                }
                SymIcon("chevron_right", 22.dp, Snell.text3)
            }
        }
    }
}

private fun updateText(u: UpdateState): String = when (u) {
    is UpdateState.Checking -> "Checking…"
    is UpdateState.UpToDate -> "You're on the latest version"
    is UpdateState.Available -> "Update ${u.info.tag} available"
    is UpdateState.Working -> u.status
    is UpdateState.Error -> u.message
    else -> "Up to date"
}

@Composable
internal fun ScreenHeader(title: String) {
    Box(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 28.dp), contentAlignment = Alignment.CenterStart) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Snell.s1).border(1.dp, Snell.border, RoundedCornerShape(14.dp)).padding(horizontal = 20.dp), content = content)
}

@Composable
private fun RowPad(divider: Boolean, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) { content() }
    if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(Snell.s2))
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            SnellSwitch(checked, onChange)
        }
    }
}

package gg.maeve.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import gg.maeve.launcher.ui.components.MaeveMark
import gg.maeve.launcher.ui.screens.HomeScreen
import gg.maeve.launcher.ui.screens.ModsScreen
import gg.maeve.launcher.ui.screens.SettingsScreen
import gg.maeve.launcher.ui.screens.SignInScreen
import gg.maeve.launcher.ui.theme.Maeve
import gg.maeve.launcher.ui.theme.MaeveFonts

@Composable
fun Shell(vm: LauncherViewModel) {
    if (vm.session == null) { SignInScreen(vm); return }
    Row(Modifier.fillMaxSize()) {
        Sidebar(vm)
        Box(Modifier.weight(1f).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (vm.screen) {
                Screen.SETTINGS -> SettingsScreen(vm)
                Screen.MODS -> ModsScreen(vm)
                else -> HomeScreen(vm)
            }
        }
    }
}

@Composable
private fun Sidebar(vm: LauncherViewModel) {
    Column(
        Modifier.width(220.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 18.dp, start = 4.dp)) {
            MaeveMark(size = 26.dp)
            Text("Maeve", fontFamily = MaeveFonts.Marcellus, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
        }
        NavItem("Home", vm.screen == Screen.HOME) { vm.screen = Screen.HOME }
        NavItem("Mods", vm.screen == Screen.MODS) { vm.screen = Screen.MODS }
        NavItem("Settings", vm.screen == Screen.SETTINGS) { vm.screen = Screen.SETTINGS }
        Spacer(Modifier.weight(1f))
        Text(vm.session?.username ?: "", color = Maeve.text2, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun NavItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(if (selected) Maeve.accentSubtle else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.primary else Maeve.text2,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

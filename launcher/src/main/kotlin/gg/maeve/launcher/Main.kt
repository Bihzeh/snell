package gg.maeve.launcher

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import gg.maeve.launcher.ui.LauncherViewModel
import gg.maeve.launcher.ui.Shell
import gg.maeve.launcher.ui.theme.MaeveTheme

fun main() = application {
    val windowState = rememberWindowState(width = 1060.dp, height = 680.dp)
    val scope = rememberCoroutineScope()
    val vm = remember { LauncherViewModel(scope) }
    Window(onCloseRequest = ::exitApplication, state = windowState, title = "Maeve") {
        MaeveTheme {
            LaunchedEffect(Unit) { vm.checkForUpdates() }
            Shell(vm)
        }
    }
}

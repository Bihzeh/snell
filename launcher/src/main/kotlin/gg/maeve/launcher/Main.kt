package gg.maeve.launcher

import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import gg.maeve.launcher.ui.LauncherViewModel
import gg.maeve.launcher.ui.LogWindow
import gg.maeve.launcher.ui.Shell
import gg.maeve.launcher.ui.chrome.WindowChrome
import gg.maeve.launcher.ui.theme.MaeveTheme
import java.awt.Dimension

fun main() = application {
    val state = rememberWindowState(width = 1280.dp, height = 800.dp, position = WindowPosition(Alignment.Center))
    val scope = rememberCoroutineScope()
    val vm = remember { LauncherViewModel(scope) }
    Window(
        onCloseRequest = ::exitApplication,
        state = state,
        undecorated = true,   // custom Obsidian chrome; edge-resize still works (UndecoratedWindowResizer)
        resizable = true,
        title = "Maeve",
        icon = painterResource("brand/maeve.png"),
    ) {
        LaunchedEffect(Unit) { window.minimumSize = Dimension(1000, 640) }
        val chrome = remember {
            WindowChrome(
                onMinimize = { state.isMinimized = true },
                onToggleMaximize = {
                    state.placement =
                        if (state.placement == WindowPlacement.Maximized) WindowPlacement.Floating else WindowPlacement.Maximized
                },
                onClose = { exitApplication() },
                dragWrapper = { content -> WindowDraggableArea { content() } },
            )
        }
        MaeveTheme {
            LaunchedEffect(Unit) { vm.checkForUpdates() }
            Shell(vm, chrome)
        }
    }

    if (vm.showLogWindow && vm.logWindowOpen) {
        LogWindow(vm) { vm.logWindowOpen = false }
    }
}

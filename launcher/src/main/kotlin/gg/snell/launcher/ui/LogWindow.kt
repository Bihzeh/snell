package gg.snell.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import gg.snell.launcher.ui.components.Brand
import gg.snell.launcher.ui.theme.Snell
import gg.snell.launcher.ui.theme.SnellFonts
import gg.snell.launcher.ui.theme.SnellTheme

/** Secondary window streaming live game stdout/stderr; opened on launch when enabled in settings. */
@Composable
fun ApplicationScope.LogWindow(vm: LauncherViewModel, onClose: () -> Unit) {
    val state = rememberWindowState(width = 780.dp, height = 480.dp, position = WindowPosition(Alignment.Center))
    Window(onCloseRequest = onClose, state = state, title = "Snell - Game logs", icon = painterResource(Brand.APP_ICON)) {
        SnellTheme { LogPanel(vm) }
    }
}

@Composable
fun LogPanel(vm: LauncherViewModel) {
    val listState = rememberLazyListState()
    var autoScroll by remember { mutableStateOf(true) }
    LaunchedEffect(vm.log.size, autoScroll) {
        if (autoScroll && vm.log.isNotEmpty()) listState.scrollToItem(vm.log.lastIndex)
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().height(46.dp).background(Snell.bg2).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (vm.playing) Snell.success else Snell.text3))
            Spacer(Modifier.width(8.dp))
            Text(if (vm.playing) "Running" else "Stopped", color = Snell.text2, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text("${vm.log.size} lines", color = Snell.text3, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            Chip("Auto-scroll", active = autoScroll) { autoScroll = !autoScroll }
            Spacer(Modifier.width(6.dp))
            Chip("Copy") { copyLogs(vm.log) }
            Spacer(Modifier.width(6.dp))
            Chip("Clear") { vm.log.clear() }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Snell.s2))
        SelectionContainer(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp)) {
                items(vm.log) { line ->
                    Text(line, fontFamily = SnellFonts.Mono, fontSize = 12.sp, lineHeight = 17.sp, color = lineColor(line))
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, active: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(7.dp)).background(if (active) Snell.accentSubtle else Snell.s2)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = if (active) Snell.accentHi else Snell.text2, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun lineColor(line: String): Color {
    val u = line.uppercase()
    return when {
        "ERROR" in u || "EXCEPTION" in u || "FATAL" in u -> Color(0xFFF06B6B)
        "WARN" in u -> Color(0xFFFF883E)
        else -> Color(0xFFB6BCC8)
    }
}

private fun copyLogs(lines: List<String>) = runCatching {
    java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(java.awt.datatransfer.StringSelection(lines.joinToString("\n")), null)
}

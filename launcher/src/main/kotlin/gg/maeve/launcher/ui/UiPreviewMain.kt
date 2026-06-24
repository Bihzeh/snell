package gg.maeve.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import gg.maeve.launcher.auth.DeviceCodePrompt
import gg.maeve.launcher.game.GameSession
import gg.maeve.launcher.update.SemVer
import gg.maeve.launcher.update.UpdateInfo
import gg.maeve.launcher.update.UpdateState
import gg.maeve.launcher.ui.theme.MaeveTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Headless render of the launcher screens to PNGs (CPU Skia — no display needed),
 * so the UI can be visually verified on a server box. Run: ./gradlew :launcher:uiPreview
 */
@OptIn(ExperimentalComposeUiApi::class)
private fun render(name: String, w: Int, h: Int, content: @Composable () -> Unit) {
    val scene = ImageComposeScene(width = w, height = h, density = Density(1f)) { MaeveTheme { content() } }
    try {
        val png = scene.render().encodeToData(EncodedImageFormat.PNG)!!.bytes
        File("build/ui-preview/$name.png").apply { parentFile.mkdirs() }.writeBytes(png)
        println("rendered $name.png")
    } finally { scene.close() }
}

private fun vm(block: LauncherViewModel.() -> Unit = {}) =
    LauncherViewModel(CoroutineScope(Dispatchers.Default)).apply(block)

fun main() {
    val W = 1060; val H = 680
    render("01-signin-idle", W, H) { Shell(vm()) }
    render("02-signin-code", W, H) {
        Shell(vm { prompt = DeviceCodePrompt("QKZP-RTLM", "https://microsoft.com/link", 900, "") })
    }
    render("03-home", W, H) { Shell(vm { session = GameSession.offline("MaeveQueen"); screen = Screen.HOME }) }
    render("04-mods", W, H) { Shell(vm { session = GameSession.offline("MaeveQueen"); screen = Screen.MODS }) }
    render("05-settings", W, H) { Shell(vm { session = GameSession.offline("MaeveQueen"); screen = Screen.SETTINGS }) }
    render("06-home-update", W, H) {
        Shell(vm {
            session = GameSession.offline("MaeveQueen"); screen = Screen.HOME
            update = UpdateState.Available(UpdateInfo(SemVer(0, 1, 4), "v0.1.4", "https://dl/x.exe", "Maeve-0.1.4.exe", null))
        })
    }
    println("ui preview done")
}

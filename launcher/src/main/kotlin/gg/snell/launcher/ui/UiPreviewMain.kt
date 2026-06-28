package gg.snell.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import gg.snell.launcher.auth.DeviceCodePrompt
import gg.snell.launcher.game.GameSession
import gg.snell.launcher.update.SemVer
import gg.snell.launcher.update.UpdateInfo
import gg.snell.launcher.update.UpdateState
import gg.snell.launcher.ui.theme.SnellTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.CompositionLocalProvider
import gg.snell.launcher.ui.components.LocalSkyTimeOverride
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Headless render of the launcher screens to PNGs (CPU Skia — no display needed), so the UI can
 * be visually verified on a server box. Run: ./gradlew :launcher:uiPreview
 * Renders the in-content layout (title bar visuals, rail, screens); real window behavior
 * (drag/resize/min-max) needs a real desktop and is not exercised here.
 */
@OptIn(ExperimentalComposeUiApi::class)
private fun render(name: String, w: Int, h: Int, content: @Composable () -> Unit) {
    val scene = ImageComposeScene(width = w, height = h, density = Density(1f)) { SnellTheme { content() } }
    try {
        val png = scene.render().encodeToData(EncodedImageFormat.PNG)!!.bytes
        File("build/ui-preview/$name.png").apply { parentFile.mkdirs() }.writeBytes(png)
        println("rendered $name.png")
    } finally { scene.close() }
}

private fun vm(block: LauncherViewModel.() -> Unit = {}) =
    LauncherViewModel(CoroutineScope(Dispatchers.Default)).apply(block)

private fun signedIn(s: Screen, block: LauncherViewModel.() -> Unit = {}) = vm {
    session = GameSession.offline(); screen = s; block()
}

fun main() {
    val W = 1280; val H = 800
    render("01-signin-idle", W, H) { Shell(vm()) }
    render("02-signin-code", W, H) { Shell(vm { prompt = DeviceCodePrompt("QKZP-RTLM", "https://microsoft.com/link", 900, "") }) }
    render("03-signin-error", W, H) { Shell(vm { signInError = "That code expired before sign-in finished." }) }
    render("04-home", W, H) { CompositionLocalProvider(LocalSkyTimeOverride provides 0.42f) { Shell(signedIn(Screen.HOME)) } }
    render("05-home-progress", W, H) {
        CompositionLocalProvider(LocalSkyTimeOverride provides 0.42f) { Shell(signedIn(Screen.HOME) { playing = true; playStatus = "Downloading assets 45/100"; playFraction = 0.45f }) }
    }
    render("06-home-update", W, H) {
        Shell(signedIn(Screen.HOME) { update = UpdateState.Available(UpdateInfo(SemVer(0, 1, 5), "v0.1.5", "https://dl/x.exe", "Snell-0.1.5.exe", null)) })
    }
    render("07-mods", W, H) { Shell(signedIn(Screen.MODS)) }
    render("08-settings", W, H) { Shell(signedIn(Screen.SETTINGS)) }
    render("09-cosmetics", W, H) { Shell(signedIn(Screen.COSMETICS)) }
    render("10-friends", W, H) { Shell(signedIn(Screen.FRIENDS)) }
    render("11-home-night", W, H) { CompositionLocalProvider(LocalSkyTimeOverride provides 0.96f) { Shell(signedIn(Screen.HOME)) } }
    render("12-home-sunset", W, H) { CompositionLocalProvider(LocalSkyTimeOverride provides 0.80f) { Shell(signedIn(Screen.HOME)) } }
    render("13-logs", 780, 480) {
        LogPanel(vm {
            playing = true
            listOf(
                "[12:00:01] [main/INFO]: Setting user: Bihzeh",
                "[12:00:01] [main/INFO]: Loading Minecraft 26.2 with Fabric Loader 0.19.3",
                "[12:00:02] [main/INFO]: Sodium Renderer initialized",
                "[12:00:02] [main/INFO]: Lithium loaded 137 optimizations",
                "[12:00:03] [main/WARN]: Mod 'snell' uses a deprecated API",
                "[12:00:03] [Render thread/INFO]: OpenAL initialized",
                "[12:00:05] [main/INFO]: Snell HUD enabled",
                "[12:00:06] [Server thread/INFO]: Preparing spawn area: 84%",
                "[12:00:07] [main/ERROR]: Failed to load optional shader pack",
                "[12:00:08] [Render thread/INFO]: Loaded 12 advancements",
            ).forEach { log.add(it) }
        })
    }
    println("ui preview done")
}

package gg.snell.mod

import gg.snell.mod.config.Config
import gg.snell.mod.module.ModuleManager
import gg.snell.mod.module.hud.CoordsModule
import gg.snell.mod.module.hud.FpsModule
import gg.snell.mod.module.hud.KeystrokesModule
import gg.snell.mod.render.HudRenderController
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HudAndConfigTest {

    private fun manager() = ModuleManager(Config(Files.createTempDirectory("snell-test"))).apply {
        register(FpsModule()); register(CoordsModule()); register(KeystrokesModule())
    }

    @Test
    fun `enabled hud modules draw, disabled ones do not`() {
        val canvas = FakeHudCanvas()
        HudRenderController(manager()).draw(canvas, gameCtx())
        val texts = canvas.draws.map { it.text }
        assertTrue(texts.any { it == "60 FPS" }, "FPS should render: $texts")
        assertTrue(texts.any { it.startsWith("XYZ:") }, "Coords should render: $texts")
        assertTrue(texts.none { it.contains("[W]") }, "Keystrokes disabled -> nothing: $texts")
    }

    @Test
    fun `coords hidden when not in world`() {
        val canvas = FakeHudCanvas()
        HudRenderController(manager()).draw(canvas, gameCtx(inWorld = false))
        assertTrue(canvas.draws.none { it.text.startsWith("XYZ:") })
    }

    @Test
    fun `module toggle persists across reload`() {
        val dir = Files.createTempDirectory("snell-persist")
        val m1 = ModuleManager(Config(dir).apply { load() }).apply { register(FpsModule()) }
        m1.toggle("fps")
        val reloaded = FpsModule()
        ModuleManager(Config(dir).apply { load() }).register(reloaded)
        assertEquals(false, reloaded.enabled, "disabled state should persist across reload")
    }
}

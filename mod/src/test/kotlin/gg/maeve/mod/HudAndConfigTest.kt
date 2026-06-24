package gg.maeve.mod

import gg.maeve.mod.config.Config
import gg.maeve.mod.module.ModuleManager
import gg.maeve.mod.module.hud.CoordsModule
import gg.maeve.mod.module.hud.FpsModule
import gg.maeve.mod.module.hud.KeystrokesModule
import gg.maeve.mod.platform.GameContext
import gg.maeve.mod.platform.HudCanvas
import gg.maeve.mod.render.HudRenderController
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Records draw calls so the pure HUD logic can be tested without Minecraft. */
private class FakeCanvas : HudCanvas {
    data class Draw(val x: Int, val y: Int, val text: String)
    val draws = mutableListOf<Draw>()
    override fun drawText(x: Int, y: Int, text: String, color: Int) { draws.add(Draw(x, y, text)) }
    override fun textWidth(text: String) = text.length * 6
    override val lineHeight = 10
}

private fun ctx(inWorld: Boolean = true) = GameContext(
    fps = 60, inWorld = inWorld,
    playerX = 1.0, playerY = 64.0, playerZ = -2.0,
    keyForward = true, keyBack = false, keyLeft = false, keyRight = true,
)

class HudAndConfigTest {

    private fun manager() = ModuleManager(Config(Files.createTempDirectory("maeve-test"))).apply {
        register(FpsModule()); register(CoordsModule()); register(KeystrokesModule())
    }

    @Test
    fun `enabled hud modules draw, disabled ones do not`() {
        val canvas = FakeCanvas()
        HudRenderController(manager()).draw(canvas, ctx())

        val texts = canvas.draws.map { it.text }
        assertTrue(texts.any { it == "60 FPS" }, "FPS should render: $texts")
        assertTrue(texts.any { it.startsWith("XYZ:") }, "Coords should render: $texts")
        // Keystrokes is disabled by default -> no [W]/[D] rows
        assertTrue(texts.none { it.contains("[W]") }, "Keystrokes disabled -> nothing: $texts")
    }

    @Test
    fun `coords hidden when not in world`() {
        val canvas = FakeCanvas()
        HudRenderController(manager()).draw(canvas, ctx(inWorld = false))
        assertTrue(canvas.draws.none { it.text.startsWith("XYZ:") })
    }

    @Test
    fun `module toggle persists across reload`() {
        val dir = Files.createTempDirectory("maeve-persist")

        val m1 = ModuleManager(Config(dir).apply { load() }).apply { register(FpsModule()) }
        m1.toggle("fps") // FPS starts enabled -> now disabled, saved

        val reloaded = FpsModule()
        ModuleManager(Config(dir).apply { load() }).register(reloaded)
        assertEquals(false, reloaded.enabled, "disabled state should persist across reload")
    }
}

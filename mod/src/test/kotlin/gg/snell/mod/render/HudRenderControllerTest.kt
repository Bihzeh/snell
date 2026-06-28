package gg.snell.mod.render

import gg.snell.mod.FakeHudCanvas
import gg.snell.mod.config.Config
import gg.snell.mod.gameCtx
import gg.snell.mod.module.HudAnchor
import gg.snell.mod.module.ModuleManager
import gg.snell.mod.module.hud.FpsModule
import gg.snell.shared.SnellPalette
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class HudRenderControllerTest {
    private fun managerWith(fps: FpsModule) =
        ModuleManager(Config(Files.createTempDirectory("snell-render"))).apply { register(fps) }

    @Test fun `right-anchored element resolves to the right edge`() {
        val canvas = FakeHudCanvas(screenWidth = 800, screenHeight = 600)
        val fps = FpsModule().apply { anchor = HudAnchor.TOP_RIGHT; offsetX = 4; offsetY = 4 }
        HudRenderController(managerWith(fps)).draw(canvas, gameCtx())
        val t = canvas.transforms.single()
        // "60 FPS" = 6 chars * 6 = 36; + padding 2*2 -> localW 40; scale 1 -> footW 40
        assertEquals(800 - 40 - 4, t.pivotX)
        assertEquals(4, t.pivotY)
    }

    @Test fun `fractional scale reserves a ceil footprint for right anchor`() {
        val canvas = FakeHudCanvas(screenWidth = 800, screenHeight = 600)
        val fps = FpsModule().apply {
            anchor = HudAnchor.TOP_RIGHT; offsetX = 4; offsetY = 4
            style = style.copy(scale = 1.44f)
        }
        HudRenderController(managerWith(fps)).draw(canvas, gameCtx())
        val t = canvas.transforms.single()
        // localW 40; footW = ceil(40 * 1.44 = 57.6) = 58 -> never overflows the right gap
        assertEquals(800 - 58 - 4, t.pivotX)
        assertEquals(1.44f, t.scale)
    }

    @Test fun `background draws exactly one fill`() {
        val canvas = FakeHudCanvas()
        val fps = FpsModule().apply { style = style.copy(background = true) }
        HudRenderController(managerWith(fps)).draw(canvas, gameCtx())
        assertEquals(1, canvas.fills.size)
    }

    @Test fun `inherited line uses the module style color`() {
        val canvas = FakeHudCanvas()
        HudRenderController(managerWith(FpsModule())).draw(canvas, gameCtx())
        val d = canvas.draws.single { it.text == "60 FPS" }
        assertEquals(SnellPalette.gold, d.color)
    }

    @Test fun `scale invokes the scale transform`() {
        val canvas = FakeHudCanvas()
        val fps = FpsModule().apply { style = style.copy(scale = 2.0f) }
        HudRenderController(managerWith(fps)).draw(canvas, gameCtx())
        assertEquals(2.0f, canvas.transforms.single().scale)
    }
}

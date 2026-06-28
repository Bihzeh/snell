package gg.snell.mod.config

import gg.snell.mod.module.HudAnchor
import gg.snell.mod.module.hud.FpsModule
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigMigrationTest {
    @Test fun `v1 x y migrates to top-left offset and default style`() {
        val dir = Files.createTempDirectory("snell-v1")
        dir.resolve("config.json").writeText("""{"schema":1,"modules":{"fps":{"enabled":false,"x":10,"y":20}}}""")
        val fps = FpsModule()
        Config(dir).apply { load() }.applyTo(fps)
        assertEquals(false, fps.enabled)
        assertEquals(HudAnchor.TOP_LEFT, fps.anchor)
        assertEquals(10, fps.offsetX)
        assertEquals(20, fps.offsetY)
        assertEquals(fps.defaultStyle, fps.style)
    }

    @Test fun `v2 anchor offset and style round-trip`() {
        val dir = Files.createTempDirectory("snell-v2")
        val fps = FpsModule().apply {
            anchor = HudAnchor.BOTTOM_RIGHT; offsetX = 8; offsetY = 9
            style = style.copy(bold = true, scale = 2.0f, color = 0xFF112233.toInt(), background = true)
        }
        Config(dir).apply { snapshot(listOf(fps)); save() }
        val reloaded = FpsModule()
        Config(dir).apply { load() }.applyTo(reloaded)
        assertEquals(HudAnchor.BOTTOM_RIGHT, reloaded.anchor)
        assertEquals(8, reloaded.offsetX)
        assertEquals(9, reloaded.offsetY)
        assertTrue(reloaded.style.bold)
        assertEquals(2.0f, reloaded.style.scale)
        assertEquals(0xFF112233.toInt(), reloaded.style.color)
        assertTrue(reloaded.style.background)
    }

    @Test fun `scale is clamped on load`() {
        val dir = Files.createTempDirectory("snell-clamp")
        dir.resolve("config.json").writeText("""{"schema":2,"modules":{"fps":{"enabled":true,"scale":9.0}}}""")
        val fps = FpsModule()
        Config(dir).apply { load() }.applyTo(fps)
        assertEquals(3.0f, fps.style.scale)
    }

    @Test fun `unknown keys still load`() {
        val dir = Files.createTempDirectory("snell-unknown")
        dir.resolve("config.json").writeText("""{"schema":2,"future":42,"modules":{"fps":{"enabled":false,"surprise":true}}}""")
        val fps = FpsModule()
        Config(dir).apply { load() }.applyTo(fps)
        assertEquals(false, fps.enabled)
    }
}

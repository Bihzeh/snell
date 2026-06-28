package gg.snell.mod.config

import gg.snell.mod.module.hud.CpsModule
import gg.snell.mod.module.hud.KeystrokesModule
import kotlin.test.assertEquals
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigOptionsTest {
    @Test fun `module options round-trip through config`() {
        val dir = Files.createTempDirectory("cfg")
        val cps = CpsModule().also { it.setOption("right", false) }
        Config(dir).apply { snapshot(listOf(cps)); save() }
        val restored = CpsModule()
        assertTrue(restored.option("right"), "default before load")
        Config(dir).apply { load(); applyTo(restored) }
        assertFalse(restored.option("right"), "persisted option restored")
    }

    @Test fun `module colour options round-trip through config`() {
        val dir = Files.createTempDirectory("cfg")
        val ks = KeystrokesModule().also { it.setColorOption("box", 0xFF010203.toInt()) }
        Config(dir).apply { snapshot(listOf(ks)); save() }
        val restored = KeystrokesModule()
        Config(dir).apply { load(); applyTo(restored) }
        assertEquals(0xFF010203.toInt(), restored.colorOption("box"), "box colour restored")
    }
}

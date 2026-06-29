package gg.snell.mod.module

import gg.snell.mod.config.Config
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FontModuleTest {
    @Test fun `defaults`() {
        val f = FontModule()
        assertEquals("font", f.id)
        assertEquals("Custom Font (Geist)", f.displayName)
        assertTrue(f.enabled)
    }

    @Test fun `toggle persists across reload`() {
        val dir = Files.createTempDirectory("font-cfg")
        ModuleManager(Config(dir).apply { load() }).apply { register(FontModule()) }.toggle("font")
        val reloaded = FontModule()
        ModuleManager(Config(dir).apply { load() }).register(reloaded)
        assertFalse(reloaded.enabled)
    }
}

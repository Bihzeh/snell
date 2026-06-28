package gg.snell.mod.config

import gg.snell.mod.module.hud.FpsModule
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigSnapTest {
    @Test fun `snap defaults to on`() {
        assertTrue(Config(Files.createTempDirectory("cfg")).isSnapEnabled())
    }

    @Test fun `snap toggle persists across reload`() {
        val dir = Files.createTempDirectory("cfg")
        Config(dir).apply { setSnapEnabled(false); save() }
        val reloaded = Config(dir).apply { load() }
        assertFalse(reloaded.isSnapEnabled())
    }

    @Test fun `editor block coexists with module state`() {
        val dir = Files.createTempDirectory("cfg")
        Config(dir).apply { setSnapEnabled(false); snapshot(listOf(FpsModule())); save() }
        val reloaded = Config(dir).apply { load() }
        assertFalse(reloaded.isSnapEnabled())
        val fps = FpsModule().also { it.enabled = false }
        reloaded.applyTo(fps)
        assertTrue(fps.enabled, "module state restored from its own snapshot") // FpsModule default enabled=true was snapshotted
    }
}

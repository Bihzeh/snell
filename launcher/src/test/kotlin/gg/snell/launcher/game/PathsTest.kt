package gg.snell.launcher.game

import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PathsTest {
    @Test
    fun migratesLegacyDataDirWhenCurrentAbsent() {
        val tmp = Files.createTempDirectory("snellpaths")
        val legacy = tmp.resolve(".maeve")
        Files.createDirectories(legacy.resolve("instances").resolve("x"))
        legacy.resolve("token.txt").writeText("secret")
        val current = tmp.resolve(".snell")

        migrateLegacyData(legacy, current)

        assertTrue(current.exists(), "current dir created")
        assertEquals("secret", current.resolve("token.txt").readText())
        assertTrue(current.resolve("instances").resolve("x").exists(), "nested data carried over")
        assertFalse(legacy.exists(), "legacy dir moved, not left behind")
    }

    @Test
    fun leavesLegacyUntouchedWhenCurrentAlreadyExists() {
        val tmp = Files.createTempDirectory("snellpaths")
        val legacy = tmp.resolve(".maeve"); Files.createDirectories(legacy)
        legacy.resolve("old.txt").writeText("old")
        val current = tmp.resolve(".snell"); Files.createDirectories(current)
        current.resolve("new.txt").writeText("new")

        migrateLegacyData(legacy, current)

        // Never overwrite an existing install; both dirs stay as-is.
        assertTrue(legacy.resolve("old.txt").exists())
        assertFalse(current.resolve("old.txt").exists())
        assertEquals("new", current.resolve("new.txt").readText())
    }

    @Test
    fun noopWhenLegacyAbsent() {
        val tmp = Files.createTempDirectory("snellpaths")
        val current = tmp.resolve(".snell")
        migrateLegacyData(tmp.resolve("does-not-exist"), current) // must not throw
        assertFalse(current.exists())
    }

    @Test
    fun migratesPerInstanceConfigNamespaceFromMaeveToSnell() {
        val tmp = Files.createTempDirectory("snellpaths")
        val legacy = tmp.resolve(".maeve")
        // The mod used to write config/maeve/config.json; the renamed mod reads config/snell/.
        val cfg = legacy.resolve("instances").resolve("1.21").resolve("config").resolve("maeve")
        Files.createDirectories(cfg)
        cfg.resolve("config.json").writeText("{\"hud\":true}")
        val current = tmp.resolve(".snell")

        migrateLegacyData(legacy, current)

        val cfgRoot = current.resolve("instances").resolve("1.21").resolve("config")
        assertEquals("{\"hud\":true}", cfgRoot.resolve("snell").resolve("config.json").readText())
        assertFalse(cfgRoot.resolve("maeve").exists(), "old config namespace renamed, not left behind")
    }
}

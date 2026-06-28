package gg.snell.launcher.game

import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModProvisionerTest {

    @Test
    fun `local override is copied to snell_jar`(@TempDir mods: Path) {
        val local = Files.createTempFile(mods, "dev-mod", ".jar").also { it.writeBytes(byteArrayOf(1, 2, 3, 4)) }

        val installed = installSnellMod(mods, local, openBundled = { error("must not read bundled when override present") })

        assertTrue(installed)
        val target = mods.resolve("snell.jar")
        assertTrue(target.exists())
        assertContentEquals(byteArrayOf(1, 2, 3, 4), target.readBytes())
    }

    @Test
    fun `bundled resource is extracted when no override`(@TempDir mods: Path) {
        val bundled = byteArrayOf(10, 20, 30)

        val installed = installSnellMod(mods, localOverride = null, openBundled = { ByteArrayInputStream(bundled) })

        assertTrue(installed)
        assertContentEquals(bundled, mods.resolve("snell.jar").readBytes())
    }

    @Test
    fun `falls back to bundled when override path does not exist`(@TempDir mods: Path) {
        val bundled = byteArrayOf(7, 7, 7)

        val installed = installSnellMod(mods, mods.resolve("nope.jar"), openBundled = { ByteArrayInputStream(bundled) })

        assertTrue(installed)
        assertContentEquals(bundled, mods.resolve("snell.jar").readBytes())
    }

    @Test
    fun `returns false and writes nothing when neither override nor bundle present`(@TempDir mods: Path) {
        val installed = installSnellMod(mods, localOverride = null, openBundled = { null })

        assertFalse(installed)
        assertFalse(mods.resolve("snell.jar").exists())
    }

    @Test
    fun `status callback reports local vs bundled source`(@TempDir root: Path) {
        val a = Files.createDirectory(root.resolve("a"))
        val b = Files.createDirectory(root.resolve("b"))
        val local = Files.createTempFile(root, "dev-mod", ".jar").also { it.writeBytes(byteArrayOf(1)) }
        val statuses = mutableListOf<String>()

        installSnellMod(a, local, openBundled = { null }, onStatus = { statuses.add(it) })
        installSnellMod(b, null, openBundled = { ByteArrayInputStream(byteArrayOf(2)) }, onStatus = { statuses.add(it) })

        assertEquals(listOf("Mod: snell (local)", "Mod: snell"), statuses)
    }

    @Test
    fun `bundledModStream finds a present resource and returns null for a missing one`() {
        // launcher/src/test/resources/bundled-mods/snell.jar is on the test classpath.
        assertNotNull(bundledModStream("bundled-mods/snell.jar"))
        assertNull(bundledModStream("bundled-mods/definitely-absent.jar"))
    }

    @Test
    fun `findDevModJar picks the newest non-sources jar`(@TempDir dir: Path) {
        val old = dir.resolve("mod-0.0.1.jar").also { it.writeBytes(byteArrayOf(0)) }
        val new = dir.resolve("mod-0.1.4.jar").also { it.writeBytes(byteArrayOf(0)) }
        val sources = dir.resolve("mod-0.1.4-sources.jar").also { it.writeBytes(byteArrayOf(0)) }
        Files.setLastModifiedTime(old, FileTime.fromMillis(1_000))
        Files.setLastModifiedTime(new, FileTime.fromMillis(2_000))
        Files.setLastModifiedTime(sources, FileTime.fromMillis(9_000)) // newest, but excluded as -sources

        assertEquals(new, findDevModJar(dir))
    }

    @Test
    fun `findDevModJar returns null for a missing directory`(@TempDir dir: Path) {
        assertNull(findDevModJar(dir.resolve("does-not-exist")))
    }

    @Test
    fun `selectBundledMods always installs required deps even when toggles exclude them`() {
        // Regression: the UI passes only {sodium, lithium}; fabric-api + fabric-language-kotlin
        // are REQUIRED deps of the Snell mod and must install or Fabric aborts with
        // "requires fabric-api / fabric-language-kotlin, which is missing".
        assertEquals(
            listOf("fabric-api", "fabric-language-kotlin", "sodium", "lithium"),
            selectBundledMods(setOf("sodium", "lithium")),
        )
        assertEquals(listOf("fabric-api", "fabric-language-kotlin"), selectBundledMods(emptySet()))
    }

    @Test
    fun `selectBundledMods with null installs all bundled mods`() {
        assertEquals(
            listOf("fabric-api", "fabric-language-kotlin", "sodium", "lithium"),
            selectBundledMods(null),
        )
    }

    @Test
    fun `selectBundledMods honors a single optional toggle but keeps required deps`() {
        assertEquals(listOf("fabric-api", "fabric-language-kotlin", "sodium"), selectBundledMods(setOf("sodium")))
    }

    @Test
    fun `pruneLegacySelfMods deletes the pre-rename self-mod jar but keeps other mods`(@TempDir mods: Path) {
        // A migrated instance still has the old maeve.jar; with the new snell.jar installed
        // alongside it, Fabric would load BOTH (distinct mod ids) -> duplicate HUD/keybind/mixin.
        mods.resolve("maeve.jar").writeBytes("old self-mod".toByteArray())
        mods.resolve("sodium.jar").writeBytes("perf mod".toByteArray())
        pruneLegacySelfMods(mods)
        assertFalse(mods.resolve("maeve.jar").exists(), "legacy self-mod jar removed")
        assertTrue(mods.resolve("sodium.jar").exists(), "other mods untouched")
    }

    @Test
    fun `pruneLegacySelfMods is a no-op when no legacy jar present`(@TempDir mods: Path) {
        pruneLegacySelfMods(mods) // must not throw
        assertEquals(0L, Files.list(mods).use { it.count() })
    }
}

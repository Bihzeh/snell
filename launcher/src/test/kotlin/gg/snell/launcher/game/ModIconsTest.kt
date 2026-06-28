package gg.snell.launcher.game

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModIconsTest {
    @Test
    fun projectsUrlEncodesIdsArray() {
        val url = ModIcons.projectsUrl(listOf("sodium", "lithium"))
        assertTrue(url.startsWith("https://api.modrinth.com/v2/projects?ids="))
        assertTrue("%5B%22sodium%22%2C%22lithium%22%5D" in url, url)
    }

    @Test
    fun parsesProjectIconUrlIgnoringUnknownKeys() {
        val json = """[{"slug":"sodium","title":"Sodium","icon_url":"https://cdn/x.webp","downloads":99}]"""
        val list = LauncherJson.decodeFromString<List<ModrinthProject>>(json)
        assertEquals(1, list.size)
        assertEquals("sodium", list[0].slug)
        assertEquals("https://cdn/x.webp", list[0].icon_url)
    }

    @Test
    fun parsesTitleDescriptionAndCategories() {
        val json = """[{"slug":"sodium","title":"Sodium","description":"A high-performance rendering engine replacement for Minecraft, which greatly improves frame rates and reduces micro-stutter.","categories":["optimization"],"icon_url":"https://cdn/x.webp"}]"""
        val p = LauncherJson.decodeFromString<List<ModrinthProject>>(json).single()
        assertEquals("Sodium", p.title)
        assertEquals("A high-performance rendering engine replacement for Minecraft, which greatly improves frame rates and reduces micro-stutter.", p.description)
        assertEquals(listOf("optimization"), p.categories)
    }

    @Test
    fun toInfoCapitalizesFirstCategoryAsTag() {
        val info = ModrinthProject(
            slug = "sodium",
            title = "Sodium",
            description = "A high-performance rendering engine replacement for Minecraft, which greatly improves frame rates and reduces micro-stutter.",
            categories = listOf("optimization", "fabric"),
        ).toInfo()
        assertEquals("Sodium", info.title)
        assertEquals("Optimization", info.category)
        assertEquals("A high-performance rendering engine replacement for Minecraft, which greatly improves frame rates and reduces micro-stutter.", info.description)
    }

    @Test
    fun toInfoFallsBackToSlugWhenTitleBlankAndEmptyTagWhenNoCategories() {
        val info = ModrinthProject(slug = "mystery-mod", title = "", categories = emptyList()).toInfo()
        assertEquals("mystery-mod", info.title)
        assertEquals("", info.category)
        assertEquals("", info.description)
    }

    @Test
    fun readsCachedMetaWithoutNetwork() = runBlocking {
        val root = Files.createTempDirectory("modicons-cached")
        val icons = root.resolve("icons"); Files.createDirectories(icons)
        icons.resolve("sodium.meta.json").writeText(
            LauncherJson.encodeToString(ModInfo("Sodium", "Fast renderer", "Optimization")),
        )
        // meta cache present => load must serve it from disk without hitting the network.
        val catalog = Net().use { net -> ModIcons.load(listOf("sodium"), root, net) }
        assertEquals("Optimization", catalog.info["sodium"]?.category)
        assertEquals("Fast renderer", catalog.info["sodium"]?.description)
    }

    @Test
    fun selfHealsCorruptMetaCacheByDeletingIt() = runBlocking {
        val root = Files.createTempDirectory("modicons-corrupt")
        val icons = root.resolve("icons"); Files.createDirectories(icons)
        val meta = icons.resolve("sodium.meta.json")
        meta.writeText("{ truncated json") // simulate an interrupted/partial write
        // The corrupt file exists, so load skips the fetch; the read-back must detect the
        // bad JSON, drop the slug, and delete the file so the next load re-fetches instead
        // of being pinned to the corrupt cache forever.
        val catalog = Net().use { net -> ModIcons.load(listOf("sodium"), root, net) }
        assertTrue("sodium" !in catalog.info)
        assertTrue(!meta.exists(), "corrupt meta cache should be deleted to self-heal")
    }
}

package gg.maeve.launcher.game

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
}

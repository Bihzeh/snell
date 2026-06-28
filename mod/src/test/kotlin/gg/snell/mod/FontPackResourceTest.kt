package gg.snell.mod

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Guards the bundled font pack's structure (the parts a typo would silently break). */
class FontPackResourceTest {
    private fun text(path: String) =
        javaClass.classLoader.getResourceAsStream(path)?.bufferedReader()?.readText()

    @Test fun `pack mcmeta uses the 26_2 major-minor format array`() {
        val txt = assertNotNull(text("resourcepacks/font/pack.mcmeta"))
        val pack = Json.parseToJsonElement(txt).jsonObject["pack"]!!.jsonObject
        assertEquals(listOf(88, 0), pack["min_format"]!!.jsonArray.map { it.jsonPrimitive.int })
        assertEquals(listOf(88, 99), pack["max_format"]!!.jsonArray.map { it.jsonPrimitive.int })
    }

    @Test fun `default font puts the poppins ttf ahead of the vanilla references`() {
        val txt = assertNotNull(text("resourcepacks/font/assets/minecraft/font/default.json"))
        val providers = Json.parseToJsonElement(txt).jsonObject["providers"]!!.jsonArray
        assertEquals("reference", providers[0].jsonObject["type"]!!.jsonPrimitive.content) // space first
        assertEquals("ttf", providers[1].jsonObject["type"]!!.jsonPrimitive.content)       // then our ttf (first-wins)
        assertEquals("snell:poppins.ttf", providers[1].jsonObject["file"]!!.jsonPrimitive.content)
    }

    @Test fun `poppins ttf is bundled and non-empty`() {
        val bytes = javaClass.classLoader.getResourceAsStream("resourcepacks/font/assets/snell/font/poppins.ttf")?.readBytes()
        assertNotNull(bytes)
        assertTrue(bytes.size > 1000, "poppins.ttf should be a real font")
    }
}

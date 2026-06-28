package gg.snell.mod.module

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModuleOptionsTest {
    private fun opts() = ModuleOptions(
        listOf(
            ToggleOption("a", "Alpha", true),
            ToggleOption("b", "Beta", false),
            ColorOption("c", "Colour", 0xFF112233.toInt()),
        ),
    )

    @Test fun `toggles default until set`() {
        val o = opts()
        assertTrue(o.bool("a")); assertFalse(o.bool("b"))
        o.setBool("a", false); assertFalse(o.bool("a"))
    }

    @Test fun `colour default until set`() {
        val o = opts()
        assertEquals(0xFF112233.toInt(), o.color("c"))
        o.setColor("c", 0xFF445566.toInt()); assertEquals(0xFF445566.toInt(), o.color("c"))
    }

    @Test fun `unknown keys are falsy`() {
        val o = opts(); assertFalse(o.bool("nope")); assertEquals(0, o.color("nope"))
    }

    @Test fun `typed filters split toggles and colours`() {
        val o = opts()
        assertEquals(listOf("a", "b"), o.toggles.map { it.key })
        assertEquals(listOf("c"), o.colorOptions.map { it.key })
    }
}

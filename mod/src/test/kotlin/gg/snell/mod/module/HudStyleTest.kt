package gg.snell.mod.module

import gg.snell.shared.SnellPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HudStyleTest {
    @Test fun `defaults match the launcher palette`() {
        val s = HudStyle()
        assertEquals(SnellPalette.text, s.color)
        assertTrue(s.shadow)
        assertFalse(s.bold)
        assertFalse(s.background)
        assertEquals(1.0f, s.scale)
        assertEquals(TextAlign.LEFT, s.align)
        assertEquals(SnellPalette.surfaceAlpha(0xC0), s.backgroundColor)
    }
}

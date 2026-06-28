package gg.snell.mod.render

import gg.snell.mod.module.HudAnchor
import gg.snell.mod.module.TextAlign
import kotlin.test.Test
import kotlin.test.assertEquals

class HudLayoutTest {
    @Test fun `top-left is identity offset`() {
        assertEquals(4 to 4, HudLayout.resolveTopLeft(HudAnchor.TOP_LEFT, 4, 4, 50, 20, 800, 600))
    }

    @Test fun `top-right measures from right edge`() {
        assertEquals((800 - 50 - 4) to 4, HudLayout.resolveTopLeft(HudAnchor.TOP_RIGHT, 4, 4, 50, 20, 800, 600))
    }

    @Test fun `bottom-right measures from both far edges`() {
        assertEquals((800 - 50 - 4) to (600 - 20 - 4), HudLayout.resolveTopLeft(HudAnchor.BOTTOM_RIGHT, 4, 4, 50, 20, 800, 600))
    }

    @Test fun `center centers the block`() {
        assertEquals(((800 - 50) / 2) to ((600 - 20) / 2), HudLayout.resolveTopLeft(HudAnchor.CENTER, 0, 0, 50, 20, 800, 600))
    }

    @Test fun `right anchor keeps a constant gap across screen sizes`() {
        for (sw in intArrayOf(800, 400, 1280)) {
            val (left, _) = HudLayout.resolveTopLeft(HudAnchor.TOP_RIGHT, 4, 4, 50, 20, sw, 600)
            assertEquals(4, sw - (left + 50)) // gap from right edge is always the offset
        }
    }

    @Test fun `lineX aligns within the block`() {
        assertEquals(10, HudLayout.lineX(10, 50, 30, TextAlign.LEFT))
        assertEquals(10 + (50 - 30) / 2, HudLayout.lineX(10, 50, 30, TextAlign.CENTER))
        assertEquals(10 + (50 - 30), HudLayout.lineX(10, 50, 30, TextAlign.RIGHT))
    }
}

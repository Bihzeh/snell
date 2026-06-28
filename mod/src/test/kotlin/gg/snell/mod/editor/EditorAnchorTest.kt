package gg.snell.mod.editor

import gg.snell.mod.module.HudAnchor
import gg.snell.mod.render.HudLayout
import kotlin.test.Test
import kotlin.test.assertEquals

class EditorAnchorTest {
    @Test fun `anchor from position picks the right third`() {
        val sw = 900; val sh = 600
        assertEquals(HudAnchor.TOP_LEFT, EditorAnchor.anchorFromPosition(Rect(10, 10, 40, 20), sw, sh))
        assertEquals(HudAnchor.CENTER, EditorAnchor.anchorFromPosition(Rect(430, 290, 40, 20), sw, sh))
        assertEquals(HudAnchor.BOTTOM_RIGHT, EditorAnchor.anchorFromPosition(Rect(840, 560, 40, 20), sw, sh))
    }

    @Test fun `offset round-trips through resolveTopLeft for every anchor`() {
        val sw = 800; val sh = 600
        val box = Rect(317, 211, 48, 24)
        for (anchor in HudAnchor.entries) {
            val (ox, oy) = EditorAnchor.offsetForAnchor(anchor, box, sw, sh)
            val (left, top) = HudLayout.resolveTopLeft(anchor, ox, oy, box.width, box.height, sw, sh)
            assertEquals(box.left to box.top, left to top, "round-trip for $anchor")
        }
    }
}

package gg.snell.mod.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PositionLayoutTest {
    @Test fun `mods button sits under the logo and centered`() {
        val logo = PositionLayout.logoRect(800, 600)
        val mods = PositionLayout.modsButton(800, 600)
        assertTrue(mods.top >= logo.bottom, "mods button below the logo")
        assertEquals(800 / 2, mods.cx(), "centered horizontally")
    }

    @Test fun `done button is bottom-right and disjoint from mods`() {
        val done = PositionLayout.doneButton(800, 600)
        val mods = PositionLayout.modsButton(800, 600)
        assertTrue(done.right <= 800 && done.bottom <= 600)
        assertTrue(done.top > mods.bottom || done.left > mods.right, "done does not overlap mods")
    }
}

class GridLayoutTest {
    @Test fun `cards are one per module and inside the panel`() {
        val count = 9
        val panel = GridLayout.panelRect(800, 600, count)
        val cards = GridLayout.cards(800, 600, count)
        assertEquals(count, cards.size)
        for (c in cards) {
            assertTrue(c.left >= panel.left && c.right <= panel.right, "card within panel width")
            assertTrue(c.top >= panel.top && c.bottom <= panel.bottom, "card within panel height")
        }
    }

    @Test fun `cards do not overlap`() {
        val cards = GridLayout.cards(800, 600, 6)
        for (i in cards.indices) for (j in i + 1 until cards.size) {
            val a = cards[i]; val b = cards[j]
            val disjoint = a.right <= b.left || b.right <= a.left || a.bottom <= b.top || b.bottom <= a.top
            assertTrue(disjoint, "cards $i and $j overlap")
        }
    }

    @Test fun `back button is inside the panel`() {
        val panel = GridLayout.panelRect(800, 600, 4)
        val back = GridLayout.backButton(800, 600, 4)
        assertTrue(back.left >= panel.left && back.right <= panel.right && back.top >= panel.top)
    }
}

class CustomizeLayoutTest {
    @Test fun `hud popup contains every style control`() {
        val popup = CustomizeLayout.popupRect(800, 600, true)
        val controls = CustomizeLayout.controls(popup, 0, 0)
        for (id in listOf("sv", "hue", "alpha", "hex", "visible", "scale-", "scale+", "reset")) {
            val c = controls.firstOrNull { it.id == id }
            assertNotNull(c, "control $id present")
            assertTrue(c.rect.left >= popup.left && c.rect.right <= popup.right, "$id within popup width")
            assertTrue(c.rect.top >= popup.top && c.rect.bottom <= popup.bottom, "$id within popup height")
        }
        for (id in CustomizeLayout.TOGGLES) assertNotNull(controls.firstOrNull { it.id == id }, "$id present")
        CustomizeLayout.SWATCHES.indices.forEach { i ->
            assertNotNull(controls.firstOrNull { it.id == "swatch:$i" }, "swatch:$i present")
        }
    }

    @Test fun `close button is inside the popup top bar`() {
        val popup = CustomizeLayout.popupRect(800, 600, true)
        val close = CustomizeLayout.closeButton(popup)
        assertTrue(close.right <= popup.right && close.top >= popup.top && close.bottom <= popup.top + 20)
    }

    @Test fun `non-hud popup is smaller and exposes only the enable toggle`() {
        val hud = CustomizeLayout.popupRect(800, 600, true)
        val plain = CustomizeLayout.popupRect(800, 600, false)
        assertTrue(plain.height < hud.height, "non-hud popup is shorter")
        val en = CustomizeLayout.enableToggle(plain)
        assertTrue(en.left >= plain.left && en.right <= plain.right && en.top >= plain.top && en.bottom <= plain.bottom)
    }

    @Test fun `popup grows to fit many option rows`() {
        val small = CustomizeLayout.popupRect(800, 600, true, 0, 0)
        val big = CustomizeLayout.popupRect(800, 600, true, 0, 12)
        assertTrue(big.height > small.height, "popup grows once the right column exceeds the picker column")
        for (r in CustomizeLayout.optionRows(big, 0, 12)) assertTrue(r.bottom <= big.bottom, "all option rows inside popup")
    }

    @Test fun `option rows sit inside the popup`() {
        val popup = CustomizeLayout.popupRect(800, 600, true)
        val rows = CustomizeLayout.optionRows(popup, 0, 2)
        assertEquals(2, rows.size)
        for (r in rows) {
            assertTrue(r.left >= popup.left && r.right <= popup.right, "option row within width")
            assertTrue(r.top >= popup.top && r.bottom <= popup.bottom, "option row within height")
        }
    }

    @Test fun `controls fit within the popup at a small gui size`() {
        val popup = CustomizeLayout.popupRect(360, 270, true) // gui-scale-4-ish
        for (c in CustomizeLayout.controls(popup, 0, 0)) {
            assertTrue(c.rect.right <= popup.right && c.rect.bottom <= popup.bottom, "${c.id} overflows small popup")
        }
    }
}

class PositionExtrasTest {
    @Test fun `snap button present and disjoint from mods and done`() {
        val snap = PositionLayout.snapButton(800, 600)
        val mods = PositionLayout.modsButton(800, 600)
        val done = PositionLayout.doneButton(800, 600)
        assertTrue(snap.width > 0 && snap.height > 0)
        assertTrue(disjoint(snap, mods) && disjoint(snap, done), "snap overlaps a button")
    }

    @Test fun `resize handle sits at the box bottom-right`() {
        val box = Rect(100, 100, 40, 20)
        val h = PositionLayout.resizeHandle(box)
        assertEquals(box.right, h.right); assertEquals(box.bottom, h.bottom)
        assertTrue(h.width in 1..box.width && h.height in 1..box.height)
    }

    private fun disjoint(a: Rect, b: Rect) =
        a.right <= b.left || b.right <= a.left || a.bottom <= b.top || b.bottom <= a.top
}

private fun Rect.cx() = left + width / 2

package gg.snell.mod.menu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Pure-layout regression for the scrollable pickers + the options rail/content (no rendering). */
class ScreenLayoutTest {
    private val w = 520
    private val h = 360

    @Test fun `world footer carries the action ids and scroll clamps`() {
        assertEquals(WorldSelectLayout.FOOTER_IDS, WorldSelectLayout.footerButtons(w, h).map { it.id })
        assertEquals(0, WorldSelectLayout.maxScroll(0, w, h))
        assertTrue(WorldSelectLayout.maxScroll(100, w, h) > 0)
    }

    @Test fun `world visible range stays within the list and is empty when no rows`() {
        assertTrue(WorldSelectLayout.visibleRange(0, 0, w, h).isEmpty())
        val r = WorldSelectLayout.visibleRange(100, 0, w, h)
        assertEquals(0, r.first)
        assertTrue(r.last in 0..99)
    }

    @Test fun `server footer is populated and scroll clamps`() {
        assertEquals(ServerSelectLayout.FOOTER_IDS, ServerSelectLayout.footerButtons(w, h).map { it.id })
        assertEquals(0, ServerSelectLayout.maxScroll(0, w, h))
        assertTrue(ServerSelectLayout.maxScroll(80, w, h) > 0)
        assertEquals(0, ServerSelectLayout.visibleRange(80, 0, w, h).first)
    }

    @Test fun `options rail carries the categories and content rows stack`() {
        assertEquals(OptionsLayout.CATEGORIES, OptionsLayout.railItems(w, h).map { it.id })
        val r0 = OptionsLayout.rowRect(0, 0, w, h)
        val r1 = OptionsLayout.rowRect(1, 0, w, h)
        assertTrue(r1.top > r0.top, "rows flow downward")
        assertTrue(OptionsLayout.controlRect(r0).right <= r0.right, "control stays inside its row")
        assertTrue(OptionsLayout.maxScroll(100, w, h) > 0)
    }
}

package gg.snell.mod.menu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pure-layout regression for the title + pause screens (no rendering). */
class MenuLayoutTest {
    private val w = 480
    private val h = 270

    @Test fun `title nav + foot carry their ids and stack`() {
        val nav = TitleLayout.navButtons(w, h)
        val foot = TitleLayout.footRow(w, h)
        assertEquals(TitleLayout.NAV_IDS, nav.map { it.id })
        assertEquals(TitleLayout.FOOT_IDS, foot.map { it.id })
        for (i in 1 until nav.size) assertTrue(nav[i].rect.top >= nav[i - 1].rect.bottom, "nav stacked: ${nav[i].id}")
        assertTrue(foot.first().rect.top >= nav.last().rect.bottom, "foot below nav")
    }

    @Test fun `title hit maps a nav row and misses the empty corner`() {
        val sp = TitleLayout.navButtons(w, h).first { it.id == "singleplayer" }.rect
        assertEquals("singleplayer", TitleLayout.hit(w, h, sp.left + 2, sp.top + 2))
        assertNull(TitleLayout.hit(w, h, w - 2, h - 2))
    }

    @Test fun `pause controls carry their ids and fit the card`() {
        val c = PauseLayout.controls(w, h)
        val p = PauseLayout.panelRect(w, h)
        assertEquals(PauseLayout.IDS, c.map { it.id })
        assertTrue(c.first().rect.top >= p.top, "first control inside the card")
        assertTrue(c.last().rect.bottom <= p.bottom, "last control inside the card")
    }

    @Test fun `pause hit resolves the save-and-quit button`() {
        val d = PauseLayout.controls(w, h).last().rect
        assertEquals("savequit", PauseLayout.hit(w, h, d.left + d.width / 2, d.top + d.height / 2))
    }
}

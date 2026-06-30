package gg.snell.mod.menu

import gg.snell.mod.ui.node.Layout
import gg.snell.mod.ui.node.Metrics
import gg.snell.mod.ui.node.find
import gg.snell.mod.ui.node.hit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pure-layout regression for the title (node tree) + pause screens (no rendering). */
class MenuLayoutTest {
    private val w = 480
    private val h = 270

    private object FixedMetrics : Metrics {
        override fun textWidth(s: String) = s.length * 6
        override fun monoWidth(s: String) = s.length * 6
        override fun displayWidth(s: String) = s.length * 40
        override val lineHeight = 9
    }

    private fun titleTree() = TitleView.build(TitleData()).also { Layout.layout(it, w, h, FixedMetrics) }

    @Test fun `title nav rows + foot carry ids and stack`() {
        val t = titleTree()
        val discord = t.find("discord")!!.rect
        val sp = t.find("singleplayer")!!.rect
        val mp = t.find("multiplayer")!!.rect
        assertTrue(sp.top >= discord.bottom, "singleplayer below discord")
        assertTrue(mp.top >= sp.bottom, "multiplayer below singleplayer")
        assertTrue(t.find("options")!!.rect.top >= mp.bottom, "foot below nav")
        assertNotNull(t.find("quit"))
    }

    @Test fun `title featured discord card is taller than the plain nav rows`() {
        val t = titleTree()
        assertEquals(46, t.find("discord")!!.rect.height, "featured card height")
        assertTrue(t.find("discord")!!.rect.height > t.find("singleplayer")!!.rect.height)
    }

    @Test fun `title hit maps a nav row and misses the empty centre`() {
        val t = titleTree()
        val sp = t.find("singleplayer")!!.rect
        assertEquals("singleplayer", t.hit(sp.left + 2, sp.top + 2))
        assertNull(t.hit(w / 2, h / 2)) // empty middle
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

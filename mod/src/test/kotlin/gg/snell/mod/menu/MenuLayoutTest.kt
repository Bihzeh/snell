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

/** Pure-layout regression for the title + pause node trees (no rendering). */
class MenuLayoutTest {
    // The ported screens lay out in the 810-tall design space; use a matching 16:9 canvas so the
    // command column (360px, left) leaves the centre empty like it does in-game.
    private val w = 1440
    private val h = 810

    private object FixedMetrics : Metrics {
        override fun textWidth(s: String) = s.length * 6
        override fun monoWidth(s: String) = s.length * 6
        override fun displayWidth(s: String) = s.length * 40
        override val lineHeight = 9
    }

    private fun titleTree() = TitleView.build(TitleData()).also { Layout.layout(it, w, h, FixedMetrics) }
    // Pause lays out in its 810-tall design space (SnellPauseScreen.designH); a matching canvas here.
    private fun pauseTree() = PauseView.build(PauseData("Survival World")).also { Layout.layout(it, 520, 810, FixedMetrics) }

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
        assertEquals(70, t.find("discord")!!.rect.height, "featured card height")
        assertTrue(t.find("discord")!!.rect.height > t.find("singleplayer")!!.rect.height)
    }

    @Test fun `title hit maps a nav row and misses the empty centre`() {
        val t = titleTree()
        val sp = t.find("singleplayer")!!.rect
        assertEquals("singleplayer", t.hit(sp.left + 2, sp.top + 2))
        assertNull(t.hit(w / 2, h / 2)) // empty middle
    }

    @Test fun `pause controls carry their ids and stack inside the card`() {
        val t = pauseTree()
        PauseView.IDS.forEach { assertNotNull(t.find(it), "pause id $it present") }
        val resume = t.find("resume")!!.rect
        val savequit = t.find("savequit")!!.rect
        assertTrue(t.find("quickswitch")!!.rect.top >= resume.bottom, "quick-switch below resume")
        assertTrue(savequit.top >= t.find("statistics")!!.rect.bottom, "save&quit below the grid")
        // 2×2 grid: row 2 below row 1, and the right column right of the left.
        assertTrue(t.find("statistics")!!.rect.top >= t.find("options")!!.rect.bottom, "grid row2 below row1")
        assertTrue(t.find("advancements")!!.rect.left >= t.find("options")!!.rect.right, "advancements right of options")
    }

    @Test fun `pause hit resolves the save-and-quit button`() {
        val t = pauseTree()
        val d = t.find("savequit")!!.rect
        assertEquals("savequit", t.hit(d.left + d.width / 2, d.top + d.height / 2))
    }
}

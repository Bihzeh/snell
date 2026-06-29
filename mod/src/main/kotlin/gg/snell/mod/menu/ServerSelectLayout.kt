package gg.snell.mod.menu

import gg.snell.mod.editor.Control
import gg.snell.mod.editor.Rect

/**
 * Pure layout for the bespoke multiplayer server picker (design: "Snell In-Game Menus"): a centred
 * card with a back-button + title + refresh header, a scrollable server list, and a footer action
 * bar (Join / Add Server / Direct Connect / Cancel). No Minecraft types — unit-testable and
 * headlessly renderable. The canvas has no clipping, so [visibleRange] yields only on-screen rows.
 */
object ServerSelectLayout {
    val FOOTER_IDS = listOf("join", "add", "direct", "cancel")
    const val HEADER_H = 34
    const val FOOTER_H = 32
    const val ROW_H = 40
    const val ROW_GAP = 6
    private const val PAD = 12
    private const val SCROLL_GUTTER = 8
    private val WEIGHTS = mapOf("join" to 7, "add" to 6, "direct" to 7)

    fun panelRect(w: Int, h: Int): Rect {
        val pw = (w - 44).coerceIn(320, 600)
        val ph = (h - 32).coerceIn(220, 660)
        return Rect((w - pw) / 2, (h - ph) / 2, pw, ph)
    }

    fun backButton(w: Int, h: Int): Rect {
        val p = panelRect(w, h)
        return Rect(p.left + PAD, p.top + (HEADER_H - 22) / 2, 22, 22)
    }

    fun refreshButton(w: Int, h: Int): Rect {
        val p = panelRect(w, h)
        return Rect(p.right - PAD - 22, p.top + (HEADER_H - 22) / 2, 22, 22)
    }

    fun listRect(w: Int, h: Int): Rect {
        val p = panelRect(w, h)
        val top = p.top + HEADER_H + PAD
        val bottom = p.bottom - FOOTER_H - PAD
        return Rect(p.left + PAD, top, p.width - 2 * PAD, (bottom - top).coerceAtLeast(0))
    }

    private fun stride() = ROW_H + ROW_GAP
    fun contentHeight(count: Int): Int = if (count <= 0) 0 else count * ROW_H + (count - 1) * ROW_GAP
    fun maxScroll(count: Int, w: Int, h: Int): Int = (contentHeight(count) - listRect(w, h).height).coerceAtLeast(0)

    fun rowRect(index: Int, scrollY: Int, w: Int, h: Int): Rect {
        val l = listRect(w, h)
        return Rect(l.left, l.top + index * stride() - scrollY, l.width - SCROLL_GUTTER, ROW_H)
    }

    fun scrollbarX(w: Int, h: Int): Int = listRect(w, h).right - 3

    fun visibleRange(count: Int, scrollY: Int, w: Int, h: Int): IntRange {
        if (count <= 0) return IntRange.EMPTY
        val l = listRect(w, h); val s = stride()
        val first = (scrollY / s).coerceAtLeast(0)
        val last = ((scrollY + l.height) / s).coerceAtMost(count - 1)
        return if (last < first) IntRange.EMPTY else first..last
    }

    /** Footer bar: a weighted left cluster (join/add/direct) + a right-pinned Cancel. */
    fun footerButtons(w: Int, h: Int): List<Control> {
        val p = panelRect(w, h)
        val barLeft = p.left + PAD; val barRight = p.right - PAD
        val y = p.bottom - FOOTER_H + (FOOTER_H - 24) / 2; val bh = 24; val gap = 6
        val cancelW = 54
        val cancel = Rect(barRight - cancelW, y, cancelW, bh)
        val leftIds = listOf("join", "add", "direct")
        val region = (cancel.left - 12) - barLeft
        val budget = region - (leftIds.size - 1) * gap
        val wsum = leftIds.sumOf { WEIGHTS.getValue(it) }
        val slots = leftIds.map { budget * WEIGHTS.getValue(it) / wsum }.toMutableList()
        slots[slots.lastIndex] = budget - slots.dropLast(1).sum()
        val out = ArrayList<Control>(FOOTER_IDS.size); var x = barLeft
        leftIds.forEachIndexed { i, id -> out += Control(id, Rect(x, y, slots[i], bh)); x += slots[i] + gap }
        out += Control("cancel", cancel)
        return out
    }

    fun footerHit(w: Int, h: Int, mx: Int, my: Int): String? =
        footerButtons(w, h).firstOrNull { it.rect.contains(mx, my) }?.id

    fun rowAt(w: Int, h: Int, scrollY: Int, count: Int, mx: Int, my: Int): Int {
        if (!listRect(w, h).contains(mx, my)) return -1
        for (i in visibleRange(count, scrollY, w, h)) if (rowRect(i, scrollY, w, h).contains(mx, my)) return i
        return -1
    }

    fun hit(w: Int, h: Int, mx: Int, my: Int): String? {
        if (backButton(w, h).contains(mx, my)) return "back"
        if (refreshButton(w, h).contains(mx, my)) return "refresh"
        return footerHit(w, h, mx, my)
    }
}

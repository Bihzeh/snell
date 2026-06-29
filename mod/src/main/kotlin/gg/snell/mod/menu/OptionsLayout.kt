package gg.snell.mod.menu

import gg.snell.mod.editor.Control
import gg.snell.mod.editor.Rect

/**
 * Pure layout for the bespoke Options screen (design: "Snell In-Game Menus"): a large centred card —
 * a header (back + "Options" + Done), a left category rail (Video / Controls / Audio | Mods) and a
 * scrollable content column of full-width rows (label on the left, a control on the right). The
 * content is a flat list the screen builds from the live game options (section headers + option rows
 * share the same [rowRect] cadence). No Minecraft types — unit-testable and headlessly renderable.
 */
object OptionsLayout {
    val CATEGORIES = listOf("video", "controls", "audio", "mods")
    const val HEADER_H = 32
    const val RAIL_W = 104
    const val ROW_H = 26
    const val ROW_GAP = 4
    private const val PAD = 12
    private const val CTRL_W = 150
    private const val SCROLL_GUTTER = 8

    fun panelRect(w: Int, h: Int): Rect {
        val pw = (w - 36).coerceIn(360, 660)
        val ph = (h - 28).coerceIn(240, 700)
        return Rect((w - pw) / 2, (h - ph) / 2, pw, ph)
    }

    fun backButton(w: Int, h: Int): Rect {
        val p = panelRect(w, h)
        return Rect(p.left + PAD, p.top + (HEADER_H - 22) / 2, 22, 22)
    }

    fun doneButton(w: Int, h: Int): Control {
        val p = panelRect(w, h); val bw = 58
        return Control("done", Rect(p.right - PAD - bw, p.top + (HEADER_H - 22) / 2, bw, 22))
    }

    fun railRect(w: Int, h: Int): Rect {
        val p = panelRect(w, h); val top = p.top + HEADER_H
        return Rect(p.left, top, RAIL_W, p.bottom - top)
    }

    /** Category rail items; a 10px gap precedes "mods" (the design's divider). */
    fun railItems(w: Int, h: Int): List<Control> {
        val rail = railRect(w, h); val x = rail.left + 8; val cw = rail.width - 16; val ih = 26
        var y = rail.top + 10
        val out = ArrayList<Control>(CATEGORIES.size)
        for (id in CATEGORIES) {
            if (id == "mods") y += 10
            out += Control(id, Rect(x, y, cw, ih)); y += ih + 4
        }
        return out
    }

    fun contentRect(w: Int, h: Int): Rect {
        val p = panelRect(w, h); val rail = railRect(w, h)
        val left = rail.right + 1 + PAD; val top = p.top + HEADER_H + PAD
        return Rect(left, top, p.right - PAD - left, p.bottom - PAD - top)
    }

    private fun stride() = ROW_H + ROW_GAP
    fun contentHeight(count: Int): Int = if (count <= 0) 0 else count * ROW_H + (count - 1) * ROW_GAP
    fun maxScroll(count: Int, w: Int, h: Int): Int = (contentHeight(count) - contentRect(w, h).height).coerceAtLeast(0)

    fun rowRect(index: Int, scrollY: Int, w: Int, h: Int): Rect {
        val c = contentRect(w, h)
        return Rect(c.left, c.top + index * stride() - scrollY, c.width - SCROLL_GUTTER, ROW_H)
    }

    /** The right-hand control area within a row (toggle / cycle / slider hit-test + draw). */
    fun controlRect(row: Rect): Rect {
        val cw = minOf(CTRL_W, row.width / 2)
        return Rect(row.right - cw, row.top, cw, row.height)
    }

    fun scrollbarX(w: Int, h: Int): Int = contentRect(w, h).right - 3

    fun visibleRange(count: Int, scrollY: Int, w: Int, h: Int): IntRange {
        if (count <= 0) return IntRange.EMPTY
        val c = contentRect(w, h); val s = stride()
        val first = (scrollY / s).coerceAtLeast(0)
        val last = ((scrollY + c.height) / s).coerceAtMost(count - 1)
        return if (last < first) IntRange.EMPTY else first..last
    }

    fun railHit(w: Int, h: Int, mx: Int, my: Int): String? =
        railItems(w, h).firstOrNull { it.rect.contains(mx, my) }?.id

    /** Header hits only (back / done / category). Content-row hits go through [rowRect]/[controlRect]. */
    fun hit(w: Int, h: Int, mx: Int, my: Int): String? {
        if (backButton(w, h).contains(mx, my)) return "back"
        if (doneButton(w, h).rect.contains(mx, my)) return "done"
        return railHit(w, h, mx, my)
    }
}

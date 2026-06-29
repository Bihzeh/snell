package gg.snell.mod.menu

import gg.snell.mod.editor.Control
import gg.snell.mod.editor.Rect

/**
 * Pure layout for the bespoke pause menu (design: "Snell In-Game Menus"): a compact centred card —
 * a brand header (mark + "Paused" + world name), a primary Back-to-Game button, a Quick-Switch row
 * (placeholder), a 2×2 grid (Options / Advancements / Statistics / Open-to-LAN) and a Save & Quit
 * danger button. No Minecraft types — unit-testable and headlessly renderable.
 */
object PauseLayout {
    /** Activatable ids, top→bottom. `quickswitch`/`openToLan` are placeholders; the rest delegate. */
    val IDS = listOf("resume", "quickswitch", "options", "advancements", "statistics", "openToLan", "savequit")

    private const val PAD = 16
    private const val PH = 222

    fun panelRect(w: Int, h: Int): Rect {
        val pw = (w * 0.48f).toInt().coerceIn(220, 300)
        return Rect((w - pw) / 2, (h - PH) / 2, pw, PH)
    }

    /** The header block (mark + Paused eyebrow + world name) at the top of the card. */
    fun headerRect(w: Int, h: Int): Rect {
        val p = panelRect(w, h)
        return Rect(p.left + PAD, p.top + PAD, p.width - 2 * PAD, 24)
    }

    fun controls(w: Int, h: Int): List<Control> {
        val p = panelRect(w, h); val x = p.left + PAD; val cw = p.width - 2 * PAD
        var y = p.top + PAD + 24 + 10
        val out = ArrayList<Control>()
        out += Control("resume", Rect(x, y, cw, 26)); y += 26 + 8
        out += Control("quickswitch", Rect(x, y, cw, 26)); y += 26 + 8
        val gw = (cw - 8) / 2; val gh = 22
        out += Control("options", Rect(x, y, gw, gh))
        out += Control("advancements", Rect(x + gw + 8, y, cw - gw - 8, gh)); y += gh + 8
        out += Control("statistics", Rect(x, y, gw, gh))
        out += Control("openToLan", Rect(x + gw + 8, y, cw - gw - 8, gh)); y += gh + 8
        out += Control("savequit", Rect(x, y, cw, 24))
        return out
    }

    fun hit(w: Int, h: Int, mx: Int, my: Int): String? =
        controls(w, h).firstOrNull { it.rect.contains(mx, my) }?.id
}

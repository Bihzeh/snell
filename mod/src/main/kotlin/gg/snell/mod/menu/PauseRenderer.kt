package gg.snell.mod.menu

import gg.snell.mod.editor.Rect
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.ui.SnellBtn
import gg.snell.mod.ui.SnellUi
import gg.snell.shared.SnellPalette

/**
 * Pure renderer for the bespoke pause menu (design: "Snell In-Game Menus") — a scrim over the world
 * and a compact card: slipstream mark + world name, a primary Back-to-Game, a Quick-Switch row, a
 * 2×2 grid (Options / Advancements / Statistics / Open-to-LAN) and a Save & Quit danger button.
 * Icons are real Tabler glyphs.
 */
object PauseRenderer {
    fun render(canvas: EditorCanvas, w: Int, h: Int, mouseX: Int, mouseY: Int, worldName: String = "World") {
        SnellUi.scrim(canvas, w, h)
        val p = PauseLayout.panelRect(w, h)
        SnellUi.panel(canvas, p)

        val hd = PauseLayout.headerRect(w, h)
        SnellUi.slipstream(canvas, hd.left, hd.top, 18)
        val tx = hd.left + 24
        SnellUi.sectionLabel(canvas, tx, hd.top, "Paused")
        canvas.drawText(tx, hd.top + canvas.lineHeight + 3, SnellUi.ellipsize(canvas, worldName, hd.right - tx), SnellPalette.text)

        for (c in PauseLayout.controls(w, h)) {
            val hover = c.rect.contains(mouseX, mouseY)
            when (c.id) {
                "resume" -> SnellUi.button(canvas, c.rect, "Back to Game", SnellBtn.Primary, hover, iconName = "play")
                "quickswitch" -> quickSwitchRow(canvas, c.rect, hover)
                "savequit" -> SnellUi.button(canvas, c.rect, "Save & Quit to Title", SnellBtn.Danger, hover, iconName = "quit")
                else -> {
                    val (label, ic) = when (c.id) {
                        "options" -> "Options" to "options"
                        "advancements" -> "Advancements" to "advancements"
                        "statistics" -> "Statistics" to "statistics"
                        else -> "Open to LAN" to "lan"
                    }
                    SnellUi.button(canvas, c.rect, label, SnellBtn.Secondary, hover, iconName = ic)
                }
            }
        }
    }

    /** The accented Quick-Switch row (placeholder action): icon tile + title/subtitle + chevron. */
    private fun quickSwitchRow(canvas: EditorCanvas, r: Rect, hover: Boolean) {
        canvas.fill(r.left, r.top, r.width, r.height, if (hover) SnellPalette.withAlpha(SnellPalette.accent, 0x1C) else SnellPalette.accentSubtle)
        canvas.border(r.left, r.top, r.width, r.height, SnellPalette.withAlpha(SnellPalette.accent, if (hover) 0x66 else 0x40))
        SnellUi.round(canvas, r, SnellPalette.menuPanel)
        val t = r.height - 10
        val tile = Rect(r.left + 5, r.top + 5, t, t)
        SnellUi.iconTile(canvas, tile, SnellPalette.withAlpha(SnellPalette.accent, 0x22))
        SnellUi.icon(canvas, "quickswitch", tile.left + tile.width / 2, tile.top + tile.height / 2, t - 2, SnellPalette.accent)
        val tx = tile.right + 7
        canvas.drawText(tx, r.top + 5, "Quick Switch", SnellPalette.text)
        canvas.drawText(tx, r.top + 5 + canvas.lineHeight + 1, SnellUi.ellipsize(canvas, "Jump to another server or world", r.right - 14 - tx), SnellPalette.text2)
        SnellUi.icon(canvas, "chevron", r.right - 9, r.top + r.height / 2, 9, SnellPalette.menuText3)
    }
}

package gg.snell.mod.menu

import gg.snell.mod.editor.Rect
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.ui.SnellBtn
import gg.snell.mod.ui.SnellUi
import gg.snell.shared.SnellPalette

/**
 * Pure renderer for the bespoke pause menu (design: "Snell In-Game Menus") — a scrim over the world
 * and a compact card: brand header + world name, a primary Back-to-Game, a Quick-Switch row, a 2×2
 * grid (Options / Advancements / Statistics / Open-to-LAN) and a Save & Quit danger button.
 */
object PauseRenderer {
    fun render(canvas: EditorCanvas, w: Int, h: Int, mouseX: Int, mouseY: Int, worldName: String = "World") {
        SnellUi.scrim(canvas, w, h)
        val p = PauseLayout.panelRect(w, h)
        SnellUi.panel(canvas, p)

        val hd = PauseLayout.headerRect(w, h)
        var by = hd.top + 2
        listOf(10 to 0x1AA0D9, 13 to 0x00D9FF, 8 to 0x0E6FA8).forEach { (bw, rgb) ->
            canvas.fill(hd.left, by, bw, 3, 0xFF000000.toInt() or rgb); by += 4
        }
        val tx = hd.left + 20
        SnellUi.sectionLabel(canvas, tx, hd.top, "Paused")
        canvas.drawText(tx, hd.top + canvas.lineHeight + 3, SnellUi.ellipsize(canvas, worldName, hd.right - tx), SnellPalette.text)

        for (c in PauseLayout.controls(w, h)) {
            val hover = c.rect.contains(mouseX, mouseY)
            when (c.id) {
                "resume" -> {
                    SnellUi.button(canvas, c.rect, "Back to Game", SnellBtn.Primary, hover)
                    SnellUi.playGlyph(canvas, c.rect.left + 16, c.rect.top + c.rect.height / 2, 7, SnellPalette.onAccent)
                }
                "quickswitch" -> quickSwitchRow(canvas, c.rect, hover)
                "savequit" -> SnellUi.button(canvas, c.rect, "Save & Quit to Title", SnellBtn.Danger, hover)
                else -> {
                    val label = when (c.id) {
                        "options" -> "Options"; "advancements" -> "Advancements"; "statistics" -> "Statistics"; else -> "Open to LAN"
                    }
                    SnellUi.button(canvas, c.rect, label, SnellBtn.Secondary, hover)
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
        canvas.fill(tile.left + 3, tile.top + tile.height / 2 - 2, tile.width - 6, 1, SnellPalette.accent)
        canvas.fill(tile.left + 3, tile.top + tile.height / 2 + 2, tile.width - 6, 1, SnellPalette.accent)
        val tx = tile.right + 7
        canvas.drawText(tx, r.top + 5, "Quick Switch", SnellPalette.text)
        canvas.drawText(tx, r.top + 5 + canvas.lineHeight + 1, SnellUi.ellipsize(canvas, "Jump to another server or world", r.right - 12 - tx), SnellPalette.text2)
        SnellUi.chevronRight(canvas, r.right - 10, r.top + r.height / 2, 3, SnellPalette.menuText3)
    }
}

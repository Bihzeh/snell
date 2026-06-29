package gg.snell.mod.menu

import gg.snell.mod.editor.Rect
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.ui.SnellBtn
import gg.snell.mod.ui.SnellUi
import gg.snell.shared.SnellPalette

/**
 * Pure renderer for the bespoke singleplayer world picker (design: "Snell In-Game Menus") — scrim +
 * card, a back/title/search header, the visible slice of the world list (grid-textured icon, name +
 * mode pill, meta, mono detail, selection check), a scrollbar, and the footer action bar. Rows are
 * drawn first, then the header/footer bands are repainted on top to mask any overflow (no clipping).
 */
object WorldSelectRenderer {
    fun render(
        canvas: EditorCanvas, w: Int, h: Int, mouseX: Int, mouseY: Int,
        rows: List<WorldRow>, selected: Int, scrollY: Int, search: String, searchFocused: Boolean,
    ) {
        SnellUi.scrim(canvas, w, h)
        val p = WorldSelectLayout.panelRect(w, h)
        SnellUi.panel(canvas, p)

        val list = WorldSelectLayout.listRect(w, h)
        if (rows.isEmpty()) {
            emptyState(canvas, list, search.isNotEmpty())
        } else {
            for (i in WorldSelectLayout.visibleRange(rows.size, scrollY, w, h)) {
                val r = WorldSelectLayout.rowRect(i, scrollY, w, h)
                if (r.bottom <= list.top || r.top >= list.bottom) continue
                row(canvas, r, rows[i], i == selected, r.contains(mouseX, mouseY))
            }
            SnellUi.scrollbar(canvas, WorldSelectLayout.scrollbarX(w, h), list.top, list.height, WorldSelectLayout.contentHeight(rows.size), scrollY)
        }

        // --- header band (masks overflow above the list) ---
        canvas.fill(p.left + 1, p.top + 1, p.width - 2, list.top - (p.top + 1), SnellPalette.menuPanel)
        val back = WorldSelectLayout.backButton(w, h)
        SnellUi.squareButton(canvas, back, back.contains(mouseX, mouseY))
        SnellUi.icon(canvas, "back", back.left + back.width / 2, back.top + back.height / 2, 12, SnellPalette.text)
        val titleX = back.right + 9
        canvas.drawText(titleX, p.top + 6, "Singleplayer", SnellPalette.text)
        canvas.drawText(titleX, p.top + 6 + canvas.lineHeight + 1, "${rows.size} ${if (rows.size == 1) "world" else "worlds"}", SnellPalette.menuText3)
        SnellUi.textField(canvas, WorldSelectLayout.searchRect(w, h), search, searchFocused, "Search worlds")
        SnellUi.divider(canvas, p.left + 1, p.top + WorldSelectLayout.HEADER_H, p.width - 2)

        // --- footer band (masks overflow below the list) ---
        canvas.fill(p.left + 1, list.bottom, p.width - 2, p.bottom - 1 - list.bottom, SnellPalette.menuPanel)
        SnellUi.divider(canvas, p.left + 1, p.bottom - WorldSelectLayout.FOOTER_H, p.width - 2)
        val hasSel = selected in rows.indices
        for (c in WorldSelectLayout.footerButtons(w, h)) {
            val style = when (c.id) { "play" -> SnellBtn.Primary; "delete" -> SnellBtn.Danger; "cancel" -> SnellBtn.Ghost; else -> SnellBtn.Secondary }
            val enabled = when (c.id) { "play", "edit", "delete" -> hasSel; else -> true }
            val label = when (c.id) { "play" -> "Play"; "create" -> "Create New"; "edit" -> "Edit"; "delete" -> "Delete"; else -> "Cancel" }
            val ic = when (c.id) { "play" -> "play"; "create" -> "create"; "edit" -> "edit"; "delete" -> "delete"; else -> null }
            SnellUi.button(canvas, c.rect, label, style, hover = enabled && c.rect.contains(mouseX, mouseY), enabled = enabled, iconName = ic)
        }
    }

    private fun row(canvas: EditorCanvas, r: Rect, world: WorldRow, selected: Boolean, hover: Boolean) {
        SnellUi.listRow(canvas, r, selected, hover)
        val ic = r.height - 8
        val icon = Rect(r.left + 5, r.top + (r.height - ic) / 2, ic, ic)
        canvas.fill(icon.left, icon.top, icon.width, icon.height, SnellPalette.withAlpha(SnellPalette.accent, 0x18))
        canvas.border(icon.left, icon.top, icon.width, icon.height, SnellPalette.withAlpha(SnellPalette.accent, 0x44))
        canvas.fill(icon.left, icon.top + icon.height / 2, icon.width, 1, SnellPalette.withAlpha(SnellUi.WHITE, 0x10))
        canvas.fill(icon.left + icon.width / 2, icon.top, 1, icon.height, SnellPalette.withAlpha(SnellUi.WHITE, 0x10))
        SnellUi.round(canvas, icon, SnellPalette.menuPanel)

        val tx = icon.right + 8
        if (selected) SnellUi.icon(canvas, "check", r.right - 12, r.top + r.height / 2, 11, SnellPalette.accent)
        val nameMaxW = (r.width * 0.42f).toInt()
        val name = SnellUi.ellipsize(canvas, world.name, nameMaxW)
        canvas.drawText(tx, r.top + 5, name, SnellPalette.text)
        SnellUi.chip(canvas, tx + canvas.textWidth(name) + 6, r.top + 4, world.mode, modeColor(world.mode))
        val textW = r.right - 18 - tx
        canvas.drawText(tx, r.top + 5 + canvas.lineHeight + 2, SnellUi.ellipsize(canvas, world.meta, textW), SnellPalette.text2)
        canvas.drawText(tx, r.top + 5 + 2 * canvas.lineHeight + 3, SnellUi.ellipsize(canvas, world.detail, textW), SnellPalette.menuText3)
    }

    private fun modeColor(mode: String): Int = when {
        mode.startsWith("Hard", true) -> SnellPalette.danger
        mode.startsWith("Creat", true) -> SnellPalette.info
        mode.startsWith("Spect", true) -> SnellPalette.menuText3
        else -> SnellPalette.accent
    }

    private fun emptyState(canvas: EditorCanvas, list: Rect, filtered: Boolean) {
        val title = if (filtered) "No worlds match your search" else "No worlds yet"
        val hint = if (filtered) "Try a different name." else "Create a new world to get started."
        val cy = list.top + list.height / 2
        canvas.drawText(list.left + (list.width - canvas.textWidth(title)) / 2, cy - canvas.lineHeight, title, SnellPalette.text2)
        canvas.drawText(list.left + (list.width - canvas.textWidth(hint)) / 2, cy + 2, hint, SnellPalette.menuText3)
    }
}

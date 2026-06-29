package gg.snell.mod.menu

import gg.snell.mod.editor.Rect
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.ui.SnellBtn
import gg.snell.mod.ui.SnellUi
import gg.snell.shared.SnellPalette

/**
 * Pure renderer for the bespoke Options screen (design: "Snell In-Game Menus") — backdrop + card, a
 * back/title/Done header, a left category rail (Video / Controls / Audio | Mods) and a scrollable
 * content column of section headers + option rows (label/description on the left, a toggle / cycle /
 * slider on the right). Rows draw first; header + rail repaint on top to mask overflow.
 */
object OptionsRenderer {
    private val categoryLabels = mapOf("video" to "Video", "controls" to "Controls", "audio" to "Audio", "mods" to "Mods")

    fun render(
        canvas: EditorCanvas, w: Int, h: Int, mouseX: Int, mouseY: Int,
        entries: List<OptionEntry>, activeCategory: String, scrollY: Int,
    ) {
        SnellUi.backdrop(canvas, w, h)
        val p = OptionsLayout.panelRect(w, h)
        SnellUi.panel(canvas, p)

        val content = OptionsLayout.contentRect(w, h)
        for (i in OptionsLayout.visibleRange(entries.size, scrollY, w, h)) {
            val rr = OptionsLayout.rowRect(i, scrollY, w, h)
            if (rr.bottom <= content.top || rr.top >= content.bottom) continue
            when (val e = entries[i]) {
                is OptionEntry.Section -> SnellUi.sectionLabel(canvas, rr.left, rr.top + (rr.height - canvas.lineHeight) / 2, e.label)
                is OptionEntry.Item -> optionRow(canvas, rr, e.item, rr.contains(mouseX, mouseY))
            }
        }
        SnellUi.scrollbar(canvas, OptionsLayout.scrollbarX(w, h), content.top, content.height, OptionsLayout.contentHeight(entries.size), scrollY)

        // mask overflow above/below the content column, then lay header + rail on top.
        canvas.fill(content.left, p.top + 1, p.right - 1 - content.left, content.top - (p.top + 1), SnellPalette.menuPanel)
        canvas.fill(content.left, content.bottom, p.right - 1 - content.left, p.bottom - 1 - content.bottom, SnellPalette.menuPanel)

        // header
        val back = OptionsLayout.backButton(w, h)
        SnellUi.squareButton(canvas, back, back.contains(mouseX, mouseY))
        SnellUi.chevronLeft(canvas, back.left + back.width / 2, back.top + back.height / 2, 3, SnellPalette.text)
        canvas.drawText(back.right + 9, p.top + (OptionsLayout.HEADER_H - canvas.lineHeight) / 2, "Options", SnellPalette.text)
        val done = OptionsLayout.doneButton(w, h)
        SnellUi.button(canvas, done.rect, "Done", SnellBtn.Primary, done.rect.contains(mouseX, mouseY))
        SnellUi.divider(canvas, p.left + 1, p.top + OptionsLayout.HEADER_H, p.width - 2)

        // rail
        val rail = OptionsLayout.railRect(w, h)
        canvas.fill(rail.left + 1, rail.top, rail.width - 1, rail.height - 1, SnellPalette.withAlpha(SnellUi.WHITE, 0x04))
        canvas.fill(rail.right, rail.top, 1, rail.height, SnellUi.rowBorder)
        for (c in OptionsLayout.railItems(w, h)) {
            val active = c.id == activeCategory
            SnellUi.categoryItem(canvas, c.rect, active, c.rect.contains(mouseX, mouseY))
            canvas.drawText(c.rect.left + 10, c.rect.top + (c.rect.height - canvas.lineHeight) / 2, categoryLabels[c.id] ?: c.id, if (active) SnellPalette.text else SnellPalette.text2)
        }
    }

    private fun optionRow(canvas: EditorCanvas, rr: Rect, item: OptionItem, hover: Boolean) {
        SnellUi.listRow(canvas, rr, selected = false, hover = hover)
        val ctrl = OptionsLayout.controlRect(rr)
        val labelX = rr.left + 8
        val labelMaxW = ctrl.left - 6 - labelX
        val hasDesc = item.description.isNotEmpty()
        if (hasDesc) {
            canvas.drawText(labelX, rr.top + 4, SnellUi.ellipsize(canvas, item.label, labelMaxW), SnellPalette.text)
            canvas.drawText(labelX, rr.top + 4 + canvas.lineHeight + 1, SnellUi.ellipsize(canvas, item.description, labelMaxW), SnellPalette.menuText3)
        } else {
            canvas.drawText(labelX, rr.top + (rr.height - canvas.lineHeight) / 2 + 1, SnellUi.ellipsize(canvas, item.label, labelMaxW), SnellPalette.text)
        }
        when (item.kind) {
            OptionKind.Toggle -> {
                val sw = 24; val sh = 12
                SnellUi.switch(canvas, Rect(ctrl.right - sw, ctrl.top + (ctrl.height - sh) / 2, sw, sh), item.on)
            }
            OptionKind.Cycle -> {
                val bh = 16
                val r = Rect(ctrl.left, ctrl.top + (ctrl.height - bh) / 2, ctrl.width, bh)
                SnellUi.button(canvas, r, SnellUi.ellipsize(canvas, item.valueText, ctrl.width - 16), SnellBtn.Secondary, hover)
                SnellUi.chevronDown(canvas, r.right - 8, r.top + r.height / 2, 2, SnellPalette.menuText3)
            }
            OptionKind.Slider -> SnellUi.slider(canvas, Rect(ctrl.left, ctrl.top, ctrl.width, ctrl.height), item.fraction, item.valueText)
        }
    }
}

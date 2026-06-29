package gg.snell.mod.menu

import gg.snell.mod.editor.Rect
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.ui.PillRole
import gg.snell.mod.ui.SnellBtn
import gg.snell.mod.ui.SnellUi
import gg.snell.shared.SnellPalette

/**
 * Pure renderer for the bespoke multiplayer server picker (design: "Snell In-Game Menus") — scrim +
 * card, a back/title/refresh header, the visible slice of the server list (initial tile, name, motd,
 * players + ping + signal bars / status pill), a scrollbar, and the footer action bar. Rows draw
 * first; the header/footer bands repaint on top to mask overflow.
 */
object ServerSelectRenderer {
    fun render(canvas: EditorCanvas, w: Int, h: Int, mouseX: Int, mouseY: Int, rows: List<ServerRow>, selected: Int, scrollY: Int) {
        SnellUi.scrim(canvas, w, h)
        val p = ServerSelectLayout.panelRect(w, h)
        SnellUi.panel(canvas, p)

        val list = ServerSelectLayout.listRect(w, h)
        if (rows.isEmpty()) {
            emptyState(canvas, list)
        } else {
            for (i in ServerSelectLayout.visibleRange(rows.size, scrollY, w, h)) {
                val r = ServerSelectLayout.rowRect(i, scrollY, w, h)
                if (r.bottom <= list.top || r.top >= list.bottom) continue
                drawRow(canvas, r, rows[i], i == selected, mouseX, mouseY)
            }
            SnellUi.scrollbar(canvas, ServerSelectLayout.scrollbarX(w, h), list.top, list.height, ServerSelectLayout.contentHeight(rows.size), scrollY)
        }

        // header band
        canvas.fill(p.left + 1, p.top + 1, p.width - 2, list.top - (p.top + 1), SnellPalette.menuPanel)
        val back = ServerSelectLayout.backButton(w, h)
        SnellUi.squareButton(canvas, back, back.contains(mouseX, mouseY))
        SnellUi.icon(canvas, "back", back.left + back.width / 2, back.top + back.height / 2, 12, SnellPalette.text)
        val titleX = back.right + 9
        canvas.drawText(titleX, p.top + 6, "Multiplayer", SnellPalette.text)
        canvas.drawText(titleX, p.top + 6 + canvas.lineHeight + 1, "${rows.size} ${if (rows.size == 1) "server" else "servers"}", SnellPalette.menuText3)
        val refresh = ServerSelectLayout.refreshButton(w, h)
        SnellUi.squareButton(canvas, refresh, refresh.contains(mouseX, mouseY))
        SnellUi.icon(canvas, "refresh", refresh.left + refresh.width / 2, refresh.top + refresh.height / 2, 12, SnellPalette.text2)
        SnellUi.divider(canvas, p.left + 1, p.top + ServerSelectLayout.HEADER_H, p.width - 2)

        // footer band
        canvas.fill(p.left + 1, list.bottom, p.width - 2, p.bottom - 1 - list.bottom, SnellPalette.menuPanel)
        SnellUi.divider(canvas, p.left + 1, p.bottom - ServerSelectLayout.FOOTER_H, p.width - 2)
        val hasSel = selected in rows.indices
        for (c in ServerSelectLayout.footerButtons(w, h)) {
            val style = when (c.id) { "join" -> SnellBtn.Primary; "cancel" -> SnellBtn.Ghost; else -> SnellBtn.Secondary }
            val enabled = if (c.id == "join") hasSel else true
            val label = when (c.id) { "join" -> "Join"; "add" -> "Add Server"; "direct" -> "Direct Connect"; else -> "Cancel" }
            val ic = when (c.id) { "join" -> "play"; "add" -> "add"; "direct" -> "link"; else -> null }
            SnellUi.button(canvas, c.rect, label, style, hover = enabled && c.rect.contains(mouseX, mouseY), enabled = enabled, iconName = ic)
        }
    }

    private fun drawRow(canvas: EditorCanvas, r: Rect, row: ServerRow, selected: Boolean, mouseX: Int, mouseY: Int) {
        SnellUi.listRow(canvas, r, selected, r.contains(mouseX, mouseY))
        val ts = r.height - 10
        val tile = Rect(r.left + 5, r.top + 5, ts, ts)
        val tint = initialColor(row.name)
        SnellUi.iconTile(canvas, tile, SnellPalette.withAlpha(tint, 0x33), SnellPalette.withAlpha(tint, 0x66))
        val glyph = row.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        canvas.drawText(tile.left + (tile.width - canvas.textWidth(glyph)) / 2, tile.top + (tile.height - canvas.lineHeight) / 2 + 1, glyph, tint)

        val tx = tile.right + 8
        val rx = r.right - 8
        val nameMaxW = (rx - 60) - tx
        canvas.drawText(tx, r.top + 6, SnellUi.ellipsize(canvas, row.name, nameMaxW), SnellPalette.text)
        val sub = row.motd.ifEmpty { row.address }
        canvas.drawText(tx, r.top + 6 + canvas.lineHeight + 2, SnellUi.ellipsize(canvas, sub, nameMaxW), SnellPalette.text2)

        when (row.status) {
            ServerStatus.Online -> {
                if (row.players.isNotEmpty()) canvas.drawText(rx - canvas.textWidth(row.players), r.top + 6, row.players, SnellPalette.text)
                val pc = pingColor(row.ping)
                drawBars(canvas, rx - 16, r.top + r.height - 9, pc, barsFor(row.ping))
                if (row.ping >= 0) {
                    val pt = "${row.ping}ms"
                    canvas.drawText(rx - 18 - canvas.textWidth(pt), r.top + r.height - 12, pt, pc)
                }
            }
            ServerStatus.Offline -> rightPill(canvas, r, rx, "Offline", PillRole.Offline)
            ServerStatus.Pinging -> rightPill(canvas, r, rx, "Pinging", PillRole.Neutral)
        }
    }

    private fun rightPill(canvas: EditorCanvas, r: Rect, rx: Int, text: String, role: PillRole) {
        val pw = canvas.textWidth(text) + 16
        SnellUi.pill(canvas, rx - pw, r.top + (r.height - (canvas.lineHeight + 6)) / 2, text, role)
    }

    private fun drawBars(canvas: EditorCanvas, x: Int, baseY: Int, color: Int, lit: Int) {
        for (i in 0..3) {
            val bh = 2 + i * 2
            canvas.fill(x + i * 4, baseY - bh, 2, bh, if (i < lit) color else SnellPalette.withAlpha(SnellUi.WHITE, 0x16))
        }
    }

    private fun barsFor(ping: Int): Int = when {
        ping < 0 -> 0; ping < 60 -> 4; ping < 120 -> 3; ping < 250 -> 2; else -> 1
    }

    private fun pingColor(ping: Int): Int = when {
        ping < 0 -> SnellPalette.menuText3; ping < 80 -> SnellPalette.accent; ping < 180 -> SnellPalette.gold; else -> SnellPalette.danger
    }

    /** A stable tint per server, keyed off the name's first character. */
    private fun initialColor(name: String): Int {
        val palette = intArrayOf(SnellPalette.accent, SnellPalette.gold, SnellPalette.info, SnellPalette.ember, SnellPalette.success)
        val c = name.trim().firstOrNull()?.code ?: 0
        return palette[c % palette.size]
    }

    private fun emptyState(canvas: EditorCanvas, list: Rect) {
        val title = "No servers yet"
        val hint = "Use \"Add Server\" to add one."
        val cy = list.top + list.height / 2
        canvas.drawText(list.left + (list.width - canvas.textWidth(title)) / 2, cy - canvas.lineHeight, title, SnellPalette.text2)
        canvas.drawText(list.left + (list.width - canvas.textWidth(hint)) / 2, cy + 2, hint, SnellPalette.menuText3)
    }
}

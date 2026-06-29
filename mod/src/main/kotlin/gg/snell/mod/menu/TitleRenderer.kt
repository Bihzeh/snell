package gg.snell.mod.menu

import gg.snell.mod.editor.LogoArt
import gg.snell.mod.editor.Rect
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.ui.SnellBtn
import gg.snell.mod.ui.SnellUi
import gg.snell.shared.SnellPalette

/**
 * Pure renderer for the bespoke title screen (design: "Snell In-Game Menus") — a left command column
 * (brand lockup, Discord/Singleplayer/Multiplayer nav rows, Options/Quit), top-right quick actions +
 * account chip, and a bottom-right "What's new" card. Every pixel goes through [EditorCanvas].
 */
object TitleRenderer {
    fun render(
        canvas: EditorCanvas, w: Int, h: Int, mouseX: Int, mouseY: Int,
        version: String = "26.2", username: String = "Player", statusLabel: String = "Online", crowns: String = "0",
    ) {
        SnellUi.backdrop(canvas, w, h)
        lockup(canvas, TitleLayout.logoRect(w, h))

        for (c in TitleLayout.navButtons(w, h)) {
            val hover = c.rect.contains(mouseX, mouseY)
            val (title, sub) = when (c.id) {
                "discord" -> "Link your Discord" to "Free cosmetics, role perks & party sync"
                "singleplayer" -> "Singleplayer" to "Create or load a world"
                else -> "Multiplayer" to "Join a server"
            }
            val tileColor = if (c.id == "discord") SnellPalette.discord else SnellPalette.accent
            val tile = SnellUi.navButton(canvas, c.rect, tileColor, title, sub, hover, accent = c.id != "discord")
            navGlyph(canvas, c.id, tile, tileColor)
        }

        for (c in TitleLayout.footRow(w, h)) {
            val hover = c.rect.contains(mouseX, mouseY)
            val label = if (c.id == "options") "Options" else "Quit Game"
            SnellUi.button(canvas, c.rect, label, if (c.id == "quit") SnellBtn.Danger else SnellBtn.Secondary, hover)
        }

        for (c in TitleLayout.topActions(w, h)) {
            val hover = c.rect.contains(mouseX, mouseY)
            val r = c.rect
            when (c.id) {
                "wallet" -> {
                    SnellUi.squareButton(canvas, r, hover)
                    SnellUi.dot(canvas, r.left + 8, r.top + r.height / 2, 5, SnellPalette.gold)
                    canvas.drawText(r.left + 14, r.top + (r.height - canvas.lineHeight) / 2, crowns, SnellPalette.gold)
                }
                "cosmetics" -> { SnellUi.squareButton(canvas, r, hover); SnellUi.plusGlyph(canvas, r.left + r.width / 2, r.top + r.height / 2, 6, SnellPalette.text2) }
                "friends" -> { SnellUi.squareButton(canvas, r, hover); SnellUi.dot(canvas, r.left + r.width / 2, r.top + r.height / 2, 4, SnellPalette.text2) }
            }
        }
        accountChip(canvas, TitleLayout.accountChip(w, h), username, statusLabel)
        whatsNew(canvas, TitleLayout.whatsNewRect(w, h), version)

        canvas.drawText(22, h - 13, "SNELL $version  ·  Minecraft 26.2 · Fabric", SnellPalette.menuText3)
    }

    /** The real Snell brand mark (gold-crown gem, via [LogoArt]) beside the scaled SNELL wordmark. */
    private fun lockup(canvas: EditorCanvas, r: Rect) {
        val markSize = r.height
        val mark = Rect(r.left, r.top, markSize, markSize)
        for (b in LogoArt.bands(mark)) canvas.fill(b.rect.left, b.rect.top, b.rect.width, b.rect.height, b.color)
        SnellUi.heading(canvas, mark.right + 9, r.top + 6, "SNELL", 2.0f)
    }

    /** A clean minimal glyph inside a nav row's icon tile (no SVG primitives in-game). */
    private fun navGlyph(canvas: EditorCanvas, id: String, tile: Rect, color: Int) {
        val cx = tile.left + tile.width / 2; val cy = tile.top + tile.height / 2
        when (id) {
            "discord" -> { // rounded chat bubble + two eyes
                val bw = tile.width - 6; val bh = tile.height - 7
                val b = Rect(tile.left + 3, tile.top + 3, bw, bh)
                canvas.fill(b.left, b.top, b.width, b.height, color)
                SnellUi.round(canvas, b, SnellPalette.menuPanel)
                SnellUi.dot(canvas, cx - 2, cy - 1, 2, SnellPalette.menuPanel); SnellUi.dot(canvas, cx + 2, cy - 1, 2, SnellPalette.menuPanel)
            }
            "singleplayer" -> { // a single person: head + shoulders
                SnellUi.dot(canvas, cx, cy - 3, 4, color)
                val sh = Rect(cx - 4, cy + 1, 8, 5)
                canvas.fill(sh.left, sh.top, sh.width, sh.height, color); SnellUi.round(canvas, sh, SnellPalette.menuPanel)
            }
            "multiplayer" -> { // a group: three heads
                SnellUi.dot(canvas, cx - 4, cy + 1, 3, color); SnellUi.dot(canvas, cx + 4, cy + 1, 3, color)
                SnellUi.dot(canvas, cx, cy - 2, 4, color)
            }
        }
    }

    private fun accountChip(canvas: EditorCanvas, r: Rect, username: String, status: String) {
        canvas.fill(r.left, r.top, r.width, r.height, SnellUi.rowFill)
        canvas.border(r.left, r.top, r.width, r.height, SnellUi.rowBorder)
        SnellUi.round(canvas, r, SnellPalette.menuBase)
        val sk = r.height - 8
        val skin = Rect(r.left + 4, r.top + 4, sk, sk)
        canvas.fill(skin.left, skin.top, skin.width, skin.height, SnellPalette.menuInset)
        canvas.border(skin.left, skin.top, skin.width, skin.height, SnellPalette.outline)
        val tx = skin.right + 5
        canvas.drawText(tx, r.top + 4, SnellUi.ellipsize(canvas, username, r.right - 4 - tx), SnellPalette.text)
        SnellUi.dot(canvas, tx + 2, r.bottom - 7, 3, SnellPalette.accent)
        canvas.drawText(tx + 7, r.bottom - 11, status, SnellPalette.accent)
    }

    private fun whatsNew(canvas: EditorCanvas, r: Rect, version: String) {
        canvas.fill(r.left, r.top, r.width, r.height, SnellPalette.withAlpha(SnellPalette.gold, 0x12))
        canvas.border(r.left, r.top, r.width, r.height, SnellPalette.withAlpha(SnellPalette.gold, 0x40))
        SnellUi.round(canvas, r, SnellPalette.menuBase)
        SnellUi.dot(canvas, r.left + 8, r.top + 8, 4, SnellPalette.gold)
        canvas.drawText(r.left + 14, r.top + 5, "WHAT'S NEW · $version", SnellPalette.gold)
        val (l1, l2) = wrap2(canvas, "Sodium 0.6 rebuilt for 26.2, a new keystroke HUD, and faster cold-start.", r.width - 16)
        canvas.drawText(r.left + 8, r.top + 18, l1, SnellPalette.text2)
        if (l2.isNotEmpty()) canvas.drawText(r.left + 8, r.top + 18 + canvas.lineHeight + 1, SnellUi.ellipsize(canvas, l2, r.width - 16), SnellPalette.text2)
    }

    /** Greedy word-wrap into (first line, remainder) for the given pixel width. */
    private fun wrap2(canvas: EditorCanvas, text: String, maxW: Int): Pair<String, String> {
        if (canvas.textWidth(text) <= maxW) return text to ""
        val words = text.split(' '); val sb = StringBuilder(); var i = 0
        while (i < words.size && canvas.textWidth(("$sb ${words[i]}").trim()) <= maxW) {
            if (sb.isNotEmpty()) sb.append(' '); sb.append(words[i]); i++
        }
        return sb.toString() to words.drop(i).joinToString(" ")
    }
}

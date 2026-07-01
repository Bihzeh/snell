package gg.snell.mod.menu

import gg.snell.mod.editor.Rect
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.ui.PillRole
import gg.snell.mod.ui.SnellBtn
import gg.snell.mod.ui.SnellUi
import gg.snell.mod.ui.node.Anchor
import gg.snell.mod.ui.node.Cross
import gg.snell.mod.ui.node.Dir
import gg.snell.mod.ui.node.Edge
import gg.snell.mod.ui.node.Lazy
import gg.snell.mod.ui.node.Len
import gg.snell.mod.ui.node.Node
import gg.snell.mod.ui.node.find
import gg.snell.mod.ui.node.spacer
import gg.snell.shared.SnellPalette

/** Minecraft-free state the server picker renders (the screen maps vanilla ServerData/pings onto it). */
data class ServerState(
    val rows: List<ServerRow>,
    val selected: Int = -1,
    val scrollY: Int = 0,
)

/**
 * Declarative multiplayer server picker (design "Snell In-Game Menus"): a centred panel — a
 * back/title/refresh header band, a virtualized server list (initial tile, name/motd/address, players +
 * ping + signal bars or a status pill), and a footer action bar. Authored in the 810-tall design space
 * at mockup ×0.75, replacing ServerSelectLayout/ServerSelectRenderer. Same Stack band-masking as
 * [WorldView] (no scissor clipping).
 */
object ServerView {
    val FOOTER_IDS = listOf("join", "add", "direct", "cancel")
    const val ROW_H = 66      // mockup 88 (60 icon + 2×14 pad) ×0.75
    const val ROW_GAP = 8
    private const val HEADER_H = 72
    private const val FOOTER_H = 69
    private const val LIST_PAD_X = 16
    private const val LIST_PAD_Y = 12

    fun build(s: ServerState): Node = Node(
        dir = Dir.Stack, width = Len.Flex(), height = Len.Flex(),
        children = listOf(panel(s)),
    )

    fun contentHeight(count: Int): Int = if (count <= 0) 0 else count * ROW_H + (count - 1) * ROW_GAP

    fun maxScroll(laid: Node, count: Int): Int {
        val list = laid.find("list") ?: return 0
        return (contentHeight(count) - (list.rect.height - 2 * LIST_PAD_Y)).coerceAtLeast(0)
    }

    private fun panel(s: ServerState) = Node(
        anchor = Anchor.Center, width = Len.Fixed(810), height = Len.Fixed(660), dir = Dir.Stack,
        paint = { c, r, _, _ -> SnellUi.panel(c, r) },
        children = listOf(
            Node(
                dir = Dir.Column, width = Len.Flex(), height = Len.Flex(), cross = Cross.Stretch,
                children = listOf(Node(height = Len.Fixed(HEADER_H)), list(s), Node(height = Len.Fixed(FOOTER_H))),
            ),
            header(s),
            footer(s),
        ),
    )

    // ---- header band ----------------------------------------------------------------------------
    private fun header(s: ServerState) = Node(
        anchor = Anchor.TopLeft, width = Len.Flex(), height = Len.Fixed(HEADER_H),
        dir = Dir.Row, cross = Cross.Center, gap = 13, padding = Edge(l = 22, r = 22),
        paint = { c, r, _, _ ->
            c.fill(r.left + 1, r.top + 1, r.width - 2, r.height - 1, SnellPalette.menuPanel)
            SnellUi.divider(c, r.left + 1, r.bottom, r.width - 2)
        },
        children = listOf(
            squareIcon("back", "back", SnellPalette.text),
            Node(
                width = Len.Flex(), height = Len.Fixed(33), // content-tight so Cross.Center truly centres the ink
                paint = { c, r, _, _ ->
                    SnellUi.heading(c, r.left, r.top, "Multiplayer", pixelHeight = 18, letterSpacingEm = 0.02f)
                    val n = s.rows.size
                    c.drawMono(r.left, r.top + 24, "$n ${if (n == 1) "server" else "servers"}", SnellPalette.menuText3)
                },
            ),
            squareIcon("refresh", "refresh", SnellPalette.text2),
        ),
    )

    private fun squareIcon(id: String, icon: String, tint: Int) = Node(
        id = id, width = Len.Fixed(33), height = Len.Fixed(33),
        paint = { c, r, mx, my ->
            SnellUi.squareButton(c, r, r.contains(mx, my))
            SnellUi.icon(c, icon, r.left + r.width / 2, r.top + r.height / 2, 15, tint)
        },
    )

    // ---- list -----------------------------------------------------------------------------------
    private fun list(s: ServerState) = Node(
        id = "list", height = Len.Flex(), clip = true,
        padding = Edge(LIST_PAD_X, LIST_PAD_Y, LIST_PAD_X, LIST_PAD_Y),
        lazy = Lazy(s.rows.size, ROW_H, ROW_GAP, s.scrollY) { i -> row(s, i) },
        paint = { c, r, _, _ ->
            if (s.rows.isEmpty()) {
                emptyState(c, r)
            } else {
                SnellUi.scrollbar(c, r.right - 8, r.top + LIST_PAD_Y, r.height - 2 * LIST_PAD_Y, contentHeight(s.rows.size), s.scrollY)
            }
        },
    )

    private fun row(s: ServerState, i: Int) = Node(
        id = "row:$i",
        paint = { c, r, mx, my -> serverRow(c, r, s.rows[i], i == s.selected, r.contains(mx, my)) },
    )

    private fun serverRow(c: EditorCanvas, r: Rect, row: ServerRow, selected: Boolean, hover: Boolean) {
        SnellUi.listRow(c, r, selected, hover)
        val ts = 45 // mockup 60 icon tile
        val tile = Rect(r.left + 10, r.top + (r.height - ts) / 2, ts, ts)
        val tint = initialColor(row.name)
        SnellUi.iconTile(c, tile, SnellPalette.withAlpha(tint, 0x33), SnellPalette.withAlpha(tint, 0x66))
        val glyph = row.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        c.drawText(tile.left + (tile.width - c.textWidth(glyph)) / 2, tile.top + (tile.height - c.lineHeight) / 2 + 1, glyph, tint)

        val tx = tile.right + 13
        val rx = r.right - 13
        val nameMaxW = (rx - 90) - tx
        // Optically centre the text block against the tile (+2: MC text ink rides high in its box).
        // No MOTD -> a 2-line row (name + address); the 3-line form printed the address twice.
        val ty: Int
        if (row.motd.isNotEmpty()) {
            ty = r.top + (r.height - (3 * c.lineHeight + 8)) / 2 + 2
            c.drawText(tx, ty, SnellUi.ellipsize(c, row.name, nameMaxW), SnellPalette.text)
            c.drawText(tx, ty + c.lineHeight + 4, SnellUi.ellipsize(c, row.motd, nameMaxW), SnellPalette.text2)
            c.drawMono(tx, ty + 2 * c.lineHeight + 8, SnellUi.ellipsize(c, row.address, nameMaxW), SnellPalette.menuText3)
        } else {
            ty = r.top + (r.height - (2 * c.lineHeight + 4)) / 2 + 2
            c.drawText(tx, ty, SnellUi.ellipsize(c, row.name, nameMaxW), SnellPalette.text)
            c.drawMono(tx, ty + c.lineHeight + 4, SnellUi.ellipsize(c, row.address, nameMaxW), SnellPalette.menuText3)
        }

        when (row.status) {
            ServerStatus.Online -> {
                if (row.players.isNotEmpty()) c.drawMono(rx - c.monoWidth(row.players), ty, row.players, SnellPalette.text)
                val pc = pingColor(row.ping)
                drawBars(c, rx - 20, r.top + r.height - 14, pc, barsFor(row.ping))
                if (row.ping >= 0) {
                    val pt = "${row.ping}ms"
                    // -21 bottom-aligns the mono ink with the signal bars' baseline (at -18 it hung below).
                    c.drawMono(rx - 24 - c.monoWidth(pt), r.top + r.height - 21, pt, pc)
                }
            }
            ServerStatus.Offline -> rightPill(c, r, rx, "Offline", PillRole.Offline)
            ServerStatus.Pinging -> rightPill(c, r, rx, "Pinging", PillRole.Neutral)
        }
    }

    private fun rightPill(c: EditorCanvas, r: Rect, rx: Int, text: String, role: PillRole) {
        val pw = c.textWidth(text) + 21 // must match SnellUi.pill's width
        SnellUi.pill(c, rx - pw, r.top + (r.height - (c.lineHeight + 6)) / 2, text, role)
    }

    private fun drawBars(c: EditorCanvas, x: Int, baseY: Int, color: Int, lit: Int) {
        for (i in 0..3) {
            val bh = 3 + i * 3 // mockup bar heights 7/11/15/19 ×0.75
            c.fill(x + i * 5, baseY - bh, 3, bh, if (i < lit) color else SnellPalette.withAlpha(SnellUi.WHITE, 0x16))
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
        val ch = name.trim().firstOrNull()?.code ?: 0
        return palette[ch % palette.size]
    }

    private fun emptyState(c: EditorCanvas, list: Rect) {
        val title = "No servers yet"
        val hint = "Use \"Add Server\" to add one."
        val cy = list.top + list.height / 2
        c.drawText(list.left + (list.width - c.textWidth(title)) / 2, cy - c.lineHeight, title, SnellPalette.text2)
        c.drawText(list.left + (list.width - c.textWidth(hint)) / 2, cy + 2, hint, SnellPalette.menuText3)
    }

    // ---- footer band ----------------------------------------------------------------------------
    private fun footer(s: ServerState) = Node(
        anchor = Anchor.BottomLeft, width = Len.Flex(), height = Len.Fixed(FOOTER_H),
        dir = Dir.Row, cross = Cross.Center, gap = 9, padding = Edge(l = 21, r = 21),
        paint = { c, r, _, _ ->
            c.fill(r.left + 1, r.top, r.width - 2, r.height - 1, SnellPalette.menuPanel)
            SnellUi.divider(c, r.left + 1, r.top, r.width - 2)
        },
        children = run {
            val hasSel = s.selected in s.rows.indices
            listOf(
                footBtn("join", "Join Server", "play", SnellBtn.Primary, 115, hasSel), // ~19px side pads like the other primaries
                footBtn("add", "Add Server", "add", SnellBtn.Secondary, 105, true),
                footBtn("direct", "Direct Connect", "link", SnellBtn.Secondary, 125, true),
                spacer(),
                footBtn("cancel", "Cancel", null, SnellBtn.Ghost, 66, true),
            )
        },
    )

    private fun footBtn(id: String, label: String, icon: String?, style: SnellBtn, w: Int, enabled: Boolean) = Node(
        id = id, width = Len.Fixed(w), height = Len.Fixed(39),
        paint = { c, r, mx, my -> SnellUi.button(c, r, label, style, hover = enabled && r.contains(mx, my), enabled = enabled, iconName = icon) },
    )
}

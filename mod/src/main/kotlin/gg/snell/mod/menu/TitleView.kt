package gg.snell.mod.menu

import gg.snell.mod.editor.Point
import gg.snell.mod.editor.Rect
import gg.snell.mod.editor.Size
import gg.snell.mod.ui.SnellBtn
import gg.snell.mod.ui.SnellUi
import gg.snell.mod.ui.node.Anchor
import gg.snell.mod.ui.node.Cross
import gg.snell.mod.ui.node.Dir
import gg.snell.mod.ui.node.Edge
import gg.snell.mod.ui.node.Len
import gg.snell.mod.ui.node.Node
import gg.snell.mod.ui.node.spacer
import gg.snell.shared.SnellPalette

/** Pre-formatted, Minecraft-free data the title view renders (the screen fills it from real game state). */
data class TitleData(
    val modVersion: String = "0.0.0",
    val mcVersion: String = "26.2",
    val username: String = "Player",
    val status: String = "Online",
    val crowns: String = "0",
    val singleplayerSub: String = "Create or load a world",
    val multiplayerSub: String = "Join a server",
    val whatsNew: String = "Sodium 0.6 rebuilt for 26.2, a new keystroke HUD, and faster cold-start.",
)

/**
 * Declarative title screen: a left command column (wordmark, featured Discord card, nav rows,
 * Options/Quit), top-right wallet + actions + account chip, bottom-right What's-new, bottom-left
 * version. The featured card is a real Row[icon | flex middle | link] so the title gets the leftover
 * width and stops truncating to "Li…". Pure (no MC types) -> unit-testable + preview-renderable.
 */
object TitleView {
    private const val M = 22

    fun build(d: TitleData): Node = Node(
        dir = Dir.Stack, width = Len.Flex(), height = Len.Flex(), padding = Edge.all(M),
        children = listOf(commandColumn(d), topActions(d), whatsNew(d), footerLine(d)),
    )

    // ---- left command column ------------------------------------------------------------------
    private fun commandColumn(d: TitleData) = Node(
        anchor = Anchor.TopLeft, width = Len.Frac(0.46f, 200, 280), height = Len.Flex(),
        dir = Dir.Column, cross = Cross.Stretch, gap = 7,
        children = listOf(
            wordmark(),
            spacer(),
            featuredDiscord(),
            navRow("singleplayer", "Singleplayer", d.singleplayerSub),
            navRow("multiplayer", "Multiplayer", d.multiplayerSub),
            footRow(),
        ),
    )

    private fun wordmark() = Node(
        height = Len.Fixed(30),
        paint = { c, r, _, _ ->
            SnellUi.slipstream(c, r.left, r.top, r.height)
            val px = (r.height * 0.78f).toInt()
            SnellUi.heading(c, r.left + r.height + 12, r.top + (r.height - px) / 2, "SNELL", pixelHeight = px)
        },
    )

    /** Featured Discord card as a subtree so the layout engine sizes title vs badge vs link (no hand-math collision). */
    private fun featuredDiscord(): Node {
        val brand = SnellPalette.discord
        return Node(
            id = "discord", height = Len.Fixed(46),
            dir = Dir.Row, cross = Cross.Center, gap = 10, padding = Edge(8, 7, 8, 7),
            paint = { c, r, mx, my ->
                val hover = r.contains(mx, my)
                c.fill(r.left + 4, r.bottom, r.width - 8, 4, SnellPalette.withAlpha(brand, 0x33))
                SnellUi.surface(c, r, SnellPalette.withAlpha(brand, if (hover) 0x3A else 0x2A), SnellPalette.withAlpha(brand, if (hover) 0x8C else 0x73))
            },
            children = listOf(
                Node( // solid brand icon tile
                    width = Len.Fixed(32), height = Len.Fixed(32), anchor = Anchor.CenterLeft,
                    paint = { c, r, _, _ ->
                        SnellUi.iconTile(c, r, brand, SnellUi.lighten(brand, 0.2f))
                        SnellUi.icon(c, "discord", r.left + r.width / 2, r.top + r.height / 2, r.width - 8, SnellUi.WHITE)
                    },
                ),
                Node( // middle: title + REWARDS over subtitle. The engine sizes this to the leftover
                    // width (icon + link are Fixed); the title/badge sub-layout is done in one paint
                    // against the known r.width (robust — avoids starving a nested flex child).
                    width = Len.Flex(), height = Len.Fixed(24), anchor = Anchor.Center,
                    paint = { c, r, _, _ ->
                        val bw = c.textWidth("REWARDS") + 12
                        val titleMax = (r.width - bw - 6).coerceAtLeast(10)
                        val title = SnellUi.ellipsize(c, "Link your Discord", titleMax)
                        c.drawText(r.left, r.top + 1, title, SnellPalette.text)
                        SnellUi.badge(c, r.left + c.textWidth(title) + 6, r.top, "REWARDS", brand)
                        c.drawText(r.left, r.top + 1 + c.lineHeight + 3, SnellUi.ellipsize(c, "Free cosmetics, role perks & party sync", r.width), SnellPalette.text2)
                    },
                ),
                Node( // Link CTA
                    width = Len.Fixed(46), height = Len.Fixed(20), anchor = Anchor.Center,
                    paint = { c, r, mx, my -> SnellUi.solidButton(c, r, "Link", brand, SnellUi.WHITE, r.contains(mx, my), "chevron") },
                ),
            ),
        )
    }

    private fun navRow(id: String, title: String, sub: String) = Node(
        id = id, height = Len.Fixed(30),
        paint = { c, r, mx, my ->
            val tile = SnellUi.navButton(c, r, SnellPalette.accent, title, sub, r.contains(mx, my))
            SnellUi.icon(c, id, tile.left + tile.width / 2, tile.top + tile.height / 2, tile.width - 5, SnellPalette.accent)
        },
    )

    private fun footRow() = Node(
        dir = Dir.Row, height = Len.Fixed(22), gap = 8, cross = Cross.Stretch,
        children = listOf(
            footButton("options", "Options", "options", SnellBtn.Secondary),
            footButton("quit", "Quit Game", "quit", SnellBtn.Danger),
        ),
    )

    private fun footButton(id: String, label: String, ic: String, style: SnellBtn) = Node(
        id = id, width = Len.Flex(1),
        paint = { c, r, mx, my -> SnellUi.button(c, r, label, style, r.contains(mx, my), iconName = ic) },
    )

    // ---- top-right cluster --------------------------------------------------------------------
    private fun topActions(d: TitleData) = Node(
        anchor = Anchor.TopRight, dir = Dir.Row, gap = 6, cross = Cross.Center, height = Len.Fixed(26),
        children = listOf(
            Node(id = "wallet", width = Len.Fixed(52), height = Len.Fixed(18), paint = { c, r, mx, my -> SnellUi.walletPill(c, r, d.crowns, r.contains(mx, my)) }),
            squareAction("cosmetics"),
            squareAction("friends"),
            Node(width = Len.Fixed(92), height = Len.Fixed(26), paint = { c, r, _, _ -> accountChip(c, r, d.username, d.status) }),
        ),
    )

    private fun squareAction(id: String) = Node(
        id = id, width = Len.Fixed(18), height = Len.Fixed(18),
        paint = { c, r, mx, my ->
            SnellUi.squareButton(c, r, r.contains(mx, my))
            SnellUi.icon(c, id, r.left + r.width / 2, r.top + r.height / 2, 12, SnellPalette.text2)
        },
    )

    private fun accountChip(c: gg.snell.mod.platform.EditorCanvas, r: Rect, username: String, status: String) {
        SnellUi.surface(c, r, SnellUi.rowFill, SnellUi.rowBorder)
        val sk = r.height - 6
        val avatar = Rect(r.left + 3, r.top + 3, sk, sk)
        SnellUi.surface(c, avatar, SnellPalette.menuInset, SnellPalette.outline)
        val initial = username.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        c.drawText(avatar.left + (avatar.width - c.textWidth(initial)) / 2, avatar.top + (avatar.height - c.lineHeight) / 2 + 1, initial, SnellPalette.accent)
        val sc = SnellUi.statusColor(status)
        SnellUi.dot(c, avatar.right - 2, avatar.bottom - 2, 6, SnellPalette.menuPanel)
        SnellUi.dot(c, avatar.right - 2, avatar.bottom - 2, 4, sc)
        val tx = avatar.right + 6
        val maxW = r.right - 6 - tx
        c.drawText(tx, r.top + 4, SnellUi.ellipsize(c, username, maxW), SnellPalette.text)
        c.drawText(tx, r.top + 4 + c.lineHeight, SnellUi.ellipsize(c, status, maxW), sc)
    }

    // ---- bottom-right / bottom-left -----------------------------------------------------------
    private fun whatsNew(d: TitleData) = Node(
        anchor = Anchor.BottomRight, width = Len.Frac(0.30f, 180, 300), height = Len.Fixed(52),
        paint = { c, r, _, _ ->
            SnellUi.surface(c, r, SnellPalette.withAlpha(SnellPalette.gold, 0x12), SnellPalette.withAlpha(SnellPalette.gold, 0x48))
            SnellUi.dot(c, r.left + 10, r.top + 9, 7, SnellPalette.gold)
            c.drawText(r.left + 17, r.top + 5, "WHAT'S NEW · ${d.modVersion}".uppercase(), SnellPalette.gold)
            val (l1, l2) = wrap2(c, d.whatsNew, r.width - 18)
            c.drawText(r.left + 9, r.top + 5 + c.lineHeight + 4, l1, SnellPalette.text)
            if (l2.isNotEmpty()) c.drawText(r.left + 9, r.top + 5 + 2 * c.lineHeight + 5, SnellUi.ellipsize(c, l2, r.width - 18), SnellPalette.text)
        },
    )

    private fun footerLine(d: TitleData) = Node(
        anchor = Anchor.BottomLeft, height = Len.Fixed(9), offset = Point(0, 7),
        paint = { c, r, _, _ ->
            var x = r.left
            val head = "SNELL ${d.modVersion}"
            c.drawMono(x, r.top, head, SnellPalette.text2)
            x += c.monoWidth(head) + 7
            SnellUi.dot(c, x, r.top + c.lineHeight / 2, 3, SnellPalette.gold)
            x += 7
            c.drawMono(x, r.top, "Minecraft ${d.mcVersion} · Fabric", SnellPalette.menuText3)
        },
    )

    /** Greedy word-wrap into (first line, remainder) for [maxW] px. */
    private fun wrap2(c: gg.snell.mod.platform.EditorCanvas, text: String, maxW: Int): Pair<String, String> {
        if (c.textWidth(text) <= maxW) return text to ""
        val words = text.split(' '); val sb = StringBuilder(); var i = 0
        while (i < words.size && c.textWidth(("$sb ${words[i]}").trim()) <= maxW) {
            if (sb.isNotEmpty()) sb.append(' '); sb.append(words[i]); i++
        }
        return sb.toString() to words.drop(i).joinToString(" ")
    }
}

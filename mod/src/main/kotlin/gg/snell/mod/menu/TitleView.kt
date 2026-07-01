package gg.snell.mod.menu

import gg.snell.mod.editor.Point
import gg.snell.mod.editor.Rect
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
 *
 * Sizes are the mockup's 1920×1080 geometry ×0.75, laid out in the 810-tall design space
 * (SnellTitleScreen.designH) so text + chrome read at the mockup density instead of the old zoomed 270.
 */
object TitleView {
    // Root insets (mockup ×0.75): the command column starts 72px in from the left, the top-right
    // cluster 30px in; anchored corners (what's-new / version) inset by the bottom/side pads.
    private val PAD = Edge(l = 72, t = 44, r = 30, b = 36)

    fun build(d: TitleData): Node = Node(
        dir = Dir.Stack, width = Len.Flex(), height = Len.Flex(), padding = PAD,
        children = listOf(commandColumn(d), topActions(d), whatsNew(d), footerLine(d)),
    )

    // ---- left command column ------------------------------------------------------------------
    private fun commandColumn(d: TitleData) = Node(
        anchor = Anchor.TopLeft, width = Len.Fixed(360), height = Len.Flex(),
        // Reserve a bottom band so the Options/Quit row clears the anchored version footer.
        dir = Dir.Column, cross = Cross.Stretch, gap = 10, padding = Edge(b = 18),
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
        height = Len.Fixed(46),
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
            id = "discord", height = Len.Fixed(70),
            dir = Dir.Row, cross = Cross.Center, gap = 13, padding = Edge(15, 12, 15, 12),
            paint = { c, r, mx, my ->
                val hover = r.contains(mx, my)
                c.fill(r.left + 4, r.bottom, r.width - 8, 4, SnellPalette.withAlpha(brand, 0x33))
                SnellUi.surface(c, r, SnellPalette.withAlpha(brand, if (hover) 0x3A else 0x2A), SnellPalette.withAlpha(brand, if (hover) 0x8C else 0x73))
            },
            children = listOf(
                Node( // solid brand icon tile
                    width = Len.Fixed(40), height = Len.Fixed(40), anchor = Anchor.CenterLeft,
                    paint = { c, r, _, _ ->
                        SnellUi.iconTile(c, r, brand, SnellUi.lighten(brand, 0.2f))
                        SnellUi.icon(c, "discord", r.left + r.width / 2, r.top + r.height / 2, r.width - 12, SnellUi.WHITE)
                    },
                ),
                Node( // middle: title + REWARDS over subtitle. The engine sizes this to the leftover
                    // width (icon + link are Fixed); the title/badge sub-layout is done in one paint
                    // against the known r.width (robust — avoids starving a nested flex child).
                    width = Len.Flex(), height = Len.Fixed(34), anchor = Anchor.Center,
                    paint = { c, r, _, _ ->
                        // Line 1: the title gets the full width (priority — never collapses to "…").
                        c.drawText(r.left, r.top + 1, SnellUi.ellipsize(c, "Link your Discord", r.width), SnellPalette.text)
                        // Line 2: REWARDS badge then the subtitle in the remaining width.
                        val by = r.top + 1 + c.lineHeight + 3
                        val bw = SnellUi.badge(c, r.left, by - 1, "REWARDS", brand)
                        c.drawText(r.left + bw + 6, by, SnellUi.ellipsize(c, "Free cosmetics, role perks & party sync", (r.width - bw - 6).coerceAtLeast(0)), SnellPalette.text2)
                    },
                ),
                Node( // compact Link CTA (chevron square) — keeps the title room in the narrow column
                    width = Len.Fixed(32), height = Len.Fixed(28), anchor = Anchor.Center,
                    paint = { c, r, mx, my ->
                        SnellUi.surface(c, r, if (r.contains(mx, my)) SnellUi.lighten(brand, 0.12f) else brand, null)
                        SnellUi.icon(c, "chevron", r.left + r.width / 2, r.top + r.height / 2, 13, SnellUi.WHITE)
                    },
                ),
            ),
        )
    }

    private fun navRow(id: String, title: String, sub: String) = Node(
        id = id, height = Len.Fixed(56),
        paint = { c, r, mx, my ->
            val tile = SnellUi.navButton(c, r, SnellPalette.accent, title, sub, r.contains(mx, my))
            SnellUi.icon(c, id, tile.left + tile.width / 2, tile.top + tile.height / 2, tile.width - 8, SnellPalette.accent)
        },
    )

    private fun footRow() = Node(
        dir = Dir.Row, height = Len.Fixed(36), gap = 11, cross = Cross.Stretch,
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
        anchor = Anchor.TopRight, dir = Dir.Row, gap = 9, cross = Cross.Center, height = Len.Fixed(35),
        children = listOf(
            Node(id = "wallet", width = Len.Fixed(62), height = Len.Fixed(35), paint = { c, r, mx, my -> SnellUi.walletPill(c, r, d.crowns, r.contains(mx, my)) }),
            squareAction("cosmetics"),
            squareAction("friends"),
            Node(width = Len.Fixed(150), height = Len.Fixed(35), paint = { c, r, _, _ -> accountChip(c, r, d.username, d.status) }),
        ),
    )

    private fun squareAction(id: String) = Node(
        id = id, width = Len.Fixed(35), height = Len.Fixed(35),
        paint = { c, r, mx, my ->
            SnellUi.squareButton(c, r, r.contains(mx, my))
            SnellUi.icon(c, id, r.left + r.width / 2, r.top + r.height / 2, 16, SnellPalette.text2)
        },
    )

    private fun accountChip(c: gg.snell.mod.platform.EditorCanvas, r: Rect, username: String, status: String) {
        SnellUi.surface(c, r, SnellUi.rowFill, SnellUi.rowBorder)
        val sk = r.height - 8
        val avatar = Rect(r.left + 4, r.top + 4, sk, sk)
        SnellUi.surface(c, avatar, SnellPalette.menuInset, SnellPalette.outline)
        val initial = username.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        c.drawText(avatar.left + (avatar.width - c.textWidth(initial)) / 2, avatar.top + (avatar.height - c.lineHeight) / 2 + 1, initial, SnellPalette.accent)
        val sc = SnellUi.statusColor(status)
        SnellUi.dot(c, avatar.right - 2, avatar.bottom - 2, 7, SnellPalette.menuPanel)
        SnellUi.dot(c, avatar.right - 2, avatar.bottom - 2, 5, sc)
        val tx = avatar.right + 7
        val maxW = r.right - 7 - tx
        c.drawText(tx, r.top + 6, SnellUi.ellipsize(c, username, maxW), SnellPalette.text)
        c.drawText(tx, r.top + 6 + c.lineHeight, SnellUi.ellipsize(c, status, maxW), sc)
    }

    // ---- bottom-right / bottom-left -----------------------------------------------------------
    private fun whatsNew(d: TitleData) = Node(
        anchor = Anchor.BottomRight, width = Len.Frac(0.26f, 220, 320), height = Len.Fixed(66),
        paint = { c, r, _, _ ->
            SnellUi.surface(c, r, SnellPalette.withAlpha(SnellPalette.gold, 0x12), SnellPalette.withAlpha(SnellPalette.gold, 0x48))
            SnellUi.dot(c, r.left + 12, r.top + 11, 7, SnellPalette.gold)
            c.drawText(r.left + 20, r.top + 7, "WHAT'S NEW · ${d.modVersion}".uppercase(), SnellPalette.gold)
            val (l1, l2) = wrap2(c, d.whatsNew, r.width - 22)
            c.drawText(r.left + 11, r.top + 7 + c.lineHeight + 5, l1, SnellPalette.text)
            if (l2.isNotEmpty()) c.drawText(r.left + 11, r.top + 7 + 2 * c.lineHeight + 6, SnellUi.ellipsize(c, l2, r.width - 22), SnellPalette.text)
        },
    )

    private fun footerLine(d: TitleData) = Node(
        anchor = Anchor.BottomLeft, height = Len.Fixed(12), offset = Point(0, 8),
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

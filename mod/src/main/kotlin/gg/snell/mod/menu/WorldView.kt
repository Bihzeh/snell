package gg.snell.mod.menu

import gg.snell.mod.editor.Rect
import gg.snell.mod.platform.EditorCanvas
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

/** Minecraft-free state the world picker renders (the screen fills it from the real save list). */
data class WorldState(
    val rows: List<WorldRow>,
    val selected: Int = -1,
    val scrollY: Int = 0,
    val search: String = "",
    val searchFocused: Boolean = false,
)

/**
 * Declarative singleplayer world picker (design "Snell In-Game Menus"): a centred panel — a
 * back/title/search header band, a virtualized world list, and a footer action bar. Authored in the
 * 810-tall design space at the mockup's 1920×1080 geometry ×0.75 (like TitleView/PauseView), replacing
 * the old zoomed WorldSelectLayout/WorldSelectRenderer pair. The panel is a Stack so the header/footer
 * bands paint AFTER the rows and mask their overflow (the canvas has no scissor clipping).
 */
object WorldView {
    val FOOTER_IDS = listOf("play", "create", "edit", "delete", "cancel")
    const val ROW_H = 76      // mockup 102 (74 tile + 2×14 pad) ×0.75
    const val ROW_GAP = 8     // mockup 10
    private const val HEADER_H = 72   // mockup 96 (44 back + 2×26 pad)
    private const val FOOTER_H = 69   // mockup 92 (52 button + 2×20 pad)
    private const val LIST_PAD_X = 16 // mockup 22
    private const val LIST_PAD_Y = 12 // mockup 16

    fun build(s: WorldState): Node = Node(
        dir = Dir.Stack, width = Len.Flex(), height = Len.Flex(),
        children = listOf(panel(s)),
    )

    fun contentHeight(count: Int): Int = if (count <= 0) 0 else count * ROW_H + (count - 1) * ROW_GAP

    /** Max scroll for [count] rows against the laid tree's list viewport. */
    fun maxScroll(laid: Node, count: Int): Int {
        val list = laid.find("list") ?: return 0
        return (contentHeight(count) - (list.rect.height - 2 * LIST_PAD_Y)).coerceAtLeast(0)
    }

    private fun panel(s: WorldState) = Node(
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

    // ---- header band (paints over row overflow) -------------------------------------------------
    private fun header(s: WorldState) = Node(
        anchor = Anchor.TopLeft, width = Len.Flex(), height = Len.Fixed(HEADER_H),
        dir = Dir.Row, cross = Cross.Center, gap = 13, padding = Edge(l = 22, r = 22),
        paint = { c, r, _, _ ->
            c.fill(r.left + 1, r.top + 1, r.width - 2, r.height - 1, SnellPalette.menuPanel)
            SnellUi.divider(c, r.left + 1, r.bottom, r.width - 2)
        },
        children = listOf(
            backButton(),
            Node(
                width = Len.Flex(), height = Len.Fixed(33), // content-tight so Cross.Center truly centres the ink
                paint = { c, r, _, _ ->
                    SnellUi.heading(c, r.left, r.top, "Singleplayer", pixelHeight = 18, letterSpacingEm = 0.02f)
                    val n = s.rows.size
                    c.drawMono(r.left, r.top + 24, "$n ${if (n == 1) "world" else "worlds"}", SnellPalette.menuText3)
                },
            ),
            Node(
                id = "search", width = Len.Fixed(210), height = Len.Fixed(33),
                paint = { c, r, _, _ -> SnellUi.textField(c, r, s.search, s.searchFocused, "Search worlds") },
            ),
        ),
    )

    // ---- list -----------------------------------------------------------------------------------
    private fun list(s: WorldState) = Node(
        id = "list", height = Len.Flex(), clip = true,
        padding = Edge(LIST_PAD_X, LIST_PAD_Y, LIST_PAD_X, LIST_PAD_Y),
        lazy = Lazy(s.rows.size, ROW_H, ROW_GAP, s.scrollY) { i -> row(s, i) },
        paint = { c, r, _, _ ->
            if (s.rows.isEmpty()) {
                emptyState(c, r, s.search.isNotEmpty())
            } else {
                SnellUi.scrollbar(c, r.right - 8, r.top + LIST_PAD_Y, r.height - 2 * LIST_PAD_Y, contentHeight(s.rows.size), s.scrollY)
            }
        },
    )

    private fun row(s: WorldState, i: Int) = Node(
        id = "row:$i",
        paint = { c, r, mx, my -> worldRow(c, r, s.rows[i], i == s.selected, r.contains(mx, my)) },
    )

    private fun worldRow(c: EditorCanvas, r: Rect, world: WorldRow, selected: Boolean, hover: Boolean) {
        SnellUi.listRow(c, r, selected, hover)
        val ts = 56 // mockup 74 thumbnail
        val tile = Rect(r.left + 10, r.top + (r.height - ts) / 2, ts, ts)
        // 9-slice tile like the server rows (the old fill+border+corner-knock triple is deprecated),
        // with the world-grid hairlines drawn over it.
        SnellUi.iconTile(c, tile, SnellPalette.withAlpha(SnellPalette.accent, 0x18), SnellPalette.withAlpha(SnellPalette.accent, 0x44))
        c.fill(tile.left, tile.top + tile.height / 2, tile.width, 1, SnellPalette.withAlpha(SnellUi.WHITE, 0x10))
        c.fill(tile.left + tile.width / 2, tile.top, 1, tile.height, SnellPalette.withAlpha(SnellUi.WHITE, 0x10))

        val tx = tile.right + 13
        if (selected) SnellUi.icon(c, "check", r.right - 18, r.top + r.height / 2, 14, SnellPalette.accent)
        val nameMaxW = (r.width * 0.42f).toInt()
        val name = SnellUi.ellipsize(c, world.name, nameMaxW)
        // Optically centre the 3-line block against the tile (+2: MC text ink rides high in its box).
        val block = 3 * c.lineHeight + 8
        val ty = r.top + (r.height - block) / 2 + 2
        c.drawText(tx, ty, name, SnellPalette.text)
        SnellUi.chip(c, tx + c.textWidth(name) + 8, ty - 1, world.mode, modeColor(world.mode))
        val textW = r.right - 26 - tx
        c.drawText(tx, ty + c.lineHeight + 4, SnellUi.ellipsize(c, world.meta, textW), SnellPalette.text2)
        c.drawMono(tx, ty + 2 * c.lineHeight + 8, SnellUi.ellipsize(c, world.detail, textW), SnellPalette.menuText3)
    }

    private fun modeColor(mode: String): Int = when {
        mode.startsWith("Hard", true) -> SnellPalette.danger
        mode.startsWith("Creat", true) -> SnellPalette.info
        mode.startsWith("Spect", true) -> SnellPalette.menuText3
        else -> SnellPalette.accent
    }

    private fun emptyState(c: EditorCanvas, list: Rect, filtered: Boolean) {
        val title = if (filtered) "No worlds match your search" else "No worlds yet"
        val hint = if (filtered) "Try a different name." else "Create a new world to get started."
        val cy = list.top + list.height / 2
        c.drawText(list.left + (list.width - c.textWidth(title)) / 2, cy - c.lineHeight, title, SnellPalette.text2)
        c.drawText(list.left + (list.width - c.textWidth(hint)) / 2, cy + 2, hint, SnellPalette.menuText3)
    }

    // ---- footer band ----------------------------------------------------------------------------
    private fun footer(s: WorldState) = Node(
        anchor = Anchor.BottomLeft, width = Len.Flex(), height = Len.Fixed(FOOTER_H),
        dir = Dir.Row, cross = Cross.Center, gap = 9, padding = Edge(l = 21, r = 21),
        paint = { c, r, _, _ ->
            c.fill(r.left + 1, r.top, r.width - 2, r.height - 1, SnellPalette.menuPanel)
            SnellUi.divider(c, r.left + 1, r.top, r.width - 2)
        },
        children = run {
            val hasSel = s.selected in s.rows.indices
            listOf(
                footBtn("play", "Play Selected World", "play", SnellBtn.Primary, 165, hasSel),
                footBtn("create", "Create New", "create", SnellBtn.Secondary, 105, true),
                footBtn("edit", "Edit", "edit", SnellBtn.Secondary, 66, hasSel),
                footBtn("delete", "Delete", "delete", SnellBtn.Danger, 78, hasSel),
                spacer(),
                footBtn("cancel", "Cancel", null, SnellBtn.Ghost, 66, true),
            )
        },
    )

    private fun footBtn(id: String, label: String, icon: String?, style: SnellBtn, w: Int, enabled: Boolean) = Node(
        id = id, width = Len.Fixed(w), height = Len.Fixed(39),
        paint = { c, r, mx, my -> SnellUi.button(c, r, label, style, hover = enabled && r.contains(mx, my), enabled = enabled, iconName = icon) },
    )

    private fun backButton() = Node(
        id = "back", width = Len.Fixed(33), height = Len.Fixed(33),
        paint = { c, r, mx, my ->
            SnellUi.squareButton(c, r, r.contains(mx, my))
            SnellUi.icon(c, "back", r.left + r.width / 2, r.top + r.height / 2, 15, SnellPalette.text)
        },
    )
}

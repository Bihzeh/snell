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
import gg.snell.shared.SnellPalette

/** Minecraft-free state the options screen renders (the adapter maps live game options onto it). */
data class OptionsState(
    val entries: List<OptionEntry>,
    val category: String = "video",
    val scrollY: Int = 0,
)

/**
 * Declarative Options screen (design "Snell In-Game Menus"): a centred panel — a back/title/Done
 * header band, a left category rail (Video / Controls / Audio | Mods) and a virtualized content
 * column of section headers + option rows (label/description left, a `ctrl:<id>` toggle / cycle /
 * slider node right). Authored in the 810-tall design space at mockup ×0.75, replacing
 * OptionsLayout/OptionsRenderer. The header band + an in-cell bottom mask cover row overflow.
 */
object OptionsView {
    val CATEGORIES = listOf("video", "controls", "audio", "mods")
    const val ROW_H = 48      // mockup ~64 option row ×0.75
    const val ROW_GAP = 3
    private const val HEADER_H = 66    // mockup 88 (44 back + 2×22 pad)
    private const val RAIL_W = 186     // mockup 248
    private const val CONTENT_PAD_X = 27 // mockup 36
    private const val CONTENT_PAD_Y = 22 // mockup 30
    private val categoryLabels = mapOf("video" to "Video", "controls" to "Controls", "audio" to "Audio", "mods" to "Mods")

    fun build(s: OptionsState): Node = Node(
        dir = Dir.Stack, width = Len.Flex(), height = Len.Flex(),
        children = listOf(panel(s)),
    )

    fun contentHeight(count: Int): Int = if (count <= 0) 0 else count * ROW_H + (count - 1) * ROW_GAP

    fun maxScroll(laid: Node, count: Int): Int {
        val list = laid.find("list") ?: return 0
        return (contentHeight(count) - (list.rect.height - 2 * CONTENT_PAD_Y)).coerceAtLeast(0)
    }

    private fun panel(s: OptionsState) = Node(
        anchor = Anchor.Center, width = Len.Fixed(930), height = Len.Fixed(675), dir = Dir.Stack,
        paint = { c, r, _, _ -> SnellUi.panel(c, r) },
        children = listOf(
            Node(
                dir = Dir.Column, width = Len.Flex(), height = Len.Flex(), cross = Cross.Stretch,
                children = listOf(
                    Node(height = Len.Fixed(HEADER_H)),
                    Node(
                        dir = Dir.Row, height = Len.Flex(), cross = Cross.Stretch,
                        children = listOf(rail(s), contentCell(s)),
                    ),
                ),
            ),
            header(s),
        ),
    )

    // ---- header band ----------------------------------------------------------------------------
    private fun header(s: OptionsState) = Node(
        anchor = Anchor.TopLeft, width = Len.Flex(), height = Len.Fixed(HEADER_H),
        dir = Dir.Row, cross = Cross.Center, gap = 13, padding = Edge(l = 22, r = 22),
        paint = { c, r, _, _ ->
            c.fill(r.left + 1, r.top + 1, r.width - 2, r.height - 1, SnellPalette.menuPanel)
            SnellUi.divider(c, r.left + 1, r.bottom, r.width - 2)
        },
        children = listOf(
            Node(
                id = "back", width = Len.Fixed(33), height = Len.Fixed(33),
                paint = { c, r, mx, my ->
                    SnellUi.squareButton(c, r, r.contains(mx, my))
                    SnellUi.icon(c, "back", r.left + r.width / 2, r.top + r.height / 2, 15, SnellPalette.text)
                },
            ),
            Node(
                width = Len.Flex(), height = Len.Fixed(20),
                paint = { c, r, _, _ -> SnellUi.heading(c, r.left, r.top, "Options", pixelHeight = 18, letterSpacingEm = 0.02f) },
            ),
            Node(
                id = "done", width = Len.Fixed(84), height = Len.Fixed(35),
                paint = { c, r, mx, my -> SnellUi.button(c, r, "Done", SnellBtn.Primary, r.contains(mx, my), iconName = "check") },
            ),
        ),
    )

    // ---- category rail --------------------------------------------------------------------------
    private fun rail(s: OptionsState) = Node(
        width = Len.Fixed(RAIL_W), dir = Dir.Column, cross = Cross.Stretch, gap = 4,
        padding = Edge(12, 16, 12, 16),
        paint = { c, r, _, _ ->
            c.fill(r.left + 1, r.top, r.width - 1, r.height - 1, SnellPalette.withAlpha(SnellUi.WHITE, 0x04))
            c.fill(r.right, r.top, 1, r.height, SnellUi.rowBorder)
        },
        children = CATEGORIES.flatMap { id ->
            // the design separates Mods from the game categories with a hairline
            val item = Node(
                id = id, height = Len.Fixed(31),
                paint = { c, r, mx, my ->
                    val active = id == s.category
                    SnellUi.categoryItem(c, r, active, r.contains(mx, my))
                    val col = if (active) SnellPalette.text else SnellPalette.text2
                    SnellUi.icon(c, id, r.left + 16, r.top + r.height / 2, 14, col)
                    c.drawText(r.left + 28, r.top + (r.height - c.lineHeight) / 2 + 1, categoryLabels[id] ?: id, col)
                    if (active) c.fill(r.left - 12, r.top + (r.height - 14) / 2, 2, 14, SnellPalette.accent)
                },
            )
            if (id == "mods") {
                listOf(Node(height = Len.Fixed(9), paint = { c, r, _, _ -> SnellUi.divider(c, r.left + 6, r.top + 4, r.width - 12) }), item)
            } else {
                listOf(item)
            }
        },
    )

    // ---- content --------------------------------------------------------------------------------
    /** The scrolling cell right of the rail: the lazy list + a bottom band masking row overflow. */
    private fun contentCell(s: OptionsState) = Node(
        dir = Dir.Stack, width = Len.Flex(), height = Len.Flex(),
        children = listOf(
            Node(
                id = "list", width = Len.Flex(), height = Len.Flex(), clip = true,
                padding = Edge(CONTENT_PAD_X, CONTENT_PAD_Y, CONTENT_PAD_X, CONTENT_PAD_Y),
                lazy = Lazy(s.entries.size, ROW_H, ROW_GAP, s.scrollY) { i -> row(s, i) },
                paint = { c, r, _, _ ->
                    SnellUi.scrollbar(c, r.right - 8, r.top + CONTENT_PAD_Y, r.height - 2 * CONTENT_PAD_Y, contentHeight(s.entries.size), s.scrollY)
                },
            ),
            // Mask must cover a full row stride: a partially-scrolled last row can extend up to
            // ROW_H below the inner viewport (arrangeLazy includes any intersecting row), which is
            // deeper than the 22px content padding alone.
            Node(
                anchor = Anchor.BottomLeft, width = Len.Flex(), height = Len.Fixed(ROW_H + ROW_GAP),
                paint = { c, r, _, _ -> c.fill(r.left, r.top, r.width - 1, r.height - 1, SnellPalette.menuPanel) },
            ),
        ),
    )

    private fun row(s: OptionsState, i: Int): Node = when (val e = s.entries[i]) {
        is OptionEntry.Section -> Node(
            id = "row:$i",
            paint = { c, r, _, _ -> SnellUi.sectionLabel(c, r.left + 2, r.bottom - c.lineHeight - 4, e.label) },
        )
        is OptionEntry.Item -> Node(
            id = "row:$i", dir = Dir.Row, cross = Cross.Center, gap = 15, padding = Edge(l = 13, r = 13),
            paint = { c, r, mx, my -> SnellUi.listRow(c, r, selected = false, hover = r.contains(mx, my)) },
            children = listOf(labelBlock(e.item), control(e.item)),
        )
    }

    private fun labelBlock(item: OptionItem) = Node(
        width = Len.Flex(), height = Len.Fixed(ROW_H),
        paint = { c, r, _, _ ->
            val maxW = r.width
            if (item.description.isNotEmpty()) {
                val block = c.lineHeight * 2 + 4
                val ty = r.top + (r.height - block) / 2 + 2
                c.drawText(r.left, ty, SnellUi.ellipsize(c, item.label, maxW), SnellPalette.text)
                c.drawText(r.left, ty + c.lineHeight + 4, SnellUi.ellipsize(c, item.description, maxW), SnellPalette.menuText3)
            } else {
                c.drawText(r.left, r.top + (r.height - c.lineHeight) / 2 + 1, SnellUi.ellipsize(c, item.label, maxW), SnellPalette.text)
            }
        },
    )

    private fun control(item: OptionItem): Node = when (item.kind) {
        OptionKind.Toggle -> Node(
            id = "ctrl:${item.id}", width = Len.Fixed(35), height = Len.Fixed(20),
            paint = { c, r, _, _ -> SnellUi.switch(c, r, item.on) },
        )
        OptionKind.Cycle -> Node(
            id = "ctrl:${item.id}", width = Len.Fixed(112), height = Len.Fixed(31),
            paint = { c, r, mx, my ->
                SnellUi.button(c, r, SnellUi.ellipsize(c, item.valueText, r.width - 24), SnellBtn.Secondary, r.contains(mx, my))
                SnellUi.icon(c, "cycle", r.right - 11, r.top + r.height / 2, 11, SnellPalette.menuText3)
            },
        )
        OptionKind.Slider -> Node(
            id = "ctrl:${item.id}", width = Len.Fixed(255), height = Len.Fixed(31),
            paint = { c, r, _, _ -> SnellUi.slider(c, Rect(r.left, r.top, r.width, r.height), item.fraction, item.valueText, valueW = 63) },
        )
    }
}

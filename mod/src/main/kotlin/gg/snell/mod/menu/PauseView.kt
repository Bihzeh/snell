package gg.snell.mod.menu

import gg.snell.mod.editor.Rect
import gg.snell.mod.ui.SnellBtn
import gg.snell.mod.ui.SnellUi
import gg.snell.mod.ui.node.Anchor
import gg.snell.mod.ui.node.Cross
import gg.snell.mod.ui.node.Dir
import gg.snell.mod.ui.node.Edge
import gg.snell.mod.ui.node.Len
import gg.snell.mod.ui.node.Node
import gg.snell.shared.SnellPalette

/** Minecraft-free data the pause view renders (the screen fills [worldName] from real game state). */
data class PauseData(val worldName: String = "World")

/**
 * Declarative pause menu (design "Snell In-Game Menus"): a centred card — a brand header (slipstream
 * mark + PAUSED eyebrow + world name), a primary Back-to-Game, an accented Quick-Switch row
 * (placeholder), a 2×2 grid (Options / Advancements / Statistics / Open-to-LAN) and a Save & Quit
 * danger button. Authored in the 810-tall design space at the mockup's 1920×1080 proportions ×0.75, so
 * it reads at the mockup density instead of the old zoomed per-pixel PauseLayout/PauseRenderer. Pure
 * (no MC types) -> unit-testable + preview-renderable. One tree drives draw + hit-test.
 */
object PauseView {
    /** Activatable ids, top→bottom. `quickswitch`/`openToLan` are placeholders; the rest delegate. */
    val IDS = listOf("resume", "quickswitch", "options", "advancements", "statistics", "openToLan", "savequit")

    fun build(d: PauseData): Node = Node(
        dir = Dir.Stack, width = Len.Flex(), height = Len.Flex(),
        children = listOf(card(d)),
    )

    /** The centred card: a Column of header + primary + quick-switch + grid + danger, height = content. */
    private fun card(d: PauseData) = Node(
        anchor = Anchor.Center, width = Len.Fixed(330), height = Len.Auto,
        dir = Dir.Column, cross = Cross.Stretch, gap = 8, padding = Edge(24, 22, 24, 22),
        paint = { c, r, _, _ -> SnellUi.panel(c, r) },
        children = listOf(
            header(d),
            button("resume", "Back to Game", "play", SnellBtn.Primary, 34),
            quickSwitch(),
            grid(),
            button("savequit", "Save & Quit to Title", "quit", SnellBtn.Danger, 32),
        ),
    )

    private fun header(d: PauseData) = Node(
        height = Len.Fixed(28),
        paint = { c, r, _, _ ->
            SnellUi.slipstream(c, r.left, r.top, 22)
            val tx = r.left + 30
            SnellUi.sectionLabel(c, tx, r.top, "Paused")
            c.drawText(tx, r.top + c.lineHeight + 4, SnellUi.ellipsize(c, d.worldName, r.right - tx), SnellPalette.text)
        },
    )

    private fun button(id: String, label: String, icon: String, style: SnellBtn, h: Int) = Node(
        id = id, height = Len.Fixed(h),
        paint = { c, r, mx, my -> SnellUi.button(c, r, label, style, r.contains(mx, my), iconName = icon) },
    )

    /** The accented Quick-Switch row (placeholder): icon tile + title/subtitle + chevron. */
    private fun quickSwitch() = Node(
        id = "quickswitch", height = Len.Fixed(42),
        paint = { c, r, mx, my ->
            val hover = r.contains(mx, my)
            // Rest is the dimmer wash, hover the brighter one (they were swapped — hovering darkened).
            SnellUi.surface(
                c, r,
                if (hover) SnellPalette.accentSubtle else SnellPalette.withAlpha(SnellPalette.accent, 0x1C),
                SnellPalette.withAlpha(SnellPalette.accent, if (hover) 0x66 else 0x40),
            )
            val t = r.height - 12
            val tile = Rect(r.left + 6, r.top + (r.height - t) / 2, t, t)
            SnellUi.iconTile(c, tile, SnellPalette.withAlpha(SnellPalette.accent, 0x22))
            SnellUi.icon(c, "quickswitch", tile.left + tile.width / 2, tile.top + tile.height / 2, t - 4, SnellPalette.accent)
            val tx = tile.right + 9
            // Centre the two-line block against the icon tile (+2 optical nudge; MC text ink rides high).
            val ty = r.top + (r.height - (c.lineHeight * 2 + 4)) / 2 + 2
            c.drawText(tx, ty, "Quick Switch", SnellPalette.text)
            c.drawText(tx, ty + c.lineHeight + 4, SnellUi.ellipsize(c, "Jump to another server or world", r.right - 14 - tx), SnellPalette.text2)
            // Vector chevron: the sprite's ink-to-box ratio makes it near-invisible at this size.
            SnellUi.chevronRight(c, r.right - 12, r.top + r.height / 2, 3, SnellPalette.text2)
        },
    )

    /** 2×2 grid of secondary actions — two Rows of two flex buttons. */
    private fun grid() = Node(
        dir = Dir.Column, cross = Cross.Stretch, gap = 8, height = Len.Auto,
        children = listOf(
            gridRow(gridBtn("options", "Options", "options"), gridBtn("advancements", "Advancements", "advancements")),
            gridRow(gridBtn("statistics", "Statistics", "statistics"), gridBtn("openToLan", "Open to LAN", "lan")),
        ),
    )

    private fun gridRow(a: Node, b: Node) =
        Node(dir = Dir.Row, gap = 8, height = Len.Fixed(30), cross = Cross.Stretch, children = listOf(a, b))

    private fun gridBtn(id: String, label: String, icon: String) = Node(
        id = id, width = Len.Flex(1),
        paint = { c, r, mx, my -> SnellUi.button(c, r, label, SnellBtn.Secondary, r.contains(mx, my), iconName = icon) },
    )
}

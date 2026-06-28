package gg.snell.mod.module.hud

import gg.snell.mod.module.HudAnchor
import gg.snell.mod.module.HudLine
import gg.snell.mod.module.HudModule
import gg.snell.mod.module.HudStyle
import gg.snell.mod.platform.GameContext
import gg.snell.shared.SnellPalette

class FpsModule : HudModule {
    override val id = "fps"
    override val displayName = "FPS"
    override var enabled = true
    override var anchor = HudAnchor.TOP_LEFT
    override var offsetX = 4
    override var offsetY = 4
    // Inlined (not `= defaultStyle`) so it doesn't rely on property declaration order / virtual dispatch.
    override val defaultStyle = HudStyle(color = SnellPalette.gold)
    override var style = HudStyle(color = SnellPalette.gold)

    override fun render(ctx: GameContext): List<HudLine> =
        listOf(HudLine("${ctx.fps} FPS"))
}

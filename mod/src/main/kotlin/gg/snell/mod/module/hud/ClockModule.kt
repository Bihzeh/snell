package gg.snell.mod.module.hud

import gg.snell.mod.module.HudAnchor
import gg.snell.mod.module.HudLine
import gg.snell.mod.module.HudModule
import gg.snell.mod.module.HudStyle
import gg.snell.mod.platform.GameContext

class ClockModule : HudModule {
    override val id = "clock"
    override val displayName = "In-game time"
    override var enabled = false
    override var anchor = HudAnchor.TOP_RIGHT
    override var offsetX = 4
    override var offsetY = 28
    override var style = HudStyle()

    override fun render(ctx: GameContext): List<HudLine> {
        if (!ctx.inWorld) return emptyList()
        return listOf(HudLine(HudFormat.clock(ctx.dayTime)))
    }
}

package gg.snell.mod.module.hud

import gg.snell.mod.module.HudAnchor
import gg.snell.mod.module.HudLine
import gg.snell.mod.module.HudModule
import gg.snell.mod.module.HudStyle
import gg.snell.mod.platform.GameContext

class DayModule : HudModule {
    override val id = "day"
    override val displayName = "Day counter"
    override var enabled = false
    override var anchor = HudAnchor.TOP_RIGHT
    override var offsetX = 4
    override var offsetY = 16
    override var style = HudStyle()

    override fun render(ctx: GameContext): List<HudLine> {
        if (!ctx.inWorld) return emptyList()
        return listOf(HudLine("Day ${HudFormat.day(ctx.dayTime)}"))
    }
}

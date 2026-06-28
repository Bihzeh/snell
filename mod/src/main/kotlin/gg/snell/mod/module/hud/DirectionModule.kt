package gg.snell.mod.module.hud

import gg.snell.mod.module.HudAnchor
import gg.snell.mod.module.HudLine
import gg.snell.mod.module.HudModule
import gg.snell.mod.module.HudStyle
import gg.snell.mod.platform.GameContext

class DirectionModule : HudModule {
    override val id = "direction"
    override val displayName = "Direction"
    override var enabled = false
    override var anchor = HudAnchor.TOP_RIGHT
    override var offsetX = 4
    override var offsetY = 4
    override var style = HudStyle()

    override fun render(ctx: GameContext): List<HudLine> {
        if (!ctx.inWorld) return emptyList()
        return listOf(HudLine("Facing: ${HudFormat.cardinal(ctx.yaw)}"))
    }
}

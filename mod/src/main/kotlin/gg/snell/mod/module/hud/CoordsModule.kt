package gg.snell.mod.module.hud

import gg.snell.mod.module.HudAnchor
import gg.snell.mod.module.HudLine
import gg.snell.mod.module.HudModule
import gg.snell.mod.module.HudStyle
import gg.snell.mod.platform.GameContext

class CoordsModule : HudModule {
    override val id = "coords"
    override val displayName = "Coordinates"
    override var enabled = true
    override var anchor = HudAnchor.TOP_LEFT
    override var offsetX = 4
    override var offsetY = 16
    override var style = HudStyle()

    override fun render(ctx: GameContext): List<HudLine> {
        if (!ctx.inWorld) return emptyList()
        return listOf(
            HudLine("XYZ: %.1f / %.1f / %.1f".format(ctx.playerX, ctx.playerY, ctx.playerZ)),
        )
    }
}

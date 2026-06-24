package gg.maeve.mod.module.hud

import gg.maeve.mod.module.HudLine
import gg.maeve.mod.module.HudModule
import gg.maeve.mod.platform.GameContext

class CoordsModule : HudModule {
    override val id = "coords"
    override val displayName = "Coordinates"
    override var enabled = true
    override var x = 4
    override var y = 16

    override fun render(ctx: GameContext): List<HudLine> {
        if (!ctx.inWorld) return emptyList()
        return listOf(
            HudLine("XYZ: %.1f / %.1f / %.1f".format(ctx.playerX, ctx.playerY, ctx.playerZ)),
        )
    }
}

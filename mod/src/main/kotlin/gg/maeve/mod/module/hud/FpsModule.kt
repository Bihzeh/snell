package gg.maeve.mod.module.hud

import gg.maeve.mod.module.HudLine
import gg.maeve.mod.module.HudModule
import gg.maeve.mod.platform.GameContext

class FpsModule : HudModule {
    override val id = "fps"
    override val displayName = "FPS"
    override var enabled = true
    override var x = 4
    override var y = 4

    override fun render(ctx: GameContext): List<HudLine> =
        listOf(HudLine("${ctx.fps} FPS"))
}

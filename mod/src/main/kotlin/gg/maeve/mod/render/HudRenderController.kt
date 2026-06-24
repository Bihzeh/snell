package gg.maeve.mod.render

import gg.maeve.mod.module.ModuleManager
import gg.maeve.mod.platform.GameContext
import gg.maeve.mod.platform.HudCanvas

/**
 * Draws all enabled HUD modules. Pure orchestration over the HudCanvas
 * abstraction, so it is fully unit-testable without Minecraft.
 */
class HudRenderController(private val modules: ModuleManager) {
    fun draw(canvas: HudCanvas, ctx: GameContext) {
        for (module in modules.hudModules()) {
            if (!module.enabled) continue
            var lineY = module.y
            for (line in module.render(ctx)) {
                canvas.drawText(module.x, lineY, line.text, line.color)
                lineY += canvas.lineHeight
            }
        }
    }
}

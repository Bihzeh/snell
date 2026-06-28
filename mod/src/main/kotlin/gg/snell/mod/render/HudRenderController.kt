package gg.snell.mod.render

import gg.snell.mod.module.ModuleManager
import gg.snell.mod.platform.GameContext
import gg.snell.mod.platform.HudCanvas

/**
 * Draws all enabled HUD modules. Pure orchestration over the HudCanvas abstraction,
 * so it is fully unit-testable without Minecraft.
 */
class HudRenderController(private val modules: ModuleManager) {
    fun draw(canvas: HudCanvas, ctx: GameContext) {
        for (module in modules.hudModules()) {
            if (!module.enabled) continue
            HudModuleRender.draw(canvas, module, ctx)
        }
    }
}

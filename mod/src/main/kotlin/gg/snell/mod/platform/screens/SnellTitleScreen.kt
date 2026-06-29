package gg.snell.mod.platform.screens

import gg.snell.mod.menu.TitleLayout
import gg.snell.mod.menu.TitleRenderer
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.platform.SnellMenuScreen
import gg.snell.mod.platform.SnellMenus
import net.minecraft.network.chat.Component

/**
 * Bespoke main menu. Renders the Snell title (command column + quick actions) and opens the bespoke
 * sub-screens directly (so they return here on back). The Discord / wallet / cosmetics / friends
 * quick actions are styled placeholders for now (no-op until those surfaces are built).
 */
class SnellTitleScreen : SnellMenuScreen(Component.literal("Snell")) {

    override fun draw(canvas: EditorCanvas, mouseX: Int, mouseY: Int) =
        TitleRenderer.render(canvas, width, height, mouseX, mouseY, SnellMenus.VERSION, mc.user.name)

    override fun hitId(mouseX: Int, mouseY: Int): String? = TitleLayout.hit(width, height, mouseX, mouseY)

    override fun onActivate(id: String) {
        when (id) {
            "singleplayer" -> mc.setScreenAndShow(SnellWorldSelectScreen(this))
            "multiplayer" -> mc.setScreenAndShow(SnellServerSelectScreen(this))
            "options" -> mc.setScreenAndShow(SnellOptionsScreen(this))
            "quit" -> mc.stop()
            // discord / wallet / cosmetics / friends — placeholders (no-op for now)
        }
    }

    override fun isPauseScreen(): Boolean = false
}

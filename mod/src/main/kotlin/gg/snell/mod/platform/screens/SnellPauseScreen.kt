package gg.snell.mod.platform.screens

import gg.snell.mod.menu.PauseData
import gg.snell.mod.menu.PauseView
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.platform.SnellMenuScreen
import gg.snell.mod.ui.node.Layout
import gg.snell.mod.ui.node.Node
import gg.snell.mod.ui.node.asMetrics
import gg.snell.mod.ui.node.hit
import gg.snell.mod.ui.node.render
import net.minecraft.client.gui.screens.achievement.StatsScreen
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen
import net.minecraft.network.chat.Component

/**
 * Bespoke pause menu. Builds the [PauseView] node tree and delegates each action to the same vanilla
 * call the original PauseScreen makes (resume / options / advancements / statistics / save & quit). The
 * Quick-Switch row and Open-to-LAN tile are styled placeholders for now. Esc resumes.
 */
class SnellPauseScreen : SnellMenuScreen(Component.literal("Paused")) {

    // Ported screens lay out in the mockup-faithful 810-tall design space (see SnellMenuScreen); the
    // still-old World/Server/Options screens keep the base 270 until they are ported too.
    override val designH: Int get() = 810

    private var laid: Node? = null

    override fun draw(canvas: EditorCanvas, mouseX: Int, mouseY: Int) {
        val t = PauseView.build(PauseData(worldName()))
        Layout.layout(t, designW, designH, canvas.asMetrics())
        t.render(canvas, mouseX, mouseY)
        laid = t
    }

    /** The real world/server name for the pause card header, with a safe fallback. */
    private fun worldName(): String = runCatching {
        if (mc.hasSingleplayerServer()) mc.singleplayerServer?.worldData?.levelName ?: "Singleplayer"
        else mc.currentServer?.name?.ifBlank { mc.currentServer?.ip ?: "" }?.ifBlank { "Multiplayer Server" } ?: "Multiplayer Server"
    }.getOrDefault(if (mc.hasSingleplayerServer()) "Singleplayer" else "Multiplayer Server")

    // Hit-test the tree laid out by the last draw() (the render frame precedes any click).
    override fun hitId(mouseX: Int, mouseY: Int): String? = laid?.hit(mouseX, mouseY)

    override fun onActivate(id: String) {
        when (id) {
            "resume" -> onClose()
            "options" -> mc.setScreenAndShow(SnellOptionsScreen(this))
            "advancements" -> mc.connection?.let { mc.setScreenAndShow(AdvancementsScreen(it.advancements, this)) }
            "statistics" -> mc.player?.let { mc.setScreenAndShow(StatsScreen(this, it.stats)) }
            "savequit" -> if (mc.hasSingleplayerServer()) mc.disconnectWithSavingScreen() else mc.disconnectWithProgressScreen()
            // quickswitch / openToLan — placeholders (no-op for now)
        }
    }

    override fun onClose() {
        super.onClose() // returns to the world (setScreen(null))
        mc.mouseHandler.grabMouse()
    }

    override fun isPauseScreen(): Boolean = true
}

package gg.snell.mod.platform.screens

import gg.snell.mod.menu.TitleData
import gg.snell.mod.menu.TitleView
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.platform.SnellMenuScreen
import gg.snell.mod.platform.SnellMenus
import gg.snell.mod.ui.node.Layout
import gg.snell.mod.ui.node.Node
import gg.snell.mod.ui.node.asMetrics
import gg.snell.mod.ui.node.hit
import gg.snell.mod.ui.node.render
import net.minecraft.client.multiplayer.ServerList
import net.minecraft.network.chat.Component

/**
 * Bespoke main menu. Renders the Snell title (command column + quick actions) and opens the bespoke
 * sub-screens directly (so they return here on back). The Discord / wallet / cosmetics / friends
 * quick actions are styled placeholders for now (no-op until those surfaces are built).
 */
class SnellTitleScreen : SnellMenuScreen(Component.literal("Snell")) {

    private var singleplayerSub = "Create or load a world"
    private var multiplayerSub = "Join a server"
    private var loaded = false // MC re-runs init() on every resize; load the counts only once

    override fun init() {
        super.init()
        if (loaded) return
        loaded = true
        // server count off the render thread (servers.dat is disk I/O + NBT parse), posted back on the client thread
        Thread({
            val n = runCatching { ServerList(mc).apply { load() }.size() }.getOrDefault(0)
            mc.execute { multiplayerSub = if (n == 0) "No servers yet" else "$n ${if (n == 1) "server" else "servers"}" }
        }, "snell-serverlist").apply { isDaemon = true; start() }
        WorldAdapter.summary(mc) { n, ago ->
            singleplayerSub = when {
                n == 0 -> "No worlds yet"
                ago.isEmpty() -> "$n ${if (n == 1) "world" else "worlds"}"
                else -> "$n ${if (n == 1) "world" else "worlds"} · last played $ago"
            }
        }
    }

    private var laid: Node? = null

    private fun data() = TitleData(
        modVersion = SnellMenus.modVersion, mcVersion = SnellMenus.mcVersion,
        username = mc.user.name, status = "Online", crowns = "0",
        singleplayerSub = singleplayerSub, multiplayerSub = multiplayerSub,
    )

    override fun draw(canvas: EditorCanvas, mouseX: Int, mouseY: Int) {
        val t = TitleView.build(data())
        Layout.layout(t, designW, designH, canvas.asMetrics())
        t.render(canvas, mouseX, mouseY)
        laid = t
    }

    // Hit-test the tree laid out by the last draw() (the render frame precedes any click).
    override fun hitId(mouseX: Int, mouseY: Int): String? = laid?.hit(mouseX, mouseY)

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

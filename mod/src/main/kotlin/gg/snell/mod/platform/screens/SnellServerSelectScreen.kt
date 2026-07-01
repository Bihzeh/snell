package gg.snell.mod.platform.screens

import gg.snell.mod.menu.ServerRow
import gg.snell.mod.menu.ServerSelectLayout
import gg.snell.mod.menu.ServerSelectRenderer
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.platform.SnellMenuScreen
import gg.snell.mod.platform.SnellMenus
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.network.chat.Component

/**
 * Bespoke multiplayer server picker. Renders the Snell card and delegates to vanilla via
 * [ServerAdapter] (list + live ping + join). Add / Edit / Direct Connect have no standalone screens in
 * 26.1.2, so they hand off to vanilla's JoinMultiplayerScreen via a one-shot swap bypass.
 */
class SnellServerSelectScreen(private val parent: Screen?) : SnellMenuScreen(Component.literal("Multiplayer")) {
    private val adapter = ServerAdapter(mc)
    private var rows: List<ServerRow> = emptyList()
    private var selected = -1
    private var scrollY = 0
    private var started = false

    override fun init() {
        super.init()
        if (!started) { started = true; adapter.pingAll() }
        rows = adapter.rows()
    }

    override fun tick() {
        adapter.tick()
        rows = adapter.rows()
    }

    override fun draw(canvas: EditorCanvas, mouseX: Int, mouseY: Int) =
        ServerSelectRenderer.render(canvas, designW, designH, mouseX, mouseY, rows, selected, scrollY)

    override fun hitId(mouseX: Int, mouseY: Int): String? = ServerSelectLayout.hit(designW, designH, mouseX, mouseY)

    override fun onPress(mouseX: Int, mouseY: Int, doubled: Boolean): Boolean {
        val i = ServerSelectLayout.rowAt(designW, designH, scrollY, rows.size, mouseX, mouseY)
        if (i >= 0) {
            selected = i
            if (doubled) joinSelected()
            return true
        }
        return false
    }

    override fun onActivate(id: String) {
        when (id) {
            "back", "cancel" -> onClose()
            "refresh" -> { adapter.pingAll(); rows = adapter.rows() }
            "join" -> joinSelected()
            "add", "direct" -> openVanillaList()
        }
    }

    override fun onScroll(amount: Double) {
        scrollY = (scrollY - (amount * 16).toInt()).coerceIn(0, ServerSelectLayout.maxScroll(rows.size, designW, designH))
    }

    private fun joinSelected() {
        if (selected in rows.indices) {
            adapter.dispose()
            adapter.join(this, selected)
        }
    }

    /** Hand Add / Edit / Direct Connect to vanilla's list (no bespoke input dialogs in 26.1.2). */
    private fun openVanillaList() {
        SnellMenus.bypassNext = true
        mc.setScreenAndShow(JoinMultiplayerScreen(parent ?: this))
    }

    override fun onClose() {
        adapter.dispose()
        mc.setScreenAndShow(parent ?: TitleScreen())
    }

    override fun isPauseScreen(): Boolean = false
}

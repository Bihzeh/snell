package gg.snell.mod.platform.screens

import gg.snell.mod.menu.WorldRow
import gg.snell.mod.menu.WorldSelectLayout
import gg.snell.mod.menu.WorldSelectRenderer
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.platform.SnellMenuScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.network.chat.Component

/**
 * Bespoke singleplayer world picker. Renders the Snell card and delegates every action to vanilla via
 * [WorldAdapter] (load summaries / play / create / edit / delete). Single-click selects a row,
 * double-click or "Play" loads it.
 */
class SnellWorldSelectScreen(private val parent: Screen?) : SnellMenuScreen(Component.literal("Singleplayer")) {
    private var rows: List<WorldRow> = emptyList()
    private var selected = -1
    private var scrollY = 0
    private var loaded = false

    override fun init() {
        super.init()
        if (!loaded) {
            loaded = true
            WorldAdapter.load(mc) { rows = it; clampScroll() }
        }
    }

    override fun draw(canvas: EditorCanvas, mouseX: Int, mouseY: Int) =
        WorldSelectRenderer.render(canvas, designW, designH, mouseX, mouseY, rows, selected, scrollY, "", false)

    override fun hitId(mouseX: Int, mouseY: Int): String? = WorldSelectLayout.hit(designW, designH, mouseX, mouseY)

    override fun onPress(mouseX: Int, mouseY: Int, doubled: Boolean): Boolean {
        val i = WorldSelectLayout.rowAt(designW, designH, scrollY, rows.size, mouseX, mouseY)
        if (i >= 0) {
            selected = i
            if (doubled) playSelected()
            return true
        }
        return false
    }

    override fun onActivate(id: String) {
        when (id) {
            "back", "cancel" -> onClose()
            "play" -> playSelected()
            "create" -> WorldAdapter.create(mc, this)
            "edit" -> rows.getOrNull(selected)?.let { WorldAdapter.edit(mc, it.folder, this) }
            "delete" -> rows.getOrNull(selected)?.let { WorldAdapter.delete(mc, it.folder, it.name, this) }
        }
    }

    override fun onScroll(amount: Double) {
        scrollY = (scrollY - (amount * 16).toInt()).coerceIn(0, WorldSelectLayout.maxScroll(rows.size, designW, designH))
    }

    private fun clampScroll() { scrollY = scrollY.coerceIn(0, WorldSelectLayout.maxScroll(rows.size, designW, designH)) }

    private fun playSelected() {
        rows.getOrNull(selected)?.let { WorldAdapter.play(mc, it.folder) { mc.setScreenAndShow(this) } }
    }

    override fun onClose() {
        mc.setScreenAndShow(parent ?: TitleScreen())
    }

    override fun isPauseScreen(): Boolean = false
}

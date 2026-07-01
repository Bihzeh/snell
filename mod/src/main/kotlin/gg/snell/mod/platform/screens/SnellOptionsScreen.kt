package gg.snell.mod.platform.screens

import gg.snell.mod.editor.Rect
import gg.snell.mod.menu.OptionEntry
import gg.snell.mod.menu.OptionKind
import gg.snell.mod.menu.OptionsLayout
import gg.snell.mod.menu.OptionsRenderer
import gg.snell.mod.module.ModuleManager
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.platform.SnellMenuScreen
import gg.snell.mod.platform.SnellMenus
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.network.chat.Component

/**
 * Bespoke Options screen — full native: every control reads/writes the live game
 * [net.minecraft.client.Options] through [OptionsAdapter] (Video / Controls / Audio) and the Mods tab
 * toggles real modules via [ModuleManager]. Entries are rebuilt only on category switch + after a
 * mutation (not per frame).
 */
class SnellOptionsScreen(
    private val parent: Screen?,
    private val modules: ModuleManager? = SnellMenus.modules,
) : SnellMenuScreen(Component.literal("Options")) {

    private var category = "video"
    private var entries: List<OptionEntry> = emptyList()
    private var scrollY = 0
    private var sliderDrag: String? = null

    override fun init() {
        super.init()
        rebuild()
    }

    private fun rebuild() {
        entries = OptionsAdapter.entries(mc.options, modules, category)
        scrollY = scrollY.coerceIn(0, OptionsLayout.maxScroll(entries.size, designW, designH))
    }

    override fun draw(canvas: EditorCanvas, mouseX: Int, mouseY: Int) =
        OptionsRenderer.render(canvas, designW, designH, mouseX, mouseY, entries, category, scrollY)

    override fun hitId(mouseX: Int, mouseY: Int): String? = OptionsLayout.hit(designW, designH, mouseX, mouseY)

    override fun onActivate(id: String) {
        when {
            id == "back" || id == "done" -> onClose()
            id in OptionsLayout.CATEGORIES -> { category = id; scrollY = 0; rebuild() }
        }
    }

    override fun onPress(mouseX: Int, mouseY: Int, doubled: Boolean): Boolean {
        for (i in OptionsLayout.visibleRange(entries.size, scrollY, designW, designH)) {
            val rr = OptionsLayout.rowRect(i, scrollY, designW, designH)
            if (!rr.contains(mouseX, mouseY)) continue
            val e = entries[i]
            if (e is OptionEntry.Item) {
                val item = e.item
                when (item.kind) {
                    OptionKind.Toggle -> { OptionsAdapter.toggle(mc.options, modules, item.id); rebuild() }
                    OptionKind.Cycle -> { OptionsAdapter.cycle(mc.options, item.id); rebuild() }
                    OptionKind.Slider -> { sliderDrag = item.id; applySlider(item.id, mouseX, OptionsLayout.controlRect(rr)); rebuild() }
                }
            }
            return true
        }
        return false
    }

    override fun onDragTo(mouseX: Int, mouseY: Int) {
        val id = sliderDrag ?: return
        val rr = rowRectFor(id) ?: return
        applySlider(id, mouseX, OptionsLayout.controlRect(rr))
        rebuild()
    }

    override fun onReleaseDrag() { sliderDrag = null }

    override fun onScroll(amount: Double) {
        scrollY = (scrollY - (amount * 16).toInt()).coerceIn(0, OptionsLayout.maxScroll(entries.size, designW, designH))
    }

    private fun applySlider(id: String, mouseX: Int, ctrl: Rect) {
        val f = ((mouseX - ctrl.left).toFloat() / ctrl.width.coerceAtLeast(1)).coerceIn(0f, 1f)
        OptionsAdapter.setSlider(mc.options, id, f)
    }

    private fun rowRectFor(id: String): Rect? {
        for (i in entries.indices) {
            val e = entries[i]
            if (e is OptionEntry.Item && e.item.id == id) return OptionsLayout.rowRect(i, scrollY, designW, designH)
        }
        return null
    }

    override fun onClose() {
        mc.options.save()
        mc.setScreenAndShow(parent ?: TitleScreen())
    }

    override fun isPauseScreen(): Boolean = false
}

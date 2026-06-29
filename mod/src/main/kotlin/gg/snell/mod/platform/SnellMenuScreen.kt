package gg.snell.mod.platform

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

/**
 * Base for bespoke Snell menu screens. Adapts Minecraft 26.x input/render to the mod's pure
 * [EditorCanvas] renderers (same seam as [SnellHudEditorScreen]): [draw] paints the whole screen
 * through the SnellUi kit, [hitId] maps a cursor point to a header/footer control id via the screen's
 * pure `*Layout`, and [onActivate] performs the (vanilla) action for a clicked id.
 *
 * Rich screens (pickers / options) opt into the extra hooks — [onPress] for content regions
 * (rows, sliders), [onScroll]/[onDragTo]/[onReleaseDrag] for lists + sliders, [onKey]/[onCharTyped]
 * for keys — all defaulting to no-op so simple screens (title/pause) ignore them.
 */
abstract class SnellMenuScreen(title: Component) : Screen(title) {

    protected val mc: Minecraft get() = Minecraft.getInstance()

    protected abstract fun draw(canvas: EditorCanvas, mouseX: Int, mouseY: Int)
    protected abstract fun hitId(mouseX: Int, mouseY: Int): String?
    protected abstract fun onActivate(id: String)

    /** Double-click on a header/footer id (defaults to a single activate). */
    protected open fun onActivate2(id: String) = onActivate(id)

    /** Press inside the content area (a list row, a slider). Return true to consume. [doubled] = double-click. */
    protected open fun onPress(mouseX: Int, mouseY: Int, doubled: Boolean): Boolean = false

    protected open fun onScroll(amount: Double) {}
    protected open fun onDragTo(mouseX: Int, mouseY: Int) {}
    protected open fun onReleaseDrag() {}
    /** A non-Esc key; return true to consume. */
    protected open fun onKey(keyCode: Int): Boolean = false
    /** A typed character; return true to consume. */
    protected open fun onCharTyped(ch: Char): Boolean = false

    override fun extractRenderState(extractor: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        super.extractRenderState(extractor, mouseX, mouseY, deltaTicks)
        draw(EditorExtractorCanvas(extractor, mc.font), mouseX, mouseY)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        val mx = event.x().toInt(); val my = event.y().toInt()
        val id = hitId(mx, my)
        if (id != null) {
            if (doubled) onActivate2(id) else onActivate(id)
            return true
        }
        if (onPress(mx, my, doubled)) return true
        return super.mouseClicked(event, doubled)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        onDragTo(event.x().toInt(), event.y().toInt())
        return super.mouseDragged(event, dragX, dragY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        onReleaseDrag()
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (scrollY != 0.0) { onScroll(scrollY); return true }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true }
        if (onKey(event.key())) return true
        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (event.codepoint() in 0..0xFFFF && onCharTyped(event.codepoint().toChar())) return true
        return super.charTyped(event)
    }
}

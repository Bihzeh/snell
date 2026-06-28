package gg.snell.mod.platform

import gg.snell.mod.editor.EditorRenderer
import gg.snell.mod.editor.EditorState
import gg.snell.mod.editor.ElementBox
import gg.snell.mod.editor.ElementLayout
import gg.snell.mod.editor.EditorView
import gg.snell.mod.editor.TextMeasurer
import gg.snell.mod.module.ModuleManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

/**
 * Three-tier HUD editor opened by Right-Shift. POSITION lets the user drag every HUD element to
 * re-anchor it (no styling), with the Snell wordmark and a "Mods" button. "Mods" opens a card
 * GRID of all modules; clicking a card opens that module's CUSTOMIZE popup (full style panel for
 * HUD modules, an enable toggle otherwise). Esc steps back one tier; Done/Esc-at-top persists.
 * All interaction logic is in the pure [EditorState]; this screen only adapts 26.2 input/render.
 */
class SnellHudEditorScreen(
    private val modules: ModuleManager,
    private val sampleCtx: () -> GameContext,
) : Screen(Component.literal("Snell HUD Editor")) {

    private val state = EditorState()
    private val renderer = EditorRenderer()

    private fun boxes(): List<ElementBox> {
        val font = Minecraft.getInstance().font
        val measurer = object : TextMeasurer {
            override fun width(text: String) = font.width(text)
            override val lineHeight = font.lineHeight
        }
        return ElementLayout.boxesFor(modules.hudModules(), sampleCtx(), measurer, width, height)
    }

    override fun extractRenderState(extractor: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        super.extractRenderState(extractor, mouseX, mouseY, deltaTicks)
        val canvas = EditorExtractorCanvas(extractor, Minecraft.getInstance().font)
        renderer.render(canvas, width, height, mouseX, mouseY, sampleCtx(), modules, state)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        val hit = if (state.view == EditorView.POSITION) boxes() else emptyList<ElementBox>() // only POSITION hit-tests element boxes
        val handled = state.onPress(event.x().toInt(), event.y().toInt(), width, height, hit, modules)
        if (state.closeRequested) { onClose(); return true }
        return handled || super.mouseClicked(event, doubled)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean =
        state.onDrag(event.x().toInt(), event.y().toInt(), width, height, modules) || super.mouseDragged(event, dragX, dragY)

    override fun mouseReleased(event: MouseButtonEvent): Boolean =
        state.onRelease() || super.mouseReleased(event)

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (!state.goBack()) onClose() // pop a tier, or close from the position screen
            return true
        }
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE && state.onBackspace(modules)) return true
        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (event.codepoint() > 0xFFFF) return super.charTyped(event) // hex digits are all BMP
        return state.onCharTyped(event.codepoint().toChar(), modules) || super.charTyped(event)
    }

    override fun onClose() {
        modules.saveAll()
        super.onClose()
    }

    override fun isPauseScreen(): Boolean = false
}

package gg.maeve.mod.platform

import gg.maeve.mod.ui.ModMenuController
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

/**
 * Basic in-game mod menu for Phase 1: keyboard-driven (Up/Down to select, Enter to
 * toggle, Esc to close). Rendered with the 26.2 retained-mode extractor. A richer
 * widget-based menu + draggable HUD editor arrive in Phase 2.
 */
class MaeveMenuScreen(
    private val controller: ModMenuController,
) : Screen(Component.literal("Maeve")) {

    private var selected = 0

    override fun extractRenderState(
        extractor: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        deltaTicks: Float,
    ) {
        super.extractRenderState(extractor, mouseX, mouseY, deltaTicks)
        val font = Minecraft.getInstance().font
        val rows = controller.rows()

        extractor.text(font, "Maeve", 20, 16, WHITE, true)
        rows.forEachIndexed { i, row ->
            val cursor = if (i == selected) "> " else "  "
            val state = if (row.enabled) "ON" else "OFF"
            val color = if (row.enabled) GREEN else GREY
            extractor.text(font, "$cursor${row.name}: $state", 20, 40 + i * 12, color, true)
        }
        extractor.text(
            font,
            "Up/Down select  -  Enter toggle  -  Esc close",
            20,
            40 + rows.size * 12 + 10,
            DARK_GREY,
            true,
        )
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val rows = controller.rows()
        if (rows.isNotEmpty()) {
            when (event.key()) {
                GLFW.GLFW_KEY_UP -> { selected = (selected - 1 + rows.size) % rows.size; return true }
                GLFW.GLFW_KEY_DOWN -> { selected = (selected + 1) % rows.size; return true }
                GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    rows.getOrNull(selected)?.let { controller.onToggle(it.id) }
                    return true
                }
            }
        }
        return super.keyPressed(event)
    }

    override fun isPauseScreen(): Boolean = false

    private companion object {
        const val WHITE = 0xFFFFFFFF.toInt()
        const val GREEN = 0xFF55FF55.toInt()
        const val GREY = 0xFFAAAAAA.toInt()
        const val DARK_GREY = 0xFF808080.toInt()
    }
}

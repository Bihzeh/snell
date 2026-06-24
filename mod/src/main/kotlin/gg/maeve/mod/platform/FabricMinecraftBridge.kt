package gg.maeve.mod.platform

import gg.maeve.mod.ui.ModMenuController
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW
import java.nio.file.Path

/**
 * The single file that touches Minecraft 26.2 + Fabric API. All symbol names here
 * were verified against the unobfuscated 26.2 jar (Mojang mappings). Rendering uses
 * the retained-mode GuiGraphicsExtractor (Blaze3D / Vulkan-safe), never raw GL.
 */
class FabricMinecraftBridge : MinecraftBridge {

    override fun configDir(): Path =
        FabricLoader.getInstance().configDir.resolve("maeve")

    override fun installHud(render: (HudCanvas, GameContext) -> Unit) {
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("maeve", "hud"),
            HudElement { extractor, _ ->
                val mc = Minecraft.getInstance()
                render(ExtractorCanvas(extractor, mc.font), capture(mc))
            },
        )
    }

    override fun installMenuKeybind(onOpen: () -> Unit) {
        val key = KeyMappingHelper.registerKeyMapping(
            KeyMapping(
                "key.maeve.menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyMapping.Category.MISC,
            ),
        )
        ClientTickEvents.END_CLIENT_TICK.register(
            ClientTickEvents.EndTick { _ ->
                while (key.consumeClick()) onOpen()
            },
        )
    }

    override fun openModMenu(controller: ModMenuController) {
        Minecraft.getInstance().setScreenAndShow(MaeveMenuScreen(controller))
    }

    private fun capture(mc: Minecraft): GameContext {
        val player = mc.player
        val pos = player?.position()
        val opts = mc.options
        return GameContext(
            fps = mc.fps,
            inWorld = player != null && mc.level != null,
            playerX = pos?.x ?: 0.0,
            playerY = pos?.y ?: 0.0,
            playerZ = pos?.z ?: 0.0,
            keyForward = opts.keyUp.isDown,
            keyBack = opts.keyDown.isDown,
            keyLeft = opts.keyLeft.isDown,
            keyRight = opts.keyRight.isDown,
        )
    }

    /** HudCanvas backed by the 26.2 retained-mode extractor. */
    private class ExtractorCanvas(
        private val extractor: GuiGraphicsExtractor,
        private val font: Font,
    ) : HudCanvas {
        override fun drawText(x: Int, y: Int, text: String, color: Int) {
            extractor.text(font, text, x, y, color, true) // dropShadow = true
        }

        override fun textWidth(text: String): Int = font.width(text)
        override val lineHeight: Int get() = font.lineHeight
    }
}

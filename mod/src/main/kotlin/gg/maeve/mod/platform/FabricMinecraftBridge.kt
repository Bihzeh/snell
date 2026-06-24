package gg.maeve.mod.platform

/*
 * ============================================================================
 * VERSION-SENSITIVE FILE — the ONLY place that references Minecraft internals.
 * ============================================================================
 *
 * Target: Minecraft 26.1 with MOJANG OFFICIAL MAPPINGS (not Yarn). Under Mojang
 * mappings the symbols differ from Yarn, e.g.:
 *     net.minecraft.client.Minecraft           (Yarn: MinecraftClient)
 *     Minecraft.getInstance()                  (Yarn: MinecraftClient.getInstance())
 *     minecraft.player : LocalPlayer           (Yarn: client.player : ClientPlayerEntity)
 *     minecraft.getFps() / fps field           (confirm exact accessor in 26.1)
 *     net.minecraft.client.gui.GuiGraphics     (Yarn: DrawContext)  <- HUD draw surface
 *
 * The HUD render hook in recent Fabric API is HudLayerRegistrationCallback /
 * HudElementRegistry (it replaced HudRenderCallback). CONFIRM the exact 26.1
 * Fabric API entrypoint and symbol names against docs.fabricmc.net before the
 * first real build, then replace the TODO bodies below. The structure is final;
 * only the Minecraft/Fabric symbol bindings need filling in.
 *
 * Until those bindings are filled in this file will not compile — it is the
 * intentional, isolated integration boundary for Phase 1 implementation.
 */
class FabricMinecraftBridge : MinecraftBridge {

    override fun captureContext(): GameContext {
        TODO(
            "Bind to Minecraft 26.1 (Mojang mappings): read fps, player x/y/z, " +
            "and movement key states; map to GameContext."
        )
    }

    override fun installHudRenderer(onRender: (HudCanvas, GameContext) -> Unit) {
        TODO(
            "Register with the Fabric HUD render callback for 26.1. Wrap GuiGraphics " +
            "in a HudCanvas (drawText via GuiGraphics.drawString / Blaze3D), then call " +
            "onRender(canvas, captureContext())."
        )
    }

    override fun registerMenuKeybind(onOpen: () -> Unit) {
        TODO(
            "Register a KeyMapping (default RIGHT_SHIFT) via Fabric KeyBindingHelper; " +
            "on press, call onOpen()."
        )
    }

    override fun configDir(): java.nio.file.Path {
        TODO("Return FabricLoader.getInstance().configDir.resolve(\"maeve\").")
    }
}

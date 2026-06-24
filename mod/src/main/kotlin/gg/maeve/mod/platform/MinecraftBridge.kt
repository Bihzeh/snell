package gg.maeve.mod.platform

/**
 * The seam between Maeve's pure logic and Minecraft/Fabric internals. Everything
 * version-sensitive lives behind this interface; see FabricMinecraftBridge for the
 * single implementation that touches Minecraft classes.
 */
interface MinecraftBridge {
    /** Build this frame's game state snapshot from Minecraft. */
    fun captureContext(): GameContext

    /** Register a per-frame HUD draw callback with the Fabric HUD render API. */
    fun installHudRenderer(onRender: (HudCanvas, GameContext) -> Unit)

    /** Register a keybind that opens the mod menu when pressed. */
    fun registerMenuKeybind(onOpen: () -> Unit)

    /** Path to the config directory (.minecraft/config/maeve). */
    fun configDir(): java.nio.file.Path
}

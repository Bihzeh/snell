package gg.maeve.mod.platform

import gg.maeve.mod.ui.ModMenuController

/**
 * The seam between Maeve's pure logic and Minecraft/Fabric internals. Everything
 * version-sensitive lives behind this interface; see FabricMinecraftBridge for the
 * single implementation that touches Minecraft classes.
 */
interface MinecraftBridge {
    /** Path to the config directory (.minecraft/config/maeve). */
    fun configDir(): java.nio.file.Path

    /** Register a per-frame HUD draw callback with the Fabric HUD element API. */
    fun installHud(render: (HudCanvas, GameContext) -> Unit)

    /** Register the keybind that opens the mod menu. */
    fun installMenuKeybind(onOpen: () -> Unit)

    /** Open the in-game mod menu for the given controller. */
    fun openModMenu(controller: ModMenuController)
}

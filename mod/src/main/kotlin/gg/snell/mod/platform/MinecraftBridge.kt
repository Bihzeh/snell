package gg.snell.mod.platform

import gg.snell.mod.module.ModuleManager

/**
 * The seam between Snell's pure logic and Minecraft/Fabric internals. Everything
 * version-sensitive lives behind this interface; see FabricMinecraftBridge for the
 * single implementation that touches Minecraft classes.
 */
interface MinecraftBridge {
    /** Path to the config directory (.minecraft/config/snell). */
    fun configDir(): java.nio.file.Path

    /** Register a per-frame HUD draw callback with the Fabric HUD element API. */
    fun installHud(render: (HudCanvas, GameContext) -> Unit)

    /** Register the keybind that opens the mod menu. */
    fun installMenuKeybind(onOpen: () -> Unit)

    /** Open the in-game HUD editor (drag/show-hide/style) for the given modules. */
    fun openHudEditor(modules: ModuleManager)

    /**
     * Add the Snell buttons (HUD Editor / Discord / Cosmetics) to the VANILLA title + pause screens
     * while [enabled] returns true. The vanilla screens themselves are untouched — no replacement,
     * no re-skin; Snell stays a lightweight overlay on the authentic Minecraft menus.
     */
    fun installMenuButtons(enabled: () -> Boolean, openEditor: () -> Unit)

    /** Register the bundled Geist font resource pack as an available (not-yet-applied) pack. */
    fun registerFontPack()

    /** Enable/disable the Geist font pack at runtime (selects it + reloads resources). */
    fun setCustomFont(enabled: Boolean)

    /** Whether the font pack is currently selected. */
    fun isCustomFontEnabled(): Boolean

    /** Apply the persisted font state once after the client finishes its initial load. */
    fun applyCustomFontOnStartup(enabled: Boolean)
}

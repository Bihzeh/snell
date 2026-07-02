package gg.snell.mod

import gg.snell.mod.config.Config
import gg.snell.mod.module.FontModule
import gg.snell.mod.module.ModuleManager
import gg.snell.mod.module.hud.ClockModule
import gg.snell.mod.module.hud.CompassModule
import gg.snell.mod.module.hud.CoordsModule
import gg.snell.mod.module.hud.CpsModule
import gg.snell.mod.module.hud.DayModule
import gg.snell.mod.module.hud.DirectionModule
import gg.snell.mod.module.hud.FpsModule
import gg.snell.mod.module.hud.KeystrokesModule
import gg.snell.mod.module.hud.SpeedModule
import gg.snell.mod.platform.FabricMinecraftBridge
import gg.snell.mod.platform.MinecraftBridge
import gg.snell.mod.render.HudRenderController
import net.fabricmc.api.ClientModInitializer

/**
 * Snell client entrypoint. Wires the module system, HUD renderer, config, the font pack, and
 * the Minecraft bridge. All Minecraft-specific work is delegated to MinecraftBridge.
 */
class SnellMod : ClientModInitializer {

    override fun onInitializeClient() {
        val bridge: MinecraftBridge = FabricMinecraftBridge()
        bridge.registerFontPack() // register the pack source only (no reload during init)

        val config = Config(bridge.configDir()).apply { load() }
        val modules = ModuleManager(config)

        modules.register(FontModule())
        modules.register(FpsModule())
        modules.register(CoordsModule())
        modules.register(CompassModule())
        modules.register(KeystrokesModule())
        modules.register(DirectionModule())
        modules.register(DayModule())
        modules.register(ClockModule())
        modules.register(SpeedModule())
        modules.register(CpsModule())

        val hud = HudRenderController(modules)
        bridge.installHud { canvas, ctx -> hud.draw(canvas, ctx) }

        // Toggling any module (from the editor's module browser, etc.) reloads the font pack when needed.
        modules.onEnabledChanged = { id, enabled -> if (id == "font") bridge.setCustomFont(enabled) }

        // Right-Shift opens the HUD editor directly (its "Modules" button browses/toggles everything).
        bridge.installMenuKeybind { bridge.openHudEditor(modules) }

        // Snell buttons (HUD Editor / Discord / Cosmetics) on the untouched vanilla title/pause —
        // the client keeps the authentic Minecraft menus instead of the old bespoke re-skins.
        bridge.installMenuButtons({ config.isMenusEnabled() }, { bridge.openHudEditor(modules) })

        // Apply the persisted font choice after the first client tick (never reload during init).
        bridge.applyCustomFontOnStartup(modules.byId("font")?.enabled ?: true)

        LOG.info("Snell initialized: {} modules", modules.all().size)
    }

    private companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger("Snell")
    }
}

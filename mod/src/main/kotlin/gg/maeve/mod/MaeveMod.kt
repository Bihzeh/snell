package gg.maeve.mod

import gg.maeve.mod.config.Config
import gg.maeve.mod.module.FontModule
import gg.maeve.mod.module.ModuleManager
import gg.maeve.mod.module.hud.ClockModule
import gg.maeve.mod.module.hud.CoordsModule
import gg.maeve.mod.module.hud.DayModule
import gg.maeve.mod.module.hud.DirectionModule
import gg.maeve.mod.module.hud.FpsModule
import gg.maeve.mod.module.hud.KeystrokesModule
import gg.maeve.mod.module.hud.SpeedModule
import gg.maeve.mod.platform.FabricMinecraftBridge
import gg.maeve.mod.platform.MinecraftBridge
import gg.maeve.mod.render.HudRenderController
import gg.maeve.mod.ui.ModMenuController
import net.fabricmc.api.ClientModInitializer

/**
 * Maeve client entrypoint. Wires the module system, HUD renderer, config, the font pack, and
 * the Minecraft bridge. All Minecraft-specific work is delegated to MinecraftBridge.
 */
class MaeveMod : ClientModInitializer {

    override fun onInitializeClient() {
        val bridge: MinecraftBridge = FabricMinecraftBridge()
        bridge.registerFontPack() // register the pack source only (no reload during init)

        val config = Config(bridge.configDir()).apply { load() }
        val modules = ModuleManager(config)

        modules.register(FontModule())
        modules.register(FpsModule())
        modules.register(CoordsModule())
        modules.register(KeystrokesModule())
        modules.register(DirectionModule())
        modules.register(DayModule())
        modules.register(ClockModule())
        modules.register(SpeedModule())

        val hud = HudRenderController(modules)
        bridge.installHud { canvas, ctx -> hud.draw(canvas, ctx) }

        // Cosmetics: Phase 3 wires HttpCosmeticsClient + the player-render mixin.

        val menu = ModMenuController(modules) { id, enabled -> if (id == "font") bridge.setCustomFont(enabled) }
        bridge.installMenuKeybind { bridge.openModMenu(menu) }

        // Apply the persisted font choice after the first client tick (never reload during init).
        bridge.applyCustomFontOnStartup(modules.byId("font")?.enabled ?: true)

        LOG.info("Maeve initialized: {} modules", modules.all().size)
    }

    private companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger("Maeve")
    }
}

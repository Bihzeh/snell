package gg.maeve.mod

import gg.maeve.mod.config.Config
import gg.maeve.mod.cosmetics.LocalStubCosmeticsClient
import gg.maeve.mod.module.ModuleManager
import gg.maeve.mod.module.hud.CoordsModule
import gg.maeve.mod.module.hud.FpsModule
import gg.maeve.mod.module.hud.KeystrokesModule
import gg.maeve.mod.platform.FabricMinecraftBridge
import gg.maeve.mod.platform.MinecraftBridge
import gg.maeve.mod.render.HudRenderController
import gg.maeve.mod.ui.ModMenuController
import net.fabricmc.api.ClientModInitializer

/**
 * Maeve client entrypoint. Wires the module system, HUD renderer, config, and the
 * Minecraft bridge. All Minecraft-specific work is delegated to MinecraftBridge.
 */
class MaeveMod : ClientModInitializer {

    override fun onInitializeClient() {
        val bridge: MinecraftBridge = FabricMinecraftBridge()

        val config = Config(bridge.configDir()).apply { load() }
        val modules = ModuleManager(config)

        // MVP module set. Phase 2 adds CPS, armor/potion HUD, scoreboard, zoom, etc.
        modules.register(FpsModule())
        modules.register(CoordsModule())
        modules.register(KeystrokesModule())

        val hud = HudRenderController(modules)
        bridge.installHud { canvas, ctx -> hud.draw(canvas, ctx) }

        @Suppress("UNUSED_VARIABLE")
        val cosmetics = LocalStubCosmeticsClient() // Phase 3: HttpCosmeticsClient

        val menu = ModMenuController(modules)
        bridge.installMenuKeybind { bridge.openModMenu(menu) }

        LOG.info("Maeve initialized: {} modules", modules.all().size)
    }

    private companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger("Maeve")
    }
}

package gg.snell.mod.mixin

import gg.snell.mod.platform.SnellMenus
import gg.snell.mod.platform.screens.SnellPauseScreen
import gg.snell.mod.platform.screens.SnellTitleScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.PauseScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Swaps vanilla menus for the bespoke Snell screens at the moment they would be shown. Hooks
 * Minecraft.setScreen at HEAD: if the incoming screen is a vanilla type we re-skin (and the
 * overhaul is enabled), it cancels the original call and opens our equivalent instead.
 *
 * No recursion: the Snell screens do NOT extend the vanilla types, so the re-issued setScreen
 * passes straight through. The Snell screens themselves still open vanilla sub-screens, which this
 * hook re-skins in turn as those bespoke screens are added.
 */
@Mixin(Minecraft::class)
class ScreenReplacementMixin {

    @Inject(method = ["setScreenAndShow"], at = [At("HEAD")], cancellable = true)
    private fun snell_replaceScreen(screen: Screen?, ci: CallbackInfo) {
        if (!SnellMenus.enabled || screen == null) return
        val replacement: Screen = when (screen) {
            is TitleScreen -> SnellTitleScreen()
            is PauseScreen -> SnellPauseScreen()
            else -> return
        }
        ci.cancel()
        Minecraft.getInstance().setScreenAndShow(replacement)
    }
}

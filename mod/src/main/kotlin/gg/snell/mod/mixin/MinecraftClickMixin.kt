package gg.snell.mod.mixin

import gg.snell.mod.module.hud.ClickTracker
import net.minecraft.client.Minecraft
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/** Counts physical mouse presses for the CPS HUD: startAttack = left, startUseItem = right. */
@Mixin(Minecraft::class)
class MinecraftClickMixin {
    @Inject(method = ["startAttack"], at = [At("HEAD")])
    private fun snell_onStartAttack(cir: CallbackInfoReturnable<Boolean>) {
        ClickTracker.onLeft()
    }

    @Inject(method = ["startUseItem"], at = [At("HEAD")])
    private fun snell_onStartUseItem(ci: CallbackInfo) {
        ClickTracker.onRight()
    }
}

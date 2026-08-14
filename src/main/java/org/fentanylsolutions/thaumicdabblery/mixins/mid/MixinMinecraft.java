package org.fentanylsolutions.thaumicdabblery.mixins.mid;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;
import org.fentanylsolutions.thaumicdabblery.feature.witcherybranch.VirtualItemUseState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Unique
    private boolean thaumicdabblery$loggedSuppressedStop;

    @Redirect(
        method = "runTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;"
                + "onStoppedUsingItem(Lnet/minecraft/entity/player/EntityPlayer;)V"),
        require = 1)
    private void thaumicdabblery$keepVirtualMysticBranchActive(PlayerControllerMP controller, EntityPlayer player) {
        if (VirtualItemUseState.isActive(player)) {
            if (!thaumicdabblery$loggedSuppressedStop) {
                ThaumicDabblery.debug("[Mystic Branch/mixin] Suppressed Minecraft.onStoppedUsingItem while B is held");
                thaumicdabblery$loggedSuppressedStop = true;
            }
        } else {
            thaumicdabblery$loggedSuppressedStop = false;
            controller.onStoppedUsingItem(player);
        }
    }
}

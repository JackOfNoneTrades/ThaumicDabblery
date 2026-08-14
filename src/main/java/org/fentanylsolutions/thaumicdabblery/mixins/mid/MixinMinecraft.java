package org.fentanylsolutions.thaumicdabblery.mixins.mid;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;

import org.fentanylsolutions.thaumicdabblery.feature.witcherybranch.VirtualItemUseState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Redirect(
        method = "runTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;"
                + "onStoppedUsingItem(Lnet/minecraft/entity/player/EntityPlayer;)V"),
        require = 1)
    private void thaumicdabblery$keepVirtualMysticBranchActive(PlayerControllerMP controller, EntityPlayer player) {
        if (!VirtualItemUseState.isActive(player)) {
            controller.onStoppedUsingItem(player);
        }
    }
}

package org.fentanylsolutions.thaumicdabblery.mixins.mid;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;
import org.fentanylsolutions.thaumicdabblery.feature.witcherybranch.VirtualItemUseState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer {

    @Unique
    private boolean thaumicdabblery$loggedVirtualBranchSubstitution;

    @Unique
    private boolean thaumicdabblery$loggedSuppressedClear;

    @Inject(method = "clearItemInUse", at = @At("HEAD"), cancellable = true)
    private void thaumicdabblery$suppressUnexpectedClear(CallbackInfo ci) {
        EntityPlayer player = (EntityPlayer) (Object) this;
        if (VirtualItemUseState.isActive(player)) {
            if (!thaumicdabblery$loggedSuppressedClear) {
                ThaumicDabblery.debug(
                    "[Mystic Branch/mixin] Suppressed EntityPlayer.clearItemInUse while virtual use is active on "
                        + (player.worldObj.isRemote ? "client" : "server"));
                thaumicdabblery$loggedSuppressedClear = true;
            }
            ci.cancel();
        } else {
            thaumicdabblery$loggedSuppressedClear = false;
        }
    }

    @Redirect(
        method = "onUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;",
            ordinal = 0),
        require = 1)
    private ItemStack thaumicdabblery$keepVirtualMysticBranchInUse(InventoryPlayer inventory) {
        EntityPlayer player = (EntityPlayer) (Object) this;
        ItemStack virtualBranch = VirtualItemUseState.getVirtualHeldItem(player);
        if (virtualBranch != null && !thaumicdabblery$loggedVirtualBranchSubstitution) {
            ThaumicDabblery.debug(
                "[Mystic Branch/mixin] EntityPlayer.onUpdate substituted the virtual branch on "
                    + (player.worldObj.isRemote ? "client" : "server"));
            thaumicdabblery$loggedVirtualBranchSubstitution = true;
        } else if (virtualBranch == null) {
            thaumicdabblery$loggedVirtualBranchSubstitution = false;
        }
        return virtualBranch != null ? virtualBranch : inventory.getCurrentItem();
    }
}

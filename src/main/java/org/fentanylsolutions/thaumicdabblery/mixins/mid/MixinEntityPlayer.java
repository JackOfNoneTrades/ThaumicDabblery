package org.fentanylsolutions.thaumicdabblery.mixins.mid;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.witcherybranch.VirtualItemUseState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer {

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
        return virtualBranch != null ? virtualBranch : inventory.getCurrentItem();
    }
}

package org.fentanylsolutions.thaumicdabblery.mixins.late.baubles;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleRules;
import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleTransfer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import baubles.api.IBauble;
import baubles.common.container.ContainerPlayerExpanded;

@Mixin(targets = "baubles.common.container.ContainerPlayerExpanded", remap = false)
public abstract class MixinContainerPlayerExpanded {

    @Inject(method = { "transferStackInSlot", "func_82846_b" }, at = @At("HEAD"), cancellable = true)
    private void td$scriptedTransfer(EntityPlayer player, int index, CallbackInfoReturnable<ItemStack> cir) {
        ContainerPlayerExpanded container = (ContainerPlayerExpanded) (Object) this;
        if (index < 0 || index >= container.inventorySlots.size()) return;
        Slot source = (Slot) container.inventorySlots.get(index);
        ItemStack stack = source.getStack();
        if (stack != null && (BaubleRules.hasRule(stack)
            || (source.inventory == container.baubles && !(stack.getItem() instanceof IBauble)))) {
            cir.setReturnValue(BaubleTransfer.transfer(container, source, player));
        }
    }
}

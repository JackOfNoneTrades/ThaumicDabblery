package org.fentanylsolutions.thaumicdabblery.mixins.late.baubles;

import java.lang.ref.WeakReference;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleHooks;
import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import baubles.api.expanded.BaubleExpandedSlots;
import baubles.common.container.InventoryBaubles;

@Mixin(value = InventoryBaubles.class, remap = false)
public abstract class MixinInventoryBaubles {

    @Shadow
    public WeakReference<EntityPlayer> player;

    @Inject(method = { "isItemValidForSlot", "func_94041_b" }, at = @At("HEAD"), cancellable = true)
    private void td$valid(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (BaubleRules.hasRule(stack))
            cir.setReturnValue(BaubleHooks.canEquip(stack, player.get(), BaubleExpandedSlots.getSlotType(slot)));
    }

    @Redirect(
        method = { "setInventorySlotContents", "func_70299_a" },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;",
            remap = true))
    private Item td$safeCallbacks(ItemStack stack) {
        return BaubleHooks.dispatch(stack);
    }
}

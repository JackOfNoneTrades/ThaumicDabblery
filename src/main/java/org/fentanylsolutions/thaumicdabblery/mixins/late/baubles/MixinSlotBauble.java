package org.fentanylsolutions.thaumicdabblery.mixins.late.baubles;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleHooks;
import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import baubles.common.container.InventoryBaubles;
import baubles.common.container.SlotBauble;

@Mixin(value = SlotBauble.class, remap = false)
public abstract class MixinSlotBauble extends Slot {

    private MixinSlotBauble(IInventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Inject(method = { "isItemValid", "func_75214_a" }, at = @At("HEAD"), cancellable = true)
    private void td$valid(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (BaubleRules.hasRule(stack)) cir.setReturnValue(
            BaubleHooks.canEquip(
                stack,
                ((InventoryBaubles) inventory).player.get(),
                ((SlotBauble) (Object) this).getSlotType()));
    }

    @Redirect(
        method = { "canTakeStack", "func_82869_a" },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;",
            remap = true))
    private Item td$safeRemoval(ItemStack stack) {
        return BaubleHooks.dispatch(stack);
    }
}

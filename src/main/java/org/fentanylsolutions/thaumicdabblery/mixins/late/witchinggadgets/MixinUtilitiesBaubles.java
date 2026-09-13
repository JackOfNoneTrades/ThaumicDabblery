package org.fentanylsolutions.thaumicdabblery.mixins.late.witchinggadgets;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleSlotsFeature;
import org.fentanylsolutions.thaumicdabblery.feature.baubles.WitchingBaubleSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import baubles.api.BaublesApi;
import witchinggadgets.common.items.baubles.ItemCloak;
import witchinggadgets.common.util.Utilities;

@Mixin(value = Utilities.class, remap = false)
public abstract class MixinUtilitiesBaubles {

    @Inject(method = "getActiveMagicalCloak", at = @At("HEAD"), cancellable = true)
    private static void td$cloaks(EntityPlayer player, CallbackInfoReturnable<ItemStack[]> cir) {
        if (!BaubleSlotsFeature.isEnabled()) return;
        ArrayList<ItemStack> found = new ArrayList<>();
        IInventory inv = BaublesApi.getBaubles(player);
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemCloak) found.add(stack);
        }
        cir.setReturnValue(found.toArray(new ItemStack[0]));
    }

    @Inject(method = "updateActiveMagicalCloak", at = @At("HEAD"), cancellable = true)
    private static void td$saveCloak(EntityPlayer player, ItemStack cloak, CallbackInfo ci) {
        if (BaubleSlotsFeature.isEnabled()) {
            if (cloak != null) WitchingBaubleSlots.sync(player, cloak);
            ci.cancel();
        }
    }

    @ModifyConstant(method = "consumeVisFromInventoryWithoutDiscount", constant = @Constant(intValue = 4))
    private static int td$amulets(int original, EntityPlayer player, thaumcraft.api.aspects.AspectList cost) {
        return BaubleSlotsFeature.isEnabled() ? BaublesApi.getBaubles(player)
            .getSizeInventory() : original;
    }
}

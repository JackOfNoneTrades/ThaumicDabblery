package org.fentanylsolutions.thaumicdabblery.mixins.late.witchinggadgets.legacy;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleSlotsFeature;
import org.fentanylsolutions.thaumicdabblery.feature.baubles.LegacyWitchingBaubles;
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

@Mixin(targets = "witchinggadgets.common.util.Utilities", remap = false)
public abstract class MixinUtilitiesBaubles {

    @Inject(method = "getActiveMagicalCloak", at = @At("RETURN"), cancellable = true)
    private static void td$includeMovedCloaks(EntityPlayer player, CallbackInfoReturnable<ItemStack[]> cir) {
        if (!BaubleSlotsFeature.isEnabled()) return;
        ArrayList<ItemStack> found = new ArrayList<>(Arrays.asList(cir.getReturnValue()));
        IInventory inv = BaublesApi.getBaubles(player);
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemCloak) {
                LegacyWitchingBaubles.remember(stack);
                if (!found.contains(stack)) found.add(stack);
            }
        }
        cir.setReturnValue(found.toArray(new ItemStack[0]));
    }

    @Inject(method = "updateActiveMagicalCloak", at = @At("HEAD"), cancellable = true)
    private static void td$saveMovedCloak(EntityPlayer player, ItemStack cloak, CallbackInfo ci) {
        if (cloak != null && (LegacyWitchingBaubles.wasBauble(cloak)
            || (BaubleSlotsFeature.isEnabled() && LegacyWitchingBaubles.slot(player, cloak) >= 0))) {
            WitchingBaubleSlots.sync(player, cloak);
            ci.cancel();
        }
    }

    @ModifyConstant(method = "consumeVisFromInventoryWithoutDiscount", constant = @Constant(intValue = 4))
    private static int td$allAmulets(int original, EntityPlayer player, thaumcraft.api.aspects.AspectList cost) {
        return BaubleSlotsFeature.isEnabled() ? BaublesApi.getBaubles(player)
            .getSizeInventory() : original;
    }
}

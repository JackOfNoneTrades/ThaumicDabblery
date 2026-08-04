package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.visdiscount.VisDiscountFeature;
import org.fentanylsolutions.thaumicdabblery.feature.visdiscount.VisDiscountRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import baubles.api.BaublesApi;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.wands.WandManager;

@Mixin(value = WandManager.class, remap = false)
public abstract class MixinWandManager {

    private static final int THAUMCRAFT_BAUBLE_SLOTS = 4;

    @Redirect(
        method = "getTotalVisDiscount",
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/api/IVisDiscountGear;getVisDiscount(Lnet/minecraft/item/ItemStack;"
                + "Lnet/minecraft/entity/player/EntityPlayer;Lthaumcraft/api/aspects/Aspect;)I"),
        require = 2)
    private static int thaumicdabblery$replaceNativeDiscount(IVisDiscountGear gear, ItemStack stack,
        EntityPlayer player, Aspect aspect) {
        if (VisDiscountFeature.isEnabled()) {
            Integer scriptedDiscount = VisDiscountRegistry.get(stack, aspect);
            if (scriptedDiscount != null) {
                return scriptedDiscount;
            }
        }
        return gear.getVisDiscount(stack, player, aspect);
    }

    @Inject(method = "getTotalVisDiscount", at = @At("RETURN"), cancellable = true)
    private static void thaumicdabblery$addScriptedAndExpandedDiscounts(EntityPlayer player, Aspect aspect,
        CallbackInfoReturnable<Float> cir) {
        if (!VisDiscountFeature.isEnabled() || player == null) {
            return;
        }

        int additionalDiscount = 0;
        IInventory baubles = BaublesApi.getBaubles(player);
        int baubleSlots = baubles.getSizeInventory();

        for (int slot = 0; slot < Math.min(THAUMCRAFT_BAUBLE_SLOTS, baubleSlots); slot++) {
            additionalDiscount += getScriptedDiscountForNonNativeGear(baubles.getStackInSlot(slot), aspect);
        }
        for (int slot = THAUMCRAFT_BAUBLE_SLOTS; slot < baubleSlots; slot++) {
            additionalDiscount += getFullDiscount(baubles.getStackInSlot(slot), player, aspect);
        }
        for (int slot = 0; slot < 4; slot++) {
            additionalDiscount += getScriptedDiscountForNonNativeGear(player.inventory.armorItemInSlot(slot), aspect);
        }

        if (additionalDiscount != 0) {
            cir.setReturnValue(cir.getReturnValueF() + additionalDiscount / 100.0F);
        }
    }

    private static int getScriptedDiscountForNonNativeGear(ItemStack stack, Aspect aspect) {
        if (stack == null || stack.getItem() instanceof IVisDiscountGear) {
            return 0;
        }
        Integer scriptedDiscount = VisDiscountRegistry.get(stack, aspect);
        return scriptedDiscount == null ? 0 : scriptedDiscount;
    }

    private static int getFullDiscount(ItemStack stack, EntityPlayer player, Aspect aspect) {
        if (stack == null) {
            return 0;
        }
        Integer scriptedDiscount = VisDiscountRegistry.get(stack, aspect);
        if (scriptedDiscount != null) {
            return scriptedDiscount;
        }
        if (stack.getItem() instanceof IVisDiscountGear) {
            return ((IVisDiscountGear) stack.getItem()).getVisDiscount(stack, player, aspect);
        }
        return 0;
    }
}

package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumcraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.wandcomponents.WandComponentVisDiscountFeature;
import org.fentanylsolutions.thaumicdabblery.feature.wandcomponents.WandComponentVisDiscountRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.WandCap;
import thaumcraft.common.items.wands.ItemWandCasting;

@Mixin(value = ItemWandCasting.class, remap = false)
public abstract class MixinItemWandCasting {

    @Redirect(
        method = "getConsumptionModifier",
        at = @At(value = "INVOKE", target = "Lthaumcraft/api/wands/WandCap;getSpecialCostModifier()F"),
        require = 1)
    private float thaumicdabblery$replaceSpecialCapModifier(WandCap cap, ItemStack stack, EntityPlayer player,
        Aspect aspect, boolean crafting) {
        return resolve(stack, cap, aspect, cap.getSpecialCostModifier());
    }

    @Redirect(
        method = "getConsumptionModifier",
        at = @At(value = "INVOKE", target = "Lthaumcraft/api/wands/WandCap;getBaseCostModifier()F"),
        require = 1)
    private float thaumicdabblery$replaceBaseCapModifier(WandCap cap, ItemStack stack, EntityPlayer player,
        Aspect aspect, boolean crafting) {
        return resolve(stack, cap, aspect, cap.getBaseCostModifier());
    }

    private static float resolve(ItemStack stack, WandCap cap, Aspect aspect, float nativeModifier) {
        if (!WandComponentVisDiscountFeature.isEnabled()) {
            return nativeModifier;
        }
        return WandComponentVisDiscountRegistry.resolve(stack, cap, aspect, nativeModifier);
    }
}

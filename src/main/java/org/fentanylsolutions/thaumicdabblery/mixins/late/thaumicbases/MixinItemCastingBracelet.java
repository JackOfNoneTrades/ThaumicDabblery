package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumicbases;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.Config;
import org.fentanylsolutions.thaumicdabblery.feature.wandcomponents.WandComponentVisDiscountFeature;
import org.fentanylsolutions.thaumicdabblery.feature.wandcomponents.WandComponentVisDiscountRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.WandCap;

@Pseudo
@Mixin(targets = "tb.common.item.ItemCastingBracelet", remap = false)
public abstract class MixinItemCastingBracelet {

    @ModifyArg(
        method = "getConsumptionModifier",
        at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"),
        index = 1,
        require = 1)
    private float thaumicdabblery$minimumVisCost(float originalMinimum) {
        return Config.minimumVisCostPercent / 100.0F;
    }

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

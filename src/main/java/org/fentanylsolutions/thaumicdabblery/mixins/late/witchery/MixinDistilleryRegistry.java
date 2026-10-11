package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.witcherydistillery.DistilleryRegistry;
import org.fentanylsolutions.thaumicdabblery.feature.witcherydistillery.WitcheryDistilleryFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.crafting.DistilleryRecipes;
import com.emoniph.witchery.crafting.DistilleryRecipes.DistilleryRecipe;

@Mixin(value = DistilleryRecipes.class, remap = false)
public abstract class MixinDistilleryRegistry {

    @Inject(method = "getDistillingResult", at = @At("HEAD"), cancellable = true, require = 1)
    private void td$select(ItemStack a, ItemStack b, ItemStack jars, CallbackInfoReturnable<DistilleryRecipe> cir) {
        if (WitcheryDistilleryFeature.isEnabled()) cir.setReturnValue(DistilleryRegistry.select(a, b, jars));
    }
}

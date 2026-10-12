package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.witcheryspinningwheel.SpinningRegistry;
import org.fentanylsolutions.thaumicdabblery.feature.witcheryspinningwheel.WitcherySpinningWheelFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.crafting.SpinningRecipes;
import com.emoniph.witchery.crafting.SpinningRecipes.SpinningRecipe;

@Mixin(value = SpinningRecipes.class, remap = false)
public abstract class MixinSpinningRegistry {

    @Inject(method = "getRecipe", at = @At("HEAD"), cancellable = true, require = 1)
    private void td$select(ItemStack input, ItemStack[] modifiers, CallbackInfoReturnable<SpinningRecipe> cir) {
        if (WitcherySpinningWheelFeature.isEnabled()) cir.setReturnValue(SpinningRegistry.select(input, modifiers));
    }
}

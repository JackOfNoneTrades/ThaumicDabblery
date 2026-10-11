package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.witcherycauldron.CauldronRecipes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.brewing.action.BrewActionList;
import com.emoniph.witchery.brewing.action.BrewActionRitualRecipe;

@Mixin(value = BrewActionRitualRecipe.class, remap = false)
public abstract class MixinCauldronRecipe {

    @Inject(method = "getRecipeResult", at = @At("HEAD"), cancellable = true, require = 1)
    private void td$recipe(BrewActionList list, CallbackInfoReturnable<ItemStack> cir) {
        BrewActionRitualRecipe action = (BrewActionRitualRecipe) (Object) this;
        if (CauldronRecipes.manages(action.ITEM_KEY)) {
            CauldronRecipes.Recipe recipe = CauldronRecipes.select(action.ITEM_KEY, list);
            cir.setReturnValue(recipe == null ? null : recipe.output.copy());
        }
    }
}

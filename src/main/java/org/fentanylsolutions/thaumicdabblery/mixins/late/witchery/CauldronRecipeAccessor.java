package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.emoniph.witchery.brewing.action.BrewActionRitualRecipe;

@Mixin(value = BrewActionRitualRecipe.class, remap = false)
public interface CauldronRecipeAccessor {

    @Accessor("recipes")
    BrewActionRitualRecipe.Recipe[] td$getRecipes();
}

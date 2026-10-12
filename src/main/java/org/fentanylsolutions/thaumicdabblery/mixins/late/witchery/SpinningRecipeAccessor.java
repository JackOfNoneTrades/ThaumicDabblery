package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.emoniph.witchery.crafting.SpinningRecipes.SpinningRecipe;

@Mixin(value = SpinningRecipe.class, remap = false)
public interface SpinningRecipeAccessor {

    @Invoker("<init>")
    static SpinningRecipe td$create(ItemStack output, ItemStack input, ItemStack[] modifiers) {
        throw new AssertionError();
    }
}

package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.emoniph.witchery.crafting.DistilleryRecipes.DistilleryRecipe;

@Mixin(value = DistilleryRecipe.class, remap = false)
public interface DistilleryRecipeAccessor {

    @Invoker("<init>")
    static DistilleryRecipe td$create(ItemStack a, ItemStack b, int jars, ItemStack x, ItemStack y, ItemStack z,
        ItemStack w) {
        throw new AssertionError();
    }

    @Invoker("isMatch")
    boolean td$matches(ItemStack a, ItemStack b, ItemStack jars);

    @Invoker("isMatch")
    boolean td$matchesInput(ItemStack a, ItemStack b);
}

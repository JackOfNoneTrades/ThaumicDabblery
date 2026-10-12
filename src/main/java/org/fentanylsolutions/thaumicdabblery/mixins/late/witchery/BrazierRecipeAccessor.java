package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.emoniph.witchery.crafting.BrazierRecipes.BrazierRecipe;

@Mixin(value = BrazierRecipe.class, remap = false)
public interface BrazierRecipeAccessor {

    @Invoker("<init>")
    static BrazierRecipe td$create(String name, boolean inBook, int ticks, ItemStack[] items) {
        throw new AssertionError();
    }
}

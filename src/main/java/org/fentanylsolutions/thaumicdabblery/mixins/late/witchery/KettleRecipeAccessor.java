package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.emoniph.witchery.crafting.KettleRecipes.KettleRecipe;

@Mixin(value = KettleRecipe.class, remap = false)
public interface KettleRecipeAccessor {

    @Accessor("hatBonus")
    int td$bonus();

    @Accessor("familiarType")
    int td$familiar();

    @Accessor("dimension")
    int td$dimension();

    @Accessor("unlocalizedName")
    String td$description();

    @Invoker("<init>")
    static KettleRecipe td$create(ItemStack output, int bonus, int familiar, float power, int color, int dimension,
        boolean inBook, ItemStack[] inputs) {
        throw new AssertionError();
    }
}

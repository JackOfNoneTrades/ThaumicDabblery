package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

import org.fentanylsolutions.thaumicdabblery.feature.witcheryoven.OvenRecipes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.emoniph.witchery.blocks.BlockWitchesOven.ContainerWitchesOven;

@Mixin(value = ContainerWitchesOven.class, remap = false)
public abstract class MixinOvenContainer {

    @Redirect(
        method = { "transferStackInSlot", "func_82846_b" },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/crafting/FurnaceRecipes;getSmeltingResult(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;",
            remap = true),
        require = 1)
    private ItemStack thaumicdabblery$customInput(FurnaceRecipes furnace, ItemStack input) {
        OvenRecipes.Recipe recipe = OvenRecipes.select(input);
        return recipe == null ? furnace.getSmeltingResult(input) : recipe.output;
    }
}

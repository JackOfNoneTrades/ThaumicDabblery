package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.witcheryoven.OvenRecipes;
import org.fentanylsolutions.thaumicdabblery.feature.witcheryoven.OvenRecipes.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.blocks.BlockWitchesOven.TileEntityWitchesOven;

@Mixin(value = TileEntityWitchesOven.class, remap = false)
public abstract class MixinWitchesOven {

    @Shadow
    private double getFumeFunnelsChance() {
        throw new AssertionError();
    }

    @Unique
    private Recipe thaumicdabblery$previousRecipe;
    @Unique
    private ItemStack thaumicdabblery$previousInput;
    @Unique
    private int thaumicdabblery$generation;

    @Inject(method = { "updateEntity", "func_145845_h" }, at = @At("HEAD"), require = 1)
    private void thaumicdabblery$resetChangedRecipe(CallbackInfo ci) {
        TileEntityWitchesOven oven = (TileEntityWitchesOven) (Object) this;
        if (oven.getWorldObj().isRemote) return;
        ItemStack input = oven.getStackInSlot(0);
        Recipe recipe = OvenRecipes.select(input);
        if (recipe != null || thaumicdabblery$previousRecipe != null) {
            boolean same = input != null && thaumicdabblery$previousInput != null
                && input.isItemEqual(thaumicdabblery$previousInput)
                && ItemStack.areItemStackTagsEqual(input, thaumicdabblery$previousInput);
            if (recipe != thaumicdabblery$previousRecipe || !same
                || thaumicdabblery$generation != OvenRecipes.generation) oven.furnaceCookTime = 0;
        }
        thaumicdabblery$previousRecipe = recipe;
        thaumicdabblery$previousInput = input == null ? null : input.copy();
        thaumicdabblery$generation = OvenRecipes.generation;
    }

    @Inject(method = "canSmelt", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumicdabblery$canCraft(CallbackInfoReturnable<Boolean> cir) {
        TileEntityWitchesOven oven = (TileEntityWitchesOven) (Object) this;
        Recipe recipe = OvenRecipes.select(oven.getStackInSlot(0));
        if (recipe != null) cir.setReturnValue(OvenRecipes.canCraft(oven, recipe));
    }

    @Inject(method = "smeltItem", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumicdabblery$craft(CallbackInfo ci) {
        TileEntityWitchesOven oven = (TileEntityWitchesOven) (Object) this;
        Recipe recipe = OvenRecipes.select(oven.getStackInSlot(0));
        if (recipe != null) {
            OvenRecipes.craft(oven, recipe, recipe.funnels ? getFumeFunnelsChance() : 0);
            ci.cancel();
        }
    }
}

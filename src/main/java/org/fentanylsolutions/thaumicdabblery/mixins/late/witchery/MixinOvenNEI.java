package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import java.util.List;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.witcheryoven.OvenNEI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;

@Mixin(targets = "com.emoniph.witchery.integration.NEIWitchesOvenRecipeHandler", remap = false)
public abstract class MixinOvenNEI extends TemplateRecipeHandler {

    @Inject(method = "loadCraftingRecipes(Ljava/lang/String;[Ljava/lang/Object;)V", at = @At("TAIL"), require = 1)
    private void thaumicdabblery$all(String id, Object[] results, CallbackInfo ci) {
        if ("witchery_cooking".equals(id)) OvenNEI.populate(this, null, false);
    }

    @Inject(method = "loadCraftingRecipes(Lnet/minecraft/item/ItemStack;)V", at = @At("TAIL"), require = 1)
    private void thaumicdabblery$output(ItemStack result, CallbackInfo ci) {
        OvenNEI.populate(this, result, false);
    }

    @Inject(method = "loadUsageRecipes(Lnet/minecraft/item/ItemStack;)V", at = @At("TAIL"), require = 1)
    private void thaumicdabblery$input(ItemStack input, CallbackInfo ci) {
        OvenNEI.populate(this, input, true);
    }

    @Override
    public List<String> handleItemTooltip(GuiRecipe<?> gui, ItemStack stack, List<String> lines, int recipe) {
        OvenNEI.tooltip(this, recipe, stack, lines);
        return super.handleItemTooltip(gui, stack, lines, recipe);
    }
}

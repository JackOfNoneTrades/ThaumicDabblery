package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import java.util.List;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.witcherydistillery.DistilleryNEI;
import org.fentanylsolutions.thaumicdabblery.feature.witcherydistillery.WitcheryDistilleryFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;

@Mixin(targets = "com.emoniph.witchery.integration.NEIDistilleryRecipeHandler", remap = false)
public abstract class MixinDistilleryNEI extends TemplateRecipeHandler {

    @Inject(
        method = "loadCraftingRecipes(Ljava/lang/String;[Ljava/lang/Object;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private void td$all(String id, Object[] results, CallbackInfo ci) {
        if (WitcheryDistilleryFeature.isEnabled() && "witchery_distilling".equals(id)) {
            DistilleryNEI.populate(this, null, false);
            ci.cancel();
        }
    }

    @Inject(
        method = "loadCraftingRecipes(Lnet/minecraft/item/ItemStack;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private void td$output(ItemStack result, CallbackInfo ci) {
        if (WitcheryDistilleryFeature.isEnabled()) {
            DistilleryNEI.populate(this, result, false);
            ci.cancel();
        }
    }

    @Inject(
        method = "loadUsageRecipes(Lnet/minecraft/item/ItemStack;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private void td$input(ItemStack input, CallbackInfo ci) {
        if (WitcheryDistilleryFeature.isEnabled()) {
            DistilleryNEI.populate(this, input, true);
            ci.cancel();
        }
    }

    @Override
    public List<String> handleItemTooltip(GuiRecipe<?> gui, ItemStack stack, List<String> lines, int recipe) {
        DistilleryNEI.tooltip(this, recipe, stack, lines);
        return super.handleItemTooltip(gui, stack, lines, recipe);
    }
}

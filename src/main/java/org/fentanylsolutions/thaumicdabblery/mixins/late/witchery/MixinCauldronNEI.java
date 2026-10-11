package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import java.util.List;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.witcherycauldron.CauldronNEI;
import org.fentanylsolutions.thaumicdabblery.feature.witcherycauldron.CauldronRecipes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;

@Mixin(targets = "com.emoniph.witchery.integration.NEICauldronRecipeHandler", remap = false)
public abstract class MixinCauldronNEI extends TemplateRecipeHandler {

    @Inject(
        method = "loadCraftingRecipes(Ljava/lang/String;[Ljava/lang/Object;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private void td$all(String id, Object[] results, CallbackInfo ci) {
        if (CauldronRecipes.active() && "witchery_brewing_plus".equals(id)) {
            CauldronNEI.populate(this, null, false);
            ci.cancel();
        }
    }

    @Inject(
        method = "loadCraftingRecipes(Lnet/minecraft/item/ItemStack;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private void td$output(ItemStack result, CallbackInfo ci) {
        if (CauldronRecipes.active()) {
            CauldronNEI.populate(this, result, false);
            ci.cancel();
        }
    }

    @Inject(
        method = "loadUsageRecipes(Lnet/minecraft/item/ItemStack;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private void td$input(ItemStack input, CallbackInfo ci) {
        if (CauldronRecipes.active()) {
            CauldronNEI.populate(this, input, true);
            ci.cancel();
        }
    }

    @Inject(method = "drawExtras", at = @At("TAIL"), require = 1)
    private void td$power(int recipe, CallbackInfo ci) {
        CauldronNEI.draw(this, recipe);
    }

    @Override
    public List<String> handleItemTooltip(GuiRecipe<?> gui, ItemStack stack, List<String> lines, int recipe) {
        CauldronNEI.tooltip(this, recipe, stack, lines);
        return super.handleItemTooltip(gui, stack, lines, recipe);
    }
}

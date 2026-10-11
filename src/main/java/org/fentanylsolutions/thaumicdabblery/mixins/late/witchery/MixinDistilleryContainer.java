package org.fentanylsolutions.thaumicdabblery.mixins.late.witchery;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

import org.fentanylsolutions.thaumicdabblery.feature.witcherydistillery.DistilleryRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.emoniph.witchery.blocks.BlockDistillery.ContainerDistillery;

@Mixin(value = ContainerDistillery.class, remap = false)
public abstract class MixinDistilleryContainer {

    @Redirect(
        method = { "transferStackInSlot", "func_82846_b" },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/crafting/FurnaceRecipes;getSmeltingResult(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;",
            remap = true),
        require = 1)
    private ItemStack thaumicdabblery$customInput(FurnaceRecipes furnace, ItemStack input) {
        if (!org.fentanylsolutions.thaumicdabblery.feature.witcherydistillery.WitcheryDistilleryFeature.isEnabled())
            return furnace.getSmeltingResult(input);
        return DistilleryRegistry.ingredient(input) ? input : null;
    }
}

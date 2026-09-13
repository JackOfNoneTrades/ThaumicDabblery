package org.fentanylsolutions.thaumicdabblery.mixins.late.baubles;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleHooks;
import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "baubles.client.gui.GuiPlayerExpanded", remap = false)
public abstract class MixinGuiPlayerExpanded {

    @Redirect(
        method = "handleMouseHover",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;",
            remap = true))
    private Item td$scriptedTypes(ItemStack stack) {
        return BaubleRules.hasRule(stack) ? BaubleHooks.dispatch(stack) : stack.getItem();
    }
}

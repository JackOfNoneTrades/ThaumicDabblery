package org.fentanylsolutions.thaumicdabblery.mixins.late.witchinggadgets;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleSlotsFeature;
import org.fentanylsolutions.thaumicdabblery.feature.baubles.WitchingBaubleSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import baubles.api.expanded.BaubleExpandedSlots;

@Mixin(targets = "witchinggadgets.common.util.handler.EventHandler", remap = false)
public abstract class MixinEventHandlerBaubleSlots {

    @Redirect(
        method = "onPlayerBreaking",
        at = @At(
            value = "INVOKE",
            target = "Lbaubles/api/expanded/BaubleExpandedSlots;getIndexesOfAssignedSlotsOfType(Ljava/lang/String;)[I"))
    private int[] td$noRequiredPhysicalSlot(String type) {
        return BaubleSlotsFeature.isEnabled() ? new int[] { 0 }
            : BaubleExpandedSlots.getIndexesOfAssignedSlotsOfType(type);
    }

    @Redirect(
        method = "onPlayerBreaking",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/inventory/IInventory;getStackInSlot(I)Lnet/minecraft/item/ItemStack;",
            remap = true))
    private ItemStack td$findItem(IInventory inv, int slot) {
        return BaubleSlotsFeature.isEnabled() ? WitchingBaubleSlots.magic(inv, 3) : inv.getStackInSlot(slot);
    }
}

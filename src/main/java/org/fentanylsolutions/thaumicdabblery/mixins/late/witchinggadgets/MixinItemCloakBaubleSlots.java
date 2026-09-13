package org.fentanylsolutions.thaumicdabblery.mixins.late.witchinggadgets;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleSlotsFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import baubles.api.BaublesApi;
import baubles.api.expanded.BaubleExpandedSlots;

@Mixin(targets = "witchinggadgets.common.items.baubles.ItemCloak", remap = false)
public abstract class MixinItemCloakBaubleSlots {

    @Redirect(
        method = "onItemTicked",
        at = @At(
            value = "INVOKE",
            target = "Lbaubles/api/expanded/BaubleExpandedSlots;getIndexesOfAssignedSlotsOfType(Ljava/lang/String;)[I"))
    private int[] td$glideSlot(String type, EntityPlayer player, ItemStack stack) {
        if (BaubleSlotsFeature.isEnabled()) {
            IInventory inv = BaublesApi.getBaubles(player);
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                if (inv.getStackInSlot(i) == stack) return new int[] { i };
            }
        }
        return BaubleExpandedSlots.getIndexesOfAssignedSlotsOfType(type);
    }
}

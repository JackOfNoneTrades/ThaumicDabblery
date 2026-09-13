package org.fentanylsolutions.thaumicdabblery.mixins.late.witchinggadgets;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleSlotsFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import baubles.common.container.InventoryBaubles;

@Mixin(targets = "witchinggadgets.common.items.baubles.ItemKama", remap = false)
public abstract class MixinKamaBaubleSlots {

    @Redirect(
        method = { "onItemRightClick", "func_77659_a" },
        at = @At(value = "INVOKE", target = "Lbaubles/common/container/InventoryBaubles;syncSlotToClients(I)V"))
    private void td$syncActualSlot(InventoryBaubles inv, int original, ItemStack stack, World world,
        EntityPlayer player) {
        if (!BaubleSlotsFeature.isEnabled()) {
            inv.syncSlotToClients(original);
            return;
        }
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack worn = inv.getStackInSlot(i);
            if (worn != null && worn.getItem() == stack.getItem() && worn.getItemDamage() == stack.getItemDamage())
                inv.syncSlotToClients(i);
        }
    }
}

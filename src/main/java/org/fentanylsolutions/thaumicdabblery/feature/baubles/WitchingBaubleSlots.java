package org.fentanylsolutions.thaumicdabblery.feature.baubles;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import baubles.api.BaublesApi;
import baubles.common.container.InventoryBaubles;
import witchinggadgets.common.WGContent;
import witchinggadgets.common.items.baubles.ItemCloak;
import witchinggadgets.common.items.baubles.ItemMagicalBaubles;

public final class WitchingBaubleSlots {

    private WitchingBaubleSlots() {}

    public static ItemStack sniper(IInventory inv) {
        // KryptonCaptain removed the title item and shifted this ring from metadata 6 to 5.
        String[] names = ItemMagicalBaubles.subNames;
        for (int i = 0; i < names.length; i++) if ("ringSniper".equals(names[i])) return magic(inv, i);
        return null;
    }

    public static ItemStack magic(IInventory inv, int metadata) {
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null && stack.getItem() == WGContent.ItemMagicalBaubles && stack.getItemDamage() == metadata)
                return stack;
        }
        return null;
    }

    public static ItemStack storage(EntityPlayer player, int gui) {
        IInventory inv = BaublesApi.getBaubles(player);
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemCloak
                && stack.getItemDamage() == 2
                && (gui == 5) == (stack.getItem() == WGContent.ItemKama)) return stack;
        }
        return null;
    }

    /** Match the actual object, never a same-metadata second pouch or cloak. Its NBT was updated in place. */
    public static void sync(EntityPlayer player, ItemStack stack) {
        InventoryBaubles inv = (InventoryBaubles) BaublesApi.getBaubles(player);
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            if (inv.getStackInSlot(i) == stack) {
                inv.markDirty();
                inv.syncSlotToClients(i);
                return;
            }
        }
        player.inventory.markDirty();
    }
}

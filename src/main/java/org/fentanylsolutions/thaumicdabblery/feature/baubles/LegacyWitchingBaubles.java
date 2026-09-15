package org.fentanylsolutions.thaumicdabblery.feature.baubles;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import baubles.api.BaublesApi;
import travellersgear.api.TravellersGearAPI;
import witchinggadgets.common.WGContent;
import witchinggadgets.common.items.baubles.ItemCloak;

/** Loaded only with the Traveller's Gear-based Witching Gadgets layout. */
public final class LegacyWitchingBaubles {

    // Traveller's Gear reserves 0..25 for its original inventory layout.
    public static final int SLOT_BASE = 0x10000;
    public static final int GUI_MARKER = -0x5444;
    // Remember GUI/effect objects after unequip too; never mistake them for TG's deserialized copies.
    private static final Map<ItemStack, Boolean> BAUBLE_OBJECTS = Collections.synchronizedMap(new WeakHashMap<>());

    private LegacyWitchingBaubles() {}

    public static ItemStack remember(ItemStack stack) {
        if (stack != null) BAUBLE_OBJECTS.put(stack, Boolean.TRUE);
        return stack;
    }

    public static boolean wasBauble(ItemStack stack) {
        return BAUBLE_OBJECTS.containsKey(stack);
    }

    public static int slot(EntityPlayer player, ItemStack stack) {
        IInventory inv = BaublesApi.getBaubles(player);
        for (int i = 0; i < inv.getSizeInventory(); i++) if (inv.getStackInSlot(i) == stack) return i;
        return -1;
    }

    /** Preserve Traveller's Gear's real inventory; substitute only in the WG effect's private read. */
    public static ItemStack[] magicInventory(EntityPlayer player, int tgSlot, int metadata) {
        ItemStack[] original = TravellersGearAPI.getExtendedInventory(player);
        if (!BaubleSlotsFeature.isEnabled()) return original;
        ItemStack moved = WitchingBaubleSlots.magic(BaublesApi.getBaubles(player), metadata);
        if (moved == null) return original;
        ItemStack[] view = original.clone();
        view[tgSlot] = moved;
        return view;
    }

    public static void encodeSlots(EntityPlayer player, Object[][] entries) {
        if (!BaubleSlotsFeature.isEnabled()) return;
        for (Object[] entry : entries) {
            ItemStack stack = (ItemStack) entry[0];
            if (!(stack.getItem() instanceof ItemCloak)) continue;
            int slot = slot(player, stack);
            if (slot >= 4) entry[1] = SLOT_BASE + slot;
        }
    }

    /** Reject stale/out-of-range IDs and never activate arbitrary items via the extended slot protocol. */
    public static ItemStack encodedStack(EntityPlayer player, int encoded) {
        if (!BaubleSlotsFeature.isEnabled()) return null;
        int slot = encoded - SLOT_BASE;
        IInventory inv = BaublesApi.getBaubles(player);
        if (slot < 4 || slot >= inv.getSizeInventory()) return null;
        ItemStack stack = inv.getStackInSlot(slot);
        return stack != null && stack.getItem() instanceof ItemCloak ? stack : null;
    }

    public static ItemStack storage(EntityPlayer player, int gui, int x, int y) {
        if (y == GUI_MARKER) {
            IInventory inv = BaublesApi.getBaubles(player);
            if (x < 0 || x >= inv.getSizeInventory()) return null;
            ItemStack stack = inv.getStackInSlot(x);
            return stack != null && stack.getItem() instanceof ItemCloak
                && stack.getItemDamage() == 2
                && (gui == 5) == (stack.getItem() == WGContent.ItemKama) ? remember(stack) : null;
        }
        // Unmodified Traveller's Gear activations must still open its own cloak, not a moved copy.
        if (gui == 4) {
            ItemStack nativeCloak = TravellersGearAPI.getExtendedInventory(player)[0];
            if (nativeCloak != null && nativeCloak.getItem() instanceof ItemCloak && nativeCloak.getItemDamage() == 2)
                return nativeCloak;
        }
        return remember(WitchingBaubleSlots.storage(player, gui));
    }
}

package org.fentanylsolutions.thaumicdabblery.feature.baubles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.common.container.InventoryBaubles;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class BaubleReconciler {

    private static final String MARKER = "thaumicdabblery.managedBaubleSlots";
    private final Set<EntityPlayer> warned = Collections.newSetFromMap(new WeakHashMap<EntityPlayer, Boolean>());

    @SubscribeEvent
    public void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.worldObj.isRemote) reconcile(event.player);
    }

    public void reconcile(EntityPlayer player) {
        InventoryBaubles inventory = (InventoryBaubles) BaublesApi.getBaubles(player);
        Set<Integer> marked = new HashSet<>();
        for (int slot : player.getEntityData()
            .getIntArray(MARKER)) marked.add(slot);
        List<Integer> next = new ArrayList<>();
        boolean waiting = false;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack == null) continue;
            boolean scripted = BaubleRules.hasRule(stack);
            if (!scripted && !marked.contains(slot) && stack.getItem() instanceof IBauble) continue;
            // Do not call canEquip on already-worn items: unique-item checks commonly reject themselves.
            if (BaubleHooks.fits(stack, BaubleExpandedSlots.getSlotType(slot))) {
                if (scripted) next.add(slot);
                continue;
            }
            if ((stack.getItem() instanceof IBauble && !((IBauble) stack.getItem()).canUnequip(stack, player))
                || capacity(player, stack) < stack.stackSize) {
                next.add(slot);
                waiting = true;
                continue;
            }
            // Flush a pouch/cloak container before moving the item that owns its stored contents.
            if (player.openContainer != player.inventoryContainer) player.closeScreen();
            if (inventory.getStackInSlot(slot) != stack || capacity(player, stack) < stack.stackSize) {
                next.add(slot);
                continue;
            }
            ItemStack removed = inventory.decrStackSize(slot, stack.stackSize);
            player.inventory.addItemStackToInventory(removed);
            // An addon callback may have filled the inventory after the capacity check. Never lose its remainder.
            if (removed.stackSize > 0) {
                if (inventory.getStackInSlot(slot) == null) inventory.setInventorySlotContents(slot, removed);
                else player.entityDropItem(removed, 0.0F);
                next.add(slot);
                waiting = true;
            }
            player.inventory.markDirty();
        }
        int[] slots = new int[next.size()];
        for (int i = 0; i < slots.length; i++) slots[i] = next.get(i);
        if (slots.length > 0) player.getEntityData()
            .setIntArray(MARKER, slots);
        else player.getEntityData()
            .removeTag(MARKER);
        if (waiting && player instanceof EntityPlayerMP
            && ((EntityPlayerMP) player).playerNetServerHandler != null
            && warned.add(player))
            player.addChatMessage(
                new ChatComponentText(
                    "A bauble no longer fits its slot. Make inventory space or remove its unequip restriction to recover it."));
        if (!waiting) warned.remove(player);
    }

    private static int capacity(EntityPlayer player, ItemStack stack) {
        int capacity = 0;
        int limit = Math.min(stack.getMaxStackSize(), player.inventory.getInventoryStackLimit());
        for (ItemStack current : player.inventory.mainInventory) {
            if (current == null) capacity += limit;
            else if (stack.isStackable() && current.isItemEqual(stack)
                && ItemStack.areItemStackTagsEqual(current, stack)) capacity += Math.max(0, limit - current.stackSize);
        }
        return capacity;
    }
}

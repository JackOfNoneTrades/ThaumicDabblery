package org.fentanylsolutions.thaumicdabblery.feature.baubles;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import baubles.api.expanded.BaubleExpandedSlots;
import baubles.common.container.ContainerPlayerExpanded;
import baubles.common.container.SlotBauble;

/** Expanded's inherited merge path does not honor per-slot limits for stackable, newly wearable items. */
public final class BaubleTransfer {

    private BaubleTransfer() {}

    public static ItemStack transfer(ContainerPlayerExpanded container, Slot source, EntityPlayer player) {
        if (!source.canTakeStack(player)) return null;
        ItemStack stack = source.getStack();
        ItemStack before = stack.copy();
        if (source.inventory == container.baubles) {
            ItemStack moving = stack.copy();
            player.inventory.addItemStackToInventory(moving);
            int count = before.stackSize - moving.stackSize;
            if (count == 0) return null;
            source.decrStackSize(count);
            source.onSlotChanged();
        } else if (source.inventory == player.inventory) {
            String[] types = BaubleRules.types(stack);
            // Prefer explicit types over configured universal-slot fallbacks; merge before empty slots.
            for (int priority = 2; priority > 0; priority--) {
                for (int occupied = 1; occupied >= 0; occupied--) {
                    for (int i = 0; i < container.getBaubleSlotCount() && stack.stackSize > 0; i++) {
                        SlotBauble target = container.getBaubleSlot(i);
                        ItemStack current = target.getStack();
                        if ((current != null) != (occupied == 1) || !target.isItemValid(stack)
                            || priority(types, target.getSlotType()) != priority) continue;
                        int limit = Math.min(target.getSlotStackLimit(), stack.getMaxStackSize());
                        if (current != null) {
                            if (!stack.isStackable() || !stack.isItemEqual(current)
                                || !ItemStack.areItemStackTagsEqual(stack, current)) continue;
                            int count = Math.min(stack.stackSize, Math.max(0, limit - current.stackSize));
                            if (count == 0) continue;
                            current.stackSize += count;
                            stack.stackSize -= count;
                            target.onSlotChanged();
                        } else {
                            int count = Math.min(stack.stackSize, limit);
                            target.putStack(stack.splitStack(count));
                        }
                        container.baubles.syncSlotToClients(i);
                    }
                }
            }
            if (stack.stackSize == before.stackSize) return null;
            if (stack.stackSize == 0) source.putStack(null);
            else source.onSlotChanged();
        } else return null;
        player.inventory.markDirty();
        return before;
    }

    private static int priority(String[] types, String target) {
        for (String type : types) {
            if (type.equals(target) || type.equals(BaubleExpandedSlots.universalType)) return 2;
        }
        return 1;
    }
}

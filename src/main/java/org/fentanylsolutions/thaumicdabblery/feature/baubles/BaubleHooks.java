package org.fentanylsolutions.thaumicdabblery.feature.baubles;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.IBaubleExpanded;
import baubles.common.BaublesConfig;

/** The adapter is only used for Baubles' interface dispatch, never stored in an ItemStack or registered as an item. */
public final class BaubleHooks {

    private static final Item ADAPTER = new DispatchItem();

    private BaubleHooks() {}

    public static Item dispatch(ItemStack stack) {
        return BaubleRules.hasRule(stack) || !(stack.getItem() instanceof IBauble) ? ADAPTER : stack.getItem();
    }

    public static boolean fits(ItemStack stack, String slotType) {
        for (String type : BaubleRules.types(stack)) {
            if (BaublesConfig.canTypeFitSlot(type, slotType)) return true;
        }
        return false;
    }

    public static boolean canEquip(ItemStack stack, EntityPlayer player, String slotType) {
        return stack != null && fits(stack, slotType)
            && (!(stack.getItem() instanceof IBauble) || ((IBauble) stack.getItem()).canEquip(stack, player));
    }

    /** Only intercept the shared helper for scripted targets; ordinary dirt right-click still places dirt. */
    public static ItemStack rightClick(ItemStack stack, World world, EntityPlayer player) {
        IInventory inventory = BaublesApi.getBaubles(player);
        if (inventory == null || stack.stackSize < 1) return stack;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            if (inventory.getStackInSlot(slot) != null || !inventory.isItemValidForSlot(slot, stack)) continue;
            if (!world.isRemote) {
                ItemStack equipped = stack.copy();
                equipped.stackSize = Math.min(
                    stack.stackSize,
                    Math.min(
                        stack.getMaxStackSize(),
                        BaublesConfig.getStackLimitForSlotType(BaubleExpandedSlots.getSlotType(slot))));
                inventory.setInventorySlotContents(slot, equipped);
                if (!player.capabilities.isCreativeMode) {
                    stack.stackSize -= equipped.stackSize;
                    player.inventory
                        .setInventorySlotContents(player.inventory.currentItem, stack.stackSize == 0 ? null : stack);
                }
            }
            break;
        }
        return stack;
    }

    private static final class DispatchItem extends Item implements IBaubleExpanded {

        @Override
        public String[] getBaubleTypes(ItemStack stack) {
            return BaubleRules.types(stack);
        }

        @Override
        public BaubleType getBaubleType(ItemStack stack) {
            return null;
        }

        @Override
        public boolean canEquip(ItemStack stack, EntityLivingBase player) {
            return !(stack.getItem() instanceof IBauble) || ((IBauble) stack.getItem()).canEquip(stack, player);
        }

        @Override
        public boolean canUnequip(ItemStack stack, EntityLivingBase player) {
            return !(stack.getItem() instanceof IBauble) || ((IBauble) stack.getItem()).canUnequip(stack, player);
        }

        @Override
        public void onEquipped(ItemStack stack, EntityLivingBase player) {
            if (stack.getItem() instanceof IBauble) ((IBauble) stack.getItem()).onEquipped(stack, player);
        }

        @Override
        public void onUnequipped(ItemStack stack, EntityLivingBase player) {
            if (stack.getItem() instanceof IBauble) ((IBauble) stack.getItem()).onUnequipped(stack, player);
        }

        @Override
        public void onWornTick(ItemStack stack, EntityLivingBase player) {
            if (stack.getItem() instanceof IBauble) ((IBauble) stack.getItem()).onWornTick(stack, player);
        }
    }
}

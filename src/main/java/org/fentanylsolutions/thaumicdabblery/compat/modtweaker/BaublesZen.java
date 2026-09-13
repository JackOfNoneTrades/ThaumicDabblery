package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleRules;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IItemStack;
import modtweaker2.helpers.InputHelper;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.baubles.Baubles")
public final class BaublesZen {

    private BaublesZen() {}

    @ZenMethod
    public static void setSlots(IItemStack item, String[] slots) {
        change(item, 0, slots);
    }

    @ZenMethod
    public static void addSlot(IItemStack item, String slot) {
        change(item, 1, new String[] { slot });
    }

    @ZenMethod
    public static void removeSlot(IItemStack item, String slot) {
        change(item, 2, new String[] { slot });
    }

    @ZenMethod
    public static void remove(IItemStack item) {
        change(item, 0, new String[0]);
    }

    private static void change(IItemStack item, int operation, String[] slots) {
        ItemStack stack = InputHelper.toStack(item);
        if (stack == null) throw new IllegalArgumentException("Bauble item cannot be null");
        MineTweakerAPI.apply(new Action(stack.copy(), operation, slots == null ? null : slots.clone()));
    }

    private static final class Action implements IUndoableAction {

        final ItemStack stack;
        final int operation;
        final String[] slots;
        BaubleRules.Change change;

        Action(ItemStack stack, int operation, String[] slots) {
            this.stack = stack;
            this.operation = operation;
            this.slots = slots;
        }

        @Override
        public void apply() {
            change = BaubleRules.edit(stack, operation, slots);
        }

        @Override
        public boolean canUndo() {
            return change != null;
        }

        @Override
        public void undo() {
            change.undo();
        }

        @Override
        public String describe() {
            return "Changing allowed bauble slots for " + stack.getDisplayName();
        }

        @Override
        public String describeUndo() {
            return "Restoring allowed bauble slots for " + stack.getDisplayName();
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }
}

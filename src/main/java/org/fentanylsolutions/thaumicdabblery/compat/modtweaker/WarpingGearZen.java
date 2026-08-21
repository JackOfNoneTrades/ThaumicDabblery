package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.itemstats.WarpingGearRegistry;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IItemStack;
import modtweaker2.helpers.InputHelper;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.thaumcraft.WarpingGear")
public final class WarpingGearZen {

    private WarpingGearZen() {}

    public static void register() {
        MineTweakerAPI.registerClass(WarpingGearZen.class);
    }

    @ZenMethod
    public static void set(IItemStack item, int amount) {
        MineTweakerAPI.apply(new SetAction(requireItem(item), amount));
    }

    private static ItemStack requireItem(IItemStack item) {
        ItemStack internal = InputHelper.toStack(item);
        if (internal == null || internal.getItem() == null) {
            throw new IllegalArgumentException("Warping gear item cannot be null");
        }
        return internal.copy();
    }

    private static final class SetAction implements IUndoableAction {

        private final ItemStack item;
        private final int amount;
        private WarpingGearRegistry.Change change;

        private SetAction(ItemStack item, int amount) {
            this.item = item;
            this.amount = amount;
        }

        @Override
        public void apply() {
            change = WarpingGearRegistry.set(item, amount);
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
            return "Setting " + item.getDisplayName() + " equipped warp to " + amount;
        }

        @Override
        public String describeUndo() {
            return "Restoring " + item.getDisplayName() + " equipped warp";
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }
}

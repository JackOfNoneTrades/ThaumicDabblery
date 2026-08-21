package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.itemstats.RunicShieldingRegistry;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IItemStack;
import modtweaker2.helpers.InputHelper;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.thaumcraft.RunicShielding")
public final class RunicShieldingZen {

    private RunicShieldingZen() {}

    public static void register() {
        MineTweakerAPI.registerClass(RunicShieldingZen.class);
    }

    @ZenMethod
    public static void setBase(IItemStack item, int amount) {
        MineTweakerAPI.apply(new SetBaseAction(requireItem(item), amount));
    }

    @ZenMethod
    public static void enableAugmentation(IItemStack item) {
        MineTweakerAPI.apply(new EnableAugmentationAction(requireItem(item)));
    }

    private static ItemStack requireItem(IItemStack item) {
        ItemStack internal = InputHelper.toStack(item);
        if (internal == null || internal.getItem() == null) {
            throw new IllegalArgumentException("Runic shielding item cannot be null");
        }
        return internal.copy();
    }

    private abstract static class RunicAction implements IUndoableAction {

        protected final ItemStack item;
        protected RunicShieldingRegistry.Change change;

        private RunicAction(ItemStack item) {
            this.item = item;
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
        public Object getOverrideKey() {
            return null;
        }
    }

    private static final class SetBaseAction extends RunicAction {

        private final int amount;

        private SetBaseAction(ItemStack item, int amount) {
            super(item);
            this.amount = amount;
        }

        @Override
        public void apply() {
            change = RunicShieldingRegistry.setBase(item, amount);
        }

        @Override
        public String describe() {
            return "Setting " + item.getDisplayName() + " base runic shielding to " + amount;
        }

        @Override
        public String describeUndo() {
            return "Restoring " + item.getDisplayName() + " base runic shielding";
        }
    }

    private static final class EnableAugmentationAction extends RunicAction {

        private EnableAugmentationAction(ItemStack item) {
            super(item);
        }

        @Override
        public void apply() {
            change = RunicShieldingRegistry.enableAugmentation(item);
        }

        @Override
        public String describe() {
            return "Allowing runic shielding augmentation on " + item.getDisplayName();
        }

        @Override
        public String describeUndo() {
            return "Restoring runic shielding augmentation eligibility for " + item.getDisplayName();
        }
    }
}

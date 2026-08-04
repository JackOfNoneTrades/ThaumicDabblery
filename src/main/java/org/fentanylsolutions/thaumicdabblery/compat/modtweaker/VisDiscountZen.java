package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.visdiscount.VisDiscountRegistry;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IItemStack;
import modtweaker2.helpers.InputHelper;
import modtweaker2.mods.thaumcraft.aspect.IAspectStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import thaumcraft.api.aspects.Aspect;

@ZenClass("mods.thaumcraft.VisDiscount")
public final class VisDiscountZen {

    private VisDiscountZen() {}

    public static void register() {
        MineTweakerAPI.registerClass(VisDiscountZen.class);
    }

    @ZenMethod
    public static void set(IItemStack item, int discount) {
        MineTweakerAPI.apply(new SetAction(requireItem(item), null, discount));
    }

    @ZenMethod
    public static void set(IItemStack item, IAspectStack aspect, int discount) {
        if (aspect == null) {
            throw new IllegalArgumentException("Vis discount aspect cannot be null");
        }

        Aspect internalAspect = Aspect.getAspect(aspect.getName());
        if (internalAspect == null) {
            throw new IllegalArgumentException("Unknown Thaumcraft aspect: " + aspect.getName());
        }
        MineTweakerAPI.apply(new SetAction(requireItem(item), internalAspect, discount));
    }

    private static ItemStack requireItem(IItemStack item) {
        ItemStack internal = InputHelper.toStack(item);
        if (internal == null || internal.getItem() == null) {
            throw new IllegalArgumentException("Vis discount item cannot be null");
        }
        return internal.copy();
    }

    private static final class SetAction implements IUndoableAction {

        private final ItemStack item;
        private final Aspect aspect;
        private final int discount;
        private VisDiscountRegistry.Change change;

        private SetAction(ItemStack item, Aspect aspect, int discount) {
            this.item = item;
            this.aspect = aspect;
            this.discount = discount;
        }

        @Override
        public void apply() {
            change = VisDiscountRegistry.set(item, aspect, discount);
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
            return "Setting " + describeTarget() + " vis discount to " + discount + "%";
        }

        @Override
        public String describeUndo() {
            return "Restoring " + describeTarget() + " vis discount";
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }

        private String describeTarget() {
            String target = item.getDisplayName();
            return aspect == null ? target : target + "'s " + aspect.getTag() + " aspect";
        }
    }
}

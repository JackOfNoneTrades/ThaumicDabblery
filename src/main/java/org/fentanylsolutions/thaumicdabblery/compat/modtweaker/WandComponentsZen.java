package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.fentanylsolutions.thaumicdabblery.feature.wandcomponents.WandComponentVisDiscountRegistry;

import cpw.mods.fml.common.registry.GameRegistry;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IItemStack;
import modtweaker2.helpers.InputHelper;
import modtweaker2.mods.thaumcraft.aspect.IAspectStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.WandCap;
import thaumcraft.common.items.wands.ItemWandCasting;

@ZenClass("mods.thaumcraft.WandComponents")
public final class WandComponentsZen {

    private WandComponentsZen() {}

    public static void register() {
        MineTweakerAPI.registerClass(WandComponentsZen.class);
    }

    @ZenMethod
    public static void setCapVisDiscount(String capId, IAspectStack aspect, int discount) {
        String validCapId = requireCap(capId);
        MineTweakerAPI.apply(new SetCapAction(validCapId, requirePrimalAspect(aspect), discount));
    }

    @ZenMethod
    public static void setCastingVisDiscount(IItemStack castingItem, IAspectStack aspect, int discount) {
        MineTweakerAPI
            .apply(new SetCastingItemAction(requireCastingItem(castingItem), requirePrimalAspect(aspect), discount));
    }

    @ZenMethod
    public static void setCastingVisDiscount(String registryName, int metadata, IAspectStack aspect, int discount) {
        MineTweakerAPI.apply(
            new SetCastingItemAction(
                requireCastingItem(registryName, metadata),
                requirePrimalAspect(aspect),
                discount));
    }

    @ZenMethod
    public static void setCastingVisDiscount(String registryName, IAspectStack aspect, int discount) {
        setCastingVisDiscount(registryName, OreDictionary.WILDCARD_VALUE, aspect, discount);
    }

    private static String requireCap(String capId) {
        if (capId == null || capId.isEmpty() || !WandCap.caps.containsKey(capId)) {
            throw new IllegalArgumentException("Unknown Thaumcraft wand cap ID: " + capId);
        }
        return capId;
    }

    private static ItemStack requireCastingItem(IItemStack castingItem) {
        return requireCastingItem(InputHelper.toStack(castingItem));
    }

    private static ItemStack requireCastingItem(ItemStack castingItem) {
        if (castingItem == null || !(castingItem.getItem() instanceof ItemWandCasting)) {
            throw new IllegalArgumentException(
                "Casting vis discount target must be a wand, staff, or fixed casting item");
        }
        return castingItem.copy();
    }

    private static ItemStack requireCastingItem(String registryName, int metadata) {
        if (registryName == null) {
            throw new IllegalArgumentException("Casting item registry name cannot be null");
        }

        int separator = registryName.indexOf(':');
        if (separator <= 0 || separator == registryName.length() - 1) {
            throw new IllegalArgumentException(
                "Casting item registry name must use the form modid:itemName: " + registryName);
        }
        if (metadata < 0 || metadata > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Casting item metadata must be between 0 and 32767: " + metadata);
        }

        Item item = GameRegistry.findItem(registryName.substring(0, separator), registryName.substring(separator + 1));
        return requireCastingItem(item == null ? null : new ItemStack(item, 1, metadata));
    }

    private static Aspect requirePrimalAspect(IAspectStack aspect) {
        if (aspect == null) {
            throw new IllegalArgumentException("Wand component vis discount aspect cannot be null");
        }

        Aspect internalAspect = Aspect.getAspect(aspect.getName());
        if (internalAspect == null || !internalAspect.isPrimal()) {
            throw new IllegalArgumentException(
                "Wand component vis discounts require a primal aspect: " + aspect.getName());
        }
        return internalAspect;
    }

    private abstract static class SetAction implements IUndoableAction {

        protected final Aspect aspect;
        protected final int discount;
        protected WandComponentVisDiscountRegistry.Change change;

        private SetAction(Aspect aspect, int discount) {
            this.aspect = aspect;
            this.discount = discount;
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

    private static final class SetCapAction extends SetAction {

        private final String capId;

        private SetCapAction(String capId, Aspect aspect, int discount) {
            super(aspect, discount);
            this.capId = capId;
        }

        @Override
        public void apply() {
            change = WandComponentVisDiscountRegistry.setCap(capId, aspect, discount);
        }

        @Override
        public String describe() {
            return "Setting " + capId + " wand cap's " + aspect.getTag() + " vis discount to " + discount + "%";
        }

        @Override
        public String describeUndo() {
            return "Restoring " + capId + " wand cap's " + aspect.getTag() + " vis discount";
        }
    }

    private static final class SetCastingItemAction extends SetAction {

        private final ItemStack castingItem;

        private SetCastingItemAction(ItemStack castingItem, Aspect aspect, int discount) {
            super(aspect, discount);
            this.castingItem = castingItem;
        }

        @Override
        public void apply() {
            change = WandComponentVisDiscountRegistry.setCastingItem(castingItem, aspect, discount);
        }

        @Override
        public String describe() {
            return "Setting " + castingItem
                .getDisplayName() + "'s " + aspect.getTag() + " vis discount to " + discount + "%";
        }

        @Override
        public String describeUndo() {
            return "Restoring " + castingItem.getDisplayName() + "'s " + aspect.getTag() + " vis discount";
        }
    }
}

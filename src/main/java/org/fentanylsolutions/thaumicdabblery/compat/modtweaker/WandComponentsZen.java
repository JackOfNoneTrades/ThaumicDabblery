package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.function.Supplier;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.fentanylsolutions.thaumicdabblery.feature.wandcomponents.WandComponentStatsRegistry;
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
import thaumcraft.api.wands.WandRod;
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

    @ZenMethod
    public static void setCapCraftingMultiplier(String capId, int multiplier) {
        WandCap cap = WandCap.caps.get(requireCap(capId));
        apply(
            "Setting " + capId + " cap crafting multiplier to " + multiplier,
            () -> WandComponentStatsRegistry.setCapCost(cap, multiplier));
    }

    @ZenMethod
    public static void setCoreCraftingCost(String coreId, int cost) {
        WandRod rod = requireCore(coreId);
        apply(
            "Setting " + coreId + " core crafting cost to " + cost,
            () -> WandComponentStatsRegistry.setCoreCost(rod, cost));
    }

    @ZenMethod
    public static void setCoreCapacity(String coreId, int capacity) {
        WandRod rod = requireCore(coreId);
        apply(
            "Setting " + coreId + " core capacity to " + capacity,
            () -> WandComponentStatsRegistry.setCoreCapacity(rod, capacity));
    }

    @ZenMethod
    public static void setCorePotency(String coreId, int levels) {
        String target = requireCore(coreId).getTag();
        apply(
            "Setting " + coreId + " core innate Potency to " + levels,
            () -> WandComponentStatsRegistry.setPotency(target, levels));
    }

    @ZenMethod
    public static void setCoreVisRegeneration(String coreId, IAspectStack aspect, int intervalTicks, double amount,
        int ceilingPercent) {
        String target = requireCore(coreId).getTag();
        Aspect primal = requirePrimalAspect(aspect);
        apply(
            "Setting " + coreId + " core " + primal.getTag() + " Vis regeneration",
            () -> WandComponentStatsRegistry.setRegeneration(target, primal, intervalTicks, amount, ceilingPercent));
    }

    @ZenMethod
    public static void disableCoreVisRegeneration(String coreId) {
        String target = requireCore(coreId).getTag();
        apply(
            "Disabling " + coreId + " core Vis regeneration",
            () -> WandComponentStatsRegistry.disableRegeneration(target));
    }

    @ZenMethod
    public static void resetCoreVisRegeneration(String coreId) {
        String target = requireCore(coreId).getTag();
        apply(
            "Restoring " + coreId + " core native Vis regeneration",
            () -> WandComponentStatsRegistry.resetRegeneration(target));
    }

    @ZenMethod
    public static void setCastingCapacity(IItemStack castingItem, int capacity) {
        setCastingCapacity(requireCastingItem(castingItem), capacity);
    }

    @ZenMethod
    public static void setCastingCapacity(String registryName, int metadata, int capacity) {
        setCastingCapacity(requireCastingItem(registryName, metadata), capacity);
    }

    @ZenMethod
    public static void setCastingCapacity(String registryName, int capacity) {
        setCastingCapacity(registryName, OreDictionary.WILDCARD_VALUE, capacity);
    }

    private static void setCastingCapacity(ItemStack stack, int capacity) {
        apply(
            "setCastingCapacity for " + stack.getDisplayName(),
            () -> WandComponentStatsRegistry.setCastingCapacity(stack, capacity));
    }

    @ZenMethod
    public static void setCastingPotency(IItemStack castingItem, int levels) {
        setCastingPotency(requireCastingItem(castingItem), levels);
    }

    @ZenMethod
    public static void setCastingPotency(String registryName, int metadata, int levels) {
        setCastingPotency(requireCastingItem(registryName, metadata), levels);
    }

    @ZenMethod
    public static void setCastingPotency(String registryName, int levels) {
        setCastingPotency(registryName, OreDictionary.WILDCARD_VALUE, levels);
    }

    private static void setCastingPotency(ItemStack stack, int levels) {
        apply(
            "setCastingPotency for " + stack.getDisplayName(),
            () -> WandComponentStatsRegistry.setPotency(WandComponentStatsRegistry.castingKey(stack), levels));
    }

    @ZenMethod
    public static void setCastingVisRegeneration(IItemStack castingItem, IAspectStack aspect, int intervalTicks,
        double amount, int ceilingPercent) {
        setCastingVisRegeneration(requireCastingItem(castingItem), aspect, intervalTicks, amount, ceilingPercent);
    }

    @ZenMethod
    public static void setCastingVisRegeneration(String registryName, int metadata, IAspectStack aspect,
        int intervalTicks, double amount, int ceilingPercent) {
        setCastingVisRegeneration(
            requireCastingItem(registryName, metadata),
            aspect,
            intervalTicks,
            amount,
            ceilingPercent);
    }

    @ZenMethod
    public static void setCastingVisRegeneration(String registryName, IAspectStack aspect, int intervalTicks,
        double amount, int ceilingPercent) {
        setCastingVisRegeneration(
            registryName,
            OreDictionary.WILDCARD_VALUE,
            aspect,
            intervalTicks,
            amount,
            ceilingPercent);
    }

    private static void setCastingVisRegeneration(ItemStack stack, IAspectStack aspect, int intervalTicks,
        double amount, int ceilingPercent) {
        Aspect primal = requirePrimalAspect(aspect);
        apply(
            "setCastingVisRegeneration for " + stack.getDisplayName(),
            () -> WandComponentStatsRegistry.setRegeneration(
                WandComponentStatsRegistry.castingKey(stack),
                primal,
                intervalTicks,
                amount,
                ceilingPercent));
    }

    @ZenMethod
    public static void disableCastingVisRegeneration(IItemStack castingItem) {
        disableCastingVisRegeneration(requireCastingItem(castingItem));
    }

    @ZenMethod
    public static void disableCastingVisRegeneration(String registryName, int metadata) {
        disableCastingVisRegeneration(requireCastingItem(registryName, metadata));
    }

    @ZenMethod
    public static void disableCastingVisRegeneration(String registryName) {
        disableCastingVisRegeneration(registryName, OreDictionary.WILDCARD_VALUE);
    }

    private static void disableCastingVisRegeneration(ItemStack stack) {
        apply(
            "disableCastingVisRegeneration for " + stack.getDisplayName(),
            () -> WandComponentStatsRegistry.disableRegeneration(WandComponentStatsRegistry.castingKey(stack)));
    }

    @ZenMethod
    public static void resetCastingVisRegeneration(IItemStack castingItem) {
        resetCastingVisRegeneration(requireCastingItem(castingItem));
    }

    @ZenMethod
    public static void resetCastingVisRegeneration(String registryName, int metadata) {
        resetCastingVisRegeneration(requireCastingItem(registryName, metadata));
    }

    @ZenMethod
    public static void resetCastingVisRegeneration(String registryName) {
        resetCastingVisRegeneration(registryName, OreDictionary.WILDCARD_VALUE);
    }

    private static void resetCastingVisRegeneration(ItemStack stack) {
        apply(
            "resetCastingVisRegeneration for " + stack.getDisplayName(),
            () -> WandComponentStatsRegistry.resetRegeneration(WandComponentStatsRegistry.castingKey(stack)));
    }

    private static WandRod requireCore(String coreId) {
        WandRod rod = WandRod.rods.get(coreId);
        if (rod == null) throw new IllegalArgumentException("Unknown Thaumcraft wand core ID: " + coreId);
        return rod;
    }

    private static void apply(String description, Supplier<Runnable> mutation) {
        MineTweakerAPI.apply(new IUndoableAction() {

            private Runnable undo;

            @Override
            public void apply() {
                undo = mutation.get();
            }

            @Override
            public boolean canUndo() {
                return undo != null;
            }

            @Override
            public void undo() {
                undo.run();
            }

            @Override
            public String describe() {
                return description;
            }

            @Override
            public String describeUndo() {
                return "Undoing: " + description;
            }

            @Override
            public Object getOverrideKey() {
                return null;
            }
        });
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
        if (castingItem.getItemDamage() < 0 || castingItem.getItemDamage() > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Casting item metadata must be between 0 and 32767");
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

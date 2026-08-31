package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.focusupgrades.FocusUpgradeRegistry;

import cpw.mods.fml.common.registry.GameRegistry;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IItemStack;
import modtweaker2.helpers.InputHelper;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;

@ZenClass("mods.thaumcraft.FocusUpgrades")
public final class FocusUpgradesZen {

    private FocusUpgradesZen() {}

    public static void register() {
        MineTweakerAPI.registerClass(FocusUpgradesZen.class);
    }

    @ZenMethod
    public static void add(IItemStack focus, int tier, int[] upgradeIds) {
        MineTweakerAPI.apply(new AddAction(requireFocus(focus), requireTier(tier), requireUpgrades(upgradeIds)));
    }

    @ZenMethod
    public static void add(String focus, int tier, int[] upgradeIds) {
        MineTweakerAPI.apply(new AddAction(requireFocus(focus), requireTier(tier), requireUpgrades(upgradeIds)));
    }

    @ZenMethod
    public static void remove(IItemStack focus) {
        MineTweakerAPI.apply(new RemoveAllAction(requireFocus(focus)));
    }

    @ZenMethod
    public static void remove(String focus) {
        MineTweakerAPI.apply(new RemoveAllAction(requireFocus(focus)));
    }

    @ZenMethod
    public static void remove(IItemStack focus, int tier) {
        MineTweakerAPI.apply(new RemoveTierAction(requireFocus(focus), requireTier(tier)));
    }

    @ZenMethod
    public static void remove(String focus, int tier) {
        MineTweakerAPI.apply(new RemoveTierAction(requireFocus(focus), requireTier(tier)));
    }

    private static ItemStack requireFocus(IItemStack focus) {
        ItemStack internal = InputHelper.toStack(focus);
        if (internal == null || !(internal.getItem() instanceof ItemFocusBasic)) {
            throw new IllegalArgumentException(
                "Focus upgrade target must be a Focal Manipulator-compatible wand focus");
        }
        return internal.copy();
    }

    private static ItemStack requireFocus(String registryName) {
        if (registryName == null) {
            throw new IllegalArgumentException("Focus registry name cannot be null");
        }

        int separator = registryName.indexOf(':');
        if (separator <= 0 || separator == registryName.length() - 1) {
            throw new IllegalArgumentException("Focus registry name must use the form modid:itemName: " + registryName);
        }

        Item item = GameRegistry.findItem(registryName.substring(0, separator), registryName.substring(separator + 1));
        return requireFocus(item == null ? null : new ItemStack(item));
    }

    private static ItemStack requireFocus(ItemStack focus) {
        if (focus == null || !(focus.getItem() instanceof ItemFocusBasic)) {
            throw new IllegalArgumentException(
                "Focus upgrade target must be a registered Focal Manipulator-compatible wand focus");
        }
        return focus.copy();
    }

    private static int requireTier(int tier) {
        if (tier < FocusUpgradeRegistry.MIN_TIER || tier > FocusUpgradeRegistry.MAX_TIER) {
            throw new IllegalArgumentException("Focus upgrade tier must be between 1 and 5: " + tier);
        }
        return tier;
    }

    private static short[] requireUpgrades(int[] upgradeIds) {
        if (upgradeIds == null || upgradeIds.length == 0) {
            throw new IllegalArgumentException("At least one focus upgrade ID is required");
        }

        Set<Short> unique = new LinkedHashSet<>();
        for (int upgradeId : upgradeIds) {
            if (upgradeId < 0 || upgradeId > Short.MAX_VALUE
                || upgradeId >= FocusUpgradeType.types.length
                || FocusUpgradeType.types[upgradeId] == null) {
                throw new IllegalArgumentException("Unknown Thaumcraft focus upgrade ID: " + upgradeId);
            }
            unique.add((short) upgradeId);
        }

        short[] result = new short[unique.size()];
        int index = 0;
        for (short upgradeId : unique) {
            result[index++] = upgradeId;
        }
        return result;
    }

    private abstract static class FocusUpgradeAction implements IUndoableAction {

        protected final ItemStack focus;
        protected FocusUpgradeRegistry.Change change;

        private FocusUpgradeAction(ItemStack focus) {
            this.focus = focus;
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

    private static final class AddAction extends FocusUpgradeAction {

        private final int tier;
        private final short[] upgradeIds;

        private AddAction(ItemStack focus, int tier, short[] upgradeIds) {
            super(focus);
            this.tier = tier;
            this.upgradeIds = upgradeIds;
        }

        @Override
        public void apply() {
            change = FocusUpgradeRegistry.add(focus, tier, upgradeIds);
        }

        @Override
        public String describe() {
            return "Adding focus upgrades " + describeUpgrades(
                upgradeIds) + " to " + focus.getDisplayName() + " tier " + tier;
        }

        @Override
        public String describeUndo() {
            return "Removing added focus upgrades from " + focus.getDisplayName() + " tier " + tier;
        }
    }

    private static final class RemoveTierAction extends FocusUpgradeAction {

        private final int tier;

        private RemoveTierAction(ItemStack focus, int tier) {
            super(focus);
            this.tier = tier;
        }

        @Override
        public void apply() {
            change = FocusUpgradeRegistry.remove(focus, tier);
        }

        @Override
        public String describe() {
            return "Removing all focus upgrades from " + focus.getDisplayName() + " tier " + tier;
        }

        @Override
        public String describeUndo() {
            return "Restoring focus upgrades for " + focus.getDisplayName() + " tier " + tier;
        }
    }

    private static final class RemoveAllAction extends FocusUpgradeAction {

        private RemoveAllAction(ItemStack focus) {
            super(focus);
        }

        @Override
        public void apply() {
            change = FocusUpgradeRegistry.removeAll(focus);
        }

        @Override
        public String describe() {
            return "Removing all focus upgrades from " + focus.getDisplayName();
        }

        @Override
        public String describeUndo() {
            return "Restoring all focus upgrades for " + focus.getDisplayName();
        }
    }

    private static String describeUpgrades(short[] upgradeIds) {
        StringBuilder description = new StringBuilder("[");
        for (int index = 0; index < upgradeIds.length; index++) {
            if (index > 0) {
                description.append(", ");
            }
            description.append(upgradeIds[index]);
        }
        return description.append(']')
            .toString();
    }
}

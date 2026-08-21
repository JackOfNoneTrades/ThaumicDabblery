package org.fentanylsolutions.thaumicdabblery.feature.itemstats;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.item.ItemStack;

import thaumcraft.common.Thaumcraft;

public final class RunicShieldingRegistry {

    private static final Map<ItemStatKey, Rule> RULES = new HashMap<>();

    private RunicShieldingRegistry() {}

    public static synchronized boolean isAugmentable(ItemStack stack) {
        return getRule(stack) != null;
    }

    public static synchronized Integer getBase(ItemStack stack) {
        Rule rule = getRule(stack);
        return rule == null ? null : rule.base;
    }

    public static synchronized Change enableAugmentation(ItemStack stack) {
        requireItem(stack);
        ItemStatKey key = ItemStatKey.fromRule(stack);
        Rule previous = RULES.get(key);
        RULES.put(key, new Rule(previous == null ? null : previous.base));
        markDirty();
        return new Change(key, previous);
    }

    public static synchronized Change setBase(ItemStack stack, int amount) {
        return set(stack, amount);
    }

    private static Change set(ItemStack stack, Integer base) {
        requireItem(stack);
        ItemStatKey key = ItemStatKey.fromRule(stack);
        Rule previous = RULES.put(key, new Rule(base));
        markDirty();
        return new Change(key, previous);
    }

    private static Rule getRule(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }

        Rule rule = RULES.get(ItemStatKey.exact(stack));
        if (rule != null || !ItemStatKey.canHaveExactAndWildcard(stack)) {
            return rule;
        }
        return RULES.get(ItemStatKey.wildcard(stack));
    }

    private static void requireItem(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            throw new IllegalArgumentException("Runic shielding item cannot be null");
        }
    }

    private static synchronized void restore(Change change) {
        if (change.previous == null) {
            RULES.remove(change.key);
        } else {
            RULES.put(change.key, change.previous);
        }
        markDirty();
    }

    private static void markDirty() {
        if (Thaumcraft.instance != null && Thaumcraft.instance.runicEventHandler != null) {
            Thaumcraft.instance.runicEventHandler.isDirty = true;
        }
    }

    private static final class Rule {

        private final Integer base;

        private Rule(Integer base) {
            this.base = base;
        }
    }

    public static final class Change {

        private final ItemStatKey key;
        private final Rule previous;

        private Change(ItemStatKey key, Rule previous) {
            this.key = key;
            this.previous = previous;
        }

        public void undo() {
            RunicShieldingRegistry.restore(this);
        }
    }
}

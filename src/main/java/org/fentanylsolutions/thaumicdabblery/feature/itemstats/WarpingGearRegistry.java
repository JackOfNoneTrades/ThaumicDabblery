package org.fentanylsolutions.thaumicdabblery.feature.itemstats;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.item.ItemStack;

public final class WarpingGearRegistry {

    private static final Map<ItemStatKey, Integer> RULES = new HashMap<>();

    private WarpingGearRegistry() {}

    public static synchronized Integer get(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }

        Integer value = RULES.get(ItemStatKey.exact(stack));
        if (value != null || !ItemStatKey.canHaveExactAndWildcard(stack)) {
            return value;
        }
        return RULES.get(ItemStatKey.wildcard(stack));
    }

    public static synchronized Change set(ItemStack stack, int amount) {
        requireItem(stack);
        ItemStatKey key = ItemStatKey.fromRule(stack);
        boolean hadPrevious = RULES.containsKey(key);
        Integer previous = RULES.put(key, amount);
        return new Change(key, hadPrevious, previous);
    }

    private static void requireItem(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            throw new IllegalArgumentException("Warping gear item cannot be null");
        }
    }

    private static synchronized void restore(Change change) {
        if (change.hadPrevious) {
            RULES.put(change.key, change.previous);
        } else {
            RULES.remove(change.key);
        }
    }

    public static final class Change {

        private final ItemStatKey key;
        private final boolean hadPrevious;
        private final Integer previous;

        private Change(ItemStatKey key, boolean hadPrevious, Integer previous) {
            this.key = key;
            this.hadPrevious = hadPrevious;
            this.previous = previous;
        }

        public void undo() {
            WarpingGearRegistry.restore(this);
        }
    }
}

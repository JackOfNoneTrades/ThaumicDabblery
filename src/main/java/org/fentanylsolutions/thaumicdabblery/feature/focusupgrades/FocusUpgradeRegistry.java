package org.fentanylsolutions.thaumicdabblery.feature.focusupgrades;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import thaumcraft.api.wands.FocusUpgradeType;

public final class FocusUpgradeRegistry {

    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 5;

    private static final Map<ItemKey, FocusRule> RULES = new HashMap<>();

    private FocusUpgradeRegistry() {}

    public static synchronized FocusUpgradeType[] getAvailableUpgrades(ItemStack focus, int tier,
        FocusUpgradeType[] nativeUpgrades) {
        TierRule rule = getTierRule(focus, tier);
        if (rule == null) {
            return nativeUpgrades;
        }

        Map<Short, FocusUpgradeType> resolved = new LinkedHashMap<>();
        if (!rule.cleared && nativeUpgrades != null) {
            for (FocusUpgradeType upgrade : nativeUpgrades) {
                if (upgrade != null) {
                    resolved.put(upgrade.id, upgrade);
                }
            }
        }
        for (short upgradeId : rule.additions) {
            FocusUpgradeType upgrade = getUpgrade(upgradeId);
            if (upgrade != null) {
                resolved.put(upgrade.id, upgrade);
            }
        }
        return resolved.values()
            .toArray(new FocusUpgradeType[resolved.size()]);
    }

    public static synchronized boolean isExplicitlyAdded(ItemStack focus, int tier, FocusUpgradeType upgrade) {
        if (upgrade == null) {
            return false;
        }
        TierRule rule = getTierRule(focus, tier);
        return rule != null && rule.additions.contains(upgrade.id);
    }

    public static synchronized Change add(ItemStack focus, int tier, short[] upgradeIds) {
        requireFocus(focus);
        requireTier(tier);
        if (upgradeIds == null || upgradeIds.length == 0) {
            throw new IllegalArgumentException("At least one focus upgrade ID is required");
        }

        ItemKey key = ItemKey.fromRule(focus);
        FocusRule previous = copy(RULES.get(key));
        FocusRule updated = previous == null ? new FocusRule() : copy(previous);
        TierRule rule = updated.tiers[tier - 1];
        if (rule == null) {
            rule = new TierRule(false);
            updated.tiers[tier - 1] = rule;
        }
        for (short upgradeId : upgradeIds) {
            requireUpgrade(upgradeId);
            rule.additions.add(upgradeId);
        }
        RULES.put(key, updated);
        return new Change(key, previous);
    }

    public static synchronized Change remove(ItemStack focus, int tier) {
        requireFocus(focus);
        requireTier(tier);

        ItemKey key = ItemKey.fromRule(focus);
        FocusRule previous = copy(RULES.get(key));
        FocusRule updated = previous == null ? new FocusRule() : copy(previous);
        updated.tiers[tier - 1] = new TierRule(true);
        RULES.put(key, updated);
        return new Change(key, previous);
    }

    public static synchronized Change removeAll(ItemStack focus) {
        requireFocus(focus);

        ItemKey key = ItemKey.fromRule(focus);
        FocusRule previous = copy(RULES.get(key));
        FocusRule updated = new FocusRule();
        for (int tier = 0; tier < MAX_TIER; tier++) {
            updated.tiers[tier] = new TierRule(true);
        }
        RULES.put(key, updated);
        return new Change(key, previous);
    }

    private static TierRule getTierRule(ItemStack focus, int tier) {
        if (focus == null || focus.getItem() == null || tier < MIN_TIER || tier > MAX_TIER) {
            return null;
        }

        FocusRule exact = RULES.get(ItemKey.exact(focus));
        TierRule rule = exact == null ? null : exact.tiers[tier - 1];
        if (rule != null || focus.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
            return rule;
        }
        FocusRule wildcard = RULES.get(ItemKey.wildcard(focus));
        return wildcard == null ? null : wildcard.tiers[tier - 1];
    }

    private static FocusUpgradeType getUpgrade(short id) {
        return id < 0 || id >= FocusUpgradeType.types.length ? null : FocusUpgradeType.types[id];
    }

    private static void requireUpgrade(short id) {
        if (getUpgrade(id) == null) {
            throw new IllegalArgumentException("Unknown Thaumcraft focus upgrade ID: " + id);
        }
    }

    private static void requireFocus(ItemStack focus) {
        if (focus == null || focus.getItem() == null) {
            throw new IllegalArgumentException("Focus upgrade target cannot be null");
        }
    }

    private static void requireTier(int tier) {
        if (tier < MIN_TIER || tier > MAX_TIER) {
            throw new IllegalArgumentException("Focus upgrade tier must be between 1 and 5: " + tier);
        }
    }

    private static FocusRule copy(FocusRule source) {
        if (source == null) {
            return null;
        }
        FocusRule copy = new FocusRule();
        for (int tier = 0; tier < MAX_TIER; tier++) {
            TierRule rule = source.tiers[tier];
            if (rule != null) {
                copy.tiers[tier] = new TierRule(rule.cleared, rule.additions);
            }
        }
        return copy;
    }

    private static synchronized void restore(Change change) {
        if (change.previous == null) {
            RULES.remove(change.key);
        } else {
            RULES.put(change.key, copy(change.previous));
        }
    }

    public static final class Change {

        private final ItemKey key;
        private final FocusRule previous;

        private Change(ItemKey key, FocusRule previous) {
            this.key = key;
            this.previous = previous;
        }

        public void undo() {
            FocusUpgradeRegistry.restore(this);
        }
    }

    private static final class FocusRule {

        private final TierRule[] tiers = new TierRule[MAX_TIER];
    }

    private static final class TierRule {

        private final boolean cleared;
        private final Set<Short> additions;

        private TierRule(boolean cleared) {
            this(cleared, new LinkedHashSet<Short>());
        }

        private TierRule(boolean cleared, Set<Short> additions) {
            this.cleared = cleared;
            this.additions = new LinkedHashSet<>(additions);
        }
    }

    private static final class ItemKey {

        private final Item item;
        private final int damage;

        private ItemKey(Item item, int damage) {
            this.item = item;
            this.damage = damage;
        }

        private static ItemKey fromRule(ItemStack stack) {
            return new ItemKey(stack.getItem(), stack.getItemDamage());
        }

        private static ItemKey exact(ItemStack stack) {
            return new ItemKey(stack.getItem(), stack.getItemDamage());
        }

        private static ItemKey wildcard(ItemStack stack) {
            return new ItemKey(stack.getItem(), OreDictionary.WILDCARD_VALUE);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ItemKey)) {
                return false;
            }
            ItemKey key = (ItemKey) other;
            return item == key.item && damage == key.damage;
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(item) + damage;
        }
    }
}

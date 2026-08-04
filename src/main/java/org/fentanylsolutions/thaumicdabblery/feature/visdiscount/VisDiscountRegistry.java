package org.fentanylsolutions.thaumicdabblery.feature.visdiscount;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import thaumcraft.api.aspects.Aspect;

public final class VisDiscountRegistry {

    private static final Map<ItemKey, DiscountRule> RULES = new HashMap<>();

    private VisDiscountRegistry() {}

    public static synchronized Integer get(ItemStack stack, Aspect aspect) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }

        int damage = getRuleDamage(stack);
        DiscountRule exactRule = RULES.get(new ItemKey(stack.getItem(), damage));
        Integer discount = exactRule == null ? null : exactRule.get(aspect);
        if (discount != null || damage == OreDictionary.WILDCARD_VALUE) {
            return discount;
        }

        DiscountRule wildcardRule = RULES.get(new ItemKey(stack.getItem(), OreDictionary.WILDCARD_VALUE));
        return wildcardRule == null ? null : wildcardRule.get(aspect);
    }

    public static synchronized Change set(ItemStack stack, Aspect aspect, int discount) {
        if (stack == null || stack.getItem() == null) {
            throw new IllegalArgumentException("Vis discount item cannot be null");
        }

        ItemKey key = new ItemKey(stack.getItem(), getRuleDamage(stack));
        DiscountRule rule = RULES.get(key);
        if (rule == null) {
            rule = new DiscountRule();
            RULES.put(key, rule);
        }

        Integer previous = rule.getDirect(aspect);
        boolean hadPrevious = previous != null;
        rule.set(aspect, discount);
        return new Change(key, aspect, hadPrevious, previous);
    }

    static synchronized TooltipRule getTooltipRule(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return TooltipRule.EMPTY;
        }

        int damage = getRuleDamage(stack);
        DiscountRule exactRule = RULES.get(new ItemKey(stack.getItem(), damage));
        DiscountRule wildcardRule = damage == OreDictionary.WILDCARD_VALUE ? null
            : RULES.get(new ItemKey(stack.getItem(), OreDictionary.WILDCARD_VALUE));

        Integer universal = exactRule != null && exactRule.universal != null ? exactRule.universal
            : wildcardRule == null ? null : wildcardRule.universal;
        Map<Aspect, Integer> aspects = new LinkedHashMap<>();

        if (wildcardRule != null && (exactRule == null || exactRule.universal == null)) {
            aspects.putAll(wildcardRule.aspects);
        }
        if (exactRule != null) {
            aspects.putAll(exactRule.aspects);
        }

        return universal == null && aspects.isEmpty() ? TooltipRule.EMPTY : new TooltipRule(universal, aspects);
    }

    private static int getRuleDamage(ItemStack stack) {
        return stack.getItem()
            .isDamageable() ? OreDictionary.WILDCARD_VALUE : stack.getItemDamage();
    }

    private static synchronized void restore(Change change) {
        DiscountRule rule = RULES.get(change.key);
        if (rule == null) {
            rule = new DiscountRule();
            RULES.put(change.key, rule);
        }

        if (change.hadPrevious) {
            rule.set(change.aspect, change.previous);
        } else {
            rule.remove(change.aspect);
            if (rule.isEmpty()) {
                RULES.remove(change.key);
            }
        }
    }

    public static final class Change {

        private final ItemKey key;
        private final Aspect aspect;
        private final boolean hadPrevious;
        private final Integer previous;

        private Change(ItemKey key, Aspect aspect, boolean hadPrevious, Integer previous) {
            this.key = key;
            this.aspect = aspect;
            this.hadPrevious = hadPrevious;
            this.previous = previous;
        }

        public void undo() {
            VisDiscountRegistry.restore(this);
        }
    }

    static final class TooltipRule {

        private static final TooltipRule EMPTY = new TooltipRule(null, new LinkedHashMap<Aspect, Integer>());

        private final Integer universal;
        private final Map<Aspect, Integer> aspects;

        private TooltipRule(Integer universal, Map<Aspect, Integer> aspects) {
            this.universal = universal;
            this.aspects = new LinkedHashMap<>(aspects);
        }

        Integer getUniversal() {
            return universal;
        }

        Map<Aspect, Integer> getAspects() {
            return aspects;
        }

        boolean isEmpty() {
            return universal == null && aspects.isEmpty();
        }
    }

    private static final class DiscountRule {

        private Integer universal;
        private final Map<Aspect, Integer> aspects = new LinkedHashMap<>();

        private Integer get(Aspect aspect) {
            Integer aspectDiscount = aspect == null ? null : aspects.get(aspect);
            return aspectDiscount == null ? universal : aspectDiscount;
        }

        private Integer getDirect(Aspect aspect) {
            return aspect == null ? universal : aspects.get(aspect);
        }

        private void set(Aspect aspect, int discount) {
            if (aspect == null) {
                universal = discount;
            } else {
                aspects.put(aspect, discount);
            }
        }

        private void remove(Aspect aspect) {
            if (aspect == null) {
                universal = null;
            } else {
                aspects.remove(aspect);
            }
        }

        private boolean isEmpty() {
            return universal == null && aspects.isEmpty();
        }
    }

    private static final class ItemKey {

        private final Item item;
        private final int damage;

        private ItemKey(Item item, int damage) {
            this.item = item;
            this.damage = damage;
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

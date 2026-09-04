package org.fentanylsolutions.thaumicdabblery.feature.wandcomponents;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.WandCap;

public final class WandComponentVisDiscountRegistry {

    private static final Map<String, DiscountRule> CAP_RULES = new HashMap<>();
    private static final Map<ItemKey, DiscountRule> CASTING_RULES = new HashMap<>();

    private WandComponentVisDiscountRegistry() {}

    public static synchronized float resolve(ItemStack castingStack, WandCap cap, Aspect aspect, float nativeModifier) {
        Integer discount = getCastingDiscount(castingStack, aspect);
        if (discount == null && cap != null) {
            discount = getCapDiscount(cap.getTag(), aspect);
        }
        return discount == null ? nativeModifier : 1.0F - discount / 100.0F;
    }

    public static synchronized Change setCap(String capId, Aspect aspect, int discount) {
        requireAspect(aspect);
        if (capId == null || capId.isEmpty()) {
            throw new IllegalArgumentException("Wand cap ID cannot be empty");
        }

        DiscountRule rule = CAP_RULES.get(capId);
        if (rule == null) {
            rule = new DiscountRule();
            CAP_RULES.put(capId, rule);
        }

        Integer previous = rule.get(aspect);
        boolean hadPrevious = previous != null;
        rule.set(aspect, discount);
        return Change.forCap(capId, aspect, hadPrevious, previous);
    }

    public static synchronized Change setCastingItem(ItemStack stack, Aspect aspect, int discount) {
        requireCastingItem(stack);
        requireAspect(aspect);

        ItemKey key = ItemKey.fromRule(stack);
        DiscountRule rule = CASTING_RULES.get(key);
        if (rule == null) {
            rule = new DiscountRule();
            CASTING_RULES.put(key, rule);
        }

        Integer previous = rule.get(aspect);
        boolean hadPrevious = previous != null;
        rule.set(aspect, discount);
        return Change.forCastingItem(key, aspect, hadPrevious, previous);
    }

    private static Integer getCapDiscount(String capId, Aspect aspect) {
        if (capId == null || aspect == null) {
            return null;
        }
        DiscountRule rule = CAP_RULES.get(capId);
        return rule == null ? null : rule.get(aspect);
    }

    private static Integer getCastingDiscount(ItemStack stack, Aspect aspect) {
        if (stack == null || stack.getItem() == null || aspect == null) {
            return null;
        }

        DiscountRule exact = CASTING_RULES.get(ItemKey.exact(stack));
        Integer discount = exact == null ? null : exact.get(aspect);
        if (discount != null || stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
            return discount;
        }

        DiscountRule wildcard = CASTING_RULES.get(ItemKey.wildcard(stack));
        return wildcard == null ? null : wildcard.get(aspect);
    }

    private static void requireCastingItem(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            throw new IllegalArgumentException("Casting item cannot be null");
        }
    }

    private static void requireAspect(Aspect aspect) {
        if (aspect == null || !aspect.isPrimal()) {
            throw new IllegalArgumentException("Wand component vis discounts require a primal aspect");
        }
    }

    private static synchronized void restore(Change change) {
        Map<?, DiscountRule> rules = change.capId == null ? CASTING_RULES : CAP_RULES;
        Object key = change.capId == null ? change.itemKey : change.capId;
        DiscountRule rule = rules.get(key);
        if (rule == null) {
            rule = new DiscountRule();
            if (change.capId == null) {
                CASTING_RULES.put(change.itemKey, rule);
            } else {
                CAP_RULES.put(change.capId, rule);
            }
        }

        if (change.hadPrevious) {
            rule.set(change.aspect, change.previous);
        } else {
            rule.remove(change.aspect);
            if (rule.isEmpty()) {
                if (change.capId == null) {
                    CASTING_RULES.remove(change.itemKey);
                } else {
                    CAP_RULES.remove(change.capId);
                }
            }
        }
    }

    public static final class Change {

        private final String capId;
        private final ItemKey itemKey;
        private final Aspect aspect;
        private final boolean hadPrevious;
        private final Integer previous;

        private Change(String capId, ItemKey itemKey, Aspect aspect, boolean hadPrevious, Integer previous) {
            this.capId = capId;
            this.itemKey = itemKey;
            this.aspect = aspect;
            this.hadPrevious = hadPrevious;
            this.previous = previous;
        }

        private static Change forCap(String capId, Aspect aspect, boolean hadPrevious, Integer previous) {
            return new Change(capId, null, aspect, hadPrevious, previous);
        }

        private static Change forCastingItem(ItemKey itemKey, Aspect aspect, boolean hadPrevious, Integer previous) {
            return new Change(null, itemKey, aspect, hadPrevious, previous);
        }

        public void undo() {
            WandComponentVisDiscountRegistry.restore(this);
        }
    }

    private static final class DiscountRule {

        private final Map<Aspect, Integer> aspects = new LinkedHashMap<>();

        private Integer get(Aspect aspect) {
            return aspects.get(aspect);
        }

        private void set(Aspect aspect, int discount) {
            aspects.put(aspect, discount);
        }

        private void remove(Aspect aspect) {
            aspects.remove(aspect);
        }

        private boolean isEmpty() {
            return aspects.isEmpty();
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

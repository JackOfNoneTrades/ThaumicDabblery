package org.fentanylsolutions.thaumicdabblery.feature.wandcomponents;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.IWandRodOnUpdate;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandRodPrimalOnUpdate;

/** Reloadable rules. An absent regeneration program inherits; an empty one explicitly disables TC regeneration. */
public final class WandComponentStatsRegistry {

    // Leave room for sceptre scaling and adding another full charge in Thaumcraft's integer arithmetic.
    public static final int MAX_CAPACITY = Integer.MAX_VALUE / 300;
    private static final Map<String, NativeValue> NATIVE_VALUES = new LinkedHashMap<>();
    private static final Map<Object, Integer> CAPACITIES = new HashMap<>();
    private static final Map<Object, Integer> POTENCY = new HashMap<>();
    private static final Map<Object, Map<Aspect, Regeneration>> REGENERATION = new HashMap<>();

    private WandComponentStatsRegistry() {}

    public static synchronized Runnable setCapCost(WandCap cap, int cost) {
        requireCraftCost(cost);
        for (WandRod rod : WandRod.rods.values()) {
            requireProduct(cost, configuredValue("cost:" + rod.getTag(), rod.getCraftCost()));
        }
        return setNative("cap:" + cap.getTag(), cap::getCraftCost, cap::setCraftCost, cost);
    }

    public static synchronized Runnable setCoreCost(WandRod rod, int cost) {
        requireCraftCost(cost);
        for (WandCap cap : WandCap.caps.values()) {
            requireProduct(cost, configuredValue("cap:" + cap.getTag(), cap.getCraftCost()));
        }
        return setNative("cost:" + rod.getTag(), rod::getCraftCost, rod::setCraftCost, cost);
    }

    public static synchronized Runnable setCoreCapacity(WandRod rod, int capacity) {
        requireCapacity(capacity);
        return setNative("capacity:" + rod.getTag(), rod::getCapacity, rod::setCapacity, capacity);
    }

    public static synchronized Runnable setCastingCapacity(ItemStack stack, int capacity) {
        requireCapacity(capacity);
        return replace(CAPACITIES, castingKey(stack), capacity);
    }

    public static synchronized Runnable setPotency(Object target, int levels) {
        if (levels < 0 || levels > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Innate Potency must be between 0 and 32767");
        }
        return replace(POTENCY, target, levels);
    }

    public static synchronized Runnable setRegeneration(Object target, Aspect aspect, int interval, double amount,
        int ceilingPercent) {
        if (aspect == null || !aspect.isPrimal()) {
            throw new IllegalArgumentException("Vis regeneration requires a primal aspect");
        }
        if (interval < 1 || !Double.isFinite(amount)
            || amount < 0.01
            || amount > MAX_CAPACITY
            || ceilingPercent < 1
            || ceilingPercent > 100) {
            throw new IllegalArgumentException(
                "Regeneration requires interval >= 1 tick, amount between 0.01 and " + MAX_CAPACITY
                    + " Vis, and a ceiling between 1 and 100 percent");
        }
        Map<Aspect, Regeneration> previous = REGENERATION.get(target);
        Map<Aspect, Regeneration> next = previous == null ? new LinkedHashMap<>() : new LinkedHashMap<>(previous);
        next.put(aspect, new Regeneration(interval, (int) Math.round(amount * 100), ceilingPercent));
        return replace(REGENERATION, target, Collections.unmodifiableMap(next));
    }

    public static synchronized Runnable disableRegeneration(Object target) {
        return replace(REGENERATION, target, Collections.emptyMap());
    }

    public static synchronized Runnable resetRegeneration(Object target) {
        return replace(REGENERATION, target, null);
    }

    public static Object castingKey(ItemStack stack) {
        return new CastingKey(stack.getItem(), stack.getItemDamage());
    }

    public static synchronized Integer getPotency(ItemStack stack) {
        return resolve(POTENCY, stack);
    }

    public static synchronized Integer getCorePotency(String coreId) {
        return POTENCY.get(coreId);
    }

    public static synchronized Map<Aspect, Regeneration> getCoreRegeneration(String coreId) {
        return REGENERATION.get(coreId);
    }

    public static synchronized Map<Aspect, Regeneration> getRegeneration(ItemStack stack) {
        return resolve(REGENERATION, stack);
    }

    public static synchronized int capacity(ItemStack stack, int nativeCapacity) {
        if (!WandComponentStatsFeature.isEnabled()) return nativeCapacity;
        Integer capacity = resolveCasting(CAPACITIES, stack);
        return capacity == null ? nativeCapacity : capacity * 100;
    }

    public static void updateNative(IWandRodOnUpdate update, ItemStack stack, EntityPlayer player) {
        // Only replace base TC's callback. Addon conversions and other abilities remain untouched.
        if (!WandComponentStatsFeature.isEnabled() || getRegeneration(stack) == null
            || update.getClass() != WandRodPrimalOnUpdate.class) {
            update.onUpdate(stack, player);
        }
    }

    public static synchronized void tick(ItemStack stack, EntityPlayer player) {
        if (!WandComponentStatsFeature.isEnabled()) return;
        ItemWandCasting wand = (ItemWandCasting) stack.getItem();
        int maximum = wand.getMaxVis(stack);
        Map<Aspect, Regeneration> program = getRegeneration(stack);
        if (hasCapacityRule(stack)) {
            for (Aspect aspect : Aspect.getPrimalAspects()) {
                int stored = wand.getVis(stack, aspect);
                if (stored > maximum) wand.storeVis(stack, aspect, maximum);
            }
        }
        if (program == null) return;
        for (Map.Entry<Aspect, Regeneration> entry : program.entrySet()) {
            Regeneration rule = entry.getValue();
            if (player.ticksExisted % rule.interval != 0) continue;
            int ceiling = (int) ((long) maximum * rule.ceilingPercent / 100);
            int stored = wand.getVis(stack, entry.getKey());
            if (stored < ceiling) {
                wand.storeVis(stack, entry.getKey(), (int) Math.min(ceiling, (long) stored + rule.amount));
            }
        }
    }

    private static boolean hasCapacityRule(ItemStack stack) {
        WandRod rod = ((ItemWandCasting) stack.getItem()).getRod(stack);
        return resolveCasting(CAPACITIES, stack) != null
            || rod != null && NATIVE_VALUES.containsKey("capacity:" + rod.getTag());
    }

    private static <T> T resolve(Map<Object, T> rules, ItemStack stack) {
        T value = resolveCasting(rules, stack);
        WandRod rod = ((ItemWandCasting) stack.getItem()).getRod(stack);
        return value != null || rod == null ? value : rules.get(rod.getTag());
    }

    private static <T> T resolveCasting(Map<Object, T> rules, ItemStack stack) {
        T value = rules.get(castingKey(stack));
        return value != null ? value : rules.get(new CastingKey(stack.getItem(), OreDictionary.WILDCARD_VALUE));
    }

    private static <T> Runnable replace(Map<Object, T> rules, Object key, T value) {
        T previous = rules.get(key);
        putOrRemove(rules, key, value);
        return () -> {
            synchronized (WandComponentStatsRegistry.class) {
                putOrRemove(rules, key, previous);
            }
        };
    }

    private static <K, V> void putOrRemove(Map<K, V> map, K key, V value) {
        if (value == null) map.remove(key);
        else map.put(key, value);
    }

    private static Runnable setNative(String key, IntSupplier getter, IntConsumer setter, int value) {
        NativeValue previous = NATIVE_VALUES.get(key);
        int original = previous == null ? getter.getAsInt() : previous.original;
        NativeValue next = new NativeValue(setter, original, value);
        NATIVE_VALUES.put(key, next);
        next.apply();
        return () -> {
            synchronized (WandComponentStatsRegistry.class) {
                putOrRemove(NATIVE_VALUES, key, previous);
                if (previous == null) setter.accept(original);
                else previous.apply();
            }
        };
    }

    public static synchronized void refreshNativeValues() {
        for (NativeValue value : NATIVE_VALUES.values()) value.apply();
    }

    private static void requireCapacity(int capacity) {
        if (capacity < 1 || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("Vis capacity must be between 1 and " + MAX_CAPACITY);
        }
    }

    private static int configuredValue(String key, int fallback) {
        NativeValue value = NATIVE_VALUES.get(key);
        return value == null ? fallback : value.value;
    }

    private static void requireCraftCost(int cost) {
        if (cost < 0 || cost > 21845) {
            throw new IllegalArgumentException("Wand crafting cost/multiplier must be between 0 and 21845");
        }
    }

    private static void requireProduct(int first, int second) {
        if ((long) first * second * 3 / 2 > Short.MAX_VALUE) {
            throw new IllegalArgumentException(
                "Combined cap/core cost exceeds Thaumcraft's safe recipe metadata range (32767 including sceptre scaling)");
        }
    }

    public static final class Regeneration {

        public final int interval;
        public final int amount;
        public final int ceilingPercent;

        private Regeneration(int interval, int amount, int ceilingPercent) {
            this.interval = interval;
            this.amount = amount;
            this.ceilingPercent = ceilingPercent;
        }
    }

    private static final class NativeValue {

        private final IntConsumer setter;
        private final int original;
        private final int value;

        private NativeValue(IntConsumer setter, int original, int value) {
            this.setter = setter;
            this.original = original;
            this.value = value;
        }

        private void apply() {
            setter.accept(WandComponentStatsFeature.isEnabled() ? value : original);
        }
    }

    private static final class CastingKey {

        private final Item item;
        private final int metadata;

        private CastingKey(Item item, int metadata) {
            this.item = item;
            this.metadata = metadata;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof CastingKey)) return false;
            CastingKey key = (CastingKey) other;
            return item == key.item && metadata == key.metadata;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(item) * 31 + metadata;
        }
    }
}

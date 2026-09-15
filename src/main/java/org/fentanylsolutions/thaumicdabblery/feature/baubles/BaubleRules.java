package org.fentanylsolutions.thaumicdabblery.feature.baubles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.IBaubleExpanded;

/** Immutable, undoable slot edits. Wildcard edits run before metadata-specific edits. */
public final class BaubleRules {

    private static final Map<Key, List<Edit>> RULES = new HashMap<>();

    private BaubleRules() {}

    public static synchronized boolean hasRule(ItemStack stack) {
        return BaubleSlotsFeature.isEnabled() && stack != null
            && (RULES.containsKey(new Key(stack, true)) || RULES.containsKey(new Key(stack, false)));
    }

    public static String[] nativeTypes(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof IBaubleExpanded) {
            String[] types = ((IBaubleExpanded) item).getBaubleTypes(stack);
            return types == null ? new String[0] : types.clone();
        }
        if (item instanceof IBauble) {
            BaubleType type = ((IBauble) item).getBaubleType(stack);
            return type == null ? new String[0] : new String[] { BaubleExpandedSlots.getTypeFromBaubleType(type) };
        }
        return new String[0];
    }

    public static synchronized String[] types(ItemStack stack) {
        if (stack == null) return new String[0];
        Set<String> result = new LinkedHashSet<>(Arrays.asList(nativeTypes(stack)));
        result.remove(null);
        result.remove("");
        if (BaubleSlotsFeature.isEnabled()) {
            Key wildcard = new Key(stack, true);
            apply(result, RULES.get(wildcard));
            Key exact = new Key(stack, false);
            if (!exact.equals(wildcard)) apply(result, RULES.get(exact));
        }
        return result.toArray(new String[0]);
    }

    private static void apply(Set<String> result, List<Edit> edits) {
        if (edits == null) return;
        for (Edit edit : edits) {
            if (edit.operation == 0) result.clear();
            if (edit.operation == 2) result.removeAll(edit.types);
            else result.addAll(edit.types);
        }
    }

    public static synchronized Change edit(ItemStack stack, int operation, String[] types) {
        if (stack == null || stack.getItem() == null) throw new IllegalArgumentException("Bauble item cannot be null");
        if (stack.hasTagCompound()) throw new IllegalArgumentException(
            "Bauble rules match item and metadata, not NBT; remove the tag from the script target");
        if (types == null || operation < 0 || operation > 2)
            throw new IllegalArgumentException("Invalid bauble slot edit");
        Set<String> normalized = new LinkedHashSet<>();
        for (String type : types) {
            if (type == null) throw new IllegalArgumentException("Bauble slot type cannot be null");
            String name = type.trim()
                .toLowerCase(Locale.ROOT);
            if (name.isEmpty() || name.equals(BaubleExpandedSlots.unknownType)
                || !BaubleExpandedSlots.isTypeRegistered(name))
                throw new IllegalArgumentException("Unknown Baubles Expanded slot type: " + type);
            normalized.add(name);
        }
        Key key = new Key(stack, false);
        List<Edit> previous = RULES.get(key);
        List<Edit> next = previous == null ? new ArrayList<>() : new ArrayList<>(previous);
        next.add(new Edit(operation, normalized));
        RULES.put(key, next);
        return new Change(key, previous);
    }

    private static final class Edit {

        final int operation;
        final Set<String> types;

        Edit(int operation, Set<String> types) {
            this.operation = operation;
            this.types = types;
        }
    }

    public static final class Change {

        private final Key key;
        private final List<Edit> previous;

        private Change(Key key, List<Edit> previous) {
            this.key = key;
            this.previous = previous;
        }

        public void undo() {
            synchronized (BaubleRules.class) {
                if (previous == null) RULES.remove(key);
                else RULES.put(key, previous);
            }
        }
    }

    private static final class Key {

        final Item item;
        final int damage;

        Key(ItemStack stack, boolean wildcard) {
            item = stack.getItem();
            damage = wildcard || item.isDamageable() ? OreDictionary.WILDCARD_VALUE : stack.getItemDamage();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key && ((Key) other).item == item && ((Key) other).damage == damage;
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(item) + damage;
        }
    }
}

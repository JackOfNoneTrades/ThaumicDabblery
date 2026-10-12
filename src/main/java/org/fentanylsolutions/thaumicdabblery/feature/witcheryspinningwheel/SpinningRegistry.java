package org.fentanylsolutions.thaumicdabblery.feature.witcheryspinningwheel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.fentanylsolutions.thaumicdabblery.mixins.late.witchery.SpinningRecipeAccessor;

import com.emoniph.witchery.crafting.SpinningRecipes;
import com.emoniph.witchery.crafting.SpinningRecipes.SpinningRecipe;

public final class SpinningRegistry {

    private static final Map<SpinningRecipe, Recipe> DEFINITIONS = new IdentityHashMap<>();
    private static final Map<String, Integer> ORDINALS = new HashMap<>();

    private SpinningRegistry() {}

    public static String key(ItemStack s) {
        return Item.itemRegistry.getNameForObject(s.getItem()) + "/" + s.getItemDamage();
    }

    public static List<Recipe> recipes() {
        List<Recipe> result = new ArrayList<>();
        for (SpinningRecipe k : SpinningRecipes.instance().recipes) {
            Recipe r = DEFINITIONS.get(k);
            if (r == null) {
                String key = key(k.result);
                int n = ORDINALS.getOrDefault(key, 0);
                ORDINALS.put(key, n + 1);
                r = new Recipe("builtin:" + key + "/" + n, k);
                DEFINITIONS.put(k, r);
            }
            result.add(r);
        }
        return Collections.unmodifiableList(result);
    }

    public static void replace(List<Recipe> rules) {
        for (Recipe r : rules) if (r.changed) validate(r);
        List<SpinningRecipe> next = new ArrayList<>();
        Map<SpinningRecipe, Recipe> definitions = new IdentityHashMap<>();
        for (Recipe r : rules) {
            SpinningRecipe k = r.changed
                ? SpinningRecipeAccessor.td$create(r.output.copy(), r.input.copy(), copies(r.modifiers))
                : r.original;
            next.add(k);
            definitions.put(k, r);
        }
        SpinningRecipes.instance().recipes.clear();
        SpinningRecipes.instance().recipes.addAll(next);
        DEFINITIONS.clear();
        DEFINITIONS.putAll(definitions);
    }

    public static ItemStack stack(ItemStack s, boolean input, boolean single) {
        if (s == null || s.getItem() == null
            || s.getItemDamage() < 0
            || s.getItemDamage() == 32767
            || s.stackSize < 1
            || s.stackSize > Math.min(64, s.getMaxStackSize())
            || single && s.stackSize != 1
            || input && s.hasTagCompound())
            throw new IllegalArgumentException(
                "expected concrete stacks within stack limits; modifiers must be single items and inputs cannot require NBT");
        return s.copy();
    }

    public static ItemStack[] copies(ItemStack[] values) {
        ItemStack[] result = new ItemStack[values.length];
        for (int i = 0; i < values.length; i++) result[i] = values[i].copy();
        return result;
    }

    private static void validate(Recipe r) {
        stack(r.input, true, false);
        stack(r.output, false, false);
        if (r.modifiers == null || r.modifiers.length > 3)
            throw new IllegalArgumentException("expected zero to three modifiers");
        for (ItemStack s : r.modifiers) stack(s, true, true);
    }

    public static boolean modifiersMatch(ItemStack[] required, ItemStack[] slots) {
        boolean[] used = new boolean[slots.length];
        for (ItemStack requirement : required) {
            boolean found = false;
            for (int i = 0; i < slots.length; i++)
                if (!used[i] && slots[i] != null && slots[i].stackSize > 0 && slots[i].isItemEqual(requirement)) {
                    used[i] = true;
                    found = true;
                    break;
                }
            if (!found) return false;
        }
        return true;
    }

    public static SpinningRecipe select(ItemStack input, ItemStack[] modifiers) {
        if (input == null) return null;
        for (SpinningRecipe k : SpinningRecipes.instance().recipes) {
            if (input.isItemEqual(k.fibre) && modifiersMatch(k.modifiers, modifiers)) {
                // A matching recipe with insufficient fibre waits instead of falling through to a cheaper one.
                return input.stackSize >= k.fibre.stackSize ? k : null;
            }
        }
        return null;
    }

    public static NBTTagCompound signature(SpinningRecipe k) {
        if (k == null) return null;
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Input", k.fibre.writeToNBT(new NBTTagCompound()));
        tag.setTag("Output", k.result.writeToNBT(new NBTTagCompound()));
        NBTTagList modifiers = new NBTTagList();
        for (ItemStack s : k.modifiers) modifiers.appendTag(s.writeToNBT(new NBTTagCompound()));
        tag.setTag("Modifiers", modifiers);
        return tag;
    }

    public static final class Recipe {

        public final String id;
        public final SpinningRecipe original;
        public boolean changed;
        public ItemStack input, output;
        public ItemStack[] modifiers;

        public Recipe(String id, ItemStack input, ItemStack[] modifiers, ItemStack output) {
            this.id = id;
            this.input = input;
            this.modifiers = modifiers;
            this.output = output;
            original = null;
            changed = true;
        }

        private Recipe(String id, SpinningRecipe k) {
            this.id = id;
            original = k;
            input = k.fibre.copy();
            output = k.result.copy();
            modifiers = copies(k.modifiers);
        }

        public Recipe copy() {
            return new Recipe(id, input.copy(), copies(modifiers), output.copy());
        }
    }
}

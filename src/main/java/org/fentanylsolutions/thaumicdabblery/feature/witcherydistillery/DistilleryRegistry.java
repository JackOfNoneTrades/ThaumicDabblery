package org.fentanylsolutions.thaumicdabblery.feature.witcherydistillery;

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

import org.fentanylsolutions.thaumicdabblery.mixins.late.witchery.DistilleryRecipeAccessor;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.crafting.DistilleryRecipes;
import com.emoniph.witchery.crafting.DistilleryRecipes.DistilleryRecipe;

public final class DistilleryRegistry {

    private static final Map<DistilleryRecipe, Recipe> definitions = new IdentityHashMap<>();
    private static final Map<String, Integer> ordinals = new HashMap<>();

    private DistilleryRegistry() {}

    public static String key(ItemStack s) {
        return s == null ? "empty" : Item.itemRegistry.getNameForObject(s.getItem()) + "/" + s.getItemDamage();
    }

    public static Recipe definition(DistilleryRecipe k) {
        return definitions.get(k);
    }

    public static List<Recipe> recipes() {
        List<Recipe> result = new ArrayList<>();
        for (DistilleryRecipe k : DistilleryRecipes.instance().recipes) {
            Recipe r = definitions.get(k);
            if (r == null) {
                String key = key(k.outputs[0]);
                int n = ordinals.containsKey(key) ? ordinals.get(key) : 0;
                ordinals.put(key, n + 1);
                r = new Recipe("native:" + key + "/" + n, k);
                definitions.put(k, r);
            }
            result.add(r);
        }
        return Collections.unmodifiableList(result);
    }

    public static void replace(List<Recipe> recipes) {
        List<DistilleryRecipe> next = new ArrayList<>();
        Map<DistilleryRecipe, Recipe> added = new IdentityHashMap<>();
        for (Recipe r : recipes) {
            DistilleryRecipe k = r.original;
            if (r.changed) {
                validate(r);
                ItemStack[] in = copies(r.inputs), out = copies(r.outputs);
                k = DistilleryRecipeAccessor.td$create(in[0], in[1], r.jars, out[0], out[1], out[2], out[3]);
            }
            next.add(k);
            added.put(k, r);
        }
        DistilleryRecipes.instance().recipes.clear();
        DistilleryRecipes.instance().recipes.addAll(next);
        definitions.clear();
        definitions.putAll(added);
    }

    public static ItemStack input(ItemStack s) {
        if (s == null || s.getItem() == null
            || s.stackSize != 1
            || s.getItemDamage() < 0
            || s.getItemDamage() == 32767
            || s.hasTagCompound()
            || Witchery.Items.GENERIC.itemEmptyClayJar.isMatch(s))
            throw new IllegalArgumentException(
                "inputs must be single exact items without NBT; empty clay jars belong in the jar slot");
        return s.copy();
    }

    private static void validate(Recipe r) {
        if (r.inputs == null || r.inputs.length != 2 || r.inputs[0] == null)
            throw new IllegalArgumentException("expected one or two inputs");
        input(r.inputs[0]);
        if (r.inputs[1] != null) input(r.inputs[1]);
        if (r.outputs == null || r.outputs.length != 4 || r.outputs[0] == null)
            throw new IllegalArgumentException("expected one to four outputs");
        for (ItemStack s : r.outputs) if (s != null && (s.getItem() == null || s.getItemDamage() < 0
            || s.getItemDamage() == 32767
            || s.stackSize < 1
            || s.stackSize > Math.min(64, s.getMaxStackSize())))
            throw new IllegalArgumentException("outputs must be concrete stacks within their stack limits");
        if (r.jars < 0 || r.jars > Math.min(
            64,
            Witchery.Items.GENERIC.itemEmptyClayJar.createStack()
                .getMaxStackSize()))
            throw new IllegalArgumentException("jar cost must be between 0 and the clay jar stack limit");
    }

    private static ItemStack[] copies(ItemStack[] array) {
        ItemStack[] result = new ItemStack[array.length];
        for (int i = 0; i < array.length; i++) result[i] = array[i] == null ? null : array[i].copy();
        return result;
    }

    public static boolean same(ItemStack a, ItemStack b) {
        return a == null ? b == null : b != null && a.isItemEqual(b);
    }

    public static boolean uses(DistilleryRecipe k, ItemStack query) {
        Recipe r = definition(k);
        for (ItemStack in : k.inputs) if (in != null
            && (r != null && r.changed ? same(in, query) : ((DistilleryRecipeAccessor) k).td$matchesInput(query, in)))
            return true;
        return false;
    }

    public static boolean ingredient(ItemStack query) {
        for (DistilleryRecipe k : DistilleryRecipes.instance().recipes) if (uses(k, query)) return true;
        return false;
    }

    public static DistilleryRecipe select(ItemStack a, ItemStack b, ItemStack jars) {
        for (DistilleryRecipe k : DistilleryRecipes.instance().recipes) {
            Recipe r = definition(k);
            boolean match = r != null && r.changed
                ? (same(a, k.inputs[0]) && same(b, k.inputs[1]) || same(a, k.inputs[1]) && same(b, k.inputs[0]))
                : ((DistilleryRecipeAccessor) k)
                    .td$matches(a, b, Witchery.Items.GENERIC.itemEmptyClayJar.createStack(Math.max(64, k.jars)));
            if (match) return k.jars == 0
                || jars != null && Witchery.Items.GENERIC.itemEmptyClayJar.isMatch(jars) && jars.stackSize >= k.jars ? k
                    : null;
        }
        return null;
    }

    public static NBTTagCompound signature(DistilleryRecipe recipe) {
        if (recipe == null) return null;
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Jars", recipe.jars);
        tag.setTag("Inputs", stacks(recipe.inputs));
        tag.setTag("Outputs", stacks(recipe.outputs));
        return tag;
    }

    private static NBTTagList stacks(ItemStack[] values) {
        NBTTagList list = new NBTTagList();
        for (ItemStack value : values)
            list.appendTag(value == null ? new NBTTagCompound() : value.writeToNBT(new NBTTagCompound()));
        return list;
    }

    public static final class Recipe {

        public final String id;
        public final DistilleryRecipe original;
        public boolean changed;
        public ItemStack[] inputs, outputs;
        public int jars;

        public Recipe(String id, ItemStack[] inputs, ItemStack[] outputs, int jars) {
            this.id = id;
            this.inputs = inputs;
            this.outputs = outputs;
            this.jars = jars;
            original = null;
            changed = true;
        }

        private Recipe(String id, DistilleryRecipe k) {
            this.id = id;
            original = k;
            inputs = copies(k.inputs);
            outputs = copies(k.outputs);
            jars = k.jars;
        }

        public Recipe copy() {
            Recipe r = new Recipe(id, original, inputs, outputs, jars);
            return r;
        }

        private Recipe(String id, DistilleryRecipe original, ItemStack[] inputs, ItemStack[] outputs, int jars) {
            this.id = id;
            this.original = original;
            this.inputs = copies(inputs);
            this.outputs = copies(outputs);
            this.jars = jars;
            changed = true;
        }
    }
}

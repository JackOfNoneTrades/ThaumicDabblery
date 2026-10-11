package org.fentanylsolutions.thaumicdabblery.feature.witcherykettle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.mixins.late.witchery.KettleRecipeAccessor;

import com.emoniph.witchery.crafting.KettleRecipes;
import com.emoniph.witchery.crafting.KettleRecipes.KettleRecipe;

public final class KettleRegistry {

    private static final Map<KettleRecipe, Recipe> definitions = new IdentityHashMap<>();
    private static final Map<String, Integer> ordinals = new HashMap<>();

    private KettleRegistry() {}

    public static String key(ItemStack s) {
        return Item.itemRegistry.getNameForObject(s.getItem()) + "/" + s.getItemDamage();
    }

    public static Recipe definition(KettleRecipe nativeRecipe) {
        return definitions.get(nativeRecipe);
    }

    public static List<Recipe> recipes() {
        List<Recipe> result = new ArrayList<>();
        for (KettleRecipe k : KettleRecipes.instance().recipes) {
            Recipe r = definitions.get(k);
            if (r == null) {
                String key = key(k.output);
                int ordinal = ordinals.containsKey(key) ? ordinals.get(key) : 0;
                ordinals.put(key, ordinal + 1);
                r = new Recipe("native:" + key + "/" + ordinal, k);
                definitions.put(k, r);
            }
            result.add(r);
        }
        return Collections.unmodifiableList(result);
    }

    public static void replace(List<Recipe> recipes) {
        List<KettleRecipe> next = new ArrayList<>();
        Map<KettleRecipe, Recipe> added = new IdentityHashMap<>();
        for (Recipe r : recipes) {
            KettleRecipe k = r.original;
            if (r.changed) {
                validate(r);
                k = KettleRecipeAccessor.td$create(
                    r.output.copy(),
                    r.bonus,
                    r.familiar,
                    r.power,
                    r.color,
                    r.dimension == null ? 0 : r.dimension,
                    r.inBook,
                    copies(r.inputs));
                if (r.description != null) k.setUnlocalizedName(r.description);
            }
            next.add(k);
            added.put(k, r);
        }
        // Keep the public list authoritative for Witchery's book and other recipe consumers.
        KettleRecipes.instance().recipes.clear();
        KettleRecipes.instance().recipes.addAll(next);
        definitions.clear();
        definitions.putAll(added);
    }

    private static ItemStack[] copies(ItemStack[] inputs) {
        ItemStack[] result = new ItemStack[inputs.length];
        for (int i = 0; i < inputs.length; i++) result[i] = inputs[i].copy();
        return result;
    }

    public static ItemStack input(ItemStack s) {
        if (s == null || s.getItem() == null
            || s.stackSize != 1
            || s.getItemDamage() < 0
            || s.getItemDamage() == 32767
            || s.hasTagCompound()
            || s.getItem() == Items.glass_bottle)
            throw new IllegalArgumentException(
                "ingredients must be single exact items without NBT; glass bottles are reserved for collection");
        return s.copy();
    }

    private static void validate(Recipe r) {
        if (r.inputs == null || r.inputs.length != 6)
            throw new IllegalArgumentException("a kettle recipe needs exactly six ingredients");
        for (ItemStack s : r.inputs) input(s);
        ItemStack s = r.output;
        if (s == null || s.getItem() == null
            || s.getItemDamage() < 0
            || s.getItemDamage() == 32767
            || s.stackSize < 1
            || s.stackSize > Math.min(64, s.getMaxStackSize()))
            throw new IllegalArgumentException("output must be a concrete stack within its stack limit");
        if (!Float.isFinite(r.power) || r.power < 0)
            throw new IllegalArgumentException("power must be finite and nonnegative");
        if (r.bonus < 0 || r.bonus > (Math.min(64, s.getMaxStackSize()) - 1) / 2) throw new IllegalArgumentException(
            "bonus must fit the output stack limit, including two successful clothing bonuses");
        if (r.color == 0) throw new IllegalArgumentException("brew color cannot be the native ruined-brew sentinel");
        if (r.familiar < 0 || r.familiar > 3) throw new IllegalArgumentException("unknown familiar");
    }

    public static String familiarName(int type) {
        return new String[] { "none", "cat", "toad", "owl" }[type];
    }

    public static int familiar(String value) {
        for (int i = 0; i < 4; i++) if (familiarName(i).equalsIgnoreCase(value)) return i;
        throw new IllegalArgumentException("familiar must be none, cat, toad, or owl");
    }

    public static List<String> requirements(KettleRecipe k) {
        KettleRecipeAccessor a = (KettleRecipeAccessor) k;
        Recipe r = definition(k);
        Integer dimension = r != null ? r.dimension : a.td$dimension() == 0 ? null : a.td$dimension();
        List<String> lines = new ArrayList<>();
        if (dimension != null) lines.add("Dimension: " + dimension);
        if (a.td$familiar() != 0) lines.add("Collection requires a " + familiarName(a.td$familiar()) + " familiar");
        if (a.td$bonus() > 0) lines.add("Clothing bonus: +" + a.td$bonus() + " per successful roll");
        return lines;
    }

    public static final class Recipe {

        public final String id;
        public final KettleRecipe original;
        public final boolean custom;
        public boolean changed;
        public ItemStack[] inputs;
        public ItemStack output;
        public float power;
        public int color = 0xFF8844FF, bonus, familiar;
        public Integer dimension;
        public boolean inBook = true;
        public String description;

        public Recipe(String id, ItemStack[] inputs, ItemStack output, float power) {
            this.id = id;
            this.inputs = inputs;
            this.output = output;
            this.power = power;
            original = null;
            custom = true;
            changed = true;
        }

        private Recipe(String id, KettleRecipe k) {
            this.id = id;
            original = k;
            custom = false;
            KettleRecipeAccessor a = (KettleRecipeAccessor) k;
            inputs = copies(k.inputs);
            output = k.output.copy();
            power = k.power;
            color = k.getColor();
            bonus = a.td$bonus();
            familiar = a.td$familiar();
            dimension = a.td$dimension() == 0 ? null : a.td$dimension();
            inBook = k.inBook;
            description = a.td$description();
        }

        private Recipe(Recipe r) {
            id = r.id;
            original = r.original;
            custom = r.custom;
            changed = true;
            inputs = copies(r.inputs);
            output = r.output.copy();
            power = r.power;
            color = r.color;
            bonus = r.bonus;
            familiar = r.familiar;
            dimension = r.dimension;
            inBook = r.inBook;
            description = r.description;
        }

        public Recipe copy() {
            return new Recipe(this);
        }
    }
}

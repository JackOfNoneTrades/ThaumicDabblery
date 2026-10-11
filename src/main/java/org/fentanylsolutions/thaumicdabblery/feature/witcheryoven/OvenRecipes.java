package org.fentanylsolutions.thaumicdabblery.feature.witcheryoven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.blocks.BlockWitchesOven.TileEntityWitchesOven;

/** Ordered immutable definitions. No dependency on scripting classes on the crafting path. */
public final class OvenRecipes {

    private static volatile List<Recipe> rules = Collections.emptyList();
    public static volatile int generation;

    private OvenRecipes() {}

    public static List<Recipe> rules() {
        return rules;
    }

    public static void replace(List<Recipe> next) {
        rules = Collections.unmodifiableList(new ArrayList<>(next));
        generation++;
    }

    public static Recipe select(ItemStack input) {
        if (!WitcheryOvenFeature.isEnabled() || input == null) return null;
        List<Recipe> snapshot = rules;
        for (int i = snapshot.size() - 1; i >= 0; i--) if (snapshot.get(i).input.matches(input)) return snapshot.get(i);
        return null;
    }

    public static final class Input {

        public final ItemStack stack;
        public final String ore;
        public final int amount;

        public Input(ItemStack stack, String ore, int amount) {
            this.stack = stack == null ? null : stack.copy();
            this.ore = ore;
            this.amount = amount;
        }

        public boolean matches(ItemStack candidate) {
            if (candidate == null || Witchery.Items.GENERIC.itemEmptyClayJar.isMatch(candidate)) return false;
            if (ore != null) {
                for (ItemStack entry : OreDictionary.getOres(ore))
                    if (OreDictionary.itemMatches(entry, candidate, false)) return true;
                return false;
            }
            return OreDictionary.itemMatches(stack, candidate, false)
                && (!stack.hasTagCompound() || ItemStack.areItemStackTagsEqual(stack, candidate));
        }

        public List<ItemStack> examples() {
            List<ItemStack> result = new ArrayList<>();
            for (ItemStack entry : ore == null ? Collections.singletonList(stack) : OreDictionary.getOres(ore)) {
                ItemStack copy = entry.copy();
                copy.stackSize = amount;
                result.add(copy);
            }
            return result;
        }

        public String describe() {
            return (ore == null ? itemName(stack) : "<ore:" + ore + ">") + " * " + amount;
        }
    }

    public static String itemName(ItemStack stack) {
        return stack == null ? "none"
            : "<" + Item.itemRegistry.getNameForObject(stack.getItem())
                + ":"
                + stack.getItemDamage()
                + ">"
                + (stack.hasTagCompound() ? stack.getTagCompound()
                    .toString() : "");
    }

    public static final class Recipe {

        public final String id;
        public final Input input;
        public final ItemStack output, byproduct;
        public final int jars;
        public final double chance;
        public final boolean funnels, requireJars;

        public Recipe(String id, Input input, ItemStack output, ItemStack byproduct, int jars, double chance,
            boolean funnels, boolean requireJars) {
            this.id = id;
            this.input = input;
            this.output = output == null ? null : output.copy();
            this.byproduct = byproduct == null ? null : byproduct.copy();
            this.jars = jars;
            this.chance = chance;
            this.funnels = funnels;
            this.requireJars = requireJars;
        }

        public Recipe byproduct(ItemStack item, int jars, double chance, boolean funnels) {
            return new Recipe(id, input, output, item, jars, chance, funnels, item != null && requireJars);
        }

        public Recipe required(boolean required) {
            return new Recipe(id, input, output, byproduct, jars, chance, funnels, required);
        }
    }

    private static boolean fits(TileEntityWitchesOven oven, int slot, ItemStack result) {
        if (result == null) return false;
        ItemStack current = oven.getStackInSlot(slot);
        int limit = Math.min(oven.getInventoryStackLimit(), result.getMaxStackSize());
        return current == null ? result.stackSize <= limit
            : current.isItemEqual(result) && ItemStack.areItemStackTagsEqual(current, result)
                && current.stackSize <= Math.min(limit, current.getMaxStackSize()) - result.stackSize;
    }

    private static boolean canCollect(TileEntityWitchesOven oven, Recipe recipe) {
        ItemStack jars = oven.getStackInSlot(4);
        return recipe.byproduct != null && fits(oven, 3, recipe.byproduct)
            && (recipe.jars == 0 || jars != null && Witchery.Items.GENERIC.itemEmptyClayJar.isMatch(jars)
                && jars.stackSize >= recipe.jars);
    }

    public static boolean canCraft(TileEntityWitchesOven oven, Recipe recipe) {
        ItemStack input = oven.getStackInSlot(0);
        return input != null && recipe.input.matches(input)
            && input.stackSize >= recipe.input.amount
            && fits(oven, 2, recipe.output)
            && (!recipe.requireJars || canCollect(oven, recipe));
    }

    private static void insert(TileEntityWitchesOven oven, int slot, ItemStack result) {
        ItemStack current = oven.getStackInSlot(slot);
        if (current == null) oven.setInventorySlotContents(slot, result.copy());
        else current.stackSize += result.stackSize;
    }

    public static void craft(TileEntityWitchesOven oven, Recipe recipe, double funnelBonus) {
        if (oven.getWorldObj().isRemote || !canCraft(oven, recipe)) return;
        boolean collect = canCollect(oven, recipe);
        double chance = Math.min(1, recipe.chance + (recipe.funnels ? funnelBonus : 0));
        boolean produced = collect && chance > 0 && (chance >= 1 || oven.getWorldObj().rand.nextDouble() < chance);
        insert(oven, 2, recipe.output);
        if (produced) {
            insert(oven, 3, recipe.byproduct);
            if (recipe.jars > 0) oven.decrStackSize(4, recipe.jars);
        }
        oven.decrStackSize(0, recipe.input.amount);
        oven.markDirty();
        oven.getWorldObj()
            .markBlockForUpdate(oven.xCoord, oven.yCoord, oven.zCoord);
    }
}

package org.fentanylsolutions.thaumicdabblery.feature.witcheryoven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.witcheryoven.OvenRecipes.Recipe;

import com.emoniph.witchery.Witchery;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.FurnaceRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;

/** Extends the native handler so its GUI, fuels and progress indicators stay familiar. */
public final class OvenNEI {

    private OvenNEI() {}

    public static void populate(TemplateRecipeHandler handler, ItemStack query, boolean usage) {
        if (!WitcheryOvenFeature.isEnabled() || OvenRecipes.rules()
            .isEmpty()) return;
        Iterator<TemplateRecipeHandler.CachedRecipe> iterator = handler.arecipes.iterator();
        while (iterator.hasNext()) {
            TemplateRecipeHandler.CachedRecipe cached = iterator.next();
            if (cached instanceof Cached) {
                iterator.remove();
                continue;
            }
            boolean empty = false;
            for (PositionedStack input : cached.getIngredients()) {
                List<ItemStack> keep = new ArrayList<>();
                for (ItemStack candidate : input.items) if (OvenRecipes.select(candidate) == null) keep.add(candidate);
                if (keep.isEmpty()) {
                    empty = true;
                    break;
                }
                input.items = keep.toArray(new ItemStack[0]);
                input.setPermutationToRender(0);
            }
            if (empty) iterator.remove();
        }
        for (Recipe recipe : OvenRecipes.rules()) {
            if (recipe.output == null) continue;
            // PositionedStack expands wildcard metadata to the actual item variants.
            PositionedStack input = new PositionedStack(recipe.input.examples(), 51, 6);
            List<ItemStack> candidates = new ArrayList<>();
            for (ItemStack candidate : input.items)
                if (OvenRecipes.select(candidate) == recipe) candidates.add(candidate);
            if (candidates.isEmpty()) continue;
            if (query != null) {
                boolean match = usage
                    ? OvenRecipes.select(query) == recipe
                        || recipe.jars > 0 && Witchery.Items.GENERIC.itemEmptyClayJar.isMatch(query)
                    : same(recipe.output, query) || same(recipe.byproduct, query);
                if (!match) continue;
            }
            handler.arecipes.add(new Cached(handler, recipe, candidates));
        }
    }

    private static boolean same(ItemStack a, ItemStack b) {
        return a != null && b != null && a.isItemEqual(b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    public static void tooltip(TemplateRecipeHandler handler, int index, ItemStack item, List<String> lines) {
        if (index < 0 || index >= handler.arecipes.size() || !(handler.arecipes.get(index) instanceof Cached)) return;
        Recipe recipe = ((Cached) handler.arecipes.get(index)).recipe;
        if (recipe.byproduct == null) return;
        if (same(item, recipe.byproduct) || Witchery.Items.GENERIC.itemEmptyClayJar.isMatch(item)) {
            lines.add(String.format(Locale.ROOT, "Base chance: %.1f%%", recipe.chance * 100));
            if (recipe.funnels) lines.add("Side fume funnels increase this chance");
            lines.add(recipe.jars == 0 ? "No jars consumed" : recipe.jars + " empty clay jar(s) consumed on success");
            lines.add(
                recipe.requireJars ? "Waits for jars and byproduct space"
                    : "Cooking continues if the byproduct cannot be collected");
        }
    }

    public static final class Cached extends TemplateRecipeHandler.CachedRecipe {

        public final Recipe recipe;
        private final TemplateRecipeHandler owner;
        private final PositionedStack input, result, byproduct, jars;

        public Cached(TemplateRecipeHandler owner, Recipe recipe, List<ItemStack> inputs) {
            owner.super();
            this.owner = owner;
            this.recipe = recipe;
            input = new PositionedStack(inputs, 51, 6);
            result = new PositionedStack(recipe.output.copy(), 113, 10);
            byproduct = recipe.byproduct == null ? null : new PositionedStack(recipe.byproduct.copy(), 113, 42);
            jars = recipe.jars == 0 ? null
                : new PositionedStack(Witchery.Items.GENERIC.itemEmptyClayJar.createStack(recipe.jars), 78, 42);
        }

        public PositionedStack getResult() {
            return result;
        }

        public List<PositionedStack> getIngredients() {
            return getCycledIngredients(owner.cycleticks / 48, Collections.singletonList(input));
        }

        public List<PositionedStack> getOtherStacks() {
            List<PositionedStack> stacks = new ArrayList<>();
            if (FurnaceRecipeHandler.afuels != null && !FurnaceRecipeHandler.afuels.isEmpty()) stacks
                .add(FurnaceRecipeHandler.afuels.get(owner.cycleticks / 48 % FurnaceRecipeHandler.afuels.size()).stack);
            if (byproduct != null) stacks.add(byproduct);
            if (jars != null) stacks.add(jars);
            return stacks;
        }
    }
}

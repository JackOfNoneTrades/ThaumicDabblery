package org.fentanylsolutions.thaumicdabblery.feature.witcherycauldron;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.fentanylsolutions.thaumicdabblery.feature.witcherycauldron.CauldronRecipes.Recipe;

import com.emoniph.witchery.brewing.WitcheryBrewRegistry;
import com.emoniph.witchery.brewing.action.BrewActionRitualRecipe;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;

public final class CauldronNEI {

    private CauldronNEI() {}

    public static void populate(TemplateRecipeHandler handler, ItemStack query, boolean usage) {
        for (Recipe recipe : CauldronRecipes.recipes()) add(handler, recipe, query, usage);
        // Leave room for recipes registered after our native snapshot by another integration.
        for (BrewActionRitualRecipe action : WitcheryBrewRegistry.INSTANCE.getRecipes()) {
            if (CauldronRecipes.manages(action.ITEM_KEY)) continue;
            for (BrewActionRitualRecipe.Recipe expanded : action.getExpandedRecipes()) {
                ItemStack[] inputs = new ItemStack[expanded.ingredients.length - 1];
                System.arraycopy(expanded.ingredients, 0, inputs, 0, inputs.length);
                add(
                    handler,
                    new Recipe("", inputs, action.ITEM_KEY.toStack(), expanded.result, null, true),
                    query,
                    usage);
            }
        }
    }

    private static void add(TemplateRecipeHandler handler, Recipe recipe, ItemStack query, boolean usage) {
        if (!CauldronRecipes.visible(recipe)) return;
        if (query != null) {
            boolean matches = usage ? same(recipe.trigger, query) : same(recipe.output, query);
            if (usage) for (ItemStack input : recipe.ingredients) matches |= same(input, query);
            if (!matches) return;
        }
        handler.arecipes.add(new Cached(handler, recipe));
    }

    private static boolean same(ItemStack a, ItemStack b) {
        return a != null && b != null && a.isItemEqual(b);
    }

    public static void draw(TemplateRecipeHandler handler, int index) {
        if (index < handler.arecipes.size() && handler.arecipes.get(index) instanceof Cached) {
            Recipe recipe = ((Cached) handler.arecipes.get(index)).recipe;
            if (recipe.basePower() > 0) GuiDraw.drawString("Base power: " + recipe.basePower(), 5, 54, 0x404040, false);
        }
    }

    public static void tooltip(TemplateRecipeHandler handler, int index, ItemStack item, List<String> lines) {
        if (index < 0 || index >= handler.arecipes.size() || !(handler.arecipes.get(index) instanceof Cached)) return;
        Recipe recipe = ((Cached) handler.arecipes.get(index)).recipe;
        if (same(recipe.trigger, item)) lines.add("Add last to start crafting");
        else for (ItemStack ingredient : recipe.ingredients) if (same(ingredient, item)) {
            lines.add("Add ingredients individually, in the displayed order");
            break;
        }
        lines.add("Base altar power: " + recipe.basePower() + " (before chalk-circle modifiers)");
    }

    public static final class Cached extends TemplateRecipeHandler.CachedRecipe {

        public final Recipe recipe;
        private final List<PositionedStack> inputs = new ArrayList<>();
        private final PositionedStack result;

        Cached(TemplateRecipeHandler handler, Recipe recipe) {
            handler.super();
            this.recipe = recipe;
            for (int i = 0; i < recipe.ingredients.length; i++)
                inputs.add(new PositionedStack(recipe.ingredients[i].copy(), 10 + i * 18, 6));
            inputs.add(new PositionedStack(recipe.trigger.copy(), 10 + recipe.ingredients.length * 18, 6));
            result = new PositionedStack(recipe.output.copy(), 119, 31);
        }

        public PositionedStack getResult() {
            return result;
        }

        public List<PositionedStack> getIngredients() {
            return Collections.unmodifiableList(inputs);
        }
    }
}

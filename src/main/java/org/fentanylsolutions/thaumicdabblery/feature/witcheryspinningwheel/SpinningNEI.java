package org.fentanylsolutions.thaumicdabblery.feature.witcheryspinningwheel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.emoniph.witchery.crafting.SpinningRecipes;
import com.emoniph.witchery.crafting.SpinningRecipes.SpinningRecipe;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;

public final class SpinningNEI {

    private SpinningNEI() {}

    public static void populate(TemplateRecipeHandler handler, ItemStack query, boolean usage) {
        for (SpinningRecipe r : SpinningRecipes.instance().recipes) {
            if (query == null || (usage ? r.uses(query) : r.result.isItemEqual(query)))
                handler.arecipes.add(new Cached(handler, r));
        }
    }

    public static void tooltip(TemplateRecipeHandler handler, int index, ItemStack item, List<String> lines) {
        if (index < 0 || index >= handler.arecipes.size() || !(handler.arecipes.get(index) instanceof Cached)) return;
        lines.add("Time: 15 seconds at full power");
        lines.add("Altar drain: 0.6 per working tick (180 total)");
    }

    public static final class Cached extends TemplateRecipeHandler.CachedRecipe {

        private final List<PositionedStack> ingredients = new ArrayList<>();
        private final PositionedStack output;

        Cached(TemplateRecipeHandler handler, SpinningRecipe r) {
            handler.super();
            ingredients.add(new PositionedStack(r.fibre.copy(), 51, 9));
            for (int i = 0; i < r.modifiers.length; i++)
                ingredients.add(new PositionedStack(r.modifiers[i].copy(), 51 + 18 * i, 42));
            output = new PositionedStack(r.result.copy(), 113, 9);
        }

        public PositionedStack getResult() {
            return output;
        }

        public List<PositionedStack> getIngredients() {
            return Collections.unmodifiableList(ingredients);
        }
    }
}

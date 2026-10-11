package org.fentanylsolutions.thaumicdabblery.feature.witcherydistillery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.crafting.DistilleryRecipes;
import com.emoniph.witchery.crafting.DistilleryRecipes.DistilleryRecipe;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;

public final class DistilleryNEI {

    private DistilleryNEI() {}

    public static void populate(TemplateRecipeHandler handler, ItemStack query, boolean usage) {
        for (DistilleryRecipe r : DistilleryRecipes.instance().recipes) {
            boolean matches = query == null
                || (usage
                    ? DistilleryRegistry.uses(r, query)
                        || r.jars > 0 && Witchery.Items.GENERIC.itemEmptyClayJar.isMatch(query)
                    : r.resultsIn(query));
            if (matches) handler.arecipes.add(new Cached(handler, r));
        }
    }

    public static void tooltip(TemplateRecipeHandler handler, int index, ItemStack item, List<String> lines) {
        if (index < 0 || index >= handler.arecipes.size() || !(handler.arecipes.get(index) instanceof Cached)) return;
        DistilleryRecipe r = ((Cached) handler.arecipes.get(index)).recipe;
        if (r.jars > 0) lines.add("Empty clay jars: " + r.jars);
        lines.add("Time: 40 seconds at full power");
        lines.add("Altar drain: 0.6 per working tick (480 total)");
    }

    public static final class Cached extends TemplateRecipeHandler.CachedRecipe {

        public final DistilleryRecipe recipe;
        private final List<PositionedStack> inputs = new ArrayList<>(), outputs = new ArrayList<>();

        Cached(TemplateRecipeHandler handler, DistilleryRecipe r) {
            handler.super();
            recipe = r;
            for (int i = 0; i < 2; i++)
                if (r.inputs[i] != null) inputs.add(new PositionedStack(r.inputs[i].copy(), 43, 5 + i * 18));
            if (r.jars > 0)
                inputs.add(new PositionedStack(Witchery.Items.GENERIC.itemEmptyClayJar.createStack(r.jars), 43, 43));
            for (int i = 0; i < 4; i++) if (r.outputs[i] != null)
                outputs.add(new PositionedStack(r.outputs[i].copy(), 105 + i % 2 * 18, 5 + i / 2 * 18));
        }

        public PositionedStack getResult() {
            return outputs.isEmpty() ? null : outputs.get(0);
        }

        public List<PositionedStack> getIngredients() {
            return Collections.unmodifiableList(inputs);
        }

        public List<PositionedStack> getOtherStacks() {
            return outputs.size() < 2 ? Collections.emptyList()
                : Collections.unmodifiableList(outputs.subList(1, outputs.size()));
        }
    }
}

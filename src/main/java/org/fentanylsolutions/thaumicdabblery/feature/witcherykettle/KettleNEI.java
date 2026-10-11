package org.fentanylsolutions.thaumicdabblery.feature.witcherykettle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.emoniph.witchery.crafting.KettleRecipes;
import com.emoniph.witchery.crafting.KettleRecipes.KettleRecipe;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;

public final class KettleNEI {

    private KettleNEI() {}

    public static void populate(TemplateRecipeHandler handler, ItemStack query, boolean usage) {
        for (KettleRecipe r : KettleRecipes.instance().recipes) {
            boolean matches = query == null || !usage && r.output.isItemEqual(query);
            if (query != null && usage)
                for (ItemStack in : r.inputs) if (in != null && in.isItemEqual(query)) matches = true;
            if (matches) handler.arecipes.add(new Cached(handler, r));
        }
    }

    public static void draw(TemplateRecipeHandler handler, int index) {
        if (index >= 0 && index < handler.arecipes.size() && handler.arecipes.get(index) instanceof Cached) {
            KettleRecipe r = ((Cached) handler.arecipes.get(index)).recipe;
            if (r.power > 0) {
                GuiDraw.drawString("Altar power", 80, 45, 0x404040, false);
                GuiDraw.drawString(number(r.power), 80, 55, 0x404040, false);
            }
        }
    }

    public static String number(float value) {
        String text = Float.toString(value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    public static void tooltip(TemplateRecipeHandler handler, int index, ItemStack item, List<String> lines) {
        if (index < 0 || index >= handler.arecipes.size() || !(handler.arecipes.get(index) instanceof Cached)) return;
        KettleRecipe r = ((Cached) handler.arecipes.get(index)).recipe;
        if (r.power > 0) lines.add("Altar power: " + number(r.power));
        lines.add("Add ingredients individually; order does not matter");
        lines.add("Collect with glass bottles");
        lines.addAll(KettleRegistry.requirements(r));
    }

    public static final class Cached extends TemplateRecipeHandler.CachedRecipe {

        public final KettleRecipe recipe;
        private final List<PositionedStack> inputs = new ArrayList<>();
        private final PositionedStack result;

        Cached(TemplateRecipeHandler handler, KettleRecipe recipe) {
            handler.super();
            this.recipe = recipe;
            for (int i = 0; i < recipe.inputs.length; i++) if (recipe.inputs[i] != null)
                inputs.add(new PositionedStack(recipe.inputs[i].copy(), i < 3 ? 25 : 43, i % 3 * 18 + 6));
            result = new PositionedStack(recipe.output.copy(), 119, 24);
        }

        public PositionedStack getResult() {
            return result;
        }

        public List<PositionedStack> getIngredients() {
            return Collections.unmodifiableList(inputs);
        }
    }
}
